package dev.vortex.hud;

/** Fixed-size ring buffer of frame times. No allocation per frame. */
public final class FrameStats {
    private static final int N = 600;
    private static final float[] ms = new float[N];
    private static final float[] sorted = new float[N];
    private static int idx, count;
    private static long last;
    private static long lastSort;
    private static float cachedAvg, cachedLow1;

    public static void frame() {
        long now = System.nanoTime();
        if (last != 0) {
            ms[idx] = (now - last) / 1_000_000f;
            idx = (idx + 1) % N;
            if (count < N) count++;
        }
        last = now;
    }

    public static float lastFrameMs() { return count == 0 ? 0 : ms[(idx - 1 + N) % N]; }

    public static float currentFps() {
        float f = lastFrameMs();
        return f <= 0 ? 0 : 1000f / f;
    }

    private static void recompute() {
        long now = System.currentTimeMillis();
        if (now - lastSort < 500 || count == 0) return;
        lastSort = now;
        float sum = 0;
        for (int i = 0; i < count; i++) { sorted[i] = ms[i]; sum += ms[i]; }
        java.util.Arrays.sort(sorted, 0, count);
        cachedAvg = 1000f / (sum / count);
        int worst = Math.max(1, count / 100);
        float w = 0;
        for (int i = count - worst; i < count; i++) w += sorted[i];
        cachedLow1 = 1000f / (w / worst);
    }

    public static float averageFps() { recompute(); return cachedAvg; }
    public static float onePercentLow() { recompute(); return cachedLow1; }

    private FrameStats() {}
}
