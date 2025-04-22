package fr.Mathilde.battleRoyaleRework;

import fr.Mathilde.battleRoyaleRework.commands.CommandsHandler;
import fr.Mathilde.battleRoyaleRework.listeners.GameEvents;
import fr.Mathilde.battleRoyaleRework.managers.GameManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class BattleRoyaleRework extends JavaPlugin {

    private GameManager gameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.gameManager = new GameManager(this);

        CommandsHandler commandsHandler = new CommandsHandler(this);
        getCommand("battleroyale").setExecutor(commandsHandler);
        getCommand("battleroyale").setTabCompleter(commandsHandler);

        getServer().getPluginManager().registerEvents(new GameEvents(this), this);

    }

    public GameManager getGameManager() {
        return gameManager;
    }

    @Override
    public void onDisable() {
        gameManager.shutdown();
    }
}
