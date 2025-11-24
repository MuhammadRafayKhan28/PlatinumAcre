package com.platinumacre.realestateapp.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.platinumacre.realestateapp.MyUtils;
import com.platinumacre.realestateapp.R;
import com.platinumacre.realestateapp.databinding.ActivityLocationPickerBinding;

import java.util.Arrays;
import java.util.List;

public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    //View Binding
    private ActivityLocationPickerBinding binding;

    //TAG for logs in logcat
    private static final String TAG = "LOCATION_PICKER_TAG";


    private static final int DEFAULT_ZOOM = 15;


    private GoogleMap mMap = null;

    // Current Place Picker
    private PlacesClient mPlacesClient;
    private FusedLocationProviderClient mFusedLocationProviderClient;


    private Double selectedLatitude = null;
    private Double selectedLongitude = null;
    private String selectedAddress = "";
    private String selectedCity = "";
    private String selectedState = "";
    private String selectedCountry = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        //init view binding... activity_location_picker.xml = ActivityLocationPickerBinding
        binding = ActivityLocationPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        //hide the doneLl for now. We will show when user select or search location
        binding.doneLl.setVisibility(View.GONE);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.myMapFragment);
        mapFragment.getMapAsync(this);

        // Initialize the Places client
        Places.initialize(this, getString(R.string.my_maps_api_key));

        // Create a new PlacesClient instance
        mPlacesClient = Places.createClient(this);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize the AutocompleteSupportFragment to search place on map.
        AutocompleteSupportFragment autocompleteSupportFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        // List of location fields we need in search result e.g. Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG
        Place.Field[] placesList = new Place.Field[]{Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS, Place.Field.NAME};
        //set location fields list to the autocompleteSupportFragment
        autocompleteSupportFragment.setPlaceFields(Arrays.asList(placesList));
        //listen for place selections
        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onError(@NonNull Status status) {
                //exception occurred while searching/selecting location
                Log.d(TAG, "onError: status: " + status);
            }

            @Override
            public void onPlaceSelected(@NonNull Place place) {
                Log.d(TAG, "onPlaceSelected: place: " + place);
                //Place selected. The param "place" contain all fields that we set as list.
                String id = place.getId();
                LatLng latLng = place.getLatLng();

                addressFromLatLng(latLng);
            }
        });

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        //handle toolbarGpsBtn click, if GPS enabled get and show user's current location
        binding.toolbarGpsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //check if location enabled
                if (isGpsEnabled()) {
                    //GPS/Location enabled
                    requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                } else {
                    //GPS/Location not enabled
                    MyUtils.toast(LocationPickerActivity.this, "Location is not on! Turn it on to show current location");
                }
            }
        });


        //handle doneBtn click, go-back with selected location
        binding.doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent();
                intent.putExtra("latitude", selectedLatitude);
                intent.putExtra("longitude", selectedLongitude);
                intent.putExtra("address", selectedAddress);
                intent.putExtra("city", selectedCity);
                intent.putExtra("country", selectedCountry);
                intent.putExtra("state", selectedState);
                setResult(RESULT_OK, intent);

                finish();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        //init Google Map
        mMap = googleMap;

        // Prompt the user for permission.
        requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);

        //handle mMap click, get latitude, longitude when of where user clicked on map
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                //function call to get the address details from the latLng
                addressFromLatLng(latLng);
            }
        });

    }

    @SuppressLint("MissingPermission")
    private ActivityResultLauncher<String> requestLocationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    Log.d(TAG, "onActivityResult: isGranted: " + isGranted);
                    //lets check if from permission dialog user have granted the permission or denied the result is in isGranted as true/false
                    if (isGranted) {
                        //enable google map's gps button to set current location on map
                        mMap.setMyLocationEnabled(true);
                        pickCurrentPlace();
                    } else {
                        //user denied permission so we can't pick location
                        MyUtils.toast(LocationPickerActivity.this, "Permission denied...!");
                    }
                }
            }
    );

    /**
     * This function will be called only if location permission is granted.
     * We will only check if map object is not null then proceed to show location on map
     */
    private void pickCurrentPlace() {
        Log.d(TAG, "pickCurrentPlace: ");
        if (mMap == null) {
            return;
        }


        detectAndShowDeviceLocationMap();
    }

    /**
     * Get the current location of the device, and position the map's camera
     */
    @SuppressLint("MissingPermission")
    private void detectAndShowDeviceLocationMap() {

        //Get the best and most recent location of the device, which may be null in rare cases when a location is not available.
        try {

            Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
            locationResult.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        //setup LatLng from location param of onSuccess
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));

                        //function call to retrieve the address from the latLng
                        addressFromLatLng(latLng);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //Exception occurs while getting location
                    Log.e(TAG, "onFailure: ", e);
                }
            });
        } catch (Exception e) {
            //Exception occurs while getting location
            Log.d(TAG, "detectAndShowDeviceLocationMap: ", e);
        }
    }

    /**
     * Check if GPS/Location is enabled or not
     */
    private boolean isGpsEnabled() {
        //init LocationManager
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        //boolean variables to return values as true/false
        boolean gpsEnabled = false;
        boolean networkEnabled = false;
        //Check if GPS_PROVIDER enabled
        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            Log.e(TAG, "isGpsEnabled: ", e);
        }
        //Check if NETWORK_PROVIDER enabled
        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            Log.e(TAG, "isGpsEnabled: ", e);
        }

        //return results
        return !(!gpsEnabled && !networkEnabled);
    }

    private void addressFromLatLng(LatLng latLng) {
        //init Geocoder class to get the address details from LatLng
        Geocoder geocoder = new Geocoder(this);

        try {
            //get maximum 1 result (Address) from the list of available address list of addresses on basis of latitude and longitude we passed
            List<Address> addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            //get Address object from the list addressList of type List<Address>
            Address address = addressList.get(0);
            //get the address details
            String addressLine = address.getAddressLine(0);
            String subLocality = address.getSubLocality();
            selectedLatitude = latLng.latitude;
            selectedLongitude = latLng.longitude;
            selectedCountry = address.getCountryName();
            selectedState = address.getAdminArea();
            selectedCity = address.getLocality();
            selectedAddress = addressLine;

            Log.d(TAG, "addressFromLatLng: selectedLatitude: " + selectedLatitude);
            Log.d(TAG, "addressFromLatLng: selectedLongitude: " + selectedLongitude);
            Log.d(TAG, "addressFromLatLng: selectedCountry: " + selectedCountry);
            Log.d(TAG, "addressFromLatLng: selectedState: " + selectedState);
            Log.d(TAG, "addressFromLatLng: selectedCity: " + selectedCity);
            Log.d(TAG, "addressFromLatLng: selectedAddress: " + selectedAddress);

            addMarker(latLng, subLocality, addressLine);

        } catch (Exception e) {

            Log.e(TAG, "addressFromLatLng: ", e);
        }
    }

    /**
     * Add Marker on map after searching/picking location
     *
     * @param latLng  LatLng of the location picked
     * @param title   Title of the location picked
     * @param address Address of the location picked
     */
    private void addMarker(LatLng latLng, String title, String address) {
        //clear map before adding new marker. As we only need one Location Marker on map so if there is an already one clear it before adding new
        mMap.clear();

        try {
            //Setup marker options with latLng, address title, and complete address
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title(title);
            markerOptions.snippet(address);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            //add marker to the map and move camera to the newly added marker
            mMap.addMarker(markerOptions);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));

            //show the doneLl, so user can goback (with selected location) to the activity/fragment class that is requesting the location
            binding.doneLl.setVisibility(View.VISIBLE);
            binding.selectedPlaceTv.setText(address);
        } catch (Exception e) {

            Log.e(TAG, "addMarker: ", e);
        }
    }

}