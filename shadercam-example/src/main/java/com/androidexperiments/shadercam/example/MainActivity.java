package com.androidexperiments.shadercam.example;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.androidexperiments.shadercam.example.gl.ExampleRenderer;
import com.androidexperiments.shadercam.fragments.CameraFragment;
import com.androidexperiments.shadercam.gl.CameraRenderer;
import com.androidexperiments.shadercam.utils.ShaderUtils;
import com.google.labs.androidexperiments.shadercamera.example.R;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Written by Anthony Tripaldi
 *
 * Very basic implemention of shader camera.
 */
public class MainActivity extends Activity implements CameraRenderer.OnRendererReadyListener
{
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String TAG_CAMERA_FRAGMENT = "tag_camera_frag";

    /**
     * filename for our test video output
     */
    private static final String TEST_VIDEO_FILE_NAME = "test_video.mp4";

    /**
     * We inject our views from our layout xml here using {@link ButterKnife}
     */
    @InjectView(R.id.texture_view) TextureView mTextureView;
    @InjectView(R.id.btn_record) Button mRecordBtn;

    /**
     * Custom fragment used for encapsulating all the {@link android.hardware.camera2} apis.
     */
    private CameraFragment mCameraFragment;

    /**
     * Our custom renderer for this example, which extends {@link CameraRenderer} and then adds custom
     * shaders, which turns shit green, which is easy.
     */
    private ExampleRenderer mRenderer;

    /**
     * boolean for triggering restart of camera after completed rendering
     */
    private boolean mRestartCamera = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        setCameraFragment();
        setupInteraction();
    }

    /**
     * create the camera fragment responsible for handling camera state and add it to our activity
     */
    private void setCameraFragment()
    {
        mCameraFragment = CameraFragment.getInstance();
        mCameraFragment.setCameraToUse(CameraFragment.CAMERA_PRIMARY); //pick which camera u want to use, we default to forward
        mCameraFragment.setTextureView(mTextureView);

        //add fragment to our setup and let it work its magic
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.add(mCameraFragment, TAG_CAMERA_FRAGMENT);
        transaction.commit();
    }

    /**
     * add a listener for touch on our surface view that will pass raw values to our renderer for
     * use in our shader to control color channels.
     */
    private void setupInteraction() {
        mTextureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mRenderer.setTouchPoint(event.getRawX(), event.getRawY());
                return true;
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        ShaderUtils.goFullscreen(this.getWindow());

        if(!mTextureView.isAvailable())
            mTextureView.setSurfaceTextureListener(mTextureListener); //set listener to handle when its ready
        else
            setReady(mTextureView.getSurfaceTexture(), mTextureView.getWidth(), mTextureView.getHeight());
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        shutdownCamera(false);

        mTextureView.setSurfaceTextureListener(null);
    }

    /**
     * {@link ButterKnife} uses annotations to make setting {@link android.view.View.OnClickListener}'s
     * easier than ever with the {@link OnClick} annotation.
     */
    @OnClick(R.id.btn_record)
    public void onClickRecord()
    {
        if(mRenderer.isRecording())
            stopRecording();
        else
            startRecording();
    }

    @OnClick(R.id.btn_swap_camera)
    public void onClickSwapCamera()
    {
        mCameraFragment.swapCamera();
    }

    /**
     * called whenever surface texture becomes initially available or whenever a camera restarts after
     * completed recording or resuming from onpause
     * @param surface {@link SurfaceTexture} that we'll be drawing into
     * @param width width of the surface texture
     * @param height height of the surface texture
     */
    private void setReady(SurfaceTexture surface, int width, int height) {
        mRenderer = new ExampleRenderer(this, surface, mCameraFragment, width, height);
        mRenderer.setOnRendererReadyListener(this);
        mRenderer.start();

        //initial config if needed
        mCameraFragment.configureTransform(width, height);
    }

    private void startRecording()
    {
        mRenderer.startRecording(getVideoFile());
        mRecordBtn.setText("Stop");
    }

    private void stopRecording()
    {
        mRenderer.stopRecording();
        mRecordBtn.setText("Record");

        //restart so surface is recreated
        shutdownCamera(true);

        Toast.makeText(this, "File recording complete: " + getVideoFile().getAbsolutePath(), Toast.LENGTH_LONG).show();
    }

    private File getVideoFile()
    {
        return new File(Environment.getExternalStorageDirectory(), TEST_VIDEO_FILE_NAME);
    }

    /**
     * kills the camera in camera fragment and shutsdown render thread
     * @param restart whether or not to restart the camera after shutdown is complete
     */
    private void shutdownCamera(boolean restart)
    {
        mCameraFragment.closeCamera();

        mRestartCamera = restart;
        mRenderer.getRenderHandler().sendShutdown();
        mRenderer = null;
    }

    /**
     * Interface overrides from our {@link com.androidexperiments.shadercam.gl.CameraRenderer.OnRendererReadyListener}
     * interface. Since these are being called from inside the CameraRenderer thread, we need to make sure
     * that we call our methods from the {@link #runOnUiThread(Runnable)} method, so that we don't
     * throw any exceptions about touching the UI from non-UI threads.
     *
     * Another way to handle this would be to create a Handler/Message system similar to how our
     * {@link com.androidexperiments.shadercam.gl.CameraRenderer.RenderHandler} works.
     */
    @Override
    public void onRendererReady() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCameraFragment.setPreviewTexture(mRenderer.getPreviewTexture());
                mCameraFragment.openCamera();
            }
        });
    }

    @Override
    public void onRendererFinished() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mRestartCamera) {
                    setReady(mTextureView.getSurfaceTexture(), mTextureView.getWidth(), mTextureView.getHeight());
                    mRestartCamera = false;
                }
            }
        });
    }


    /**
     * {@link android.view.TextureView.SurfaceTextureListener} responsible for setting up the rest of the
     * rendering and recording elements once our TextureView is good to go.
     */
    private TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener()
        {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, final int width, final int height) {
                //convenience method since we're calling it from two places
                setReady(surface, width, height);
            }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            mCameraFragment.configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) { }
    };

}
