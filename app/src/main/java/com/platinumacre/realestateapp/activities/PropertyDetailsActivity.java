package com.platinumacre.realestateapp.activities;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.platinumacre.realestateapp.MyUtils;
import com.platinumacre.realestateapp.R;
import com.platinumacre.realestateapp.adapters.AdapterImageSlider;
import com.platinumacre.realestateapp.databinding.ActivityPropertyDetailsBinding;
import com.platinumacre.realestateapp.models.ModelImageSlider;
import com.platinumacre.realestateapp.models.ModelProperty;

import java.util.ArrayList;
import java.util.HashMap;

public class PropertyDetailsActivity extends AppCompatActivity {

    //View Binding
    private ActivityPropertyDetailsBinding binding;

    //TAG for logs in logcat
    private static final String TAG = "AD_DETAILS_TAG";

    //Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;

    //Ad id, will get from intent
    private String propertyId = "";
    //Latitude & Longitude of the Ad to view it on Map
    private double propertyLatitude = 0;
    private double propertyLongitude = 0;
    //to load seller info, chat with seller, sms and call
    private String sellerUid = null;
    private String sellerPhone = "";
    private String propertyStatus = "";

    //hold the Ad's favorite state by current user
    private boolean favorite = false;

    //list of Ad's images to show in slider
    private ArrayList<ModelImageSlider> imageSliderArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        //init view binding... activity_property_details.xml = ActivityPropertyDetailsBinding
        binding = ActivityPropertyDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        //hide some UI views in start. We will show the Edit, Delete option if the user is Ad owner. We will show Call, Chat, SMS option if user isn't Ad owner
        binding.toolbarEditBtn.setVisibility(View.GONE);
        binding.toolbarDeleteBtn.setVisibility(View.GONE);
        binding.chatBtn.setVisibility(View.GONE);
        binding.callBtn.setVisibility(View.GONE);
        binding.smsBtn.setVisibility(View.GONE);

        //get the id of the property (as we passed in AdapterProperty class while starting this activity)
        propertyId = getIntent().getStringExtra("propertyId");

        //Firebase Auth for auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        //if user is logged-in then check if the Ad is in favorites of the user
        if (firebaseAuth.getCurrentUser() != null) {
            checkIsFavorite();
        }

        loadPropertyDetailsDetails();
        loadPropertyImages();

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //handle toolbarDeleteBtn click, delete Ad
        binding.toolbarDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Alert dialog to confirm if the user really wants to delete the Ad
                MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(PropertyDetailsActivity.this);
                materialAlertDialogBuilder.setTitle("Delete Ad")
                        .setMessage("Are you sure you want to delete this Ad?")
                        .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //Delete Clicked, delete Ad
                                deleteAd();
                            }
                        })
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //Cancel Clicked, dismiss dialog
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });

        //handle toolbarEditBtn click, start AdCreateActivity to edit this Ad
        binding.toolbarEditBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editOptions();
            }
        });

        //handle toolbarFavBtn click, add/remove favorite
        binding.toolbarFavBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (favorite) {
                    //this Ad is in favorite of current user, remove from favorite
                    MyUtils.removeFromFavorite(PropertyDetailsActivity.this, propertyId);
                } else {
                    //this Ad is not in favorite of current user, add to favorite
                    MyUtils.addToFavorite(PropertyDetailsActivity.this, propertyId);
                }
            }
        });

        //handle sellerProfileCv click, start SellerProfileActivity
        binding.sellerProfileCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PropertyDetailsActivity.this, SellerProfileActivity.class);
                intent.putExtra("sellerUid", sellerUid);
                startActivity(intent);
            }
        });


        binding.chatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (firebaseAuth.getCurrentUser() == null){
                    //Not, Logged-In, Show message toast
                    MyUtils.toast(PropertyDetailsActivity.this, "Login Required...");
                    startActivity(new Intent(PropertyDetailsActivity.this, LoginOptionsActivity.class));
                } else {
                    //Logged-In, Open ChatActivity
                    Intent intent = new Intent(PropertyDetailsActivity.this, ChatActivity.class);
                    intent.putExtra("receiptUid", sellerUid);
                    startActivity(intent);
                }

            }
        });

        //handle chatBtn click, start ChatActivity
        binding.callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyUtils.callIntent(PropertyDetailsActivity.this, sellerPhone);
            }
        });

        //handle callBtn click, open Ad Creator's phone number in dialer
        binding.smsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyUtils.smsIntent(PropertyDetailsActivity.this, sellerPhone);
            }
        });

        //handle mapBtn click, open map with Ad location
        binding.mapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyUtils.mapIntent(PropertyDetailsActivity.this, propertyLatitude, propertyLongitude);
            }
        });
    }

    private void editOptions() {
        Log.d(TAG, "editOptions: ");
        //init/setup popup menu
        PopupMenu popupMenu = new PopupMenu(this, binding.toolbarEditBtn);
        //Add menu items to PopupMenu with params Group ID, Item ID, Order, Title
        popupMenu.getMenu().add(Menu.NONE, 0, 0, "Edit");
        if (propertyStatus.equalsIgnoreCase(MyUtils.AD_STATUS_AVAILABLE)) {
            popupMenu.getMenu().add(Menu.NONE, 1, 1, "Mark As Sold");
        }
        //show popup menu
        popupMenu.show();
        //handle popup menu item click
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) { //get id of the menu item clicked
                int itemId = item.getItemId();

                if (itemId == 0) {
                    //Edit Clicked, start the AdCreateActivity with Ad Id and isEditMode as true
                    Intent intent = new Intent(PropertyDetailsActivity.this, PropertyAddActivity.class);
                    intent.putExtra("isEditMode", true);
                    intent.putExtra("propertyIdForEditing", propertyId);
                    startActivity(intent);
                } else if (itemId == 1) {
                    //Mark As Sold
                    if (propertyStatus.equals(MyUtils.AD_STATUS_AVAILABLE)) {
                        showMarkAsSoldDialog();
                    } else {
                        updatePropertyAvailabilityStatus();
                    }
                }

                return true;
            }
        });
    }

    private void showMarkAsSoldDialog() {
        //Material Alert Dialog - Setup and show
        MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(this);
        alertDialogBuilder.setTitle("Mark as Sold")
                .setMessage("Are you sure you want to mark this Ad as sold?")
                .setPositiveButton("SOLD", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "onClick: Sold Clicked...");

                        //setup info to update in the existing Ad i.e. mark as sold by setting the value of status to SOLD
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("status", MyUtils.AD_STATUS_SOLD);

                        //Ad's db path to update its available/sold status. Properties > PropertyId
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
                        ref.child(propertyId)
                                .updateChildren(hashMap)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        //Success
                                        Log.d(TAG, "onSuccess: Marked as sold");
                                        MyUtils.toast(PropertyDetailsActivity.this, "Marked as sold");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        //Failure
                                        Log.e(TAG, "onFailure: ", e);
                                        MyUtils.toast(PropertyDetailsActivity.this, "Failed to mark as sold due to " + e.getMessage());
                                    }
                                });
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "onClick: Cancel Clicked...");
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void updatePropertyAvailabilityStatus() {
        //setup info to update in the existing Ad i.e. mark as sold by setting the value of status to SOLD
        HashMap<String, Object> hashMap = new HashMap<>();
        if (propertyStatus.equals(MyUtils.AD_STATUS_AVAILABLE)){
            hashMap.put("status", MyUtils.AD_STATUS_SOLD);
        } else {
            hashMap.put("status", MyUtils.AD_STATUS_AVAILABLE);
        }

        //Ad's db path to update its available/sold status. Properties > PropertyId
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
        ref.child(propertyId)
            .updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //Success
                        Log.d(TAG, "onSuccess: Marked as sold");
                        MyUtils.toast(PropertyDetailsActivity.this, "Updated");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Failure
                        Log.e(TAG, "onFailure: ", e);
                        MyUtils.toast(PropertyDetailsActivity.this, "Failed to mark as sold due to " + e.getMessage());
                    }
                });
    }

    private void loadPropertyDetailsDetails() {
        Log.d(TAG, "loadAdDetails: ");
        //Ad's db path to get the Ad details. Properties > PropertyId
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
        ref.child(propertyId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        try {
                            //setup model from firebase DataSnapshot
                            ModelProperty modelProperty = snapshot.getValue(ModelProperty.class);

                            //get data from model
                            sellerUid = modelProperty.getUid();
                            double price = modelProperty.getPrice();
                            String priceFormatted = MyUtils.formatCurrency(price);
                            long timestamp = modelProperty.getTimestamp();
                            String purpose = modelProperty.getPurpose();
                            String category = modelProperty.getCategory();
                            propertyStatus = modelProperty.getStatus();
                            String subcategory = modelProperty.getSubcategory();
                            long floors = modelProperty.getFloors();
                            long bedRooms = modelProperty.getBedRooms();
                            long bathRooms = modelProperty.getBathRooms();
                            double areaSize = modelProperty.getAreaSize();
                            String areaSizeUnit = modelProperty.getAreaSizeUnit();
                            String title = modelProperty.getTitle();
                            String description = modelProperty.getDescription();
                            String address = modelProperty.getAddress();

                            propertyLatitude = modelProperty.getLatitude();
                            propertyLongitude = modelProperty.getLongitude();

                            //format date time e.g. timestamp to dd/MM/yyyy
                            String formattedDate = MyUtils.formatTimestampDate(timestamp);

                            //check if the Property is by currently signed-in user
                            if (sellerUid.equals(firebaseAuth.getUid())) {
                                //Ad is created by currently signed-in user so
                                //1) Should be able to edit and delete Ad
                                binding.toolbarEditBtn.setVisibility(View.VISIBLE);
                                binding.toolbarDeleteBtn.setVisibility(View.VISIBLE);
                                //2) Shouldn't able to chat, call, sms (to himself), view seller profile
                                binding.chatBtn.setVisibility(View.GONE);
                                binding.callBtn.setVisibility(View.GONE);
                                binding.smsBtn.setVisibility(View.GONE);
                                binding.sellerProfileLabelTv.setVisibility(View.GONE);
                                binding.sellerProfileCv.setVisibility(View.GONE);
                            } else {
                                //Ad is not created by currently signed in user so
                                //1) Shouldn't be able to edit and delete Ad
                                binding.toolbarEditBtn.setVisibility(View.GONE);
                                binding.toolbarDeleteBtn.setVisibility(View.GONE);
                                //2) Should be able to chat, call, sms (to Ad creator), view seller profile
                                binding.chatBtn.setVisibility(View.VISIBLE);
                                binding.callBtn.setVisibility(View.VISIBLE);
                                binding.smsBtn.setVisibility(View.VISIBLE);
                                binding.sellerProfileLabelTv.setVisibility(View.VISIBLE);
                                binding.sellerProfileCv.setVisibility(View.VISIBLE);
                            }

                            //Show/Hide Status Sold
                            if (propertyStatus.equalsIgnoreCase(MyUtils.AD_STATUS_SOLD)){
                                binding.soldCv.setVisibility(View.VISIBLE);
                            } else {
                                binding.soldCv.setVisibility(View.GONE);
                            }

                            //set data to UI Views
                            binding.priceTv.setText(priceFormatted);
                            binding.dateTv.setText(formattedDate);
                            binding.purposeTv.setText(purpose);
                            binding.categoryTv.setText(category);
                            binding.subcategoryTv.setText(subcategory);
                            binding.floorsTv.setText("Floors: " + floors);
                            binding.bedsTv.setText("Bed Rooms: " + bedRooms);
                            binding.bathroomsTv.setText("Bath Rooms: " + bathRooms);
                            binding.areaSizeTv.setText("Area Size: " + areaSize + "" + areaSizeUnit);
                            binding.titleTv.setText(title);
                            binding.descriptionTv.setText(description);
                            binding.addressTv.setText(address);

                            //function call, load seller info e.g. profile image, name, member since
                            loadSellerDetails();


                        } catch (Exception e) {
                            Log.e(TAG, "onDataChange: ", e);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


    private void loadSellerDetails() {
        Log.d(TAG, "loadSellerDetails: ");
        //Db path to load seller info. Users > sellerUid
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(sellerUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get data
                        String phoneCode = "" + snapshot.child("phoneCode").getValue();
                        String phoneNumber = "" + snapshot.child("phoneNumber").getValue();
                        String name = "" + snapshot.child("name").getValue();
                        String profileImageUrl = "" + snapshot.child("profileImageUrl").getValue();
                        String timestamp = "" + snapshot.child("timestamp").getValue();
                        if (timestamp.isEmpty() || timestamp.equals("null")) {
                            timestamp = "0";
                        }
                        //format date time e.g. timestamp to dd/MM/yyyy
                        String formattedDate = MyUtils.formatTimestampDate(Long.parseLong(timestamp));
                        //phone number of seller
                        sellerPhone = phoneCode + phoneNumber;

                        //set data to UI Views
                        binding.sellerNameTv.setText(name);
                        binding.memberSinceTv.setText(formattedDate);
                        try {
                            Glide.with(PropertyDetailsActivity.this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.person_white)
                                    .into(binding.sellerProfileIv);
                        } catch (Exception e) {
                            Log.e(TAG, "onDataChange: ", e);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


    private void checkIsFavorite() {
        Log.d(TAG, "checkIsFavorite: ");
        //DB path to check if Ad is in Favorite of current user. Users > uid > Favorites > adId
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Favorites").child(propertyId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //if snapshot exists (value is true) means the Ad is in favorite of current user otherwise no
                        favorite = snapshot.exists();

                        Log.d(TAG, "onDataChange: favorite: " + favorite);
                        //check if favorite or not to set image of favBtn accordingly
                        if (favorite) {
                            //Favorite, set image ic_fav_yes to button favBtn
                            binding.toolbarFavBtn.setImageResource(R.drawable.fav_yes_black);
                        } else {
                            //Not Favorite, set image ic_fav_no to button favBtn
                            binding.toolbarFavBtn.setImageResource(R.drawable.fav_no_black);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


    private void loadPropertyImages() {
        Log.d(TAG, "loadAdImages: ");

        //init list before starting adding data into it
        imageSliderArrayList = new ArrayList<>();

        //Db path to load the Ad images. Properties > PropertyId > Images
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
        ref.child(propertyId).child("Images")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //clear list before starting adding data into it
                        imageSliderArrayList.clear();
                        //there might be multiple images, loop it to load all
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            //prepare model (spellings in model class should be same as in firebase)
                            ModelImageSlider modelImageSlider = ds.getValue(ModelImageSlider.class);
                            //add the prepared model to list
                            imageSliderArrayList.add(modelImageSlider);
                        }
                        //setup adapter and set to viewpager i.e. imageSliderVp
                        AdapterImageSlider adapterImageSlider = new AdapterImageSlider(PropertyDetailsActivity.this, imageSliderArrayList);
                        binding.imageSliderVp.setAdapter(adapterImageSlider);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


    private void deleteAd() {
        Log.d(TAG, "deleteAd: ");

        //Db path to delete the Property. Properties > PropertyId
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
        ref.child(propertyId)
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //Success
                        Log.d(TAG, "onSuccess: Deleted");
                        MyUtils.toast(PropertyDetailsActivity.this, "Deleted");
                        //finish activity and go-back
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Failure
                        Log.e(TAG, "onFailure: ", e);
                        MyUtils.toast(PropertyDetailsActivity.this, "Failed to delete due to " + e.getMessage());
                    }
                });
    }
}