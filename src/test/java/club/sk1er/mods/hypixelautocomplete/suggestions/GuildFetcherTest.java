package club.sk1er.mods.hypixelautocomplete.suggestions;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GuildFetcherTest {
    @Test
    void validResponseYieldsEveryMember() {
        String body = "{\"members\":[\"GuildMaster\",\"guildie_2\"]}";
        assertEquals(2, GuildFetcher.getMembers(body).size());
        assertEquals(Set.of("GuildMaster", "guildie_2"), GuildFetcher.parseMembers(body));
    }

    @Test
    void nonObjectJsonYieldsNoMembers() {
        String body = "[\"GuildMaster\",\"guildie_2\"]";
        assertNull(GuildFetcher.getMembers(body));
        assertEquals(Set.of(), GuildFetcher.parseMembers(body));
    }

    @Test
    void missingMembersYieldsNoMembers() {
        String body = "{\"guild\":\"Sk1er\"}";
        assertNull(GuildFetcher.getMembers(body));
        assertEquals(Set.of(), GuildFetcher.parseMembers(body));
    }

    @Test
    void invalidJsonYieldsNoMembers() {
        String body = "{\"members\":[\"GuildMaster\"";
        assertNull(GuildFetcher.getMembers(body));
        assertEquals(Set.of(), GuildFetcher.parseMembers(body));
    }
}
