<!---
# license: Licensed to the Apache Software Foundation (ASF) under one
#         or more contributor license agreements.  See the NOTICE file
#         distributed with this work for additional information
#         regarding copyright ownership.  The ASF licenses this file
#         to you under the Apache License, Version 2.0 (the
#         "License"); you may not use this file except in compliance
#         with the License.  You may obtain a copy of the License at
#
#           http://www.apache.org/licenses/LICENSE-2.0
#
#         Unless required by applicable law or agreed to in writing,
#         software distributed under the License is distributed on an
#         "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#         KIND, either express or implied.  See the License for the
#         specific language governing permissions and limitations
#         under the License.
-->

# PluginCordovaMediarecorder

This plugin defines a global `MediaRecorder` object, which provides the capability to record and save video files on the device.
It allows to start/stop recording a video with the device's default camera.
The default folder is set to the device's 'PICTURES' folder.

## Installation

    cordova plugin add https://github.com/sergemazille/PluginCordovaMediarecorder
    
## Supported Platform

- Android only.
Tested on 4.2.2 and 4.4.2 but might depends on device hardware capabilities.

## Usage

    MediaRecorder.switchRecording(action, cameraToBackground, successCallback, errorCallback);
    
where :
* 'action' can be either 'startRecording' or 'stopRecording'
* 'cameraToBackground' is a boolean. Set to *true* it will bring the app's view in front of the camera.
* 'successCallback' refers to a function to call when the device is starting to record or has stopped the recording process.
* 'errorCallback' refers to a function to call when there's something wrong either with the action or the arguments.

## Sample App

You can find a really simple example App here :
https://github.com/sergemazille/PluginCordovaMediarecorder_SampleApp

## Licence MIT
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
