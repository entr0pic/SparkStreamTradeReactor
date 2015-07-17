var http = require('http');
var fs = require('fs');
var stream = require('stream');
var readline = require('readline');

var getBaseOptions = function(){
  return {
    host: 'query.yahooapis.com',
    path: '/v1/public/yql?env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&q=',
    headers: { 'accept': 'application/json' }
  };
}


var pendingRequests = 0;
var addPendingRequest = function() {
  pendingRequests++;
}
var removePendingRequest = function() {
  pendingRequests--;
  if (pendingRequests == 0) {
            console.log("|________________________________________________________________________________________________________________________________________________________________|")
  }
}
var linesDone = 0;

var getSymbolQuery = function(symbol) {
  return 'select * from yahoo.finance.quotes where symbol="{SYMBOL}"'.replace('{SYMBOL}',symbol);
}

var instream = fs.createReadStream("symbols.csv");
var outstream = new stream;

//fs.writeFileSync('symbols_updated.csv', "");

var readResponse = function(response,callback, parent) {
  var str = ''
  response.on('data', function (chunk) {
    str += chunk;
  });

  response.on('end', function () {
    callback(str, parent);
  });
}

var saveLine = function(rawJson, parentLine) {
  try {
    responseData = JSON.parse(rawJson);
    parentLine[3] = parentLine[3].slice(0,1).toUpperCase() + parentLine[3].slice(1).toLowerCase();
    parentLine[5] = responseData.query.results.quote.Currency;
    parentLine[7] = responseData.query.results.quote.FiftydayMovingAverage|| responseData.query.results.quote.LastTradePriceOnly ;
    parentLine[8] = responseData.query.results.quote.YearLow;
    parentLine[9] = responseData.query.results.quote.YearHigh;
    parentLine[10] = responseData.query.results.quote.MarketCapitalization;
    parentLine[11] = responseData.query.results.quote.AverageDailyVolume;
    linesDone++
    if(linesDone%100==0){
        console.log("| "+(linesDone + "      ").substr(0,6) + "| " + parentLine.map(function(d,i) {return (d + Array.apply(" ", {length:20}).map(function(){return " "}).join("")).substr(0, i==1?20:10)}).join('| ') + "|");
    }

    fs.appendFileSync('newFile.csv', parentLine.join(',') + "\n");
  } catch(err) {
    //console.log(err);
  }
}

var getNextPrices = function() {
  var count = 0;
  var readCount = 0;
  var rl = readline.createInterface(instream,outstream);

    rl.on('line', function(line) {
      count++;
      var lineArray = line.split(",");
      if (lineArray.length == 12
          && lineArray[1].length
          && lineArray[2].length
          && (lineArray[3]=='equity' || lineArray[3] == "")
          && readCount<2000
          && !~existingQuotes.indexOf(lineArray[0]))  {
        var options = getBaseOptions();
        var query = getSymbolQuery(lineArray[0]);
        options.path += encodeURI(query);
        //console.log(options.host+options.path);
        var callback = function(res) {
          readResponse(res,saveLine,lineArray);
        };
        readCount++
        var req = http.request(options, callback);
        req.end();
      }
    });

  rl.on('close',function() {
    console.log("Updated {0} out of {1}".replace("{0}",readCount).replace("{1}",count));
  });

}
console.log("__________________________________________________________________________________________________________________________________________________________________")
console.log("| #    | SYMBOL    | NAME                | EXCHANGE  | TYPE      | CATEGORY  | CURRENCY  | UNIT      | PRICE     | LOW       | HIGH      | MARKET CAP| VOLUME    |");
console.log("|________________________________________________________________________________________________________________________________________________________________|")
var startTime = new Date();
var lines = fs.readFileSync('symbols_updated.csv').toString().split("\n");
lines.forEach(function(line) {
  var lineArray = line.split(",");
  var options = getBaseOptions();
  var query = getSymbolQuery(lineArray[0]);
  options.path += encodeURI(query);
  //console.log(options.host+options.path);
  var callback = function(res) {
    readResponse(res,saveLine,lineArray);
    removePendingRequest();
  };
  addPendingRequest();
  var req = http.request(options, callback);
  req.end();
});

