var kafkaLocation = process.env.KAFKA || 'vagrant';
var kafka = require('kafka-node'),
    Consumer = kafka.Consumer,
    client = new kafka.Client(kafkaLocation+':2181','trade-generator'),
    consumer = new Consumer(
        client,
        [
            { topic: 'trade-stream'}
        ],
        {
            autoCommit: false
        }
    );

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
        console.log(logJsonNicely([JSON.parse(message.value)]))
        process.stdout.write("Recieved " + messageCount++ + " messages\r");
      } catch (error) {
//        console.log(error);
//        console.log(message)
      }
    }
}).on("error",function(e) {
  console.log(e);
})
