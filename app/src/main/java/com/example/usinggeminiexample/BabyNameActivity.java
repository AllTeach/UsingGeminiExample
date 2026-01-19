package com.example.usinggeminiexample;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class BabyNameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Ensure a RecyclerView is defined in the layout.

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<BabyName> babyNames = new ArrayList<>();
        babyNames.add(new BabyName("\u05D3\u05D5\u05D3", "\u05D0\u05D4\u05D5\u05D1, \u05D9\u05D3\u05D9\u05DD", "\u05E2\u05D1\u05D2\u05D9", "\u05DE\u05DC\u05D2 \u05DE\u05E9\u05D5\u05E9\u05DC \u05D7\u05DC\u05D5\u05DE\u05D5\u05EA\u05D9\u05EA");
        babyNames.add(new BabyName("\u05E0\u05E2\u05DE\u05D9", "\u05E0\u05E2\u05D9\u05DE", "\u05E2\u05D1\u05D2\u05D9", "");
    }
}