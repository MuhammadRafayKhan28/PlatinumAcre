package com.platinumacre.realestateapp.activities;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.platinumacre.realestateapp.MyUtils;
import com.platinumacre.realestateapp.R;
import com.platinumacre.realestateapp.adapters.AdapterImagesPicked;
import com.platinumacre.realestateapp.databinding.ActivityPropertyAddBinding;
import com.platinumacre.realestateapp.models.ModelImagePicked;
import com.platinumacre.realestateapp.models.ModelProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PropertyAddActivity extends AppCompatActivity {

    //View Binding
    private ActivityPropertyAddBinding binding;

    //TAG for logs in logcat
    private static final String TAG = "PROPERTY_ADD_TAG";

    //ProgressDialog to show while adding/updating the Ad
    private ProgressDialog progressDialog;

    //Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;

    //Image Uri to hold uri of the image (picked/captured using Gallery/Camera) to add in Ad Images List
    private Uri imageUri = null;

    //list of images (picked/captured using Gallery/Camera or from internet)
    private ArrayList<ModelImagePicked> imagePickedArrayList;

    //Adapter and is responsible for managing and displaying a collection of picked images in a RecyclerView.
    private AdapterImagesPicked adapterImagesPicked;

    private boolean isEditMode = false;

    private String propertyIdForEditing = "";

    //Array Adapter to set to AutoCompleteTextView, so user can select subcategory base on category
    private ArrayAdapter<String> adapterPropertySubcategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        //init view binding... activity_property_add.xml = ActivityPropertyAddBinding
        binding = ActivityPropertyAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        //init/setup ProgressDialog to show while adding/updating the Ad
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        //Firebase Auth for auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        //Setup and set the property area size unit adapter to the Property Area Unit Filed i.e. areaSizeUnitAct
        ArrayAdapter<String> adapterCategories = new ArrayAdapter<>(this, R.layout.row_auto_complete_text, MyUtils.propertyAreaSizeUnit);
        binding.areaSizeUnitAct.setAdapter(adapterCategories);

        Intent intent = getIntent();
        isEditMode = intent.getBooleanExtra("isEditMode", false);
        Log.d(TAG, "onCreate: isEditMode: " + isEditMode);

        if (isEditMode) {
            //Edit Ad Model: Get the Ad Id for editing the Ad
            propertyIdForEditing = intent.getStringExtra("propertyIdForEditing");

            //function call to load Ad details by using Ad Id
            loadAdDetails();

            //change toolbar title and submit button text
            binding.toolbarTitleTv.setText("Update Ad");
            binding.submitBtn.setText("Updated Ad");
        } else {
            //New Ad Mode: Change toolbar title and submit button text
            binding.toolbarTitleTv.setText("Create Ad");
            binding.submitBtn.setText("Post Ad");
        }

        //init imagePickedArrayList
        imagePickedArrayList = new ArrayList<>();
        //loadImages
        loadImages();

        propertyCategoryHomes();

        //handle propertyCategoryTabLayout change listener, Choose Category
        binding.propertyCategoryTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                category = "" + tab.getText();
                Log.d(TAG, "onTabSelected: category: " + category);

                switch (category) {
                    case "Homes" ->
                        //Homes Tab clicked: Prepare adapter with categories related to Homes
                            propertyCategoryHomes();
                    case "Plots" ->
                        //Plots Tab clicked: Prepare adapter with categories related to Plots
                            propertyCategoryPlots();
                    case "Commercial" ->
                        //Commercial Tab clicked: Prepare adapter with categories related to Commercial
                            propertyCategoryCommercial();
                }

                //set adapter to propertySubcategoryAct
                binding.propertySubcategoryAct.setAdapter(adapterPropertySubcategory);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        // Set a listener for the RadioGroup
        binding.purposeRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // Find the selected RadioButton by checkedId
                RadioButton selectedRadioButton = findViewById(checkedId);

                // Get the text of the selected RadioButton
                purpose = selectedRadioButton.getText().toString();
            }
        });

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        //handle pickImageTv click, show image add options (Gallery/Camera)
        binding.pickImageTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showImagePickOptions();
            }
        });

        //handle locationAct click, launch LocationPickerActivity to pick location from MAP
        binding.locationAct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PropertyAddActivity.this, LocationPickerActivity.class);
                locationPickerActivityResultLauncher.launch(intent);
            }
        });

        //handle submitBtn click, validate data and upload
        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();
            }
        });

    }

    private void propertyCategoryHomes() {
        binding.floorsTil.setVisibility(View.VISIBLE);
        binding.bedRoomsTil.setVisibility(View.VISIBLE);
        binding.bathRoomsTil.setVisibility(View.VISIBLE);

        //Array Adapter to set to AutoCompleteTextView, so user can select subcategory base on category
        adapterPropertySubcategory = new ArrayAdapter<>(PropertyAddActivity.this, R.layout.row_auto_complete_text, MyUtils.propertyTypesHome);
        //set adapter to propertySubcategoryAct
        binding.propertySubcategoryAct.setAdapter(adapterPropertySubcategory);

        binding.propertySubcategoryAct.setText("");
    }

    private void propertyCategoryPlots() {
        binding.floorsTil.setVisibility(View.GONE);
        binding.bedRoomsTil.setVisibility(View.GONE);
        binding.bathRoomsTil.setVisibility(View.GONE);

        //Array Adapter to set to AutoCompleteTextView, so user can select subcategory base on category
        adapterPropertySubcategory = new ArrayAdapter<>(PropertyAddActivity.this, R.layout.row_auto_complete_text, MyUtils.propertyTypesPlots);
        //set adapter to propertySubcategoryAct
        binding.propertySubcategoryAct.setAdapter(adapterPropertySubcategory);

        binding.propertySubcategoryAct.setText("");
    }

    private void propertyCategoryCommercial() {
        binding.floorsTil.setVisibility(View.VISIBLE);
        binding.bedRoomsTil.setVisibility(View.GONE);
        binding.bathRoomsTil.setVisibility(View.GONE);

        //Array Adapter to set to AutoCompleteTextView, so user can select subcategory base on category
        adapterPropertySubcategory = new ArrayAdapter<>(PropertyAddActivity.this, R.layout.row_auto_complete_text, MyUtils.propertyTypesCommercial);
        //set adapter to propertySubcategoryAct
        binding.propertySubcategoryAct.setAdapter(adapterPropertySubcategory);

        binding.propertySubcategoryAct.setText("");
    }

    private void loadImages() {
        Log.d(TAG, "loadImages: ");
        //init setup adapterImagesPicked to set it RecyclerView i.e. imagesRv. Param 1 is Context, Param 2 is Images List to show in RecyclerView, Param 3 is id of the Ad
        adapterImagesPicked = new AdapterImagesPicked(this, imagePickedArrayList, propertyIdForEditing);
        //set the adapter to the RecyclerView i.e. imagesRv
        binding.imagesRv.setAdapter(adapterImagesPicked);
    }

    private ActivityResultLauncher<Intent> locationPickerActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(TAG, "onActivityResult: ");

                    //get result of location picked from LocationPickerActivity
                    if (result.getResultCode() == Activity.RESULT_OK) {

                        Intent data = result.getData();

                        if (data != null) {
                            latitude = data.getDoubleExtra("latitude", 0);
                            longitude = data.getDoubleExtra("longitude", 0);
                            address = data.getStringExtra("address");
                            city = data.getStringExtra("city");
                            country = data.getStringExtra("country");
                            state = data.getStringExtra("state");

                            Log.d(TAG, "onActivityResult: latitude: "+latitude);
                            Log.d(TAG, "onActivityResult: longitude: "+longitude);
                            Log.d(TAG, "onActivityResult: address: "+address);
                            Log.d(TAG, "onActivityResult: city: "+city);
                            Log.d(TAG, "onActivityResult: country: "+country);
                            Log.d(TAG, "onActivityResult: state: "+state);

                            binding.locationAct.setText(address);
                        }
                    } else {
                        Log.d(TAG, "onActivityResult: cancelled");
                        MyUtils.toast(PropertyAddActivity.this, "Cancelled");
                    }
                }
            }
    );

    private void showImagePickOptions() {
        Log.d(TAG, "showImagePickOptions: ");
        //init the PopupMenu. Param 1 is context. Param 2 is Anchor view for this popup. The popup will appear below the anchor if there is room, or above it if there is not.
        PopupMenu popupMenu = new PopupMenu(this, binding.pickImageTv);

        //add menu items to our popup menu Param#1 is GroupID, Param#2 is ItemID, Param#3 is OrderID, Param#4 is Menu Item Title
        popupMenu.getMenu().add(Menu.NONE, 1, 1, "Camera");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "Gallery");
        //Show Popup Menu
        popupMenu.show();
        //handle popup menu item click
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //get the id of the item clicked in popup menu
                int itemId = item.getItemId();
                //check which item id is clicked from popup menu. 1=Camera. 2=Gallery as we defined
                if (itemId == 1) {
                    //Camera is clicked we need to check if we have permission of Camera, Storage before launching Camera to Capture image

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        //Device version is TIRAMISU or above. We only need Camera permission
                        String[] cameraPermissions = new String[]{Manifest.permission.CAMERA};
                        requestCameraPermissions.launch(cameraPermissions);
                    } else {
                        //Device version is below TIRAMISU. We need Camera & Storage permissions
                        String[] cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestCameraPermissions.launch(cameraPermissions);
                    }

                } else if (itemId == 2) {
                    //Gallery is clicked we need to check if we have permission of Storage before launching Gallery to Pick image

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        //Device version is TIRAMISU or above. We don't need Storage permission to launch Gallery
                        pickImageGallery();
                    } else {
                        //Device version is below TIRAMISU. We need Storage permission to launch Gallery
                        String storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                        requestStoragePermission.launch(storagePermission);
                    }

                }
                return true;
            }
        });

    }

    private ActivityResultLauncher<String> requestStoragePermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    Log.d(TAG, "onActivityResult: isGranted: " + isGranted);
                    //let's check if permission is granted or not
                    if (isGranted) {
                        //Storage Permission granted, we can now launch gallery to pick image
                        pickImageGallery();
                    } else {
                        //Storage Permission denied, we can't launch gallery to pick image
                        MyUtils.toast(PropertyAddActivity.this, "Storage Permission denied...");
                    }
                }
            }
    );


    private ActivityResultLauncher<String[]> requestCameraPermissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    Log.d(TAG, "onActivityResult: ");
                    Log.d(TAG, "onActivityResult: " + result.toString());

                    //let's check if permissions are granted or not
                    boolean areAllGranted = true;
                    for (Boolean isGranted : result.values()) {

                        areAllGranted = areAllGranted && isGranted;
                    }

                    if (areAllGranted) {
                        //All Permissions Camera, Storage are granted, we can now launch camera to capture image
                        pickImageCamera();
                    } else {
                        //Camera or Storage or Both permissions are denied, Can't launch camera to capture image
                        MyUtils.toast(PropertyAddActivity.this, "Camera or Storage or both permissions denied...");
                    }
                }
            }
    );

    private void pickImageGallery() {
        Log.d(TAG, "pickImageGallery: ");
        //Intent to launch Image Picker e.g. Gallery
        Intent intent = new Intent(Intent.ACTION_PICK);
        //We only want to pick images
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private void pickImageCamera() {
        Log.d(TAG, "pickImageCamera: ");
        //Setup Content values, MediaStore to capture high quality image using camera intent
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "TEMPORARY_IMAGE");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "TEMPORARY_IMAGE_DESCRIPTION");
        //Uri of the image to be captured from camera
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        //Intent to launch camera
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(TAG, "onActivityResult: ");
                    //Check if image is picked or not
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        //get data from result param
                        Intent data = result.getData();
                        //get uri of image picked
                        imageUri = data.getData();

                        Log.d(TAG, "onActivityResult: imageUri: " + imageUri);
                        //timestamp will be used as id of the image picked
                        String timestamp = "" + MyUtils.getTimestamp();

                        //setup model for image. Param 1 is id, Param 2 is imageUri, Param 3 is imageUrl, fromInternet
                        ModelImagePicked modelImagePicked = new ModelImagePicked(timestamp, imageUri, null, false); //add model to the imagePickedArrayList
                        imagePickedArrayList.add(modelImagePicked);

                        //reload the images
                        loadImages();
                    } else {
                        //Cancelled
                        MyUtils.toast(PropertyAddActivity.this, "Cancelled...!");
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(TAG, "onActivityResult: ");
                    //Check if image is picked or not
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        //no need to get image uri here we will have it in pickImageCamera() function

                        Log.d(TAG, "onActivityResult: imageUri: " + imageUri);
                        //timestamp will be used as id of the image picked
                        String timestamp = "" + MyUtils.getTimestamp();

                        //setup model for image. Param 1 is id, Param 2 is imageUri, Param 3 is imageUrl, fromInternet
                        ModelImagePicked modelImagePicked = new ModelImagePicked(timestamp, imageUri, null, false); //add model to the imagePickedArrayList
                        imagePickedArrayList.add(modelImagePicked);

                        //reload the images
                        loadImages();
                    } else {
                        //Cancelled
                        MyUtils.toast(PropertyAddActivity.this, "Cancelled...!");
                    }
                }
            }
    );

    private String purpose = MyUtils.PROPERTY_PURPOSE_SELL;
    private String category = "Homes";
    private String subcategory = "";
    private String floors = "";
    private String bedRooms = "";
    private String bathRooms = "";
    private String areaSize = "";
    private String areaSizeUnit = "";
    private String price = "";
    private String title = "";
    private String description = "";
    private String email = "";
    private String phoneCode = "";
    private String phoneNumber = "";
    private String country = "";
    private String state = "";
    private String city = "";
    private String address = "";
    private double latitude = 0;
    private double longitude = 0;

    private void validateData() {
        Log.d(TAG, "validateData: ");
        //input data
        subcategory = binding.propertySubcategoryAct.getText().toString().trim();
        floors = binding.floorsEt.getText().toString().trim();
        bedRooms = binding.bedRoomsEt.getText().toString().trim();
        bathRooms = binding.bathRoomsEt.getText().toString().trim();
        areaSize = binding.areaSizeEt.getText().toString().trim();
        areaSizeUnit = binding.areaSizeUnitAct.getText().toString().trim();
        address = binding.locationAct.getText().toString().trim();
        price = binding.priceEt.getText().toString().trim();
        title = binding.titleEt.getText().toString().trim();
        description = binding.descriptionEt.getText().toString().trim();
        email = binding.emailEt.getText().toString().trim();
        phoneCode = binding.phoneCodeTil.getSelectedCountryCodeWithPlus();
        phoneNumber = binding.phoneNumberEt.getText().toString().trim();

        if (subcategory.isEmpty()) {
            //no property subcategory selected in propertySubcategoryAct, show error in propertySubcategoryAct and focus
            binding.propertySubcategoryAct.setError("Choose Subcategory...!");
            binding.propertySubcategoryAct.requestFocus();
        } else if (category.equals(getString(R.string.category_homes)) && floors.isEmpty()) {
            //no floors count entered in floorsEt, show error in bedRoomsEt and focus
            binding.floorsEt.setError("Enter Floors Count...!");
            binding.floorsEt.requestFocus();
        } else if (category.equals(getString(R.string.category_homes)) && bedRooms.isEmpty()) {
            //no bedrooms count entered in bedRoomsEt, show error in bedRoomsEt and focus
            binding.bedRoomsEt.setError("Enter Bedrooms Count...!");
            binding.bedRoomsEt.requestFocus();
        } else if (category.equals(getString(R.string.category_homes)) && bathRooms.isEmpty()) {
            //no bathrooms count entered in bathRoomsEt, show error in bathRoomsEt and focus
            binding.bathRoomsEt.setError("Enter Bathrooms Count...!");
            binding.bathRoomsEt.requestFocus();
        } else if (areaSize.isEmpty()) {
            //no area size entered in areaSizeEt, show error in areaSizeEt and focus
            binding.areaSizeEt.setError("Enter Area Size...!");
            binding.areaSizeEt.requestFocus();
        } else if (address.isEmpty()) {
            //no address selected in locationAct, show error in locationAct and focus
            binding.locationAct.setError("Pick Location...!");
            binding.locationAct.requestFocus();
        } else if (price.isEmpty()) {
            //no price entered in priceEt, show error in priceEt and focus
            binding.priceEt.setError("Enter Price...!");
            binding.priceEt.requestFocus();
        } else if (title.isEmpty()) {
            //no title entered in titleEt, show error in titleEt and focus
            binding.titleEt.setError("Enter Title...!");
            binding.titleEt.requestFocus();
        } else if (description.isEmpty()) {
            //no description entered in descriptionEt, show error in descriptionEt and focus
            binding.descriptionEt.setError("Enter Description...!");
            binding.descriptionEt.requestFocus();
        } else if (phoneNumber.isEmpty()) {
            //no phone number entered in phoneNumberEt, show error in phoneNumberEt and focus
            binding.phoneNumberEt.setError("Enter Phone Number...!");
            binding.phoneNumberEt.requestFocus();
        } else if (imagePickedArrayList.isEmpty()) {
            //no image selected/picked
            MyUtils.toast(this, "Pick at-least one image");
        } else {
            //All data is validated, we can proceed further now
            if (isEditMode) {
                updateAd();
            } else {
                postAd();
            }
        }
    }

    private void postAd() {
        Log.d(TAG, "postAd: ");
        //show progress
        progressDialog.setMessage("Publishing Ad");
        progressDialog.show();

        if (floors.isEmpty()) {
            floors = "0";
        }
        if (bedRooms.isEmpty()) {
            bedRooms = "0";
        }
        if (bathRooms.isEmpty()) {
            bathRooms = "0";
        }

        //get current timestamp
        long timestamp = MyUtils.getTimestamp();
        //firebase database Properties reference to store new Properties
        DatabaseReference refProperties = FirebaseDatabase.getInstance().getReference("Properties");
        //key id from the reference to use as Ad id
        String keyId = refProperties.push().getKey();

        //setup data to add in firebase database
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("id", "" + keyId);
        hashMap.put("uid", "" + firebaseAuth.getUid());
        hashMap.put("purpose", "" + purpose);
        hashMap.put("category", "" + category);
        hashMap.put("subcategory", "" + subcategory);
        hashMap.put("areaSizeUnit", "" + areaSizeUnit);
        hashMap.put("title", "" + title);
        hashMap.put("description", "" + description);
        hashMap.put("email", "" + email);
        hashMap.put("phoneCode", "" + phoneCode);
        hashMap.put("phoneNumber", "" + phoneNumber);
        hashMap.put("country", "" + country);
        hashMap.put("city", "" + city);
        hashMap.put("state", "" + state);
        hashMap.put("address", "" + address);
        hashMap.put("status", "" + MyUtils.AD_STATUS_AVAILABLE);
        hashMap.put("floors", Long.parseLong(floors));
        hashMap.put("bedRooms", Long.parseLong(bedRooms));
        hashMap.put("bathRooms", Long.parseLong(bathRooms));
        hashMap.put("areaSize", Double.parseDouble(areaSize));
        hashMap.put("price", Double.parseDouble(price));
        hashMap.put("timestamp", timestamp);
        hashMap.put("latitude", latitude);
        hashMap.put("longitude", longitude);

        //set data to firebase database. Properties -> PropertyId -> PropertyDataJSON
        refProperties.child(keyId)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Ad Published");

                        uploadImagesStorage(keyId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(PropertyAddActivity.this, "Failed to publish Ad due to " + e.getMessage());
                    }
                });
    }


    private void updateAd() {
        Log.d(TAG, "updateAd: ");

        progressDialog.setMessage("Updating Ad...");
        progressDialog.show();

        //setup data to add in firebase database
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("purpose", "" + purpose);
        hashMap.put("category", "" + category);
        hashMap.put("subcategory", "" + subcategory);
        hashMap.put("areaSizeUnit", "" + areaSizeUnit);
        hashMap.put("title", "" + title);
        hashMap.put("description", "" + description);
        hashMap.put("email", "" + email);
        hashMap.put("phoneCode", "" + phoneCode);
        hashMap.put("phoneNumber", "" + phoneNumber);
        hashMap.put("country", "" + country);
        hashMap.put("city", "" + city);
        hashMap.put("state", "" + state);
        hashMap.put("address", "" + address);
        hashMap.put("floors", Long.parseLong(floors));
        hashMap.put("bedRooms", Long.parseLong(bedRooms));
        hashMap.put("bathRooms", Long.parseLong(bathRooms));
        hashMap.put("areaSize", Double.parseDouble(areaSize));
        hashMap.put("price", Double.parseDouble(price));
        hashMap.put("latitude", latitude);
        hashMap.put("longitude", longitude);

        //Db path to update Ad. Properties -> PropertyId -> PropertyDataJSON
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
        ref.child(propertyIdForEditing)
                .updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //Ad data update success
                        progressDialog.dismiss();
                        //start uploading images picked for the Ad
                        uploadImagesStorage(propertyIdForEditing);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Ad data update failed

                        Log.e(TAG, "onFailure: ", e);
                        MyUtils.toast(PropertyAddActivity.this, "Failed to update Ad due to " + e.getMessage());
                    }
                });
    }

    private void uploadImagesStorage(String propertyId) {
        Log.d(TAG, "uploadImagesStorage: ");
        //there are multiple images in imagePickedArrayList, loop to upload all
        for (int i = 0; i < imagePickedArrayList.size(); i++) {
            //get model from the current position of the imagePickedArrayList
            ModelImagePicked modelImagePicked = imagePickedArrayList.get(i);

            //Upload image only if picked from gallery/camera
            if (!modelImagePicked.getFromInternet()) {

                //for name of the image in firebase storage
                String imageName = modelImagePicked.getId();
                //path and name of the image in firebase storage
                String filePathAndName = "Properties/" + imageName;

                int imageIndexForProgress = i + 1;

                //Storage reference with filePathAndName
                StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);

                storageReference.putFile(modelImagePicked.getImageUri())
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                                //calculate the current progress of the image being uploaded
                                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                                //setup progress dialog message on basis of current progress. e.g. Uploading 1 of 10 images... Progress 95%
                                String message = "Uploading " + imageIndexForProgress + " of " + imagePickedArrayList.size() + " images...\nProgress " + (int) progress + "%";
                                Log.d(TAG, "onProgress: message: " + message);
                                //show progress
                                progressDialog.setMessage(message);
                                progressDialog.show();
                            }
                        })
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Log.d(TAG, "onSuccess: ");
                                //image uploaded get url of uploaded image
                                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                                while (!uriTask.isSuccessful()) ;
                                Uri uploadedImageUrl = uriTask.getResult();

                                if (uriTask.isSuccessful()) {

                                    HashMap<String, Object> hashMap = new HashMap<>();
                                    hashMap.put("id", "" + modelImagePicked.getId());
                                    hashMap.put("imageUrl", "" + uploadedImageUrl);

                                    //add in firebase db. Properties -> PropertyId -> Images -> ImageId > ImageData
                                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
                                    ref.child(propertyId).child("Images")
                                            .child(imageName)
                                            .updateChildren(hashMap);
                                }

                                progressDialog.dismiss();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "onFailure: ", e);
                                progressDialog.dismiss();
                            }
                        });
            }

        }
    }

    private void loadAdDetails() {
        Log.d(TAG, "loadAdDetails: ");
        //Ad's db path to get the Ad details. Properties > PropertyId
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
        ref.child(propertyIdForEditing)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        try {
                            ModelProperty modelProperty = snapshot.getValue(ModelProperty.class);

                            //get the Ad details from firebase db, spellings should be same as in firebase db
                            String purpose = ""+ modelProperty.getPurpose();
                            String category = "" + modelProperty.getCategory();
                            String subcategory = "" + modelProperty.getSubcategory();
                            String floors = "" + modelProperty.getFloors();
                            String bedRooms = "" + modelProperty.getBedRooms();
                            String bathRooms = "" + modelProperty.getBathRooms();
                            String areaSize = "" + modelProperty.getAreaSize();
                            String areaSizeUnit = "" + modelProperty.getAreaSizeUnit();
                            String price = "" + modelProperty.getPrice();
                            String title = "" + modelProperty.getTitle();
                            String description = "" + modelProperty.getDescription();
                            String email = "" + modelProperty.getEmail();
                            String phoneCode = "" + modelProperty.getPhoneCode();
                            String phoneNumber = "" + modelProperty.getPhoneNumber();
                            String country = "" + modelProperty.getCountry();
                            String city = "" + modelProperty.getCity();
                            String address = "" +modelProperty.getAddress();
                            String status = "" + modelProperty.getStatus();
                            String timestamp = "" + modelProperty.getTimestamp();
                            latitude = modelProperty.getLatitude();
                            longitude = modelProperty.getLongitude();

                            //set data to UI Views (Form)
                            if (purpose.equals(getString(R.string.purpose_sell))) {
                                binding.purposeSellRb.setChecked(true);
                            } else if (purpose.equals(getString(R.string.purpose_rent))) {
                                binding.purposeRentRb.setChecked(true);
                            }

                            if (category.equals(getString(R.string.category_homes))) {
                                binding.propertyCategoryTabLayout.selectTab(binding.propertyCategoryTabLayout.getTabAt(0));
                            } else if (category.equals(getString(R.string.category_plots))) {
                                binding.propertyCategoryTabLayout.selectTab(binding.propertyCategoryTabLayout.getTabAt(1));
                            } else if (category.equals(getString(R.string.category_commercial))) {
                                binding.propertyCategoryTabLayout.selectTab(binding.propertyCategoryTabLayout.getTabAt(2));
                            }

                            binding.propertySubcategoryAct.setText(subcategory);

                            binding.floorsEt.setText(floors);
                            binding.bedRoomsEt.setText(bedRooms);
                            binding.bathRoomsEt.setText(bathRooms);
                            binding.areaSizeEt.setText(areaSize);
                            binding.areaSizeUnitAct.setText(areaSizeUnit);
                            binding.locationAct.setText(address);
                            binding.priceEt.setText(price);
                            binding.titleEt.setText(title);
                            binding.descriptionEt.setText(description);
                            binding.emailEt.setText(email);
                            binding.phoneNumberEt.setText(phoneNumber);
                            binding.phoneCodeTil.getTextView_selectedCountry().setText(phoneCode);

                            //Load the Ad images. Properties > PropertyId > Images
                            DatabaseReference refImages = snapshot.child("Images").getRef();
                            refImages.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    //might be multiple images so loop to get all
                                    for (DataSnapshot ds : snapshot.getChildren()) {
                                        String id = "" + ds.child("id").getValue();
                                        String imageUrl = "" + ds.child("imageUrl").getValue();


                                        ModelImagePicked modelImagePicked = new ModelImagePicked(id, null, imageUrl, true);
                                        imagePickedArrayList.add(modelImagePicked);
                                    }

                                    loadImages();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        } catch (Exception e){
                            Log.e(TAG, "onDataChange: ", e);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

}