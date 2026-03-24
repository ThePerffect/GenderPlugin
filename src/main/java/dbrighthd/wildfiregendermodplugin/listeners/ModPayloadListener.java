package dbrighthd.wildfiregendermodplugin.listeners;

import dbrighthd.wildfiregendermodplugin.GenderModPlugin;
import dbrighthd.wildfiregendermodplugin.wildfire.ModConstants;
import dbrighthd.wildfiregendermodplugin.wildfire.ModUser;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ModPayloadListener implements PluginMessageListener {

    private final GenderModPlugin plugin;

    public ModPayloadListener(GenderModPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (!channel.equals(ModConstants.SEND_GENDER_INFO) && !channel.equals(ModConstants.FORGE)) {
            return;
        }

        ModUser received = plugin.getNetworkManager().deserializeUser(message, channel.equals(ModConstants.FORGE));
        if (received == null) return;

        UUID realUUID = player.getUniqueId();
        UUID sentUUID = received.userId();

        if (!realUUID.equals(sentUUID)) {
            received = new ModUser(realUUID, received.configuration());
        }

        plugin.getUserManager().getUsers().put(realUUID, received);



        plugin.getNetworkManager().sync(plugin.getServer().getOnlinePlayers());
    }
}