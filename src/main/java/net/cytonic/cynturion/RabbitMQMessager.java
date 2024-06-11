package net.cytonic.cynturion;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import java.io.IOException;
import java.lang.reflect.MalformedParametersException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class RabbitMQMessager {

    public static final String SERVER_DECLARE_QUEUE = "server-declaration";
    public static final String SHUTDOWN_QUEUE = "server-shutdown";
    public static final String PLAYER_KICK_QUEUE = "player-kick";
    private final Cynturion plugin;
    private Connection connection;
    private Channel channel;

    public RabbitMQMessager(Cynturion plugin) {
        this.plugin = plugin;
    }

    /**
     * Initializes the connection to the RabbitMQ server.
     * <p>
     * This function creates a new ConnectionFactory object and sets its host,
     * password, username, and port based on the environment variables
     * "RABBITMQ_HOST", "RABBITMQ_PASSWORD", "RABBITMQ_USERNAME", and "RABBITMQ_PORT".
     * It then attempts to create a new connection using the ConnectionFactory
     * and store it in the "connection" variable. If an IOException or
     * TimeoutException is caught, a RuntimeException is thrown.
     * <p>
     * After successfully creating the connection, the function attempts to
     * create a new channel using the connection and store it in the "channel"
     * variable. If an IOException or TimeoutException is caught, a RuntimeException
     * is thrown.
     *
     * @throws RuntimeException if there is an error creating the connection or channel
     */
    public void initializeConnection() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(System.getProperty("RABBITMQ_HOST"));
        factory.setPassword(System.getProperty("RABBITMQ_PASSWORD"));
        factory.setUsername(System.getProperty("RABBITMQ_USERNAME"));
        factory.setPort(5672);
        try {
            connection = factory.newConnection();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
        try {
            channel = connection.createChannel();
            connection = factory.newConnection();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Initializes the necessary queues for the RabbitMQ messaging system.
     * <p>
     * This method declares two queues: SERVER_DECLARE_QUEUE and SHUTDOWN_QUEUE.
     * If either queue declaration fails, a RuntimeException is thrown.
     *
     * @throws RuntimeException if there is an error declaring either queue
     */
    public void initializeQueues() {
        try {
            channel.queueDeclare(SERVER_DECLARE_QUEUE, false, false, false, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            channel.queueDeclare(SHUTDOWN_QUEUE, false, false, false, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            channel.queueDeclare(PLAYER_KICK_QUEUE, false, false, false, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Consumes server declare messages from the RabbitMQ queue and registers the servers with the proxy server.
     * The message format is "{server-name}|:|{server-ip}|:|{server-port}".
     * If the server name is null or "null", a random name is generated.
     * TODO: Fix the whole id thing
     *
     * @throws MalformedParametersException if the received message is malformed
     * @throws RuntimeException             if there is an error registering the server
     */
    public void consumeServerDeclareMessages() {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            //formatting: {server-name}|:|{server-ip}|:|{server-port}
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            String[] parts = message.split("\\|:\\|");
            if (parts.length != 3) throw new MalformedParametersException("The recived message is malformed.");
            String name = parts[0];
            if (name == null || name.equalsIgnoreCase("null")) {
                Random random = new Random();
                StringBuilder id = new StringBuilder(3);

                for (int i = 0; i < 3; i++) {
                    char letter = (char) ('A' + random.nextInt(26));
                    id.append(letter);
                }
                name = id.toString();
            }
            String ip = parts[1];
            String port = parts[2];
            System.out.println("Registering the server: \"" + name + "\" with the ip and port " + ip + ":" + port);
            plugin.getProxy().registerServer(new ServerInfo(name, new InetSocketAddress(ip, Integer.parseInt(port))));

        };
        try {
            channel.basicConsume(SERVER_DECLARE_QUEUE, true, deliverCallback, consumerTag -> {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void consumeServerShutdownMessages() {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            //formatting: {server-name}|:|{server-ip}|:|{server-port}
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            String[] parts = message.split("\\|:\\|");
            if (parts.length != 3) throw new MalformedParametersException("The recived message is malformed.");
            String name = parts[0];
            if (name == null || name.equalsIgnoreCase("null"))
                throw new MalformedParametersException("The recived message is malformed.");

            String ip = parts[1];
            String port = parts[2];
            System.out.println("Unregistering the server: \"" + name + "\" with the ip and port " + ip + ":" + port);
            try {
                plugin.getProxy().unregisterServer(new ServerInfo(name, new InetSocketAddress(ip, Integer.parseInt(port))));
            } catch (Exception ignored) {
            }
        };
        try {
            channel.basicConsume(SHUTDOWN_QUEUE, true, deliverCallback, consumerTag -> {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Sends a message in RabbitMQ that a server has timed out.
     * If the message cannot be sent, a RuntimeException is thrown.
     *
     * @throws RuntimeException if there is an error sending the message
     */
    public void sendServerTimeoutMessage(ServerInfo info) {
        // formatting: {server-name}|:|{server-ip}|:|{server-port}
        String serverdata = info.getName() + "|:|" + info.getAddress().getAddress().getHostAddress() + "|:|" + info.getAddress().getPort();
        try {
            channel.basicPublish("", SHUTDOWN_QUEUE, null, serverdata.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Closes the connection to RabbitMQ and throws a RuntimeException if an IOException occurs.
     *
     * @throws RuntimeException if an IOException occurs while closing the connection
     */
    public void shutdown() {
        try {
            connection.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void consumePlayerKickMessages() {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            // FORMAT: {uuid}|:|{reason}|:|{name}|:|{message}|:|{rescuable}
            String rawMessage = new String(delivery.getBody(), StandardCharsets.UTF_8);
            String[] parts = rawMessage.split("\\|:\\|");
            if (parts.length != 5) throw new MalformedParametersException("The recived message is malformed.");

            UUID uuid = UUID.fromString(parts[0]);
            if (plugin.getProxy().getPlayer(uuid).isPresent()) {
                String reason = parts[1];
                String name = parts[2];
                Component message = JSONComponentSerializer.json().deserialize(parts[3]);
                boolean rescuable = Boolean.parseBoolean(parts[4]);

                if (rescuable) {
                    // todo: implement rescuing using backup lobby servers or something
                    return;
                }

                plugin.getProxy().getPlayer(uuid).get().disconnect(message);

                System.out.println("Kicking player: " + name + " with reason: " + reason);
            }
        };
        try {
            channel.basicConsume(PLAYER_KICK_QUEUE, true, deliverCallback, consumerTag -> {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
