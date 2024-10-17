package net.cytonic.cynturion.data;

import com.google.inject.Inject;
import net.cytonic.auditlog.Category;
import net.cytonic.auditlog.Entry;
import net.cytonic.cynturion.Cynturion;
import net.cytonic.cynturion.CynturionSettings;
import net.cytonic.enums.PlayerRank;
import net.cytonic.objects.BanData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.sql.*;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CytonicDatabase {

    @Inject
    private Logger logger;
    private final ExecutorService worker;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean ssl;
    private Connection connection;
    private Cynturion plugin;

    /**
     * Creates and initializes a new Mysql Database
     */
    public CytonicDatabase(Cynturion plugin) {
        this.worker = Executors.newSingleThreadExecutor();
        this.host = CynturionSettings.DATABASE_HOST;
        this.port = CynturionSettings.DATABASE_PORT;
        this.database = CynturionSettings.DATABASE_NAME;
        this.username = CynturionSettings.DATABASE_USER;
        this.password = CynturionSettings.DATABASE_PASSWORD;
        this.ssl = CynturionSettings.DATABASE_USE_SSL;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load database driver", e);
        }
    }

    /**
     * Checks if the database is connected
     *
     * @return if the database is connected
     */
    public boolean isConnected() {
        return (connection != null);
    }

    /**
     * connects to the database
     *
     * @return a future that completes when the connection is successful
     */
    public CompletableFuture<Void> connect() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        worker.submit(() -> {
            if (!isConnected()) {
                try {
                    connection = DriverManager.getConnection(STR."jdbc:mysql://\{host}:\{port}/\{database}?useSSL=\{ssl}&autoReconnect=true&allowPublicKeyRetrieval=true", username, password);
                    logger.info("Successfully connected to the MySQL Database!");
                    future.complete(null);
                } catch (SQLException e) {
                    logger.error("Invalid Database Credentials!", e);
                    future.completeExceptionally(e);
                }
            }
        });
        return future;
    }

    /**
     * Disconnects from the database server
     */
    public void disconnect() {
        worker.submit(() -> {
            if (isConnected()) {
                try {
                    connection.close();
                    logger.info("Database connection closed!");
                } catch (SQLException e) {
                    logger.error("An error occurred whilst disconnecting from the database. Please report the following stacktrace to CytonicMC: ", e);
                }
            }
        });
        worker.shutdown();
    }

    /**
     * Gets the player's rank. This returns {@link PlayerRank#DEFAULT} even if the player doesn't exist.
     *
     * @param uuid the player to fetch the id from
     * @return The player's {@link PlayerRank}
     * @throws IllegalStateException if the database isn't connected
     */
    @NotNull
    public CompletableFuture<PlayerRank> getPlayerRank(@NotNull final UUID uuid) {
        CompletableFuture<PlayerRank> future = new CompletableFuture<>();
        if (!isConnected())
            throw new IllegalStateException("The database must have an open connection to fetch a player's rank!");
        worker.submit(() -> {
            try {
                PreparedStatement ps = connection.prepareStatement("SELECT rank_id FROM cytonic_ranks WHERE uuid = ?");
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    future.complete(PlayerRank.valueOf(rs.getString("rank_id")));
                } else {
                    future.complete(PlayerRank.DEFAULT);
                    setPlayerRank(uuid, PlayerRank.DEFAULT);
                }
            } catch (SQLException e) {
                logger.error("An error occurred whilst fetching the rank of '{}'", uuid);
            }
        });
        return future;
    }

    /**
     * Sets the given player's rank to the specified rank.
     *
     * @param uuid The player's UUID
     * @param rank The player's rank constant
     * @return a future that completes when the update is complete
     * @throws IllegalStateException if the database isn't connected
     */
    public CompletableFuture<Void> setPlayerRank(UUID uuid, PlayerRank rank) {
        if (!isConnected())
            throw new IllegalStateException("The database must have an open connection to set a player's rank!");
        CompletableFuture<Void> future = new CompletableFuture<>();
        worker.submit(() -> {
            try {
                PreparedStatement ps = connection.prepareStatement("INSERT INTO cytonic_ranks (uuid, rank_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE rank_id = VALUES(rank_id)");
                ps.setString(1, uuid.toString());
                ps.setString(2, rank.name());
                ps.executeUpdate();
                future.complete(null);
            } catch (SQLException e) {
                logger.error("An error occurred whilst setting the rank of '{}' to '{}", uuid.toString(), rank.name());
            }
        });
        return future;
    }

    /**
     * The concurrent friendly way to fetch a player's ban status
     *
     * @param uuid THe player to check
     * @return The CompletableFuture that holds the player's ban status
     */
    public CompletableFuture<BanData> isBanned(UUID uuid) {
        if (!isConnected()) throw new IllegalStateException("The database must be connected.");
        CompletableFuture<BanData> future = new CompletableFuture<BanData>();
        worker.submit(() -> {
            try {
                PreparedStatement ps = connection.prepareStatement("SELECT * FROM cytonic_bans WHERE uuid = ?");
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    Instant expiry = Instant.parse(rs.getString("to_expire"));
                    if (expiry.isBefore(Instant.now())) {
                        future.complete(new BanData(null, null, false));
                        unbanPlayer(uuid, new Entry(uuid, null, Category.UNBAN, "Natural Expiration"));
                    } else {
                        try {
                            BanData banData = new BanData(rs.getString("reason"), expiry, true);
                            future.complete(banData);
                        } catch (Exception e) {
                            logger.error(STR."An error occurred whilst determining if the player \{uuid} is banned.", e);
                            future.complete(new BanData(null, null, true));
                        }
                    }
                } else {
                    future.complete(new BanData(null, null, false));
                }
            } catch (SQLException e) {
                logger.error(STR."An error occurred whilst determining if the player \{uuid} is banned.", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Unbans a player
     *
     * @param uuid the player to unban
     * @return a future that completes when the player is unbanned
     */
    public CompletableFuture<Void> unbanPlayer(UUID uuid, Entry entry) {
        if (!isConnected()) throw new IllegalStateException("The database must be connected.");
        CompletableFuture<Void> future = new CompletableFuture<>();
        worker.submit(() -> {
            try {
                addAuditLogEntry(entry);
                PreparedStatement ps = connection.prepareStatement("DELETE FROM cytonic_bans WHERE uuid = ?");
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
                future.complete(null);
            } catch (SQLException e) {
                logger.error(STR."An error occurred whilst unbanning the player \{uuid}.", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Adds an auditlog entry
     *
     * @param entry The entry to add
     * @return a future that completes when the entry is added
     */
    public CompletableFuture<Void> addAuditLogEntry(Entry entry) {
        if (!isConnected()) throw new IllegalStateException("The database must be connected to add an auditlog entry.");
        CompletableFuture<Void> future = new CompletableFuture<>();
        worker.submit(() -> {
            PreparedStatement ps;
            try {
                ps = connection.prepareStatement("INSERT INTO cytonic_audit_log (timestamp, uuid, reason, category, actor) VALUES (CURRENT_TIMESTAMP,?,?,?,?)");
                ps.setString(1, entry.uuid().toString());
                ps.setString(2, entry.reason());
                ps.setString(3, entry.category().name());
                ps.setString(4, entry.actor().toString());
                ps.executeUpdate();
                future.complete(null);
            } catch (SQLException e) {
                logger.error("An error occurred whilst adding an auditlog entry!", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}
