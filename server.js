
var http = require("http");
var static = require('/home/vagrant/node_modules/node-static');
var url = require('url');
var fileServer = new static.Server('/vagrant/html/');

var redis = require("/home/vagrant/node_modules/redis");
var redisClient = redis.createClient();

var WebSocketServer = require('/home/vagrant/node_modules/websocket').server;




redisClient.on("error", function (err) {
    console.log("Error " + err);
});

var endsWith = function(str, suffix) {
   return str.indexOf(suffix, str.length - suffix.length) !== -1;
};

http.createServer(function (request, response) {
   var reqUrl = request.url;
   if(endsWith(reqUrl, 'html') || endsWith(reqUrl, 'js')) {
      console.log("serving static " + reqUrl);
      fileServer.serve(request, response);
      return;
   }
   response.writeHead(200, {'Content-Type': 'application/json'});
   var queryURL = url.parse(reqUrl, true);
   var key = queryURL.query['key'];
   redisClient.exists(key, function(err, exists) {
      if(exists == 1) {
            redisClient.lrange(key, 0, 99, function(err, reply) {
               response.end(JSON.stringify(reply));
            });
      } else {
         response.end("{}");
      }
   });

}).listen(8088);
console.log('Server running at http://127.0.0.1:8088/');


var websocketServer = http.createServer(function(request, response) {
});

var wsServer = new WebSocketServer({
    httpServer: websocketServer
});

wsServer.on('request', function(request) {
    console.log((new Date()) + ' Connection from origin ' + request.origin + '.');

    var connection = request.accept(null, request.origin);

    console.log((new Date()) + ' Connection accepted.');

    var intervalHandle;
    // user sent some message
    connection.on('message', function(message) {
         var key = 'stats';
         intervalHandle = setInterval(function() {
            redisClient.hgetall(key, function(err, reply) {
              connection.sendUTF(JSON.stringify(reply));
            });
         }, 2000);
      }
    });

    connection.on('close', function(connection) {
        
    });

});

websocketServer.listen(1337);
console.log('ws running at http://127.0.0.1:1337/');
