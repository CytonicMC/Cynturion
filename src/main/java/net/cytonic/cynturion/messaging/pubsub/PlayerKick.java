package net.cytonic.cynturion.messaging.pubsub;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.cytonic.cynturion.Cynturion;
import net.cytonic.cynturion.RedisDatabase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import redis.clients.jedis.JedisPubSub;

import java.lang.reflect.MalformedParametersException;
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
        if (channel.equals(RedisDatabase.PLAYER_KICK)) {
            System.out.println(message);
            // FORMAT: {uuid}|:|{reason}|:|{name}|:|{message}|:|{rescuable}
            String[] parts = message.split("\\|:\\|");
            if (parts.length != 5) throw new MalformedParametersException("The recived message is malformed.");
            UUID uuid = UUID.fromString(parts[0]);
            Optional<Player> player = plugin.getProxy().getPlayer(uuid);
            if (player.isPresent()) {
                String reason = parts[1];
                String name = parts[2];
                Component component = JSONComponentSerializer.json().deserialize(parts[3]);
                boolean rescuable = Boolean.parseBoolean(parts[4]);

                if (rescuable) {
                    plugin.getLogger().warn("RESCUE");
                    player.get().sendMessage(MM."<red>You were kicked from your server. <gray>(\{reason})");
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
                System.out.println(STR."Kicking player: \{name} with reason: \{reason}");
            }
        }
    }
}
