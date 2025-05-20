package com.example.justcoding;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

public class chatActivity extends AppCompatActivity {
    private String username, room;
    private EditText mInputMessageView;
    private String chatServer =  "wss://chat-server-twjr.onrender.com";
    private Socket mSocket;


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

        username = getIntent().getStringExtra("username");
        room = getIntent().getStringExtra("room");

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
        } catch (JSONException e) {
            Log.d("Socket", "Sending message: " + message);
            e.printStackTrace();
        }
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

}