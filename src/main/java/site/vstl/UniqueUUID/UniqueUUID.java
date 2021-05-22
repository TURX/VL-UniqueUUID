package site.vstl.UniqueUUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class UniqueUUID extends JavaPlugin {
    private static Map<String, UUID> name2uuid;
    private static Map<UUID, String> uuid2name;

    @Override
    public void onEnable() {
        File data = new File(getDataFolder(), "storage.bin");
        if (data.isFile()) {
            try (FileInputStream fileInputStream = new FileInputStream(data);
                 BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                 GZIPInputStream gzipInputStream = new GZIPInputStream(bufferedInputStream);
                 DataInputStream bin = new DataInputStream(gzipInputStream)
            ) {
                int size = bin.readInt();
                name2uuid = new HashMap<>(size);
                uuid2name = new HashMap<>(size);
                while (size-- > 0) {
                    UUID uid = new UUID(bin.readLong(), bin.readLong());
                    String usr = bin.readUTF();
                    name2uuid.put(usr, uid);
                    uuid2name.put(uid, usr);
                }
            } catch (Throwable any) {
                getLogger().log(Level.SEVERE, "Exception in reading bin database");
            }
        } else {
            name2uuid = new HashMap<>(255);
            uuid2name = new HashMap<>(255);
        }
        getServer().getPluginManager().registerEvents(new LoginListener(), this);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, this::save, 0, 10 * 20 * 60);
        getLogger().log(Level.INFO, "Enabled");
    }

    public static class LoginListener implements Listener {
        @EventHandler
        public void listen(PlayerLoginEvent event) {
            UUID sourceUUID = event.getPlayer().getUniqueId();
            String sourceName = event.getPlayer().getName();

            UUID storageUUID = name2uuid.get(sourceName);
            String storageName = uuid2name.get(sourceUUID);
            final String kickMsg = "Another account with the same username has already been registered to this server.\n" +
                    "Please try another username.\n \n" +
                    "ユーザー名が既に登録されておりますので、他のユーザー名でもう一度お願い致します。\n \n" +
                    "该用户已经用\"正版\"或者\"统一通行证\"注册，请采取正确方式登录或使用新的ID。";
            if (sourceName.equals(storageName)) {
                // UUID match and Name Match.
                return; // allowed
            } else if (storageName == null) {
                // New to server
                if (storageUUID != null) {
                    event.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickMsg);
                } else {
                    // Welcome to our server.
                    name2uuid.put(sourceName, sourceUUID);
                    uuid2name.put(sourceUUID, sourceName);
                }
            } else {
                // This account was renamed.
                if (storageUUID == null) {
                    // Target name not used.
                    name2uuid.put(sourceName, sourceUUID);
                    uuid2name.put(sourceUUID, sourceName);
                    name2uuid.remove(storageName); // name updated.
                } else {
                    event.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickMsg);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        save();
    }

    private void save() {
        Map<UUID, String> copied;
        copied = new HashMap<>(uuid2name);
        try {
            File data = new File(getDataFolder(), "storage.bin");
            getDataFolder().mkdirs();
            try (FileOutputStream fileOutputStream = new FileOutputStream(data);
                 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                 GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bufferedOutputStream);
                 DataOutputStream bin = new DataOutputStream(gzipOutputStream)
            ) {
                bin.writeInt(copied.size());
                for (Map.Entry<UUID, String> entry : copied.entrySet()) {
                    UUID uid = entry.getKey();
                    bin.writeLong(uid.getMostSignificantBits());
                    bin.writeLong(uid.getLeastSignificantBits());
                    bin.writeUTF(entry.getValue());
                }
            }
            getLogger().log(Level.INFO, "Saved");
        } catch (IOException ioException) {
            getLogger().log(Level.SEVERE, "Exception in saving storage");
        }
    }
}
