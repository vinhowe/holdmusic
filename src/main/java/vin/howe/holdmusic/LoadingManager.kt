package vin.howe.holdmusic

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerPing
import net.kyori.text.TextComponent
import org.slf4j.Logger
import java.util.*

class LoadingManager(private val proxy: ProxyServer, private val config: Config, private val logger: Logger) {
    private val queue: Deque<Player>

    /**
     * Let everyone into the server when it loads
     */
    fun flushQueue() {
        if (queue.isEmpty()) return
        if (!proxy.getServer(config.target).isPresent) {
            return
        }
        val targetServer = proxy.getServer(config.target).get()
        targetServer.ping().handle { s: ServerPing?, t: Throwable? ->
            if (s == null) {
                return@handle t
            }
            logger.info("target server should be online! attempting to flush queue")
            for (i in queue.indices) {
                queue.remove().createConnectionRequest(targetServer).fireAndForget()
            }
            s
        }
    }

    /**
     * Send an update message to all players in queue
     */
    fun sendUpdate() {
        for (player in queue) {
            player.sendMessage(TextComponent.of("Server loading... hang tight!"))
        }
    }

    /**
     * Add a player to the queue if they join the queue server
     *
     * @param event
     */
    @Subscribe
    fun onServerConnect(event: ServerPreConnectEvent) {
        val targetServerOptional = proxy.getServer(config.target)
        logger.info("onServerConnect")
        if (!targetServerOptional.isPresent) {
            logger.info("target server not present--strange")
            return
        }
        val targetServer = targetServerOptional.get()
        targetServer.ping().handle { s: ServerPing?, t: Throwable? ->
            if (s != null) {
                return@handle s
            }
            logger.info("target server not online--trying queue")
            val player = event.player
            logger.info("original server name: " + event.originalServer.serverInfo.name)
            if (event.originalServer.serverInfo.name == config.queue) {
                queue.add(player)
                logger.info("added to queue: $player")
            }
            t
        }
    }

    /**
     * Remove a player from the queue if they leave while in it
     *
     * @param event
     */
    @Subscribe
    fun onLeave(event: DisconnectEvent) {
        val player = event.player
        val connectionOptional = player.currentServer
        if (!connectionOptional.isPresent) return
        if (connectionOptional.get().serverInfo.name == config.queue) {
            queue.remove(player)
            logger.info("removed from queue: $player")
        }
    } //    private CompletableFuture<String>

    init {
        queue = LinkedList()
    }
}