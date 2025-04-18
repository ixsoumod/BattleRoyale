package fr.ixsou.battleroyale.commands;

import fr.ixsou.battleroyale.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public class joinCommand implements CommandExecutor {

    public joinCommand(Main main) {
        this.main = main;
    }

    private final Main main;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est réservée aux joueurs !");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§cErreur dans ta commande!");
            return true;
        }

        ConfigurationSection gamesSection = main.getConfig().getConfigurationSection("games");

        if (gamesSection == null) {
            player.sendMessage("§cAucun battleroyale configurée !");
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "join": {
                // Vérifier si le joueur est déjà dans une arène
                for (String arena : gamesSection.getKeys(false)) {
                    List<String> players = main.getConfig().getStringList("games." + arena + ".players");
                    if (players.contains(player.getName())) {
                        player.sendMessage("§cTu es déjà dans la queue pour le battleroyale");
                        return true;
                    }
                }

                // Parcourir les arènes pour trouver une avec moins de 100 joueurs
                for (String arena : gamesSection.getKeys(false)) {
                    String path = "games." + arena;
                    boolean enabled = main.getConfig().getBoolean(path + ".enable", true);
                    List<String> players = main.getConfig().getStringList(path + ".players");

                    if (enabled && players.size() < 100) {
                        players.add(player.getName());
                        main.getConfig().set(path + ".players", players);
                        main.saveConfig();

                        // Envoi d’un message à tous les joueurs de cette arène
                        String joinMessage = "§a" + player.getName() + " a rejoint le battleroyale ! §8(§e" + players.size() + "§8/§e100 §8joueurs)";

                        for (String playerName : players) {
                            Player target = Bukkit.getPlayerExact(playerName);
                            if (target != null && target.isOnline()) {
                                target.sendMessage(joinMessage);
                                player.sendMessage("§aTu peux quitter la queue avec §e/leave");
                            }
                        }

                        return true;
                    }
                }

                // Si aucune arène n'est dispo
                player.sendMessage("§cTouts les battleroyales sont pleins ou désactivés !");
                return true;
            }

            case "leave": {
                boolean found = false;

                for (String arena : gamesSection.getKeys(false)) {
                    String path = "games." + arena + ".players";
                    List<String> players = main.getConfig().getStringList(path);

                    if (players.contains(player.getName())) {
                        players.remove(player.getName());
                        main.getConfig().set(path, players);
                        main.saveConfig();
                        main.getGameManager().checkAndStartCountdown(arena);
                        found = true;

                        // Message à tous les joueurs restants
                        String leaveMessage = "§c" + player.getName() + " a quitté le battleroyale ! §8(§e\" + players.size() + \"§8/§e100 §8restants)";
                        for (String name : players) {
                            Player target = Bukkit.getPlayerExact(name);
                            if (target != null && target.isOnline()) {
                                target.sendMessage(leaveMessage);
                            }
                        }

                        // Message au joueur
                        player.sendMessage("§cTu as quitté le battleroyale");
                        break;
                    }
                }

                if (!found) {
                    player.sendMessage("§cTu n’es dans aucune queue !");
                }

                return true;
            }

            default:
                player.sendMessage("§cErreur dans ta commande!");
                return true;
        }
    }
}
