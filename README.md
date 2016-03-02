# Description
* Contains simple vagrant that setups 3 machines(master 172.17.177.11, slave1 172.17.177.21 & slave2 172.17.177.22) with flink clustered setup
* Contains some examples for processing stream of tweets with flink
* TwitterJob will check tweets by list of filters(comma separated) and will save aggregated stats into local redis
* You can check current stats from master machine with redis-cli hgetall stats
* Then those stats are pushed by node.js with websocket to the client

# Setup
* place your twitter credentials in twitter.properties in form of:
 * secret=...
 * consumerSecret=...
 * token=...-...
 * consumerKey=...
* build artifact from host machine(your desktop): cd flink-tweets && mvn clean package
* start & provision cluster with : cd .. ; vagrant up
* Check that everything is ok with http://172.17.177.11:8081 (you should be able to see 2 task managers connected)
* Submit flink job that aggregates twitter stats job on master machine(vagrant ssh master) than: /usr/local/flink/bin/flink run -c example.TwitterJob /vagrant/flink-tweets/target/flink-tweets-1.0-SNAPSHOT.jar /vagrant/twitter.properties trump,clinton,sanders,cruz,rubio,drump 172.17.177.11
* Start on master machine frontend that serves the stats: nodejs /vagrant/server.js
* Open http://172.17.177.11:8088/index.html
