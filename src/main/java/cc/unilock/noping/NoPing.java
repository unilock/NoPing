package cc.unilock.noping;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.network.ConnectionManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
	id = "noping",
	name = "NoPing",
	description = "Only allow connections to forced hosts",
	version = Constants.VERSION,
	authors = { "unilock" }
)
public final class NoPing {
	public static Logger LOGGER;
	private final ProxyServer proxy;
	private final Path path;

	@Inject
	public NoPing(
			final ProxyServer proxy,
			final Logger logger,
			final @DataDirectory Path path
	) {
		this.proxy = proxy;
		this.path = path;

		LOGGER = logger;
	}

	@Subscribe
	void onProxyInitialize(final ProxyInitializeEvent event) throws Exception {
		var cmField = ((VelocityServer) proxy).getClass().getDeclaredField("cm");
		cmField.setAccessible(true);

		ChannelInitializer<Channel> oldInit = ((ConnectionManager) cmField.get(proxy)).getServerChannelInitializer().get();
		((ConnectionManager) cmField.get(proxy)).getServerChannelInitializer().set(new NoPingInitializer((VelocityServer) proxy, oldInit));
	}
}
