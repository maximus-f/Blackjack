package me.perotin.blackjack.objects;

import me.perotin.blackjack.Blackjack;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;

import static me.perotin.blackjack.objects.BlackFile.BlackFilesType.STATS;

public class BlackFile {

    private File file;
    private FileConfiguration configuration;
    private BlackFilesType type;

    public BlackFile(BlackFilesType type) {
        // hard-coding some stuff to make our lives easier, since we'll be using
        // files A LOT so it is important we have a good system set in place to easily retreive these things

        if (type == STATS) {
            file = new File(Blackjack.getInstance().getDataFolder(), "stats.yml");
            configuration = YamlConfiguration.loadConfiguration(file);
            this.type = STATS;
        }


    }

    public void save() {
        try {
            configuration.save(file);
        } catch (IOException ex) {
            ex.printStackTrace();

        }
    }

    // some generic methods to speed up the process

    public FileConfiguration getConfiguration() {
        return configuration;
    }

    public Object get(String path) {
        return configuration.get(path);
    }

    public void set(String path, Object value) {
        configuration.set(path, value);
    }

    public String getString(String path) {
        return ChatColor.translateAlternateColorCodes('&', configuration.getString(path));
    }

    public void load() {

        File lang = null;
        InputStream defLangStream = null;

        switch (type) {
            case STATS:
                lang = new File(Blackjack.getInstance().getDataFolder(), "stats.yml");
                defLangStream = Blackjack.getInstance().getResource("stats.yml");
                break;

        }
        OutputStream out = null;
        if (!lang.exists()) {
            try {
                Blackjack.getInstance().getDataFolder().mkdir();
                lang.createNewFile();
                if (defLangStream != null) {
                    out = new FileOutputStream(lang);
                    int read;
                    byte[] bytes = new byte[1024];

                    while ((read = defLangStream.read(bytes)) != -1) {
                        out.write(bytes, 0, read);
                    }
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace(); // So they notice
                Bukkit.getLogger().severe("[Blackjack] Couldn't create " + type.toString().toLowerCase() + " file.");
                Bukkit.getLogger().severe("[Blackjack] This is a fatal error. Now disabling");
                Blackjack.getInstance().getPluginLoader().disablePlugin(Blackjack.getInstance()); // Without
                // it
                // loaded,
                // we
                // can't
                // send
                // them
                // messages
            } finally {
                if (defLangStream != null) {
                    try {
                        defLangStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    public File getFile(){
        return this.file;
    }
    public static void loadFiles(){
        if(!new BlackFile(STATS).getFile().exists()) {
            Blackjack.getInstance().saveResource("stats.yml", false);
        }


        for(BlackFilesType type : BlackFilesType.values()){
            new BlackFile(type).load();
        }
    }


    public enum BlackFilesType {

        STATS

    }

}
