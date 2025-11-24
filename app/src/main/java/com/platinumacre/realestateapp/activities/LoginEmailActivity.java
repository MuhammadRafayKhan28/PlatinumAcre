package com.platinumacre.realestateapp.activities;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.platinumacre.realestateapp.MyUtils;
import com.platinumacre.realestateapp.databinding.ActivityLoginEmailBinding;

public class LoginEmailActivity extends AppCompatActivity {

    //View Binding
    private ActivityLoginEmailBinding binding;

    //Tag to show logs in logcat
    private static final String TAG = "LOGIN_TAG";

    //ProgressDialog to show while sign-in
    private ProgressDialog progressDialog;

    //Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        //activity_login_email.xml = ActivityLoginEmailBinding
        binding = ActivityLoginEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        //get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        //init/setup ProgressDialog to show while sign-in
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //handle noAccountTv click, open RegisterEmailActivity to register user with Email & Password
        binding.noAccountTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginEmailActivity.this, RegisterEmailActivity.class));
            }
        });

        //handle forgotPasswordTv click, open ForgotPasswordActivity to send password recovery instructions to registered email
        binding.forgotPasswordTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginEmailActivity.this, ForgotPasswordActivity.class));
            }
        });

        //handle loginBtn click, start login
        binding.loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });
    }

    private String email, password;

    private void validateData() {
        //input data
        email = binding.emailEt.getText().toString().trim();
        password = binding.passwordEt.getText().toString();

        Log.d(TAG, "validateData: email: " + email);
        Log.d(TAG, "validateData: password: " + password);

        //validate data
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            //email pattern is invalid, show error
            binding.emailEt.setError("Invalid Email");
            binding.emailEt.requestFocus();
        } else if (password.isEmpty()) {
            //password is not entered, show error
            binding.passwordEt.setError("Enter Password");
            binding.passwordEt.requestFocus();
        } else {
            //email pattern is valid and password is enter. start login
            loginUser();
        }

    }


    private void loginUser() {
        //show progress
        progressDialog.setMessage("Logging In");
        progressDialog.show();

        //start user login
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        //User login success
                        Log.d(TAG, "onSuccess: Logged In...");
                        progressDialog.dismiss();

                        //Start MainActivity
                        startActivity(new Intent(LoginEmailActivity.this, MainActivity.class));
                        finishAffinity(); //finish current and all activities from back stack
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //User login failed
                        Log.e(TAG, "onFailure: ", e);
                        MyUtils.toast(LoginEmailActivity.this, "Failed due to " + e.getMessage());
                        progressDialog.dismiss();
                    }
                });
    }
}