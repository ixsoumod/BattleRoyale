package fr.ixsou.battleroyale.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;

public class ScoreboardManager {

    private static final Map<String, Scoreboard> boards = new HashMap<>();
    private static final Map<String, Integer> gameTimers = new HashMap<>();

    public static void createScoreboard(Player player, int playersAlive, int maxPlayers) {
        ScoreboardManager.removeScoreboard(player); // Reset d'abord

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("br", "dummy", "§c§lBattleRoyale");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        obj.getScore("§7").setScore(6);
        obj.getScore("§fᴛᴇᴍᴘѕ: §e0s").setScore(5);
        obj.getScore(" ").setScore(4);
        obj.getScore("§fᴊᴏᴜᴇᴜʀ(ѕ) ʀᴇѕᴛᴀɴᴛ(ѕ): §a" + playersAlive + "/" + maxPlayers).setScore(3);
        obj.getScore("  ").setScore(2);
        obj.getScore("§foteria.fr").setScore(1);

        player.setScoreboard(board);
        boards.put(player.getName(), board);
        gameTimers.put(player.getName(), 0);
    }

    public static void updateScoreboard(Player player, int secondsElapsed, int playersAlive, int maxPlayers) {
        Scoreboard board = boards.get(player.getName());
        if (board == null) return;

        Objective obj = board.getObjective(DisplaySlot.SIDEBAR);
        if (obj == null) return;

        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        obj.getScore("§7").setScore(6);
        obj.getScore("§fᴛᴇᴍᴘѕ: §e" + secondsElapsed + "s").setScore(5);
        obj.getScore(" ").setScore(4);
        obj.getScore("§fᴊᴏᴜᴇᴜʀ(ѕ) ʀᴇѕᴛᴀɴᴛ(ѕ): §a" + playersAlive + "/" + maxPlayers).setScore(3);
        obj.getScore("  ").setScore(2);
        obj.getScore("§foteria.fr").setScore(1);
    }

    public static void removeScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        boards.remove(player.getName());
        gameTimers.remove(player.getName());
    }
}
