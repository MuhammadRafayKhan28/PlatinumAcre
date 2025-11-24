package com.platinumacre.realestateapp.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.platinumacre.realestateapp.adapters.AdapterProperty;
import com.platinumacre.realestateapp.databinding.ActivityMyPropertyListBinding;
import com.platinumacre.realestateapp.models.ModelProperty;

import java.util.ArrayList;

public class MyPropertyListActivity extends AppCompatActivity {

    //View Binding
    private ActivityMyPropertyListBinding binding;

    //TAG to show logs in logcat
    private static final String TAG = "MY_ADS_TAG";

    //Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;

    //adArrayList to hold ads list added to favorite by currently logged-in user to show in RecyclerView
    private ArrayList<ModelProperty> adArrayList;

    //AdapterAd class instance to set to Recyclerview to show Ads list
    private AdapterProperty adapterAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        //init view binding... activity_my_property_list.xml = ActivityMyPropertyListBinding
        binding = ActivityMyPropertyListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        //Firebase Auth for auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        //function call to load properties by currently logged-in users
        loadAds();

        //add text change listener to searchEt to search ads using filter applied in AdapterAd class
        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //this function is called whenever user type a letter, search based on what user typed
                Log.d(TAG, "onTextChanged: Query: " + s);

                try {
                    String query = s.toString();
                    adapterAd.getFilter().filter(query);
                } catch (Exception e) {
                    Log.e(TAG, "onTextChanged: ", e);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void loadAds() {
        Log.d(TAG, "loadAds: ");
        //init adArrayList before starting adding data into it
        adArrayList = new ArrayList<>();

        String myUid = "" + firebaseAuth.getUid();

        //Firebase DB listener to load ads based on Category & Distance
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
        ref.orderByChild("uid").equalTo(myUid).addValueEventListener(new ValueEventListener() {
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

                adapterAd = new AdapterProperty(MyPropertyListActivity.this, adArrayList);
                binding.propertiesRv.setAdapter(adapterAd);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

}