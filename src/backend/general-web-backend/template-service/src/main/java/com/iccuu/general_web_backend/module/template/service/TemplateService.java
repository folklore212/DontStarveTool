package com.iccuu.general_web_backend.module.template.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iccuu.general_web_backend.common.exception.BusinessException;
import com.iccuu.general_web_backend.common.util.SecurityUtil;
import com.iccuu.general_web_backend.module.template.entity.Template;
import com.iccuu.general_web_backend.module.template.entity.TemplateWorldGenBinding;
import com.iccuu.general_web_backend.module.template.entity.WorldGenPreset;
import com.iccuu.general_web_backend.module.template.enums.ShardType;
import com.iccuu.general_web_backend.module.template.enums.TemplateStatus;
import com.iccuu.general_web_backend.module.template.mapper.TemplateMapper;
import com.iccuu.general_web_backend.module.template.mapper.TemplateWorldGenBindingMapper;
import com.iccuu.general_web_backend.module.template.mapper.WorldGenPresetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateMapper templateMapper;
    private final WorldGenPresetMapper worldGenPresetMapper;
    private final TemplateWorldGenBindingMapper bindingMapper;

    public Page<Template> browse(String type, String category, String sort, int page, int size) {
        var qw = new LambdaQueryWrapper<Template>()
                .eq(Template::getStatus, TemplateStatus.PUBLISHED.getValue())
                .eq(Template::getDeletedAt, 0L);
        if (type != null) qw.eq(Template::getTemplateType, type);
        if (category != null) qw.eq(Template::getCategory, category);
        if ("downloads".equals(sort)) qw.orderByDesc(Template::getDownloadCount);
        else if ("rating".equals(sort)) qw.orderByDesc(Template::getRatingAvg);
        else qw.orderByDesc(Template::getCreatedAt);
        return templateMapper.selectPage(new Page<>(page, size), qw);
    }

    public Template detail(Long id) {
        return templateMapper.selectById(id);
    }

    @Transactional
    public Template create(Template input) {
        Template template = newTemplate(input);
        template.setAuthorId(SecurityUtil.getCurrentUserId());
        templateMapper.insert(template);
        return template;
    }

    @Transactional
    public Template update(Long id, Template input) {
        Template existing = templateMapper.selectById(id);
        if (existing == null || !existing.getAuthorId().equals(SecurityUtil.getCurrentUserId()))
            throw new BusinessException(403, "Not authorized");
        var uw = new LambdaUpdateWrapper<Template>().eq(Template::getId, id);
        if (input.getName() != null) uw.set(Template::getName, input.getName());
        if (input.getDescription() != null) uw.set(Template::getDescription, input.getDescription());
        if (input.getTemplateType() != null) uw.set(Template::getTemplateType, input.getTemplateType());
        if (input.getCategory() != null) uw.set(Template::getCategory, input.getCategory());
        if (input.getGameMode() != null) uw.set(Template::getGameMode, input.getGameMode());
        if (input.getMaxPlayers() != null) uw.set(Template::getMaxPlayers, input.getMaxPlayers());
        if (input.getTags() != null) uw.set(Template::getTags, input.getTags());
        if (input.getCoverImage() != null) uw.set(Template::getCoverImage, input.getCoverImage());
        if (input.getConfigJson() != null) uw.set(Template::getConfigJson, input.getConfigJson());
        if (input.getModList() != null) uw.set(Template::getModList, input.getModList());
        uw.set(Template::getVersion, (existing.getVersion() != null ? existing.getVersion() : 0) + 1);
        uw.set(Template::getUpdatedAt, LocalDateTime.now());
        templateMapper.update(null, uw);
        return templateMapper.selectById(id);
    }

    public void delete(Long id) {
        Template existing = templateMapper.selectById(id);
        if (existing == null || !existing.getAuthorId().equals(SecurityUtil.getCurrentUserId()))
            throw new BusinessException(403, "Not authorized");
        // Archive: keep db row but hide from default listings. @TableLogic handles deleteById callers.
        existing.setStatus(TemplateStatus.ARCHIVED.getValue());
        existing.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(existing);
    }

    @Transactional
    public Template fork(Long id) {
        Template original = templateMapper.selectById(id);
        if (original == null) throw new BusinessException(404, "Template not found: " + id);
        Template fork = newTemplate(original);
        fork.setName(original.getName() + " (Fork)");
        fork.setAuthorId(SecurityUtil.getCurrentUserId());
        templateMapper.insert(fork);
        original.setDownloadCount((original.getDownloadCount() != null ? original.getDownloadCount() : 0) + 1);
        templateMapper.updateById(original);
        return fork;
    }

    private Template newTemplate(Template source) {
        Template t = new Template();
        t.setName(source.getName());
        t.setDescription(source.getDescription());
        t.setTemplateType(source.getTemplateType());
        t.setCategory(source.getCategory());
        t.setGameMode(source.getGameMode());
        t.setMaxPlayers(source.getMaxPlayers());
        t.setTags(source.getTags());
        t.setCoverImage(source.getCoverImage());
        t.setConfigJson(source.getConfigJson());
        t.setModList(source.getModList());
        t.setStatus(TemplateStatus.PUBLISHED.getValue());
        t.setVerified(0);
        t.setVersion(1);
        t.setDownloadCount(0);
        t.setRatingCount(0);
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        return t;
    }

    public List<WorldGenPreset> getBoundWorldGen(Long serverTemplateId) {
        var bindings = bindingMapper.selectList(
                new LambdaQueryWrapper<TemplateWorldGenBinding>()
                        .eq(TemplateWorldGenBinding::getServerTemplateId, serverTemplateId)
                        .orderByAsc(TemplateWorldGenBinding::getSortOrder));
        if (bindings.isEmpty()) return List.of();
        var presetIds = bindings.stream().map(TemplateWorldGenBinding::getWorldGenPresetId).toList();
        var presets = worldGenPresetMapper.selectBatchIds(presetIds);
        var presetMap = presets.stream().collect(Collectors.toMap(WorldGenPreset::getId, p -> p));
        return bindings.stream()
                .map(b -> presetMap.get(b.getWorldGenPresetId()))
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    public void bindWorldGen(Long serverTemplateId, List<Map<String, Object>> bindings) {
        List<Long> presetIds = new ArrayList<>();
        for (var b : bindings) {
            Object rawId = b.get("presetId");
            if (rawId == null) throw new BusinessException(400, "presetId is required for each binding");
            presetIds.add(Long.valueOf(rawId.toString()));
        }
        var existing = worldGenPresetMapper.selectBatchIds(presetIds);
        var existingIds = existing.stream().map(WorldGenPreset::getId).collect(Collectors.toSet());
        for (Long id : presetIds) {
            if (!existingIds.contains(id))
                throw new BusinessException(404, "World gen preset not found: " + id);
        }

        bindingMapper.delete(new LambdaQueryWrapper<TemplateWorldGenBinding>()
                .eq(TemplateWorldGenBinding::getServerTemplateId, serverTemplateId));
        int order = 0;
        for (var b : bindings) {
            TemplateWorldGenBinding binding = new TemplateWorldGenBinding();
            binding.setServerTemplateId(serverTemplateId);
            binding.setWorldGenPresetId(Long.valueOf(b.get("presetId").toString()));
            binding.setShardType(ShardType.MASTER.getValue());
            Object rawShard = b.get("shardType");
            if (rawShard != null) binding.setShardType(rawShard.toString());
            binding.setSortOrder(order++);
            binding.setCreatedAt(LocalDateTime.now());
            bindingMapper.insert(binding);
        }
    }

    public Map<String, Object> fullDetail(Long id) {
        Template template = detail(id);
        if (template == null) return null;
        List<WorldGenPreset> presets = getBoundWorldGen(id);
        return Map.of("template", template, "worldGenPresets", presets);
    }
}
