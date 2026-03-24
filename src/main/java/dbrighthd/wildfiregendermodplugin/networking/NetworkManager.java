package dbrighthd.wildfiregendermodplugin.networking;

import dbrighthd.wildfiregendermodplugin.GenderModPlugin;
import dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftInputStream;
import dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftOutputStream;
import dbrighthd.wildfiregendermodplugin.networking.wildfire.*;
import dbrighthd.wildfiregendermodplugin.wildfire.ModConstants;
import dbrighthd.wildfiregendermodplugin.wildfire.ModUser;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Управляет сетевым обменом с клиентом Wildfire Gender Mod
 */
public class NetworkManager {

    private static final Map<Integer, ModSyncPacket> PACKET_FORMATS = Map.of(
        5, new ModSyncPacketV5()
        // Если в будущем добавишь V6 — просто добавь сюда: 6, new ModSyncPacketV6()
    );

    private final GenderModPlugin plugin;
    private ModSyncPacket packetFormat;   // текущий используемый формат

    public NetworkManager(GenderModPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Инициализация — выбираем версию протокола
     * @return true, если успешно выбрали формат пакета
     */
    public boolean init() {
        int configVersion = plugin.getConfig().getInt("mod.protocol", 5);

        packetFormat = PACKET_FORMATS.get(configVersion);

        // Если в конфиге указана версия, которой нет — fallback на 5
        if (packetFormat == null) {
            plugin.getCustomLogger().warning("Protocol version %d not found! Falling back to version 5.", configVersion);
            packetFormat = PACKET_FORMATS.get(5);
        }

        if (packetFormat == null) {
            plugin.getCustomLogger().severe("No packet format available! Mod sync will not work.");
            return false;
        }

        plugin.getCustomLogger().info("Using protocol v%d for mod version(s) %s",
            packetFormat.getVersion(), packetFormat.getModRange());

        return true;
    }

    /**
     * Отправляет настройки всех игроков указанной аудитории
     */
    public void sync(Collection<? extends Player> audience) {
        if (packetFormat == null) return;

        for (ModUser user : plugin.getUserManager().getUsers().values()) {
            byte[] fabricData = serializeUser(user, false);
            byte[] forgeData = serializeUser(user, true);

            for (Player recipient : audience) {
                if (fabricData.length > 0) {
                    sendData(recipient, ModConstants.SYNC, fabricData);
                }
                if (forgeData.length > 0) {
                    sendData(recipient, ModConstants.FORGE, forgeData);
                }
            }
        }
    }

    /**
     * Десериализация данных от клиента
     */
    public ModUser deserializeUser(byte[] data, boolean isForge) {
        if (packetFormat == null) return null;

        try (CraftInputStream input = CraftInputStream.ofBytes(data)) {
            if (isForge) {
                input.readByte(); // пропускаем байт Forge
            }

            return packetFormat.read(input);
        } catch (IOException ex) {
            plugin.getCustomLogger().warning(ex, "Could not deserialize user (forge=%s)", isForge);
        } catch (Exception ex) {
            plugin.getCustomLogger().warning(ex, "Unexpected error while deserializing user data");
        }

        return null;
    }

    /**
     * Сериализация данных для отправки клиенту
     */
    private byte[] serializeUser(ModUser user, boolean isForge) {
        if (packetFormat == null) return new byte[0];

        try (ByteArrayOutputStream payload = new ByteArrayOutputStream();
             CraftOutputStream output = new CraftOutputStream(payload)) {

            if (isForge) {
                output.writeByte(1);
            }

            packetFormat.write(user, output);
            return payload.toByteArray();

        } catch (IOException ex) {
            plugin.getCustomLogger().warning(ex, "Could not serialize user (forge=%s)", isForge);
        } catch (Exception ex) {
            plugin.getCustomLogger().warning(ex, "Unexpected error while serializing user data");
        }

        return new byte[0];
    }

    private void sendData(Player target, String channel, byte[] data) {
        if (data.length == 0) return;
        target.sendPluginMessage(plugin, channel, data);
    }

    // Геттер на случай, если где-то понадобится
    public ModSyncPacket getPacketFormat() {
        return packetFormat;
    }
}