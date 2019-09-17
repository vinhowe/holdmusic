package org.smplite.velocityqueue;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Config {
    public String target = "smp";
    public String queue = "lobby";
    public String message = "&3Position in queue: &L%position%";

    /**
     * Loads a config file, and if it doesn't exist creates one
     * @param filepath filepath of the config
     */
    static Config getConfig(String filepath)
    {
        Gson gson = new Gson();

        try {
            FileReader fr = new FileReader(filepath);
            return gson.fromJson(fr, Config.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            genConfig(filepath);
        }

        return new Config();
    }

    /**
     * Create a new config
     * @param filepath filepath of the config
     */
    static void genConfig(String filepath)
    {
        File config = new File(filepath);
        try {
            config.createNewFile();
            FileWriter fw = new FileWriter(config);
            fw.write("{\"target\": \"smp\", \"queue\": \"lobby\", \"message\": \"&3Position in queue: &L%position%\"}");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
}
