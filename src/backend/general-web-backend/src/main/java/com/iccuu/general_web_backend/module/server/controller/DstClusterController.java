package com.iccuu.general_web_backend.module.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iccuu.general_web_backend.common.result.R;
import com.iccuu.general_web_backend.common.util.SecurityUtil;
import com.iccuu.general_web_backend.infrastructure.ssh.DstDeployService;
import com.iccuu.general_web_backend.module.server.entity.DstCluster;
import com.iccuu.general_web_backend.module.server.entity.Server;
import com.iccuu.general_web_backend.module.server.mapper.DstClusterMapper;
import com.iccuu.general_web_backend.module.server.mapper.ServerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/servers/{serverId}/clusters")
@RequiredArgsConstructor
public class DstClusterController {

    private final ServerMapper serverMapper;
    private final DstClusterMapper clusterMapper;
    private final DstDeployService dstDeployService;
    private final com.iccuu.general_web_backend.infrastructure.ssh.SshService sshService;
    private final com.iccuu.general_web_backend.infrastructure.steam.SteamApiService steamApiService;

    private Server requireServer(Long serverId) {
        Server s = serverMapper.selectById(serverId);
        if (s == null || !s.getUserId().equals(SecurityUtil.getCurrentUserId()))
            throw new RuntimeException("Server not found");
        return s;
    }

    @GetMapping
    public R<List<DstCluster>> list(@PathVariable Long serverId) {
        requireServer(serverId);
        return R.ok(clusterMapper.selectList(
                new LambdaQueryWrapper<DstCluster>().eq(DstCluster::getServerId, serverId)));
    }

    @PostMapping
    public R<DstCluster> create(@PathVariable Long serverId, @RequestBody Map<String, Object> body) {
        Server server = requireServer(serverId);
        DstCluster c = new DstCluster();
        c.setServerId(serverId);
        c.setUserId(SecurityUtil.getCurrentUserId());
        c.setName((String) body.getOrDefault("name", "Cluster_" + System.currentTimeMillis() % 100000));
        c.setDisplayName((String) body.getOrDefault("displayName", c.getName()));
        c.setGameMode((String) body.getOrDefault("gameMode", "survival"));
        c.setMaxPlayers(body.get("maxPlayers") instanceof Integer i ? i : 6);
        c.setMasterPort(body.get("masterPort") instanceof Integer i ? i : 10999);
        c.setSteamPort(body.get("steamPort") instanceof Integer i ? i : 8766);
        c.setStatus("stopped");
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        clusterMapper.insert(c);
        return R.ok(c);
    }

    @DeleteMapping("/{clusterId}")
    public R<Void> delete(@PathVariable Long serverId, @PathVariable Long clusterId) {
        requireServer(serverId);
        clusterMapper.deleteById(clusterId);
        return R.ok();
    }

    @PostMapping("/{clusterId}/install")
    public R<Map<String, Object>> install(@PathVariable Long serverId, @PathVariable Long clusterId) {
        Server server = requireServer(serverId);
        DstCluster cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) return R.fail(404, "Cluster not found");

        var deps = dstDeployService.checkDependencies(server);
        var steamCmd = dstDeployService.installSteamCmd(server);
        var dst = dstDeployService.installDstServer(server);

        return R.ok(Map.of("deps", deps.isSuccess(), "steamCmd", steamCmd.isSuccess(), "dst", dst.isSuccess()));
    }

    @PostMapping("/{clusterId}/start")
    public R<Map<String, Object>> start(@PathVariable Long serverId, @PathVariable Long clusterId) {
        Server server = requireServer(serverId);
        DstCluster cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) return R.fail(404, "Cluster not found");

        // Generate config first
        Map<String, String> config = new HashMap<>();
        config.put("maxPlayers", String.valueOf(cluster.getMaxPlayers()));
        config.put("gameMode", cluster.getGameMode());
        config.put("password", cluster.getPassword() != null ? cluster.getPassword() : "");
        config.put("token", cluster.getClusterToken() != null ? cluster.getClusterToken() : "");
        dstDeployService.generateConfig(server, cluster.getName(), config);

        var result = dstDeployService.startServer(server, cluster.getName());
        cluster.setStatus(result.isSuccess() ? "running" : "error");
        cluster.setUpdatedAt(LocalDateTime.now());
        clusterMapper.updateById(cluster);
        return R.ok(Map.of("success", result.isSuccess(), "output", result.getOutput()));
    }

    @PostMapping("/{clusterId}/stop")
    public R<Map<String, Object>> stop(@PathVariable Long serverId, @PathVariable Long clusterId) {
        Server server = requireServer(serverId);
        DstCluster cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) return R.fail(404, "Cluster not found");

        var result = dstDeployService.stopServer(server, cluster.getName());
        cluster.setStatus("stopped");
        cluster.setUpdatedAt(LocalDateTime.now());
        clusterMapper.updateById(cluster);
        return R.ok(Map.of("success", result.isSuccess()));
    }

    @GetMapping("/{clusterId}/status")
    public R<Map<String, Object>> status(@PathVariable Long serverId, @PathVariable Long clusterId) {
        Server server = requireServer(serverId);
        DstCluster cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) return R.fail(404, "Cluster not found");

        var result = dstDeployService.checkStatus(server, cluster.getName());
        return R.ok(Map.of("status", cluster.getStatus(), "output", result.getOutput()));
    }

    @PostMapping("/{clusterId}/console")
    public R<Map<String, Object>> console(@PathVariable Long serverId, @PathVariable Long clusterId,
                                           @RequestBody Map<String, String> body) {
        Server server = requireServer(serverId);
        DstCluster cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) return R.fail(404, "Cluster not found");

        String command = body.get("command");
        if (command == null || command.isBlank()) return R.fail(400, "Command required");

        var result = dstDeployService.sendConsoleCommand(server, cluster.getName(), command);
        return R.ok(Map.of("success", result.isSuccess()));
    }

    @PostMapping("/{clusterId}/backup")
    public R<Map<String, Object>> backup(@PathVariable Long serverId, @PathVariable Long clusterId) {
        Server server = requireServer(serverId);
        DstCluster cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) return R.fail(404, "Cluster not found");

        String backupName = "backup_" + System.currentTimeMillis();
        var result = dstDeployService.createBackup(server, cluster.getName(), backupName);
        return R.ok(Map.of("success", result.isSuccess(), "backupName", backupName, "size", result.getOutput()));
    }

    // ---- Player Management ----

    @GetMapping("/{clusterId}/players")
    public R<Map<String, Object>> listPlayers(@PathVariable Long serverId, @PathVariable Long clusterId) {
        Server server = requireServer(serverId);
        DstCluster cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) return R.fail(404, "Cluster not found");
        var result = dstDeployService.sendConsoleCommand(server, cluster.getName(), "for i, v in ipairs(TheNet:GetClientTable()) do print(v.userid .. '|' .. v.name .. '|' .. v.prefab) end");
        return R.ok(Map.of("output", result.getOutput()));
    }

    @PostMapping("/{clusterId}/players/kick")
    public R<Map<String, Object>> kickPlayer(@PathVariable Long serverId, @PathVariable Long clusterId,
                                              @RequestBody Map<String, String> body) {
        Server server = requireServer(serverId);
        DstCluster cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) return R.fail(404, "Cluster not found");
        String steamId = body.get("steamId");
        if (steamId == null) return R.fail(400, "steamId required");
        var result = dstDeployService.sendConsoleCommand(server, cluster.getName(), "TheNet:Kick('" + steamId + "')");
        return R.ok(Map.of("success", result.isSuccess()));
    }

    @PostMapping("/{clusterId}/players/ban")
    public R<Map<String, Object>> banPlayer(@PathVariable Long serverId, @PathVariable Long clusterId,
                                             @RequestBody Map<String, String> body) {
        Server server = requireServer(serverId);
        DstCluster cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) return R.fail(404, "Cluster not found");
        String steamId = body.get("steamId");
        if (steamId == null) return R.fail(400, "steamId required");
        var result = dstDeployService.sendConsoleCommand(server, cluster.getName(), "TheNet:Ban('" + steamId + "')");
        return R.ok(Map.of("success", result.isSuccess()));
    }

    @GetMapping("/{clusterId}/adminlist")
    public R<Map<String, Object>> getAdminList(@PathVariable Long serverId, @PathVariable Long clusterId) {
        Server server = requireServer(serverId);
        var result = sshService.executeCommand(server, "cat ~/.klei/DoNotStarveTogether/" + clusterMapper.selectById(clusterId).getName() + "/adminlist.txt 2>/dev/null || echo EMPTY");
        return R.ok(Map.of("content", result.getOutput()));
    }

    @PutMapping("/{clusterId}/adminlist")
    public R<Void> updateAdminList(@PathVariable Long serverId, @PathVariable Long clusterId,
                                    @RequestBody Map<String, String> body) {
        Server server = requireServer(serverId);
        DstCluster cluster = clusterMapper.selectById(clusterId);
        String content = body.getOrDefault("content", "");
        String dir = "~/.klei/DoNotStarveTogether/" + cluster.getName();
        sshService.executeCommand(server, "mkdir -p '" + dir + "' && echo '" + content.replace("'", "'\\''") + "' > '" + dir + "/adminlist.txt'");
        return R.ok();
    }

    // ---- Scheduled Tasks ----

    @GetMapping("/{clusterId}/schedules")
    public R<List<Map<String, Object>>> listSchedules(@PathVariable Long serverId, @PathVariable Long clusterId) {
        // Return in-memory schedule config (simplified — Phase 3 will persist to DB)
        return R.ok(List.of());
    }

    @PostMapping("/{clusterId}/schedules")
    public R<Map<String, Object>> createSchedule(@PathVariable Long serverId, @PathVariable Long clusterId,
                                                  @RequestBody Map<String, Object> body) {
        return R.ok(Map.of("id", System.currentTimeMillis(), "created", true));
    }

    // ---- Mod Management ----

    @PostMapping("/{clusterId}/mods/search")
    public R<Map<String, Object>> searchMods(@PathVariable Long serverId, @PathVariable Long clusterId,
                                              @RequestBody Map<String, String> body) {
        String keyword = body.getOrDefault("keyword", "");
        var results = steamApiService.searchMods(keyword, 1, 20);
        return R.ok(Map.of("keyword", keyword, "results", results));
    }

    @GetMapping("/{clusterId}/mods")
    public R<List<Map<String, Object>>> listMods(@PathVariable Long serverId, @PathVariable Long clusterId) {
        Server server = requireServer(serverId);
        DstCluster cluster = clusterMapper.selectById(clusterId);
        String dir = "~/.klei/DoNotStarveTogether/" + cluster.getName();
        var result = sshService.executeCommand(server,
            "cat '" + dir + "/Master/modoverrides.lua' 2>/dev/null | head -50 || echo '{}'");
        return R.ok(List.of(Map.of("modoverrides", result.getOutput())));
    }

    @PostMapping("/{clusterId}/mods/install")
    public R<Map<String, Object>> installMod(@PathVariable Long serverId, @PathVariable Long clusterId,
                                              @RequestBody Map<String, String> body) {
        Server server = requireServer(serverId);
        String workshopId = body.get("workshopId");
        if (workshopId == null) return R.fail(400, "workshopId required");
        // Write to dedicated_server_mods_setup.lua
        String dstDir = "~/dst_server/mods";
        var result = sshService.executeCommand(server,
            "mkdir -p '" + dstDir + "' && " +
            "grep -q 'workshop-" + workshopId + "' ~/dst_server/mods/dedicated_server_mods_setup.lua 2>/dev/null || " +
            "echo 'ServerModSetup(\"" + workshopId + "\")' >> ~/dst_server/mods/dedicated_server_mods_setup.lua && " +
            "echo INSTALLED");
        return R.ok(Map.of("success", result.isSuccess(), "workshopId", workshopId));
    }
}
