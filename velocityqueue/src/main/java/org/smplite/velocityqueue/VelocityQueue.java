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
import java.util.concurrent.TimeUnit;

@Plugin(id = "velocityqueue",
		name = "Velocity Queue",
		version = "1.0-SNAPSHOT",
		description = "Idk what I'm doing. That's what I get for never properly learning Java.",
		authors = {"Epix"})
public class VelocityQueue {
	private ProxyServer proxy;
	private Logger logger;
	private Config config;

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
		Queue queue = new Queue(proxy, config, logger);
		proxy.getEventManager().register(this, queue);

		// Run queue flusher
		proxy.getScheduler().buildTask(this, queue::flushQueue).repeat(1, TimeUnit.SECONDS).schedule();

		// Run updater
		proxy.getScheduler().buildTask(this, queue::sendUpdate).repeat(15, TimeUnit.SECONDS).schedule();
	}
}
