package com.platinumacre.realestateapp.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.platinumacre.realestateapp.MyUtils;
import com.platinumacre.realestateapp.R;
import com.platinumacre.realestateapp.activities.LocationPickerActivity;
import com.platinumacre.realestateapp.adapters.AdapterProperty;
import com.platinumacre.realestateapp.databinding.BsFilterCategoryBinding;
import com.platinumacre.realestateapp.databinding.FragmentHomeBinding;
import com.platinumacre.realestateapp.models.ModelProperty;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    //View Binding
    private FragmentHomeBinding binding;

    //TAG for logs in logcat
    private static final String TAG = "HOME_TAG";

    //Context for this fragment class
    private Context mContext;

    //SharedPreferences to store the selected location from map to load ads nearby
    private SharedPreferences locationSp;

    //location info required to load ads nearby. We will get this info from the SharedPreferences saved after picking from map
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private String currentAddress = "";
    private String currentCity = "";

    //We will save selected filter options in class level so whenever user open the filter dialog it will show with previously selected filters
    private String filterPurpose = MyUtils.PROPERTY_PURPOSE_ANY;
    private String filterCategory = "";
    private String filterSubcategory = "";
    private Double filterPriceMin = 0.0;
    private Double filterPriceMax = null;

    @Override
    public void onAttach(@NonNull Context context) {
        //get and init the context for this fragment class
        mContext = context;
        super.onAttach(context);
    }

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate/bind the layout for this fragment
        binding = FragmentHomeBinding.inflate(LayoutInflater.from(mContext), container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //init the shared preferences param 1 is name of the Shared Preferences file, param 2 is mode of the SharedPreferences
        locationSp = mContext.getSharedPreferences("LOCATION_SP", Context.MODE_PRIVATE);
        //get saved current latitude, longitude, address from the Shared Preferences. In next steps we will pick these info from map and save in it
        currentLatitude = locationSp.getFloat("CURRENT_LATITUDE", 0.0f);
        currentLongitude = locationSp.getFloat("CURRENT_LONGITUDE", 0.0f);
        currentAddress = locationSp.getString("CURRENT_ADDRESS", "");
        currentCity = locationSp.getString("CURRENT_CITY", "");

        //if current location is not 0 i.e. location is picked
        if (currentLatitude != 0.0 && currentLongitude != 0.0) {
            binding.cityTv.setText(currentCity);
        }

        loadProperties();

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

        //handle cityTv click, select location to show properties nearBy
        binding.cityTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, LocationPickerActivity.class);
                locationPickerActivityResult.launch(intent);
            }
        });

        binding.filterTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFilterDialog();
            }
        });
    }

    private ActivityResultLauncher<Intent> locationPickerActivityResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //check if from map, location is picked or not
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d(TAG, "onActivityResult: RESULT_OK");

                        Intent data = result.getData();

                        if (data != null) {
                            Log.d(TAG, "onActivityResult: Location picked");
                            //get location info from intent
                            currentLatitude = data.getDoubleExtra("latitude", 0.0);
                            currentLongitude = data.getDoubleExtra("longitude", 0.0);
                            currentAddress = data.getStringExtra("address");
                            currentCity = data.getStringExtra("city");
                            //save location info to shared preferences so when we launch app next time we don't need to pick again
                            locationSp.edit()
                                    .putFloat("CURRENT_LATITUDE", Float.parseFloat("" + currentLatitude))
                                    .putFloat("CURRENT_LONGITUDE", Float.parseFloat("" + currentLongitude))
                                    .putString("CURRENT_ADDRESS", currentAddress)
                                    .putString("CURRENT_CITY", currentCity)
                                    .apply();
                            //set the picked address
                            binding.cityTv.setText(currentCity);
                            //after picking address reload all ads again based on newly picked location
                            loadProperties();

                        }
                    } else {
                        Log.d(TAG, "onActivityResult: Cancelled!");
                        MyUtils.toast(mContext, "Cancelled!");
                    }
                }
            }
    );

    private ArrayList<ModelProperty> adArrayList;
    private AdapterProperty adapterAd;

    private void loadProperties() {
        //init adArrayList before starting adding data into it
        adArrayList = new ArrayList<>();

        Log.d(TAG, "loadProperties: filterPurpose: " + filterPurpose);
        Log.d(TAG, "loadProperties: filterCategory: " + filterCategory);
        Log.d(TAG, "loadProperties: filterSubcategory: " + filterSubcategory);
        Log.d(TAG, "loadProperties: filterPriceMin: " + filterPriceMin);
        Log.d(TAG, "loadProperties: filterPriceMax: " + filterPriceMax);

        //show selected filter status
        if (filterPurpose.equals(MyUtils.PROPERTY_PURPOSE_ANY)) {
            binding.filterSelectedTv.setText("Showing All");
        } else {
            binding.filterSelectedTv.setText("Showing " + filterPurpose + " > " + filterCategory + " > " + filterSubcategory);
        }

        //Firebase DB listener to load ads based on Category & Distance
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //clear adArrayList each time starting adding data into it
                adArrayList.clear();
                //load ads list
                for (DataSnapshot ds : snapshot.getChildren()) {

                    try {
                        //Prepare ModelAd with all data from Firebase DB
                        ModelProperty modelAd = ds.getValue(ModelProperty.class);

                        double propertyLatitude = modelAd.getLatitude();
                        double propertyLongitude = modelAd.getLongitude();

                        //function call with returned value as distance in kilometer.
                        double distance = MyUtils.calculateDistanceKm(currentLatitude, currentLongitude, propertyLatitude, propertyLongitude);
                        Log.d(TAG, "loadProperties: onDataChange: distance: " + distance);

                        //Filter: First check property distance if is <= required e.g. 50km then show
                        if (distance <= MyUtils.MAX_DISTANCE_TO_LOAD_PROPERTIES_KM && isPropertyMatchingFilter(modelAd)) {
                            adArrayList.add(modelAd);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onDataChange: ", e);
                    }

                }

                adapterAd = new AdapterProperty(mContext, adArrayList);
                binding.propertiesRv.setAdapter(adapterAd);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private boolean isPropertyMatchingFilter(ModelProperty property) {
        // No filtering if purpose is "any"
        if (filterPurpose.equals(MyUtils.PROPERTY_PURPOSE_ANY)) {
            return true;
        }

        boolean matchesPurpose = property.getPurpose().equalsIgnoreCase(filterPurpose);
        boolean matchesCategory = property.getCategory().equalsIgnoreCase(filterCategory);
        boolean matchesSubcategory = property.getSubcategory().equalsIgnoreCase(filterSubcategory);

        boolean matchesAllTypes = matchesPurpose && matchesCategory && matchesSubcategory;
        boolean matchesPrice = property.getPrice() >= filterPriceMin &&
                (filterPriceMax == null || property.getPrice() <= filterPriceMax);

        return matchesAllTypes && matchesPrice;
    }

    private ArrayAdapter<String> stringArrayPropertyCategory;
    private ArrayAdapter<String> stringArrayPropertySubcategory;

    private void showFilterDialog() {
        Log.d(TAG, "showImagePickOptions: ");

        BsFilterCategoryBinding bindingBs = BsFilterCategoryBinding.inflate(getLayoutInflater());

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(mContext);
        bottomSheetDialog.setContentView(bindingBs.getRoot());

        bottomSheetDialog.show();

        if (!filterCategory.isEmpty()){
            bindingBs.propertyCategoryAct.setText(filterCategory);
        }
        if (!filterSubcategory.isEmpty()){
            bindingBs.propertySubcategoryAct.setText(filterSubcategory);
        }
        if (filterPriceMin != 0.0){
            bindingBs.priceMinEt.setText("" + filterPriceMin);
        }
        if (filterPriceMax != null){
            bindingBs.priceMaxEt.setText("" + filterPriceMax);
        }

        //handle tabBuyTv click, select Buy Tab to show properties available to Buy
        bindingBs.tabBuyTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                filterPurpose = MyUtils.PROPERTY_PURPOSE_SELL;

                bindingBs.tabBuyTv.setBackgroundResource(R.drawable.shape_rounded_white);
                bindingBs.tabBuyTv.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimary));
                bindingBs.tabBuyTv.setTypeface(null, Typeface.BOLD);

                bindingBs.tabRentTv.setBackground(null);
                bindingBs.tabRentTv.setTextColor(ContextCompat.getColor(mContext, R.color.black));
                bindingBs.tabRentTv.setTypeface(null, Typeface.NORMAL);
            }
        });

        //handle tabRentTv click, select Rent Tab to show properties available to Rent
        bindingBs.tabRentTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                filterPurpose = MyUtils.PROPERTY_PURPOSE_RENT;

                bindingBs.tabBuyTv.setBackground(null);
                bindingBs.tabBuyTv.setTextColor(ContextCompat.getColor(mContext, R.color.black));
                bindingBs.tabBuyTv.setTypeface(null, Typeface.NORMAL);

                bindingBs.tabRentTv.setBackgroundResource(R.drawable.shape_rounded_white);
                bindingBs.tabRentTv.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimary));
                bindingBs.tabRentTv.setTypeface(null, Typeface.BOLD);
            }
        });

        //Setup adapter to set categories on propertyCategoryAct
        stringArrayPropertyCategory = new ArrayAdapter<>(mContext, R.layout.row_auto_complete_text, MyUtils.propertyTypes);
        //set adapter to propertyCategoryAct
        bindingBs.propertyCategoryAct.setAdapter(stringArrayPropertyCategory);

        bindingBs.propertyCategoryAct.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                filterCategory = (String) parent.getItemAtPosition(position);
                Log.d(TAG, "onItemClick: filterCategory: " + filterCategory);

                filterSubcategory = "";
                bindingBs.propertySubcategoryAct.setText(filterSubcategory);

                if (filterCategory.equals(MyUtils.propertyTypes[0])) {
                    stringArrayPropertySubcategory = new ArrayAdapter<>(mContext, R.layout.row_auto_complete_text, MyUtils.propertyTypesHome);
                } else if (filterCategory.equals(MyUtils.propertyTypes[1])) {
                    stringArrayPropertySubcategory = new ArrayAdapter<>(mContext, R.layout.row_auto_complete_text, MyUtils.propertyTypesPlots);
                } else if (filterCategory.equals(MyUtils.propertyTypes[2])) {
                    stringArrayPropertySubcategory = new ArrayAdapter<>(mContext, R.layout.row_auto_complete_text, MyUtils.propertyTypesCommercial);
                }
                //set adapter to propertySubcategoryAct
                bindingBs.propertySubcategoryAct.setAdapter(stringArrayPropertySubcategory);

            }
        });

        bindingBs.propertySubcategoryAct.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                filterSubcategory = (String) parent.getItemAtPosition(position);
            }
        });

        bindingBs.resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDialog.dismiss();

                filterPurpose = MyUtils.PROPERTY_PURPOSE_ANY;
                filterCategory = "";
                filterSubcategory = "";
                filterPriceMin = 0.0;
                filterPriceMax = null;

                loadProperties();
            }
        });

        bindingBs.applyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Since the tab tabBuyTv is by default selected so assign it with it "Sell"
                if (filterPurpose.equals(MyUtils.PROPERTY_PURPOSE_ANY)) {
                    filterPurpose = MyUtils.PROPERTY_PURPOSE_SELL;
                }

                //Category is required
                if (filterCategory.isEmpty()) {
                    bindingBs.propertyCategoryAct.setError("Choose Category");
                    bindingBs.propertyCategoryAct.requestFocus();

                    return;
                }
                //Subcategory is required
                if (filterSubcategory.isEmpty()) {
                    bindingBs.propertySubcategoryAct.setError("Choose Subcategory");
                    bindingBs.propertySubcategoryAct.requestFocus();

                    return;
                }

                //input min and max price
                String priceMin = bindingBs.priceMinEt.getText().toString().trim();
                String priceMax = bindingBs.priceMaxEt.getText().toString().trim();

                //if min price is empty then consider it as 0.0
                if (priceMin.isEmpty()) {
                    filterPriceMin = 0.0;
                } else {
                    filterPriceMin = Double.parseDouble(priceMin);
                }
                //if max price is not entered then we will consider user doesn't want to filter based on max price
                if (priceMax.isEmpty()) {
                    filterPriceMax = null;
                } else {
                    filterPriceMax = Double.parseDouble(priceMax);
                }

                bottomSheetDialog.dismiss();
                loadProperties();

            }
        });

    }

}