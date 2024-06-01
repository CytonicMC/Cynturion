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

    /**
     * Initializes the proxy when it is being initialized.
     *
     * @param event the event triggered when the proxy is being initialized
     */
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        redis = new RedisDatabase(this);
        rabbitmq = new RabbitMQMessager(this);
        redis.loadServers();
        rabbitmq.initializeConnection();
        rabbitmq.initializeQueues();
        rabbitmq.consumeServerDeclareMessages();
    }

    /**
     * Subscribes to the LoginEvent and sends a login message to Redis using the player's information.
     *
     * @param event the LoginEvent triggered when a player joins the server
     */
    @Subscribe
    public void onPlayerJoin(LoginEvent event) {
        redis.sendLoginMessage(event.getPlayer());
    }

    /**
     * Subscribes to the DisconnectEvent and sends a logout message to Redis using the player's information.
     *
     * @param event the DisconnectEvent triggered when a player leaves the server
     */
    @Subscribe
    public void onPlayerLeave(DisconnectEvent event) {
        redis.sendLogoutMessage(event.getPlayer());
    }

    /**
     * Subscribes to the PlayerChooseInitialServerEvent and sets the initial server for the player.
     *
     * @param event the PlayerChooseInitialServerEvent triggered when a player chooses their initial server
     */
    @Subscribe
    public void onPlayerChooseServer(PlayerChooseInitialServerEvent event) {
        event.setInitialServer(Iterables.getFirst(proxyServer.getAllServers(), null));
    }

    /**
     * Returns the proxy server.
     *
     * @return the proxy server
     */
    public ProxyServer getProxy() {
        return proxyServer;
    }
}
