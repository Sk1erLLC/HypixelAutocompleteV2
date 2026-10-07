package club.sk1er.mods.hypixelautocomplete;


import club.sk1er.mods.hypixelautocomplete.suggestions.SuggestionHistoryManager;
import club.sk1er.mods.hypixelautocomplete.suggestions.SuggestionService;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Set;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

//#if FORGE
//$$ import net.minecraftforge.fml.common.Mod;
//$$ import net.minecraftforge.client.ConfigScreenHandler;
//$$ import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//#if MC==11902
//$$ import net.minecraftforge.fml.ModLoadingContext;
//#endif
//$$ @Mod(HypixelAutoComplete.MOD_ID)
//#endif
public class HypixelAutoComplete implements ModInitializer {
    public static final String MOD_ID = "hypixel_auto_complete";
    public static final String MOD_VERSION = "2.0";
    public static final String MOD_NAME = "Hypixel Autocomplete";

    private final SuggestionService suggestionService = new SuggestionService();
    private final SuggestionHistoryManager suggestionHistoryManager = new SuggestionHistoryManager();
    public static HypixelAutoComplete instance;

    public SuggestionHistoryManager getSuggestionHistoryManager() {
        return suggestionHistoryManager;
    }

    public SuggestionService getSuggestionService() {
        return suggestionService;
    }

    @Override
    public void onInitialize() {
        instance = this;

        // Create suggestion provider for usernames in history
        SuggestionProvider<FabricClientCommandSource> usernameSuggestions = (context, builder) -> {
            String remaining = builder.getRemaining();

            // Find where the current username being typed starts (after the last space)
            int lastSpaceIndex = remaining.lastIndexOf(' ');
            String currentWord = lastSpaceIndex >= 0 ? remaining.substring(lastSpaceIndex + 1) : remaining;
            String prefix = lastSpaceIndex >= 0 ? remaining.substring(0, lastSpaceIndex + 1) : "";

            // Parse already-typed usernames to exclude them from suggestions (lowercase for case-insensitive comparison)
            Set<String> alreadyTypedSet = new HashSet<>();
            if (!prefix.trim().isEmpty()) {
                for (String name : prefix.trim().split("\\s+")) {
                    alreadyTypedSet.add(name.toLowerCase());
                }
            }

            // Suggest usernames that match the current word and aren't already in the command
            SuggestionHistoryManager.getSuggestions().stream()
                .filter(username -> username.toLowerCase().startsWith(currentWord.toLowerCase()))
                .filter(username -> !alreadyTypedSet.contains(username.toLowerCase()))
                .forEach(username -> builder.suggest(prefix + username));

            return builder.buildFuture();
        };

        // Register /delname command with username suggestions
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("delname")
                .then(argument("usernames", StringArgumentType.greedyString())
                    .suggests(usernameSuggestions)
                    .executes(context -> {
                        String input = StringArgumentType.getString(context, "usernames");
                        String[] usernames = input.split("\\s+");

                        FabricClientCommandSource source = context.getSource();
                        int removed = 0;
                        int notFound = 0;

                        for (String username : usernames) {
                            if (username.trim().isEmpty()) continue;

                            if (suggestionHistoryManager.removeNameFromHistory(username)) {
                                removed++;
                            } else {
                                notFound++;
                            }
                        }

                        // Send feedback message
                        if (removed > 0 && notFound == 0) {
                            source.sendFeedback(Text.literal("§aRemoved " + removed + " username" + (removed == 1 ? "" : "s") + " from suggestion history"));
                        } else if (removed == 0 && notFound > 0) {
                            source.sendFeedback(Text.literal("§cNo usernames found in suggestion history"));
                        } else if (removed > 0 && notFound > 0) {
                            source.sendFeedback(Text.literal("§aRemoved " + removed + " username" + (removed == 1 ? "" : "s") + "§r, §c" + notFound + " not found"));
                        }

                        return 1;
                    })
                )
            )
        );
    }
}
