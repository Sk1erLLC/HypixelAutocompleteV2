package club.sk1er.mods.hypixelautocomplete.suggestions;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Service for interacting with the Mojang/Minecraft API
 * Used to resolve proper username casing
 */
public class MinecraftAPIService {
    private static final String MOJANG_API_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final Logger logger = LogManager.getLogger("HypixelAutoComplete");
    private static final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    /**
     * Fetches the proper casing for a username from the Mojang API
     * @param username The username to lookup (case-insensitive)
     * @return CompletableFuture with the properly-cased username, or null if not found
     */
    public static CompletableFuture<String> getProperUsername(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MOJANG_API_URL + username))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    String properName = json.get("name").getAsString();
                    logger.info("Resolved username '{}' to proper casing '{}'", username, properName);
                    return properName;
                } else if (response.statusCode() == 204 || response.statusCode() == 404) {
                    logger.warn("Username '{}' not found in Mojang API", username);
                    return null;
                } else {
                    logger.error("Mojang API returned status code {} for username '{}'", response.statusCode(), username);
                    return null;
                }
            } catch (Exception e) {
                logger.error("Failed to fetch proper username for '{}': {}", username, e.getMessage());
                return null;
            }
        });
    }
}
