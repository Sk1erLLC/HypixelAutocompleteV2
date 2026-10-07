package club.sk1er.mods.hypixelautocomplete.suggestions;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuggestionServiceTest {
    @Test
    void hypixelAddressesCount() {
        for (String address : List.of("mc.hypixel.net", "hypixel.net", "MC.HYPIXEL.NET", "hypixel.net:25565", "alpha.hypixel.net", "hypixel")) {
            assertTrue(SuggestionService.isHypixelAddress(address), address);
        }
    }

    @Test
    void otherAddressesDoNot() {
        for (String address : List.of("localhost", "127.0.0.1:25565", "play.example.com", "hypixel.com", "hypixelnet.org", "")) {
            assertFalse(SuggestionService.isHypixelAddress(address), address);
        }
        assertFalse(SuggestionService.isHypixelAddress(null));
    }
}
