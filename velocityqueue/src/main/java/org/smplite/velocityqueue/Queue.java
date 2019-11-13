package org.smplite.velocityqueue;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.lucko.luckperms.LuckPerms;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.slf4j.Logger;
import me.lucko.luckperms.api.*;

import java.util.*;

public class Queue {
	private ProxyServer proxy;
	private Config config;
	private Logger logger;

	// Singleton instance of LuckPerms api
	private LuckPermsApi luckPerms;

	private Deque<Player> regularQueue;
	private Deque<Player> priorityQueue;

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

		regularQueue = new LinkedList<>();
		priorityQueue = new LinkedList<>();

		// Loads the singleton instance of LuckPerms
		luckPerms = LuckPerms.getApi();
	}

	/**
	 * Lets as many people as possible into the server
	 */
	public void flushQueue()
	{
		// Ignore if queue is empty
		if (regularQueue.isEmpty() && priorityQueue.isEmpty()) return;

		// Get status of target server
		RegisteredServer targetServer = proxy.getServer(config.target).get();

		// Gets how many slots are available to connect, minimum amount is 0 obviously
		int slotsAvailable = Math.max(config.maxPlayers - targetServer.getPlayersConnected().size(), 0);

		for(int i = 0; i < slotsAvailable; i++)
		{
			if (!priorityQueue.isEmpty())
				priorityQueue.remove().createConnectionRequest(targetServer).fireAndForget();
			else if (!regularQueue.isEmpty())
				regularQueue.remove().createConnectionRequest(targetServer).fireAndForget();
			else
				break;
		}
	}

	/**
	 * Send an update message to all players in the positionMap
	 */
	public void sendUpdate()
	{
		int i = 1;
		for(Player p : priorityQueue) {
			p.sendMessage(TextComponent.of(config.message.replaceAll("%position%", Integer.toString(i))).color(TextColor.GOLD));
			i++	;
		}

		// Reset for regular queue so it doesn't seem like they're positions have changed
		i = 1;
		for (Player p : regularQueue) {
			p.sendMessage(TextComponent.of(config.message.replaceAll("%position%", Integer.toString(i))).color(TextColor.GOLD));
			i++;
		}
	}

	/**
	 * Checks if a player has a specified permission node through LuckPerms API
	 */
	public boolean hasPermission(Player player, String permission){
		return player.hasPermission(permission);
	}

	/**
	 * Add a player to the queue if they join the queue server
	 * @param e
	 */
	@Subscribe
	public void onServerConnect(ServerPreConnectEvent e)
	{
		RegisteredServer targetServer = proxy.getServer(config.target).get();

		// Player is a staff, ignore all and connect instantly
		if(hasPermission(e.getPlayer(), "velocityqueue.queue.staff")){
			e.setResult(ServerPreConnectEvent.ServerResult.allowed(targetServer));
		}

		if (e.getOriginalServer().getServerInfo().getName().equals(config.queue)) {
			// If the queue is empty and the server isn't capped, send the player through, skipping the queue.
			if (priorityQueue.isEmpty() && regularQueue.isEmpty() && targetServer.getPlayersConnected().size() < config.maxPlayers) {
					// Don't wait, directly send
					e.setResult(ServerPreConnectEvent.ServerResult.allowed(targetServer));
					// We aren't creating a connection request here, we are just modifying the existing one
					return;
			}

			// Add player to respective queue, depending on permissions
			if(hasPermission(e.getPlayer(), "velocityqueue.queue.priority")){
				priorityQueue.add(e.getPlayer());
				logger.info("Added to priority queue: " + e.getPlayer().toString());
			}else{
				regularQueue.add(e.getPlayer());
				logger.info("Added to regular queue: " + e.getPlayer().toString());
			}
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
			regularQueue.remove(p);
			priorityQueue.remove(p);
			logger.info("Removed from queue: " + p.toString());
		}
	}
}