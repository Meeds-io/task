const path = require('path');

const config = {
  context: path.resolve(__dirname, '.'),
  // set the entry point of the application
  // can use multiple entry
  entry: {
    tasks: './src/main/webapp/vue-app/tasks/main.js',
    taskSearch: './src/main/webapp/vue-app/taskSearch/main.js',
    tasksManagement: './src/main/webapp/vue-app/tasks-management/main.js'
  },
  output: {
    filename: 'js/[name].bundle.js',
    libraryTarget: 'amd'
  },
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: [
          'babel-loader',
          'eslint-loader'
        ]
      },
      {
        test: /\.vue$/,
        use: [
          'vue-loader',
          'eslint-loader'
        ]
      }
    ]
  },
  externals: {
    vue: 'Vue',
    vuetify: 'Vuetify',
    jquery: '$',
  }
};

module.exports = config;