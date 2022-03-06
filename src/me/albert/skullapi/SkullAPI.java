package me.albert.skullapi;

import me.albert.skullapi.mysql.MySQL;
import me.albert.skullapi.utils.CustomConfig;
import me.albert.skullapi.utils.SkullUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.atomic.AtomicInteger;

public class SkullAPI extends JavaPlugin {
    public static CustomConfig mysqlSettings;
    public static CustomConfig mainSettings;
    public static AtomicInteger rateLimit = new AtomicInteger(0);
    public static boolean enableSave = false;
    public static Integer delay = 10;
    public static Integer limit = 300;
    private static SkullAPI instance;

    public static SkullAPI getInstance() {
        return instance;
    }

    public static ItemStack getSkull(String playerName) {
        playerName = playerName.toLowerCase();
        if (MySQL.ENABLED) {
            String[] texture = MySQL.getPlayer(playerName);
            if (texture == null) return null;
            return SkullUtil.create(texture[0], texture[1]);
        }
        String signature = getInstance().getConfig().getString(playerName + ".signature");
        String value = getInstance().getConfig().getString(playerName + ".value");
        if (signature != null && value != null) {
            return SkullUtil.create(signature, value);
        }
        return null;
    }

    private static void saveHeads() {
        Bukkit.getScheduler().runTaskLaterAsynchronously(SkullAPI.getInstance(), () -> {
            try {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    try {
                        SkullUtil.savePlayer(p);
                    } catch (Exception ignored) {
                    }
                }
                if (!MySQL.ENABLED)
                    Bukkit.getScheduler().runTask(SkullAPI.getInstance(), SkullAPI.getInstance()::saveConfig);
            } catch (Exception ignored) {
            }
            saveHeads();
        }, 20L * delay);
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        mysqlSettings = new CustomConfig("mysql.yml", this);
        mainSettings = new CustomConfig("settings.yml", this);
        enableSave = mainSettings.getConfig().getBoolean("enable-save");
        limit = mainSettings.getConfig().getInt("mojang-limit");
        delay = mainSettings.getConfig().getInt("save-delay");
        if (mysqlSettings.getConfig().getBoolean("enable")) {
            MySQL.setUP();
        }
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            rateLimit.set(0);
        }, 20 * 60 * 10, 20 * 60 * 10);
        if (enableSave) {
            saveHeads();
        }
        getLogger().info("Loaded");
    }

    @Override
    public void onDisable() {
        if (MySQL.ENABLED) {
            MySQL.close();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cPlayer only");
            return true;
        }
        Player p = (Player) sender;
        if (args.length == 2 && args[0].equalsIgnoreCase("get")) {
            ItemStack head = getSkull(args[1]);
            if (head == null) {
                sender.sendMessage("§cplayer no recorded!");
                return true;
            }
            p.getInventory().addItem(head);
            p.sendMessage("§asuccess");
            return true;
        }
        sender.sendMessage("§b/skullapi get player");
        return true;
    }
}
