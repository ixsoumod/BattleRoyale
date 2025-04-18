package fr.ixsou.battleroyale.commands;

import fr.ixsou.battleroyale.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public class ForceStartCommand implements CommandExecutor {

    private final Main main;

    public ForceStartCommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cCommande réservée aux joueurs.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("group.admin")) {
            player.sendMessage("§cTu n’as pas la permission d’utiliser cette commande.");
            return true;
        }

        ConfigurationSection gamesSection = main.getConfig().getConfigurationSection("games");

        if (gamesSection == null) {
            player.sendMessage("§cAucune arène configurée.");
            return true;
        }

        for (String arena : gamesSection.getKeys(false)) {
            List<String> players = main.getConfig().getStringList("games." + arena + ".players");

            if (players.contains(player.getName())) {
                // Désactiver l’arène dans la config
                main.getConfig().set("games." + arena + ".enable", false);
                main.saveConfig();

                // Démarrer la partie directement
                main.getGameManager().startCountdown(arena, players);
                player.sendMessage("§aTu as forcé le lancement de l’arène §e" + arena);
                return true;
            }
        }

        player.sendMessage("§cTu n’es inscrit dans aucune arène !");
        return true;
    }
}
