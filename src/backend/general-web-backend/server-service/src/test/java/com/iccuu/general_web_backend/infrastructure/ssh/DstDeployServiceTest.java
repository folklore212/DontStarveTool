package com.iccuu.general_web_backend.infrastructure.ssh;

import com.iccuu.general_web_backend.module.server.entity.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DstDeployServiceTest {

    private DstDeployService service;
    private String lastCommand;

    @BeforeEach
    void setup() {
        SshService mockSsh = new SshService() {
            @Override
            public SshResult testConnection(Server s) {
                return new SshResult(true, "ok", "");
            }

            @Override
            public SshResult executeCommand(Server s, String cmd) {
                lastCommand = cmd;
                return new SshResult(true, "ok", "");
            }

            @Override
            public SshResult executeCommand(Server s, String cmd, long timeoutMs) {
                lastCommand = cmd;
                return new SshResult(true, "ok", "");
            }
        };
        service = new DstDeployService(mockSsh);
    }

    @Test
    void generateConfigCommandShouldContainClusterDir() {
        Server server = new Server();
        server.setId(1L);
        Map<String, String> config = Map.of("token", "tkn123", "password", "pwd", "gameMode", "survival");
        service.generateConfig(server, "MyWorld", config);
        assertNotNull(lastCommand, "command should have been sent");
        assertTrue(lastCommand.contains("MyWorld"), "command should contain cluster name");
    }

    @Test
    void checkStatusCommandShouldContainClusterName() {
        Server server = new Server();
        server.setId(1L);
        service.checkStatus(server, "GameWorld");
        assertNotNull(lastCommand);
        assertTrue(lastCommand.contains("GameWorld"), "should contain cluster name");
    }

    @Test
    void startServerCommandShouldContainClusterName() {
        Server server = new Server();
        server.setId(1L);
        Map<String, String> config = Map.of("token", "tkn", "password", "pwd");
        service.generateConfig(server, "MyServer", config);
        assertNotNull(lastCommand);
        assertTrue(lastCommand.contains("MyServer"));
    }
}
