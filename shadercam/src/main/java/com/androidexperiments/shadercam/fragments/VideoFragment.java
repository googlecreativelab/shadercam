package com.androidexperiments.shadercam.fragments;

import com.androidexperiments.shadercam.gl.VideoRenderer;
import com.uncorkedstudios.android.view.recordablesurfaceview.RecordableSurfaceView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Fragment for operating the camera, it doesnt have any UI elements, just controllers
 */
public class VideoFragment extends Fragment implements VideoRenderer.OnRendererReadyListener {

    private static final String TAG = "VideoFragment";

    private static VideoFragment __instance;

    /**
     * A {@link RecordableSurfaceView} for camera preview.
     */
    private RecordableSurfaceView mRecordableSurfaceView;

    private VideoRenderer mVideoRenderer;

    /**
     * A refernce to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * A reference to the current {@link CameraCaptureSession} for preview.
     */
    private CameraCaptureSession mPreviewSession;

    /**
     * Surface to render preview of camera
     */
    private int mPreviewTexture;

    /**
     * The {@link Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * The {@link Size} of video preview/recording.
     */
    private Size mVideoSize;

    /**
     * Camera preview.
     */
    private CaptureRequest.Builder mPreviewBuilder;


    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    private SurfaceTexture mSurfaceTexture;

    /**
     * Use these for changing which camera to use on start
     */
    public static final int CAMERA_PRIMARY = 0;

    /**
     * The id of what is typically the forward facing camera.
     * If this fails, use {@link #CAMERA_PRIMARY}, as it might be the only camera registered.
     */
    public static final int CAMERA_FORWARD = 1;

    /**
     * Default Camera to use
     */
    protected int mCameraToUse = CAMERA_PRIMARY;


    /**
     * Listener for when openCamera is called and a proper video size is created
     */
    private VideoFragment.OnViewportSizeUpdatedListener mOnViewportSizeUpdatedListener;

    private float mVideoSizeAspectRatio;

    private float mPreviewSurfaceAspectRatio;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private boolean mCameraIsOpen = false;

    /**
     * Get instance of this fragment that sets retain instance true so it is not affected
     * by device orientation changes and other updates
     *
     * @return instance of CameraFragment
     */
    public static VideoFragment getInstance() {
        if (__instance == null) {
            __instance = new VideoFragment();
            __instance.setRetainInstance(true);
        }
        return __instance;
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
    }

    @Override
    public void onPause() {
        super.onPause();
        closeCamera();
        stopBackgroundThread();
        mRecordableSurfaceView.setRendererCallbacks(null);
    }

    public void setVideoRenderer(VideoRenderer videoRenderer) {
        mVideoRenderer = videoRenderer;
        if (mVideoRenderer == null) {
            return;
        }
        mVideoRenderer.setVideoFragment(VideoFragment.getInstance());
        mVideoRenderer.setOnRendererReadyListener(this);
        mRecordableSurfaceView.setRendererCallbacks(mVideoRenderer);
        mVideoRenderer.onSurfaceChanged(mRecordableSurfaceView.getWidth(),
                mRecordableSurfaceView.getHeight());
    }


    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        Log.e(TAG, "RELEASE TEXTURE");
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
            mSurfaces.clear();
        }
    }

    /**
     * Switch between the back(primary) camera and the front(selfie) camera
     */
    public void swapCamera() {
        closeCamera();

        if (mCameraToUse == CAMERA_FORWARD) {
            mCameraToUse = CAMERA_PRIMARY;
        } else {
            mCameraToUse = CAMERA_FORWARD;
        }

        openCamera();
    }

    /**
     * Tries to open a CameraDevice. The result is listened by `mStateCallback`.
     */
    public void openCamera() {
        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }

        //sometimes openCamera gets called multiple times, so lets not get stuck in our semaphore lock
        if (mCameraDevice != null && mCameraIsOpen) {
            return;
        }

        final CameraManager manager = (CameraManager) activity
                .getSystemService(Context.CAMERA_SERVICE);

        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            String[] cameraList = manager.getCameraIdList();

            //make sure we dont get array out of bounds error, default to primary [0] if thats the case
            if (mCameraToUse >= cameraList.length) {
                mCameraToUse = CAMERA_PRIMARY;
            }

            String cameraId = cameraList[mCameraToUse];

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap streamConfigurationMap = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            mVideoSize = getOptimalPreviewSize(
                    streamConfigurationMap.getOutputSizes(MediaRecorder.class),
                    mRecordableSurfaceView.getWidth(), mRecordableSurfaceView.getHeight());
            mPreviewSize = getOptimalPreviewSize(
                    streamConfigurationMap.getOutputSizes(SurfaceTexture.class),
                    mRecordableSurfaceView.getWidth(), mRecordableSurfaceView.getHeight());

            //send back for updates to renderer if needed
            updateViewportSize(mVideoSizeAspectRatio, mPreviewSurfaceAspectRatio);

            manager.openCamera(cameraId, mStateCallback, null);
        } catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            activity.finish();
        } catch (NullPointerException e) {
            e.printStackTrace();
            // Currently an NPE is thrown when the Camera2API is used but not supported on the device this code runs.
            new CameraFragment.ErrorDialog().show(getFragmentManager(), "dialog");
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }

    /**
     * Callback from our {@link com.androidexperiments.shadercam.fragments.CameraFragment.OnViewportSizeUpdatedListener CameraFragment.OnViewportSizeUpdatedListener}
     * which is called every time we open the camera, to make sure we are using the most up-to-date values for calculating our
     * renderer's glViewport. Without this, TextureView's that aren't exactly the same size as the size of Camera api video
     * will become distorted.
     *
     * @param videoAspect   float of the aspect ratio of the size of video returned in openCamera
     * @param surfaceAspect aspect ratio of our available textureview surface
     */
    public void updateViewportSize(float videoAspect, float surfaceAspect) {
        int sw = mRecordableSurfaceView.getWidth();
        int sh = mRecordableSurfaceView.getHeight();

        int vpW, vpH;

        if (videoAspect == surfaceAspect) {
            vpW = sw;
            vpH = sh;
        } else if (videoAspect < surfaceAspect) {
            float ratio = (float) sw / mVideoSize.getHeight();
            vpW = (int) (mVideoSize.getHeight() * ratio);
            vpH = (int) (mVideoSize.getWidth() * ratio);
        } else {
            float ratio = (float) sw / mVideoSize.getWidth();
            vpW = (int) (mVideoSize.getWidth() * ratio);
            vpH = (int) (mVideoSize.getHeight() * ratio);
        }

        if (mOnViewportSizeUpdatedListener != null) {
            mOnViewportSizeUpdatedListener.onViewportSizeUpdated(vpW, vpH);
        }

    }

    private CameraCaptureSession.StateCallback mCaptureSessionStateCallback
            = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            mPreviewSession = cameraCaptureSession;
            Log.e(TAG, "CaptureSession Configured: " + cameraCaptureSession);
            updatePreview();
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
            Activity activity = getActivity();
            Log.e(TAG, "config failed: " + cameraCaptureSession);
            if (null != activity) {
                Toast.makeText(activity, "CaptureSession Config Failed", Toast.LENGTH_SHORT)
                        .show();
            }
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            super.onClosed(session);
            Log.e(TAG, "onClosed: " + session);

        }
    };

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its status.
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            mCameraIsOpen = true;

            startPreview();

        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            mCameraIsOpen = false;
            Log.e(TAG, "DISCONNECTED FROM CAMERA");

        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            mCameraIsOpen = false;

            Log.e(TAG, "CameraDevice.StateCallback onError() " + error);

            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }
    };


    /**
     * close camera when not in use/pausing/leaving
     */
    public void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCameraDevice) {
                mPreviewSession.stopRepeating();
                mCameraDevice.close();
                mCameraDevice = null;
                mCameraIsOpen = false;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } catch (CameraAccessException acex) {
            Log.e(TAG, "Failed to stop repeaing preview request", acex);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private List<Surface> mSurfaces;

    /**
     * Start the camera preview.
     */
    private void startPreview() {

        if (null == mCameraDevice || null == mPreviewSize) {
            return;
        }
        try {

            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            if (mSurfaces == null) {
                mSurfaces = new ArrayList<>();
            }

            if (mPreviewTexture == -1) {
                mPreviewTexture = mVideoRenderer.getCameraTexture();
            }
            assert mPreviewTexture != -1;

            mSurfaceTexture = new SurfaceTexture(mPreviewTexture);
            mVideoRenderer.setSurfaceTexture(mSurfaceTexture);
            if (mSurfaces.size() != 0) {
                for (Surface sf : mSurfaces) {
                    sf.release();
                }
                mSurfaces.clear();
            }

            Surface previewSurface = new Surface(mSurfaceTexture);
            mSurfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);
            mSurfaceTexture.setDefaultBufferSize(mRecordableSurfaceView.getHeight(),
                    mRecordableSurfaceView.getWidth());

            mCameraDevice.createCaptureSession(mSurfaces, mCaptureSessionStateCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);

            mSurfaceTexture.setOnFrameAvailableListener(mVideoRenderer);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }
    };


    private Size getOptimalPreviewSize(Size[] sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null) {
            return null;
        }

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Size size : sizes) {
            double ratio = (double) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }
            if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.getHeight() - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getHeight() - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    /**
     * set the RecordableSurfaceView to render the camera preview inside
     */
    public void setRecordableSurfaceView(RecordableSurfaceView rsv) {
        mRecordableSurfaceView = rsv;
    }

    /**
     * Get the current video size used for recording
     *
     * @return {@link Size} of current video from camera.
     */
    public Size getVideoSize() {
        return mVideoSize;
    }

    /**
     * Get the current camera type. Either {@link #CAMERA_FORWARD} or {@link #CAMERA_PRIMARY}
     *
     * @return current camera type
     */
    public int getCurrentCameraType() {
        return mCameraToUse;
    }

    /**
     * Set which camera to use, defaults to {@link #CAMERA_PRIMARY}.
     *
     * @param camera_id can also be {@link #CAMERA_FORWARD} for forward facing, but use primary if that fails.
     */
    public void setCameraToUse(int camera_id) {
        mCameraToUse = camera_id;
    }

    /**
     * Set the texture that we'll be drawing our camera preview to. This is created from our TextureView
     * in our Renderer to be used with our shaders.
     */
    public void setPreviewTexture(int previewSurface) {
        this.mPreviewTexture = previewSurface;
    }

    public void setOnViewportSizeUpdatedListener(
            VideoFragment.OnViewportSizeUpdatedListener listener) {
        this.mOnViewportSizeUpdatedListener = listener;
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        mSurfaceTexture = surfaceTexture;
    }

    @Override
    public void onRendererReady() {

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                openCamera();
            }
        });
    }

    @Override
    public void onRendererFinished() {

    }

    public VideoRenderer getVideoRenderer() {
        return mVideoRenderer;
    }

    /**
     * Listener interface that will send back the newly created {@link Size} of our camera output
     */
    public interface OnViewportSizeUpdatedListener {

        void onViewportSizeUpdated(int viewportWidth, int viewportHeight);
    }

    /**
     * Simple ErrorDialog for display
     */
    public static class ErrorDialog extends DialogFragment {

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage("This device doesn't support Camera2 API.")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }
}
