package com.iccuu.general_web_backend.module.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iccuu.general_web_backend.common.result.R;
import com.iccuu.general_web_backend.infrastructure.ssh.DstDeployService;
import com.iccuu.general_web_backend.module.server.entity.Server;
import com.iccuu.general_web_backend.module.server.mapper.ServerMapper;
import com.iccuu.general_web_backend.server.client.RemoteModSearchProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Internal API for node-gateway JSON-RPC forwarding.
 * Receives parsed JSON-RPC method calls and executes them on the target server.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/internal/nodes")
@RequiredArgsConstructor
public class NodeForwardController {

    private final ServerMapper serverMapper;
    private final DstDeployService deployService;
    private final RemoteModSearchProvider modSearch;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/forward")
    public R<Map<String, Object>> forward(@RequestBody ForwardRequest request) {
        Server server = serverMapper.selectById(request.serverId());
        if (server == null) {
            return R.fail(404, "Server not found");
        }

        try {
            Map<String, Object> result = execute(server, request.method(), request.params());
            return R.ok(result);
        } catch (Exception e) {
            log.error("Node command failed: method={} serverId={}", request.method(), request.serverId(), e);
            return R.fail(500, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> execute(Server server, String method, Object paramsObj) throws Exception {
        Map<String, Object> params = paramsObj instanceof Map ? (Map<String, Object>) paramsObj : Collections.emptyMap();
        String clusterName = String.valueOf(params.getOrDefault("cluster_name", ""));

        return switch (method) {
            case "dst.start" -> handleStart(server, clusterName, params);
            case "dst.stop" -> handleStop(server, clusterName, params);
            case "dst.restart" -> handleRestart(server, clusterName, params);
            case "dst.status" -> handleStatus(server, clusterName);
            case "dst.console.send" -> handleConsoleSend(server, clusterName, params);
            case "dst.players.list" -> handlePlayersList(server, clusterName);
            case "dst.players.kick" -> handlePlayersKick(server, clusterName, params);
            case "dst.players.ban" -> handlePlayersBan(server, clusterName, params);
            case "dst.players.unban" -> handlePlayersUnban(server, clusterName, params);
            case "dst.adminlist.get" -> handleAdminList(server, clusterName);
            case "node.metrics" -> handleMetrics(server);
            default -> throw new IllegalArgumentException("Unknown method: " + method);
        };
    }

    private Map<String, Object> handleStart(Server server, String clusterName, Map<String, Object> params) {
        var result = deployService.startServer(server, clusterName);
        return Map.of("success", result.isSuccess(), "output", result.getOutput());
    }

    private Map<String, Object> handleStop(Server server, String clusterName, Map<String, Object> params) {
        var result = deployService.stopServer(server, clusterName);
        return Map.of("success", result.isSuccess(), "output", result.getOutput());
    }

    private Map<String, Object> handleRestart(Server server, String clusterName, Map<String, Object> params) {
        deployService.stopServer(server, clusterName);
        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
        var result = deployService.startServer(server, clusterName);
        return Map.of("success", result.isSuccess(), "output", result.getOutput());
    }

    private Map<String, Object> handleStatus(Server server, String clusterName) {
        var result = deployService.checkStatus(server, clusterName);
        return Map.of("success", result.isSuccess(), "output", result.getOutput());
    }

    private Map<String, Object> handleConsoleSend(Server server, String clusterName, Map<String, Object> params) {
        String shard = String.valueOf(params.getOrDefault("shard", "Master"));
        String command = String.valueOf(params.getOrDefault("command", ""));
        var result = deployService.sendConsoleCommand(server, clusterName, command);
        return Map.of("sent", result.isSuccess(), "output", result.getOutput());
    }

    private Map<String, Object> handlePlayersList(Server server, String clusterName) {
        var result = deployService.sendConsoleCommand(server, clusterName,
                "for i, v in ipairs(TheNet:GetClientTable()) do print(string.format(\"%s|%s|%s\", v.name, v.userid, v.prefab)) end");
        // Parse the raw console output into player list
        List<Map<String, String>> players = new ArrayList<>();
        if (result.isSuccess() && result.getOutput() != null) {
            for (String line : result.getOutput().split("\n")) {
                String[] parts = line.split("\\|");
                if (parts.length >= 3) {
                    players.add(Map.of("name", parts[0], "steam_id", parts[1], "character", parts[2]));
                }
            }
        }
        return Map.of("players", players);
    }

    private Map<String, Object> handlePlayersKick(Server server, String clusterName, Map<String, Object> params) {
        String steamId = String.valueOf(params.getOrDefault("steam_id", ""));
        var result = deployService.sendConsoleCommand(server, clusterName,
                "TheNet:Kick(\"" + steamId + "\")");
        return Map.of("success", result.isSuccess());
    }

    private Map<String, Object> handlePlayersBan(Server server, String clusterName, Map<String, Object> params) {
        String steamId = String.valueOf(params.getOrDefault("steam_id", ""));
        var result = deployService.sendConsoleCommand(server, clusterName,
                "TheNet:Ban(\"" + steamId + "\")");
        return Map.of("success", result.isSuccess());
    }

    private Map<String, Object> handlePlayersUnban(Server server, String clusterName, Map<String, Object> params) {
        String steamId = String.valueOf(params.getOrDefault("steam_id", ""));
        var result = deployService.sendConsoleCommand(server, clusterName,
                "TheNet:UnBan(\"" + steamId + "\")");
        return Map.of("success", result.isSuccess());
    }

    private Map<String, Object> handleAdminList(Server server, String clusterName) {
        var result = deployService.sendConsoleCommand(server, clusterName,
                "for i, v in ipairs(TheNet:GetAdminList()) do print(v) end");
        List<String> admins = new ArrayList<>();
        if (result.isSuccess() && result.getOutput() != null) {
            admins.addAll(List.of(result.getOutput().split("\n")));
        }
        return Map.of("admins", admins);
    }

    private Map<String, Object> handleMetrics(Server server) {
        var result = deployService.checkStatus(server, "");
        return Map.of("success", result.isSuccess(), "status", result.getOutput());
    }

    record ForwardRequest(Long nodeId, Long serverId, String method, Object params) {}
}
