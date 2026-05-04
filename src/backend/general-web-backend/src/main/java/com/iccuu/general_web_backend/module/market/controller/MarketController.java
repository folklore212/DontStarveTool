package com.iccuu.general_web_backend.module.market.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iccuu.general_web_backend.common.result.PageResult;
import com.iccuu.general_web_backend.common.result.R;
import com.iccuu.general_web_backend.common.util.SecurityUtil;
import com.iccuu.general_web_backend.module.market.entity.MarketConfig;
import com.iccuu.general_web_backend.module.market.mapper.MarketConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/marketplace")
@RequiredArgsConstructor
public class MarketController {

    private final MarketConfigMapper mapper;

    @GetMapping
    public R<PageResult<MarketConfig>> browse(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort) {

        var qw = new LambdaQueryWrapper<MarketConfig>().eq(MarketConfig::getStatus, "published");
        if (category != null) qw.eq(MarketConfig::getCategory, category);
        if ("downloads".equals(sort)) qw.orderByDesc(MarketConfig::getDownloadCount);
        else if ("rating".equals(sort)) qw.orderByDesc(MarketConfig::getRatingAvg);
        else qw.orderByDesc(MarketConfig::getCreatedAt);

        Page<MarketConfig> result = mapper.selectPage(new Page<>(page, size), qw);
        return R.ok(PageResult.of(result.getTotal(), page, size, result.getRecords()));
    }

    @GetMapping("/{id}")
    public R<MarketConfig> detail(@PathVariable Long id) {
        return R.ok(mapper.selectById(id));
    }

    @PostMapping
    public R<MarketConfig> publish(@RequestBody MarketConfig config) {
        config.setAuthorId(SecurityUtil.getCurrentUserId());
        config.setStatus("published");
        config.setDownloadCount(0);
        config.setRatingCount(0);
        config.setVersion(1);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        mapper.insert(config);
        return R.ok(config);
    }

    @PutMapping("/{id}")
    public R<MarketConfig> update(@PathVariable Long id, @RequestBody MarketConfig config) {
        MarketConfig existing = mapper.selectById(id);
        if (existing == null || !existing.getAuthorId().equals(SecurityUtil.getCurrentUserId()))
            return R.fail(403, "Not authorized");
        config.setId(id);
        config.setVersion(existing.getVersion() + 1);
        config.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(config);
        return R.ok(mapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public R<Void> unpublish(@PathVariable Long id) {
        MarketConfig existing = mapper.selectById(id);
        if (existing == null || !existing.getAuthorId().equals(SecurityUtil.getCurrentUserId()))
            return R.fail(403, "Not authorized");
        existing.setStatus("archived");
        mapper.updateById(existing);
        return R.ok();
    }

    @PostMapping("/{id}/fork")
    public R<MarketConfig> fork(@PathVariable Long id) {
        MarketConfig original = mapper.selectById(id);
        if (original == null) return R.fail(404, "Not found");
        MarketConfig fork = new MarketConfig();
        fork.setAuthorId(SecurityUtil.getCurrentUserId());
        fork.setTitle(original.getTitle() + " (Fork)");
        fork.setDescription(original.getDescription());
        fork.setTags(original.getTags());
        fork.setConfigJson(original.getConfigJson());
        fork.setModList(original.getModList());
        fork.setCategory(original.getCategory());
        fork.setGameMode(original.getGameMode());
        fork.setDownloadCount(0);
        fork.setRatingCount(0);
        fork.setVersion(1);
        fork.setStatus("published");
        fork.setCreatedAt(LocalDateTime.now());
        fork.setUpdatedAt(LocalDateTime.now());
        mapper.insert(fork);
        original.setDownloadCount((original.getDownloadCount() != null ? original.getDownloadCount() : 0) + 1);
        mapper.updateById(original);
        return R.ok(fork);
    }

    @PostMapping("/{id}/review")
    public R<Void> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        MarketConfig config = mapper.selectById(id);
        if (config == null) return R.fail(404, "Not found");
        int rating = body.get("rating") instanceof Integer i ? i : 5;
        int oldCount = config.getRatingCount() != null ? config.getRatingCount() : 0;
        var oldAvg = config.getRatingAvg() != null ? config.getRatingAvg() : java.math.BigDecimal.ZERO;
        var newAvg = oldAvg.multiply(java.math.BigDecimal.valueOf(oldCount))
                .add(java.math.BigDecimal.valueOf(rating))
                .divide(java.math.BigDecimal.valueOf(oldCount + 1), 2, java.math.RoundingMode.HALF_UP);
        config.setRatingAvg(newAvg);
        config.setRatingCount(oldCount + 1);
        mapper.updateById(config);
        return R.ok();
    }

    @PostMapping("/{id}/deploy")
    public R<Map<String, Object>> deploy(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        MarketConfig config = mapper.selectById(id);
        if (config == null) return R.fail(404, "Not found");
        config.setDownloadCount((config.getDownloadCount() != null ? config.getDownloadCount() : 0) + 1);
        mapper.updateById(config);
        return R.ok(Map.of("config", config.getConfigJson(), "mods", config.getModList(),
                "serverId", body.getOrDefault("serverId", 0)));
    }
}
