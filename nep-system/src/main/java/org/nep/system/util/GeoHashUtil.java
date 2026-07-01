package org.nep.system.util;

/**
 * GeoHash 编码/解码工具 — 用于附近反馈查询的空间索引
 * GeoHash将经纬度编码为一个字符串，前缀匹配 = 空间邻近
 */
public class GeoHashUtil {

    private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";
    private static final int[] BITS = {16, 8, 4, 2, 1};

    /** 经纬度编码为GeoHash (默认精度8位 ≈ 38m × 19m) */
    public static String encode(double lat, double lng, int precision) {
        double[] latRange = {-90.0, 90.0};
        double[] lngRange = {-180.0, 180.0};
        StringBuilder hash = new StringBuilder();
        boolean isEven = true;
        int bit = 0, ch = 0;

        while (hash.length() < precision) {
            if (isEven) {
                double mid = (lngRange[0] + lngRange[1]) / 2;
                if (lng >= mid) { ch |= BITS[bit]; lngRange[0] = mid; }
                else lngRange[1] = mid;
            } else {
                double mid = (latRange[0] + latRange[1]) / 2;
                if (lat >= mid) { ch |= BITS[bit]; latRange[0] = mid; }
                else latRange[1] = mid;
            }
            isEven = !isEven;
            if (bit < 4) bit++; else { hash.append(BASE32.charAt(ch)); bit = 0; ch = 0; }
        }
        return hash.toString();
    }

    /** 默认7位精度 */
    public static String encode(double lat, double lng) {
        return encode(lat, lng, 7);
    }

    /** 计算两点间距离(米) — Haversine公式 */
    public static double distance(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371000; // 地球半径(米)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
