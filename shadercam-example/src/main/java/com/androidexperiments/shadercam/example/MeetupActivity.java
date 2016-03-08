package com.androidexperiments.shadercam.example;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.widget.SeekBar;

import com.androidexperiments.shadercam.example.gl.SuperAwesomeRenderer;
import com.androidexperiments.shadercam.gl.CameraRenderer;

/**
 * For our NYC Android Developers Meetup, we've created a super simple
 * implementation of ShaderCam, with sliders
 */
public class MeetupActivity extends SimpleShaderActivity implements SeekBar.OnSeekBarChangeListener {
    private SuperAwesomeRenderer mMyRenderer;
    private SeekBar mSeekbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSeekbar = (SeekBar) findViewById(R.id.seek_bar);
        mSeekbar.setOnSeekBarChangeListener(this);
    }

    @Override
    protected CameraRenderer getRenderer(SurfaceTexture surface, int width, int height) {
        mMyRenderer = new SuperAwesomeRenderer(this, surface, width, height);
        return mMyRenderer;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mMyRenderer.setTileAmount(map(progress, 0.f, 100.f, 0.1f, 1.9f));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        //dont need
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        //dont need
    }

    /**
     * Takes a value, assumes it falls between start1 and stop1, and maps it to a value
     * between start2 and stop2.
     *
     * For example, above, our slide goes 0-100, starting at 50. We map 0 on the slider
     * to .1f and 100 to 1.9f, in order to better suit our shader calculations
     */
    float map(float value, float start1, float stop1, float start2, float stop2) {
        return start2 + (stop2 - start2) * ((value - start1) / (stop1 - start1));
    }
}
