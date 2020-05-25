package vin.howe.holdmusic

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import net.kyori.text.TextComponent
import org.slf4j.Logger
import vin.howe.holdmusic.director.*
import java.net.InetSocketAddress
import java.util.*

class Manager(private val proxy: ProxyServer, private val config: Config, private val logger: Logger) {
    private val queue: Deque<Player> = LinkedList()
    private val manager = DirectorManager()
    private var server: ServerInfo? = null

    companion object {
        private const val SERVER_NAME = "play"
        private const val SERVER_PORT = 25565
    }

    /**
     * Tell director who's on right now
     */
    fun updateUsage() {
        if (proxy.playerCount == 0) {
            return
        }

        val uuids = proxy.allPlayers.map { it.uniqueId }

//        val status: StatusResponse
        try {
//            status = manager.reportUsage(uuids).join()
            manager.reportUsage(uuids).join()
        } catch (e: Exception) {
            logger.error(e.toString())
        }

        logger.info("Logged usage: ${proxy.playerCount} online now")
    }

    /**
     * Let everyone into the server when it loads
     */
    fun updateHoldMusic() {
        if (queue.isEmpty()) return

        if (!attemptConnect(SERVER_NAME, proxy)) {
            return
        }

        queue.forEach { player -> player.createConnectionRequest(proxy.getServer(SERVER_NAME).get()).connect().thenAccept { queue.remove(player) } }
    }

    /**
     * Send an update message to all players in queue
     */
    fun sendLoadingMessage() {
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
        val eligibility: CheckEligibleResponse

        try {
            eligibility = manager.checkEligible(event.player.uniqueId.toString()).join()
        } catch (e: Exception) {
            logger.error("checkEligible failed:")
            logger.error(e.toString())
            event.player.disconnect(TextComponent.of("Unable to verify eligibility for ${event.player.username} (${event.player.uniqueId}). This is likely a server error."))
            return
        }

        if (!eligibility.exists) {
            event.player.disconnect(TextComponent.of("${event.player.username} (${event.player.uniqueId}) is not registered. Contact the server admin for more information."))
            return
        }

        if (!eligibility.eligible) {
            event.player.disconnect(TextComponent.of("${event.player.username} (${event.player.uniqueId}) is not currently eligible. Contact the server admin for more information."))
            return
        }

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

        val managerResponse: PokeResponse
        try {
            managerResponse = manager.poke().join()
        } catch (e: Exception) {
            logger.error("attemptConnect failed:")
            logger.error(e.toString())
            println(e)
            return false
        }

        val serverOnline = managerResponse.status == ServerStatus.ONLINE.status

        if (!serverOnline) return serverOnline

        val newServer = ServerInfo(SERVER_NAME, InetSocketAddress.createUnresolved(managerResponse.url, SERVER_PORT))

        if (!serverRegistered || server == null || newServer.address != server!!.address) {
            logger.info("\"$SERVER_NAME\" building and registering server")

            server = newServer
            if (proxy.getServer(SERVER_NAME).isPresent) {
                proxy.unregisterServer(proxy.getServer(SERVER_NAME).get().serverInfo)
            }
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

