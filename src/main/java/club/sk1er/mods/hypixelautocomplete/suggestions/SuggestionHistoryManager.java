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
    // Key is lowercase username, value is UsernameEntry with original casing
    private static HashMap<String, UsernameEntry> suggestions = new HashMap<>();

    private Path configPath;
    private Path tempHistoryFilePath;
    private Path historyFilePath;

    public SuggestionHistoryManager() {
        configPath = MinecraftClient.getInstance().runDirectory.toPath().resolve("config");
        tempHistoryFilePath = configPath.resolve("suggestionHistoryTemp.txt");
        historyFilePath = configPath.resolve("suggestionHistory.txt");

        loadConfig();

        // Shutdown hook to save config on exit
        Thread printingHook = new Thread(this::saveConfig);
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
        logger.info("Captured message: {}", message);

        String[] split = message.split(" ");

        if (split.length < 2) {
            return;
        }

        if (SuggestionService.getPartyCommands().contains(split[0] + " ")) {

            // if command is a non-username party command (leave, disband, etc)
            if (SuggestionService.getPartyCommandsExcludeNoUser().contains(split[1])) {
                return;
            }

            // if command has two non-user args (/p invite, /p transfer, etc)
            int startIndex = SuggestionService.getPartyCommandsExcludeUser().contains(split[1]) ? 2 : 1;
            for (int i = startIndex; i < split.length; i++) {
                captureUsername(split[i]);
            }
        } else if (SuggestionService.getWhisperCommands().contains(split[0] + " ")) {
            // single-username command
            captureUsername(split[1]);
        }
    }

    /**
     * Captures a username and stores it in the suggestions map with lowercase key
     * For new usernames, fetches the proper casing from the Minecraft API
     * @param username The username to capture (with original casing)
     */
    private void captureUsername(String username) {
        String lowercaseKey = username.toLowerCase();

        if (suggestions.containsKey(lowercaseKey)) {
            // Username already exists, just increment the count
            suggestions.get(lowercaseKey).incrementOccurrences();
        } else {
            // New username - add it with the typed casing first
            UsernameEntry entry = new UsernameEntry(username, 1);
            suggestions.put(lowercaseKey, entry);

            // Asynchronously fetch the proper casing from Minecraft API
            logger.info("New username '{}' detected, fetching proper casing from Minecraft API", username);
            MinecraftAPIService.getProperUsername(username).thenAccept(properName -> {
                if (properName != null && !properName.equals(username)) {
                    // Update with the proper casing
                    entry.setUsername(properName);
                    logger.info("Updated username casing from '{}' to '{}'", username, properName);
                    saveConfig(); // Persist the updated casing
                }
            }).exceptionally(throwable -> {
                logger.error("Error fetching proper username for '{}': {}", username, throwable.getMessage());
                return null;
            });
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
                if (split.length == 2) {
                    String username = split[0];
                    int occurrences = Integer.parseInt(split[1]);
                    String lowercaseKey = username.toLowerCase();
                    suggestions.put(lowercaseKey, new UsernameEntry(username, occurrences));
                } else {
                    logger.error("Malformed line in history file: {}", line);
                }
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

            for (Map.Entry<String, UsernameEntry> entry : suggestions.entrySet()) {
                UsernameEntry usernameEntry = entry.getValue();
                writer.write(usernameEntry.getUsername() + " " + usernameEntry.getOccurrences() + "\n");
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
    // Returns usernames with original casing
    public static Set<String> getSuggestions() {
        Set<String> usernames = new java.util.HashSet<>();
        for (UsernameEntry entry : suggestions.values()) {
            usernames.add(entry.getUsername());
        }
        return usernames;
    }

    /**
     * Removes a username from the suggestion history (case-insensitive)
     * @param username The username to remove
     * @return true if the username was found and removed, false otherwise
     */
    public boolean removeNameFromHistory(String username) {
        String lowercaseKey = username.toLowerCase();
        if (suggestions.containsKey(lowercaseKey)) {
            UsernameEntry entry = suggestions.remove(lowercaseKey);
            saveConfig();
            logger.info("Removed username '{}' from suggestion history", entry.getUsername());
            return true;
        }
        logger.info("Username '{}' not found in suggestion history", username);
        return false;
    }

}
