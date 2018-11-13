package com.androidexperiments.shadercam.example;

import com.androidexperiments.shadercam.example.gl.ExampleVideoRenderer;
import com.androidexperiments.shadercam.example.gl.SuperAwesomeRenderer;
import com.androidexperiments.shadercam.example.gl.TestRenderer;
import com.androidexperiments.shadercam.gl.CameraRenderer;
import com.androidexperiments.shadercam.gl.VideoRenderer;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.widget.SeekBar;

/**
 * For our NYC Android Developers Meetup, we've created a super simple
 * implementation of ShaderCam, with sliders
 */
public class MeetupActivityV2 extends SimpleRSVShaderActivity implements SeekBar.OnSeekBarChangeListener {
    private SeekBar mSeekbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        mVideoRenderer = new VideoRenderer(this);
        mVideoRenderer = new TestRenderer(this);
//        mVideoRenderer = new ExampleVideoRenderer(this);
        mSeekbar = (SeekBar) findViewById(R.id.seek_bar);
        mSeekbar.setOnSeekBarChangeListener(this);
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        ((TestRenderer)mVideoRenderer).setTileAmount(map(progress, 0.f, 100.f, 0.1f, 1.9f));
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
