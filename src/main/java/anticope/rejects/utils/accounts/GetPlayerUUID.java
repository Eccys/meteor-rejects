package anticope.rejects.utils.accounts;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class GetPlayerUUID {

    public static UUID getUUID(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return UUID.randomUUID();
        }
        UUID parsed = parseUuidFromMojangJson(
                getURLContent("https://api.mojang.com/users/profiles/minecraft/" + playerName.trim()));
        return parsed != null ? parsed : UUID.randomUUID();
    }

    public static UUID parseUuidFromMojangJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            Gson gsonReader = new Gson();
            JsonObject jsonObject = gsonReader.fromJson(json, JsonObject.class);
            if (jsonObject == null || !jsonObject.has("id") || jsonObject.get("id").isJsonNull()) {
                return null;
            }
            String rawId = jsonObject.get("id").getAsString().trim();
            String dashed = rawId.contains("-")
                    ? rawId
                    : rawId.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})", "$1-$2-$3-$4-$5");
            return UUID.fromString(dashed);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String getURLContent(String requestedUrl) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(requestedUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "";
            }
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                return br.lines().collect(Collectors.joining());
            }
        } catch (Exception e) {
            return "";
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

}