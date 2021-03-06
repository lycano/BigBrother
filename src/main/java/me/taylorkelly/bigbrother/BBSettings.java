package me.taylorkelly.bigbrother;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import me.taylorkelly.bigbrother.datablock.explosions.TNTLogger;
import me.taylorkelly.bigbrother.datasource.BBDB;
import me.taylorkelly.bigbrother.datasource.BBDB.DBFailCallback;

import me.taylorkelly.util.TimeParser;

import org.bukkit.Server;
import com.sk89q.worldedit.blocks.ItemType;

// TODO: Split all these vars into separate classes in anticipation of yamlification.
public class BBSettings {

    public static boolean blockBreak;
    public static boolean blockPlace;
    public static boolean teleport;
    public static boolean chestChanges;
    public static boolean commands;
    public static boolean chat;
    public static boolean disconnect;
    public static boolean login;
    public static boolean doorOpen;
    public static boolean buttonPress;
    public static boolean leverSwitch;
    public static boolean leafDrops;
    public static boolean fire;
    public static boolean tntExplosions;
    public static boolean creeperExplosions;
    public static boolean miscExplosions;
    public static boolean ipPlayer;
    public static boolean lavaFlow;
    public static boolean pickupItem;
    public static boolean dropItem;


    public static boolean libraryAutoDownload;
    public static boolean debugMode;
    public static boolean restoreFire = false;
    public static boolean autoWatch = true;
    public static boolean flatLog = false;
    public static int defaultSearchRadius = 2;
    public static int sendDelay = 4;
    public static int stickItem = 280;
    // TODO: Get long version of this
    public static long cleanseAge = TimeParser.parseInterval("3d");
    // Tested with this value, 10000rows = 1-2s on a
    // Pentium 4 MySQL server with 1GB RAM and a SATA MySQL HDD
    public static long deletesPerCleansing = 20000L;
    private static ArrayList<String> watchList;
    private static ArrayList<String> seenList;
    private static ArrayList<Integer> blockExclusionList;
    public static int rollbacksPerTick;
    private static BigBrother plugin;
    public static File dataFolder;

    public static void initialize(BigBrother plg, File dataFolder) {
        BBSettings.dataFolder=dataFolder;
        BBSettings.plugin=plg;
        watchList = new ArrayList<String>();
        seenList = new ArrayList<String>();
        blockExclusionList = new ArrayList<Integer>();

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        final File yml = new File(dataFolder, "BigBrother.yml");
        BBLogging.debug("Path to BigBrother.yml: " + yml.getPath());
        loadLists(dataFolder);
        loadYaml(yml);
        BBLogging.debug("Loaded Settings");

    }

    private static void loadYaml(File yamlfile) {
        final BetterConfig yml = new BetterConfig(yamlfile);
        
        // If the file's not there, don't load it
        if(yamlfile.exists())
            yml.load();
        
        // Import old settings into new config defaults and remove the old versions.
        if(yml.getProperty("database.mysql.username")!=null) {
            BBDB.username = yml.getString("database.mysql.username", BBDB.username);
            yml.removeProperty("database.mysql.username");
            BBDB.password= yml.getString("database.mysql.password", BBDB.password);
            yml.removeProperty("database.mysql.password");
            BBDB.hostname = yml.getString("database.mysql.hostname", BBDB.hostname);
            yml.removeProperty("database.mysql.hostname");
            BBDB.schema = yml.getString("database.mysql.database", BBDB.schema);
            yml.removeProperty("database.mysql.database");
            BBDB.port = yml.getInt("database.mysql.port", BBDB.port);
            yml.removeProperty("database.mysql.port");
            BBDB.prefix = yml.getString("database.mysql.prefix", BBDB.prefix);
            yml.removeProperty("database.mysql.prefix"); 
        }
        
        BBDB.init(yml,new DBFailCallback() {
            public void disableMe() {
                plugin.getServer().getPluginManager().disablePlugin(plugin);
            }
        });
        loadWatchSettings(yml);
        
        List<Object> excluded = yml.getList("general.excluded-blocks");
        // Dodge NPE reported by Mineral (and set a default)
        if(excluded==null) {
            yml.setProperty("general.excluded-blocks", blockExclusionList);
        } else {
            for (Object o : excluded) {
                int id = 0;
                if(o instanceof Integer)
                    id = (int)(Integer)o;
                else if(o instanceof String) {
                    id = ItemType.lookup((String)o).getID();
                }
                blockExclusionList.add(id);
            }
        }
        stickItem = yml.getInt("general.stick-item", 280);// "The item used for /bb stick");
        restoreFire = yml.getBoolean("general.restore-fire", false);// "Restore fire when rolling back");
        autoWatch = yml.getBoolean("general.auto-watch", true);// "Automatically start watching players");
        defaultSearchRadius = yml.getInt("general.default-search-radius", 5);// "Default search radius for bbhere and bbfind");
        flatLog = yml.getBoolean("general.personal-log-files", false);// "If true, will also log actions to .logs (one for each player)");
        rollbacksPerTick = yml.getInt("general.rollbacks-per-tick", 2000);// "If true, will also log actions to .logs (one for each player)");
        debugMode = yml.getBoolean("general.debug-mode", false);// "If true, will also log actions to .logs (one for each player)");
        libraryAutoDownload = yml.getBoolean("general.library-autodownload", true);// "If true, will also log actions to .logs (one for each player)");
        TNTLogger.THRESHOLD = yml.getDouble("general.tnt-threshold", 10.0);// "If true, will also log actions to .logs (one for each player)");
        yml.save();
    }

    private static void loadWatchSettings(BetterConfig watched) {
        blockBreak = watched.getBoolean("watched.blocks.block-break", true);// "Watch when players break blocks");
        blockPlace = watched.getBoolean("watched.blocks.block-place", true);// "Watch when players place blocks");
        teleport = watched.getBoolean("watched.player.teleport", true);// "Watch when players teleport around");
        chestChanges = watched.getBoolean("watched.blocks.chest-changes", true);// "Watch when players add/remove items from chests");
        commands = watched.getBoolean("watched.chat.commands", true);// "Watch for all player commands");
        chat = watched.getBoolean("watched.chat.chat", true);// "Watch for player chat");
        login = watched.getBoolean("watched.player.login", true);// "Watch for player logins");
        disconnect = watched.getBoolean("watched.player.disconnect", true);// "Watch for player disconnects");
        doorOpen = watched.getBoolean("watched.misc.door-open", false);// "Watch for when player opens doors");
        buttonPress = watched.getBoolean("watched.misc.button-press", false);// "Watch for when player pushes buttons");
        leverSwitch = watched.getBoolean("watched.misc.lever-switch", false);// "Watch for when player switches levers");
        fire = watched.getBoolean("watched.misc.flint-logging", true);// "Watch for when players start fires");
        leafDrops = watched.getBoolean("watched.environment.leaf-decay", false);// "Watch for when leaves drop");
        tntExplosions = watched.getBoolean("watched.explosions.tnt", true);// "Watch for when TNT explodes");
        creeperExplosions = watched.getBoolean("watched.explosions.creeper", true);// "Watch for when Creepers explodes");
        miscExplosions = watched.getBoolean("watched.explosions.misc", true);// "Watch for miscellaneous explosions");
        ipPlayer = watched.getBoolean("watched.player.ip-player", true);
        dropItem = watched.getBoolean("watched.player.drop-item", false);
        pickupItem = watched.getBoolean("watched.player.pickup-item", false);
        lavaFlow = watched.getBoolean("watched.environment.lava-flow", false);
    }

    /**
     * @todo Move to SQL tables.
     * @param dataFolder
     */
    private static void loadLists(File dataFolder) {
        File file = new File(dataFolder, "WatchedPlayers.txt");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            final Scanner sc = new Scanner(file);
            while (sc.hasNextLine()) {
                final String player = sc.nextLine();
                if (player.equals("")) {
                    continue;
                }
                if (player.contains(" ")) {
                    continue;
                }
                watchList.add(player);
            }
        } catch (final FileNotFoundException e) {
            BBLogging.severe("Cannot read file " + file.getName());
        } catch (final IOException e) {
            BBLogging.severe("IO Exception with file " + file.getName());
        }

        file = new File(dataFolder, "SeenPlayers.txt");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            final Scanner sc = new Scanner(file);
            while (sc.hasNextLine()) {
                final String player = sc.nextLine();
                if (player.equals("")) {
                    continue;
                }
                if (player.contains(" ")) {
                    continue;
                }
                seenList.add(player);
            }
        } catch (final FileNotFoundException e) {
            BBLogging.severe("Cannot read file " + file.getName());
        } catch (final IOException e) {
            BBLogging.severe("IO Exception with file " + file.getName());
        }

    }

    public static Watcher getWatcher(Server server, File dataFolder) {
        return new Watcher(server);
    }

    public enum DBMS {
        H2, 
        MYSQL,
        POSTGRES,
    }

    /**
     * Replace placeholder with the table prefix.
     * @param sql
     * @param placeholder
     * @return
     */
    public static String replaceWithPrefix(String sql, String placeholder) {
        return sql.replace(placeholder, BBDB.prefix);
    }
    
    /**
     * Check if a blocktype is being ignored.
     * @param type
     * @return
     */
    public static boolean isBlockIgnored(int type) {
        return blockExclusionList.contains(type);
    }
}
