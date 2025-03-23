const xtracfgLib = require("../index.js");

function testBasic() {
  const command =
    '{"command": "info", "source": "/Users/az/development/configs-ldproxy/demogh", "verbose": "true", "debug": "true"}';

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
