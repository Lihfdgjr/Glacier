package gg.glacier.util;

import java.util.Collection;

public final class MathUtil {
    private MathUtil() {}

    public static double mean(Collection<? extends Number> values) {
        if (values.isEmpty()) return 0;
        double sum = 0;
        for (Number n : values) sum += n.doubleValue();
        return sum / values.size();
    }

    public static double stdDev(Collection<? extends Number> values) {
        if (values.size() < 2) return 0;
        double mu = mean(values);
        double sq = 0;
        for (Number n : values) {
            double d = n.doubleValue() - mu;
            sq += d * d;
        }
        return Math.sqrt(sq / values.size());
    }

    /** Coefficient of variation = stdDev / mean. Lower = more robotic. */
    public static double cv(Collection<? extends Number> values) {
        double mu = mean(values);
        if (mu <= 0) return Double.POSITIVE_INFINITY;
        return stdDev(values) / mu;
    }

    public static String formatVl(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return Double.toString(v);
        return v == (long) v ? Long.toString((long) v) : String.format("%.1f", v);
    }

    public static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }

    public static float wrapDegrees(float f) {
        f = f % 360f;
        if (f >= 180f) f -= 360f;
        if (f < -180f) f += 360f;
        return f;
    }

    /**
     * Ray vs axis-aligned bounding box, slab method.
     * Returns the distance along the ray to the first intersection, or -1 if
     * the ray does not hit the box while travelling forward.
     */
    public static double rayAabbDistance(double ox, double oy, double oz,
                                         double dx, double dy, double dz,
                                         double minX, double minY, double minZ,
                                         double maxX, double maxY, double maxZ) {
        double tMin = 0.0, tMax = Double.MAX_VALUE;
        double[] o = { ox, oy, oz };
        double[] d = { dx, dy, dz };
        double[] mn = { minX, minY, minZ };
        double[] mx = { maxX, maxY, maxZ };
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1e-9) {
                if (o[i] < mn[i] || o[i] > mx[i]) return -1;
            } else {
                double t1 = (mn[i] - o[i]) / d[i];
                double t2 = (mx[i] - o[i]) / d[i];
                if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
                if (t1 > tMin) tMin = t1;
                if (t2 < tMax) tMax = t2;
                if (tMin > tMax) return -1;
            }
        }
        return tMin;
    }
}

