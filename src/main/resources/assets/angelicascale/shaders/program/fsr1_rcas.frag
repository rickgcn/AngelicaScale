#version 420 compatibility
#extension GL_ARB_shading_language_packing : enable
#extension GL_ARB_gpu_shader5 : enable

uniform sampler2D uInputTexture;
uniform ivec4 uConst0;

#define A_GPU 1
#define A_GLSL 1
#include "../include/ffx_a.h"

#define FSR_RCAS_F 1
AF4 FsrRcasLoadF(ASU2 p) {
    return texelFetch(uInputTexture, ivec2(p), 0);
}

void FsrRcasInputF(inout AF1 r, inout AF1 g, inout AF1 b) {}

#include "../include/ffx_fsr1.h"

out vec4 fragColor;

void main() {
    AF3 color;
    FsrRcasF(color.r, color.g, color.b, AU2(ASU2(gl_FragCoord.xy)), AU4(uConst0));
    fragColor = vec4(color, 1.0);
}
