package fr.Mathilde.battleRoyaleRework.commands;

import fr.Mathilde.battleRoyaleRework.BattleRoyaleRework;
import fr.Mathilde.battleRoyaleRework.managers.GameManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class CommandsHandler implements CommandExecutor, TabCompleter {

    private final BattleRoyaleRework plugin;
    private final GameManager gameManager;
    private static final List<String> SUB_COMMANDS = Arrays.asList("join", "leave", "setspawnpoint", "reload", "forcestart", "help");

    public CommandsHandler(BattleRoyaleRework plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "join" -> handleJoinCommand(sender, subArgs);
            case "leave" -> handleLeaveCommand(sender);
            case "setspawnpoint" -> handleSetSpawnPointCommand(sender, subArgs);
            case "reload" -> handleReloadCommand(sender);
            case "forcestart" -> handleForceStartCommand(sender, subArgs);
            case "help" -> sendHelp(sender);
            default -> {
                sender.sendMessage("§cCommande inconnue: §4" + subCommand);
                sendHelp(sender);
            }
        }
        return true;
    }

    private void handleJoinCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande est réservée aux joueurs !");
            return;
        }

        if (gameManager.getPlayerArena(player.getUniqueId().toString()) != null) {
            player.sendMessage("§cTu es déjà dans une arène !");
            return;
        }

        Optional<String> arena = gameManager.findAvailableArena(args.length > 0 ? args[0] : null);
        if (arena.isPresent()) {
            String arenaName = arena.get();
            gameManager.addPlayer(arenaName, player.getUniqueId().toString());
            player.sendMessage("§aTu as rejoint l'arène §e" + arenaName);

            // Récupère le nombre de joueurs actuels et le nombre requis pour démarrer
            Set<String> playersInArena = gameManager.getPlayersInArena(arenaName);
            int currentPlayers = playersInArena.size();
            int playersToStart = gameManager.PLAYERS_TO_START;

            // Broadcast aux joueurs déjà dans l'arène
            for (String playerName : playersInArena) {
                Player arenaPlayer = plugin.getServer().getPlayer(playerName);
                if (arenaPlayer != null && !arenaPlayer.getUniqueId().toString().equals(player.getUniqueId().toString())) {
                    arenaPlayer.sendMessage("§e" + player.getName() + " a rejoint l'arène ! (" + currentPlayers + "/" + playersToStart + ")");
                }
            }

            // Message au joueur qui rejoint
            if (currentPlayers >= playersToStart) {
                player.sendMessage("§aLe nombre de joueurs requis pour démarrer la partie est atteint !");
            } else {
                player.sendMessage("§eEn attente de joueurs... (" + currentPlayers + "/" + playersToStart + ")");
            }
        } else {
            player.sendMessage("§cAucune arène disponible !");
        }
    }


    private void handleLeaveCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande est réservée aux joueurs !");
            return;
        }

        if (gameManager.removePlayerFromAllArenas(player.getUniqueId().toString())) {
            player.sendMessage("§cTu as quitté l'arène.");
        } else {
            player.sendMessage("§cTu n'es dans aucune arène !");
        }
    }

    private void handleSetSpawnPointCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande uniquement pour les joueurs.");
            return;
        }

        if (!player.hasPermission("battleroyale.setspawnpoint")) {
            player.sendMessage("§cTu n'as pas la permission.");
            return;
        }

        if (args.length != 1) {
            player.sendMessage("§cUsage : /br setspawnpoint <world>");
            return;
        }

        String worldName = args[0];
        Location loc = player.getLocation();
        if (gameManager.addSpawnPoint(worldName, loc)) {
            player.sendMessage("§aSpawnpoint ajouté pour le monde §e" + worldName);
        } else {
            player.sendMessage("§cImpossible d'ajouter le spawnpoint !");
        }
    }

    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("battleroyale.reload")) {
            sender.sendMessage("§cTu n'as pas la permission.");
            return;
        }

        plugin.reloadConfig();
        gameManager.reloadAllSpawnPoints();
        sender.sendMessage("§aLa configuration a été rechargée avec succès !");
    }

    private void handleForceStartCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande réservée aux joueurs.");
            return;
        }

        if (!player.hasPermission("battleroyale.forcestart")) {
            player.sendMessage("§cTu n'as pas la permission d'utiliser cette commande.");
            return;
        }

        String arenaName = args.length > 0 ? args[0] : gameManager.getPlayerArena(player.getUniqueId().toString());
        if (arenaName == null) {
            player.sendMessage("§cAucune arène spécifiée ou trouvée !");
            return;
        }

        if (gameManager.startGame(arenaName)) {
            player.sendMessage("§aTu as forcé le lancement de l'arène §e" + arenaName);
        } else {
            player.sendMessage("§cImpossible de démarrer l'arène §e" + arenaName + "§c. Vérifie qu'elle est activée et qu'il y a des joueurs.");
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== BattleRoyale - Aide ===");
        sender.sendMessage("§e/br join [arena] §7- Rejoindre une arène");
        sender.sendMessage("§e/br leave §7- Quitter l'arène actuelle");

        if (sender.hasPermission("battleroyale.setspawnpoint")) {
            sender.sendMessage("§e/br setspawnpoint <arena> §7- Définir un point d'apparition");
        }

        if (sender.hasPermission("battleroyale.forcestart")) {
            sender.sendMessage("§e/br forcestart [arena] §7- Forcer le démarrage d'une arène");
        }

        if (sender.hasPermission("battleroyale.reload")) {
            sender.sendMessage("§e/br reload §7- Recharger la configuration");
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filterCompletions(SUB_COMMANDS, args[0]);
        } else if (args.length == 2 && List.of("join", "setspawnpoint", "forcestart").contains(args[0].toLowerCase())) {
            return filterCompletions(gameManager.getArenaNames(), args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filterCompletions(List<String> completions, String partial) {
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
    }
}