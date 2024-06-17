package net.cytonic.cynturion.data.obj;


/**
 * A class that holds data about a Cytosis server
 */
public record CytonicServer(String ip, String id, int port) {

    public String serialize() {
        return String.format("%s|%s|%d", ip, id, port);
    }

    public static CytonicServer deserialize(String serialized) {
        String[] parts = serialized.split("\\|");
        return new CytonicServer(parts[0], parts[1], Integer.parseInt(parts[2]));
    }
}