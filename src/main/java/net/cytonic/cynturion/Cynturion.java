package net.cytonic.cynturion;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "cynturion",
        name = "Cynturion",
        version = BuildConstants.VERSION,
        authors = "Foxikle",
        url = "https://cytonic.net",
        description = "The proxy plugin powering the synchronization of the backend servers."
)
public class Cynturion {

    @Inject
    private Logger logger;
    @Inject
    private ProxyServer proxyServer;
    private RedisDatabase redis;
    private RabbitMQMessager rabbitmq;

    @Inject
    public Cynturion(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxyServer = server;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        redis = new RedisDatabase();
        rabbitmq = new RabbitMQMessager(this);
        rabbitmq.initializeConnection();
        rabbitmq.initializeQueues();
        rabbitmq.consumeServerDeclareMessages();
    }

    @Subscribe
    public void onPlayerJoin(LoginEvent event) {
        redis.sendLoginMessage(event.getPlayer());
    }

    @Subscribe
    public void onPlayerLeave(DisconnectEvent event) {
        redis.sendLogoutMessage(event.getPlayer());
    }

    @Subscribe
    public void onPlayerChooseServer(PlayerChooseInitialServerEvent event) {
        event.setInitialServer(Iterables.getFirst(proxyServer.getAllServers(), null));
    }

    public ProxyServer getProxy() {
        return proxyServer;
    }
}
