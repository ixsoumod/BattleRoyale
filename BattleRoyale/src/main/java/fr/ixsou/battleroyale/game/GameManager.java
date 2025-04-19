package fr.ixsou.battleroyale.game;

import fr.ixsou.battleroyale.Main;
import fr.ixsou.battleroyale.utils.ScoreboardManager;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BarFlag;


import java.util.*;

public class GameManager {

    private final Main main;
    private final Set<String> countdownStarted = new HashSet<>();

    public GameManager(Main main) {
        this.main = main;
    }

    public void checkAndStartCountdown(String arenaName) {
        List<String> players = main.getConfig().getStringList("games." + arenaName + ".players");

        if (players.size() >= 20 && !countdownStarted.contains(arenaName)) {
            countdownStarted.add(arenaName);
            startCountdown(arenaName, players);
        }
    }

    public void startCountdown(String arenaName, List<String> playerNames) {
        Bukkit.broadcastMessage("§e[Battle Royale] §aLe battleroyale commence dans §e60 secondes§a !");

        new BukkitRunnable() {
            int time = 60;

            @Override
            public void run() {
                if (time == 60 || time == 30 || time == 10 || time <= 5) {
                    for (String name : playerNames) {
                        Player p = Bukkit.getPlayerExact(name);
                        if (p != null) p.sendMessage("§7Départ dans §e" + time + "s");
                    }
                }

                if (time <= 0) {
                    this.cancel();
                    teleportAndFreeze(arenaName, playerNames);
                }

                time--;
            }
        }.runTaskTimer(main, 0L, 20L);
    }

    private List<Location> loadSpawnPoints(String arenaName) {
        List<Location> locs = new ArrayList<>();
        ConfigurationSection arenaSection = main.getConfig().getConfigurationSection("games." + arenaName);
        if (arenaSection == null) return locs;

        String worldName = arenaSection.getString("world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            Bukkit.getLogger().warning("[BattleRoyale] Monde '" + worldName + "' introuvable pour le battleroyale '" + arenaName + "'");
            return locs;
        }

        List<String> raw = arenaSection.getStringList("spawnpoint");

        for (String entry : raw) {
            String[] split = entry.split(",");
            if (split.length >= 3) {
                try {
                    double x = Double.parseDouble(split[0]);
                    double y = Double.parseDouble(split[1]);
                    double z = Double.parseDouble(split[2]);
                    float yaw = (split.length >= 4) ? Float.parseFloat(split[3]) : 0.0f;
                    float pitch = (split.length >= 5) ? Float.parseFloat(split[4]) : 0.0f;
                    locs.add(new Location(world, x, y, z, yaw, pitch));
                } catch (Exception ignored) {
                    Bukkit.getLogger().warning("[BattleRoyale] Coordonnées invalides pour '" + arenaName + "': " + entry);
                }
            }
        }

        return locs;
    }

    private void freezePlayer(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setWalkSpeed(0f);
        player.setFlySpeed(0f);
        player.setSprinting(false);
        player.setFoodLevel(20);
        player.setInvulnerable(true);
    }

    private void teleportAndFreeze(String arenaName, List<String> playerNames) {
        List<Location> spawnPoints = loadSpawnPoints(arenaName);
        Collections.shuffle(spawnPoints);

        for (int i = 0; i < playerNames.size(); i++) {
            String playerName = playerNames.get(i);
            Player player = Bukkit.getPlayerExact(playerName);
            if (player != null) {
                Location spawn = (i < spawnPoints.size()) ? spawnPoints.get(i) : spawnPoints.get(0);
                player.teleport(spawn);
                ScoreboardManager.createScoreboard(player, playerNames.size(), 100);
                freezePlayer(player);
            }
        }

        new BukkitRunnable() {
            int time = 15;

            @Override
            public void run() {
                for (String name : playerNames) {
                    Player p = Bukkit.getPlayerExact(name);
                    if (p != null) {
                        p.sendTitle("§cDépart du battleroyale dans", "§e" + time + "s", 0, 20, 0);
                    }
                }

                if (time <= 0) {
                    this.cancel();
                    unfreezeAndStart(arenaName, playerNames);
                }

                time--;
            }
        }.runTaskTimer(main, 0L, 20L);
    }

    private void unfreezeAndStart(String arenaName, List<String> playerNames) {
        for (String name : playerNames) {
            Player player = Bukkit.getPlayerExact(name);
            if (player != null) {
                player.setWalkSpeed(0.2f);
                player.setFlySpeed(0.1f);
                player.setInvulnerable(false);
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealthScale(40.0);

                // Casser les blocs autour
                Location loc = player.getLocation();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        Location blockLoc = loc.clone().add(dx, 0, dz);
                        blockLoc.getBlock().breakNaturally();
                    }
                }

                main.getConfig().set("games." + arenaName + ".players", playerNames);
                main.saveConfig();



                new BukkitRunnable() {
                    int elapsed = 0;

                    @Override
                    public void run() {
                        List<String> players = main.getConfig().getStringList("games." + arenaName + ".players");
                        if (players.size() == 1) {
                            Player winner = Bukkit.getPlayerExact(players.get(0));
                            if (winner != null && winner.isOnline()) {
                                winner.sendTitle("§6Félicitations !", "§aTu as gagné la partie !", 10, 70, 20);
                                winner.sendMessage("§aTu as gagné la partie !");

                                Location loc = winner.getLocation();
                                for (int i = 0; i < 3; i++) {
                                    Firework fw = winner.getWorld().spawn(loc, Firework.class);
                                    FireworkMeta meta = fw.getFireworkMeta();
                                    meta.addEffect(FireworkEffect.builder()
                                            .withColor(Color.LIME)
                                            .with(FireworkEffect.Type.BALL_LARGE)
                                            .flicker(true)
                                            .trail(true)
                                            .build());
                                    meta.setPower(1);
                                    fw.setFireworkMeta(meta);
                                    fw.detonate();
                                }

                                Bukkit.getScheduler().runTaskLater(main, () -> {
                                    if (winner.isOnline()) {
                                        Bukkit.dispatchCommand(winner, "lobby");
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvdelete Event");
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvconfirm");
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvclone EventTemplate Event");

                                    }
                                    // Reset de l’arène
                                    main.getConfig().set("games." + arenaName + ".players", null);
                                    main.getConfig().set("games." + arenaName + ".enable", Optional.of(true));
                                    main.saveConfig();

                                }, 200L); // 200 ticks = 10 secondes

                                cancel(); // Stop la boucle
                            }

                        } else if (players.isEmpty()) {
                            cancel(); // Plus personne => stop
                        }

                        for (String name : players) {
                            Player p = Bukkit.getPlayerExact(name);
                            if (p != null) {
                                ScoreboardManager.updateScoreboard(p, elapsed, players.size(), 100);
                            }
                        }

                        elapsed++;
                    }
                }.runTaskTimer(main, 0L, 20L);


                player.sendMessage("§aLe battleroyale commence ! Bonne chance.");
            }
        }

        Bukkit.broadcastMessage("§6[Battle Royale] §fLe battleroyale §ecommence ! §fQue le meilleur gagne !");
    }
}
