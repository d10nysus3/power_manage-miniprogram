#!/usr/bin/env node

// 修补 os.networkInterfaces 以避免错误
const Module = require('module');
const originalRequire = Module.prototype.require;

Module.prototype.require = function(id) {
  const module = originalRequire.apply(this, arguments);
  
  if (id === 'os' || id === 'node:os') {
    const originalNetworkInterfaces = module.networkInterfaces;
    module.networkInterfaces = function() {
      try {
        return originalNetworkInterfaces();
      } catch (error) {
        console.warn('Warning: networkInterfaces failed, returning empty object');
        return {};
      }
    };
  }
  
  return module;
};

// 运行 uni-app CLI
require('@dcloudio/vite-plugin-uni/dist/cli/index.js');

