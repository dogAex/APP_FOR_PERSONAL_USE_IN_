#include <iostream>
#include <cmath>
#include <iomanip>

struct QG_Color { float r, g, b; };

QG_Color ComputeFieldTheory(float u, float v, float time) {
    float dist = sqrt(pow(u - 0.5f, 2.0f) + pow(v - 0.5f, 2.0f));
    float d = 0.5f + 0.1f * sinf(time);
    float phase_diff = 2.0f * 3.1415926f * (d * sinf(dist * 3.1415926f)) / 0.7606f;
    float diff_r = fabsf(cosf(phase_diff * 1.8f));
    float diff_g = fabsf(cosf(phase_diff * 2.0f));
    float diff_b = fabsf(cosf(phase_diff * 2.2f));
    float metric = 1e6f * dist * dist;
    float interference = fabsf(sinf(u * metric + time) * cosf(v * metric - time));
    float spin = sinf(u * 3.1415f + time) * cosf(v * 3.1415f - time);
    float algebra_metric = 1e6f * (u*u + v*v);
    float alg_r = fabsf(sinf(algebra_metric * 0.001f) * spin);
    float alg_g = fabsf(cosf(algebra_metric * 0.002f) * spin);
    float alg_b = fabsf(sinf(algebra_metric * 0.003f + 0.7071f));
    float bloom = 1.0f / (1.0f + dist * dist * 500.0f);
    QG_Color out;
    out.r = (interference * 5e5f * bloom) + (diff_r * 2e5f) + (alg_r * 1e5f);
    out.g = (interference * 5e5f * bloom) + (diff_g * 2e5f) + (alg_g * 1e5f);
    out.b = (interference * 5e5f * bloom) + (diff_b * 2e5f) + (alg_b * 1e5f);
    out.r = out.r / (out.r + 1.0f); out.g = out.g / (out.g + 1.0f); out.b = out.b / (out.b + 1.0f);
    return out;
}

int main() {
    std::cout << "=== QG Ultimate Engine PC Core ===\n";
    QG_Color c = ComputeFieldTheory(0.5f, 0.5f, 1.618f);
    unsigned int *bits = (unsigned int *)&c;
    std::cout << "Hex: 0x" << std::hex << std::uppercase << bits[0] << " 0x" << bits[1] << " 0x" << bits[2] << "\n";
    std::cout << "Checksum: 0x" << (bits[0] ^ bits[1] ^ bits[2]) << "\n";
    return 0;
}
