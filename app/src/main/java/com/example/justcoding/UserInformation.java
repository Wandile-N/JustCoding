package com.example.justcoding;

import android.app.Activity;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

public class UserInformation {


    public UserInformation(ArrayList<EditText> arr){

    }



    public String validatePassword(String pass1, String pass2, Activity act){
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$";
        String password = "Pass";
        if (!pass1.equals(pass2)){
            password = "passwords do not match!";
        }
        else if (!pass1.matches(passwordPattern)) {
            password = "Weak password! Use 8+ characters, uppercase, lowercase, digit & symbol";
        }else {
            password = pass1;
        }
        return password;
    }
}
