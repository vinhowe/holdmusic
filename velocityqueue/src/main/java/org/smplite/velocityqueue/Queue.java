package org.smplite.velocityqueue;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class Queue {
	private List<Player> players;
	private Logger logger;

	/**
	 * Initializes a queue
	 */
	public Queue(Logger logger)
	{
		players = new ArrayList<Player>();
		this.logger = logger;
	}

	/**
	 * Add a player to the queue if they join the queue server
	 * @param e
	 */
	@Subscribe
	public void onServerConnected(ServerConnectedEvent e)
	{
		logger.info(e.getServer().toString());
	}
}