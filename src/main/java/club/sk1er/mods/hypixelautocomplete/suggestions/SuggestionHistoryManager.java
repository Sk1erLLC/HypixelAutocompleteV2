package club.sk1er.mods.hypixelautocomplete.suggestions;

import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

public class SuggestionHistoryManager {


    private final Path configFile = MinecraftClient.getInstance().runDirectory.toPath().resolve("config/hypixel-autocomplete/history.json");
    private final Logger logger = LogManager.getLogger("HypixelAutoComplete");

    public SuggestionHistoryManager() {
        // Add shutdown hook to save config on exit
    }

    /**
     * Fired by (@MixinChatScreen) when a message is sent.
     * In this function, we will need to extract the usernames from the command
     * if that command is one of the tab complete commands and then store them in a history list.
     * This history list must persist between game sessions, so we will need to save it to a file.
     * As a fun tangent we should also store the amount of times a given username has been sent
     * so we can add a little command to see who your best friends are.
     */
    public void capture(String message) {
        logger.info("Captured message: {}", message);
    }


    public void loadConfig() {

    }

    public void saveConfig() {

        // Save current state to config. First, write all data to a temp file, then move it over the original file.
    }

    // Connect this to SuggestionManager to get suggestions
    public Set<String> getSuggestions() {
        return Collections.emptySet();
    }

}
