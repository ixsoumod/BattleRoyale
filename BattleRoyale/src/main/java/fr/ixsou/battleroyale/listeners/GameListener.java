package fr.ixsou.battleroyale.listeners;

import fr.ixsou.battleroyale.Main;
import fr.ixsou.battleroyale.utils.ScoreboardManager;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class GameListener implements Listener {

    private final Main main;

    public GameListener(Main main) {
        this.main = main;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String playerName = player.getName();
        ConfigurationSection gamesSection = main.getConfig().getConfigurationSection("games");

        if (gamesSection == null) return;

        for (String arena : gamesSection.getKeys(false)) {
            List<String> players = main.getConfig().getStringList("games." + arena + ".players");

            if (players.contains(playerName)) {
                // Éclair
                player.getWorld().strikeLightningEffect(player.getLocation());

                // Passer en "spectateur"
                player.setGameMode(GameMode.SURVIVAL);
                player.setAllowFlight(true);
                player.setFlying(true);
                player.setInvulnerable(true);
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 1, false, false, false));


                ScoreboardManager.removeScoreboard(player);


                // Retirer le joueur de la partie
                players.remove(playerName);
                main.getConfig().set("games." + arena + ".players", players);
                main.saveConfig();

                for (String otherName : players) {
                    Player otherPlayer = Bukkit.getPlayerExact(otherName);
                    if (otherPlayer != null && !otherPlayer.getName().equals(playerName)) {
                        otherPlayer.sendMessage("§c" + playerName + " est mort !");
                    }
                }

            }

            }
    }

    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World newWorld = player.getWorld();
        ConfigurationSection gamesSection = main.getConfig().getConfigurationSection("games");

        if (gamesSection == null) return;

        for (String arena : gamesSection.getKeys(false)) {
            List<String> players = main.getConfig().getStringList("games." + arena + ".players");

            if (players.contains(player.getName())) {
                String worldName = main.getConfig().getString("games." + arena + ".world");
                World gameWorld = Bukkit.getWorld(worldName);
                if (gameWorld == null || !newWorld.getName().equalsIgnoreCase(gameWorld.getName())) {
                    // Quitte le monde de la game => Reset
                    players.remove(player.getName());
                    main.getConfig().set("games." + arena + ".players", players);
                    main.saveConfig();

                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.setInvulnerable(false);
                    player.setGameMode(GameMode.SURVIVAL);
                    player.removePotionEffect(PotionEffectType.INVISIBILITY);
                    resetPlayerState(player);
                    ScoreboardManager.removeScoreboard(player);
                    player.sendMessage("§cTu as quitté le battleroyale !");
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ConfigurationSection gamesSection = main.getConfig().getConfigurationSection("games");

        if (gamesSection == null) return;

        for (String arena : gamesSection.getKeys(false)) {
            List<String> players = main.getConfig().getStringList("games." + arena + ".players");

            if (players.contains(player.getName())) {
                // Retirer le joueur de la partie
                players.remove(player.getName());
                main.getConfig().set("games." + arena + ".players", players);
                main.saveConfig();
                ScoreboardManager.removeScoreboard(player);


                // Reset vie & états
                resetPlayerState(player);
            }
        }
    }

    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ConfigurationSection gamesSection = main.getConfig().getConfigurationSection("games");

        if (gamesSection == null) return;

        for (String arena : gamesSection.getKeys(false)) {
            List<String> players = main.getConfig().getStringList("games." + arena + ".players");
            resetPlayerState(player);
            ScoreboardManager.removeScoreboard(player);


            if (players.contains(player.getName())) {
                resetPlayerState(player); // On reset bien l'absorption, fly etc.
            }
        }
    }



    public void resetPlayerState(Player player) {
        ScoreboardManager.removeScoreboard(player);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setInvulnerable(false);
        player.setGameMode(GameMode.SURVIVAL);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.setAbsorptionAmount(0); // Supprime la double barre de vie
        player.setHealthScale(20.0); // Réinitialise la vie
    }

}
