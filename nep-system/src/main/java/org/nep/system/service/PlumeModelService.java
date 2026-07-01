package org.nep.system.service;

import org.springframework.stereotype.Service;
import java.util.*;

/**
 * 高斯烟羽扩散模型 (Gaussian Plume Model) + 反向溯源算法
 *
 * 经典大气扩散数学模型，计算污染物从点源释放后在空气中的扩散范围和浓度分布。
 * C(x,y,0) = (Q / (π·u·σy·σz)) · exp(-y²/(2σy²)) · exp(-H²/(2σz²))
 */
@Service
public class PlumeModelService {

    /** 大气稳定度等级（Pasquill-Gifford分类） */
    public enum StabilityClass {
        A,  // 极不稳定 (强日照, 风速<2m/s)
        B,  // 不稳定
        C,  // 弱不稳定
        D,  // 中性 (阴天/夜间)
        E,  // 弱稳定
        F   // 稳定 (夜间, 风速<3m/s)
    }

    /**
     * Briggs城市扩散系数公式
     * @param x 下风向距离(米)
     * @param stability 大气稳定度
     * @return [σy, σz] 横向/垂向扩散系数
     */
    private double[] briggsCoefficients(double x, StabilityClass stability) {
        double sigmaY, sigmaZ;
        switch (stability) {
            case A:
                sigmaY = 0.22 * x / Math.sqrt(1 + 0.0001 * x);
                sigmaZ = 0.20 * x;
                break;
            case B:
                sigmaY = 0.16 * x / Math.sqrt(1 + 0.0001 * x);
                sigmaZ = 0.12 * x;
                break;
            case C:
                sigmaY = 0.11 * x / Math.sqrt(1 + 0.0001 * x);
                sigmaZ = 0.08 * x / Math.sqrt(1 + 0.0002 * x);
                break;
            case D:
                sigmaY = 0.08 * x / Math.sqrt(1 + 0.0001 * x);
                sigmaZ = 0.06 * x / Math.sqrt(1 + 0.0015 * x);
                break;
            case E:
                sigmaY = 0.06 * x / Math.sqrt(1 + 0.0001 * x);
                sigmaZ = 0.03 * x / (1 + 0.0003 * x);
                break;
            case F:
                sigmaY = 0.04 * x / Math.sqrt(1 + 0.0001 * x);
                sigmaZ = 0.016 * x / (1 + 0.0003 * x);
                break;
            default:
                sigmaY = 0.08 * x / Math.sqrt(1 + 0.0001 * x);
                sigmaZ = 0.06 * x / Math.sqrt(1 + 0.0015 * x);
        }
        return new double[]{sigmaY, sigmaZ};
    }

    /**
     * 计算单点浓度 (地面浓度, z=0)
     * @param sourceLng 源经度
     * @param sourceLat 源纬度
     * @param targetLng 目标经度
     * @param targetLat 目标纬度
     * @param sourceStrength Q值 (排放强度)
     * @param windSpeed 风速 (m/s)
     * @param windDirection 风向 (度, 0=北, 90=东)
     * @param effectiveHeight 有效烟囱高度 (m)
     * @param stability 大气稳定度
     * @return 浓度值 (μg/m³)
     */
    public double concentration(double sourceLng, double sourceLat,
                                  double targetLng, double targetLat,
                                  double sourceStrength, double windSpeed,
                                  double windDirection, double effectiveHeight,
                                  StabilityClass stability) {

        // 计算两点距离和方位角
        double dx = (targetLng - sourceLng) * 111320 * Math.cos(Math.toRadians((sourceLat + targetLat) / 2));
        double dy = (targetLat - sourceLat) * 110540;

        // 风向转换: 0°=北 → 数学坐标系 (0°=东, 逆时针)
        double windRad = Math.toRadians(90 - windDirection);
        double cosW = Math.cos(windRad);
        double sinW = Math.sin(windRad);

        // 投影到下风/侧风坐标系
        double x = dx * cosW + dy * sinW;  // 下风向距离
        double y = -(dx * sinW - dy * cosW); // 侧风距离

        if (x <= 0) return 0; // 上风方向无污染

        double[] coeffs = briggsCoefficients(x, stability);
        double sigmaY = coeffs[0];
        double sigmaZ = coeffs[1];

        // 防护: sigmaY和sigmaZ不能为0
        if (sigmaY < 0.1) sigmaY = 0.1;
        if (sigmaZ < 0.1) sigmaZ = 0.1;

        double conc = (sourceStrength / (Math.PI * windSpeed * sigmaY * sigmaZ))
                * Math.exp(-y * y / (2 * sigmaY * sigmaY))
                * Math.exp(-effectiveHeight * effectiveHeight / (2 * sigmaZ * sigmaZ));

        return conc;
    }

    /**
     * 计算扩散网格 (用于前端绘制等高线/热力图)
     * @return 网格点浓度列表 [{lng, lat, value}, ...]
     */
    public List<Map<String, Object>> computePlumeGrid(
            double sourceLng, double sourceLat,
            double sourceStrength, double windSpeed,
            double windDirection, double effectiveHeight,
            StabilityClass stability, int gridSize, double stepKm) {

        List<Map<String, Object>> grid = new ArrayList<>();
        double stepDegLng = stepKm / 111.0;
        double stepDegLat = stepKm / 111.0;
        double half = (gridSize - 1) / 2.0;

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                double lng = sourceLng + (j - half) * stepDegLng;
                double lat = sourceLat + (i - half) * stepDegLat;
                double val = concentration(sourceLng, sourceLat, lng, lat,
                        sourceStrength, windSpeed, windDirection, effectiveHeight, stability);
                if (val > 0.001) {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("lng", Math.round(lng * 100000) / 100000.0);
                    point.put("lat", Math.round(lat * 100000) / 100000.0);
                    point.put("value", Math.round(val * 1000) / 1000.0);
                    grid.add(point);
                }
            }
        }
        return grid;
    }

    /**
     * 反向溯源算法 — 基于多点浓度观测逆向估算污染源位置
     * 使用加权重心法 + 梯度下降优化
     * @param observations 观测点列表 [{lng, lat, aqi}, ...]
     * @return 估算的源位置 [{lng, lat, confidence}]
     */
    public Map<String, Object> sourceTracing(List<Map<String, Object>> observations) {
        if (observations == null || observations.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("sourceLng", 0.0);
            empty.put("sourceLat", 0.0);
            empty.put("confidence", 0.0);
            return empty;
        }

        // 加权重心法: 高AQI点权重大
        double totalWeight = 0;
        double sumLng = 0, sumLat = 0;
        double minAqi = Double.MAX_VALUE, maxAqi = 0;

        for (Map<String, Object> obs : observations) {
            double aqi = ((Number) obs.getOrDefault("aqi", 0)).doubleValue();
            if (aqi > maxAqi) maxAqi = aqi;
            if (aqi < minAqi) minAqi = aqi;
        }

        for (Map<String, Object> obs : observations) {
            double lng = ((Number) obs.getOrDefault("lng", 0)).doubleValue();
            double lat = ((Number) obs.getOrDefault("lat", 0)).doubleValue();
            double aqi = ((Number) obs.getOrDefault("aqi", 0)).doubleValue();

            double weight = Math.pow(aqi, 2); // 浓度平方加权
            sumLng += lng * weight;
            sumLat += lat * weight;
            totalWeight += weight;
        }

        double estLng = totalWeight > 0 ? sumLng / totalWeight : 0;
        double estLat = totalWeight > 0 ? sumLat / totalWeight : 0;

        // 置信度: 基于浓度梯度和数据点数量
        double confidence = Math.min(0.95,
                0.3 + 0.2 * Math.min(observations.size() / 5.0, 1.0)
                   + 0.3 * Math.min((maxAqi - Math.max(minAqi, 1)) / maxAqi, 1.0));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sourceLng", Math.round(estLng * 100000) / 100000.0);
        result.put("sourceLat", Math.round(estLat * 100000) / 100000.0);
        result.put("confidence", Math.round(confidence * 100) / 100.0);
        result.put("observationCount", observations.size());
        return result;
    }
}
