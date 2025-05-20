package com.example.justcoding;

import android.content.ContentValues;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import io.socket.client.IO;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

public class chatActivity extends AppCompatActivity {
    PHPRequest req;
    private String lampServer = "https://lamp.ms.wits.ac.za/home/s2588145/";
    private String chatServer =  "wss://chat-server-twjr.onrender.com";
    private String username, room;
    private EditText mInputMessageView;
    private Socket mSocket;
    int senderID, receiverID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chat), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        {
            try {
                mSocket = IO.socket(chatServer);
            } catch (URISyntaxException e){
                Log.d("Socket error", e.toString());
            }
        }
        mSocket.connect();
        req = new PHPRequest(lampServer);

        username = getIntent().getStringExtra("username");
        room = getIntent().getStringExtra("room");
        senderID = getIntent().getIntExtra("senderID",  -1);
        receiverID = getIntent().getIntExtra("receiverID", -1);
        Log.d("ChatActivity", "Sender ID: " + senderID + ", Receiver ID: " + receiverID);

        loadChatHistory();

        mInputMessageView = findViewById(R.id.messageInput);

        mSocket.on("message", onNewMessage);
        mSocket.emit("join", createJoinObject(username, room));
    }

    private void addMessage(String message, boolean isUser) {
        LinearLayout chatLayout = findViewById(R.id.chatMessagesLayout);
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextSize(16);
        textView.setPadding(10, 10, 10, 10);
        textView.setBackgroundResource(isUser ? R.drawable.user_bubble : R.drawable.counsellor_bubble);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(10, 10, 10, 10);
        params.gravity = isUser ? Gravity.END : Gravity.START;

        textView.setLayoutParams(params);
        chatLayout.addView(textView);

        // Scroll to bottom
        ScrollView scrollView = findViewById(R.id.chatScrollView);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private JSONObject createJoinObject(String username, String room) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("username", username);
            obj.put("room", room);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    private void attemptSend(){
        String message = mInputMessageView.getText().toString().trim();
        if (TextUtils.isEmpty(message)){
            return;
        }

        mInputMessageView.setText("");
        mInputMessageView.setHint("Type a message ...");

        JSONObject msg = new JSONObject();
        try {
            msg.put("username", username);
            msg.put("message", message);
            msg.put("room", room);
            mSocket.emit("new message", msg);

            sendMessage(message);
        } catch (JSONException e) {
            Log.d("Socket", "Sending message: " + message);
            e.printStackTrace();
        }
    }

    public void sendMessage(String message){
        ContentValues cv = new ContentValues();
        cv.put("sender_id", senderID);
        cv.put("receiver_id", receiverID);
        cv.put("content", message);
        req.doRequest(chatActivity.this, "sendMessage", cv, new RequestHandler() {
            @Override
            public void processResponse(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    String status = obj.getString("status");
                    if (status.equals("success")){
                        Log.d("Storing in database: ", "message sent");
                    }else{
                        Log.d("Message not stored: ", obj.getString("msg"));
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
            @Override
            public Map<String, String> getParams() {
                return Collections.emptyMap();
            }
        });

    }

    private Emitter.Listener onNewMessage = new Emitter.Listener(){
        @Override
        public void call(final Object... args){
            chatActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username = "";
                    String message = "";

                    try {
                        username = data.getString("username");
                        message = data.getString("message");
                    }catch (JSONException e){
                        Log.d("JSON Error on emitter: ", e.toString());
                    }

                    addMessage(message, username.equals(chatActivity.this.username));
                }
            });
        }
    };

    public void getMessages(){
        ContentValues cv = new ContentValues();
        req.doRequest(chatActivity.this, "getMessages", cv, new RequestHandler() {
            @Override
            public void processResponse(String response) {

            }

            @Override
            public Map<String, String> getParams() {
                return Collections.emptyMap();
            }
        });
    }

    private void loadChatHistory() {
        ContentValues cv = new ContentValues();
        cv.put("sender_id", senderID);
        cv.put("receiver_id", receiverID);

        req.doRequest(chatActivity.this, "getMessages", cv, new RequestHandler() {
            @Override
            public void processResponse(String response) {
                try {
                    JSONArray messages = new JSONArray(response);
                    for (int i = 0; i < messages.length(); i++) {
                        JSONObject messageObj = messages.getJSONObject(i);
                        String content = messageObj.getString("content");
                        int sender = messageObj.getInt("sender_id");

                        // Determine if this is the current user's message
                        boolean isUser = (sender == senderID);
                        addMessage(content, isUser);
                    }
                } catch (JSONException e) {
                    Log.d("JSON error", "Error parsing chat history: " + e.toString());
                }
            }

            @Override
            public Map<String, String> getParams() {
                return Collections.emptyMap();
            }
        });
    }


    public void send(View view) {
        attemptSend();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (room != null) {
            mSocket.off("message", onNewMessage);
        }
        mSocket.disconnect();
    }

    // Just to prevent sleep glitches and always synchronise the UI
    // with the latest text
    private void ensureSocketConnected() {
        if (!mSocket.connected()) {
            mSocket.connect();
            mSocket.emit("join", createJoinObject(username, room));
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        ensureSocketConnected();
        loadChatHistory();
    }






}