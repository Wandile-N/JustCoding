package com.example.justcoding;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    PHPRequest req;
    String lampServer = "https://lamp.ms.wits.ac.za/home/s2588145/";
    String socketUrl = "wss://chat-server-twjr.onrender.com";

    ListView chatListView;
    ArrayList<String> usernames;
    ArrayAdapter<String> adapter;
    int userId, receiverID;
    String username;
    String room = "room_"+ String.valueOf(userId);

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
        usernames = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, R.layout.chat_list_item, R.id.chatUserName, usernames);
        chatListView.setAdapter(adapter);

        // variables needed from the previous activity:
        userId = getIntent().getIntExtra("userID", -1);
        username = getIntent().getStringExtra("username");
        if (userId == -1) {
            Toast.makeText(MainActivity.this, "User ID missing!", Toast.LENGTH_LONG).show();
            finish(); // End the activity, or redirect to login
            return;
        }
        // Fetch match from PHP server
        fetchMatch();
    }

    private void fetchMatch() {
        req = new PHPRequest(lampServer);
        ContentValues cv = new ContentValues();
        cv.put("username", username);
        req.doRequest(MainActivity.this, "getMatch", cv, new RequestHandler() {
            @Override
            public void processResponse(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    boolean matched = json.getBoolean("match");
                    if (matched){
                        String username = json.getString("connected_with");
                        receiverID = json.getInt("connected_with_id");
                        usernames.add(username);
                        adapter.notifyDataSetChanged();

                        chatListView.setOnItemClickListener((parent, view, position, id) -> {
                            String selectedUser = usernames.get(position);
                            Intent intent = new Intent(MainActivity.this, chatActivity.class);
                            intent.putExtra("username", selectedUser);
                            intent.putExtra("room", room);
                            intent.putExtra("senderID", userId);
                            intent.putExtra("receiverID", receiverID);
                            startActivity(intent);
                        });
                    }else {
                        Toast.makeText(MainActivity.this, "No match found yet!", Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    Toast.makeText(MainActivity.this, "JSON Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("MATCH_ERR", e.toString());
                }
            }
            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                return params;
            }
        });
    }
}
