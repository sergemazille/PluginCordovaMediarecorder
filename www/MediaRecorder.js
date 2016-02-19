module.exports = {

    switchRecording: function (action, cameraToBackground, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "MediaRecorderBridge", action, [cameraToBackground]);
    }
};
