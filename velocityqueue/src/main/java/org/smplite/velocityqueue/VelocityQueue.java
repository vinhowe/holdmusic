package org.smplite.velocityqueue;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

@Plugin(id = "velocityqueue",
		name = "Velocity Queue",
		version = "1.0-SNAPSHOT",
		description = "Idk what I'm doing. That's what I get for never properly learning Java.",
		authors = {"Epix"})
public class VelocityQueue {
	private ProxyServer proxy;
	private Logger logger;
	private Config config;

	private Queue queue;

	@Inject
	public VelocityQueue(ProxyServer proxy, @DataDirectory Path path, Logger logger)
	{
		this.proxy = proxy;
		this.logger = logger;

		new File(path.toString()).mkdirs(); // Create path directories
		this.config = Config.getConfig(path + "/config.json");
		logger.info(config.message);
	}

	@Subscribe
	public void onProxyInitialize(ProxyInitializeEvent e)
	{
		// Create and register queue
		this.queue = new Queue(config, logger);
		proxy.getEventManager().register(this, this.queue);
	}
}
