package net.cytonic.cynturion;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.PingOptions;
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
        rabbitmq.consumeServerShutdownMessages();
        rabbitmq.consumePlayerKickMessages();
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

    @Subscribe
    public void onServerConnect(ServerPreConnectEvent event) {
        event.getOriginalServer().ping(PingOptions.DEFAULT).whenComplete((serverPing, throwable) -> {
            if(throwable != null) {
                logger.error("Failed to ping server {}", event.getOriginalServer().getServerInfo().getName(), throwable);
                return;
            }

            if (serverPing == null) {
                logger.info("Server {} is not online, unregistering", event.getOriginalServer().getServerInfo().getName());
                proxyServer.unregisterServer(event.getOriginalServer().getServerInfo());
                rabbitmq.sendServerTimeoutMessage(event.getOriginalServer().getServerInfo());
                redis.removeServer(event.getOriginalServer().getServerInfo());
            }
        });
    }

    /**
     * Returns the proxy server.
     *
     * @return the proxy server
     */
    public ProxyServer getProxy() {
        return proxyServer;
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        redis.shutdown();
        rabbitmq.shutdown();
    }
}
