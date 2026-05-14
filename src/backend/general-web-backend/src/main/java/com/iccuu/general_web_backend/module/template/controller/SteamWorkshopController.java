package com.iccuu.general_web_backend.module.template.controller;

import com.iccuu.general_web_backend.common.result.R;
import com.iccuu.general_web_backend.module.template.service.SteamWorkshopCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workshop")
@RequiredArgsConstructor
public class SteamWorkshopController {

    private final SteamWorkshopCacheService cacheService;

    @GetMapping("/hot")
    public R<List<Map<String, Object>>> hotMods() {
        return R.ok(cacheService.getHotMods());
    }

    @GetMapping("/search")
    public R<List<Map<String, Object>>> search(@RequestParam(required = false) String keyword) {
        return R.ok(cacheService.searchCached(keyword));
    }
}
