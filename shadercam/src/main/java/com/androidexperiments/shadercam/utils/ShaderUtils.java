package com.androidexperiments.shadercam.utils;

import android.content.Context;
import android.view.View;
import android.view.Window;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Utilites for ShaderCamera
 */
public class ShaderUtils
{
    /**
     * Helper for getting strings from any file type in /assets/ folder. Primarily used for shaders.
     *
     * @param ctx Context to use
     * @param filename name of the file, including any folders, inside of the /assets/ folder.
     * @return String of contents of file, lines separated by <code>\n</code>
     * @throws java.io.IOException if file is not found
     */
    public static String getStringFromFileInAssets(Context ctx, String filename) throws IOException {
        return getStringFromFileInAssets(ctx, filename, true);
    }

    public static String getStringFromFileInAssets(Context ctx, String filename, boolean useNewline) throws IOException
    {
        InputStream is = ctx.getAssets().open(filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder builder = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null)
        {
            builder.append(line + (useNewline ? "\n" : ""));
        }
        is.close();
        return builder.toString();
    }


    /**
     * Convenience method for getting into Immersive mode.
     */
    public static void goFullscreen(Window window) {
        window.getDecorView()
                .setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
    }
}
