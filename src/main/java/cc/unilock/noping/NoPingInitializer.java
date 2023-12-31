package cc.unilock.noping;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.HandshakeSessionHandler;
import com.velocitypowered.proxy.network.ServerChannelInitializer;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public class NoPingInitializer extends ServerChannelInitializer {
    private final Method INIT_CHANNEL;
    private final ChannelInitializer<Channel> delegate;
    private final VelocityServer server;

    public NoPingInitializer(VelocityServer server, ChannelInitializer<Channel> delegate) {
        super(server);
        this.server = server;
        this.delegate = delegate;
        try {
            INIT_CHANNEL = delegate.getClass().getDeclaredMethod("initChannel", Channel.class);
            INIT_CHANNEL.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void initChannel(@NotNull Channel ch) {
        try {
            INIT_CHANNEL.invoke(delegate, ch);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            if (ch.pipeline().get(MinecraftConnection.class) == null)
                super.initChannel(ch);
            MinecraftConnection handler = ch.pipeline().get(MinecraftConnection.class);
            HandshakeSessionHandler originalSessionHandler = (HandshakeSessionHandler) handler.getActiveSessionHandler();
            handler.setActiveSessionHandler(StateRegistry.STATUS, new NoPingHandshakeSessionHandler(originalSessionHandler, handler, server));
        }
    }
}
