var exec = require('cordova/exec');

// exports.coolMethod = function (arg0, success, error) {
//     exec(success, error, 'HTY711', 'coolMethod', [arg0]);
// };

exports.customInput = function (arg0, success, error) {
    exec(success, error, 'HTY711', 'customInput', [arg0]);
};

exports.init = function (arg0, success, error) {
    exec(success, error, 'HTY711', 'init', [arg0]);
};

exports.readGiftCard = function (arg0, success, error) {
    exec(success, error, 'HTY711', 'readGiftCard', [arg0]);
};

exports.clearDisplay = function (arg0, success, error) {
    exec(success, error, 'HTY711', 'clearDisplay', [arg0]);
};

exports.displayText = function (arg0, success, error) {
    exec(success, error, 'HTY711', 'displayText', [arg0]);
};

exports.isConnecteed = function (arg0, success, error) {
    exec(success, error, 'HTY711', 'isConnecteed', [arg0]);
};