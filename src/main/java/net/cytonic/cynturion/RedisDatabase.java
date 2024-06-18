package net.cytonic.cynturion;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.cytonic.cynturion.data.obj.CytonicServer;
import net.cytonic.cynturion.messaging.pubsub.*;
import redis.clients.jedis.*;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisDatabase extends JedisPubSub {

    /**
     * Cached player names
     */
    public static final String ONLINE_PLAYER_NAME_KEY = "online_player_names";
    /**
     * Cached player UUIDs
     */
    public static final String ONLINE_PLAYER_UUID_KEY = "online_player_uuids";
    /**
     * Cached Servers
     */
    public static final String ONLINE_SERVER_KEY = "online_servers";

    /**
     * Player login/out channel
     */
    public static final String PLAYER_STATUS_CHANNEL = "player_status";
    /**
     * Server startup / shutdown
     */
    public static final String SERVER_STATUS_CHANNEL = "server_status";
    /**
     * Player send channel
     */
    public static final String PLAYER_SEND_CHANNEL = "player_send";
    
    
    private final ExecutorService worker = Executors.newCachedThreadPool();
    // cache client
    private final JedisPooled jedis;
    // publish client
    private final JedisPooled jedisPub;
    // subscribe client
    private final JedisPooled jedisSub;
    private final Cynturion plugin;

    /**
     * Initializes the connection to redis using the loaded settings and the Jedis client
     */
    public RedisDatabase(Cynturion plugin) {
        this.plugin = plugin;
        HostAndPort hostAndPort = new HostAndPort(CynturionSettings.REDIS_HOST, 6379);
        JedisClientConfig config = DefaultJedisClientConfig.builder().password(CynturionSettings.REDIS_PASSWORD).build();
        this.jedis = new JedisPooled(hostAndPort, config);
        this.jedisPub = new JedisPooled(hostAndPort, config);
        this.jedisSub = new JedisPooled(hostAndPort, config);
        
        worker.submit(() -> jedisSub.subscribe(new ServerStatus(plugin, this), SERVER_STATUS_CHANNEL));
        worker.submit(() -> jedisSub.subscribe(new PlayerSend(plugin, this), PLAYER_SEND_CHANNEL));
    }

    /**
     * Sends a message in redis that the specified player joined
     *
     * @param player the player who joined
     */
    public void sendLoginMessage(Player player) {
        // <PLAYER_NAME>|:|<PLAYER_UUID>|:|<JOIN/LEAVE>
        jedisPub.publish(PLAYER_STATUS_CHANNEL, player.getUsername() + "|:|" + player.getUniqueId() + "|:|JOIN");
        jedis.sadd(ONLINE_PLAYER_NAME_KEY, player.getUsername());
        jedis.sadd(ONLINE_PLAYER_UUID_KEY, player.getUniqueId().toString());
    }

    /**
     * Sends a message in redis that the specified user left
     *
     * @param player The player who left
     */
    public void sendLogoutMessage(Player player) {
        // <PLAYER_NAME>|:|<PLAYER_UUID>|:|<JOIN/LEAVE>
        jedisPub.publish(PLAYER_STATUS_CHANNEL, player.getUsername() + "|:|" + player.getUniqueId() + "|:|LEAVE");
        jedis.srem(ONLINE_PLAYER_NAME_KEY, player.getUsername());
        jedis.srem(ONLINE_PLAYER_UUID_KEY, player.getUniqueId().toString());
    }

    /**
     * Sends a fake message pretending to be the server that stopped, since it stopped responding.
     * @param info the server to remove
     */
    public void sendUnregisterServerMessage(ServerInfo info) {
        // formatting: <START/STOP>|:|<SERVER_ID>|:|<SERVER_IP>|:|<SERVER_PORT>
        jedisPub.publish(SERVER_STATUS_CHANNEL, "STOP|:|" + info.getName() + "|:|" + info.getAddress().getAddress().getHostAddress() + "|:|" + info.getAddress().getPort());
    }

    /**
     * Loads the servers from the SERVER_STATUS_CHANNEL and registers them with the proxy server.
     * The servers are expected to be in the format "{server-name}|:|{server-ip}|:|{server-port}".
     */
    public void loadServers() {
        worker.submit(() -> jedis.smembers(ONLINE_SERVER_KEY).forEach(s -> {
            CytonicServer server = CytonicServer.deserialize(s);
            System.out.println("Registering the server: " + server.id() + " with the ip and port " + server.ip() + ":" + server.port());
            plugin.getProxy().registerServer(new ServerInfo(server.id(), new InetSocketAddress(server.ip(), server.port())));
        }));
    }

    /**
     * Adds a server to the Redis database by constructing a server data string and adding it to the ONLINE_SERVER_KEY set.
     *
     * @param info the ServerInfo object representing the server to be added
     * **/
    public void addServer(ServerInfo info) {
        jedis.sadd(ONLINE_SERVER_KEY, new CytonicServer(info.getAddress().getAddress().getHostAddress(), info.getName(), info.getAddress().getPort()).serialize());
    }

    /**
     * Removes a server from the Redis database by constructing a server data string and removing it from the ONLINE_SERVER_KEY set.
     *
     * @param info the ServerInfo object representing the server to be removed
     */
    public void removeServer(ServerInfo info) {
        jedis.srem(ONLINE_SERVER_KEY, new CytonicServer(info.getAddress().getAddress().getHostAddress(), info.getName(), info.getAddress().getPort()).serialize());
    }

    /**
     * Closes the connection to the Redis database.
     * <p>
     * This method is used to gracefully shut down the connection to the Redis database by calling the `close()` method on the `jedis` object.
     */
    public void shutdown() {
        jedis.close();
    }
}

