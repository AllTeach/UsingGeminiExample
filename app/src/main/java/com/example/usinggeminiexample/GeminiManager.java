package com.example.usinggeminiexample;

import android.graphics.Bitmap;
import android.util.Base64;

import androidx.annotation.NonNull;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public class GeminiManager {

    public interface GeminiCallback {
        void onResponse(String response);
    }

    private final String credential;
    private final OkHttpClient httpClient;

    // Use Gemini 2.5 Flash for the best balance of speed and high quota
    private static final String MODEL_ID = "gemini-2.5-flash";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_ID + ":generateContent";

    public GeminiManager(String credential) {
        this.credential = credential;
        this.httpClient = new OkHttpClient.Builder().build();
    }

    public void sendText(String inputText, @NonNull GeminiCallback callback) {
        new Thread(() -> {
            try {
                JSONObject root = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject contentObj = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject part = new JSONObject();

                part.put("text", inputText);
                parts.put(part);
                contentObj.put("parts", parts);
                contents.put(contentObj);
                root.put("contents", contents);

                MediaType jsonMediaType = MediaType.parse("application/json; charset=utf-8");
                RequestBody body = RequestBody.create(root.toString(), jsonMediaType);

                Request.Builder reqBuilder = new Request.Builder();

                // Build the URL with API Key
                HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL).newBuilder();

                if (credential != null && !credential.startsWith("ya29")) {
                    urlBuilder.addQueryParameter("key", credential);
                }

                reqBuilder.url(urlBuilder.build());

                if (credential != null && credential.startsWith("ya29")) {
                    reqBuilder.addHeader("Authorization", "Bearer " + credential);
                }

                Request request = reqBuilder.post(body)
                        .addHeader("Content-Type", "application/json")
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    String respBody = response.body() != null ? response.body().string() : "";

                    if (!response.isSuccessful()) {
                        // Log specifically for 429 or 404
                        String errorMsg = "Error " + response.code() + ": " + respBody;
                        callback.onResponse(errorMsg);
                        return;
                    }
                    callback.onResponse(respBody);
                }
            } catch (Exception e) {
                callback.onResponse("Exception: " + e.getMessage());
            }
        }).start();
    }

    public void sendImageAndText(Bitmap bitmap, String prompt, @NonNull GeminiCallback callback) {
        new Thread(() -> {
            try {
                // 1. Convert Bitmap to Base64
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream); // 80% quality to save quota/bandwidth
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                String base64Image = Base64.encodeToString(byteArray, Base64.NO_WRAP);

                // 2. Build JSON with 2 parts: Image and Text
                JSONObject root = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject contentObj = new JSONObject();
                JSONArray parts = new JSONArray();

                // Part 1: The Image
                JSONObject imagePart = new JSONObject();
                JSONObject inlineData = new JSONObject();
                inlineData.put("mimeType", "image/jpeg");
                inlineData.put("data", base64Image);
                imagePart.put("inlineData", inlineData);
                parts.put(imagePart);

                // Part 2: The Text
                JSONObject textPart = new JSONObject();
                textPart.put("text", prompt);
                parts.put(textPart);

                contentObj.put("parts", parts);
                contents.put(contentObj);
                root.put("contents", contents);

                // 3. Make the Request (reuse your existing OkHttp logic)
                MediaType jsonMediaType = MediaType.parse("application/json; charset=utf-8");
                RequestBody body = RequestBody.create(root.toString(), jsonMediaType);

                // Note: Use Gemini 2.5 Flash as it handles images much better than older versions
                String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + credential;

                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    String respBody = response.body().string();
                    callback.onResponse(respBody);
                }
            } catch (Exception e) {
                callback.onResponse("Error: " + e.getMessage());
            }
        }).start();
    }

}