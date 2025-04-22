package fr.Mathilde.battleRoyaleRework.ManageWorld;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.io.File;
import java.util.logging.Level;

public class WorldManager {

    /**
     * Supprime un monde existant.
     *
     * @param worldName Nom du monde à supprimer.
     * @return true si le monde a été supprimé avec succès, sinon false.
     */
    public static boolean deleteWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            // Décharger le monde
            Bukkit.unloadWorld(world, false);
        }

        // Supprimer les fichiers du monde
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (worldFolder.exists()) {
            return deleteFolder(worldFolder);
        }
        return false;
    }

    private static boolean deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }
        return folder.delete();
    }

    /**
     * Clone un monde à partir d'un modèle.
     *
     * @param templateName Nom du monde modèle.
     * @param newWorldName Nom du nouveau monde.
     * @return true si le clonage a réussi, sinon false.
     */
    public static boolean cloneWorld(String templateName, String newWorldName) {
        File templateFolder = new File(Bukkit.getWorldContainer(), templateName);
        File newWorldFolder = new File(Bukkit.getWorldContainer(), newWorldName);

        if (!templateFolder.exists()) {
            Bukkit.getLogger().log(Level.WARNING, "[BattleRoyale] Le modèle '{0}' n'existe pas.", templateName);
            return false;
        }

        if (newWorldFolder.exists()) {
            Bukkit.getLogger().log(Level.WARNING, "[BattleRoyale] Le monde '{0}' existe déjà.", newWorldName);
            return false;
        }

        try {
            copyFolder(templateFolder, newWorldFolder);
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "[BattleRoyale] Erreur lors du clonage du monde.", e);
            return false;
        }
    }

    private static void copyFolder(File source, File destination) throws Exception {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }

            String[] children = source.list();
            if (children != null) {
                for (String file : children) {
                    copyFolder(new File(source, file), new File(destination, file));
                }
            }
        } else {
            java.nio.file.Files.copy(source.toPath(), destination.toPath());
        }
    }

    /**
     * Charge un monde dans le serveur.
     *
     * @param worldName Nom du monde à charger.
     * @return Le monde chargé ou null en cas d'échec.
     */
    public static World loadWorld(String worldName) {
        return Bukkit.createWorld(new WorldCreator(worldName).type(WorldType.NORMAL));
    }
}