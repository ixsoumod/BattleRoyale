package fr.Mathilde.battleRoyaleRework.managers;

//import fr.ixsou.battleroyale.Main;
//import fr.ixsou.battleroyale.utils.ScoreboardManager;
import fr.Mathilde.battleRoyaleRework.BattleRoyaleRework;
import fr.Mathilde.battleRoyaleRework.ManageWorld.WorldManager;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class GameManager {

    private final BattleRoyaleRework plugin;
    private final Map<String, Set<String>> arenaPlayers = new ConcurrentHashMap<>();
    private final Set<String> countdownStarted = ConcurrentHashMap.newKeySet();
    private final Map<String, BukkitTask> arenaTasks = new ConcurrentHashMap<>();

    // Constants to improve readability and ease of configuration
    public static final int PLAYERS_TO_START = 20;
    private static final int COUNTDOWN_SECONDS = 60;
    private static final int PRE_GAME_FREEZE_SECONDS = 15;
    private static final int PLAYER_HEALTH_SCALE = 40;
    private static final int WINNER_CELEBRATION_TICKS = 200; // 10 seconds

    // Cache configuration values
    private final Map<String, List<Location>> spawnPointCache = new HashMap<>();

    public GameManager(BattleRoyaleRework plugin) {
        this.plugin = plugin;
        // Pre-load spawn points for all arenas
        loadAllSpawnPoints();
    }

    private void loadAllSpawnPoints() {
        ConfigurationSection gamesSection = plugin.getConfig().getConfigurationSection("games");
        if (gamesSection == null) return;

        for (String arenaName : gamesSection.getKeys(false)) {
            spawnPointCache.put(arenaName, loadSpawnPoints(arenaName));
        }
    }

    public Set<String> getPlayers(String arenaName) {
        return arenaPlayers.getOrDefault(arenaName, Collections.emptySet());
    }

    public void addPlayer(String arenaName, String playerName) {
        arenaPlayers.computeIfAbsent(arenaName, k -> ConcurrentHashMap.newKeySet()).add(playerName);
        checkAndStartCountdown(arenaName);
    }

    public void removePlayer(String arenaName, String playerName) {
        Set<String> players = arenaPlayers.get(arenaName);
        if (players != null) {
            players.remove(playerName);
            if (players.isEmpty()) {
                arenaPlayers.remove(arenaName);
                cancelTask(arenaName);
            }
        }
    }

    private void cancelTask(String arenaName) {
        BukkitTask task = arenaTasks.remove(arenaName);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    public void checkAndStartCountdown(String arenaName) {
        Set<String> players = getPlayers(arenaName);

        if (players.size() >= PLAYERS_TO_START && !countdownStarted.contains(arenaName)) {
            countdownStarted.add(arenaName);
            startCountdown(arenaName, new ArrayList<>(players));
        }
    }

    public void startCountdown(String arenaName, List<String> playerNames) {
        Bukkit.broadcastMessage(ChatColor.YELLOW + "[Battle Royale] " + ChatColor.GREEN +
                "Le battleroyale commence dans " + ChatColor.YELLOW + COUNTDOWN_SECONDS + " secondes" + ChatColor.GREEN + " !");

        BukkitTask task = new BukkitRunnable() {
            int time = COUNTDOWN_SECONDS;

            @Override
            public void run() {
                if (playerNames.isEmpty()) {
                    cancel();
                    countdownStarted.remove(arenaName);
                    return;
                }

                // Announce time at significant intervals
                if (time == 60 || time == 30 || time == 10 || time <= 5) {
                    String message = ChatColor.GRAY + "Départ dans " + ChatColor.YELLOW + time + "s";
                    playerNames.stream()
                            .map(Bukkit::getPlayerExact)
                            .filter(Objects::nonNull)
                            .forEach(p -> p.sendMessage(message));
                }

                if (time <= 0) {
                    cancel();
                    arenaTasks.remove(arenaName);
                    teleportAndFreeze(arenaName, playerNames);
                }

                time--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        arenaTasks.put(arenaName, task);
    }

    private List<Location> loadSpawnPoints(String worldName) {
        List<Location> locs = new ArrayList<>();
        ConfigurationSection spawnPointsSection = plugin.getConfig().getConfigurationSection("spawnpoints");
        if (spawnPointsSection == null) return locs;

        List<String> raw = spawnPointsSection.getStringList(worldName);
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("[BattleRoyale] Le monde '" + worldName + "' n'est pas chargé.");
            return locs;
        }

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
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("[BattleRoyale] Coordonnées invalides pour '" + worldName + "': " + entry);
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

    public Map<String, Set<String>> getArenaPlayers() {
        return Collections.unmodifiableMap(arenaPlayers);
    }

    private void teleportAndFreeze(String arenaName, List<String> playerNames) {
        ConfigurationSection arenaSection = plugin.getConfig().getConfigurationSection("games." + arenaName);
        if (arenaSection == null) {
            plugin.getLogger().warning("[BattleRoyale] L'arène '" + arenaName + "' n'existe pas.");
            return;
        }

        List<String> worlds = arenaSection.getStringList("worlds");
        if (worlds.isEmpty()) {
            plugin.getLogger().warning("[BattleRoyale] Aucun monde défini pour l'arène '" + arenaName + "'.");
            return;
        }

        // Choisir un monde aléatoire
        String randomWorld = worlds.get(ThreadLocalRandom.current().nextInt(worlds.size()));
        List<Location> spawnPoints = loadSpawnPoints(randomWorld);
        if (spawnPoints.isEmpty()) {
            plugin.getLogger().warning("[BattleRoyale] Aucun point de spawn trouvé pour le monde '" + randomWorld + "'.");
            return;
        }

        // Mélanger les points de spawn
        Collections.shuffle(spawnPoints);

        // Téléporter les joueurs
        List<Player> onlinePlayers = playerNames.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .toList();

        for (int i = 0; i < onlinePlayers.size(); i++) {
            Player player = onlinePlayers.get(i);
            Location spawn = spawnPoints.get(i % spawnPoints.size());
            player.teleport(spawn);
            freezePlayer(player);
        }

        // Lancer le compte à rebours
        startCountdown(arenaName, playerNames);
    }

    private void unfreezeAndStart(String arenaName, List<Player> players) {
        // Save a list of player names for config - more efficient than repeatedly getting names
        List<String> playerNames = players.stream()
                .map(Player::getName)
                .collect(Collectors.toList());

        // Store in config for persistence
        plugin.getConfig().set("games." + arenaName + ".players", playerNames);
        plugin.saveConfig();

        for (Player player : players) {
            // Unfreeze player
            player.setWalkSpeed(0.2f);
            player.setFlySpeed(0.1f);
            player.setInvulnerable(false);
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealthScale(PLAYER_HEALTH_SCALE);

            // Break blocks around the player more efficiently
            Location loc = player.getLocation();
            World world = loc.getWorld();
            if (world != null) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        world.getBlockAt(loc.getBlockX() + dx, loc.getBlockY(), loc.getBlockZ() + dz).breakNaturally();
                    }
                }
            }

            player.sendMessage(ChatColor.GREEN + "Le battleroyale commence ! Bonne chance.");
        }

        // Start the game manager task
        BukkitTask task = new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                List<String> currentPlayers = plugin.getConfig().getStringList("games." + arenaName + ".players");

                // Game ended with a winner
                if (currentPlayers.size() == 1) {
                    Player winner = Bukkit.getPlayerExact(currentPlayers.get(0));
                    if (winner != null && winner.isOnline()) {
                        announceWinner(winner, arenaName);
                    }
                    cancel();
                    arenaTasks.remove(arenaName);

                    return;
                }
                // Game ended with no players
                else if (currentPlayers.isEmpty()) {
                    cancel();
                    arenaTasks.remove(arenaName);
                    resetArena(arenaName);
                    return;
                }

                // Update scoreboard for remaining players
                for (String name : currentPlayers) {
                    Player p = Bukkit.getPlayerExact(name);
                    if (p != null && p.isOnline()) {
                        ScoreboardManager.updateScoreboard(p, elapsed, currentPlayers.size(), 100);
                    }
                }

                elapsed++;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        arenaTasks.put(arenaName, task);

        Bukkit.broadcastMessage(ChatColor.GOLD + "[Battle Royale] " + ChatColor.WHITE +
                "Le battleroyale " + ChatColor.YELLOW + "commence ! " +
                ChatColor.WHITE + "Que le meilleur gagne !");

        // Add game to active arenas
        countdownStarted.remove(arenaName);
    }

    private void announceWinner(Player winner, String arenaName) {
        winner.sendTitle(ChatColor.GOLD + "Félicitations !", ChatColor.GREEN + "Tu as gagné la partie !", 10, 70, 20);
        winner.sendMessage(ChatColor.GREEN + "Tu as gagné la partie !");

        // Met à jour le scoreboard pour afficher la victoire
        ScoreboardManager.updateWinnerScoreboard(winner, arenaName);

        // Spawn des feux d'artifice
        spawnWinnerFireworks(winner);

        resetArena(arenaName);

    }

    private void spawnWinnerFireworks(Player winner) {
        Location loc = winner.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        for (int i = 0; i < 3; i++) {
            Firework fw = world.spawn(loc, Firework.class);
            FireworkMeta meta = fw.getFireworkMeta();

            meta.addEffect(FireworkEffect.builder()
                    .withColor(Color.LIME)
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .flicker(true)
                    .trail(true)
                    .build());

            meta.setPower(1);
            fw.setFireworkMeta(meta);

            // Use Paper's detonate() method if available, otherwise let it explode naturally
            try {
                fw.detonate();
            } catch (NoSuchMethodError e) {
                // Method not available, firework will explode naturally
            }
        }
    }

    private void resetArena(String arenaName) {
        plugin.getConfig().set("games." + arenaName + ".players", null);
        plugin.getConfig().set("games." + arenaName + ".enable", true);
        plugin.saveConfig();
    }

    // Method to clean up resources when plugin disables
    public void shutdown() {
        // Cancel all running tasks
        arenaTasks.values().forEach(task -> {
            if (!task.isCancelled()) {
                task.cancel();
            }
        });

        arenaTasks.clear();
        countdownStarted.clear();
        spawnPointCache.clear();
    }

    /**
     * Recharge les points d'apparition pour une arène spécifique
     *
     * @param arenaName Le nom de l'arène à recharger
     * @return Une liste des emplacements chargés
     */
    public List<Location> reloadSpawnPoints(String arenaName) {
        // Supprimer les points du cache existant
        spawnPointCache.remove(arenaName);

        // Charger à nouveau les points d'apparition
        List<Location> spawnPoints = loadSpawnPoints(arenaName);

        // Mettre à jour le cache
        spawnPointCache.put(arenaName, spawnPoints);

        plugin.getLogger().info("[BattleRoyale] " + spawnPoints.size() + " points d'apparition rechargés pour l'arène " + arenaName);

        return spawnPoints;
    }

    /**
     * Recharge tous les points d'apparition pour toutes les arènes
     */
    public void reloadAllSpawnPoints() {
        // Vider le cache existant
        spawnPointCache.clear();

        // Récupérer la section de configuration pour les jeux
        ConfigurationSection gamesSection = plugin.getConfig().getConfigurationSection("games");
        if (gamesSection == null) {
            plugin.getLogger().warning("[BattleRoyale] Aucune arène trouvée dans la configuration");
            return;
        }

        int totalPoints = 0;
        int totalArenas = 0;

        // Recharger les points d'apparition pour chaque arène
        for (String arenaName : gamesSection.getKeys(false)) {
            List<Location> points = reloadSpawnPoints(arenaName);
            if (!points.isEmpty()) {
                totalArenas++;
                totalPoints += points.size();
            }
        }

        plugin.getLogger().info("[BattleRoyale] Rechargement terminé: " + totalPoints +
                " points d'apparition dans " + totalArenas + " arènes");
    }

    public Optional<String> findAvailableArena(String arenaName) {
        ConfigurationSection gamesSection = plugin.getConfig().getConfigurationSection("games");
        if (gamesSection == null) {
            return Optional.empty();
        }

        // Si un nom d'arène est fourni, vérifiez sa disponibilité
        if (arenaName != null) {
            ConfigurationSection arenaSection = gamesSection.getConfigurationSection(arenaName);
            if (arenaSection != null && arenaSection.getBoolean("enable", false)) {
                return Optional.of(arenaName);
            }
        }

        // Sinon, recherchez la première arène disponible
        for (String arena : gamesSection.getKeys(false)) {
            ConfigurationSection arenaSection = gamesSection.getConfigurationSection(arena);
            if (arenaSection != null && arenaSection.getBoolean("enable", false)) {
                return Optional.of(arena);
            }
        }

        // Aucune arène disponible
        return Optional.empty();
    }

    public boolean removePlayerFromAllArenas(String playerName) {
        boolean removed = false;

        for (String arenaName : arenaPlayers.keySet()) {
            Set<String> players = arenaPlayers.get(arenaName);
            if (players != null && players.remove(playerName)) {
                removed = true;

                // Si l'arène n'a plus de joueurs, annulez les tâches associées
                if (players.isEmpty()) {
                    arenaPlayers.remove(arenaName);
                    cancelTask(arenaName);
                }
            }
        }

        return removed;
    }

    public boolean addSpawnPoint(String worldName, Location location) {
        ConfigurationSection spawnPointsSection = plugin.getConfig().getConfigurationSection("spawnpoints");
        if (spawnPointsSection == null) {
            spawnPointsSection = plugin.getConfig().createSection("spawnpoints");
        }

        String spawnPoint = location.getX() + "," + location.getY() + "," + location.getZ() + "," + location.getYaw() + "," + location.getPitch();
        List<String> spawnPoints = spawnPointsSection.getStringList(worldName);
        spawnPoints.add(spawnPoint);
        spawnPointsSection.set(worldName, spawnPoints);
        plugin.saveConfig();

        plugin.getLogger().info("[BattleRoyale] Point d'apparition ajouté pour le monde '" + worldName + "'.");
        return true;
    }

    public String getPlayerArena(String playerName) {
        for (Map.Entry<String, Set<String>> entry : arenaPlayers.entrySet()) {
            if (entry.getValue().contains(playerName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Set<String> getPlayersInArena(String arenaName) {
        return arenaPlayers.getOrDefault(arenaName, Collections.emptySet());
    }

    public boolean startGame(String arenaName) {
        Set<String> players = arenaPlayers.get(arenaName);
        if (players == null) {
            plugin.getLogger().warning("[BattleRoyale] Impossible de démarrer l'arène '" + arenaName + "'. Pas assez de joueurs.");
            return false;
        }

        List<String> playerNames = new ArrayList<>(players);
        teleportAndFreeze(arenaName, playerNames);
        plugin.getLogger().info("[BattleRoyale] La partie pour l'arène '" + arenaName + "' a commencé.");
        return true;
    }

    public List<String> getArenaNames() {
        ConfigurationSection gamesSection = plugin.getConfig().getConfigurationSection("games");
        if (gamesSection == null) {
            return Collections.emptyList();
        }

        return gamesSection.getKeys(false).stream()
                .filter(arenaName -> gamesSection.getConfigurationSection(arenaName).getBoolean("enable", false))
                .collect(Collectors.toList());
    }

    public Boolean isPlayerInGame(Player player) {
        for (Set<String> players : arenaPlayers.values()) {
            if (players.contains(player.getName())) {
                return true;
            }
        }
        return false;
    }
}