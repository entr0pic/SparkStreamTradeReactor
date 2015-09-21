var app = require('express')();
var http = require('http').Server(app);
var io = require('socket.io')(http);
var port = process.env.HTTP_PORT || 3030;

var kafkaLocation = process.env.KAFKA || 'vagrant';
var topicName = process.env.KAFKA_TOPIC || "trades";
var statsTopic = "kmstats";

var kafka = require('kafka-node'),
    Consumer = kafka.Consumer,
    client = new kafka.Client(kafkaLocation+':2181','trade-consumer'),
    tradeConsumer = new Consumer(
        client,
        [
            { topic: topicName}
        ],
        {
            autoCommit: false
        }
    ),
    statsConsumer = new Consumer(
        client,
        [
            { topic: statsTopic}
        ],
        {
            autoCommit: false
        }
    );;

app.get('/', function(req, res){
  res.sendFile(__dirname + '/index.html');
});




//var lightning = new Lightning({host:'http://' + (process.env.LIGHTNING_HOST||'lightning') + ':'+(process.env.LIGHTNING_PORT||'3000')});

var mapCSVtoJSON = function(csvString) {
  var trimQuotes = function(input) {
    if (input.slice(0,1) == '"' && input.slice(-1) == '"')
      return input.slice(1,input.length-1);
    return input;
  };
  var spliter = csvString.length == trimQuotes(csvString).length ? ',' : '","';
  var lines = csvString.split("\n").map(trimQuotes);
  var headers = lines.shift().split(spliter).map(trimQuotes);
  return lines.map(function(line){
    return line.split(spliter).map(trimQuotes).reduce(function(json,cell,index){
      json[headers[index]] = cell;
      return json;
    },{});
  })
}

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

//var fs = require('fs');
//
//var exchanges = mapCSVtoJSON(fs.readFileSync('exchanges.csv').toString());
//var ccps = mapCSVtoJSON(fs.readFileSync('ccps.csv').toString());
//var banks = mapCSVtoJSON(fs.readFileSync('banks.csv').toString()).filter(function(d){return d.swift && !~d.swift.indexOf('"')});
//var symbols = mapCSVtoJSON(fs.readFileSync('symbols_clean.csv').toString()).filter(function(d){return d.Currency});

var messageCount = 0;
var tradesMsgCnt = 0;
var statsMsgCnt = 0;

 tradeConsumer.on('message', function (message) {
     try {
        if (message.value) {
            var jsonValue = JSON.parse(message.value);
            console.log(logJsonNicely([jsonValue]));
            process.stdout.write("Received " + messageCount++ + "(" + tradesMsgCnt++ + ")" + " ["+topicName+"] messages\r");
            io.emit(topicName, jsonValue);
        } else {
            process.stdout.write("Message ["+topicName+"] " + message);
        }
    } catch (error) {
        process.stderr.write("Err ["+topicName+"]: ", error)
    }
}).on("error",function(e) {
  console.log("Err: ", e);
});

 statsConsumer.on('message', function (message) {
    try {
        process.stdout.write("Message ["+statsTopic+"] " + message);
        process.stdout.write("Received " + messageCount++ + "(" + statsMsgCnt++ + ")" + " ["+statsTopic+"] messages\r");
        var jsonValue = JSON.parse(message);
        io.emit(statsTopic,message);
    } catch (error) {
        process.stderr.write("Err ["+statsTopic+"]: ", error);
    }
}).on("error",function(e) {
     console.log("Err ["+statsTopic+"]: ", e);
});


http.listen(port, function(){
  console.log('listening on *:' + port);
});

//lightning
//    .lineStreaming([1])
//    .then(function(viz) {
//      viz.append([jsonValue.price]);
//});
