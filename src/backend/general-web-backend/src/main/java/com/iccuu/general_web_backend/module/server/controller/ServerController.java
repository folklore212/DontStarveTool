package com.iccuu.general_web_backend.module.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iccuu.general_web_backend.common.result.PageResult;
import com.iccuu.general_web_backend.common.result.R;
import com.iccuu.general_web_backend.common.util.SecurityUtil;
import com.iccuu.general_web_backend.infrastructure.ssh.SshService;
import com.iccuu.general_web_backend.module.server.entity.Server;
import com.iccuu.general_web_backend.module.server.mapper.ServerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor
public class ServerController {

    private final ServerMapper serverMapper;
    private final SshService sshService;

    @GetMapping("/analytics")
    public R<Map<String, Object>> analytics() {
        Long userId = SecurityUtil.getCurrentUserId();
        long totalServers = serverMapper.selectCount(
                new LambdaQueryWrapper<Server>().eq(Server::getUserId, userId));
        long onlineServers = serverMapper.selectCount(
                new LambdaQueryWrapper<Server>().eq(Server::getUserId, userId).eq(Server::getStatus, "online"));
        return R.ok(Map.of(
                "totalServers", totalServers,
                "onlineServers", onlineServers,
                "offlineServers", totalServers - onlineServers
        ));
    }

    @GetMapping
    public R<PageResult<Server>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        Page<Server> mpPage = serverMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Server>().eq(Server::getUserId, userId).orderByDesc(Server::getSortOrder).orderByDesc(Server::getCreatedAt));
        return R.ok(PageResult.of(mpPage.getTotal(), page, size, mpPage.getRecords()));
    }

    @PostMapping
    public R<Server> create(@RequestBody Server server) {
        server.setUserId(SecurityUtil.getCurrentUserId());
        server.setStatus("unknown");
        server.setCreatedAt(LocalDateTime.now());
        server.setUpdatedAt(LocalDateTime.now());
        serverMapper.insert(server);
        return R.ok(server);
    }

    @PutMapping("/{id}")
    public R<Server> update(@PathVariable Long id, @RequestBody Server server) {
        Server existing = serverMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(SecurityUtil.getCurrentUserId())) {
            return R.fail(404, "Server not found");
        }
        server.setId(id);
        server.setUpdatedAt(LocalDateTime.now());
        serverMapper.updateById(server);
        return R.ok(serverMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        Server existing = serverMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(SecurityUtil.getCurrentUserId())) {
            return R.fail(404, "Server not found");
        }
        serverMapper.deleteById(id);
        return R.ok();
    }

    @GetMapping("/{id}/collaborators")
    public R<Map<String, Object>> listCollaborators(@PathVariable Long id) {
        return R.ok(Map.of("collaborators", java.util.Collections.emptyList()));
    }

    @PostMapping("/{id}/collaborators")
    public R<Void> inviteCollaborator(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok();
    }

    @DeleteMapping("/{id}/collaborators/{userId}")
    public R<Void> removeCollaborator(@PathVariable Long id, @PathVariable Long userId) {
        return R.ok();
    }

    @PostMapping("/{id}/test")
    public R<Map<String, Object>> testConnection(@PathVariable Long id) {
        Server server = serverMapper.selectById(id);
        if (server == null) return R.fail(404, "Server not found");

        long start = System.currentTimeMillis();
        var result = sshService.testConnection(server);
        long elapsed = System.currentTimeMillis() - start;

        server.setStatus(result.isSuccess() ? "online" : "offline");
        server.setLastTestAt(LocalDateTime.now());
        if (result.isSuccess() && result.getMetadata() != null) {
            server.setOsInfo(result.getMetadata());
        }
        serverMapper.updateById(server);

        return R.ok(Map.of(
                "success", result.isSuccess(),
                "message", result.isSuccess() ? "Connection successful" : result.getOutput(),
                "elapsed", elapsed
        ));
    }
}
