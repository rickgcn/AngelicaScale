#version 420 compatibility
#extension GL_ARB_texture_gather : require
#extension GL_ARB_shading_language_packing : enable
#extension GL_ARB_gpu_shader5 : enable

uniform sampler2D uInputTexture;
uniform ivec4 uConst0;
uniform ivec4 uConst1;
uniform ivec4 uConst2;
uniform ivec4 uConst3;

#define A_GPU 1
#define A_GLSL 1
#include "../include/ffx_a.h"

#define FSR_EASU_F 1
AF4 FsrEasuRF(AF2 p) {
    return AF4(textureGather(uInputTexture, p, 0));
}

AF4 FsrEasuGF(AF2 p) {
    return AF4(textureGather(uInputTexture, p, 1));
}

AF4 FsrEasuBF(AF2 p) {
    return AF4(textureGather(uInputTexture, p, 2));
}

#include "../include/ffx_fsr1.h"

out vec4 fragColor;

void main() {
    AF3 color;
    FsrEasuF(
        color,
        AU2(ASU2(gl_FragCoord.xy)),
        AU4(uConst0),
        AU4(uConst1),
        AU4(uConst2),
        AU4(uConst3));
    fragColor = vec4(color, 1.0);
}
