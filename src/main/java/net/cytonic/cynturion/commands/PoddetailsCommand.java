package net.cytonic.cynturion.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.cytonic.cynturion.Cynturion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class PoddetailsCommand implements SimpleCommand {

    private final Cynturion plugin;

    public PoddetailsCommand(Cynturion plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (invocation.source() instanceof Player player) {
            if(!plugin.getPermissionManager().hasPermission("cynturion.poddetails", player.getUniqueId())) {
                player.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
                return;
            }
            player.sendMessage(Component.text("Pod Name: " + System.getenv("HOSTNAME"), NamedTextColor.YELLOW));
        }
    }
}
