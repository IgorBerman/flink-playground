
var http = require("http");
var WebSocketServer = require('/home/vagrant/node_modules/websocket').server;
var static = require('/home/vagrant/node_modules/node-static');
var redis = require("/home/vagrant/node_modules/redis");
var client = redis.createClient();
client.on("error", function (err) {
    console.log("Error " + err);
});
var url = require('url');
var file = new static.Server('/vagrant/html/');


var endsWith = function(str, suffix) {
   //console.log("checking '" + str + "' vs '" + suffix + "'" + str.indexOf(str, str.length - suffix.length));
   return str.indexOf(suffix, str.length - suffix.length) !== -1;
};

http.createServer(function (request, response) {
   var reqUrl = request.url;
   if(endsWith(reqUrl, 'html') || endsWith(reqUrl, 'js')) {
      console.log("serving static " + reqUrl);
      file.serve(request, response);
      return;
   }
   response.writeHead(200, {'Content-Type': 'application/json'});
   var queryURL = url.parse(reqUrl, true);
   var key = queryURL.query['key'];
   //console.log("Quering: " + key);
   client.exists(key, function(err, exists) {
      //console.log(exists);
      if(exists == 1) {
            client.lrange(key, 0, 99, function(err, reply) {
               response.end(JSON.stringify(reply));
            });
      } else {
         response.end("{}");
      }
   });

}).listen(8088);
// Console will print the message
console.log('Server running at http://127.0.0.1:8088/');


var websocketServer = http.createServer(function(request, response) {

});

var wsServer = new WebSocketServer({
    httpServer: websocketServer
});
wsServer.on('request', function(request) {
    console.log((new Date()) + ' Connection from origin ' + request.origin + '.');

    // accept connection - you should check 'request.origin' to make sure that
    // client is connecting from your website
    // (http://en.wikipedia.org/wiki/Same_origin_policy)
    var connection = request.accept(null, request.origin);

    console.log((new Date()) + ' Connection accepted.');

    // user sent some message
    connection.on('message', function(message) {
        if (message.type === 'utf8') { // accept only text

         var key = message.utf8Data;
         console.log("Quering: " + key);
         setInterval(function() {
            client.exists(key, function(err, exists) {
               //console.log(exists);
               if(exists == 1) {
                     client.lrange(key, 0, 0, function(err, reply) {
                        connection.sendUTF(JSON.stringify(reply));
                     });
               }
            })
         }, 1000);
      }
    });

    // user disconnected
    connection.on('close', function(connection) {
        
    });

});

websocketServer.listen(1337);

// Console will print the message
console.log('ws running at http://127.0.0.1:1337/');
