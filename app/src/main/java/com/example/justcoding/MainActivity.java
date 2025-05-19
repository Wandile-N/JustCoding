package com.example.justcoding;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    String lampServer = "https://lamp.ms.wits.ac.za/home/s2588145/";
    ListView chatListView;
    ArrayList<String> usernames;
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        chatListView = findViewById(R.id.chatListView);

        // Dummy data for now (replace with real usernames from DB)
        usernames = new ArrayList<>();
        usernames.add("Counsellor_John");
        usernames.add("Counsellor_Anna");
        usernames.add("Anonymous_43");
        usernames.add("Therapist_Mike");

        adapter = new ArrayAdapter<>(this, R.layout.chat_list_item, R.id.chatUserName, usernames);
        chatListView.setAdapter(adapter);

        chatListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedUser = usernames.get(position);

            Intent intent = new Intent(MainActivity.this, chatActivity.class);
            intent.putExtra("username", selectedUser); // send this to chat screen
            startActivity(intent);
        });

    }
}