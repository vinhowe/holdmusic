package org.smplite.velocityqueue;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(id = "velocityqueue",
        name = "Velocity Queue",
        version = "1.0-SNAPSHOT",
        description = "Idk what I'm doing. That's what I get for never properly learning Java.",
        authors = {"Epix"})
public class VelocityQueue {
    private final ProxyServer proxy;
    private final Logger logger;

    @Inject
    public VelocityQueue(ProxyServer proxy, Logger logger)
    {
        this.proxy = proxy;
        this.logger = logger;

        logger.info("Hello, Velocity!");
    }
}
