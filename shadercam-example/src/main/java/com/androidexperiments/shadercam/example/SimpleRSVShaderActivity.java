package com.androidexperiments.shadercam.example;

import com.androidexperiments.shadercam.example.gl.ExampleVideoRenderer;
import com.androidexperiments.shadercam.fragments.CameraFragment;
import com.androidexperiments.shadercam.fragments.PermissionsHelper;
import com.androidexperiments.shadercam.fragments.VideoFragment;
import com.androidexperiments.shadercam.gl.CameraRenderer;
import com.androidexperiments.shadercam.gl.VideoRenderer;
import com.androidexperiments.shadercam.utils.ShaderUtils;
import com.uncorkedstudios.android.view.recordablesurfaceview.RecordableSurfaceView;

import android.Manifest;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Written by Anthony Tripaldi - modified by someone else
 *
 * Very basic implemention of shader camera.
 */
public class SimpleRSVShaderActivity extends FragmentActivity implements PermissionsHelper.PermissionsListener {

    private static final String TAG = SimpleRSVShaderActivity.class.getSimpleName();

    private static final String TAG_CAMERA_FRAGMENT = "tag_camera_frag";

    /**
     * filename for our test video output
     */
    private static final String TEST_VIDEO_FILE_NAME = "test_video.mp4";

    /**
     * We inject our views from our layout xml here using {@link ButterKnife}
     */
    @Bind(R.id.surface_view)
    RecordableSurfaceView mRecordableSurfaceView;

    @Bind(R.id.btn_record)
    Button mRecordBtn;

    /**
     * Custom fragment used for encapsulating all the {@link android.hardware.camera2} apis.
     */
    private VideoFragment mVideoFragment;

    /**
     * Our custom renderer for this example, which extends {@link CameraRenderer} and then adds custom
     * shaders, which turns shit green, which is easy.
     */
    protected VideoRenderer mVideoRenderer;

    /**
     * boolean for triggering restart of camera after completed rendering
     */
    private boolean mRestartCamera = false;

    private PermissionsHelper mPermissionsHelper;

    private boolean mPermissionsSatisfied = false;

    private File mOutputFile;

    private boolean mIsRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rsv);

        ButterKnife.bind(this);
        setupVideoFragment();
        setupInteraction();

        //setup permissions for M or start normally
        if (PermissionsHelper.isMorHigher()) {
            setupPermissions();
        }

    }


    /**
     * create the camera fragment responsible for handling camera state and add it to our activity
     */
    private void setupVideoFragment() {
        if (mVideoFragment != null && mVideoFragment.isAdded()) {
            return;
        }

        mVideoFragment = VideoFragment.getInstance();
        mVideoFragment.setCameraToUse(
                CameraFragment.CAMERA_PRIMARY); //pick which camera u want to use, we default to forward

        //add fragment to our setup and let it work its magic
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(mVideoFragment, TAG_CAMERA_FRAGMENT);
        transaction.commit();
        mVideoFragment.setRecordableSurfaceView(mRecordableSurfaceView);
        mRecordableSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

    }

    private void setupPermissions() {
        mPermissionsHelper = PermissionsHelper.attach(this);
        mPermissionsHelper.setRequestedPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE

        );
    }


    /**
     * add a listener for touch on our surface view that will pass raw values to our renderer for
     * use in our shader to control color channels.
     */
    private void setupInteraction() {
        mRecordableSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mVideoFragment.getVideoRenderer() instanceof ExampleVideoRenderer) {
                    ((ExampleVideoRenderer) mVideoFragment.getVideoRenderer())
                            .setTouchPoint(event.getRawX(), event.getRawY());
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Things are good to go and we can continue on as normal. If this is called after a user
     * sees a dialog, then onResume will be called next, allowing the app to continue as normal.
     */
    @Override
    public void onPermissionsSatisfied() {
        Log.d(TAG, "onPermissionsSatisfied()");
        mPermissionsSatisfied = true;
        mRecordableSurfaceView.resume();
        try {
            mOutputFile = getVideoFile();
            android.graphics.Point size = new android.graphics.Point();
            getWindowManager().getDefaultDisplay().getRealSize(size);
            mRecordableSurfaceView.initRecorder(mOutputFile, size.x, size.y, null, null);
        } catch (IOException ioex) {
            Log.e(TAG, "Couldn't re-init recording", ioex);
        }
    }

    /**
     * User did not grant the permissions needed for out app, so we show a quick toast and kill the
     * activity before it can continue onward.
     *
     * @param failedPermissions string array of which permissions were denied
     */
    @Override
    public void onPermissionsFailed(String[] failedPermissions) {
        Log.e(TAG, "onPermissionsFailed()" + Arrays.toString(failedPermissions));
        mPermissionsSatisfied = false;
        Toast.makeText(this, "shadercam needs all permissions to function, please try again.",
                Toast.LENGTH_LONG).show();
        this.finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume()");

        ShaderUtils.goFullscreen(this.getWindow());

        /**
         * if we're on M and not satisfied, check for permissions needed
         * {@link PermissionsHelper#checkPermissions()} will also instantly return true if we've
         * checked prior and we have all the correct permissions, allowing us to continue, but if its
         * false, we want to {@code return} here so that the popup will trigger without {@link #setReady(SurfaceTexture, int, int)}
         * being called prematurely
         */
        //
        if (PermissionsHelper.isMorHigher() && !mPermissionsSatisfied) {
            if (!mPermissionsHelper.checkPermissions()) {
                return;
            } else {
                mPermissionsSatisfied
                        = true; //extra helper as callback sometimes isnt quick enough for future results
                mOutputFile = getVideoFile();
                android.graphics.Point size = new android.graphics.Point();
                getWindowManager().getDefaultDisplay().getRealSize(size);
                try {
                    mRecordableSurfaceView.initRecorder(mOutputFile, size.x, size.y, null, null);
                } catch (IOException ioex) {
                    Log.e(TAG, "Couldn't re-init recording", ioex);
                }
                mVideoFragment.setVideoRenderer(mVideoRenderer);
            }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        shutdownCamera(false);
        mRecordableSurfaceView.pause();
    }

    /**
     * {@link ButterKnife} uses annotations to make setting {@link android.view.View.OnClickListener}'s
     * easier than ever with the {@link OnClick} annotation.
     */
    @OnClick(R.id.btn_record)
    public void onClickRecord() {
        if (mIsRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    @OnClick(R.id.btn_swap_camera)
    public void onClickSwapCamera() {
        mVideoFragment.swapCamera();
    }


    private void startRecording() {

        mRecordableSurfaceView.startRecording();
        mIsRecording = true;
        mRecordBtn.setText("Stop");

    }

    private void stopRecording() {
        mRecordBtn.setText("Record");
        Log.e(TAG, "Stopping");

        mRecordableSurfaceView.stopRecording();
        try {
            mOutputFile = getVideoFile();
            android.graphics.Point size = new android.graphics.Point();
            getWindowManager().getDefaultDisplay().getRealSize(size);
            mRecordableSurfaceView.initRecorder(mOutputFile, size.x, size.y, null, null);
        } catch (IOException ioex) {
            Log.e(TAG, "Couldn't re-init recording", ioex);
        }

        mIsRecording = false;
        Toast.makeText(this, "File recording complete: " + getVideoFile().getAbsolutePath(),
                Toast.LENGTH_LONG).show();
    }

    private File getVideoFile() {
        return new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis() + "_" + TEST_VIDEO_FILE_NAME);
    }

    /**
     * kills the camera in camera fragment and shutsdown render thread
     *
     * @param restart whether or not to restart the camera after shutdown is complete
     */
    private void shutdownCamera(boolean restart) {
        //make sure we're here in a working state with proper permissions when we kill the camera
        if (PermissionsHelper.isMorHigher() && !mPermissionsSatisfied) {
            return;
        }

        //check to make sure we've even created the cam and renderer yet
//        if (mVideoFragment == null || mRenderer == null) {
//            return;
//        }

        mVideoFragment.closeCamera();

        mRestartCamera = restart;
//        mRenderer = null;
    }

//    /**
//     * Interface overrides from our {@link com.androidexperiments.shadercam.gl.VideoRenderer.OnRendererReadyListener}
//     * interface. Since these are being called from inside the CameraRenderer thread, we need to make sure
//     * that we call our methods from the {@link #runOnUiThread(Runnable)} method, so that we don't
//     * throw any exceptions about touching the UI from non-UI threads.
//     *
//     * Another way to handle this would be to create a Handler/Message system similar to how our
//     * {@link com.androidexperiments.shadercam.gl.VideoRenderer.RenderHandler} works.
//     */
//    @Override
//    public void onRendererReady() {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                mVideoFragment.setPreviewTexture(mRenderer.getCameraTexture());
//                mVideoFragment.openCamera();
//            }
//        });
//    }
//
//    @Override
//    public void onRendererFinished() {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                if (mRestartCamera) {
//
//                    mRestartCamera = false;
//                }
//            }
//        });
//    }


}
