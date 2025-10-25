package club.sk1er.mods.hypixelautocomplete.suggestions;

import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class SuggestionHistoryManager {
    private final Path configFile = MinecraftClient.getInstance().runDirectory.toPath().resolve("config/hypixel-autocomplete/history.json");
    private final Logger logger = LogManager.getLogger("HypixelAutoComplete");

    // Usernames player has interacted with, saved with the num interactions
    private static HashMap<String, Integer> suggestions = new HashMap<>();

    private Path configPath;
    private Path tempHistoryFilePath;
    private Path historyFilePath;

    public SuggestionHistoryManager() {
        configPath = MinecraftClient.getInstance().runDirectory.toPath().resolve("config");
        tempHistoryFilePath = configPath.resolve("suggestionHistoryTemp.txt");
        historyFilePath = configPath.resolve("suggestionHistory.txt");

        loadConfig();

        // Shutdown hook to save config on exit
        Thread printingHook = new Thread(() -> saveConfig());
        Runtime.getRuntime().addShutdownHook(printingHook);
    }

    /**
     * Fired by (@MixinChatScreen) when a message is sent.
     * In this function, we will need to extract the usernames from the command
     * if that command is one of the tab complete commands and then store them in a history list.
     * This history list must persist between game sessions, so we will need to save it to a file.
     * As a fun tangent we should also store the amount of times a given username has been sent
     * so we can add a little command to see who your best friends are.
     * purging users who have only been invited once?
     */
    public void capture(String message) {
        if (!SuggestionService.isHypixel()) return;

        logger.info("Captured message: {}", message);

        String[] split = message.split(" ");

        if (split.length < 2) {
            logger.error("Invalid message: {}", message);
            return;
        }

        if (SuggestionService.getPartyCommands().contains(split[0] + " ")) {
            // if command is a non-username party command (leave, disband, etc)
            if (SuggestionService.getPartyCommandsExcludeNoUser().contains(split[1])) {
                return;
            }

            // if command has two non-user args (/p invite, /p transfer, etc)
            int startIndex = SuggestionService.getPartyCommandsUser().contains(split[1]) ? 2 : 1;
            for (int i = startIndex; i < split.length; i++) {
                // the number of times the user has been partied or messaged
                int occurrences = suggestions.containsKey(split[i]) ? suggestions.get(split[i]) + 1 : 1;

                suggestions.put(split[i], occurrences);
            }
        } else if (SuggestionService.getWhisperCommands().contains(split[0])) {
            // single-username command
            int occurrences = suggestions.containsKey(split[1]) ? suggestions.get(split[1]) + 1 : 1;

            suggestions.put(split[1], occurrences);
        }
    }


    public void loadConfig() {
        File historyFile = new File (historyFilePath.toString());

        try {
            if (!historyFile.exists()) {
                return;
            }

            Scanner scanner = new Scanner(historyFile);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] split = line.split(" ");

                if (split.length < 2) {
                    logger.error("Invalid data in config file");
                    return;
                }

                suggestions.put(split[0], Integer.parseInt(split[1]));
            }

            scanner.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public void saveConfig() {
        // Save current state to config. First, write all data to a temp file, then move it over the original file.
        File tempFile = new File(tempHistoryFilePath.toString());
        File parentDirectory = tempFile.getParentFile();

        if (parentDirectory != null && !parentDirectory.exists()) {
            parentDirectory.mkdirs(); // Creates the directory and any necessary parent directories
        }

        try {
            FileWriter writer = new FileWriter(tempFile);

            for (Map.Entry<String, Integer> entry : suggestions.entrySet()) {
                writer.write(entry.getKey() + " " + entry.getValue() + "\n");
            }

            writer.close();

            try {
                Files.move(tempHistoryFilePath, historyFilePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (FileSystemException e) {
                e.printStackTrace();
                Files.move(tempHistoryFilePath, historyFilePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            logger.error("Error saving file: " + e.getMessage());
        } finally {
            try {
                Files.deleteIfExists(tempHistoryFilePath);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }

    // Connect this to SuggestionManager to get suggestions
    public static Set<String> getSuggestions() {
        return new java.util.HashSet<>(suggestions.keySet());
    }

}
