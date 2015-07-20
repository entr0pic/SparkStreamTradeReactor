var fs = require('fs');
var kafka = require('kafka-node');

var tradesPerSecond = [0,20];
var tadesQueue = [];

var kafkaLocation = process.env.KAFKA || 'vagrant';

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

function getRandomInt(range) {
    return Math.floor(Math.random() * (range[1] - range[0] + 1)) + range[0];
}

var getRepeatingCharacter = function(char,length) {
  return Array.apply(null,{length:length}).map(function(d,i){return ''}).join(char);
}
var randomSample = function(sampleArray, sampleSize) {
  if (sampleArray.length > sampleSize) {
    var randIndexes = Array.apply(null,{length:sampleArray.length})
                          .map(function(d,i){return i})
                          .sort(function() {return Math.random()*2-1;})
                          .filter(function(d,i){return i < sampleSize});
    return sampleArray.filter(function(d,i){return ~randIndexes.indexOf(i);});
  } else {
    return sampleArray;
  }
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

var exchanges = mapCSVtoJSON(fs.readFileSync('exchanges.csv').toString());
var ccps = mapCSVtoJSON(fs.readFileSync('ccps.csv').toString());
var banks = mapCSVtoJSON(fs.readFileSync('banks.csv').toString()).filter(function(d){return d.swift && !~d.swift.indexOf('"')});
var symbols = mapCSVtoJSON(fs.readFileSync('symbols_clean.csv').toString()).filter(function(d){return d.Currency});

console.log("exchanges");
logJsonNicely(randomSample(exchanges, 10));
console.log("ccps");
logJsonNicely(randomSample(ccps, 10));
console.log("banks");
logJsonNicely(randomSample(banks, 30));
console.log("symbols");
logJsonNicely(randomSample(symbols, 10));

var getRandomPrice = function(symbol) {
  if (symbol.YearHigh && symbol.YearLow)
    return Math.random()*(symbol.YearHigh-symbol.YearLow) + symbol.YearLow;
  if (symbol.LastPrice) {
    return (Math.random()-0.5)*20 * symbol.LastPrice;
  } else {
    return Math.random() * 20;
  }
}

var generateTradePairs = function(count, startDate) {
  if (!startDate) startDate = new Date();
  var trades = [];
  for (var i = 0; i<count; i++) {
    startDate.setMilliseconds(startDate.getMilliseconds() + Math.random() * 1000)
    var dateTime = startDate.toISOString().split('T');
    var exchangeInfo = randomSample(exchanges,1);
    var ccp = randomSample(ccps,1);
    var bank = randomSample(banks,2);
    var symbol = randomSample(symbols,1);
    var side = Math.random()>0.5;
    var price = getRandomPrice(symbol[0])||Math.random()*10;
    var volume = Math.ceil(Math.random()*(symbol[0].AverageDailyVolume||1000));
    trades = trades.concat([{
      trade_date: dateTime[0],
      trade_time: startDate.toISOString(),
      party: bank[0].swift,
      counterparty: bank[1].swift,
      ccp: ccp[0].BICCode,
      exchange: symbol[0].Exchange,
      symbol: symbol[0].Symbol,
      currency: symbol[0].Currency,
      side: side?'B':'S',
      type: symbol[0].Type,
      category: symbol[0].Category,
      price: price,
      volume: volume,
      unit: symbol[0].Unit
    },{
      trade_date: dateTime[0],
      trade_time: startDate.toISOString(),
      party: banks[1].swift,
      counterparty: banks[0].swift,
      ccp: ccp[0].BICCode,
      exchange: symbol[0].Exchange,
      symbol: symbol[0].Symbol,
      currency: symbol[0].Currency,
      side: side?'S':'B',
      type: symbol[0].Type,
      category: symbol[0].Category,
      price: price,
      volume: volume,
      unit: symbol[0].Unit
    }])
  }
  return trades;
}

var newTrades = generateTradePairs(4);

console.log("\n\nGENERATED TRADES\n")
logJsonNicely(newTrades);



Producer = kafka.Producer,
client = new kafka.Client(kafkaLocation+':2181','trade-generator'),
producer = new Producer(client);
//
//payloads = [
//        { topic: 'new-trade', messages: 'hi' },
//        { topic: 'topic2', messages: ['hello', 'world'] }
//    ];
producer.createTopics(['trade-stream'], true, function (err, data) {});
producer.on('ready', function () {
    setInterval(function(){
      payloads = [{topic:'trade-stream',messages: generateTradePairs(getRandomInt(tradesPerSecond)).map(JSON.stringify)}];
      producer.send(payloads, function (err, data) {
        console.log(err||data);
      });
    },1000)
}).on('error',function(error){
  console.log(error)
});
