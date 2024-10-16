const addon = require("bindings")("xtracfg-native");

module.exports = {
  execute: addon.execute,
  subscribe: addon.subscribe,
};
