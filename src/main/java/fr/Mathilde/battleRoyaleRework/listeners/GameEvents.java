package fr.Mathilde.battleRoyaleRework.listeners;


import fr.Mathilde.battleRoyaleRework.BattleRoyaleRework;
import fr.Mathilde.battleRoyaleRework.managers.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class GameEvents implements Listener {

    private final BattleRoyaleRework plugin;

    public GameEvents(BattleRoyaleRework plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        handlePlayerExit(event.getEntity(), true);
    }

    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World newWorld = player.getWorld();
        ConfigurationSection gamesSection = plugin.getConfig().getConfigurationSection("games");

        if (gamesSection == null) return;

        for (String arena : gamesSection.getKeys(false)) {
            List<String> players = plugin.getConfig().getStringList("games." + arena + ".players");

            if (players.contains(player.getName())) {
                String worldName = plugin.getConfig().getString("games." + arena + ".world");
                World gameWorld = Bukkit.getWorld(worldName);
                if (gameWorld == null || !newWorld.getName().equalsIgnoreCase(gameWorld.getName())) {
                    handlePlayerExit(player, false);
                    player.sendMessage("§cTu as quitté le battleroyale !");
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        handlePlayerExit(event.getPlayer(), false);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ConfigurationSection gamesSection = plugin.getConfig().getConfigurationSection("games");

        if (gamesSection == null) return;

        for (String arena : gamesSection.getKeys(false)) {
            List<String> players = plugin.getConfig().getStringList("games." + arena + ".players");
            resetPlayerState(player);
            ScoreboardManager.removeScoreboard(player);

            if (players.contains(player.getName())) {
                resetPlayerState(player);
            }
        }
    }

    private void handlePlayerExit(Player player, boolean isDeath) {
        String playerName = player.getName();
        ConfigurationSection gamesSection = plugin.getConfig().getConfigurationSection("games");

        if (gamesSection == null) return;

        for (String arena : gamesSection.getKeys(false)) {
            List<String> players = plugin.getConfig().getStringList("games." + arena + ".players");

            if (players.contains(playerName)) {
                if (isDeath) {
                    player.getWorld().strikeLightningEffect(player.getLocation());
                    player.setGameMode(GameMode.SURVIVAL);
                    player.setAllowFlight(true);
                    player.setFlying(true);
                    player.setInvulnerable(true);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 1, false, false, false));
                }

                players.remove(playerName);
                plugin.getConfig().set("games." + arena + ".players", players);
                plugin.saveConfig();

                for (String otherName : players) {
                    Player otherPlayer = Bukkit.getPlayerExact(otherName);
                    if (otherPlayer != null) {
                        otherPlayer.sendMessage("§c" + playerName + " est mort !");
                    }
                }

                resetPlayerState(player);
                ScoreboardManager.removeScoreboard(player);
                break;
            }
        }
    }

    private void resetPlayerState(Player player) {
        ScoreboardManager.removeScoreboard(player);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setInvulnerable(false);
        player.setGameMode(GameMode.SURVIVAL);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.setAbsorptionAmount(0);
        player.setHealthScale(20.0);
    }

}
