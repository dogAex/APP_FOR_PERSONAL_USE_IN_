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
        // 光栅衍射
        "  float d = 0.5 + 0.1 * sin(u_sync_time);\n" +
        "  float phase_diff = 2.0 * 3.1415926 * (d * sin(dist * 3.1415926)) / 0.7606;\n" +
        "  vec3 diff_color = vec3(abs(cos(phase_diff * 1.8)), abs(cos(phase_diff * 2.0)), abs(cos(phase_diff * 2.2)));\n" +
        // 亿万度规超位态
        "  float metric = 1e12 * dist * dist;\n" +
        "  float quantum_time = floor(u_sync_time * 60.0) / 60.0;\n" +
        "  float time_phase = u_sync_time - quantum_time;\n" +
        "  float super_pos_history = sin(uv.x * metric + quantum_time) * cos(uv.y * metric + quantum_time);\n" +
        "  float super_pos_current = sin(uv.x * metric - u_sync_time) * cos(uv.y * metric - u_sync_time);\n" +
        "  float interference = abs(mix(super_pos_history, super_pos_current, time_phase));\n" +
        // 李代数算子
        "  float spin = sin(uv.x * 3.1415 + u_sync_time) * cos(uv.y * 3.1415 - u_sync_time);\n" +
        "  float algebra_metric = 1e6 * (uv.x*uv.x + uv.y*uv.y);\n" +
        "  vec3 algebra_rgb = vec3(abs(sin(algebra_metric * 0.001) * spin), abs(cos(algebra_metric * 0.002) * spin), abs(sin(algebra_metric * 0.003 + 0.7071))) * 1e5;\n" +
        // 终极融合与最高权重覆盖
        "  float bloom = 1.0 / (1.0 + dist * dist * 500.0);\n" +
        "  vec3 hdrColor = mix(vec3(0.9, 0.2, 1.0), vec3(0.1, 0.9, 1.0), interference) * (5e11 * bloom);\n" +
        "  hdrColor += diff_color * 2e5;\n" +
        "  hdrColor += algebra_rgb;\n" +
        "  hdrColor += vec3(super_pos_history * 2e5, 0.0, super_pos_current * 2e5);\n" +
        // ACES
        "  vec3 finalColor = hdrColor / (hdrColor + vec3(1.0));\n" +
        "  finalColor = pow(finalColor, vec3(1.0 / 2.2));\n" +
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
