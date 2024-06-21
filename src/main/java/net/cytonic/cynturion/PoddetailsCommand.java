package net.cytonic.cynturion;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class PoddetailsCommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        if (invocation.source() instanceof Player player) {
            // TODO add permission checking
            player.sendMessage(Component.text("Pod Name: " + System.getenv("HOSTNAME"), NamedTextColor.YELLOW));
        }
    }
}
