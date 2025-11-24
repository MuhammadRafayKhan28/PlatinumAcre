package com.platinumacre.realestateapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.platinumacre.realestateapp.R;
import com.platinumacre.realestateapp.MyUtils;
import com.platinumacre.realestateapp.activities.PropertyDetailsActivity;
import com.platinumacre.realestateapp.databinding.RowPropertyBinding;
import com.platinumacre.realestateapp.models.ModelProperty;

import java.util.ArrayList;

public class AdapterProperty extends RecyclerView.Adapter<AdapterProperty.HolderProperty> implements Filterable {

    //View Binding
    private RowPropertyBinding binding;

    private static final String TAG = "ADAPTER_AD_TAG";
    //Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;
    //Context of activity/fragment from where instance of AdapterAd class is created
    private Context context;
    //adArrayList The list of the Ads
    public ArrayList<ModelProperty> adArrayList;
    private ArrayList<ModelProperty> filterList;

    private Filter filter;

    /**
     * Constructor*
     *
     * @param context     The context of activity/fragment from where instance of AdapterAd class is created *
     * @param adArrayList The list of ads
     */
    public AdapterProperty(Context context, ArrayList<ModelProperty> adArrayList) {
        this.context = context;
        this.adArrayList = new ArrayList<>(adArrayList); // working list
        this.filterList = new ArrayList<>(adArrayList); // original list to reset

        //get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public HolderProperty onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate/bind the row_ad.xml
        binding = RowPropertyBinding.inflate(LayoutInflater.from(context), parent, false);

        return new HolderProperty(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderProperty holder, int position) {
        //get data from particular position of list and set to the UI Views of row_ad.xml and Handle clicks
        ModelProperty modelAd = adArrayList.get(position);

        String title = modelAd.getTitle();
        String description = modelAd.getDescription();
        String address = modelAd.getAddress();
        String purpose = modelAd.getPurpose();
        String category = modelAd.getCategory();
        String subcategory = modelAd.getSubcategory();
        String status = modelAd.getStatus();
        double price = modelAd.getPrice();
        long timestamp = modelAd.getTimestamp();
        String formattedDate = MyUtils.formatTimestampDate(timestamp);

        //function call: load first image from available images of Ad e.g. if there are 5 images of Ad, load first one
        loadAdFirstImage(modelAd, holder);

        //if user is logged in then check that if the Ad is in favorite of current user
        if (firebaseAuth.getCurrentUser() != null) {
            checkIsFavorite(modelAd, holder);
        }

        //Show/Hide Status Sold
        if (status.equalsIgnoreCase(MyUtils.AD_STATUS_SOLD)){
            holder.soldCv.setVisibility(View.VISIBLE);
        } else {
            holder.soldCv.setVisibility(View.GONE);
        }

        //set data to UI Views of row_ad.xml
        holder.titleTv.setText(title);
        holder.descriptionTv.setText(description);
        holder.purposeTv.setText(purpose);
        holder.categoryTv.setText(category);
        holder.subcategoryTv.setText(subcategory);
        holder.addressTv.setText(address);
        holder.dateTv.setText(formattedDate);
        holder.priceTv.setText(MyUtils.formatCurrency(price));

        //handle itemView (i.e. Ad) click, open the AdDetailsActivity. also pass the id of the Ad to intent to load details
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PropertyDetailsActivity.class);
                intent.putExtra("propertyId", modelAd.getId());
                context.startActivity(intent);
            }
        });

        //handle favBtn click, add/remove the ad to/from favorite of current user
        holder.favoriteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check if ad is in favorite of current user or not - true/false
                boolean favorite = modelAd.isFavorite();
                if (favorite) {
                    //this Ad is in favorite of current user, remove from favorite
                    MyUtils.removeFromFavorite(context, modelAd.getId());
                } else {
                    //this Ad is not in favorite of current user, add to favorite
                    MyUtils.addToFavorite(context, modelAd.getId());
                }
            }
        });

    }

    private void checkIsFavorite(ModelProperty modelAd, HolderProperty holder) {
        //DB path to check if Ad is in Favorite of current user. Users > uid > Favorites > adId
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Favorites").child(modelAd.getId())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //if snapshot exists (value is true) means the Ad is in favorite of current user otherwise no
                        boolean favorite = snapshot.exists();
                        //set that value (true/false) to model
                        modelAd.setFavorite(favorite);
                        //check if favorite or not to set image of favBtn accordingly
                        if (favorite) {
                            //Favorite, set image ic_fav_yes to button favBtn
                            holder.favoriteBtn.setImageResource(R.drawable.fav_yes_black);
                        } else {
                            //Not Favorite, set image ic_fav_no to button favBtn
                            holder.favoriteBtn.setImageResource(R.drawable.fav_no_black);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadAdFirstImage(ModelProperty modelAd, HolderProperty holder) {
        Log.d(TAG, "loadAdFirstImage: ");
        //load first image from available images of Ad e.g. if there are 5 images of Ad, load first one
        //Ad id to get image of it
        String adId = modelAd.getId();
        //
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Properties");
        reference.child(adId).child("Images").limitToFirst(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //this will return only 1 image as we have used query .limitToFirst(1)
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            //get url of the image
                            String imageUrl = "" + ds.child("imageUrl").getValue();
                            Log.d(TAG, "onDataChange: imageUrl: " + imageUrl);
                            //set image to Image Vew i.e. imageIv
                            try {
                                Glide.with(context)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.building_asset01)
                                        .into(holder.propertyIv);
                            } catch (Exception e) {
                                Log.e(TAG, "onDataChange: ", e);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    public int getItemCount() {
        //return the size of list
        return adArrayList.size();
    }

    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    ArrayList<ModelProperty> filteredList = new ArrayList<>();

                    if (constraint == null || constraint.length() == 0) {
                        // No search query; return originalList
                        filteredList.addAll(filterList);
                    } else {
                        String searchQuery = constraint.toString().toLowerCase().trim();
                        for (ModelProperty property : filterList) {
                            //We will search based on title, description, category, subcategory
                            String title = property.getTitle().toLowerCase();
                            String description = property.getDescription().toLowerCase();
                            String category = property.getCategory().toLowerCase();
                            String subcategory = property.getSubcategory().toLowerCase();


                            if (title.contains(searchQuery)
                                    || description.contains(searchQuery)
                                    || category.contains(searchQuery)
                                    || subcategory.contains(searchQuery)) {
                                //Filter success, add to filteredList
                                filteredList.add(property);
                            }
                        }
                    }

                    results.values = filteredList;
                    results.count = filteredList.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    adArrayList.clear();
                    adArrayList.addAll((ArrayList<ModelProperty>) results.values);
                    notifyDataSetChanged();
                }
            };
        }
        return filter;
    }

    class HolderProperty extends RecyclerView.ViewHolder {

        //UI Views of the row_ad.xml
        ShapeableImageView propertyIv;
        TextView priceTv, titleTv, descriptionTv, purposeTv, categoryTv, subcategoryTv, addressTv, dateTv;
        ImageButton favoriteBtn;
        MaterialCardView soldCv;

        public HolderProperty(@NonNull View itemView) {
            super(itemView);

            //init UI Views of the row_ad.xml
            propertyIv = binding.propertyIv;
            titleTv = binding.titleTv;
            descriptionTv = binding.descriptionTv;
            purposeTv = binding.purposeTv;
            categoryTv = binding.categoryTv;
            subcategoryTv = binding.subcategoryTv;
            addressTv = binding.addressTv;
            dateTv = binding.dateTv;
            priceTv = binding.priceTv;
            favoriteBtn = binding.favoriteBtn;
            soldCv = binding.soldCv;
        }
    }
}
