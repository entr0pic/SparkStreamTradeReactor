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

var existingQuotes = [];

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
    parentLine[3] = 'Currency';
    parentLine[5] = responseData.query.results.quote.Currency;
    parentLine[6] = responseData.query.results.quote.Currency;
    parentLine[7] = responseData.query.results.quote.FiftydayMovingAverage || responseData.query.results.quote.LastTradePriceOnly ;
    parentLine[8] = responseData.query.results.quote.YearLow;
    parentLine[9] = responseData.query.results.quote.YearHigh;
    parentLine[10] = responseData.query.results.quote.MarketCapitalization;
    parentLine[11] = responseData.query.results.quote.AverageDailyVolume;
    console.log(parentLine.map(function(d,i) {return (d + Array.apply(" ", {length:20}).map(function(){return " "}).join("")).substr(0, i==1?20:10)}).join('| '));
    fs.appendFileSync('symbols_updated.csv', parentLine.join(',') + "\n");
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
          && (lineArray[3]=='currency')
          && readCount<8000
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

var getExistingQuotes = function() {
  var newIn = fs.createReadStream("symbols_updated.csv");
  var out = new stream;
  var updatedFile = readline.createInterface(newIn,out);
  var existingSymbolCount = 0;
  updatedFile.on('line', function(line) {
    existingSymbolCount++;
    existingQuotes.push(line.split(",")[0]);
  });

  updatedFile.on('close',function() {
    console.log("There are {0} existing symbols".replace("{0}",existingSymbolCount));
    getNextPrices();
  });
}


getExistingQuotes();
