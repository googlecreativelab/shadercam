shadercam
=========

Simple OpenGL Shaders with the [camera2](https://developer.android.com/reference/android/hardware/camera2/package-summary.html) apis in Android 5.0+

examples
--------

Check out [`shadercam-example`](https://github.com/googlecreativelab/shadercam/tree/master/shadercam-example) here for basic usage.

Also, **shadercam** was built for usage with a couple Android Experiments:

* [Lip Swap](https://github.com/googlecreativelab/lipswap)
* [Tunnel Vision](https://github.com/googlecreativelab/tunnelvision)

permissions
-----------

**updated 9/14/15**

We've added a [`PermissionsHelper`](https://github.com/googlecreativelab/shadercam/blob/master/shadercam/src/main/java/com/androidexperiments/shadercam/fragments/PermissionsHelper.java) 
fragment to make handling Android M's new permissions model a bit easier.

Refer to the example applications [`MainActivity.java`](https://github.com/googlecreativelab/shadercam/blob/master/shadercam-example/src/main/java/com/androidexperiments/shadercam/example/MainActivity.java#L82)
for implementation specifics. 

usage
-----

Import **shadercam** in your `build.gradle` file:

```
compile project(':shadercam')
```
or
```
compile 'com.androidexperiments:shadercam:1.1.0'
```

**shadercam** comes with a simple implementation of the camera2 apis called `CameraFragment`, which only
requires that you add a `TextureView` to your layout.

```
private void setCameraFragment() {
    mCameraFragment = CameraFragment.getInstance();
    mCameraFragment.setCameraToUse(CameraFragment.CAMERA_PRIMARY); //or CAMERA_BACK
    mCameraFragment.setTextureView(mTextureView); //the TextureView we added to our layout

    //add fragment to our setup and let it work its magic
    getSupportFragmentManager().beginTransaction()
        .add(mCameraFragment, TAG_CAMERA_FRAGMENT) //any tag is fine if u want to access later
        .commit();
}
```

Once your CameraFragment is setup, we need to wait until our `TextureView` is ready to create
 our `CameraRenderer`.

```
public void onResume() {
    if(!mTextureView.isAvailable())
        mTextureView.setSurfaceTextureListener(mTextureListener);
    else
        setReady(mTextureView.getSurfaceTexture(), mTextureView.getWidth(), mTextureView.getHeight());
}
```

Our texture listener is your normal, every day `TextureView.SurfaceTextureListener` that will also call our `setReady` method that will create our renderer.
Now all you have to do is extend `CameraRenderer` to do anything you want with the video feed!

```
private void setReady(SurfaceTexture surface, int width, int height) {
    mRenderer = new ExampleRenderer(this, surface, mCameraFragment, width, height);
    mRenderer.setOnRendererReadyListener(this);
    mRenderer.start();

    //initial config if needed
    mCameraFragment.configureTransform(width, height);
}
```

Check out `MainActivity` and `ExampleRenderer` in `shadercam-example` for more in depth explanations and details.

more info
---------

If you make something cool with shadercam, let us know by heading over to [Android Experiments](http://www.androidexperiments.com) and submitting your experiment!

Report any issues [here](https://github.com/googlecreativelab/shadercam/issues) - we love pull requests!

license
-------

```
Copyright 2015 Google Inc.

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