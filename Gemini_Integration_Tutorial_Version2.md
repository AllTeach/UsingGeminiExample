# Integrating Gemini AI into an Android Application: A Step-by-Step Tutorial

This tutorial guides you through the process of integrating Google's Gemini AI API into an Android application. We will build an app that can send text prompts and images to the Gemini model and display the responses.

## Prerequisites & Theoretical Setup

Before writing code, we need to understand the components required for network communication and API authentication.

### 1. Obtaining an API Key
To access Google's Gemini models, you need an API key. This key identifies your application and tracks your usage against quotas.

1.  Go to [Google AI Studio](https://aistudio.google.com/).
2.  Sign in with your Google account.
3.  Click on **"Get API key"**.
4.  Create a key in a new or existing Google Cloud project.
5.  **Important:** Copy this key and save it securely. You will need to paste it into the `MainActivity.java` file later.

### 2. Gradle Dependencies (`build.gradle.kts`)
We need external libraries to handle network requests and JSON parsing.

*   **OkHttp:** A robust and efficient HTTP client for Android. We use it to send POST requests to the Gemini API.
*   **org.json:** A library to manually construct the JSON payloads required by the Gemini API and parse the JSON responses.

Open your app-level `build.gradle.kts` file and add the following to the `dependencies` block:

```kotlin
dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    // Network client for making API calls
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    // JSON library for parsing responses and building requests
    implementation("org.json:json:20230227")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
```

### 3. Manifest Permissions (`AndroidManifest.xml`)
Android applications run in a sandbox. By default, they cannot access the internet. We must explicitly request this permission in the manifest file. Without this, any network call will fail with a `SecurityException`.

Open `app/src/main/AndroidManifest.xml` and add the `<uses-permission>` tag **above** the `<application>` tag:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- REQUIRED: Permission to access the internet -->
    <uses-permission android:name="android.permission.INTERNET"></uses-permission>

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.UsingGeminiExample">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

---

## Phase 2: Building the Network Layer (`GeminiManager`)

We will create a helper class called `GeminiManager`. This class isolates the complexity of networking and JSON formatting from our UI code.

### Theoretical Concepts
1.  **Threading:** Network operations in Android **must not** run on the main UI thread. If they do, the app will freeze and eventually crash (ANR). We will use a standard Java `Thread` to perform the network call in the background.
2.  **Callbacks:** Since the network call happens on a background thread, we cannot simply `return` the data immediately. Instead, we define an interface (`GeminiCallback`) to "call back" the main code when the data is ready.
3.  **Model Selection:** We are using `gemini-2.5-flash`. This model is optimized for speed and cost-efficiency (higher rate limits) compared to the "Pro" models, making it ideal for testing and tutorials.

Create a new file `GeminiManager.java`:

```java
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

    // Interface to return data to MainActivity asynchronously
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
                // Construct the JSON Structure required by Gemini
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

                // Optional: Handle bearer tokens if needed (advanced usage)
                if (credential != null && credential.startsWith("ya29")) {
                    reqBuilder.addHeader("Authorization", "Bearer " + credential);
                }

                Request request = reqBuilder.post(body)
                        .addHeader("Content-Type", "application/json")
                        .build();

                // Execute the request synchronously (safe because we are inside a Thread)
                try (Response response = httpClient.newCall(request).execute()) {
                    String respBody = response.body() != null ? response.body().string() : "";

                    if (!response.isSuccessful()) {
                        // Log specifically for 429 (Rate Limit) or 404
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
    
    // ... We will add the Image method in Phase 4
}
```

---

## Phase 3: The User Interface (`MainActivity`)

Now we connect the UI to the `GeminiManager`.

### Theoretical Concepts
1.  **Main Thread vs. Background Thread:** The `GeminiCallback` returns data on the background thread (where the network call happened). However, Android **only allows UI updates (like `setText`) on the Main Thread**.
2.  **`runOnUiThread`:** To bridge this gap, we use `runOnUiThread(() -> { ... })` to post the update logic back to the main thread.
3.  **JSON Parsing:** The API returns a complex JSON object. We need to drill down into `candidates` -> `content` -> `parts` to find the actual text answer.

Open `MainActivity.java`:

```java
package com.example.usinggeminiexample;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.usinggeminiexample.GeminiManager;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private EditText editTextInput;
    private TextView textViewResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Initialize UI components
        initUI();

    }

    private void initUI() {
        editTextInput = findViewById(R.id.editTextInput);
        textViewResult = findViewById(R.id.textViewResult);
        Button buttonSendText = findViewById(R.id.buttonSendText);

        // TODO: REPLACE "YOUR_API_KEY_HERE!!!!" with the key you got from AI Studio
        GeminiManager geminiManager = new GeminiManager("YOUR_API_KEY_HERE!!!!");

        // Set button click listener
        buttonSendText.setOnClickListener(v -> {
            String inputText = editTextInput.getText().toString();
            // Prepend instructions to keep answers concise for mobile
            inputText = "answer in 4-5 short sentences maximum. Question: " + inputText;
            
            // Call the API and update the result TextView
            geminiManager.sendText(inputText, response -> {
                // If the response starts with "Error", show it as is
                if (response.startsWith("Error")) {
                    runOnUiThread(() -> textViewResult.setText(response));
                    return;
                }

                // Otherwise, parse the clean text
                String cleanText = parseGeminiResponse(response);

                // Update the UI on the Main Thread
                runOnUiThread(() -> {
                    textViewResult.setText(cleanText);
                });
            });
        });
        
        // ... Image logic will be added next
    }

    private String parseGeminiResponse(String rawJson) {
        try {
            JSONObject responseJson = new JSONObject(rawJson);

            // 1. Get the 'candidates' array
            JSONArray candidates = responseJson.getJSONArray("candidates");

            // 2. Get the first candidate's 'content'
            JSONObject firstCandidate = candidates.getJSONObject(0);
            JSONObject content = firstCandidate.getJSONObject("content");

            // 3. Get the first 'part' and its 'text'
            JSONArray parts = content.getJSONArray("parts");
            String aiText = parts.getJSONObject(0).getString("text");

            return aiText;
        } catch (Exception e) {
            // Fallback in case of an error (like a safety block or empty response)
            return "Could not parse response: " + e.getMessage();
        }
    }
}
```

---

## Phase 4: Testing Text Scenarios

At this point, you should run the app and test simple text questions.

**Test Scenarios:**
1.  **Simple Fact:** "What is the capital of France?"
2.  **Creative:** "Write a haiku about coding."

**Important Note on Limits:**
We are using the `gemini-2.5-flash` model. While it has a generous free tier, if you send requests too quickly, you might receive a `429 Too Many Requests` error. If this happens, wait a minute and try again.

---

## Phase 5: Adding Image Support (Multimodal)

Gemini is multimodal, meaning it can understand images. We will now add the ability to send an image (Bitmap) along with a prompt.

### Theoretical Concepts
*   **Base64 Encoding:** APIs cannot easily receive raw binary image files in a JSON body. We must convert the `Bitmap` into a text string using Base64 encoding.
*   **Image Compression:** To save bandwidth and quota, we compress the JPEG to 80% quality before sending.

### Step 1: Update `GeminiManager.java`
Add the following method to the class:

```java
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
```

### Step 2: Update `MainActivity.java`
Add the button listener for the image upload button.

```java
        Button btnSendImageAndText = findViewById(R.id.buttonUpload);
        btnSendImageAndText.setOnClickListener(view -> {
            // 1. Get the bitmap from the ImageView
            // Ensure you have an ImageView with id 'imageView' in your layout
            ImageView myImageView = findViewById(R.id.imageView);
            Bitmap bitmap = ((BitmapDrawable) myImageView.getDrawable()).getBitmap();
            
            String inputText = editTextInput.getText().toString();
            inputText = "answer only if the question relates to the image otherwise - answer that you don't support this," +
                    "if the question relates - please answer in 4-5 short sentences maximum. Question: " + inputText;
            
            geminiManager.sendImageAndText(bitmap, inputText, response -> {
                // If the response starts with "Error", show it as is
                if (response.startsWith("Error")) {
                    runOnUiThread(() -> textViewResult.setText(response));
                    return;
                }

                // Otherwise, parse the clean text
                String cleanText = parseGeminiResponse(response);

                // Update the UI on the Main Thread
                runOnUiThread(() -> {
                    textViewResult.setText(cleanText);
                });
            });

        });
```

### Testing Multimodal
1.  Ensure your layout (`activity_main.xml`) has an `ImageView` loaded with a drawable (e.g., a photo of a cat or a landmark).
2.  Run the app.
3.  Type a question like "What color is the animal?"
4.  Click the upload button.
5.  Gemini should analyze both the image data and your text to provide an answer.