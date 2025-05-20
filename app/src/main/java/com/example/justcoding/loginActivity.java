package com.example.justcoding;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class loginActivity extends AppCompatActivity {
    PHPRequest req;
    EditText username, password;
    Button btnLogin;
    String lampServer = "https://lamp.ms.wits.ac.za/home/s2588145/";
    String user, pword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        req = new PHPRequest(lampServer);
        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);
        btnLogin = (Button) findViewById(R.id.btnLogin);
    }

    public void log(View view) {
        req = new PHPRequest(lampServer);
        ContentValues cv = new ContentValues();
        user = username.getText().toString();
        pword = password.getText().toString();

        // 🔒 Prevent spamming the login button
        btnLogin.setEnabled(false);

        cv.put("username", user);
        req.doRequest(loginActivity.this, "login", cv, new RequestHandler() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void processResponse(String response) {
                String log = logIn(response);
                int userID = getUserID(response);

                // ✅ Re-enable the button no matter what
                btnLogin.setEnabled(true);

                if(log.equals("Login successful!")){
                    Intent intent = new Intent(loginActivity.this, MainActivity.class);
                    intent.putExtra("userID", userID);
                    intent.putExtra("username", user);
                    Toast.makeText(loginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    // Don't forget to putExtra (all the account details, except password and salt)
                    startActivity(intent);
                    password.setText("");
                    finish();
                }else{
                    password.setError(log);
                    Toast.makeText(loginActivity.this, log, Toast.LENGTH_LONG).show();
                }

            }
            @Override
            public Map<String, String> getParams() {
                return Collections.emptyMap();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String logIn(String json)  {
        String login = "Login successful!";
        JSONArray ja = null;
        try {
            ja = new JSONArray(json);
            JSONObject obj = ja.getJSONObject(0);
            if(obj.has("error")){
                String errorMsg = obj.getString("error");
                login = errorMsg;
            }else{
                String salt = obj.getString("salt");
                String user_hashed_password = hashPassword(pword, salt);
                String db_hashed_password = obj.getString("password");
                if(!db_hashed_password.equals(user_hashed_password)){
                    login = "Incorrect password!";
                }
            }
        } catch (Exception e) {
            Toast.makeText(loginActivity.this, "Login failed. Please check your internet or try again later.", Toast.LENGTH_LONG).show();
        }
        return login;
    }

    public void signUp(View view) {
        Intent i = new Intent(loginActivity.this, signUpActivity.class);
        startActivity(i);
    }

    public int getUserID(String json){
        try {
            JSONArray ja = new JSONArray(json);
            if (ja.length() > 0) {
                JSONObject obj = ja.getJSONObject(0);
                if (obj.has("account_id")) {
                    return obj.getInt("account_id");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return -1;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public String hashPassword(String password, String salt) throws Exception {
        byte[] saltBytes = Base64.getDecoder().decode(salt);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, 65536, 256); // iterations, key length
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = factory.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }

}