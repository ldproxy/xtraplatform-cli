const xtracfgLib = require("../src/index.js");

function testBasic() {
  const command =
    '{"command": "info", "source": "/Users/pascal/Documents/GitHub/demo"}';

  const result = xtracfgLib.execute(command);

  console.log("Result:", result);
}

function testSubscribe() {
  if (typeof xtracfgLib.subscribe === "function") {
    xtracfgLib.subscribe(console.log); // Übergabe von console.log als Callback
  } else {
    console.error("subscribe is not a function");
  }
}

testSubscribe();
testBasic();
