package org.nep.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nep.common.result.Result;
import org.nep.system.entity.City;
import org.nep.system.entity.Province;
import org.nep.system.mapper.CityMapper;
import org.nep.system.mapper.ProvinceMapper;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "区域管理")
@RestController
@RequestMapping("/api/region")
public class RegionController {

    private final ProvinceMapper provinceMapper;
    private final CityMapper cityMapper;
    public RegionController(ProvinceMapper pm, CityMapper cm) { this.provinceMapper = pm; this.cityMapper = cm; }

    @Operation(summary = "所有省份")
    @GetMapping("/provinces")
    public Result<List<Province>> provinces() {
        LambdaQueryWrapper<Province> w = new LambdaQueryWrapper<>();
        w.orderByAsc(Province::getSortOrder);
        return Result.ok(provinceMapper.selectList(w));
    }

    @Operation(summary = "省份下城市")
    @GetMapping("/cities/{provinceId}")
    public Result<List<City>> cities(@PathVariable Long provinceId) {
        LambdaQueryWrapper<City> w = new LambdaQueryWrapper<>();
        w.eq(City::getProvinceId, provinceId).orderByAsc(City::getSortOrder);
        return Result.ok(cityMapper.selectList(w));
    }

    @Operation(summary = "所有城市")
    @GetMapping("/cities")
    public Result<List<City>> allCities() { return Result.ok(cityMapper.selectList(null)); }
}
