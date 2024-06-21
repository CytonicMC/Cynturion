package net.cytonic.cynturion;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.PingOptions;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Collections;

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
        CynturionSettings.importFromProperties();
        CynturionSettings.importFromEnv();
        redis = new RedisDatabase(this);
        rabbitmq = new RabbitMQMessager(this);
        redis.loadServers();
        rabbitmq.initializeConnection();
        rabbitmq.initializeQueues();
        rabbitmq.consumeServerDeclareMessages();
        rabbitmq.consumeServerShutdownMessages();
        rabbitmq.consumePlayerKickMessages();
        CommandManager cm = proxyServer.getCommandManager();
        cm.register(cm.metaBuilder("proxypoddetails").aliases("proxypod").build(), new PoddetailsCommand());
        proxyServer.getCommandManager().unregister("server");
    }

    /**
     * Subscribes to the LoginEvent and sends a login message to Redis using the player's information.
     *
     * @param event the LoginEvent triggered when a player joins the server
     */
    @Subscribe
    public void onPlayerJoin(LoginEvent event) {
        redis.sendLoginMessage(event.getPlayer());
        proxyServer.getCommandManager().unregister("server");
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
     * Subscribes to the KickedFromServerEvent and attempts to rescue the player.
     *
     * @param event the KickedFromServerEvent triggered when a player is kicked from the server
     */
    //todo: Make a dedicated list of fallbacks
    public void onKick(KickedFromServerEvent event) {
        System.out.println("Kicked from server!!! (trying to rescue)");
        event.setResult(KickedFromServerEvent.RedirectPlayer.create(Iterables.getFirst(proxyServer.getAllServers(), null), Component.text("Whoops! You were kicked from the server, but I rescued you! :)")));
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
                if (throwable != null) {
                    logger.error("Failed to ping server {}", event.getOriginalServer().getServerInfo().getName());
                    logger.warn("Unregistering server {}", event.getOriginalServer().getServerInfo().getName());
                    getProxy().unregisterServer(event.getOriginalServer().getServerInfo());
                    getRedis().removeServer(event.getOriginalServer().getServerInfo());
                    getRedis().sendUnregisterServerMessage(event.getOriginalServer().getServerInfo());
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

    public RedisDatabase getRedis() {
        return redis;
    }

    public RabbitMQMessager getRabbitMQ() {
        return rabbitmq;
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        redis.shutdown();
        rabbitmq.shutdown();
    }
}
