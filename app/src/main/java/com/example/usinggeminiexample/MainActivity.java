package com.example.usinggeminiexample;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

        mGetPicture = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                new ActivityResultCallback<Bitmap>() {
                    @Override
                    public void onActivityResult(Bitmap result) {
                        // Handle the returned Bitmap
                        ImageView myImageView = findViewById(R.id.imageView);
                        myImageView.setImageBitmap(result);
                    }
                }
        );

    }

    private void initUI() {
        editTextInput = findViewById(R.id.editTextInput);
        textViewResult = findViewById(R.id.textViewResult);
        Button buttonSendText = findViewById(R.id.buttonSendText);

        // Initialize GeminiManager with API key
        GeminiManager geminiManager = new GeminiManager(BuildConfig.GEMINI_API_KEY);

        // Set button click listener
        buttonSendText.setOnClickListener(v -> {
            String inputText = editTextInput.getText().toString();
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

        Button btnSendImageAndText = findViewById(R.id.buttonUpload);
        btnSendImageAndText.setOnClickListener(view -> {
            // 1. Get the bitmap from the ImageView
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



    private ActivityResultLauncher<Void> mGetPicture;




    public void takePicture(View view) {

        mGetPicture.launch(null);
    }
}