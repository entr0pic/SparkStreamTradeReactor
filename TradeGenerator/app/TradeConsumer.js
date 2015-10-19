var app = require('express')();
var http = require('http').Server(app);
var io = require('socket.io')(http);
var port = process.env.HTTP_PORT || 3030;

var kafkaLocation = process.env.KAFKA || 'vagrant';
var topicNames = process.env.KAFKA_TOPICS || "trades";

topicNames = topicNames.split(',').map(function(d) {return d.trim(); });

var kafka = require('kafka-node'),
    Consumer = kafka.Consumer,
    client = new kafka.Client(kafkaLocation+':2181','trade-consumer');


console.log("Subscribing to topics:");
console.log(topicNames.map(function(d) {return {topic: d}}));

app.get('/', function(req, res){
  res.sendFile(__dirname + '/index.html');
}).get('/description', function(req, res){
  res.sendFile(__dirname + '/compose.html');
}).get('/background', function(req, res){
  res.sendFile(__dirname + '/background.html');
}).get('/js/dagred3.js', function(req, res){
  res.sendFile(__dirname + '/dagred3.js');
}).get('/assets/*', function(req,res){
  res.sendFile(__dirname + '/' + req.url.split('/').pop());
});

var messageCount = 0;
var topicCount = topicNames.map(function(d){var obj = {}; obj[d] = 0; return obj;});

var startConsuming = function() {
  var consumer = new Consumer(
        client,
        topicNames.map(function(d) {return {topic: d}}),
        {
            autoCommit: false
        }
    );
  consumer.on('message', function (message) {
   console.log(message);
       try {
          if (message.value) {
              io.emit(message.topic, message);
              process.stdout.write("Received " + (messageCount++) + "(" + (topicCount[message.topic]++) + ")" + " ["+message.topic+"] messages: " + message.value + "\r");

          } else {
              process.stdout.write("Message " + message);
          }
      } catch (error) {
          process.stderr.write("Err ", error);
      }
  }).on("error",function(e) {
    console.log("Err: ", e);
    console.log('Retrying in 2 seconds...');
    setTimeout(startConsuming,2000);
  });
};

http.listen(port, function(){
  console.log('listening on *:' + port);
  startConsuming();
});

