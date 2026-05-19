package com.iccuu.general_web_backend.module.server.controller;

import com.iccuu.general_web_backend.common.result.R;
import com.iccuu.general_web_backend.module.server.entity.DstCluster;
import com.iccuu.general_web_backend.module.server.service.ClusterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/servers/{serverId}/clusters")
@RequiredArgsConstructor
public class DstClusterController {

    private final ClusterService clusterService;

    @GetMapping
    public R<List<DstCluster>> list(@PathVariable Long serverId) {
        return R.ok(clusterService.listClusters(serverId));
    }

    @PostMapping
    public R<DstCluster> create(@PathVariable Long serverId, @RequestBody DstCluster cluster) {
        return R.ok(clusterService.createCluster(serverId, cluster));
    }

    @DeleteMapping("/{clusterId}")
    public R<Void> deleteCluster(@PathVariable Long serverId, @PathVariable Long clusterId) {
        clusterService.deleteCluster(serverId, clusterId);
        return R.ok();
    }

    @PostMapping("/{clusterId}/install")
    public R<Map<String, Object>> install(@PathVariable Long serverId, @PathVariable Long clusterId) {
        return R.ok(clusterService.install(serverId, clusterId));
    }

    @PostMapping("/{clusterId}/start")
    public R<Map<String, Object>> start(@PathVariable Long serverId, @PathVariable Long clusterId) {
        return R.ok(clusterService.start(serverId, clusterId));
    }

    @PostMapping("/{clusterId}/stop")
    public R<Map<String, Object>> stop(@PathVariable Long serverId, @PathVariable Long clusterId) {
        return R.ok(clusterService.stop(serverId, clusterId));
    }

    @GetMapping("/{clusterId}/status")
    public R<Map<String, Object>> status(@PathVariable Long serverId, @PathVariable Long clusterId) {
        return R.ok(clusterService.status(serverId, clusterId));
    }

    @PostMapping("/{clusterId}/console")
    public R<Map<String, Object>> sendConsoleCommand(@PathVariable Long serverId, @PathVariable Long clusterId,
                                                      @RequestBody Map<String, String> body) {
        return R.ok(clusterService.sendConsoleCommand(serverId, clusterId, body.getOrDefault("command", "")));
    }

    @PostMapping("/{clusterId}/backup")
    public R<Map<String, Object>> createBackup(@PathVariable Long serverId, @PathVariable Long clusterId,
                                                @RequestBody Map<String, String> body) {
        return R.ok(clusterService.createBackup(serverId, clusterId, body.getOrDefault("name", "backup")));
    }

    @PostMapping("/{clusterId}/mods/search")
    public R<Map<String, Object>> searchMods(@PathVariable Long serverId, @PathVariable Long clusterId,
                                              @RequestBody Map<String, String> body) {
        return R.ok(clusterService.searchMods(serverId, clusterId, body.getOrDefault("keyword", "")));
    }

    @GetMapping("/{clusterId}/mods")
    public R<Map<String, Object>> listMods(@PathVariable Long serverId, @PathVariable Long clusterId) {
        return R.ok(clusterService.listMods(serverId, clusterId));
    }

    @PostMapping("/{clusterId}/mods/install")
    public R<Map<String, Object>> installMod(@PathVariable Long serverId, @PathVariable Long clusterId,
                                              @RequestBody Map<String, String> body) {
        return R.ok(clusterService.installMod(serverId, clusterId, body.getOrDefault("workshopId", "")));
    }

    @GetMapping("/{clusterId}/players")
    public R<Map<String, Object>> listPlayers(@PathVariable Long serverId, @PathVariable Long clusterId) {
        return R.ok(clusterService.listPlayers(serverId, clusterId));
    }

    @PostMapping("/{clusterId}/players/kick")
    public R<Map<String, Object>> kickPlayer(@PathVariable Long serverId, @PathVariable Long clusterId,
                                              @RequestBody Map<String, String> body) {
        return R.ok(clusterService.kickPlayer(serverId, clusterId, body.getOrDefault("steamId", "")));
    }

    @PostMapping("/{clusterId}/players/ban")
    public R<Map<String, Object>> banPlayer(@PathVariable Long serverId, @PathVariable Long clusterId,
                                             @RequestBody Map<String, String> body) {
        return R.ok(clusterService.banPlayer(serverId, clusterId, body.getOrDefault("steamId", "")));
    }

    @GetMapping("/{clusterId}/adminlist")
    public R<List<String>> getAdminList(@PathVariable Long serverId, @PathVariable Long clusterId) {
        return R.ok(clusterService.getAdminList(serverId, clusterId));
    }

    @PutMapping("/{clusterId}/adminlist")
    public R<Void> setAdminList(@PathVariable Long serverId, @PathVariable Long clusterId,
                                 @RequestBody List<String> admins) {
        clusterService.setAdminList(serverId, clusterId, admins);
        return R.ok();
    }

    @GetMapping("/{clusterId}/health")
    public R<Map<String, Object>> health(@PathVariable Long serverId, @PathVariable Long clusterId) {
        return R.ok(Map.of("id", System.currentTimeMillis(), "created", true));
    }

    @GetMapping("/{clusterId}/schedules")
    public R<List<Map<String, Object>>> listSchedules(@PathVariable Long serverId, @PathVariable Long clusterId) {
        return R.ok(List.of());
    }

    @PostMapping("/{clusterId}/schedules")
    public R<Map<String, Object>> createSchedule(@PathVariable Long serverId, @PathVariable Long clusterId) {
        return R.ok(Map.of("id", System.currentTimeMillis(), "created", true));
    }
}
