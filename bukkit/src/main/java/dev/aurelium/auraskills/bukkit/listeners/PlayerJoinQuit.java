package dev.aurelium.auraskills.bukkit.listeners;

import dev.aurelium.auraskills.api.event.AuraSkillsEventHandler;
import dev.aurelium.auraskills.api.event.AuraSkillsListener;
import dev.aurelium.auraskills.api.event.user.UserLoadEvent;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.user.BukkitUser;
import dev.aurelium.auraskills.common.config.Option;
import dev.aurelium.auraskills.common.storage.sql.SqlStorageProvider;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PlayerJoinQuit implements Listener, AuraSkillsListener {

    private final AuraSkills plugin;

    public PlayerJoinQuit(AuraSkills plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getStorageProvider() instanceof SqlStorageProvider) { // Handles MySQL storage
            if (plugin.configBoolean(Option.MYSQL_ALWAYS_LOAD_ON_JOIN) || !plugin.getUserManager().hasUser(player.getUniqueId())) {
                int loadDelay = plugin.configInt(Option.MYSQL_LOAD_DELAY);
                if (loadDelay == 0) {
                    // Load immediately
                    loadUserAsync(player);
                } else {
                    // Delay loading
                    plugin.getScheduler().scheduleSync(() -> loadUserAsync(player), loadDelay * 50L, TimeUnit.MILLISECONDS);
                }
            }
        } else { // Yaml storage
            if (!plugin.getUserManager().hasUser(player.getUniqueId())) {
                loadUserAsync(player);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        User user = plugin.getUser(player);

        plugin.getScheduler().executeAsync(() -> {
            try {
                plugin.getStorageProvider().save(user);
                plugin.getUserManager().removeUser(player.getUniqueId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void loadUserAsync(Player player) {
        plugin.getScheduler().executeAsync(() -> {
            try {
                User user = plugin.getStorageProvider().load(player.getUniqueId());
                plugin.getUserManager().addUser(user);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @AuraSkillsEventHandler
    public void onUserLoad(UserLoadEvent event) {
        detectUserLanguage(BukkitUser.getUser(event.getUser()), BukkitUser.getPlayer(event.getUser()));
    }

    private void detectUserLanguage(User user, Player player) {
        if (user.getLocale() != null || !plugin.configBoolean(Option.TRY_DETECT_CLIENT_LANGUAGE)) {
            return;
        }

        try {
            Locale locale = new Locale(player.getLocale().split("_")[0].toLowerCase(Locale.ROOT));
            if (plugin.getMessageProvider().getLoadedLanguages().contains(locale)) {
                user.setLocale(locale);
            }
        } catch (Exception ignored) {}
    }

}
