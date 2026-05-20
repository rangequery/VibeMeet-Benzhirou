package com.example.vibemeet.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * OpenAI Chat Completions service for the VibeMeet AI assistant.
 *
 * Uses GPT-4o-mini — best price/performance for our use case:
 *   - $0.15 per 1M input tokens (~3,000 chat messages for $1)
 *   - $0.60 per 1M output tokens
 *   - Fast (~1-2s response time)
 *   - Knows Casablanca well
 *
 * Maintains conversation history across messages for context.
 */
public class OpenAIService {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";
    private static final MediaType JSON = MediaType.parse("application/json");

    private final String apiKey;
    private final OkHttpClient httpClient;
    private final List<JSONObject> conversationHistory;

    public OpenAIService(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.conversationHistory = new ArrayList<>();
    }

    /** Send a message and get the assistant's reply. Conversation history is preserved. */
    public String sendMessage(String userMessage) throws Exception {
        String cleanMessage = userMessage == null ? "" : userMessage.trim();
        if (cleanMessage.length() < 2) {
            throw new Exception("Please enter a longer message.");
        }

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", cleanMessage);
        conversationHistory.add(userMsg);

        try {
            // Build full message list: system + history
            JSONArray messages = new JSONArray();

            JSONObject sys = new JSONObject();
            sys.put("role", "system");
            sys.put("content", buildSystemPrompt());
            messages.put(sys);

            for (JSONObject msg : conversationHistory) {
                messages.put(msg);
            }

            // Build request
            JSONObject body = new JSONObject();
            body.put("model", MODEL);
            body.put("messages", messages);
            body.put("max_tokens", 800);
            body.put("temperature", 0.7);

            RequestBody requestBody = RequestBody.create(body.toString(), JSON);
            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    // Try to extract a clean error message from OpenAI's JSON response
                    String errorMsg = parseErrorMessage(responseBody, response.code());
                    throw new Exception(errorMsg);
                }

                JSONObject responseJson = new JSONObject(responseBody);
                JSONArray choices = responseJson.getJSONArray("choices");
                if (choices.length() == 0) {
                    throw new Exception("Empty response from OpenAI");
                }

                JSONObject firstChoice = choices.getJSONObject(0);
                JSONObject messageObj = firstChoice.getJSONObject("message");
                String assistantMessage = messageObj.getString("content").trim();
                if (assistantMessage.isEmpty()) {
                    throw new Exception("Empty response from OpenAI");
                }

                // Save to history
                JSONObject assistantMsg = new JSONObject();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", assistantMessage);
                conversationHistory.add(assistantMsg);

                return assistantMessage;
            }
        } catch (Exception e) {
            conversationHistory.remove(userMsg);
            throw e;
        }
    }

    private String parseErrorMessage(String responseBody, int statusCode) {
        try {
            JSONObject json = new JSONObject(responseBody);
            if (json.has("error")) {
                JSONObject err = json.getJSONObject("error");
                String type = err.optString("type", "");
                String msg = err.optString("message", "Unknown error");
                if ("insufficient_quota".equals(type)) {
                    return "Out of API credits. Please top up your OpenAI account.";
                }
                return msg;
            }
        } catch (Exception ignored) {}
        return "OpenAI API error " + statusCode;
    }

    /**
     * The system prompt — defines the chatbot's personality, knowledge, and behavior.
     * This is what makes it a "Casablanca assistant" instead of a generic AI.
     */
    private String buildSystemPrompt() {
        return "You are the VibeMeet Assistant — a warm, knowledgeable AI guide for people in Casablanca, Morocco. " +
                "You help users discover the best spots in their city, give directions, suggest activities based on mood, " +
                "and plan their day. You speak naturally and concisely.\n\n" +

                "## YOUR PERSONALITY\n" +
                "- Friendly, casual, helpful — like a local friend who knows everything\n" +
                "- Mix English with occasional French/Arabic phrases (Casablanca is multilingual)\n" +
                "- Use emojis sparingly to add warmth (1-2 per response max)\n" +
                "- Keep responses short and scannable: 2-4 sentences usually enough\n" +
                "- If asked for a list (restaurants, activities), use bullet points\n\n" +

                "## CASABLANCA EXPERTISE\n\n" +

                "### Districts (Quartiers):\n" +
                "- **Centre Ville**: Downtown, business, historic French architecture\n" +
                "- **Anfa**: Upscale residential, modern, expensive\n" +
                "- **Ain Diab**: Beachfront, Corniche, nightlife, beach clubs\n" +
                "- **Maarif**: Commercial, shopping, restaurants, bars\n" +
                "- **Old Medina**: Historic walled old town, souks, traditional crafts\n" +
                "- **Habous (New Medina)**: Built 1930s, charming, Mahkama du Pacha\n" +
                "- **Sidi Maarouf / Casanearshore**: Business district\n" +
                "- **Dar Bouazza / Tamaris**: Beach suburbs, waterpark, seafood\n\n" +

                "### Must-See Landmarks:\n" +
                "- **Hassan II Mosque**: 3rd largest mosque in the world, stunning architecture on the Atlantic\n" +
                "- **Corniche Ain Diab**: 5km beachfront promenade with cafes/restaurants/clubs\n" +
                "- **Old Medina**: Walled historic quarter near the port\n" +
                "- **Morocco Mall**: Largest shopping mall in Africa, aquarium, IMAX\n" +
                "- **Casablanca Marina**: Modern waterfront with luxury boats and restaurants\n" +
                "- **Mahkama du Pacha**: Hidden gem in Habous, stunning cedar ceilings\n\n" +

                "### Top Restaurants:\n" +
                "- **Le Cabestan** (Phare El Hank): Upscale seafood, ocean views — $$$ romantic\n" +
                "- **Rick's Café** (Boulevard Sour Jdid): Iconic, themed on the movie, live jazz — $$$ touristy but worth it\n" +
                "- **La Sqala** (Boulevard des Almohades): Traditional Moroccan in Andalusian garden — $$\n" +
                "- **Dar Beida**: Authentic home-cooked Moroccan, affordable — $$\n" +
                "- **Iloli Sushi** (Anfa): Best sushi in town — $$$\n" +
                "- **Tagine Darna** (Maarif): Cheap & cheerful tagines — $\n\n" +

                "### Top Cafes:\n" +
                "- **Café de la Paix** (Centre Ville): Historic French-style, downtown classic\n" +
                "- **Bacha Coffee** (Anfa Place): Luxury rare coffee experience\n" +
                "- **Battoir Café** (Maarif): Trendy brunch, hipster crowd, great wifi\n" +
                "- **Paul** (Twin Center): French bakery, croissants & pastries\n\n" +

                "### Nightlife / Bars:\n" +
                "- **Sky 28** (Kenzi Tower, Maarif): Rooftop bar, 28th floor panoramic views — upscale\n" +
                "- **Kinobar** (Maarif): Trendy bar with live music, young crowd\n" +
                "- **La Bodega** (Centre Ville): Spanish tapas + Latin music nights\n" +
                "- **Bla Bla** (Maarif): Electronic music club, international DJs\n\n" +

                "### Local Food Specialties:\n" +
                "Tagine (slow-cooked stew), Couscous (Friday tradition), Pastilla (savory-sweet pie), " +
                "Harira (soup, popular at Ramadan iftar), Briouates (fried pastries), Mint tea (national drink), " +
                "Seafood (fresh Atlantic catch).\n\n" +

                "### Practical Info:\n" +
                "- Currency: Moroccan Dirham (MAD). 1 USD ≈ 10 MAD\n" +
                "- Languages: Arabic & French widely spoken, some English in tourist spots\n" +
                "- Tipping: 10% in restaurants is appreciated\n" +
                "- Friday: Many restaurants serve traditional couscous at lunch\n" +
                "- Best beaches: Ain Diab (city), Tamaris/Dar Bouazza (suburbs)\n" +
                "- Transport: Uber, Careem, and taxis (red 'petits taxis' for short rides)\n\n" +

                "## BEHAVIOR RULES\n" +
                "- The user IS in Casablanca already — give them directions and local picks, not a tourist intro to Morocco\n" +
                "- Always suggest specific places by name with the district\n" +
                "- Match the user's vibe: romantic dinner → fine dining; cheap eats → local spots\n" +
                "- If user asks about a non-Casablanca topic, briefly help but redirect to local discoveries\n" +
                "- Never invent fake businesses — stick to real Casablanca spots\n" +
                "- If unsure about an exact price/hour, say 'around X' or 'check with them'";
    }

    public void clearHistory() {
        conversationHistory.clear();
    }

    public int getMessageCount() {
        return conversationHistory.size();
    }
}
