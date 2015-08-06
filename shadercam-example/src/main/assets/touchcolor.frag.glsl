#extension GL_OES_EGL_image_external : require

//necessary
precision mediump float;
uniform samplerExternalOES camTexture;

varying vec2 v_CamTexCoordinate;
varying vec2 v_TexCoordinate;

uniform float offsetR;
uniform float offsetG;
uniform float offsetB;

void main ()
{
    vec4 cameraColor = texture2D(camTexture, v_CamTexCoordinate);
    gl_FragColor = cameraColor * vec4(offsetR, offsetG, offsetB, 1.);
}