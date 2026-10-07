package club.sk1er.mods.hypixelautocomplete.suggestions;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class SuggestionService {


    // Party commands to exclude from saved recommendations with a username
    private static final List<String> PARTY_COMMANDS_EXCLUDE_USER = List.of(
            "invite",
            "challenge",
            "promote",
            "remove",
            "kick",
            "accept"
    );
    // Party commands to exclude from saved recommendations without a username
    private static final List<String> PARTY_COMMANDS_EXCLUDE_NOUSER = List.of(
            "kickoffline",
            "leave",
            "disband",
            "private",
            "home",
            "warp",
            "list",
            "mute"
    );

    private static final List<String> PARTY_COMMAND_PREFIXES = List.of("/p ", "/party ");

    // All party commands (both with and without username, plus the empty string)
    private static final List<String> PARTY_COMMANDS = PARTY_COMMAND_PREFIXES.stream()
            .flatMap(prefix -> Stream.concat(
                    Stream.concat(PARTY_COMMANDS_EXCLUDE_USER.stream(), PARTY_COMMANDS_EXCLUDE_NOUSER.stream()),
                    Stream.of("") // include empty command
            ).map(cmd -> prefix + cmd + (cmd.isEmpty() ? "" : " ")))
            .toList();


    // Commands that accept a single username
    private static final List<String> WHISPER_COMMANDS = List.of(
        "/msg ",
        "/tell ",
        "/w ",
        "/t ",
        "/whisper ",
        "/boop "
    );

    public static List<String> getPartyCommands() { return PARTY_COMMANDS; }
    public static List<String> getPartyCommandsExcludeUser() { return PARTY_COMMANDS_EXCLUDE_USER; }
    public static List<String> getPartyCommandsExcludeNoUser() { return PARTY_COMMANDS_EXCLUDE_NOUSER; }
    public static List<String> getWhisperCommands() { return WHISPER_COMMANDS; }

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
        if (!isOnHypixel()) {
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

        String[] split = matchedCommand.split(" ");
        String splitCommand = split.length > 1 ? split[1] : "";

        // if command shouldn't be followed by a username (/p leave, etc)
        if (!splitCommand.isEmpty() && PARTY_COMMANDS_EXCLUDE_NOUSER.contains(splitCommand)) {
            return null;
        }

        Set<String> members = new HashSet<>(getGuildMembers());
        members.addAll(SuggestionHistoryManager.getSuggestions());

        // For party commands, add potential second args that are not usernames
        if (PARTY_COMMANDS.contains(matchedCommand) && !PARTY_COMMANDS_EXCLUDE_USER.contains(splitCommand) && !PARTY_COMMANDS_EXCLUDE_NOUSER.contains(splitCommand)) {
            members.addAll(PARTY_COMMANDS_EXCLUDE_USER);
            members.addAll(PARTY_COMMANDS_EXCLUDE_NOUSER);
        }

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

    /**
     * Whether the player is connected to Hypixel, judged by the address of the server they joined.
     */
    public static boolean isOnHypixel() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getCurrentServerEntry() == null) {
            return false;
        }
        return isHypixelAddress(client.getCurrentServerEntry().address);
    }

    /**
     * Whether a server address is Hypixel's. Kept free of game state so it can be unit tested.
     */
    static boolean isHypixelAddress(String address) {
        if (address == null) {
            return false;
        }
        String lowercase = address.toLowerCase();
        return lowercase.contains("hypixel.net") || lowercase.equals("hypixel");
    }
}
