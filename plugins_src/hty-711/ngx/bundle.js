'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var tslib = require('tslib');
var core$1 = require('@angular/core');
var core = require('@ionic-native/core');

var HTY711 = /** @class */ (function (_super) {
    tslib.__extends(HTY711, _super);
    function HTY711() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    HTY711.prototype.customInput = function (arg0) { return core.cordova(this, "customInput", {}, arguments); };
    HTY711.prototype.init = function (arg0) { return core.cordova(this, "init", {}, arguments); };
    HTY711.prototype.readGiftCard = function (arg0) { return core.cordova(this, "readGiftCard", {}, arguments); };
    HTY711.prototype.clearDisplay = function (arg0) { return core.cordova(this, "clearDisplay", {}, arguments); };
    HTY711.prototype.displayText = function (arg0) { return core.cordova(this, "displayText", {}, arguments); };
    HTY711.prototype.isConnected = function (arg0) { return core.cordova(this, "isConnected", {}, arguments); };
    HTY711.prototype.startScan = function (arg0) { return core.cordova(this, "startScan", {}, arguments); };
    HTY711.prototype.stopScan = function (arg0) { return core.cordova(this, "stopScan", {}, arguments); };
    HTY711.pluginName = "HTY711";
    HTY711.plugin = "cordova-plugin-hty711";
    HTY711.pluginRef = "window.plugins.HTY711";
    HTY711.repo = "https://github.com/midgi/cordova-plugins-hty711.git";
    HTY711.install = "";
    HTY711.installVariables = [];
    HTY711.platforms = ["Android"];
    HTY711.decorators = [
        { type: core$1.Injectable }
    ];
    return HTY711;
}(core.IonicNativePlugin));

exports.HTY711 = HTY711;
