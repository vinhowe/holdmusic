package vin.howe.holdmusic

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import com.velocitypowered.api.proxy.server.ServerPing
import net.kyori.text.TextComponent
import org.slf4j.Logger
import java.net.InetSocketAddress
import java.util.*

class LoadingManager(private val proxy: ProxyServer, private val config: Config, private val logger: Logger) {
    private val queue: Deque<Player> = LinkedList()
    private val manager = DirectorManager()
    private var server: ServerInfo? = null

    companion object {
        private const val SERVER_NAME = "play"
        private const val SERVER_PORT = 25565
    }

    /**
     * Let everyone into the server when it loads
     */
    fun updateHoldMusic() {
        if (queue.isEmpty()) return

        if (!attemptConnect(SERVER_NAME, proxy)) {
            return
        }

        queue.forEach { player -> player.createConnectionRequest(proxy.getServer(SERVER_NAME).get()).connect().thenAccept{ queue.remove(player) } }
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
        if (attemptConnect(SERVER_NAME, proxy)) {
            event.result = ServerPreConnectEvent.ServerResult.allowed(proxy.getServer(SERVER_NAME).get());
            return
        }

        val player = event.player

        if (event.originalServer.serverInfo.name == config.queue) {
            queue.add(player)
            logger.info("added to queue: $player")
        }
    }

    private fun attemptConnect(serverName: String, proxy: ProxyServer): Boolean {
        val targetServerOptional = proxy.getServer(serverName)
        val serverRegistered = targetServerOptional.isPresent

        val managerResponse = manager.call().join()
        val serverOnline = managerResponse.status == ServerStatus.ONLINE.status

        if (!serverOnline) return serverOnline

        val newServer = ServerInfo(SERVER_NAME, InetSocketAddress.createUnresolved(managerResponse.url, SERVER_PORT))

        if (!serverRegistered || server == null || newServer.address != server!!.address) {
            logger.info("\"$SERVER_NAME\" building and registering server")

            server = newServer
            proxy.registerServer(newServer)
        }
        return true
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
    }
}

enum class ServerStatus(val status: String) {
    OFFLINE("offline"),
    LOADING("loading"),
    ONLINE("online"),
}
