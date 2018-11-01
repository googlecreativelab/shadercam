shadercam
=========

Simple OpenGL Shaders with the [camera2](https://developer.android.com/reference/android/hardware/camera2/package-summary.html) apis in Android 7.0+

examples
--------

Check out [`shadercam-example`](https://github.com/googlecreativelab/shadercam/tree/master/shadercam-example) here for basic usage.

Also, **shadercam** was built for usage with a couple Android Experiments:

* [Lip Swap](https://github.com/googlecreativelab/lipswap)
* [Tunnel Vision](https://github.com/googlecreativelab/tunnelvision)

permissions
-----------

**updated 10/31/18**

**shadercam** is now using [RecordableSurfaceView](https://github.com/UncorkedStudios/recordablesurfaceview) to draw to the screen as well as record video. This means some things have changed but compatibility across devices has improved. See below for a guide on upgrading. All previous classes and methods are still available, so the new API is non-breaking, but includes no fixes for pre-2.0 versions.

usage (as of release-2.0)
-----

Import **shadercam** in your `build.gradle` file:

```
implementation project(':shadercam')
```
or

In the project gradle add jitpack:

```
repositories {
        google()
        jcenter()
        maven { url 'https://www.jitpack.io' }

    }
```
And then in your app gradle, add the dependency for shadercam:

```
implementation 'com.github.uncorkedstudios:shadercam:2.0.1'
```

Integration Example
-----
_**Changed from v1.0:** shadercam now expects that a VideoRenderer be created and attached before  connecting to the Camera preview service. This is more in line with OpenGL renderer lifecycle patterns_

**shadercam** comes with a simple implementation of the camera2 apis called `VideoFragment`, which only
requires that you add a `RecordableSurfaceView` to your layout.

```
    <com.uncorkedstudios.android.view.recordablesurfaceview.RecordableSurfaceView
        android:id="@+id/texture"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_alignParentTop="true" />
```

Then it is a fairly simple matter of configuring and attaching the Camera management fragment:

```
private void setupCameraFragment(VideoRenderer renderer) {

        mVideoFragment = VideoFragment.getInstance();
        mVideoFragment.setCameraToUse(VideoFragment.CAMERA_FORWARD);
        
        //pass in a reference to the RecordableSurfaceView - this is important
        mVideoFragment.setRecordableSurfaceView(mRecordableSurfaceView);

        //Connect your renderer
        mVideoFragment.setVideoRenderer(renderer);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(mVideoFragment, TAG_CAMERA_FRAGMENT);
        transaction.commit();
    }
```
Once the VideoFragment has been provided a renderer (a default one is provided in the case where you do not provide a custom one), and a RecordableSurfaceView to render onto, it can then be added to the activity and brought to life for viewing the camera feed. 

In order to record to a video file, there are a few more steps to take.

For example, in this implementation of  onResume, we make sure that `RecordableSurfaceView` has a handle to a file that it has permissions to write a movie file to: 

```
public void onResume() {
   ...
               
    mRecordableSurfaceView.initRecorder(mCurrentVideoFile, size.x, size.y, null, null);
    
    ...
}
```

> Note that phones with Notches may behave differently based on the underlying system's implementation, for example, the outputs of [getRealSize](https://developer.android.com/reference/android/view/Display#getRealSize(android.graphics.Point)) vs. [getSize](https://developer.android.com/reference/android/view/Display#getSize(android.graphics.Point)) in the [Display](https://developer.android.com/reference/android/view/Display) class will return different shapes that either include or exclude the notch. 


Lifecycle
----
The new `VideoRenderer` class implements `RecordableSurfaceView.RendererCallbacks` so your top-level renderer can recieve lifecycle callbacks like onSurfaceChanged and onDrawFrame - closer to the native [GLSurfaceView.Renderer](https://developer.android.com/reference/android/opengl/GLSurfaceView.Renderer) pattern.

> `public void onSurfaceCreated()`


> `public void onSurfaceChanged(int width, int height) `

etc. 

Examples:
----
Check out `MeetupActivityV2`, `SimpleRSVShaderActivity` and `ExampleVideoRenderer` in `shadercam-example` for some straightforward example code.

more info
---------

If you make something cool with shadercam, let us know by heading over to [Android Experiments](http://www.androidexperiments.com) and submitting your experiment!

Report any issues [here](https://github.com/googlecreativelab/shadercam/issues) - we love pull requests!

license
-------

```
Copyright 2018 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```