package org.smplite.velocityqueue;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Queue {
	private List<Player> players;
	private Config config;
	private Logger logger;

	/**
	 * Initializes a queue
	 * @param config config instance for the plugin
	 * @param logger logger instance for the plugin
	 */
	public Queue(Config config, Logger logger)
	{
		players = new ArrayList<Player>();
		this.config = config;
		this.logger = logger;
	}

	/**
	 * Add a player to the queue if they join the queue server
	 * @param e
	 */
	@Subscribe
	public void onServerConnected(ServerConnectedEvent e)
	{
		if (e.getServer().toString().equals(config.queue)) {
			// Add player to queue
			players.add(e.getPlayer());
			logger.info("Added to queue: " + e.getPlayer().toString());
		}
	}

	/**
	 * Removes a player from the queue if they were in it
	 * @param e
	 */
	@Subscribe
	public void onLeave(DisconnectEvent e)
	{
		Player p = e.getPlayer();
		Optional<ServerConnection> s = p.getCurrentServer();
		if (!s.isPresent()) return;

		if (s.toString().equals(config.queue)) {
			// Remove player from queue
			players.remove(p);
			logger.info("Removed from queue: " + p.toString());
		}
	}
}