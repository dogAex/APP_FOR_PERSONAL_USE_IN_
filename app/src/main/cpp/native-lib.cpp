#include <jni.h>
#include <string>
#include <cmath>
#include <android/log.h>
#include <sys/system_properties.h>
#include <cstdlib>

#define LOG_TAG "QG_Engine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_qg_reshade_MainActivity_probeKernel(JNIEnv* env, jobject) {
    float time = 1.618f;
    float u = 0.5f, v = 0.5f;
    float dist = sqrt(pow(u - 0.5f, 2.0f) + pow(v - 0.5f, 2.0f));
    float d = 0.5f + 0.1f * sinf(time);
    float phase_diff = 2.0f * 3.1415926f * (d * sinf(dist * 3.1415926f)) / 0.7606f;
    float diff_r = fabsf(cosf(phase_diff * 1.8f));
    float metric = 1e6f * dist * dist;
    float interference = fabsf(sinf(u * metric + time) * cosf(v * metric - time));
    unsigned int checksum = *(unsigned int*)&interference ^ *(unsigned int*)&diff_r;

    char buf[128];
    snprintf(buf, sizeof(buf), "QG Kernel Active. Checksum: 0x%08X", checksum);
    return env->NewStringUTF(buf);
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_qg_reshade_MainActivity_getGtaoParams(JNIEnv* env, jobject) {
    float defaults[3] = {0.02f, 1.5f, 0.1f};
    char val[PROP_VALUE_MAX];
    if (__system_property_get("debug.qg.gtao.radius", val) > 0) defaults[0] = atof(val);
    if (__system_property_get("debug.qg.gtao.intensity", val) > 0) defaults[1] = atof(val);
    if (__system_property_get("debug.qg.gtao.bias", val) > 0) defaults[2] = atof(val);
    
    jfloatArray result = env->NewFloatArray(3);
    env->SetFloatArrayRegion(result, 0, 3, defaults);
    return result;
}
