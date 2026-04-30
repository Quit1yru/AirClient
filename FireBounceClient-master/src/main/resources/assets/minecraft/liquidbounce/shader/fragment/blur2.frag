#version 120

uniform sampler2D texture;
uniform vec2 texelSize;
uniform int radius;
uniform float strength;
uniform int direction;
uniform float quality;

void main() {
    vec2 uv = gl_TexCoord[0].xy;
    vec4 color = texture2D(texture, uv);
    float totalAlpha = color.a;

    // 高斯模糊核
    for (int i = -radius; i <= radius; i++) {
        float weight = exp(-float(i * i) / (2.0 * strength * strength));

        if (direction == 0) { // 水平模糊
            vec2 offset = vec2(float(i) * texelSize.x * quality, 0.0);
            color += texture2D(texture, uv + offset) * weight;
        } else { // 垂直模糊
            vec2 offset = vec2(0.0, float(i) * texelSize.y * quality);
            color += texture2D(texture, uv + offset) * weight;
        }
        totalAlpha += weight;
    }

    gl_FragColor = color / totalAlpha;
}