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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.platinumacre.realestateapp.MyUtils;
import com.platinumacre.realestateapp.databinding.ActivityRegisterEmailBinding;

import java.util.HashMap;

public class RegisterEmailActivity extends AppCompatActivity {

    //View Binding
    private ActivityRegisterEmailBinding binding;

    //Tag to show logs in logcat
    private static final String TAG = "REGISTER_TAG";

    //Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;

    //ProgressDialog to show while sign-up
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        //activity_register_email.xml = ActivityRegisterEmailBinding
        binding = ActivityRegisterEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        //get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        //init/setup ProgressDialog to show while sign-up
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //handle haveAccountTv click, go-back-to LoginEmailActivity
        binding.haveAccountTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //handle registerBtn click, start user registeration
        binding.registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });

    }

    private String email, password, cPassword;

    private void validateData() {
        //input data
        email = binding.emailEt.getText().toString().trim();
        password = binding.passwordEt.getText().toString();
        cPassword = binding.cPasswordEt.getText().toString();

        Log.d(TAG, "validateData: email: " + email);
        Log.d(TAG, "validateData: password: " + password);
        Log.d(TAG, "validateData: cPassword: " + cPassword);

        //validate data
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            //email pattern is invalid, show error
            binding.emailEt.setError("Invalid Email Pattern");
            binding.emailEt.requestFocus();
        } else if (password.isEmpty()) {
            //password is not entered, show error
            binding.passwordEt.setError("Enter Password");
            binding.passwordEt.requestFocus();
        } else if (!password.equals(cPassword)) {
            //password and confirm password is not same, show error
            binding.cPasswordEt.setError("Password doesn't match");
            binding.cPasswordEt.requestFocus();
        } else {
            //all data is valid, start sign-up
            registerUser();
        }
    }

    private void registerUser() {
        //show progress
        progressDialog.setMessage("Creating Account");
        progressDialog.show();

        //start user sign-up
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        //User Register success, We also need to save user info to firebase db
                        Log.d(TAG, "onSuccess: Register Success");
                        updateUserInfo();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //User Register failed
                        Log.e(TAG, "onFailure: ", e);
                        MyUtils.toast(RegisterEmailActivity.this, "Failed due to " + e.getMessage());
                        progressDialog.dismiss();
                    }
                });
    }

    private void updateUserInfo() {
        //change progress dialog message
        progressDialog.setMessage("Saving User Info");

        //get current timestamp e.g. to show user registration date/time
        long timestamp = MyUtils.getTimestamp();
        String registerUserEmail = firebaseAuth.getCurrentUser().getEmail(); //get email of registered user
        String registerUserUid = firebaseAuth.getUid(); //get uid of registered user

        //setup data to save in firebase realtime db. most of the data will be empty and will set in edit profile
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid", registerUserUid);
        hashMap.put("email", registerUserEmail);
        hashMap.put("name", "");
        hashMap.put("timestamp", timestamp);
        hashMap.put("phoneCode", "");
        hashMap.put("phoneNumber", "");
        hashMap.put("profileImageUrl", "");
        hashMap.put("dob", "");
        hashMap.put("userType", MyUtils.USER_TYPE_EMAIL); //possible values Email/Phone/Google
        hashMap.put("token", "");

        //set data to firebase db
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(registerUserUid)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //Firebase db save success
                        Log.d(TAG, "onSuccess: Info saved...");
                        progressDialog.dismiss();

                        startActivity(new Intent(RegisterEmailActivity.this, MainActivity.class));
                        finishAffinity();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Firebase db save failed
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(RegisterEmailActivity.this, "Failed to save info due to " + e.getMessage());
                    }
                });

    }

}