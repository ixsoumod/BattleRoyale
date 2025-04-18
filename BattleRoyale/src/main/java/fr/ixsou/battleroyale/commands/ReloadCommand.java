package fr.ixsou.battleroyale.commands;

import fr.ixsou.battleroyale.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final Main main;

    public ReloadCommand(Main main) {
        this.main = main;
    }

    // Command execution logic
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        main.reloadConfig();
        main.saveConfig();
        sender.sendMessage("§aLa configuration a été rechargée avec succès !");
        return true;
    }
}
