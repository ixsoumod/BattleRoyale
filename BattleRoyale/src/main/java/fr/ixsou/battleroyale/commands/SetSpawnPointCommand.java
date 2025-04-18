package fr.ixsou.battleroyale.commands;

import fr.ixsou.battleroyale.Main;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SetSpawnPointCommand implements CommandExecutor {

    private final Main main;

    public SetSpawnPointCommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cCommande uniquement pour les joueurs.");
            return true;
        }

        if (!p.hasPermission("battleroyale.setspawnpoint")) {
            p.sendMessage("§cTu n'as pas la permission.");
            return true;
        }

        if (args.length != 1) {
            p.sendMessage("§cUsage : /setspawnpoint <arena>");
            return true;
        }

        String arenaName = args[0];
        Location loc = p.getLocation();
        String data = loc.getX() + "," + loc.getY() + "," + loc.getZ();

        List<String> spawns = main.getConfig().getStringList("games." + arenaName + ".spawnpoint");
        spawns.add(data);
        main.getConfig().set("games." + arenaName + ".spawnpoint", spawns);
        main.saveConfig();

        p.sendMessage("§aSpawnpoint ajouté pour l’arène §e" + arenaName);
        return true;
    }
}
