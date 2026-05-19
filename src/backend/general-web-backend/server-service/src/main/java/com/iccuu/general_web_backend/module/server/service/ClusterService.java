package com.iccuu.general_web_backend.module.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iccuu.general_web_backend.common.exception.BusinessException;
import com.iccuu.general_web_backend.infrastructure.ssh.DstDeployService;
import com.iccuu.general_web_backend.infrastructure.ssh.SshService;
import com.iccuu.general_web_backend.module.server.entity.DstCluster;
import com.iccuu.general_web_backend.module.server.entity.Server;
import com.iccuu.general_web_backend.module.server.mapper.DstClusterMapper;
import com.iccuu.general_web_backend.module.server.mapper.ServerMapper;
import com.iccuu.general_web_backend.common.util.SecurityUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClusterService {

    private final DstClusterMapper clusterMapper;
    private final ServerMapper serverMapper;
    private final SshService sshService;
    private final DstDeployService deployService;
    private final ModSearchProvider modSearch;

    private Server requireOwnedServer(Long serverId) {
        Server s = serverMapper.selectById(serverId);
        if (s == null || !s.getUserId().equals(SecurityUtil.getCurrentUserId()))
            throw new BusinessException(403, "Not authorized");
        return s;
    }

    public List<DstCluster> listClusters(Long serverId) {
        return clusterMapper.selectList(new LambdaQueryWrapper<DstCluster>().eq(DstCluster::getServerId, serverId));
    }

    public DstCluster createCluster(Long serverId, DstCluster cluster) {
        requireOwnedServer(serverId);
        cluster.setServerId(serverId);
        clusterMapper.insert(cluster);
        return cluster;
    }

    public void deleteCluster(Long serverId, Long clusterId) {
        requireOwnedServer(serverId);
        var c = clusterMapper.selectById(clusterId);
        if (c == null || !c.getServerId().equals(serverId))
            throw new BusinessException(404, "Cluster not found");
        clusterMapper.deleteById(clusterId);
    }

    public Map<String, Object> install(Long serverId, Long clusterId) {
        Server server = requireOwnedServer(serverId);
        var cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) throw new BusinessException(404, "Cluster not found");
        var depsOk = deployService.checkDependencies(server).isSuccess();
        var steamOk = deployService.installSteamCmd(server).isSuccess();
        var dstOk = deployService.installDstServer(server).isSuccess();
        return Map.of("deps", depsOk, "steamCmd", steamOk, "dst", dstOk);
    }

    public Map<String, Object> start(Long serverId, Long clusterId) {
        Server server = requireOwnedServer(serverId);
        var cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) throw new BusinessException(404, "Cluster not found");
        var r = deployService.startServer(server, cluster.getName());
        return Map.of("success", r.isSuccess(), "output", r.getOutput());
    }

    public Map<String, Object> stop(Long serverId, Long clusterId) {
        Server server = requireOwnedServer(serverId);
        var cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) throw new BusinessException(404, "Cluster not found");
        var r = deployService.stopServer(server, cluster.getName());
        return Map.of("success", r.isSuccess(), "output", r.getOutput());
    }

    public Map<String, Object> status(Long serverId, Long clusterId) {
        Server server = requireOwnedServer(serverId);
        var cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) throw new BusinessException(404, "Cluster not found");
        var r = deployService.checkStatus(server, cluster.getName());
        return Map.of("running", r.getOutput().contains("running"), "output", r.getOutput());
    }

    public Map<String, Object> sendConsoleCommand(Long serverId, Long clusterId, String command) {
        Server server = requireOwnedServer(serverId);
        var cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) throw new BusinessException(404, "Cluster not found");
        deployService.sendConsoleCommand(server, cluster.getName(), command);
        return Map.of("sent", true);
    }

    public Map<String, Object> createBackup(Long serverId, Long clusterId, String backupName) {
        Server server = requireOwnedServer(serverId);
        var cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) throw new BusinessException(404, "Cluster not found");
        var r = deployService.createBackup(server, cluster.getName(), backupName);
        return Map.of("created", r.isSuccess(), "output", r.getOutput());
    }

    public Map<String, Object> searchMods(Long serverId, Long clusterId, String keyword) {
        requireOwnedServer(serverId);
        var cached = modSearch.searchCached(keyword);
        if (!cached.isEmpty()) return Map.of("keyword", keyword, "results", cached, "source", "cache");
        var results = modSearch.fetchFromSteam(keyword, 1, 20);
        return Map.of("keyword", keyword, "results", results, "source", "live");
    }

    public Map<String, Object> listMods(Long serverId, Long clusterId) {
        Server server = requireOwnedServer(serverId);
        var cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) throw new BusinessException(404, "Cluster not found");
        String dir = "~/.klei/DoNotStarveTogether/" + cluster.getName() + "/Master/modoverrides.lua";
        var result = sshService.executeCommand(server, "cat '" + dir + "' 2>/dev/null | head -50 || echo '{}'");
        return Map.of("raw", result.getOutput());
    }

    public Map<String, Object> installMod(Long serverId, Long clusterId, String workshopId) {
        Server server = requireOwnedServer(serverId);
        var cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) throw new BusinessException(404, "Cluster not found");
        String dir = "~/.klei/DoNotStarveTogether/" + cluster.getName() + "/Master";
        String cmd = "echo '\nServerModSetup(\"" + workshopId + "\")' >> " + dir + "/dedicated_server_mods_setup.lua";
        sshService.executeCommand(server, cmd);
        return Map.of("installed", true, "workshopId", workshopId);
    }

    public Map<String, Object> listPlayers(Long serverId, Long clusterId) {
        Server server = requireOwnedServer(serverId);
        var cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) throw new BusinessException(404, "Cluster not found");
        var result = sshService.executeCommand(server, "echo 'TheNet:GetClientTable()' | nc localhost 10999 2>/dev/null | head -50 || echo '[]'");
        return Map.of("raw", result.getOutput());
    }

    public Map<String, Object> kickPlayer(Long serverId, Long clusterId, String steamId) {
        Server server = requireOwnedServer(serverId);
        // authorized via requireOwnedServer above
        // Kick via screen console command
        deployService.sendConsoleCommand(server, "Master", "TheNet:Kick(\"" + steamId + "\")");
        return Map.of("kicked", true);
    }

    public Map<String, Object> banPlayer(Long serverId, Long clusterId, String steamId) {
        Server server = requireOwnedServer(serverId);
        var cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) throw new BusinessException(404, "Cluster not found");
        deployService.sendConsoleCommand(server, "Master", "TheNet:Ban(\"" + steamId + "\")");
        return Map.of("banned", true);
    }

    public List<String> getAdminList(Long serverId, Long clusterId) {
        Server server = requireOwnedServer(serverId);
        var cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) throw new BusinessException(404, "Cluster not found");
        String dir = "~/.klei/DoNotStarveTogether/" + cluster.getName() + "/Master/adminlist.txt";
        var result = sshService.executeCommand(server, "cat '" + dir + "' 2>/dev/null || echo ''");
        return result.getOutput().isBlank() ? Collections.emptyList() : List.of(result.getOutput().split("\n"));
    }

    public void setAdminList(Long serverId, Long clusterId, List<String> admins) {
        Server server = requireOwnedServer(serverId);
        var cluster = clusterMapper.selectById(clusterId);
        if (cluster == null) throw new BusinessException(404, "Cluster not found");
        String dir = "~/.klei/DoNotStarveTogether/" + cluster.getName() + "/Master/adminlist.txt";
        String content = String.join("\n", admins);
        sshService.executeCommand(server, "echo '" + content + "' > " + dir);
    }
}
