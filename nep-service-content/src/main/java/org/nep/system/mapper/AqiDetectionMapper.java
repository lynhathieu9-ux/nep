package org.nep.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Select;
import org.nep.system.entity.AqiDetection;

import java.util.List;
import java.util.Map;

/**
 * AQI检测数据 Mapper
 * 支持污染热点地图所需的聚合查询
 */
public interface AqiDetectionMapper extends BaseMapper<AqiDetection> {

    /**
     * 按城市聚合 AQI 检测数据（最近30天）
     * 返回字段: cityId, cityName, provinceId, avgAqi, maxAqi, detectionCount
     */
    @Select("""
        SELECT
            ad.city_id AS cityId,
            c.name AS cityName,
            ad.province_id AS provinceId,
            p.name AS provinceName,
            ROUND(AVG(ad.final_aqi), 1) AS avgAqi,
            MAX(ad.final_aqi) AS maxAqi,
            COUNT(*) AS detectionCount
        FROM nep_aqi_detection ad
        LEFT JOIN nep_city c ON ad.city_id = c.id
        LEFT JOIN nep_province p ON ad.province_id = p.id
        WHERE ad.deleted = 0
          AND ad.detection_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
        GROUP BY ad.city_id, c.name, ad.province_id, p.name
        ORDER BY avgAqi DESC
        """)
    List<Map<String, Object>> aggregateByCity30Days();

    /**
     * 按省份聚合 AQI 检测数据（最近30天）
     * 返回字段: provinceId, provinceName, avgAqi, maxAqi, totalDetections
     */
    @Select("""
        SELECT
            ad.province_id AS provinceId,
            p.name AS provinceName,
            p.code AS provinceCode,
            ROUND(AVG(ad.final_aqi), 1) AS avgAqi,
            MAX(ad.final_aqi) AS maxAqi,
            COUNT(DISTINCT ad.city_id) AS cityCount,
            COUNT(*) AS totalDetections
        FROM nep_aqi_detection ad
        LEFT JOIN nep_province p ON ad.province_id = p.id
        WHERE ad.deleted = 0
          AND ad.detection_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
        GROUP BY ad.province_id, p.name, p.code
        ORDER BY avgAqi DESC
        """)
    List<Map<String, Object>> aggregateByProvince30Days();

    /**
     * 获取有检测数据的城市ID列表（用于布隆过滤器初始化）
     */
    @Select("SELECT DISTINCT city_id FROM nep_aqi_detection WHERE deleted = 0 AND city_id IS NOT NULL")
    List<Long> selectDistinctCityIds();

    /**
     * 获取有检测数据的省份ID列表（用于布隆过滤器初始化）
     */
    @Select("SELECT DISTINCT province_id FROM nep_aqi_detection WHERE deleted = 0 AND province_id IS NOT NULL")
    List<Long> selectDistinctProvinceIds();

    /**
     * 统计最近24小时活跃的网格员数（去重）
     */
    @Select("SELECT COUNT(DISTINCT inspector_id) FROM nep_aqi_detection WHERE deleted = 0 AND detection_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR)")
    long countActiveInspectors24h();

    /**
     * 按省份分组统计各污染物超标累计数量（需求书 NEPM/NEPV 统计要求）。
     * <p>单项 AQI 分指数 > 100 视为该污染物超标。返回：
     * provinceId, provinceName, so2Exceed(SO2超标数), coExceed(CO超标数),
     * pm25Exceed(PM2.5超标数), aqiExceed(综合AQI等级超标数), total(检测总数)。</p>
     */
    @Select("""
        SELECT
            ad.province_id AS provinceId,
            p.name AS provinceName,
            SUM(CASE WHEN ad.so2_aqi > 100 THEN 1 ELSE 0 END) AS so2Exceed,
            SUM(CASE WHEN ad.co_aqi > 100 THEN 1 ELSE 0 END) AS coExceed,
            SUM(CASE WHEN ad.pm25_aqi > 100 THEN 1 ELSE 0 END) AS pm25Exceed,
            SUM(CASE WHEN ad.final_aqi > 100 THEN 1 ELSE 0 END) AS aqiExceed,
            COUNT(*) AS total
        FROM nep_aqi_detection ad
        LEFT JOIN nep_province p ON ad.province_id = p.id
        WHERE ad.deleted = 0
        GROUP BY ad.province_id, p.name
        ORDER BY aqiExceed DESC, total DESC
        """)
    List<Map<String, Object>> aggregatePollutantExceedByProvince();
}
