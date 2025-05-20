package com.example.justcoding;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class signUpActivity extends AppCompatActivity {
    PHPRequest req;
    String lampServer = "https://lamp.ms.wits.ac.za/home/s2588145/";
    EditText username, password, confirmPassword;
    RadioGroup roleGroup;
    int selectedId ;
    RadioButton selectedRadio;
    String salt;

    ArrayList<EditText> student;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signup), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        student = new ArrayList<EditText>();

        roleGroup = findViewById(R.id.roleGroup);
        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.pass);
        confirmPassword = (EditText) findViewById(R.id.confirmPass);
    }

    public boolean validateInput(EditText field) {
        if (field.getText().toString().trim().isEmpty()) {
            field.setError("This field is required!");
            return false;
        }
        return true;
    }


    public String checkUser(String response){
        if (response.equals("exists")){
            return "exists";
        }
        return "register";
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16]; // 128-bit salt
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt); // store or send as string
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String hashPassword(String password, String salt) throws Exception {
        byte[] saltBytes = Base64.getDecoder().decode(salt);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, 65536, 256); // iterations, key length
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = factory.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void register(View view) throws Exception {
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$";
        selectedId = roleGroup.getCheckedRadioButtonId();
        selectedRadio = findViewById(selectedId);

        if (!validateInput(username) || !validateInput(password) || !validateInput(confirmPassword)) {
            return;
        }else if (!(password.getText().toString()).matches(passwordPattern)) {
            password.setError("Weak password! Use 8+ characters, uppercase, lowercase, digit & symbol");
        }else if(!password.getText().toString().equals(confirmPassword.getText().toString())){
            confirmPassword.setError("Passwords do not match!");
        }else if (selectedId == -1){
            Toast.makeText(signUpActivity.this, "Please choose between 'user' or 'counselor'.", Toast.LENGTH_SHORT).show();
        }else{
            String user = username.getText().toString();
            String pass = password.getText().toString();
            String role = selectedRadio.getText().toString().toLowerCase(); // "user" or "counsellor"

            req = new PHPRequest(lampServer);
            salt = generateSalt();
            String password = hashPassword(pass, salt);
            ContentValues cv = new ContentValues();
            cv.put("username",user);
            cv.put("password", password);
            cv.put("salt", salt);
            cv.put("role", role);

            final String[] existence = {""};
            req.doRequest(signUpActivity.this, "checkUser", cv, new RequestHandler() {
                @Override
                public void processResponse(String response) {
                    if (checkUser(response).equals("exists")) {
                        username.setError("Username already exists!");
                    } else {
                        req.doRequest(signUpActivity.this, "register", cv, new RequestHandler() {
                            @Override
                            public void processResponse(String response) {
                                if ((response.trim()).equals("Signup failed!")){
                                    Toast.makeText(signUpActivity.this, response, Toast.LENGTH_LONG).show();
                                } else {
                                    if(role.equals("counsellor")){
                                        Intent i = new Intent(signUpActivity.this, problemSelectionActivity.class);
                                        i.putExtra("selection_mode", "multiple");
                                        i.putExtra("username", username.getText().toString());
                                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(i);

                                    }else{
                                        Intent intent = new Intent(signUpActivity.this, problemSelectionActivity.class);
                                        intent.putExtra("username", user); // from login/registration
                                        intent.putExtra("selection_mode", "single");
                                        intent.putExtra("role", role); // "user" or "counsellor"
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                    }

                                }
                            }
                            @Override
                            public Map<String, String> getParams() {
                                return Collections.emptyMap();
                            }
                        });
                    }
                }
                @Override
                public Map<String, String> getParams() {
                    return Collections.emptyMap();
                }
            });
        }
    }
}