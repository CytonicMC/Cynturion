package net.cytonic.cynturion.messaging.pubsub;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.cytonic.containers.ServerStatusContainer;
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
    @SuppressWarnings("preview")
    public void onMessage(String channel, String message) {
        if (channel.equals(RedisDatabase.SERVER_STATUS_CHANNEL)) {
            System.out.println(STR."Server status message: \{message}");

            ServerStatusContainer container = ServerStatusContainer.deserialize(message);
            ServerInfo info = new ServerInfo(container.serverName(), new InetSocketAddress(container.ip(), container.port()));
            if (container.mode() == ServerStatusContainer.Mode.START) {
                System.out.println(STR."Registering the server: \{container.serverName()} with the ip and port \{container.ip()}:\{container.port()}");
                RegisteredServer rs = plugin.getProxy().registerServer(info);
                redis.addServer(info);
                plugin.getServerGroupingManager().addGroupedServer(container.group(), rs);
            } else if (container.mode() == ServerStatusContainer.Mode.STOP) {
                System.out.println(STR."Unregistering the server: \{container.serverName()} with the ip and port \{container.ip()}:\{container.port()}");
                RegisteredServer rs = plugin.getProxy().getServer(info.getName()).orElse(null);
                if (rs == null) {
                    return;
                }
                plugin.getProxy().unregisterServer(info);
                plugin.getServerGroupingManager().addGroupedServer(container.group(), rs);
                redis.removeServer(info);
            }
        }
    }
}