var http = require('http');
var fs = require('fs');

var getBaseOptions = function(){
  return {
    host: 'query.yahooapis.com',
    path: '/v1/public/yql?q=',
    headers: { 'accept': 'application/json' }
  };
}

var getCountriesYQL = "select * from html where url='http://www.bankswiftcode.org/letter-{LETTER}/' and xpath='//table//table//a'";
var getBankPage = "select * from html where url='http://www.bankswiftcode.org{COUNTRY}' and xpath='//table//tr/td/p/a'";
var getBanksYQL = "select * from html where url='http://www.bankswiftcode.org{COUNTRY}' and xpath='//table//table//table//tr'";

var countryPaths = [];
var bankDetails = [];
var pages = [];


var parseCountries = function(responseString, parent) {
  try {
    var response = JSON.parse(responseString);
    //console.log(response.query.results);
    if (response.query.results.a) {
      response.query.results.a.forEach(function(d) {
        if (d.content == undefined) return;
        countryPaths.push({name:d.content,path:d.href});
        pages.push({country:d.content,path:d.href});
      })
    }
  } catch (error) {
    console.log('Invalid JSON response:' + responseString)
  }
}

var parsePages = function(responseString, parent) {
  try {
    var response = JSON.parse(responseString);
    //console.log(response.query.results);
    if (response.query.results.a) {
      response.query.results.a.forEach(function(d) {
        if (!pages.some(function(p){return p.path == d.href}))
          pages.push({country:parent,path:d.href});
      })
    }
  } catch (error) {
    console.log('Invalid JSON response:' + responseString)
  }
}

var extractContents = function(data) {
  if (typeof data != 'object' || data == null) return data;
  return Object.keys(data).reduce(function(all,current){
    if (current=="content") {
      all += data[current];
    } else if (typeof data[current] == 'object') {
      all += extractContents(data[current]);
    }
    return all;
  },"");
}

var parseBanks = function(responseString, parent) {
  try {
    var response = JSON.parse(responseString);
    //console.log(response.query.results);
    if (response.query.results.tr) {
      response.query.results.tr
                      .filter(function(d){return d.td && !isNaN(d.td[0].replace(".",""))})
                      .map(function(d){return d.td;})
                      .forEach(function(d) {
        if (d.length < 5) return;
        d = d.map(extractContents);
        bankDetails.push({
          name:d[1],
          country: parent,
          city:d[2],
          branch: d[3],
          swift: d[4]
        });
      })
    }
  } catch (error) {
    console.log(error ,responseString)
  }
}

var bankToComma = function(bankLine) {
  return Object.keys(bankLine).map(function(d) {return '"' + bankLine[d] + '"'}).join(',');
}

readResponse = function(response,callback, parent) {
  var str = ''
  response.on('data', function (chunk) {
    str += chunk;
  });

  response.on('end', function () {
    callback(str, parent);
  });
}

var letters = "abcdefghijklmnopqrstuvwxyz".split("");

var nextLetter = function() {
  if (!letters.length) return false;
  var letter = letters.shift();
  var options = getBaseOptions();
  var query = getCountriesYQL.replace("{LETTER}",letter);
  options.path += encodeURI(query);
  console.log(options.path);
  var callback = function(res) {
    if (!nextLetter()){
      console.log(countryPaths);
      console.log(pages);
      nextCountry();
    }
    readResponse(res,parseCountries);
  };
  var req = http.request(options, callback);
  req.end();
  return true;
};

nextLetter();


var nextPage = function() {
  if (!pages.length) return false;
  var page = pages.shift();
  var options = getBaseOptions();
  var query = getBanksYQL.replace("{COUNTRY}", page.path);
  options.path += encodeURI(query);
  console.log(options.path);
  var callback = function(res) {
    if (!nextPage()){
      fs.writeFileSync('banks.json', JSON.stringify(bankDetails,1,'    '));
      fs.writeFileSync('banks.csv', [Object.keys(bankDetails[0]).map(function(d){return '"'+d+'"';}).join(",")].concat(bankDetails.map(bankToComma)).join('\n'));
    }
    readResponse(res,parseBanks,page.country);
  };
  var req = http.request(options, callback);
  req.end();
  return true;
};

var nextCountry = function() {
  if (!countryPaths.length) return false;
  var country = countryPaths.shift();
  var options = getBaseOptions();
  var query = getBankPage.replace("{COUNTRY}",country.path);
  options.path += encodeURI(query);
  console.log(options.path);
  var callback = function(res) {
    if (!nextCountry()){
      console.log(pages);
      nextPage();
    }
    readResponse(res,parsePages,country.name);
  };
  var req = http.request(options, callback);
  req.end();
  return true;
}

