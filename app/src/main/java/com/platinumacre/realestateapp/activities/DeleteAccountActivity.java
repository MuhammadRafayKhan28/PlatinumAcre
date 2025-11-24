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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.platinumacre.realestateapp.MyUtils;
import com.platinumacre.realestateapp.R;
import com.platinumacre.realestateapp.databinding.ActivityDeleteAccountBinding;

public class DeleteAccountActivity extends AppCompatActivity {

    //View Binding
    private ActivityDeleteAccountBinding binding;
    //TAG for logs in logcat
    private static final String TAG = "DELETE_ACCOUNT_TAG";
    //ProgressDialog to show while deleting account
    private ProgressDialog progressDialog;
    //FirebaseAuth for auth related tasks
    private FirebaseAuth firebaseAuth;
    //FirebaseUser to get current user and delete
    private FirebaseUser firebaseUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityDeleteAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        //init/setup ProgressDialog to show while deleting account
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        //get instance of FirebaseAuth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();
        //get instance of FirebaseUser to get current user and delete
        firebaseUser = firebaseAuth.getCurrentUser();

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //handle submitBtn click, start account deletion
        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteUserData();
            }
        });

    }

    private void deleteUserData(){
        Log.d(TAG, "deleteUserData: Deleting user data...");

        progressDialog.setMessage("Deleting user data");
        progressDialog.show();
        //1) Delete User Data: Users > UserId
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid())
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //User data delete success
                        Log.d(TAG, "onSuccess: User data deleted....");
                        //start deleting user properties
                        deleteUserProperties();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //User data delete failed
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(DeleteAccountActivity.this, "Failed to delete user data due to " + e.getMessage());
                    }
                });
    }

    private void deleteUserProperties(){
        // Log message for debugging
        Log.d(TAG, "deleteUserProperties: Deleting user properties...");
        // Set progress dialog message and show it
        progressDialog.setMessage("Deleting user properties");
        progressDialog.show();
        // Database reference to "Properties"
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
        // Query to get properties of the current user
        ref.orderByChild("uid").equalTo(firebaseAuth.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Check if the user has any properties
                        if (!snapshot.exists()){
                            // Log that no properties found
                            Log.d(TAG, "onDataChange: No properties by this user");
                            // If no properties, no need to proceed further, start deleting user account from firebase auth
                            deleteAccount();
                            return;
                        }
                        // Get the total count of properties to be deleted
                        final  int total = (int) snapshot.getChildrenCount();
                        // Array to keep track of deleted properties count
                        final int[] deletedCount = {0};

                        for (DataSnapshot ds: snapshot.getChildren()){
                            //Let's start deleting user properties one by one
                            ds.getRef().removeValue()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            // Property deleted successfully
                                            deletedCount[0]++;
                                            Log.d(TAG, "onSuccess: Property deleted: " + deletedCount[0] + "/" + total);
                                            if (deletedCount[0] == total){
                                                //All user properties deleted, start deleting user account
                                                Log.d(TAG, "onSuccess: All user properties deleted");
                                                deleteAccount();
                                            }
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            //Property delete failed
                                            Log.e(TAG, "onFailure: Failed to delete property ", e);
                                        }
                                    });
                        }


                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void deleteAccount(){
        Log.d(TAG, "deleteAccount: Deleting user account...");

        progressDialog.setMessage("Deleting user account");
        progressDialog.show();

        //3) Delete user account from Firebase Auth
        firebaseUser.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //User account deleted successfully
                        Log.d(TAG, "onSuccess: Deleted user account");
                        progressDialog.dismiss();
                        startMainActivity();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //User account delete failed
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(DeleteAccountActivity.this, "Failed to delete account due to " + e.getMessage());
                        startMainActivity();
                    }
                });
    }


    private void startMainActivity(){

        firebaseAuth.signOut();

        startActivity(new Intent(this, MainActivity.class));
        finishAffinity();
    }

}