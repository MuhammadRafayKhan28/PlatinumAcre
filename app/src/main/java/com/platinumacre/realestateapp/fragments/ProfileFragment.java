package com.platinumacre.realestateapp.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.platinumacre.realestateapp.MyUtils;
import com.platinumacre.realestateapp.R;
import com.platinumacre.realestateapp.activities.ChangePasswordActivity;
import com.platinumacre.realestateapp.activities.DeleteAccountActivity;
import com.platinumacre.realestateapp.activities.MainActivity;
import com.platinumacre.realestateapp.activities.MyPropertyListActivity;
import com.platinumacre.realestateapp.activities.PropertyAddActivity;
import com.platinumacre.realestateapp.activities.ProfileEditActivity;
import com.platinumacre.realestateapp.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {

    //View Binding
    private FragmentProfileBinding binding;

    //TAG for logs in logcat
    private static final String TAG = "PROFILE_TAG";

    //Context for this fragment class
    private Context mContext;

    //Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;


    //ProgressDialog to show while verify account
    private ProgressDialog progressDialog;

    @Override
    public void onAttach(@NonNull Context context) {
        //get and init the context for this fragment class
        mContext = context;
        super.onAttach(context);
    }

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate/bind the layout for this fragment
        binding = FragmentProfileBinding.inflate(LayoutInflater.from(mContext), container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //init/setup ProgressDialog to show while account verification
        progressDialog = new ProgressDialog(mContext);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        loadMyInfo();

        //handle postAdBtn click, start PostAdActivity
        binding.postAdBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(mContext, PropertyAddActivity.class));
            }
        });
        //handle logoutBtn click, logout user and start MainActivity
        binding.logoutCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //logout user
                firebaseAuth.signOut();
                //start MainActivity
                startActivity(new Intent(mContext, MainActivity.class));
                getActivity().finishAffinity();
            }
        });

        //handle myAdsCv click, start MyAdsListActivity
        binding.myAdsCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, MyPropertyListActivity.class));
            }
        });

        //handle editProfileCv click, start ProfileEditActivity
        binding.editProfileCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, ProfileEditActivity.class));
            }
        });

        //handle changePasswordCv click, start ChangePasswordActivity
        binding.changePasswordCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, ChangePasswordActivity.class));
            }
        });

        //handle verifyAccountCv click, start user verification
        binding.verifyAccountCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyAccount();
            }
        });

        //handle deleteAccountCv click, start DeleteAccountActivity
        binding.deleteAccountCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //start DeleteAccountActivity
                startActivity(new Intent(mContext, DeleteAccountActivity.class));
                getActivity().finishAffinity(); //remove all activities from back-stack because we will delete user and its data, so it may produce null exceptions if we don't
            }
        });
    }

    private void loadMyInfo() {
        //Reference of current user info in Firebase Realtime Database to get user info
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child("" + firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get user info, spellings should be same as in firebase realtime database
                        String dob = "" + snapshot.child("dob").getValue();
                        String email = "" + snapshot.child("email").getValue();
                        String name = "" + snapshot.child("name").getValue();
                        String phoneCode = "" + snapshot.child("phoneCode").getValue();
                        String phoneNumber = "" + snapshot.child("phoneNumber").getValue();
                        String profileImageUrl = "" + snapshot.child("profileImageUrl").getValue();
                        String timestamp = "" + snapshot.child("timestamp").getValue();
                        String userType = "" + snapshot.child("userType").getValue();

                        //concatenate phone code and phone number to make full phone number
                        String phone = phoneCode + phoneNumber;

                        //to avoid null or format exceptions
                        if (timestamp.equals("null")) {
                            timestamp = "0";
                        }

                        //format timestamp to dd/MM/yyyy
                        String formattedDate = MyUtils.formatTimestampDate(Long.parseLong(timestamp));

                        //set data to UI
                        binding.emailTv.setText(email);
                        binding.fullNameTv.setText(name);
                        binding.dobTv.setText(dob);
                        binding.phoneTv.setText(phone);
                        binding.memberSinceTv.setText(formattedDate);

                        //check user type i.e. Email/Phone/Google In case of Phone & Google account is already verified but in case of Email account user have to verify
                        if (userType.equals("Email")) {
                            //userType is Email, have to check if verified or not
                            boolean isVerified = firebaseAuth.getCurrentUser().isEmailVerified();
                            if (isVerified) {
                                //Verified, hide the Verify Account option
                                binding.verifyAccountCv.setVisibility(View.GONE);
                                binding.verificationTv.setText("Verified");
                            } else {
                                //Not verified, show the Verify Account option
                                binding.verifyAccountCv.setVisibility(View.VISIBLE);
                                binding.verificationTv.setText("Not Verified");
                            }
                        } else {
                            //userType is Google or Phone, no need to check if verified or not as it is already verified, hide the Verify Account option
                            binding.verifyAccountCv.setVisibility(View.GONE);
                            binding.verificationTv.setText("Verified");
                        }

                        try {
                            //Set profile image to profileIv
                            Glide.with(mContext)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.person_white)
                                    .into(binding.profileIv);
                        } catch (Exception e) {
                            Log.e(TAG, "onDataChange: ", e);
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void verifyAccount() {
        Log.d(TAG, "verifyAccount: ");
        //show progress
        progressDialog.setMessage("Sending Account verification instructions to your email");
        progressDialog.show();

        //send account/email verification instructions to the registered email.
        firebaseAuth.getCurrentUser().sendEmailVerification()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //instructions sent, check email, sometimes it goes in spam folder so if not in inbox check your spam folder
                        Log.d(TAG, "onSuccess: Sent");
                        progressDialog.dismiss();
                        MyUtils.toast(mContext, "Account verification instructions sent to your email");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Failed to send instructions
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(mContext, "Failed due to " + e.getMessage());
                    }
                });
    }

}