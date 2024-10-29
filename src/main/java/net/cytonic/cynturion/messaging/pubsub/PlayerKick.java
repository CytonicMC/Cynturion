package net.cytonic.cynturion.messaging.pubsub;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.cytonic.containers.PlayerKickContainer;
import net.cytonic.cynturion.Cynturion;
import net.cytonic.cynturion.RedisDatabase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import redis.clients.jedis.JedisPubSub;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static net.cytonic.utils.MiniMessageTemplate.MM;

public class PlayerKick extends JedisPubSub {

    private final Cynturion plugin;

    public PlayerKick(Cynturion plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessage(String channel, String message) {
        try {
            if (channel.equals(RedisDatabase.PLAYER_KICK)) {
                PlayerKickContainer container = PlayerKickContainer.deserialize(message);
                plugin.getLogger().info(message);
                UUID uuid = container.uuid();
                Optional<Player> player = plugin.getProxy().getPlayer(uuid);
                if (player.isPresent()) {
                    Component component = JSONComponentSerializer.json().deserialize(container.kickMessage());
                    boolean rescuable = container.reason().isRescuable();
                    if (rescuable) {
                        plugin.getLogger().warn("RESCUE");
                        player.get().sendMessage(MM."<red>You were kicked from your server. <gray>(\{container.reason()})");
                        Optional<RegisteredServer> fallback = plugin.getServerGroupingManager().chooseFallback();


                        fallback.ifPresent(group -> plugin.getProxy().getScheduler().buildTask(plugin, task -> player.get().createConnectionRequest(group).connect().whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                plugin.getLogger().error("An error occurred!", throwable);
                                player.get().disconnect(MM."<red>Failed to rescue:".appendNewline().append(component));
                            }

                            plugin.getLogger().warn(result.getStatus().name());
                            if (result.isSuccessful()) {
                                task.cancel();
                            }
                        })).repeat(100, TimeUnit.MILLISECONDS).schedule());


                        if (fallback.isEmpty()) {
                            player.get().disconnect(MM."<red>Failed to rescue:".appendNewline().append(component));
                        }
//                     todo: implement rescuing using backup lobby servers or something
                        return;
                    }
                    player.get().disconnect(component);
                    System.out.println(STR."Kicking player: \{uuid} with reason: \{container.reason()}");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().error("error", e);
        }
    }
}
