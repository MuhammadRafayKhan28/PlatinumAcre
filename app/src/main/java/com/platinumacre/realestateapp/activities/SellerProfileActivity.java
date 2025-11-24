package com.platinumacre.realestateapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.platinumacre.realestateapp.MyUtils;
import com.platinumacre.realestateapp.R;
import com.platinumacre.realestateapp.adapters.AdapterProperty;
import com.platinumacre.realestateapp.databinding.ActivitySellerProfileBinding;
import com.platinumacre.realestateapp.models.ModelProperty;

import java.util.ArrayList;

public class SellerProfileActivity extends AppCompatActivity {

    //View Binding
    private ActivitySellerProfileBinding binding;

    //TAG for logs in logcat
    private static final String TAG = "SELLER_PROFILE_TAG";


    private String sellerUid = "";

    //adArrayList to hold ads list added to favorite by currently logged-in user to show in RecyclerView
    private ArrayList<ModelProperty> adArrayList;

    //AdapterAd class instance to set to Recyclerview to show Ads list
    private AdapterProperty adapterAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        //init view binding... activity_seller_profile.xml = ActivitySellerProfileBinding
        binding = ActivitySellerProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });


        sellerUid = getIntent().getStringExtra("sellerUid");//function call to load ads by currently logged-in users
        loadSellerDetails();
        loadAds();

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void loadSellerDetails(){
        Log.d(TAG, "loadSellerDetails: ");
        //Db path to load seller info. Users > sellerUid
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(sellerUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get data
                        String name = ""+ snapshot.child("name").getValue();
                        String profileImageUrl = ""+ snapshot.child("profileImageUrl").getValue();
                        long timestamp = (Long) snapshot.child("timestamp").getValue();
                        //format date time e.g. timestamp to dd/MM/yyyy
                        String formattedDate = MyUtils.formatTimestampDate(timestamp);

                        //set data to UI Views
                        binding.sellerNameTv.setText(name);
                        binding.sellerMemberSinceTv.setText(formattedDate);
                        try {
                            Glide.with(SellerProfileActivity.this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.person_white)
                                    .into(binding.sellerProfileIv);
                        } catch (Exception e){
                            Log.e(TAG, "onDataChange: ", e);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }

    private void loadAds() {
        Log.d(TAG, "loadAds: ");
        //init adArrayList before starting adding data into it
        adArrayList = new ArrayList<>();

        //Firebase DB listener to load ads based on Category & Distance
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
        ref.orderByChild("uid").equalTo(sellerUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //clear adArrayList each time starting adding data into it
                adArrayList.clear();
                //load ads list
                for (DataSnapshot ds : snapshot.getChildren()) {

                    try {
                        //Prepare ModelAd with all data from Firebase DB
                        ModelProperty modelAd = ds.getValue(ModelProperty.class);
                        adArrayList.add(modelAd);
                    } catch (Exception e) {
                        Log.e(TAG, "onDataChange: ", e);
                    }

                }

                adapterAd = new AdapterProperty(SellerProfileActivity.this, adArrayList);
                binding.propertiesRv.setAdapter(adapterAd);

                //set ads count
                String adsCount = "" + adArrayList.size();
                binding.publishedAdsCountTv.setText(adsCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

}