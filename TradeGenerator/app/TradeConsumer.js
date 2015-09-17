var app = require('express')();
var http = require('http').Server(app);
var io = require('socket.io')(http);
var port = process.ENV.PORT || 3030;

var kafkaLocation = process.env.KAFKA || 'vagrant';
var topicName = process.env.KAFKA_TOPIC || "trades";
var kafka = require('kafka-node'),
    Lightning = require('lightning.js'),
    Consumer = kafka.Consumer,
    client = new kafka.Client(kafkaLocation+':2181','trade-generator'),
    consumer = new Consumer(
        client,
        [
            { topic: topicName}
        ],
        {
            autoCommit: false
        }
    );

app.get('/', function(req, res){
  res.sendFile(__dirname + '/index.html');
});




//var lightning = new Lightning({host:'http://' + (process.env.LIGHTNING_HOST||'lightning') + ':'+(process.env.LIGHTNING_PORT||'3000')});

var getRepeatingCharacter = function(char,length) {
  return Array.apply(null,{length:length}).map(function(d,i){return ''}).join(char);
}

var logJsonNicely = function (jsonArray) {
  if (jsonArray.length) {
    var headers = Object.keys(jsonArray[0]);
    var headerString = "| " + headers.join("   | ") + "   |";
    console.log(getRepeatingCharacter('_',headerString.length))
    console.log(headerString);
    console.log(getRepeatingCharacter('_',headerString.length))
    var rows = jsonArray.map(function(d) {
      return headers.map(function(h,i){return (d[h] + getRepeatingCharacter(' ', h.length + 6)).substr(0,h.length + 3);}).join('| ');
    });
    console.log("| " + rows.join('|\n| ') + "|");
    console.log(getRepeatingCharacter('_',headerString.length))
  } else {
    console.log('No Data');
  }
};

var messageCount = 0;

 consumer.on('message', function (message) {
    if (message.value) {
      try {
        var jsonValue = JSON.parse(message.value);
        console.log(logJsonNicely([jsonValue]));
        process.stdout.write("Recieved " + messageCount++ + " messages\r");
        io.emit('trades', jsonValue);
      } catch (error) {

      }
    }
}).on("error",function(e) {
  console.log(e);
});

//io.on('connection', function(socket){
//
//});

http.listen(port, function(){
  console.log('listening on *:' + port);
});

//lightning
//    .lineStreaming([1])
//    .then(function(viz) {
//      viz.append([jsonValue.price]);
//});
