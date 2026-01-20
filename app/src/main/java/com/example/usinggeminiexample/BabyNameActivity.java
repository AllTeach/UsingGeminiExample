package com.example.usinggeminiexample;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class BabyNameActivity extends AppCompatActivity {

    private EditText editTextCustomName;
    private Button buttonSearch;
    private RecyclerView recyclerView;
    private BabyNameAdapter adapter;
    private List<BabyName> babyNameList;
    private GeminiManager geminiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_baby_name);

        String key = BuildConfig.GEMINI_API_KEY;
        // 1. Initialize Gemini Manager
        geminiManager = new GeminiManager(key);

        editTextCustomName = findViewById(R.id.editTextCustomName);
        buttonSearch = findViewById(R.id.buttonSearch);
        // 2. Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerViewNames);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 3. Initialize Data (Hebrew Names)
        babyNameList = new ArrayList<>();
        babyNameList.add(new BabyName("דוד"));
        babyNameList.add(new BabyName("נועה"));
        babyNameList.add(new BabyName("אריאל"));
        babyNameList.add(new BabyName("מאיה"));
        babyNameList.add(new BabyName("דניאל"));
        babyNameList.add(new BabyName("תמר"));
        babyNameList.add(new BabyName("בן"));
        babyNameList.add(new BabyName("איתי"));

        // 4. Setup Adapter
        adapter = new BabyNameAdapter(babyNameList, (babyName, position) -> {
            if (babyName.isExpanded()) {
                babyName.setExpanded(false);
                adapter.notifyItemChanged(position);
            } else {
                if (babyName.hasDetails()) {
                    babyName.setExpanded(true);
                    adapter.notifyItemChanged(position);
                } else {
                    fetchNameDetails(babyName, position);
                }
            }
        });

        recyclerView.setAdapter(adapter);


        buttonSearch.setOnClickListener(v -> {
            String newNameStr = editTextCustomName.getText().toString().trim();
            if (!newNameStr.isEmpty()) {
                BabyName newName = new BabyName(newNameStr);
                // Add to the top of the list
                babyNameList.add(0, newName);
                adapter.notifyItemInserted(0);
                recyclerView.scrollToPosition(0);

                // Fetch details immediately
                fetchNameDetails(newName, 0);

                // Clear input
                editTextCustomName.setText("");
            }
        });
    }

    private void fetchNameDetails(BabyName babyName, int position) {
        babyName.setLoading(true);
        adapter.notifyItemChanged(position);

        // Prompt in Hebrew, asking for specific JSON structure
        String prompt = "ספק מידע על השם: " + babyName.getName() +
                ". החזר אובייקט JSON בלבד (ללא מרקדאון) עם המפתחות הבאים:\n" +
                "'meaning' (משמעות השם בקצרה),\n" +
                "'origin' (מקור השם),\n" +
                "'details' (הסבר היסטורי קצר של 1-2 משפטים),\n" +
                "'alternatives' (רשימה של 3 שמות נוספים עם משמעות דומה, מופרדים בפסיקים).\n" +
                "התשובה חייבת להיות בעברית מלאה.";

        geminiManager.sendText(prompt, response -> {
            if (response.startsWith("Error")) {
                runOnUiThread(() -> {
                    babyName.setLoading(false);
                    Toast.makeText(this, "שגיאה בקבלת נתונים" + response, Toast.LENGTH_SHORT).show();
                    Log.d("ERROR GEMINI", "fetchNameDetails: " + response);
                    adapter.notifyItemChanged(position);
                });
                return;
            }

            String contentText = parseGeminiResponse(response);

            try {
                // Cleanup JSON string
                contentText = contentText.replace("```json", "").replace("```", "").trim();

                JSONObject json = new JSONObject(contentText);
                babyName.setMeaning(json.optString("meaning", "לא ידוע"));
                babyName.setOrigin(json.optString("origin", "לא ידוע"));
                babyName.setDetails(json.optString("details", "אין פרטים נוספים."));
                babyName.setAlternatives(json.optString("alternatives", ""));
            } catch (Exception e) {
                // Fallback
                babyName.setMeaning("לחץ לפרטים");
                babyName.setOrigin("לא ידוע");
                babyName.setDetails(contentText);
            }

            runOnUiThread(() -> {
                babyName.setLoading(false);
                babyName.setExpanded(true);
                adapter.notifyItemChanged(position);
            });
        });
    }

    private String parseGeminiResponse(String rawJson) {
        try {
            JSONObject responseJson = new JSONObject(rawJson);
            JSONArray candidates = responseJson.getJSONArray("candidates");
            JSONObject firstCandidate = candidates.getJSONObject(0);
            JSONObject content = firstCandidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            return parts.getJSONObject(0).getString("text");
        } catch (Exception e) {
            return "Error parsing response";
        }
    }
}