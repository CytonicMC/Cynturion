package net.cytonic.cynturion.messaging.pubsub;

import com.velocitypowered.api.proxy.Player;
import net.cytonic.cynturion.Cynturion;
import net.cytonic.cynturion.RedisDatabase;
import redis.clients.jedis.JedisPubSub;
import java.util.UUID;

public class PlayerSend extends JedisPubSub {

    private final Cynturion plugin;
    private final RedisDatabase redis;

    public PlayerSend(Cynturion plugin, RedisDatabase redis) {
        this.plugin = plugin;
        this.redis = redis;
    }

    @Override
    public void onMessage(String channel, String message) {
        try {
            if (channel.equals(RedisDatabase.PLAYER_SEND_CHANNEL)) {
                // formatting: <PLAYER_UUID>|:|<SERVER_ID>
                String[] parts = message.split("\\|:\\|");
                UUID uuid = UUID.fromString(parts[0]);
                String serverId = parts[1];
                if (plugin.getProxy().getPlayer(uuid).isPresent()) {
                    Player player = plugin.getProxy().getPlayer(uuid).get();
                    if (plugin.getProxy().getServer(serverId).isPresent()) {
                        player.createConnectionRequest(plugin.getProxy().getServer(serverId).get()).connect();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error " + e);
        }
    }
}
