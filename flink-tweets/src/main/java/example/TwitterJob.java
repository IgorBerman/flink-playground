package example;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.HashSet;
import java.util.StringTokenizer;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.com.google.common.base.Splitter;
import org.apache.flink.shaded.com.google.common.collect.Sets;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.connectors.json.JSONParseFlatMap;
import org.apache.flink.streaming.connectors.twitter.TwitterFilterSource;
import org.apache.flink.util.Collector;

import redis.clients.jedis.Jedis;

public class TwitterJob {

	public static void main(String[] args) throws Exception {
		// set up the execution environment
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);

		String authPath = args[0];
		String filters = args[1];
		String redisHost = args[2];
		TwitterFilterSource tweets = new TwitterFilterSource(authPath);
		tweets.filterLanguage("en");
		for (String filter : Splitter.on(",").split(filters)) {
			tweets.trackTerm(filter);
		}

		DataStream<String> streamSource = env.addSource(tweets).rebalance();
		// streamSource.print();
		KeyedStream<Tuple2<String, Integer>, String> keyed = streamSource
				.flatMap(new SelectEnglishAndTokenizeFlatMap()).filter(new WordsFilter(filters))
				.keyBy(new WordKeySelector());

		WindowedStream<Tuple2<String, Integer>, String, TimeWindow> windowed = keyed.timeWindow(Time.seconds(1));

		SingleOutputStreamOperator<Tuple2<String, Integer>> summed = windowed.sum(1);
		
		summed.addSink(new RedisSink(redisHost));
		summed.addSink(new PrintSinkFunction<Tuple2<String, Integer>>());
		// execute program
		env.execute("Twitter Streaming Example");
	}

	public static class WordKeySelector implements KeySelector<Tuple2<String, Integer>, String> {
		@Override
		public String getKey(Tuple2<String, Integer> value) throws Exception {
			return value.getField(0);
		}
	}

	public static class WordsFilter implements FilterFunction<Tuple2<String, Integer>> {
		private HashSet<String> filterSet;

		public WordsFilter(String filter) {
			filterSet = Sets.newHashSet(Splitter.on(",").split(filter));
		}

		@Override
		public boolean filter(Tuple2<String, Integer> value) throws Exception {
			return filterSet.contains(value.getField(0));
		}

	}

	public static class SelectEnglishAndTokenizeFlatMap extends JSONParseFlatMap<String, Tuple2<String, Integer>> {
		private static final long serialVersionUID = 1L;

		/**
		 * Select the language from the incoming JSON text
		 */
		@Override
		public void flatMap(String value, Collector<Tuple2<String, Integer>> out) throws Exception {
			// message of tweet
			try {
				String string = getString(value, "text");
				StringTokenizer tokenizer = new StringTokenizer(string);

				// split the message
				while (tokenizer.hasMoreTokens()) {
					String result = tokenizer.nextToken().replaceAll("\\s*", "").toLowerCase();

					if (!result.equals("")) {
						out.collect(new Tuple2<>(result, 1));
					}
				}
			} catch (Exception e) {

			}
		}
	}

	private static class RedisSink extends RichSinkFunction<Tuple2<String, Integer>> {
		private Jedis jedisConn;
		private String redisHost;

		public RedisSink(String redisHost) {
			this.redisHost = redisHost;
		}

		@Override
		public void open(Configuration parameters) throws Exception {
			super.open(parameters);
			jedisConn = new Jedis(redisHost);
		}

		@Override
		public void invoke(Tuple2<String, Integer> t) throws Exception {
			String key = String.valueOf(t.getField(0));
			String val = String.valueOf(t.getField(1));
			jedisConn.hset("stats", key, val);
		}

		@Override
		public void close() throws Exception {
			super.close();
			jedisConn.close();
		}
	}

}
