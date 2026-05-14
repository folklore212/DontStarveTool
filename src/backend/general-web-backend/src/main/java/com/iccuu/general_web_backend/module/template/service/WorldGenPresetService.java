package com.iccuu.general_web_backend.module.template.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iccuu.general_web_backend.module.template.entity.WorldGenPreset;
import com.iccuu.general_web_backend.module.template.mapper.WorldGenPresetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorldGenPresetService {

    private final WorldGenPresetMapper presetMapper;

    public List<WorldGenPreset> listAll() {
        return presetMapper.selectList(
                new LambdaQueryWrapper<WorldGenPreset>()
                        .eq(WorldGenPreset::getDeletedAt, 0L)
                        .orderByAsc(WorldGenPreset::getSortOrder));
    }

    public Page<WorldGenPreset> browse(int page, int size) {
        return presetMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<WorldGenPreset>()
                        .eq(WorldGenPreset::getDeletedAt, 0L)
                        .orderByAsc(WorldGenPreset::getSortOrder));
    }

    public WorldGenPreset detail(Long id) {
        return presetMapper.selectById(id);
    }

    public WorldGenPreset create(WorldGenPreset preset) {
        preset.setCreatedAt(LocalDateTime.now());
        preset.setUpdatedAt(LocalDateTime.now());
        presetMapper.insert(preset);
        return preset;
    }

    public WorldGenPreset update(Long id, WorldGenPreset data) {
        data.setId(id);
        data.setUpdatedAt(LocalDateTime.now());
        presetMapper.updateById(data);
        return presetMapper.selectById(id);
    }

    public void delete(Long id) {
        var existing = presetMapper.selectById(id);
        if (existing != null) {
            existing.setDeletedAt(System.currentTimeMillis());
            presetMapper.updateById(existing);
        }
    }

    private static final Map<String, Object> PRESET_METADATA = new HashMap<>();
    static {
        PRESET_METADATA.put("worldSize", Map.of(
            "label", "World Size",
            "icon", "/images/worldgen/world_size.png",
            "options", List.of(
                Map.of("value", "small", "label", "Small", "icon", "/images/worldgen/size_small.png"),
                Map.of("value", "medium", "label", "Medium", "icon", "/images/worldgen/size_medium.png"),
                Map.of("value", "default", "label", "Default", "icon", "/images/worldgen/size_default.png"),
                Map.of("value", "large", "label", "Large", "icon", "/images/worldgen/size_large.png"),
                Map.of("value", "huge", "label", "Huge", "icon", "/images/worldgen/size_huge.png")
            )
        ));
        PRESET_METADATA.put("branching", Map.of(
            "label", "Branching",
            "icon", "/images/worldgen/branching.png",
            "options", List.of(
                Map.of("value", "never", "label", "Never", "icon", "/images/worldgen/branch_never.png"),
                Map.of("value", "least", "label", "Least", "icon", "/images/worldgen/branch_least.png"),
                Map.of("value", "default", "label", "Default", "icon", "/images/worldgen/branch_default.png"),
                Map.of("value", "most", "label", "Most", "icon", "/images/worldgen/branch_most.png"),
                Map.of("value", "random", "label", "Random", "icon", "/images/worldgen/branch_random.png")
            )
        ));
        PRESET_METADATA.put("loopMode", Map.of(
            "label", "Loop",
            "icon", "/images/worldgen/loop.png",
            "options", List.of(
                Map.of("value", "never", "label", "Never", "icon", "/images/worldgen/loop_never.png"),
                Map.of("value", "default", "label", "Default", "icon", "/images/worldgen/loop_default.png"),
                Map.of("value", "always", "label", "Always", "icon", "/images/worldgen/loop_always.png")
            )
        ));
        PRESET_METADATA.put("seasonStart", Map.of(
            "label", "Starting Season",
            "icon", "/images/worldgen/season.png",
            "options", List.of(
                Map.of("value", "default", "label", "Autumn", "icon", "/images/worldgen/season_autumn.png"),
                Map.of("value", "winter", "label", "Winter", "icon", "/images/worldgen/season_winter.png"),
                Map.of("value", "spring", "label", "Spring", "icon", "/images/worldgen/season_spring.png"),
                Map.of("value", "summer", "label", "Summer", "icon", "/images/worldgen/season_summer.png")
            )
        ));
        PRESET_METADATA.put("dayMode", Map.of(
            "label", "Day/Night Cycle",
            "icon", "/images/worldgen/day_cycle.png",
            "options", List.of(
                Map.of("value", "default", "label", "Default", "icon", "/images/worldgen/day_default.png"),
                Map.of("value", "longday", "label", "Long Day", "icon", "/images/worldgen/day_long.png"),
                Map.of("value", "longdusk", "label", "Long Dusk", "icon", "/images/worldgen/day_dusk.png"),
                Map.of("value", "longnight", "label", "Long Night", "icon", "/images/worldgen/day_night.png"),
                Map.of("value", "onlyday", "label", "Only Day", "icon", "/images/worldgen/day_only.png"),
                Map.of("value", "onlynight", "label", "Only Night", "icon", "/images/worldgen/night_only.png")
            )
        ));
        PRESET_METADATA.put("autumnLength", Map.of(
            "label", "Autumn Length",
            "icon", "/images/worldgen/autumn.png",
            "options", List.of(
                Map.of("value", "noseason", "label", "None", "icon", "/images/worldgen/len_none.png"),
                Map.of("value", "veryshortseason", "label", "Very Short", "icon", "/images/worldgen/len_veryshort.png"),
                Map.of("value", "shortseason", "label", "Short", "icon", "/images/worldgen/len_short.png"),
                Map.of("value", "default", "label", "Default", "icon", "/images/worldgen/len_default.png"),
                Map.of("value", "longseason", "label", "Long", "icon", "/images/worldgen/len_long.png"),
                Map.of("value", "verylongseason", "label", "Very Long", "icon", "/images/worldgen/len_verylong.png"),
                Map.of("value", "random", "label", "Random", "icon", "/images/worldgen/len_random.png")
            )
        ));
        PRESET_METADATA.put("resourceVariety", Map.of(
            "label", "Resource Variety",
            "icon", "/images/worldgen/resources.png",
            "options", List.of(
                Map.of("value", "classic", "label", "Classic", "icon", "/images/worldgen/res_classic.png"),
                Map.of("value", "default", "label", "Default", "icon", "/images/worldgen/res_default.png"),
                Map.of("value", "highlyrandom", "label", "Highly Random", "icon", "/images/worldgen/res_random.png")
            )
        ));
        PRESET_METADATA.put("creatures", Map.of(
            "label", "Creatures",
            "icon", "/images/worldgen/creatures.png",
            "options", List.of(
                Map.of("value", "none", "label", "None", "icon", "/images/worldgen/creatures_none.png"),
                Map.of("value", "less", "label", "Less", "icon", "/images/worldgen/creatures_less.png"),
                Map.of("value", "default", "label", "Default", "icon", "/images/worldgen/creatures_default.png"),
                Map.of("value", "more", "label", "More", "icon", "/images/worldgen/creatures_more.png")
            )
        ));
        PRESET_METADATA.put("boons", Map.of(
            "label", "Boons & Set Pieces",
            "icon", "/images/worldgen/boons.png",
            "options", List.of(
                Map.of("value", "none", "label", "None", "icon", "/images/worldgen/boons_none.png"),
                Map.of("value", "less", "label", "Less", "icon", "/images/worldgen/boons_less.png"),
                Map.of("value", "default", "label", "Default", "icon", "/images/worldgen/boons_default.png"),
                Map.of("value", "more", "label", "More", "icon", "/images/worldgen/boons_more.png")
            )
        ));
        PRESET_METADATA.put("weather", Map.of(
            "label", "Weather",
            "icon", "/images/worldgen/weather.png",
            "options", List.of(
                Map.of("value", "none", "label", "None", "icon", "/images/worldgen/weather_none.png"),
                Map.of("value", "less", "label", "Less", "icon", "/images/worldgen/weather_less.png"),
                Map.of("value", "default", "label", "Default", "icon", "/images/worldgen/weather_default.png"),
                Map.of("value", "more", "label", "More", "icon", "/images/worldgen/weather_more.png")
            )
        ));
        PRESET_METADATA.put("regrowth", Map.of(
            "label", "Regrowth Speed",
            "icon", "/images/worldgen/regrowth.png",
            "options", List.of(
                Map.of("value", "slow", "label", "Slow", "icon", "/images/worldgen/regrowth_slow.png"),
                Map.of("value", "default", "label", "Default", "icon", "/images/worldgen/regrowth_default.png"),
                Map.of("value", "fast", "label", "Fast", "icon", "/images/worldgen/regrowth_fast.png")
            )
        ));
        PRESET_METADATA.put("startingGear", Map.of(
            "label", "Starting Gear",
            "icon", "/images/worldgen/starting_gear.png",
            "options", List.of(
                Map.of("value", "classic", "label", "Classic", "icon", "/images/worldgen/gear_classic.png"),
                Map.of("value", "default", "label", "Default", "icon", "/images/worldgen/gear_default.png"),
                Map.of("value", "highlyrandom", "label", "Highly Random", "icon", "/images/worldgen/gear_random.png")
            )
        ));
    }

    public Map<String, Object> getPresetMetadata() {
        return PRESET_METADATA;
    }
}
