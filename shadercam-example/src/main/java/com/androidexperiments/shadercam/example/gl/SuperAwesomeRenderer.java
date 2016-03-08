package com.androidexperiments.shadercam.example.gl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.SystemClock;

import com.androidexperiments.shadercam.gl.CameraRenderer;

/**
 * Our super awesome shader. It calls its super constructor with the new
 * glsl files we've created for this. Then it overrides {@link #setUniformsAndAttribs()}
 * to pass in our global time uniform
 */
public class SuperAwesomeRenderer extends CameraRenderer {
    private float mTileAmount = 1.f;

    public SuperAwesomeRenderer(Context context, SurfaceTexture texture, int width, int height) {
        super(context, texture, width, height, "superawesome.frag.glsl", "superawesome.vert.glsl");
    }

    @Override
    protected void setUniformsAndAttribs() {
        //always call super so that the built-in fun stuff can be set first
        super.setUniformsAndAttribs();

        int globalTimeHandle = GLES20.glGetUniformLocation(mCameraShaderProgram, "iGlobalTime");
        GLES20.glUniform1f(globalTimeHandle, SystemClock.currentThreadTimeMillis() / 100.0f);

        int resolutionHandle = GLES20.glGetUniformLocation(mCameraShaderProgram, "iResolution");
        GLES20.glUniform3f(resolutionHandle, mTileAmount, mTileAmount, 1.f);
    }

    public void setTileAmount(float tileAmount) {
        this.mTileAmount = tileAmount;
    }
}
