package com.iccuu.general_web_backend.module.template.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iccuu.general_web_backend.common.result.PageResult;
import com.iccuu.general_web_backend.common.result.R;
import com.iccuu.general_web_backend.module.template.entity.WorldGenPreset;
import com.iccuu.general_web_backend.module.template.service.WorldGenPresetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/worldgen")
@RequiredArgsConstructor
public class WorldGenController {

    private final WorldGenPresetService presetService;

    @GetMapping("/metadata")
    public R<Map<String, Object>> metadata() {
        return R.ok(presetService.getPresetMetadata());
    }

    @GetMapping
    public R<PageResult<WorldGenPreset>> browse(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<WorldGenPreset> result = presetService.browse(page, size);
        return R.ok(PageResult.of(result.getTotal(), page, size, result.getRecords()));
    }

    @GetMapping("/{id}")
    public R<WorldGenPreset> detail(@PathVariable Long id) {
        return R.ok(presetService.detail(id));
    }

    @PostMapping
    public R<WorldGenPreset> create(@RequestBody WorldGenPreset preset) {
        return R.ok(presetService.create(preset));
    }

    @PutMapping("/{id}")
    public R<WorldGenPreset> update(@PathVariable Long id, @RequestBody WorldGenPreset preset) {
        return R.ok(presetService.update(id, preset));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        presetService.delete(id);
        return R.ok();
    }
}
