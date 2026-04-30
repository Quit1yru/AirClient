#version 120

uniform sampler2D texture;
uniform vec2 texelSize;
uniform float blurAmount;

varying vec2 texCoord;

// 高斯模糊权重
const float weights[5] = float[5](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

void main() {
    vec4 color = texture2D(texture, texCoord);

    if (blurAmount < 0.001) {
        gl_FragColor = color;
        return;
    }

    vec4 result = color * weights[0];

    // 根据模糊强度调整采样半径
    float radius = blurAmount * 3.0;

    for (int i = 1; i < 5; i++) {
        float offset = float(i) * radius;

        // 当前方向采样（水平或垂直）
        vec2 offsetCoord = texelSize * offset;

        // 正方向
        vec2 samplePos1 = texCoord + offsetCoord;
        if (samplePos1.x >= 0.0 && samplePos1.x <= 1.0 && samplePos1.y >= 0.0 && samplePos1.y <= 1.0) {
            result += texture2D(texture, samplePos1) * weights[i];
        }

        // 负方向
        vec2 samplePos2 = texCoord - offsetCoord;
        if (samplePos2.x >= 0.0 && samplePos2.x <= 1.0 && samplePos2.y >= 0.0 && samplePos2.y <= 1.0) {
            result += texture2D(texture, samplePos2) * weights[i];
        }
    }

    // 应用透明度
    gl_FragColor = vec4(result.rgb, color.a);
}