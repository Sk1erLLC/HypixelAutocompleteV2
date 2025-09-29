package club.sk1er.mods.hypixelautocomplete.mixin;

import club.sk1er.mods.hypixelautocomplete.suggestions.SuggestionService;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(ChatInputSuggestor.class)
public abstract class ChatInputSuggestorMixin {
    @Shadow @Final MinecraftClient client;
    @Final @Shadow TextFieldWidget textField;
    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow protected abstract void showCommandSuggestions();

    @Inject(
        method = "refresh",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/brigadier/CommandDispatcher;getCompletionSuggestions(Lcom/mojang/brigadier/ParseResults;I)Ljava/util/concurrent/CompletableFuture;"
        ),
        cancellable = true
    )
    private void hypixelautocomplete$overridePartyInvite(CallbackInfo ci) {
        String input = this.textField.getText();
        int cursor = this.textField.getCursor();

        CompletableFuture<Suggestions> guildSuggestions = SuggestionService.INSTANCE.buildSuggestions(input, cursor);
        if (guildSuggestions == null) {
            return;
        }

        this.pendingSuggestions = guildSuggestions;
        this.pendingSuggestions.thenRun(() -> {
            if (this.pendingSuggestions.isDone()) {
                this.showCommandSuggestions();
            }
        });
        ci.cancel();
    }
}
