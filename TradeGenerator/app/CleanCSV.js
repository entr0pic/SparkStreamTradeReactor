var fs = require('fs');
var stream = require('stream');
var readline = require('readline');

var instream = fs.createReadStream("symbols.txt");
var outstream = new stream;

//fs.writeFileSync('symbols.csv', "");

var valid = /^[a-zA-Z&\s\-\.\(\)]+$/;

var count = 0;
var parsed = 0;
var rl = readline.createInterface(instream,outstream);
rl.on('line', function(line) {
  count++;
  var lineArray = line.split("\t");
  var parsed = lineArray.every(function(part) {return valid.test(part);});
  if (parsed) {
    parsed++;
    fs.appendFileSync('symbols.csv', lineArray.join(',') + "\n");
  }
});

rl.on('close',function() {
  console.log("Parsed {0} out of {1}".replace("{0}",parsed).replace("{1}",count));
})
