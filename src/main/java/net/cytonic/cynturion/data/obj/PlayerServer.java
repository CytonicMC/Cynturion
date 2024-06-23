package net.cytonic.cynturion.data.obj;

import java.util.UUID;

public record PlayerServer(UUID uuid, CytonicServer server) {

    public static PlayerServer deserialize(String serialized) {
        String[] parts = serialized.split("\\|");
        return new PlayerServer(UUID.fromString(parts[0]),new CytonicServer(parts[1], parts[2], Integer.parseInt(parts[3])));
    }
}
