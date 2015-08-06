package com.androidexperiments.shadercam.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * Simple TextureView implementation that forces its height to match its width.
 * From the XML you can make layout_height whatever u want, it will be ignored and
 *
 */
public class SquareTextureView extends TextureView {
    public SquareTextureView(Context context) {
        super(context);
    }

    public SquareTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * get width of the view once its measured and repeat for its height. in XML you
     * can set the width to your desired size and the height will always match regardless
     * of what you put
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, width);
    }
}
