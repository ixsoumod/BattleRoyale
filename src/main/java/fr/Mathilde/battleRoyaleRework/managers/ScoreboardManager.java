package fr.Mathilde.battleRoyaleRework.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player scoreboards for the Battle Royale game
 */
public class ScoreboardManager {

    // Use UUIDs instead of names for better player tracking
    private static final Map<UUID, Scoreboard> playerBoards = new ConcurrentHashMap<>();

    // Reusable strings to avoid creating new objects
    private static final String TITLE = ChatColor.RED + "" + ChatColor.BOLD + "BattleRoyale";
    private static final String FOOTER = ChatColor.WHITE + "oteria.fr";
    private static final String TIME_PREFIX = ChatColor.WHITE + "ᴛᴇᴍᴘѕ: " + ChatColor.YELLOW;
    private static final String PLAYERS_PREFIX = ChatColor.WHITE + "ᴊᴏᴜᴇᴜʀ(ѕ) ʀᴇѕᴛᴀɴᴛ(ѕ): " + ChatColor.GREEN;

    // Score positions
    private static final int[] SCORES = {6, 5, 4, 3, 2, 1};

    /**
     * Creates a new scoreboard for a player
     *
     * @param player The player to create a scoreboard for
     * @param playersAlive Current number of players alive
     * @param maxPlayers Maximum number of players in the game
     */
    public static void createScoreboard(Player player, int playersAlive, int maxPlayers) {
        // Safety check
        if (player == null) return;

        // Remove existing scoreboard first
        removeScoreboard(player);

        // Get the scoreboard manager efficiently
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        // Create a new scoreboard
        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("br", Criteria.DUMMY, TITLE);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Set initial scores
        updateScoreboardEntries(objective, 0, playersAlive, maxPlayers);

        // Apply the scoreboard
        player.setScoreboard(board);
        playerBoards.put(player.getUniqueId(), board);
    }

    /**
     * Updates an existing player scoreboard
     *
     * @param player The player whose scoreboard to update
     * @param secondsElapsed Time elapsed in seconds
     * @param playersAlive Current number of players alive
     * @param maxPlayers Maximum number of players in the game
     */
    public static void updateScoreboard(Player player, int secondsElapsed, int playersAlive, int maxPlayers) {
        // Safety checks
        if (player == null) return;

        // Get the player's scoreboard
        Scoreboard board = playerBoards.get(player.getUniqueId());
        if (board == null) {
            // Create a new scoreboard if none exists
            createScoreboard(player, playersAlive, maxPlayers);
            return;
        }

        // Get the objective
        Objective objective = board.getObjective(DisplaySlot.SIDEBAR);
        if (objective == null) {
            // Create a new objective if none exists
            objective = board.registerNewObjective("br", Criteria.DUMMY, TITLE);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            // Reset existing scores
            for (String entry : board.getEntries()) {
                board.resetScores(entry);
            }
        }

        // Update the scoreboard entries
        updateScoreboardEntries(objective, secondsElapsed, playersAlive, maxPlayers);
    }

    /**
     * Sets the scoreboard entries for an objective
     *
     * @param objective The objective to update
     * @param secondsElapsed Time elapsed in seconds
     * @param playersAlive Current number of players alive
     * @param maxPlayers Maximum number of players in the game
     */
    private static void updateScoreboardEntries(Objective objective, int secondsElapsed, int playersAlive, int maxPlayers) {
        // Format time and players strings once
        String timeText = TIME_PREFIX + secondsElapsed + "s";
        String playersText = PLAYERS_PREFIX + playersAlive + "/" + maxPlayers;

        // Set the scores efficiently
        objective.getScore(ChatColor.GRAY + "").setScore(SCORES[0]);
        objective.getScore(timeText).setScore(SCORES[1]);
        objective.getScore(" ").setScore(SCORES[2]);
        objective.getScore(playersText).setScore(SCORES[3]);
        objective.getScore("  ").setScore(SCORES[4]);
        objective.getScore(FOOTER).setScore(SCORES[5]);
    }

    /**
     * Removes a player's scoreboard
     *
     * @param player The player whose scoreboard to remove
     */
    public static void removeScoreboard(Player player) {
        // Safety check
        if (player == null) return;

        // Get the scoreboard manager
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            // Set the player's scoreboard to a blank one
            player.setScoreboard(manager.getNewScoreboard());
        }

        // Remove the player from our map
        playerBoards.remove(player.getUniqueId());
    }

    /**
     * Cleans up resources when the plugin is disabled
     */
    public static void cleanup() {
        playerBoards.clear();
    }

    /**
     * Checks if a player has a scoreboard
     *
     * @param player The player to check
     * @return true if the player has a scoreboard, false otherwise
     */
    public static boolean hasScoreboard(Player player) {
        return player != null && playerBoards.containsKey(player.getUniqueId());
    }

    public static void updateWinnerScoreboard(Player winner, String arenaName) {
        // Vérifie si le joueur est valide
        if (winner == null) return;

        // Crée un nouveau scoreboard pour le gagnant
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("victory", Criteria.DUMMY, ChatColor.GOLD + "Victoire !");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Ajoute des informations au scoreboard
        objective.getScore(ChatColor.GREEN + "Arène: " + ChatColor.YELLOW + arenaName).setScore(2);
        objective.getScore(ChatColor.GREEN + "Statut: " + ChatColor.YELLOW + "Gagnant").setScore(1);

        // Applique le scoreboard au joueur
        winner.setScoreboard(board);

        // Envoie un message de confirmation
        winner.sendMessage(ChatColor.GOLD + "[BattleRoyale] Scoreboard mis à jour pour la victoire !");
    }
}