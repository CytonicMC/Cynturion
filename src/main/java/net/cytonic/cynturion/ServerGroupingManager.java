package net.cytonic.cynturion;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.cytonic.objects.ServerGroup;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerGroupingManager {
    private final List<ServerGroup> serverGroups = new ArrayList<>();
    private final Map<String, Set<RegisteredServer>> groupedServers = new ConcurrentHashMap<>();

    public void addGroupedServer(ServerGroup group, RegisteredServer info) {
        serverGroups.stream().filter(group1 -> group1.id().equals(group.id())).findAny().ifPresentOrElse(group1 -> {
        }, () -> {
            serverGroups.add(group);
        });
        Set<RegisteredServer> infos = groupedServers.computeIfAbsent(group.id(), k -> new HashSet<>());
        infos.add(info);
        groupedServers.put(group.id(), infos);
    }

    public void removeServer(String id, @Nullable ServerGroup group) {
        if (group == null) {
            groupedServers.values().forEach(registeredServers -> registeredServers.removeIf(registeredServer -> registeredServer.getServerInfo().getName().equals(id)));
            return;
        }
        groupedServers.get(group.id()).removeIf(registeredServer -> registeredServer.getServerInfo().getName().equals(id));
    }

    public void addServerGroup(ServerGroup group) {
        serverGroups.add(group);
    }

    public Optional<RegisteredServer> chooseFallback() {
        RegisteredServer info = null;
        for (ServerGroup group : serverGroups) {
            if (group.canFallback()) {
                if (groupedServers.get(group.id()) == null || groupedServers.get(group.id()).isEmpty()) {
                    continue;
                }
                Set<RegisteredServer> set = groupedServers.get(group.id());
                int randomIndex = new Random().nextInt(set.size());
                // get random server
                info = set.stream().skip(randomIndex).findFirst().orElse(null);
                break;
            }
        }
        return Optional.ofNullable(info);
    }
}
