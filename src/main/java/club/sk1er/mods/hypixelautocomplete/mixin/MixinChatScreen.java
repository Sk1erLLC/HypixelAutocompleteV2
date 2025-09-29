package club.sk1er.mods.hypixelautocomplete.mixin;

import club.sk1er.mods.hypixelautocomplete.suggestions.SuggestionHistoryManager;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class MixinChatScreen {

    @Inject(method = "sendMessage", at = @At("HEAD") )
    private void onSendMessage(String message, boolean addToHistory, CallbackInfo ci) {
        SuggestionHistoryManager.INSTANCE.capture(message);
    }
}
