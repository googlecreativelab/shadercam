package com.androidexperiments.shadercam.example.gl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.SystemClock;
import android.util.Log;

import com.androidexperiments.shadercam.gl.CameraRenderer;

/**
 * Our super awesome shader. It calls its super constructor with the new new
 * glsl files we've created for this. Then it overrides the necessary methods to
 * add some interactivity.
 */
public class SuperAwesomeRenderer extends CameraRenderer {
    public SuperAwesomeRenderer(Context context, SurfaceTexture texture, int width, int height) {
        super(context, texture, width, height, "superawesome.frag.glsl", "superawesome.vert.glsl");
    }

    @Override
    protected void setUniformsAndAttribs() {
        //always call super so that the built-in fun stuff can be set first
        super.setUniformsAndAttribs();

        int globalTimeHandle = GLES20.glGetUniformLocation(mCameraShaderProgram, "iGlobalTime");
        float time = SystemClock.currentThreadTimeMillis() / 100.0f;
        GLES20.glUniform1f(globalTimeHandle, time);
    }
}
