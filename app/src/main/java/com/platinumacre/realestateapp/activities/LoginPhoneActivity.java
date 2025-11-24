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
import android.view.View;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.platinumacre.realestateapp.MyUtils;
import com.platinumacre.realestateapp.databinding.ActivityLoginPhoneBinding;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class LoginPhoneActivity extends AppCompatActivity {

    //View Binding
    private ActivityLoginPhoneBinding binding;

    //Tag to show logs in logcat
    private static final String TAG = "LOGIN_PHONE_TAG";

    //ProgressDialog to show while phone login
    private ProgressDialog progressDialog;

    //Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;


    private PhoneAuthProvider.ForceResendingToken forceResendingToken;


    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;


    private String mVerificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        //activity_phone_login.xml = ActivityLoginPhoneBinding
        binding = ActivityLoginPhoneBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        //For the start show phone input UI and hide OTP UI
        binding.phoneInputRl.setVisibility(View.VISIBLE);
        binding.optInputRl.setVisibility(View.GONE);

        //init/setup ProgressDialog to show while login
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        //Firebase Auth for auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        //listen for phone login callbacks. Hint: you may put here instead of creating a function
        phoneLoginCallBack();

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //handle sendOtpBtn send OTP to input phone number
        binding.sendOtpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });


        //handle resendOtpTv click, resend OTP
        binding.resendOtpTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resendVerificationCode(forceResendingToken);
            }
        });


        //handle verifyOtpBtn click, verify OTP received
        binding.verifyOtpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //input OTP
                String otp = binding.otpEt.getText().toString().trim();

                Log.d(TAG, "onClick: OTP: " + otp);
                //validate if otp is entered and length is 6 characters
                if (otp.isEmpty()) {
                    //OTP is empty, show error
                    binding.otpEt.setError("Enter OTP");
                    binding.otpEt.requestFocus();
                } else if (otp.length() < 6) {
                    //OTP length is less then 6, show error
                    binding.otpEt.setError("OTP length must be 6 Characters");
                    binding.otpEt.requestFocus();
                } else {
                    verifyPhoneNumberWithCode(mVerificationId, otp);
                }
            }
        });
    }

    private String phoneCode = "", phoneNumber = "", phoneNumberWithCode = "";

    private void validateData() {
        //input data
        phoneCode = binding.phoneCodeTil.getSelectedCountryCodeWithPlus();
        phoneNumber = binding.phoneNumberEt.getText().toString().trim();
        phoneNumberWithCode = phoneCode + phoneNumber;

        Log.d(TAG, "validateData: phoneCode: " + phoneCode);
        Log.d(TAG, "validateData: phoneNumber: " + phoneNumber);
        Log.d(TAG, "validateData: phoneNumberWithCode: " + phoneNumberWithCode);
        //validate data
        if (phoneNumber.isEmpty()) {
            //phoneNumber is not entered, show error
            MyUtils.toast(this, "Please enter phone number");
        } else {
            startPhoneNumberVerification();
        }
    }

    private void startPhoneNumberVerification() {
        Log.d(TAG, "startPhoneNumberVerification: ");
        //show progress
        progressDialog.setMessage("Sending OTP to " + phoneNumberWithCode);
        progressDialog.show();


        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(firebaseAuth)           //FirebaseAuth instance
                        .setPhoneNumber(phoneNumberWithCode)        //Phone Number with country code e.g. +92*********
                        .setTimeout(60L, TimeUnit.SECONDS)   //Timeout and unit
                        .setActivity(this)                          //Activity (for callback binding)
                        .setCallbacks(mCallbacks)
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void phoneLoginCallBack() {
        Log.d(TAG, "phoneLoginCallBack: ");

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                super.onCodeSent(verificationId, token);
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                forceResendingToken = token;
                //OTP is sent so hide progress for now
                progressDialog.dismiss();
                //OTP is sent so hide phone ui and show otp ui
                binding.phoneInputRl.setVisibility(View.INVISIBLE);
                binding.optInputRl.setVisibility(View.VISIBLE);

                //Show toast for success sending OTP
                MyUtils.toast(LoginPhoneActivity.this, "OTP Sent to " + phoneNumberWithCode);
                //show user a message that Please type the verification code sent to the phone number user has input
                binding.loginLabelTv.setText("Please type the verification code sent to " + phoneNumberWithCode);
            }

            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                Log.d(TAG, "onVerificationCompleted: ");
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.

                signinWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Log.e(TAG, "onVerificationFailed: ", e);
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                progressDialog.dismiss();

                MyUtils.toast(LoginPhoneActivity.this, "" + e.getMessage());
            }
        };
    }

    private void verifyPhoneNumberWithCode(String verificationId, String otp) {
        Log.d(TAG, "verifyPhoneNumberWithCode: verificationId: " + verificationId);
        Log.d(TAG, "verifyPhoneNumberWithCode: otp: " + otp);

        //show progress
        progressDialog.setMessage("Verifying OTP");
        progressDialog.show();
        //PhoneAuthCredential with verification id and OTP to signIn user with signInWithPhoneAuthCredential
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);

        signinWithPhoneAuthCredential(credential);

    }

    private void resendVerificationCode(PhoneAuthProvider.ForceResendingToken token) {
        Log.d(TAG, "resendVerificationCode: ForceResendingToken: " + token);
        //show progress dialog
        progressDialog.setMessage("Resending OTP to " + phoneNumberWithCode);
        progressDialog.show();


        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(firebaseAuth)           //FirebaseAuth instance
                        .setPhoneNumber(phoneNumberWithCode)        //Phone Number with country code e.g. +92*********
                        .setTimeout(60L, TimeUnit.SECONDS)  //Timeout and unit
                        .setActivity(this)                          //Activity (for callback binding)
                        .setCallbacks(mCallbacks)
                        .setForceResendingToken(token)
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void signinWithPhoneAuthCredential(PhoneAuthCredential credential) {
        Log.d(TAG, "signinWithPhoneAuthCredential: ");
        progressDialog.setMessage("Logging In");

        //SignIn in to firebase auth using Phone Credentials
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        Log.d(TAG, "onSuccess: ");
                        //SignIn Success, let's check if the user is new (New Account Register) or existing (Existing Login)
                        if (authResult.getAdditionalUserInfo().isNewUser()) {
                            //New User, Account created. Let's save user info to firebase realtime database
                            Log.d(TAG, "onSuccess: New User, Account created...");

                            updateUserInfoDb();
                        } else {
                            Log.d(TAG, "onSuccess: Existing User, Logged In");
                            //New User, Account created. No need to save user info to firebase realtime database, Start MainActivity
                            startActivity(new Intent(LoginPhoneActivity.this, MainActivity.class));
                            finishAffinity();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //SignIn failed, show exception message
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(LoginPhoneActivity.this, "Failed to login due to " + e.getMessage());
                    }
                });

    }

    private void updateUserInfoDb() {
        Log.d(TAG, "updateUserInfoDb: ");
        progressDialog.setMessage("Saving user info");
        progressDialog.show();

        //Let's save user info to Firebase Realtime database key names should be same as we done in Register User via email and Google
        //get current timestamp e.g. to show user registration date/time
        long timestamp = MyUtils.getTimestamp();
        String registerUserUid = firebaseAuth.getUid();

        //setup data to save in firebase realtime db. most of the data will be empty and will set in edit profile
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid", registerUserUid);
        hashMap.put("email", "");
        hashMap.put("name", "");
        hashMap.put("timestamp", timestamp);
        hashMap.put("phoneCode", "" + phoneCode);
        hashMap.put("phoneNumber", "" + phoneNumber);
        hashMap.put("profileImageUrl", "");
        hashMap.put("dob", "");
        hashMap.put("userType", MyUtils.USER_TYPE_PHONE); //possible values Email/Phone/Google
        hashMap.put("token", "");

        //set data to firebase db
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(registerUserUid)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //User inf save success
                        Log.d(TAG, "onSuccess: User info saved");
                        progressDialog.dismiss();

                        //Start MainActivity
                        startActivity(new Intent(LoginPhoneActivity.this, MainActivity.class));
                        finishAffinity();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //User inf save failed
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(LoginPhoneActivity.this, "Failed to save user info due to " + e.getMessage());
                    }
                });
    }
}