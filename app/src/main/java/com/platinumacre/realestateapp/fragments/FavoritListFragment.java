package com.platinumacre.realestateapp.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.platinumacre.realestateapp.adapters.AdapterProperty;
import com.platinumacre.realestateapp.databinding.FragmentFavoritListBinding;
import com.platinumacre.realestateapp.models.ModelProperty;

import java.util.ArrayList;

public class FavoritListFragment extends Fragment {

    //View Binding
    private FragmentFavoritListBinding binding;

    //TAG to show logs in logcat
    private static final String TAG = "FAV_TAG";

    //Context for this fragment class
    private Context mContext;

    //Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;

    //adArrayList to hold ads list added to favorite by currently logged-in user to show in RecyclerView
    private ArrayList<ModelProperty> adArrayList;

    //AdapterAd class instance to set to Recyclerview to show Ads list
    private AdapterProperty adapterAd;

    public FavoritListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        //get and init the context for this fragment class
        mContext = context;
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate/bind the layout (fragment_favorite_list.xml) for this fragment
        binding = FragmentFavoritListBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Firebase Auth for auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();
        //function call to load ads by currently logged-in users
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
                } catch (Exception e){
                    Log.e(TAG, "onTextChanged: ", e);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void loadAds(){
        Log.d(TAG, "loadAds: ");
        //init adArrayList before starting adding data into it
        adArrayList = new ArrayList<>();

        //Firebase DB listener to get the ids of the Ads added to favorite by currently logged-in user. e.g. Users > uid > Favorites > FAV_ADS_DATA
        DatabaseReference favRef = FirebaseDatabase.getInstance().getReference("Users");
        favRef.child(firebaseAuth.getUid()).child("Favorites")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //clear adArrayList each time starting adding data into it
                        adArrayList.clear();

                        //load favorite Ad ids
                        for (DataSnapshot ds: snapshot.getChildren()){
                            //get the id of the Ad. e.g. Users > uid > Favorites > adId
                            String adId = ""+ ds.child("propertyId").getValue();
                            Log.d(TAG, "onDataChange: adId: "+adId);

                            //Firebase DB listener to load Ad details based on id of the Ad we just got
                            DatabaseReference adRef = FirebaseDatabase.getInstance().getReference("Properties");
                            adRef.child(adId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                                            try {
                                                //Prepare ModelAd with all data from Firebase DB
                                                ModelProperty modelAd = snapshot.getValue(ModelProperty.class);
                                                //add prepared model to adArrayList
                                                adArrayList.add(modelAd);
                                            } catch (Exception e){
                                                Log.e(TAG, "onDataChange: ", e);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                        }

                        //sometimes fav ads were not loading due to nested db listeners because we are getting data using 2 paths so added some delay e.g. half second
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //init/setup AdapterAd class and set to recyclerview
                                adapterAd = new AdapterProperty(mContext, adArrayList);
                                binding.adsRv.setAdapter(adapterAd);
                            }
                        }, 500);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }
}