package vin.howe.holdmusic

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import org.slf4j.Logger
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@Suppress("unused")
@Plugin(id = "holdmusic", name = "holdmusic", version = "1.0-SNAPSHOT", description = "Holding room while server loads", authors = ["Vin Howe", "Epix"])
class HoldMusic @Inject constructor(private val proxy: ProxyServer, @DataDirectory path: Path, private val logger: Logger) {
    private val config: Config

    @Subscribe
    fun onProxyInitialize(e: ProxyInitializeEvent?) {
        val queue = LoadingManager(proxy, config, logger)
        proxy.eventManager.register(this, queue)
        proxy.scheduler.buildTask(this) { queue.flushQueue() }.repeat(1, TimeUnit.SECONDS).schedule()
        proxy.scheduler.buildTask(this) { queue.sendUpdate() }.repeat(15, TimeUnit.SECONDS).schedule()
    }

    init {
        File(path.toString()).mkdirs() // Create path directories
        config = Config.loadConfig(Paths.get(path.toString(), "/config.json").toString(), logger)
        logger.info("HoldMusic loaded")
    }
}