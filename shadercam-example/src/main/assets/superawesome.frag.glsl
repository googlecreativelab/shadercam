#extension GL_OES_EGL_image_external : require

//necessary
precision mediump float;
uniform samplerExternalOES camTexture;

varying vec2 v_CamTexCoordinate;
varying vec2 v_TexCoordinate;
//end necessary

// passed in via our SuperAwesomeRenderer.java
uniform float	iGlobalTime;

// play around with xy for different sized effect
vec3	        iResolution = vec3(1.,1.,1.);

vec2 mirror(vec2 x)
{
	return abs(fract(x/2.0) - 0.5)*2.0;
}

void main ()
{
    vec2 uv = -1.0 + 2.0 * v_CamTexCoordinate.xy / iResolution.xy;

	float a = iGlobalTime * 0.2;

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
