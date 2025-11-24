package com.platinumacre.realestateapp.adapters;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.platinumacre.realestateapp.MyUtils;
import com.platinumacre.realestateapp.R;
import com.platinumacre.realestateapp.databinding.RowImagesPickedBinding;
import com.platinumacre.realestateapp.models.ModelImagePicked;

import java.util.ArrayList;

public class AdapterImagesPicked extends RecyclerView.Adapter<AdapterImagesPicked.HolderImagesPicked> {

    //View Binding
    private RowImagesPickedBinding binding;
    //Tag to show logs in logcat
    private static final String TAG = "IMAGES_TAG";

    //Context of activity/fragment from where instance of AdapterImagesPicked class is created
    private Context context;
    //imagePickedArrayList The list of the images picked/captured from Gallery/Camera or from Internet
    private ArrayList<ModelImagePicked> imagePickedArrayList;

    private String adId;

    /**Constructor*
     * @param context The context of activity/fragment from where instance of AdapterImagesPicked class is created
     * @param imagePickedArrayList The list of the images picked/captured from Gallery/Camera or from Internet
     * @param propertyId is Id of the Property (will be used to delete image from firebase and maybe for more)*/
    public AdapterImagesPicked(Context context, ArrayList<ModelImagePicked> imagePickedArrayList, String adId) {
        this.context = context;
        this.imagePickedArrayList = imagePickedArrayList;
        this.adId = adId;
    }

    @NonNull
    @Override
    public HolderImagesPicked onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate/bind the row_images_picked.xml
        binding = RowImagesPickedBinding.inflate(LayoutInflater.from(context), parent, false);

        return new HolderImagesPicked(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderImagesPicked holder, int position) {
        //get data from particular position of list and set to the UI Views of row_images_picked.xml and Handle clicks
        ModelImagePicked model = imagePickedArrayList.get(position);

        if (model.getFromInternet()){
            //Image is from internet/firebase db. Get image Url of the image to set in imageIv
            String imageUrl = model.getImageUrl();

            Log.d(TAG, "onBindViewHolder: imageUrl: "+imageUrl);

            //set the image in imageIv
            try {
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.image_gray)
                        .into(holder.imageIv);
            } catch (Exception e) {
                Log.e(TAG, "onBindViewHolder: ", e);
            }
        } else {
            //Image is picked from Gallery/Camera. Get image Uri of the image to set in imageIv
            Uri imageUri = model.getImageUri();

            Log.d(TAG, "onBindViewHolder: imageUri: "+imageUri);

            //set the image in imageIv
            try {
                Glide.with(context)
                        .load(imageUri)
                        .placeholder(R.drawable.image_gray)
                        .into(holder.imageIv);
            } catch (Exception e){
                Log.e(TAG, "onBindViewHolder: ", e);
            }

        }

        // handle closeBtn click, remove image from imagePickedArrayList
        holder.closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check if image is from Device Storage or Firebase
                if (model.getFromInternet()){
                    //image is from firebase storage, need to delete from firebase storage
                    deleteImageFirebase(model, holder, position);
                } else {
                    //image is from device storage, just remove from list.
                    imagePickedArrayList.remove(model);
                    notifyItemRemoved(position);
                }
            }
        });
    }

    private void deleteImageFirebase(ModelImagePicked model, HolderImagesPicked holder, int position) {
        //Id of the  image to delete  image
        String imageId = model.getId();

        Log.d(TAG, "deleteImageFirebase: adId: "+adId);
        Log.d(TAG, "deleteImageFirebase: imageId: "+imageId);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Properties");
        ref.child(adId).child("Images").child(imageId)
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //Delete Success
                        Log.d(TAG, "onSuccess: Deleted");
                        MyUtils.toast(context, "Image Deleted!");

                        try {
                            //Remove from imagePickedArrayList
                            imagePickedArrayList.remove(model);
                            notifyItemRemoved(position);
                        } catch (Exception e){
                            Log.e(TAG, "onSuccess: ", e);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Delete Failure
                        Log.e(TAG, "onFailure: ", e);
                        MyUtils.toast(context, "Failed to delete image due to "+e.getMessage());
                    }
                });
    }

    @Override
    public int getItemCount() {
        return imagePickedArrayList.size(); //return the size of list
    }

    /** View holder class to hold/init UI Views of the row_images_picked.xml*/
    class HolderImagesPicked extends RecyclerView.ViewHolder{

        //UI Views of the row_images_picked.xml
        ImageView imageIv;
        ImageButton closeBtn;

        public HolderImagesPicked(@NonNull View itemView) {
            super(itemView);

            //init UI Views of the row_images_picked.xml
            imageIv = binding.imageIv;
            closeBtn = binding.closeBtn;
        }
    }
}
