package fr.ixsou.battleroyale;

import fr.ixsou.battleroyale.commands.ForceStartCommand;
import fr.ixsou.battleroyale.commands.ReloadCommand;
import fr.ixsou.battleroyale.commands.SetSpawnPointCommand;
import fr.ixsou.battleroyale.commands.joinCommand;
import fr.ixsou.battleroyale.game.GameManager;
import fr.ixsou.battleroyale.listeners.GameListener;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public final class Main extends JavaPlugin {

    private GameManager gameManager;

    @Override
    public void onEnable() {
        this.gameManager = new GameManager(this);
        getCommand("battleroyale").setExecutor(new joinCommand(this)); // déjà présent
        getCommand("battleroyaleforcestart").setExecutor(new ForceStartCommand(this));
        getCommand("battleroyalereload").setExecutor(new ReloadCommand(this));
        getCommand("setspawnpoint").setExecutor(new SetSpawnPointCommand(this));

        getServer().getPluginManager().registerEvents(new GameListener(this), this);

        saveDefaultConfig();
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    @Override
    public void onDisable() {
        saveDefaultConfig();
    }



}
