#version 120

uniform sampler2D texture;
uniform vec2 texelSize;   // (1/width,0) 或 (0,1/height)
uniform float radius;     // 模糊半径，建议范围 1~15

void main() {
    vec2 uv = gl_TexCoord[0].xy;

    vec4 color = vec4(0.0);
    float total = 0.0;

    int r = int(radius);

    // 固定最大范围 [-20,20]，用 if 控制实际半径
    for (int i = -20; i <= 20; i++) {
        if (abs(i) <= r) {
            float fi = float(i);
            float weight = exp(-(fi * fi) / (2.0 * radius * radius));
            color += texture2D(texture, uv + texelSize * fi) * weight;
            total += weight;
        }
    }

    gl_FragColor = color / total;
}
