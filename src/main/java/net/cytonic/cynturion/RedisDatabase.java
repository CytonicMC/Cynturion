package net.cytonic.cynturion;

import com.velocitypowered.api.proxy.Player;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;

public class RedisDatabase {
    public static final String PLAYER_STATUS_CHANNEL = "player_status";
    public static final String ONLINE_PLAYER_NAME_KEY = "online_player_names";
    public static final String ONLINE_PLAYER_UUID_KEY = "online_player_uuids";
    private final Jedis jedis;

    /**
     * Initializes the connection to redis using the loaded settings and the Jedis client
     */
    public RedisDatabase() {
        HostAndPort hostAndPort = new HostAndPort(System.getenv("REDIS_HOST"), 6379);
        JedisClientConfig config = DefaultJedisClientConfig.builder().password(System.getenv("REDIS_PASSWORD")).build();
        this.jedis = new Jedis(hostAndPort, config);
//        this.jedis = new Jedis(System.getenv("REDIS_HOST"), Integer.parseInt(System.getenv("REDIS_PORT")));
        this.jedis.auth(System.getenv("REDIS_PASSWORD"));
    }


    /**
     * Sends a message in redis that the specified player joined
     * @param player the player who joined
     */
    public void sendLoginMessage(Player player) {
        // <PLAYER_NAME>|:|<PLAYER_UUID>|:|<JOIN/LEAVE>
        jedis.publish(PLAYER_STATUS_CHANNEL, player.getUsername() + "|:|" + player.getUniqueId() + "|:|JOIN");
        jedis.sadd(ONLINE_PLAYER_NAME_KEY, player.getUsername());
        jedis.sadd(ONLINE_PLAYER_UUID_KEY, player.getUniqueId().toString());
    }

    /**
     * Sends a message in redis that the specified user left
     * @param player The player who left
     */
    public void sendLogoutMessage(Player player) {
        // <PLAYER_NAME>|:|<PLAYER_UUID>|:|<JOIN/LEAVE>
        jedis.publish(PLAYER_STATUS_CHANNEL, player.getUsername() + "|:|" + player.getUniqueId() + "|:|LEAVE");
        jedis.srem(ONLINE_PLAYER_NAME_KEY, player.getUsername());
        jedis.srem(ONLINE_PLAYER_UUID_KEY, player.getUniqueId().toString());
    }
}

