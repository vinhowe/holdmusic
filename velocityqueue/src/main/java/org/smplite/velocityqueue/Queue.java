package org.smplite.velocityqueue;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.slf4j.Logger;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;

public class Queue {
	private  ProxyServer proxy;
	private Config config;
	private Logger logger;

	private Deque<Player> players;

	/**
	 * Initializes a queue
	 * @param config config instance for the plugin
	 * @param logger logger instance for the plugin
	 */
	public Queue(ProxyServer proxy, Config config, Logger logger)
	{
		this.proxy = proxy;
		this.config = config;
		this.logger = logger;

		players = new LinkedList<Player>();
	}

	/**
	 * Lets as many people as possible into the server
	 */
	public void flushQueue()
	{
		// Get status of target server
		RegisteredServer targetServer = proxy.getServer(config.target).get();

		if (targetServer.getPlayersConnected().size() < config.maxPlayers) {
			// Allow players onto the server
			int allowance = Math.min(config.maxPlayers - targetServer.getPlayersConnected().size(), players.size());

			for (int i = 0; i < allowance; i++) {
				Player p = players.remove();

				p.createConnectionRequest(targetServer).fireAndForget();
			}
		}
	}

	/**
	 * Tells players their queue position
	 */
	public void sendUpdate()
	{
		int i = 1;
		for (Player p : players) {
			p.sendMessage(TextComponent.of(config.message.replaceAll("%position%", Integer.toString(i))).color(TextColor.GOLD));
			i++;
		}
	}

	/**
	 * Add a player to the queue if they join the queue server
	 * @param e
	 */
	@Subscribe
	public void onServerConnected(ServerConnectedEvent e)
	{
		if (e.getServer().getServerInfo().getName().equals(config.queue)) {
			// If the queue is empty and the server isn't capped, send the player through, skipping the queue.
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

		if (s.get().getServerInfo().getName().equals(config.queue)) {
			// Remove player from queue
			players.remove(p);
			logger.info("Removed from queue: " + p.toString());
		}
	}
}