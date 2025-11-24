package com.platinumacre.realestateapp.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.platinumacre.realestateapp.R;
import com.platinumacre.realestateapp.databinding.RowImageSliderBinding;
import com.platinumacre.realestateapp.models.ModelImageSlider;

import java.util.ArrayList;

public class AdapterImageSlider extends RecyclerView.Adapter<AdapterImageSlider.HolderImageSlider> {

    //View Binding
    private RowImageSliderBinding binding;


    private static final String TAG = "IMAGE_SLIDER_TAG";

    //Context of activity/fragment from where instance of AdapterAd class is created
    private Context context;

    //imageSliderArrayList The list of the images
    private ArrayList<ModelImageSlider> imageSliderArrayList;

    /**
     * Constructor*
     *
     * @param context     The context of activity/fragment from where instance of AdapterAd class is created *
     * @param imageSliderArrayList The list of images
     */
    public AdapterImageSlider(Context context, ArrayList<ModelImageSlider> imageSliderArrayList) {
        this.context = context;
        this.imageSliderArrayList = imageSliderArrayList;
    }

    @NonNull
    @Override
    public HolderImageSlider onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate/bind the row_image_slider.xml
        binding = RowImageSliderBinding.inflate(LayoutInflater.from(context), parent, false);

        return new HolderImageSlider(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderImageSlider holder, int position) {
        //get data from particular position of list and set to the UI Views of row_image_slider.xml and Handle clicks
        ModelImageSlider modelImageSlider = imageSliderArrayList.get(position);

        //get url of the image
        String imageUrl = modelImageSlider.getImageUrl();
        //Show current image/total images e.g. 1/3 here 1 is current image and 3 is total images
        String imageCount = (position + 1) +"/" +imageSliderArrayList.size();

        //set image count
        holder.imageCountTv.setText(imageCount);
        //set image
        try {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.image_gray)
                    .into(holder.imageIv);
        } catch (Exception e){
            Log.e(TAG, "onBindViewHolder: ", e);
        }

        //handle image click, open in full screen e.g. ImageViewActivity
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

    }

    @Override
    public int getItemCount() {
        //return the size of list
        return imageSliderArrayList.size();
    }


    class HolderImageSlider extends RecyclerView.ViewHolder{

        //UI Views of the row_ad.xml
        ShapeableImageView imageIv;
        TextView imageCountTv;

        public HolderImageSlider(@NonNull View itemView) {
            super(itemView);

            //init UI Views of the row_ad.xml
            imageIv = binding.imageIv;
            imageCountTv = binding.imageCountTv;
        }
    }
}
