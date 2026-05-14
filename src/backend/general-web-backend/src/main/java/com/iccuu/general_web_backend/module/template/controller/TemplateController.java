package com.iccuu.general_web_backend.module.template.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iccuu.general_web_backend.common.result.PageResult;
import com.iccuu.general_web_backend.common.result.R;
import com.iccuu.general_web_backend.module.template.entity.Template;
import com.iccuu.general_web_backend.module.template.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public R<PageResult<Template>> browse(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort) {
        Page<Template> result = templateService.browse(type, category, sort, page, size);
        return R.ok(PageResult.of(result.getTotal(), page, size, result.getRecords()));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        Map<String, Object> full = templateService.fullDetail(id);
        if (full == null) return R.fail(404, "Template not found");
        return R.ok(full);
    }

    @PostMapping
    public R<Template> create(@RequestBody Template template) {
        return R.ok(templateService.create(template));
    }

    @PutMapping("/{id}")
    public R<Template> update(@PathVariable Long id, @RequestBody Template template) {
        return R.ok(templateService.update(id, template));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return R.ok();
    }

    @PostMapping("/{id}/fork")
    public R<Template> fork(@PathVariable Long id) {
        return R.ok(templateService.fork(id));
    }

    @GetMapping("/{id}/world-gen")
    public R<List<?>> getBoundWorldGen(@PathVariable Long id) {
        return R.ok(templateService.getBoundWorldGen(id));
    }

    @PutMapping("/{id}/world-gen")
    public R<Void> bindWorldGen(@PathVariable Long id, @RequestBody List<Map<String, Object>> bindings) {
        templateService.bindWorldGen(id, bindings);
        return R.ok();
    }
}
