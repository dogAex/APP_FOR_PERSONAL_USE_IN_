package com.qg.reshade;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class FieldRenderer extends GLSurfaceView implements GLSurfaceView.Renderer {
    private int program;
    private FloatBuffer vertexBuffer;
    private long startTime;

    public FieldRenderer(Context context) {
        super(context);
        setEGLContextClientVersion(3);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        getHolder().setFormat(android.graphics.PixelFormat.TRANSLUCENT);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    private final String vCode = "attribute vec4 vPosition; void main() { gl_Position = vPosition; }";

    private final String fCode =
        "#version 300 es\n" +
        "precision highp float;\n" +
        "out vec4 fragColor;\n" +
        "uniform float u_sync_time;\n" +
        "uniform vec2 u_resolution;\n" +
        "void main() {\n" +
        "  vec2 uv = gl_FragCoord.xy / u_resolution;\n" +
        "  vec2 center = uv - vec2(0.5);\n" +
        "  float dist = length(center);\n" +
        "  \n" +
        // ========================================================
        // 1. 1亿k 全息投影与相空间坍缩 (Base Holographic Field)
        // ========================================================
        "  float metric_holographic = 1e11 * (uv.x * uv.y);\n" +
        "  float bosonic = sin(uv.x * metric_holographic + u_sync_time) * cos(uv.y * metric_holographic - u_sync_time);\n" +
        "  float fermionic = cos(uv.x * metric_holographic - u_sync_time) * sin(uv.y * metric_holographic + u_sync_time);\n" +
        "  float collapse_interference = abs(bosonic + fermionic);\n" +
        "  \n" +
        // 利用 smoothstep 将极高维能量投影坍缩到 0~1 的色彩相空间
        "  float base_r = smoothstep(0.2, 0.8, collapse_interference);\n" +
        "  float base_g = smoothstep(0.1, 0.9, collapse_interference * 0.5);\n" +
        "  float base_b = smoothstep(0.3, 1.0, collapse_interference * 1.5);\n" +
        "  vec3 holographic_base = vec3(base_r, base_g, base_b) * 1e5;\n" +
        "  \n" +
        // ========================================================
        // 2. 光栅衍射与李代数演化 (Diffraction & Algebra)
        // ========================================================
        "  float d = 0.5 + 0.1 * sin(u_sync_time);\n" +
        "  float phase_diff = 2.0 * 3.1415926 * (d * sin(dist * 3.1415926)) / 0.7606;\n" +
        "  vec3 diff_color = vec3(abs(cos(phase_diff * 1.8)), abs(cos(phase_diff * 2.0)), abs(cos(phase_diff * 2.2)));\n" +
        "  \n" +
        "  float spin = sin(uv.x * 3.1415 + u_sync_time) * cos(uv.y * 3.1415 - u_sync_time);\n" +
        "  float algebra_metric = 1e6 * (uv.x*uv.x + uv.y*uv.y);\n" +
        "  vec3 algebra_rgb = vec3(abs(sin(algebra_metric * 0.001) * spin), abs(cos(algebra_metric * 0.002) * spin), abs(sin(algebra_metric * 0.003 + 0.7071))) * 1e5;\n" +
        "  \n" +
        // ========================================================
        // 3. 万亿度规超位态时间拖影 (Superposition & Time Smear)
        // ========================================================
        "  float metric = 1e12 * dist * dist;\n" +
        "  float quantum_time = floor(u_sync_time * 60.0) / 60.0;\n" +
        "  float time_phase = u_sync_time - quantum_time;\n" +
        "  float super_pos_history = sin(uv.x * metric + quantum_time) * cos(uv.y * metric + quantum_time);\n" +
        "  float super_pos_current = sin(uv.x * metric - u_sync_time) * cos(uv.y * metric - u_sync_time);\n" +
        "  float interference = abs(mix(super_pos_history, super_pos_current, time_phase));\n" +
        "  \n" +
        // ========================================================
        // 4. 终极融合与最高权重物理覆盖 (Ultimate Fusion)
        // ========================================================
        "  float bloom = 1.0 / (1.0 + dist * dist * 500.0);\n" +
        // 以全息投影打底，混合紫蓝干涉泛光
        "  vec3 hdrColor = holographic_base;\n" +
        "  hdrColor += mix(vec3(0.9, 0.2, 1.0), vec3(0.1, 0.9, 1.0), interference) * (5e11 * bloom);\n" +
        // 叠加衍射与代数高光
        "  hdrColor += diff_color * 2e5;\n" +
        "  hdrColor += algebra_rgb;\n" +
        // 叠加历史拖影
        "  hdrColor += vec3(super_pos_history * 2e5, 0.0, super_pos_current * 2e5);\n" +
        "  \n" +
        // ACES 电影级映射压回可见光
        "  vec3 finalColor = hdrColor / (hdrColor + vec3(1.0));\n" +
        "  finalColor = pow(finalColor, vec3(1.0 / 2.2));\n" +
        "  \n" +
        // 保持 0.65 的透明叠加
        "  fragColor = vec4(finalColor, 0.65);\n" +
        "}";

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        int vShader = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER);
        GLES30.glShaderSource(vShader, vCode);
        GLES30.glCompileShader(vShader);

        int fShader = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER);
        GLES30.glShaderSource(fShader, fCode);
        GLES30.glCompileShader(fShader);

        program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vShader);
        GLES30.glAttachShader(program, fShader);
        GLES30.glLinkProgram(program);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        float[] quad = { -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f };
        vertexBuffer = ByteBuffer.allocateDirect(quad.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(quad).position(0);
        startTime = System.currentTimeMillis();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) { GLES30.glViewport(0, 0, width, height); }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glUseProgram(program);
        GLES30.glUniform1f(GLES30.glGetUniformLocation(program, "u_sync_time"), (System.currentTimeMillis() - startTime) / 1000.0f);
        GLES30.glUniform2f(GLES30.glGetUniformLocation(program, "u_resolution"), getWidth(), getHeight());
        int pos = GLES30.glGetAttribLocation(program, "vPosition");
        GLES30.glEnableVertexAttribArray(pos);
        GLES30.glVertexAttribPointer(pos, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
    }
}
