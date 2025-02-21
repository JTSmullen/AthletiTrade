import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class NbaDataService {

    private static final String API_BASE_URL = "http://127.0.0.1:5000";

    public String getPlayerID(String playerName) throws Exception {
        String encodedPlayerName = URLEncoder.encode(playerName, StandardCharsets.UTF_8.toString());
        String url = API_BASE_URL + "/player/" + encodedPlayerName;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            CloseableHttpResponse response = httpClient.execute(httpGet);

            try {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    if (jsonResponse.has("player_id")) {
                        int playerIdInt = jsonResponse.getInt("player_id");
                        return String.valueOf(playerIdInt);
                    } else {
                        throw new Exception("Player ID not found in response");
                    }
                } else if (statusCode == 404) {
                    return null; // Player not found
                }
                else {
                    throw new Exception("API request failed with status code: " + statusCode);
                }
            } finally {
                response.close();
            }
        } catch (Exception e) {
            throw new Exception("Error fetching player ID: " + e.getMessage());
        }
    }

    public String getPlayerGameLog(String playerId, String season) throws Exception {
        String url = API_BASE_URL + "/playergamelog/" + playerId + "/" + season;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            CloseableHttpResponse response = httpClient.execute(httpGet);

            try {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    return EntityUtils.toString(response.getEntity());
                } else {
                    throw new Exception("API request failed with status code: " + statusCode);
                }
            } finally {
                response.close();
            }
        } catch (Exception e) {
            throw new Exception("Error fetching player game log: " + e.getMessage());
        }
    }
}