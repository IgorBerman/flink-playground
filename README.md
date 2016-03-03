![Alt text](/elections.png?raw=true "Optional Title")
# Description
* Setups flink cluster with vagrant vms(master 172.17.177.11, slave1 172.17.177.21 & slave2 172.17.177.22)
* Flink-tweets project has flink TwitterJob that aggregates filtered tweets every second
* Aggregated stats are saved in local redis(You can check current stats from master machine with redis-cli hgetall stats)
* Those stats are pushed by node.js server to the browser(tested with chrome)

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
