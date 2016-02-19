package com.sergemazille;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MediaRecorderBridge extends CordovaPlugin {

    private Camera camera;
    private TextureView cameraTexturePreview; // preview surface
    private MediaRecorder mediaRecorder;

    private boolean isRecording = false; // flag

    private Context context;
    private Resources resources;
    private String packageName;

    private CallbackContext callbackContext;
    private Boolean cameraToBackground; // will check the option provided by the user

    RelativeLayout mainLayout;
    RelativeLayout.LayoutParams mainLayoutParams;
    View mainView;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        // Helpers
        context = cordova.getActivity().getApplicationContext();
        resources = cordova.getActivity().getResources();
        packageName = cordova.getActivity().getPackageName();
    }

    public boolean execute(String action, JSONArray args, final CallbackContext cBContext) throws JSONException {

        callbackContext = cBContext;

        if (action.equals("startRecording")) {

            cameraToBackground = args.getBoolean(0);

            // container for inflated camera_layout.xml view
            mainLayout = new RelativeLayout(context);
            mainLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

            // inflate camera_layout.xml and give it to mainLayout container
            mainView = cordova.getActivity().getLayoutInflater().inflate(resources.getIdentifier("camera_layout", "layout", packageName), mainLayout, false);

            // add the newly created view to WebView. We're dealing with the app's view so we have to run this operation on UI Thread
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    cordova.getActivity().addContentView(mainView, mainLayoutParams);

                    // just put the app's view on the foreground if cameraToBackground is set to true
                    if (cameraToBackground) {
                        webView.getView().setBackgroundColor(0x00000000); // transparent background
                        webView.getView().bringToFront();
                    }
                }
            });


            // helps older devices to keep up...
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // reference to camera preview surface, needed for the mediaRecorder object
            cameraTexturePreview = (TextureView) cordova.getActivity().findViewById(resources.getIdentifier("cameraTexturePreview", "id", packageName));

            // prepares the camera and the media recorder. It's a long process... so it needs to be run on an asynchronous task (so it won't crash)
            new MediaPrepareTask().execute(null, null, null);
            return true;

        } else if (action.equals("stopRecording")) {

            return stopRecording();

        } else {

            callbackContext.error("This action does not exist...");
            return false;
        }
    }

    public boolean stopRecording() {
        // doesn't run if not recording
        if (isRecording) {

            // stop recording and free camera for other apps
            mediaRecorder.stop(); // stop mediaRecorder before freeing it
            releaseMediaRecorder();
            releaseCamera();

            // set views' original states (app's view so run on UI Thread)
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {

                    // set app's view from transparent to white
                    webView.getView().setBackgroundColor(0xFFFFFFFF);

                    // remove the plugin's container view
                    mainLayout.removeView(mainView);
                }
            });

            isRecording = false;
            callbackContext.success(); // We need to tell the app so it can switch the recording button for the next click
            displayToast("Recording stopped...");
            return true;
        }
        return false;
    }

    // setup camera and mediaRecorder (launched from the asynchronous task)
    public boolean prepareMediaRecorder() {

        // 1. setup camera
        camera = getCameraInstance();

        Camera.Parameters cameraParameters = camera.getParameters();
        List<Camera.Size> supportedPreviewSizes = cameraParameters.getSupportedPreviewSizes();

        int width = cameraTexturePreview.getWidth();
        int height = cameraTexturePreview.getHeight();

        Camera.Size optimalPreviewSizes = getOptimalPreviewSize(supportedPreviewSizes, width, height);

        camera.setDisplayOrientation(90); // change only the preview display orientation, not the recorded video file

        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH); // can be changed to QUALITY_HIGH, QUALITY_LOW, QUALITY_1080P, QUALITY_480P...

        profile.videoFrameWidth = optimalPreviewSizes.width;
        profile.videoFrameHeight = optimalPreviewSizes.height;

        cameraParameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
        camera.setParameters(cameraParameters);

        try {
            camera.setPreviewTexture(cameraTexturePreview.getSurfaceTexture());
        } catch (IOException e) {
            callbackContext.error("SetPreviewDisplay..." + e.getMessage());
        }

        // 2. setup mediaRecorder
        mediaRecorder = new MediaRecorder();

        mediaRecorder.setOrientationHint(90); // change the recorded video file orientation

        // the order of those operations is important...
        camera.unlock();
        mediaRecorder.setCamera(camera);

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mediaRecorder.setProfile(profile);

        // get access to the PICTURES folder of the device (can be changed in the body of this method)
        mediaRecorder.setOutputFile(getOutputMediaFile().toString()); // toString because we need a path, not an actual file

        // let's see if everything went right
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            return false; // not good...
        }

        return true; // good...
    }

    /* *** Life cycle events *** */
    // camera needs to be freed for other apps
    @Override
    public void onStop() {
        super.onStop();

        releaseMediaRecorder();
        releaseCamera();
    }

    /* *** Helpers *** */

    // release mediaRecorder...
    public void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            camera.lock();
        }
    }

    // release camera...
    public void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    // Used to alert the user when camera is recording or stopped
    public void displayToast(String message) {

        Context context = cordova.getActivity().getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }

    public Camera getCameraInstance() {
        Camera cam = null;
        try {
            cam = Camera.open();
        } catch (Exception e) {
            // do something for debugging
        }
        return cam;
    }

    // thanks to Google's camera helper code (http://developer.android.com/samples/BasicMediaDecoder/src/com.example.android.common.media/CameraHelper.html)
    public Camera.Size getOptimalPreviewSize(List<Camera.Size> supportedPreviewSizes, int surfacePreviewWidth, int surfacePreviewHeight) {

        // Use a very small tolerance because we want an exact match.
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) surfacePreviewWidth / surfacePreviewHeight;
        if (supportedPreviewSizes == null)
            return null;

        Camera.Size optimalSize = null;

        // Start with max value and refine as we iterate over available preview sizes. This is the
        // minimum difference between view and camera height.
        double minDiff = Double.MAX_VALUE;

        // Target view height
        int targetHeight = surfacePreviewHeight;

        // Try to find a preview size that matches aspect ratio and the target view size.
        // Iterate over all available sizes and pick the largest size that can fit in the view and
        // still maintain the aspect ratio.
        for (Camera.Size size : supportedPreviewSizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find preview size that matches the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : supportedPreviewSizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    // slight variation from Google's sample (http://developer.android.com/guide/topics/media/camera.html#saving-media)
    public static File getOutputMediaFile() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        if (!Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return null;
        }

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MediaRecorder");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("CameraSample", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");

        return mediaFile;
    }

    // asynchronous task for camera and mediaRecorder setups
    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            if (prepareMediaRecorder()) { // run only if the setup phase went right
                mediaRecorder.start();
                isRecording = true;
                callbackContext.success(); // We need to tell the app so it can switch the recording button for the next click

            } else {
                // too bad, something went wrong... We need to stop everything to avoid unstable behavior of the camera
                releaseMediaRecorder();
                releaseCamera();
                return false; // not good...
            }

            return true; // good...
        }

        // when the task is over
        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            // everything went right :
            if (result) {
                displayToast("Recording...");
            } else {
                displayToast("Something went wrong...");
            }
        }
    }
}