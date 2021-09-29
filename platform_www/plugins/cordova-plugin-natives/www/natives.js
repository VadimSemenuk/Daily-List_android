cordova.define("cordova-plugin-natives.natives", function(require, exports, module) {
var Natives = {};

Natives.updateWidget = function(success, error) {
    cordova.exec(success, error, 'Natives', 'updateWidget');
};

Natives.updateWidgetList = function(success, error) {
    cordova.exec(success, error, 'Natives', 'updateWidgetList');
};

Natives.scheduleDayChangeNotification = function() {
    cordova.exec(null, null, 'Natives', 'scheduleDayChangeNotification');
}

Natives.events = {};

Natives.addEventListener = function(event, callback) {
    if (!Natives.events[event]) {
        Natives.events[event] = {
            listeners: []
        };
    }

    Natives.events[event].listeners.push(callback);
    cordova.exec(null, null, 'Natives', 'eventAdded', [event]);
};

Natives.removeEventListener = function(event, callback) {
    var nextListeners = [];

    for (var i = 0; i < Natives.events[event].listeners.length; i++) {
        var eventListener = Natives.events[event].listeners[i];
        if (eventListener != callback) {
            nextListeners.push(eventListener);
        }
    }

    Natives.events[event].listeners = nextListeners;
};

Natives.removeEvents = function() {
    Natives.events = {};
};

Natives.fireEvent = function(event, props) {
    for (var i = 0; i < Natives.events[event].listeners.length; i++) {
        Natives.events[event].listeners[i](props);
    }
};

module.exports = Natives;

});
