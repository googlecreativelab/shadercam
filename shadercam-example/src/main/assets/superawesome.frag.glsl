#extension GL_OES_EGL_image_external : require

//necessary
precision mediump float;
uniform samplerExternalOES camTexture;

varying vec2 v_CamTexCoordinate;
varying vec2 v_TexCoordinate;
//end necessary

// Code taken from https://www.shadertoy.com/view/XsfGzj and modified to work in this context as an example
// of how quickly and easily you can get up and running with shadercam

// passed in via our SuperAwesomeRenderer.java
uniform float	iGlobalTime;

// play around with xy for different sized effect, or pass in via GLES20.glUniform3f();
uniform vec3    iResolution;

// simple helper method
vec2 mirror(vec2 x)
{
	return abs(fract(x/2.0) - 0.5)*2.0;
}

void main ()
{
    vec2 uv = -1.0 + 2.0 * v_CamTexCoordinate.xy / iResolution.xy;

	float a = iGlobalTime * 0.002;

	for (float i = 1.0; i < 6.0; i += 1.0) {
		uv = vec2(sin(a)*uv.y - cos(a)*uv.x, sin(a)*uv.x + cos(a)*uv.y);
		uv = mirror(uv);

		// These two lines can be changed for slightly different effects
		// This is just something simple that looks nice
		a += i;
		a /= i;
	}

    gl_FragColor = texture2D(camTexture, mirror(uv*2.0));
}
