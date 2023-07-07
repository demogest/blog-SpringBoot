module.exports = {
  productionSourceMap: false,
  devServer: {
    port: 8002,
    proxy: {
      "/api": {
        target: "http://localhost:8888",
        changeOrigin: true,
        pathRewrite: {
          "^/api": ""
        }
      }
    },
    disableHostCheck: true
  }
};

const path = require("path");
function resolve(dir) {
  return path.join(__dirname, dir);
}
