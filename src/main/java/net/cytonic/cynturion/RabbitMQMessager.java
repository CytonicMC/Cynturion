package net.cytonic.cynturion;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMQMessager {

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
        factory.setHost(CynturionSettings.RABBITMQ_HOST);
        factory.setPassword(CynturionSettings.RABBITMQ_PASSWORD);
        factory.setUsername(CynturionSettings.RABBITMQ_USERNAME);
        factory.setPort(5672);
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Initializes the necessary queues for the RabbitMQ messaging system.
     * <p>
     * This method declares queues.
     * If queue declaration fails, a RuntimeException is thrown.
     *
     * @throws RuntimeException if there is an error declaring either queue
     */
    public void initializeQueues() {
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
}
