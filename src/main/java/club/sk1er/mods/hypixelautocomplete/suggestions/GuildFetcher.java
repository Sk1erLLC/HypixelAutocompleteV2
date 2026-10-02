package club.sk1er.mods.hypixelautocomplete.suggestions;

import club.sk1er.mods.hypixelautocomplete.HypixelAutoComplete;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class GuildFetcher {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final String USER_AGENT = "Hypixel Auto Complete " + HypixelAutoComplete.MOD_VERSION;
    private static final String API_TEMPLATE = "https://api.sk1er.club/autocomplete/%s";

    private GuildFetcher() {
    }

    public static Set<String> fetch(MinecraftClient minecraft) {
        //#if MC<=12107
        UUID uuid = minecraft.getGameProfile().getId();
        //#else
        //$$ UUID uuid = minecraft.getGameProfile().id();
        //#endif
        HttpURLConnection connection = null;
        try {
            String format = String.format(API_TEMPLATE, uuid);
            connection = openConnection(new URL(format));
            int status = connection.getResponseCode();
            InputStream stream = status >= HttpURLConnection.HTTP_BAD_REQUEST
                ? connection.getErrorStream()
                : connection.getInputStream();
            if (stream == null) {
                return Set.of();
            }
            String body = IOUtils.toString(stream, StandardCharsets.UTF_8);
            if (status != HttpURLConnection.HTTP_OK) {
                return Set.of();
            }
            return parseMembers(body);
        } catch (IOException e) {
            e.printStackTrace();
            return Set.of();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout((int) REQUEST_TIMEOUT.toMillis());
        connection.setReadTimeout((int) REQUEST_TIMEOUT.toMillis());
        connection.setRequestMethod("GET");
        connection.setUseCaches(false);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        return connection;
    }

    // Package-private so the parsing can be unit tested without a connection.
    static Set<String> parseMembers(String body) {
        JsonArray members = getMembers(body);
        if (members == null) {
            return Set.of();
        }
        Set<String> collected = new HashSet<>();
        for (JsonElement member : members) {
            collected.add(member.getAsString());
        }
        return collected;
    }

    static JsonArray getMembers(String body) {
        try {
            JsonElement root = JsonParser.parseString(body);
            if (!root.isJsonObject()) {
                return null;
            }
            return root.getAsJsonObject().getAsJsonArray("members");
        } catch (Exception ignored) {
            return null;
        }
    }
}
