package com.androidexperiments.shadercam.example;

import android.graphics.SurfaceTexture;

import com.androidexperiments.shadercam.example.gl.SuperAwesomeRenderer;
import com.androidexperiments.shadercam.gl.CameraRenderer;

/**
 * For our NYC Android Developers Meetup, we've created a super simple
 * implementation of ShaderCam, where you will only need to create
 * 3 files to
 */
public class MeetupActivity extends SimpleShaderActivity {
    private SuperAwesomeRenderer mMyRenderer;

    @Override
    protected CameraRenderer getRenderer(SurfaceTexture surface, int width, int height) {
        mMyRenderer = new SuperAwesomeRenderer(this, surface, width, height);
        return mMyRenderer;
    }
}

/*

 */