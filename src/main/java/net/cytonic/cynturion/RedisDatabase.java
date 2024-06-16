package net.cytonic.cynturion;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerInfo;
import redis.clients.jedis.*;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisDatabase extends JedisPubSub {

    public static final String PLAYER_STATUS_CHANNEL = "player_status";
    public static final String ONLINE_PLAYER_NAME_KEY = "online_player_names";
    public static final String ONLINE_PLAYER_UUID_KEY = "online_player_uuids";
    public static final String SERVER_STATUS_KEY = "server_status";
    public static final String SERVER_STATUS_CHANNEL = "server_status_channel";
    private final ExecutorService worker = Executors.newCachedThreadPool();
    private final JedisPooled jedis;
    private final Cynturion plugin;

    /**
     * Initializes the connection to redis using the loaded settings and the Jedis client
     */
    public RedisDatabase(Cynturion plugin) {
        this.plugin = plugin;
        HostAndPort hostAndPort = new HostAndPort(System.getProperty("REDIS_HOST"), 6379);
        JedisClientConfig config = DefaultJedisClientConfig.builder().password(System.getProperty("REDIS_PASSWORD")).build();
        this.jedis = new JedisPooled(hostAndPort, config);
        worker.submit(() -> jedis.subscribe(this, SERVER_STATUS_CHANNEL));
    }

    @Override
    public void onMessage(String channel, String message) {
        if (channel.equals(SERVER_STATUS_CHANNEL)) {
            String[] parts = message.split("\\|:\\|");
            String name = parts[0];
            String ip = parts[1];
            int port = Integer.parseInt(parts[2]);
            ServerInfo info = new ServerInfo(name, new InetSocketAddress(ip, port));
            if (parts[3].equalsIgnoreCase("START")) {
                System.out.println("Registering the server: " + name + " with the ip and port " + ip + ":" + port);
                plugin.getProxy().registerServer(info);
                addServer(info);
            }else if (parts[3].equalsIgnoreCase("SHUTDOWN")) {
                System.out.println("Unregistering the server: " + name + " with the ip and port " + ip + ":" + port);
                plugin.getProxy().unregisterServer(info);
                removeServer(info);
            }
        }
    }


    /**
     * Sends a message in redis that the specified player joined
     *
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
     *
     * @param player The player who left
     */
    public void sendLogoutMessage(Player player) {
        // <PLAYER_NAME>|:|<PLAYER_UUID>|:|<JOIN/LEAVE>
        jedis.publish(PLAYER_STATUS_CHANNEL, player.getUsername() + "|:|" + player.getUniqueId() + "|:|LEAVE");
        jedis.srem(ONLINE_PLAYER_NAME_KEY, player.getUsername());
        jedis.srem(ONLINE_PLAYER_UUID_KEY, player.getUniqueId().toString());
    }

    /**
     * Loads the servers from the SERVER_STATUS_CHANNEL and registers them with the proxy server.
     * The servers are expected to be in the format "{server-name}|:|{server-ip}|:|{server-port}".
     */
    public void loadServers() {
        worker.submit(() -> {
            // formatting: {server-name}|:|{server-ip}|:|{server-port}
            jedis.smembers(SERVER_STATUS_KEY).forEach(s -> {
                InetSocketAddress address = new InetSocketAddress(s.split("\\|:\\|")[1], Integer.parseInt(s.split("\\|:\\|")[2]));
                String name = s.split("\\|:\\|")[0];
                ServerInfo serverInfo = new ServerInfo(name, address);
                System.out.println("Registering the server: " + name + " with the ip and port " + address.getAddress().getHostAddress() + ":" + address.getPort());
                plugin.getProxy().registerServer(serverInfo);
            });
        });
    }

    /**
     * Adds a server to the Redis database by constructing a server data string and adding it to the SERVER_STATUS_KEY set.
     *
     * @param info the ServerInfo object representing the server to be added
     * **/
    public void addServer(ServerInfo info) {
        String serverdata = info.getName() + "|:|" + info.getAddress().getAddress().getHostAddress() + "|:|" + info.getAddress().getPort();
        jedis.sadd(SERVER_STATUS_KEY, serverdata);
    }

    /**
     * Removes a server from the Redis database by constructing a server data string and removing it from the SERVER_STATUS_KEY set.
     *
     * @param info the ServerInfo object representing the server to be removed
     */
    public void removeServer(ServerInfo info) {
        String serverdata = info.getName() + "|:|" + info.getAddress().getAddress().getHostAddress() + "|:|" + info.getAddress().getPort();
        jedis.srem(SERVER_STATUS_KEY, serverdata);
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

