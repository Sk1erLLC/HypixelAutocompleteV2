package club.sk1er.mods.hypixelautocomplete.suggestions;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class SuggestionService {

    // Commands that accept multiple usernames
    private static final List<String> PARTY_COMMANDS = List.of(
        "/party invite ",
        "/p invite "
    );

    // Commands that accept a single username
    private static final List<String> WHISPER_COMMANDS = List.of(
        "/msg ",
        "/tell ",
        "/w ",
        "/t ",
        "/whisper ",
        "/boop "
    );

    private final Logger logger = LogManager.getLogger("HypixelAutoComplete");
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "HypixelAutoComplete-Worker");
        thread.setDaemon(true);
        return thread;
    });

    private final AtomicReference<Set<String>> guildMembers = new AtomicReference<>(Collections.emptySet());

    public SuggestionService() {
        scheduleGuildRefresh();
    }

    public void scheduleGuildRefresh() {
        executor.submit(this::refreshGuildMembers);
    }

    public List<String> getGuildMembers() {
        return guildMembers.get().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    public CompletableFuture<Suggestions> buildSuggestions(String input, int cursor) {
        if (!isHypixel()) {
            return null;
        }

        String matchedCommand = getMatchingCommand(input, cursor);
        if (matchedCommand == null) {
            return null;
        }

        // For whisper commands, only suggest on the first argument
        if (WHISPER_COMMANDS.contains(matchedCommand)) {
            int commandEnd = matchedCommand.length();
            int start = Math.max(input.lastIndexOf(' ') + 1, 0);
            String afterCommand = input.substring(commandEnd, start).trim();
            // If there's text before the current word, we're past the first argument
            if (!afterCommand.isEmpty()) {
                return new SuggestionsBuilder(input, start-1).suggest("").buildFuture();
            }
        }

        List<String> members = getGuildMembers();
        if (members.isEmpty()) {
            return null;
        }

        int start = Math.max(input.lastIndexOf(' ') + 1, 0);
        String partialText = input.substring(start).toLowerCase();

        // For whisper commands, check if username is complete or unknown
        if (WHISPER_COMMANDS.contains(matchedCommand)) {
            // Check if it's an exact match (full username)
            boolean isExactMatch = members.stream()
                .anyMatch(member -> member.equalsIgnoreCase(partialText));
            
            // Check if there are any partial matches
            boolean hasPartialMatches = members.stream()
                .anyMatch(member -> member.toLowerCase().startsWith(partialText));
            
            // Return empty suggestion if exact match or no matches
            if (isExactMatch || !hasPartialMatches) {
                return new SuggestionsBuilder(input, start).suggest("").buildFuture();
            }
        }

        // Extract already mentioned users from the command (for party commands)
        Set<String> alreadyMentioned = getAlreadyMentionedUsers(input, matchedCommand, start);

        SuggestionsBuilder builder = new SuggestionsBuilder(input, start);
        members.stream()
            .filter(member -> member.toLowerCase().startsWith(partialText))
            .filter(member -> !alreadyMentioned.contains(member.toLowerCase()))
            .forEach(builder::suggest);

        // Always provide at least one suggestion to avoid displaying
        // unknown command error
        if (builder.build().isEmpty()) {
            builder.suggest("");
        }

        return builder.buildFuture();
    }

    private String getMatchingCommand(String input, int cursor) {
        if (input == null) {
            return null;
        }
        String slice = input.substring(0, Math.min(cursor, input.length())).toLowerCase();
        
        for (String cmd : PARTY_COMMANDS) {
            if (slice.startsWith(cmd)) {
                return cmd;
            }
        }
        
        for (String cmd : WHISPER_COMMANDS) {
            if (slice.startsWith(cmd)) {
                return cmd;
            }
        }
        
        return null;
    }

    private Set<String> getAlreadyMentionedUsers(String input, String command, int currentStart) {
        Set<String> mentioned = new HashSet<>();
        int commandEnd = command.length();

        // Extract all previously mentioned users (for party commands)
        if (currentStart > commandEnd) {
            String usersPart = input.substring(commandEnd, currentStart).trim();
            if (!usersPart.isEmpty()) {
                Arrays.stream(usersPart.split("\\s+"))
                    .filter(user -> !user.isEmpty())
                    .map(String::toLowerCase)
                    .forEach(mentioned::add);
            }
        }

        return mentioned;
    }

    private void refreshGuildMembers() {
        CompletableFuture.supplyAsync(() -> GuildFetcher.fetch(MinecraftClient.getInstance()), executor)
            .thenAccept(guildMembers::set)
            .thenAccept(members -> logger.log(Level.DEBUG, "Fetched {} guild members", guildMembers.get().size()));
    }

    private boolean isHypixel() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getCurrentServerEntry() == null) {
            return false;
        }
        String address = client.getCurrentServerEntry().address.toLowerCase();
        return address.contains("hypixel.net") || address.equals("hypixel");
    }
}
