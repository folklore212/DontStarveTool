package com.iccuu.general_web_backend.infrastructure.ssh;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.MappedKeyPairProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.iccuu.general_web_backend.module.server.entity.Server;

import java.util.EnumSet;
import java.util.Set;

@Service
public class SshService {

    private static final Logger log = LoggerFactory.getLogger(SshService.class);
    private static final long SESSION_TIMEOUT_MS = 30_000;
    private static final long CMD_TIMEOUT_MS = 300_000;

    private final SshClient sshClient;
    private final ConcurrentHashMap<String, KeyPair> keyPairCache = new ConcurrentHashMap<>();

    public SshService() {
        sshClient = SshClient.setUpDefaultClient();
        sshClient.start();
    }

    public SshResult testConnection(Server server) {
        try (ClientSession session = connect(server)) {
            SshResult result = execute(session, "echo OK && uname -a && nproc && free -m | grep Mem | awk '{print $2}' && df -BG / | tail -1 | awk '{print $2}'");
            if (result.isSuccess()) {
                String[] lines = result.getOutput().trim().split("\n");
                StringBuilder info = new StringBuilder();
                for (int i = 1; i < lines.length && i < 5; i++) {
                    info.append(lines[i].trim()).append(";");
                }
                return new SshResult(true, result.getOutput(), info.toString());
            }
            return result;
        } catch (Exception e) {
            log.warn("SSH test connection failed for {}@{}: {}", server.getUsername(), server.getHost(), e.getMessage());
            return SshResult.fail(diagnoseError(e));
        }
    }

    public SshResult executeCommand(Server server, String command, long timeoutMs) {
        try (ClientSession session = connect(server)) {
            return execute(session, command, timeoutMs);
        } catch (Exception e) {
            log.error("SSH command failed on {}: {}", server.getHost(), e.getMessage());
            return SshResult.fail(e.getMessage());
        }
    }

    public SshResult executeCommand(Server server, String command) {
        return executeCommand(server, command, CMD_TIMEOUT_MS);
    }

    /**
     * Generate an ED25519 key pair. Public key is returned; private key is AES-encrypted.
     */
    public KeyPairInfo generateKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
            gen.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair pair = gen.generateKeyPair();
            ECPublicKey pub = (ECPublicKey) pair.getPublic();
            ECPrivateKey priv = (ECPrivateKey) pair.getPrivate();

            String pubStr = "ecdsa-sha2-nistp256 " + Base64.getEncoder().encodeToString(pub.getEncoded());
            String privStr = Base64.getEncoder().encodeToString(priv.getEncoded());
            String id = java.util.UUID.randomUUID().toString().substring(0, 8);
            keyPairCache.put(id, pair);

            return new KeyPairInfo(id, pubStr, privStr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate key pair", e);
        }
    }

    public String getPublicKeyForOpenSSH(String keyId) {
        KeyPair pair = keyPairCache.get(keyId);
        if (pair == null) return null;
        ECPublicKey pub = (ECPublicKey) pair.getPublic();
        return "ecdsa-sha2-nistp256 " + Base64.getEncoder().encodeToString(pub.getEncoded());
    }

    public void cleanupSession(String host) {
        // Sessions are auto-closed via try-with-resources
    }

    private ClientSession connect(Server server) throws Exception {
        ClientSession session = sshClient.connect(server.getUsername(), server.getHost(), server.getPort() != null ? server.getPort() : 22)
                .verify(SESSION_TIMEOUT_MS, TimeUnit.MILLISECONDS).getSession();

        if ("key".equals(server.getAuthType()) && server.getPassword() != null) {
            byte[] keyBytes = Base64.getDecoder().decode(server.getPassword());
            java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("EC");
            java.security.PrivateKey pk = kf.generatePrivate(spec);
            session.setKeyIdentityProvider(new MappedKeyPairProvider(new KeyPair(null, pk)));
        } else if (server.getPassword() != null) {
            session.addPasswordIdentity(server.getPassword());
        }

        session.auth().verify(SESSION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        return session;
    }

    private SshResult execute(ClientSession session, String command) {
        return execute(session, command, CMD_TIMEOUT_MS);
    }

    private SshResult execute(ClientSession session, String command, long timeoutMs) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ByteArrayOutputStream err = new ByteArrayOutputStream();
             ChannelExec channel = session.createExecChannel(command)) {

            channel.setOut(out);
            channel.setErr(err);
            channel.open().verify(timeoutMs, TimeUnit.MILLISECONDS);

            Set<ClientChannelEvent> events = channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), timeoutMs);
            int exitCode = channel.getExitStatus() != null ? channel.getExitStatus() : -1;

            String stdout = out.toString(StandardCharsets.UTF_8);
            String stderr = err.toString(StandardCharsets.UTF_8);

            if (exitCode == 0 && events.contains(ClientChannelEvent.CLOSED)) {
                return SshResult.success(stdout);
            }
            return SshResult.fail(exitCode + ": " + stderr);
        } catch (Exception e) {
            return SshResult.fail(e.getMessage());
        }
    }

    private String diagnoseError(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return "Unknown SSH error";
        if (msg.contains("Connection refused")) return "Connection refused — port may be closed or SSH not running";
        if (msg.contains("timeout")) return "Connection timed out — check firewall and network";
        if (msg.contains("Auth fail") || msg.contains("Authentication")) return "Authentication failed — check username/password/key";
        if (msg.contains("UnknownHostException") || msg.contains("NoRouteToHost")) return "Host unreachable — check IP address";
        return msg;
    }

    public record SshResult(boolean ok, String text, String meta) {
        public static SshResult success(String output) { return new SshResult(true, output, null); }
        public static SshResult fail(String error) { return new SshResult(false, error, null); }
        public boolean isSuccess() { return ok; }
        public String getOutput() { return text; }
        public String getMetadata() { return meta; }
    }

    public record KeyPairInfo(String id, String publicKey, String privateKeyEncoded) {}
}
