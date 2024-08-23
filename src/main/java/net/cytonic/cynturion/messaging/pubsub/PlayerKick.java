package net.cytonic.cynturion.messaging.pubsub;

import net.cytonic.cynturion.Cynturion;
import net.cytonic.cynturion.RedisDatabase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import redis.clients.jedis.JedisPubSub;

import java.lang.reflect.MalformedParametersException;
import java.util.UUID;

public class PlayerKick extends JedisPubSub {

    private final Cynturion plugin;

    public PlayerKick(Cynturion plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessage(String channel, String message) {
        if (channel.equals(RedisDatabase.PLAYER_KICK)) {
            // FORMAT: {uuid}|:|{reason}|:|{name}|:|{message}|:|{rescuable}
            String[] parts = message.split("\\|:\\|");
            if (parts.length != 5) throw new MalformedParametersException("The recived message is malformed.");
            UUID uuid = UUID.fromString(parts[0]);
            if (plugin.getProxy().getPlayer(uuid).isPresent()) {
                String reason = parts[1];
                String name = parts[2];
                Component component = JSONComponentSerializer.json().deserialize(parts[3]);
                boolean rescuable = Boolean.parseBoolean(parts[4]);
                if (rescuable) {
                    // todo: implement rescuing using backup lobby servers or something
                    return;
                }
                plugin.getProxy().getPlayer(uuid).get().disconnect(component);
                System.out.println(STR."Kicking player: \{name} with reason: \{reason}");
            }
        }
    }
}
