package net.cytonic.cynturion.messaging.pubsub;

import com.velocitypowered.api.proxy.server.ServerInfo;
import net.cytonic.cynturion.Cynturion;
import net.cytonic.cynturion.RedisDatabase;
import redis.clients.jedis.JedisPubSub;

import java.net.InetSocketAddress;

public class ServerStatus extends JedisPubSub {
    private final Cynturion plugin;
    private final RedisDatabase redis;

    public ServerStatus(Cynturion plugin, RedisDatabase redis) {
        this.plugin = plugin;
        this.redis = redis;
    }

    @Override
    public void onMessage(String channel, String message) {
        if (channel.equals(RedisDatabase.SERVER_STATUS_CHANNEL)) {
            // formatting: <START/STOP>|:|<SERVER_ID>|:|<SERVER_IP>|:|<SERVER_PORT>

            String[] parts = message.split("\\|:\\|");
            String name = parts[1];
            String ip = parts[2];
            int port = Integer.parseInt(parts[3]);
            ServerInfo info = new ServerInfo(name, new InetSocketAddress(ip, port));
            if (parts[0].equalsIgnoreCase("START")) {
                System.out.println("Registering the server: " + name + " with the ip and port " + ip + ":" + port);
                plugin.getProxy().registerServer(info);
                redis.addServer(info);
            } else if (parts[0].equalsIgnoreCase("STOP")) {
                System.out.println("Unregistering the server: " + name + " with the ip and port " + ip + ":" + port);
                plugin.getProxy().unregisterServer(info);
                redis.removeServer(info);
            }
        }
    }
}
