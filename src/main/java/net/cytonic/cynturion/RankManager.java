package net.cytonic.cynturion;

import com.google.inject.Inject;
import net.cytonic.enums.PlayerRank;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RankManager {
    @Inject
    private Logger logger;
    private final Cynturion plugin;
    private ConcurrentHashMap<UUID, PlayerRank> ranks = new ConcurrentHashMap<>();

    public RankManager(Cynturion plugin) {
        this.plugin = plugin;
    }

    /**
     * !!!! THIS DOES NOT CHANGE THE VALUE IN THE DATABASE !!!!
     * @param uuid the uuid of the player
     * @param rank the new rank
     */
    public void setRank(UUID uuid, PlayerRank rank) {
        ranks.put(uuid, rank);
    }

    /**
     * todo: Listen on redis for rank changes
     * @param uuid the uuid of the player
     * @return the rank of the player
     */
    public PlayerRank getRank(UUID uuid) {
        return ranks.get(uuid);
    }

    public void loadRank(UUID uuid) {
        plugin.getDatabase().getPlayerRank(uuid).whenComplete((playerRank, throwable) -> {
            if (throwable == null) {
                ranks.put(uuid, playerRank);
                logger.info("Loaded rank " + playerRank + " for " + uuid);
            } else {
                ranks.put(uuid, PlayerRank.DEFAULT);
            }
        });
    }


}
