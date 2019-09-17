package org.smplite.velocityqueue;

import com.google.inject.Inject;
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
    private final ProxyServer proxy;
    private final Logger logger;
    private final Config config;

    @Inject
    public VelocityQueue(ProxyServer proxy, @DataDirectory Path path, Logger logger)
    {
        this.proxy = proxy;
        this.logger = logger;

        new File(path.toString()).mkdirs(); // Create path directories
        this.config = Config.getConfig(path + "/config.json");
        logger.info(config.message);
    }
}
