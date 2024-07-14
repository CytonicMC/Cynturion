package net.cytonic.cynturion.permissions;

import net.cytonic.cynturion.Cynturion;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.regex.Pattern;

public class PermissionManager {

    private final Cynturion plugin;

    public PermissionManager(Cynturion plugin) {
        this.plugin = plugin;
    }

    public boolean hasPermission(@NotNull Permission permission, UUID uuid) {
        return hasPermission(permission.node(), uuid);
    }

    public boolean hasPermission(@NotNull String permission, @NotNull UUID uuid) {
        for (String node : plugin.getRankManager().getRank(uuid).getPermissions()) {
            if (node.equals(permission)) {
                return true;
            }
            if (node.contains("*")) {
                String regexSanitized = Pattern.quote(node).replace("*", "\\E(.*)\\Q");
                if (permission.matches(regexSanitized)) {
                    return true;
                }
            }
        }
        return false;
    }

}
