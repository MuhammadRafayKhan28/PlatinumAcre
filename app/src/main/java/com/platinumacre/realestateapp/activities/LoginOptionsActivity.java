package com.platinumacre.realestateapp.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.platinumacre.realestateapp.MyUtils;
import com.platinumacre.realestateapp.R;
import com.platinumacre.realestateapp.databinding.ActivityLoginOptionsBinding;

import java.util.HashMap;

public class LoginOptionsActivity extends AppCompatActivity {

    //View Binding
    private ActivityLoginOptionsBinding binding;

    //Tag to show logs in logcat
    private static final String TAG = "MAIN_TAG";

    //ProgressDialog to show while google sign in
    private ProgressDialog progressDialog;
    //Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;

    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        //activity_login_options.xml = ActivityLoginOptionsBinding
        binding = ActivityLoginOptionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        //init/setup ProgressDialog to show while sign-up
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        //Firebase Auth for auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        //handle closeBtn click, go-back
        binding.skipBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //handle loginEmailBtn click, open LoginEmailActivity to login with Email & Password
        binding.loginEmailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginOptionsActivity.this, LoginEmailActivity.class));
            }
        });

        binding.loginPhoneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginOptionsActivity.this, LoginPhoneActivity.class));
            }
        });

        //handle loginGoogleBtn click, begin google signin
        binding.loginGoogleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                beginGoogleLogin();
            }
        });

    }

    private void beginGoogleLogin() {
        Log.d(TAG, "beginGoogleLogin: ");

        Intent googleSignInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInnARL.launch(googleSignInIntent);
    }

    private ActivityResultLauncher<Intent> googleSignInnARL = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(TAG, "onActivityResult: ");

                    if (result.getResultCode() == Activity.RESULT_OK) {

                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

                        try {
                            // Google Sign In was successful, authenticate with Firebase
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            Log.d(TAG, "onActivityResult: Account ID: " + account.getId());
                            firebaseAuthWithGoogleAccount(account.getIdToken());
                        } catch (ApiException e) { // Google Sign In failed, update UI appropriately
                            Log.e(TAG, "onActivityResult: ", e);
                        }
                    } else {
                        //Cancelled from google signIn options/confirmation dialog
                        Log.d(TAG, "onActivityResult: Cancelled");
                        MyUtils.toast(LoginOptionsActivity.this, "Cancelled...");
                    }
                }
            }
    );

    private void firebaseAuthWithGoogleAccount(String idToken) {
        Log.d(TAG, "firebaseAuthWithGoogleAccount: idToken: " + idToken);

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        //SignIn in to firebase auth using Google Credentials
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        //SignIn Success, let's check if the user is new (New Account Register) or existing (Existing Login)
                        if (authResult.getAdditionalUserInfo().isNewUser()) {
                            Log.d(TAG, "onSuccess: New User, Account created...");
                            //New User, Account created. Let's save user info to firebase realtime database
                            updateUserInfoDb();
                        } else {
                            Log.d(TAG, "onSuccess: Existing User, Logged In...");
                            //New User, Account created. No need to save user info to firebase realtime database, Start MainActivity
                            startActivity(new Intent(LoginOptionsActivity.this, MainActivity.class));
                            finishAffinity();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: ", e);
                    }
                });
    }

    private void updateUserInfoDb() {
        Log.d(TAG, "updateUserInfoDb: ");
        //set message and show progress dialog
        progressDialog.setMessage("Saving User Info");
        progressDialog.show();

        //get current timestamp e.g. to show user registration date/time
        long timestamp = MyUtils.getTimestamp();
        String registerUserUid = firebaseAuth.getUid(); //get uid of registered user
        String registerUserEmail = firebaseAuth.getCurrentUser().getEmail(); //get email of registered user
        String name = firebaseAuth.getCurrentUser().getDisplayName(); //Since each Google user has name so we can get it to save in firebase db

        //setup data to save in firebase realtime db. most of the data will be empty and will set in edit profile
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid", registerUserUid);
        hashMap.put("email", registerUserEmail);
        hashMap.put("name", "" + name);
        hashMap.put("timestamp", timestamp);
        hashMap.put("phoneCode", "");
        hashMap.put("phoneNumber", "");
        hashMap.put("profileImageUrl", "");
        hashMap.put("dob", "");
        hashMap.put("userType", MyUtils.USER_TYPE_GOOGLE); //possible values Email/Phone/Google
        hashMap.put("token", "");

        //set data to firebase db
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(registerUserUid)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //Firebase db save success
                        Log.d(TAG, "onSuccess: User info saved...");
                        progressDialog.dismiss();

                        //start MainActivity
                        startActivity(new Intent(LoginOptionsActivity.this, MainActivity.class));
                        finishAffinity();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Firebase db save failed
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(LoginOptionsActivity.this, "Failed to save user info due to " + e.getMessage());
                    }
                });
    }
}