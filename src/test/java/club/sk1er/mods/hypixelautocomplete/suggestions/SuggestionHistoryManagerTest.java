package club.sk1er.mods.hypixelautocomplete.suggestions;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SuggestionHistoryManagerTest {
    private static List<String> extract(String message) {
        return SuggestionHistoryManager.extractUsernames(message);
    }

    @Test
    void whisperCommandsRecordTheRecipient() {
        for (String command : List.of("/msg", "/tell", "/w", "/t", "/whisper", "/boop")) {
            assertEquals(List.of("Notch"), extract(command + " Notch hi"), command);
        }
        assertEquals(List.of("Notch"), extract("/msg Notch"));
    }

    @Test
    void partyCommandsRecordEveryNamedPlayer() {
        assertEquals(List.of("jeb_", "Dinnerbone"), extract("/p invite jeb_ Dinnerbone"));
        assertEquals(List.of("jeb_"), extract("/party kick jeb_"));
        // "/p <names>" invites without the subcommand
        assertEquals(List.of("jeb_", "Dinnerbone"), extract("/p jeb_ Dinnerbone"));
    }

    @Test
    void partyCommandsWithoutPlayersRecordNothing() {
        assertEquals(List.of(), extract("/p leave"));
        assertEquals(List.of(), extract("/party warp"));
        assertEquals(List.of(), extract("/p invite"));
    }

    @Test
    void otherMessagesRecordNothing() {
        assertEquals(List.of(), extract("hello there"));
        assertEquals(List.of(), extract("/msg"));
        assertEquals(List.of(), extract("/msgs Notch hi"));
        assertEquals(List.of(), extract("/gamemode creative"));
    }
}
