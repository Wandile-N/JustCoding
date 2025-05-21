package com.example.justcoding;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseBooleanArray;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public class problemSelectionActivity extends AppCompatActivity {
    PHPRequest req;
    String lampServer = "https://lamp.ms.wits.ac.za/home/s2588145/";
    ListView problemListView;
    Button submitBtn;
    String[] problems = {"Depression", "Anxiety", "Academic Stress", "Family Issues", "Addiction", "Career Advice"};
    String selectionMode;  // "single" or "multiple"
    String currentUser;    // assume passed via intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_problem_selection);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.problemSelection), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        problemListView = findViewById(R.id.problemListView);
        submitBtn = findViewById(R.id.submitProblemsBtn);

        // Get mode and user from intent
        selectionMode = getIntent().getStringExtra("selection_mode");
        currentUser = getIntent().getStringExtra("username");

        // Set list mode
        if ("single".equals(selectionMode)) {
            problemListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        } else {
            problemListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_multiple_choice, problems);
        problemListView.setAdapter(adapter);

        submitBtn.setOnClickListener(v -> {
            ArrayList<String> selectedProblems = new ArrayList<>();

            if ("single".equals(selectionMode)) {
                int checkedPos = problemListView.getCheckedItemPosition();
                if (checkedPos == ListView.INVALID_POSITION) {
                    Toast.makeText(this, "Please select a problem.", Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedProblems.add(problems[checkedPos]);
            } else {
                SparseBooleanArray checked = problemListView.getCheckedItemPositions();
                for (int i = 0; i < problems.length; i++) {
                    if (checked.get(i)) {
                        selectedProblems.add(problems[i]);
                    }
                }

                if (selectedProblems.size() == 0) {
                    Toast.makeText(this, "Please select at least one problem.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            sendProblemsToServer(selectedProblems);
        });
    }

    private void sendProblemsToServer(ArrayList<String> selectedProblems) {
        StringBuilder builder = new StringBuilder();
        for (String p : selectedProblems) {
            builder.append(p).append(",");
        }
        String problemsCSV = builder.toString().replaceAll(",$", "");

        req = new PHPRequest(lampServer);
        ContentValues cv = new ContentValues();
        cv.put("problems", problemsCSV);
        cv.put("username", currentUser); // make sure this is passed from login/registration

        req.doRequest(problemSelectionActivity.this, "problemUpdate", cv, new RequestHandler() {
            @Override
            public void processResponse(String response) {
                Toast.makeText(problemSelectionActivity.this, "Problems saved. Now matching...", Toast.LENGTH_SHORT).show();

                // After problem is saved, attempt to match if role is "user"
                String role = getIntent().getStringExtra("role"); // make sure this was passed too!
                if ("user".equals(role)) {
                    ContentValues matchCv = new ContentValues();
                    matchCv.put("username", currentUser);
                    matchCv.put("problem", selectedProblems.get(0)); // Assuming only one problem for user

                    req.doRequest(problemSelectionActivity.this, "match", matchCv, new RequestHandler() {
                        @Override
                        public void processResponse(String matchResponse) {
                            runOnUiThread(() -> {
                                try {
                                    JSONObject obj = new JSONObject(matchResponse);
                                    String status = obj.getString("status");

                                    if ("matched".equals(status) || "already_matched".equals(status)) {
                                        String counsellor = obj.getString("counsellor");
                                        Toast.makeText(problemSelectionActivity.this, "Matched with " + counsellor, Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(problemSelectionActivity.this, "No counsellor available at the moment.", Toast.LENGTH_LONG).show();
                                    }
                                } catch (Exception e) {
                                    Toast.makeText(problemSelectionActivity.this, "Error processing match response.", Toast.LENGTH_LONG).show();
                                }

                                // I NEED TO PASS THE account_id !!!!
                                Intent i = new Intent(problemSelectionActivity.this, MainActivity.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(i);
                                finish();
                            });
                        }
                        @Override
                        public Map<String, String> getParams() {
                            return Collections.emptyMap();
                        }
                    });

                } else {
                    // If not a user (i.e., counsellor), just go to main menu
                    Intent i = new Intent(problemSelectionActivity.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                }
            }
            @Override
            public Map<String, String> getParams() {
                return Collections.emptyMap();
            }
        });
    }
}
