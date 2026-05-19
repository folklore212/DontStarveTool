package com.iccuu.general_web_backend.infrastructure.ssh;

import com.iccuu.general_web_backend.module.server.entity.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DstDeployService {

    private static final Logger log = LoggerFactory.getLogger(DstDeployService.class);
    private final SshService sshService;

    public DstDeployService(SshService sshService) {
        this.sshService = sshService;
    }

    private static final String STEAMCMD_DIR = "~/steamcmd";
    private static final String DST_DIR = "~/dst_server";
    private static final String DST_APP_ID = "343050";

    /**
     * Check system dependencies for DST server.
     */
    public SshService.SshResult checkDependencies(Server server) {
        return sshService.executeCommand(server,
            "echo '===OS===' && cat /etc/os-release 2>/dev/null | head -4 && " +
            "echo '===LIBS===' && ldd --version 2>/dev/null | head -1 && " +
            "echo '===SCREEN===' && which screen 2>/dev/null || echo MISSING && " +
            "echo '===STEAMCMD===' && test -f " + STEAMCMD_DIR + "/steamcmd.sh && echo INSTALLED || echo MISSING && " +
            "echo '===DST===' && test -f " + DST_DIR + "/bin/dontstarve_dedicated_server_nullrenderer && echo INSTALLED || echo MISSING");
    }

    /**
     * Install missing dependencies for DST (Ubuntu/Debian).
     */
    public SshService.SshResult installDependencies(Server server) {
        String cmd = "if command -v apt-get >/dev/null 2>&1; then " +
            "apt-get update -qq && apt-get install -y -qq libstdc++6 libcurl4 screen lib32gcc1 lib32stdc++6; " +
            "elif command -v yum >/dev/null 2>&1; then " +
            "yum install -y -q libstdc++ libcurl screen; else " +
            "echo UNKNOWN_PACKAGE_MANAGER; fi";
        return sshService.executeCommand(server, cmd, 120_000);
    }

    /**
     * Install or update SteamCMD.
     */
    public SshService.SshResult installSteamCmd(Server server) {
        String cmd = "mkdir -p " + STEAMCMD_DIR + " && cd " + STEAMCMD_DIR + " && " +
            "if [ ! -f steamcmd.sh ]; then " +
            "  curl -sqL 'https://steamcdn-a.akamaihd.net/client/installer/steamcmd_linux.tar.gz' | tar zxf -; " +
            "fi && echo STEAMCMD_READY && ./steamcmd.sh +quit 2>&1 | tail -5";
        return sshService.executeCommand(server, cmd, 120_000);
    }

    /**
     * Download/update Don't Starve Together dedicated server via SteamCMD.
     */
    public SshService.SshResult installDstServer(Server server) {
        String cmd = "mkdir -p " + DST_DIR + " && " + STEAMCMD_DIR + "/steamcmd.sh " +
            "+force_install_dir " + DST_DIR + " +login anonymous +app_update " + DST_APP_ID + " validate +quit 2>&1";
        return sshService.executeCommand(server, cmd, 600_000); // 10 min timeout for download
    }

    /**
     * Generate cluster configuration files on the remote server.
     */
    public SshService.SshResult generateConfig(Server server, String clusterName, Map<String, String> config) {
        String clusterDir = "~/.klei/DoNotStarveTogether/" + clusterName;
        String token = config.getOrDefault("token", "");
        String maxPlayers = config.getOrDefault("maxPlayers", "6");
        String password = config.getOrDefault("password", "");
        String gameMode = config.getOrDefault("gameMode", "survival");
        String desc = config.getOrDefault("description", clusterName);

        String cmd = "mkdir -p '" + clusterDir + "/Master' '" + clusterDir + "/Caves' && " +
            "cat > '" + clusterDir + "/cluster.ini' << 'CLUSTEREOF'\n" +
            "[GAMEPLAY]\ngame_mode = " + gameMode + "\nmax_players = " + maxPlayers + "\n" +
            "pvp = false\npause_when_empty = true\n\n" +
            "[NETWORK]\ncluster_description = " + desc + "\ncluster_name = " + clusterName + "\n" +
            "cluster_password = " + password + "\n\n" +
            "[MISC]\nconsole_enabled = true\n\n" +
            "[SHARD]\nshard_enabled = true\nbind_ip = 127.0.0.1\nmaster_ip = 127.0.0.1\n" +
            "master_port = 10889\ncluster_key = supersecretkey\nCLUSTEREOF\n" +
            "cat > '" + clusterDir + "/cluster_token.txt' << 'TOKENEOF'\n" + token + "\nTOKENEOF\n" +
            "cat > '" + clusterDir + "/Master/server.ini' << 'MASTEREOF'\n" +
            "[NETWORK]\nserver_port = 10999\n\n" +
            "[SHARD]\nis_master = true\n\n" +
            "[STEAM]\nmaster_server_port = 27018\nauthentication_port = 8768\nMASTEREOF\n" +
            "cat > '" + clusterDir + "/Caves/server.ini' << 'CAVESEOF'\n" +
            "[NETWORK]\nserver_port = 10998\n\n" +
            "[SHARD]\nis_master = false\nname = Caves\n\n" +
            "[STEAM]\nmaster_server_port = 27019\nauthentication_port = 8769\nCAVESEOF\n" +
            "echo CONFIG_GENERATED";

        return sshService.executeCommand(server, cmd, 30_000);
    }

    /**
     * Start DST server (Master + Caves) in screen sessions.
     */
    public SshService.SshResult startServer(Server server, String clusterName) {
        String cmd = "cd " + DST_DIR + "/bin && " +
            "screen -dmS dst_master_" + clusterName + " ./dontstarve_dedicated_server_nullrenderer -cluster " + clusterName + " -shard Master && " +
            "sleep 2 && screen -dmS dst_caves_" + clusterName + " ./dontstarve_dedicated_server_nullrenderer -cluster " + clusterName + " -shard Caves && " +
            "echo STARTED && sleep 2 && screen -ls | grep dst_";
        return sshService.executeCommand(server, cmd, 60_000);
    }

    /**
     * Stop DST server.
     */
    public SshService.SshResult stopServer(Server server, String clusterName) {
        String cmd = "screen -S dst_master_" + clusterName + " -X quit 2>/dev/null; " +
            "screen -S dst_caves_" + clusterName + " -X quit 2>/dev/null; " +
            "pkill -f 'dontstarve_dedicated_server_nullrenderer.*" + clusterName + "' 2>/dev/null; " +
            "echo STOPPED";
        return sshService.executeCommand(server, cmd, 30_000);
    }

    /**
     * Check server status.
     */
    public SshService.SshResult checkStatus(Server server, String clusterName) {
        String cmd = "if pgrep -f 'dontstarve_dedicated_server.*" + clusterName + "' > /dev/null; then " +
            "echo RUNNING; pgrep -f 'dontstarve_dedicated.*" + clusterName + "' | wc -l; " +
            "screen -ls 2>/dev/null | grep dst_ | wc -l; " +
            "else echo STOPPED; fi";
        return sshService.executeCommand(server, cmd, 15_000);
    }

    /**
     * Send console command.
     */
    public SshService.SshResult sendConsoleCommand(Server server, String clusterName, String command) {
        String cmd = "screen -S dst_master_" + clusterName + " -p 0 -X stuff '" + command + "\r'";
        return sshService.executeCommand(server, cmd, 10_000);
    }

    /**
     * Create backup of DST world.
     */
    public SshService.SshResult createBackup(Server server, String clusterName, String backupName) {
        String src = "~/.klei/DoNotStarveTogether/" + clusterName;
        String dst = "~/dst_backups/" + clusterName + "_" + backupName;
        String cmd = "mkdir -p ~/dst_backups && tar czf '" + dst + ".tar.gz' -C ~/.klei/DoNotStarveTogether '" + clusterName + "' && echo BACKUP_CREATED && ls -lh '" + dst + ".tar.gz' | awk '{print $5}'";
        return sshService.executeCommand(server, cmd, 120_000);
    }
}
