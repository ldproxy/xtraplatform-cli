const xtracfgLib = require("../index.js");
const currentDir = __dirname;

function testBasic() {
  console.log("Testing basic execute...", currentDir);
  const command = `{"command": "info", "source": "${currentDir.replaceAll(
    "\\",
    "/"
  )}", "verbose": "true", "debug": "true"}`;

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
