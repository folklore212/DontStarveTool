package com.iccuu.general_web_backend.module.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iccuu.general_web_backend.common.exception.BusinessException;
import com.iccuu.general_web_backend.common.util.SecurityUtil;
import com.iccuu.general_web_backend.infrastructure.ssh.SshService;
import com.iccuu.general_web_backend.module.server.entity.Server;
import com.iccuu.general_web_backend.module.server.mapper.ServerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ServerService {

    private final ServerMapper serverMapper;
    private final SshService sshService;

    public Server getById(Long id) {
        return serverMapper.selectById(id);
    }

    public void requireOwnership(Server server) {
        if (server == null || !server.getUserId().equals(SecurityUtil.getCurrentUserId()))
            throw new BusinessException(404, "Server not found");
    }

    public Map<String, Object> getAnalytics() {
        Long userId = SecurityUtil.getCurrentUserId();
        long total = serverMapper.selectCount(new LambdaQueryWrapper<Server>().eq(Server::getUserId, userId));
        long online = serverMapper.selectCount(new LambdaQueryWrapper<Server>().eq(Server::getUserId, userId).eq(Server::getStatus, "online"));
        return Map.of("totalServers", total, "onlineServers", online, "offlineServers", total - online);
    }

    public Page<Server> listServers(int page, int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        return serverMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Server>().eq(Server::getUserId, userId).orderByDesc(Server::getSortOrder).orderByDesc(Server::getCreatedAt));
    }

    public Server create(Server server) {
        server.setUserId(SecurityUtil.getCurrentUserId());
        server.setStatus("unknown");
        server.setCreatedAt(LocalDateTime.now());
        server.setUpdatedAt(LocalDateTime.now());
        serverMapper.insert(server);
        return server;
    }

    public Server update(Long id, Server input) {
        Server existing = serverMapper.selectById(id);
        requireOwnership(existing);
        input.setId(id);
        input.setUpdatedAt(LocalDateTime.now());
        serverMapper.updateById(input);
        return serverMapper.selectById(id);
    }

    public void delete(Long id) {
        Server existing = serverMapper.selectById(id);
        requireOwnership(existing);
        serverMapper.deleteById(id);
    }

    public Map<String, Object> testConnection(Long id) {
        Server server = serverMapper.selectById(id);
        if (server == null) throw new BusinessException(404, "Server not found");
        long start = System.currentTimeMillis();
        var result = sshService.testConnection(server);
        long elapsed = System.currentTimeMillis() - start;
        server.setStatus(result.isSuccess() ? "online" : "offline");
        server.setLastTestAt(LocalDateTime.now());
        if (result.isSuccess() && result.getMetadata() != null) server.setOsInfo(result.getMetadata());
        serverMapper.updateById(server);
        return Map.of("success", result.isSuccess(), "message", result.isSuccess() ? "Connection successful" : result.getOutput(), "elapsed", elapsed);
    }
}
