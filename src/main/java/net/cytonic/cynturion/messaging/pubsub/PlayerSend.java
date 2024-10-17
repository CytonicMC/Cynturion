package net.cytonic.cynturion.messaging.pubsub;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import net.cytonic.containers.Container;
import net.cytonic.containers.SendPlayerToServerContainer;
import net.cytonic.cynturion.Cynturion;
import net.cytonic.cynturion.RedisDatabase;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;

public class PlayerSend extends JedisPubSub {

    private final Cynturion plugin;

    public PlayerSend(Cynturion plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessage(String channel, String message) {
        plugin.getLogger().info("{} : {}", channel, message);
        try {
            if (channel.equals(RedisDatabase.PLAYER_SEND_CHANNEL)) {
                Container container = Container.deserialize(message);
                if (container instanceof SendPlayerToServerContainer c) {
                    UUID uuid = c.getPlayer();
                    String serverId = c.getServer().id();
                    if (plugin.getProxy().getPlayer(uuid).isPresent()) {
                        Player player = plugin.getProxy().getPlayer(uuid).get();
                        if (plugin.getProxy().getServer(serverId).isPresent()) {
                            ConnectionRequestBuilder builder = player.createConnectionRequest(plugin.getProxy().getServer(serverId).get());
                            builder.connect().whenComplete((result, throwable) -> {
                                if (throwable != null) {
                                    plugin.getLogger().error(throwable.getMessage(), throwable);
                                    return;
                                }
                                plugin.getLogger().error(result.getStatus().name());

                                if (!result.isSuccessful()) {
                                    builder.connect().whenComplete((result1, throwable1) -> {
                                        if (throwable1 != null) {
                                            plugin.getLogger().error(throwable1.getMessage(), throwable1);
                                        }
                                        if (!result1.isSuccessful()) {
                                            builder.connect();
                                        }
                                    });
                                }
                            });
                        } else {
                            plugin.getLogger().warn("Supposed to send player, but the server doesn't exist!");
                        }
                    } else {
                        plugin.getLogger().warn("Supposed to send player, but player isn't online!");
                    }
                } else {
                    plugin.getLogger().warn("Attempting to send to a server without a SendPlayerToServerContainer");
                }
            }
        } catch (Exception e) {
            System.out.println(STR."Error \{e}");
            e.printStackTrace(System.err);
        }
    }
}
