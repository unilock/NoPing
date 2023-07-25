package cc.unilock.noping;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.HandshakeSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.Handshake;
import io.netty.buffer.ByteBuf;

public class NoPingHandshakeSessionHandler extends HandshakeSessionHandler implements MinecraftSessionHandler {
    private final HandshakeSessionHandler original;
    private final MinecraftConnection connection;
    private final VelocityServer server;

    public NoPingHandshakeSessionHandler(HandshakeSessionHandler original, MinecraftConnection connection, VelocityServer server) {
        super(connection, server);
        this.original = original;
        this.connection = connection;
        this.server = server;
    }

    @Override
    public boolean handle(Handshake handshake) {
        var vhost = cleanVhost(handshake.getServerAddress());

        // block hostname != forced host
        if (!server.getConfiguration().getForcedHosts().containsKey(vhost)) {
            connection.close();
            return true;
        }

        return handshake.handle(original);
    }

    @Override
    public void handleGeneric(MinecraftPacket packet) {
        original.handleGeneric(packet);
    }

    @Override
    public void handleUnknown(ByteBuf buf) {
        original.handleUnknown(buf);
    }

    /**
     * Cleans the specified virtual host hostname.
     *
     * @param hostname the host name to clean
     * @return the cleaned hostname
     */
    static String cleanVhost(String hostname) {
        // Clean out any anything after any zero bytes (this includes BungeeCord forwarding and the
        // legacy Forge handshake indicator).
        String cleaned = hostname;
        int zeroIdx = cleaned.indexOf('\0');
        if (zeroIdx > -1) {
            cleaned = hostname.substring(0, zeroIdx);
        }

        // If we connect through a SRV record, there will be a period at the end (DNS usually elides
        // this ending octet).
        if (!cleaned.isEmpty() && cleaned.charAt(cleaned.length() - 1) == '.') {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }
}
