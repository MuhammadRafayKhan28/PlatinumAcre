package com.platinumacre.realestateapp.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.platinumacre.realestateapp.R;
import com.platinumacre.realestateapp.MyUtils;
import com.platinumacre.realestateapp.models.ModelChat;

import java.util.ArrayList;

public class AdapterChat extends RecyclerView.Adapter<AdapterChat.HolderChat> {

    private static final String TAG = "ADAPTER_CHAT_TAG";

    //Context of activity/fragment from where instance of AdapterChat class is created
    private Context context;
    //chatArrayList The list of the messages
    private ArrayList<ModelChat> chatArrayList;
    //a constant to indicate, the left/receipt ui i.e. row_chat_left.xml
    private static final int MSG_TYPE_LEFT = 0;
    //a constant to indicate, the right/current-user ui i.e. row_chat_right.xml
    private static final int MSG_TYPE_RIGHT = 1;

    //To get currently signed in user
    private FirebaseUser firebaseUser;

    public AdapterChat(Context context, ArrayList<ModelChat> chatArrayList) {
        this.context = context;
        this.chatArrayList = chatArrayList;

        //get currently signed in user
        firebaseUser  = FirebaseAuth.getInstance().getCurrentUser();
    }

    @NonNull
    @Override
    public HolderChat onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate the layouts row_chat_left.xml and row_chat_right.xml
        if (viewType == MSG_TYPE_RIGHT){
            //based on condition implemented in getItemViewType() the UI type is row_chat_right.xml (message by currently logged-in user)
            View view = LayoutInflater.from(context).inflate(R.layout.row_chat_right, parent, false);

            return new HolderChat(view);
        } else {
            //based on condition implemented in getItemViewType() the UI type is row_chat_left.xml (message by receipt user)
            View view = LayoutInflater.from(context).inflate(R.layout.row_chat_left, parent, false);

            return new HolderChat(view);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull HolderChat holder, int position) {
        //get data from particular position of list and set to the UI Views of row_chat_left.xml and row_chat_right.xml and Handle clicks

        ModelChat modelChat = chatArrayList.get(position);
        //get data
        String message  = modelChat.getMessage(); //contains text message in case of message type TEXT. contains image url in case of message type IMAGE
        String messageType  = modelChat.getMessageType();
        long timestamp  = modelChat.getTimestamp();
        //format date time e.g. dd/MM/yyyy hh:mm:a (03/08/2023 08:30 AM)
        String formattedDate = MyUtils.formatTimestampDateTime(timestamp);


        if (messageType.equals(MyUtils.MESSAGE_TYPE_TEXT)){
            //Message type is TEXT. Show messageTv and hide imageIv
            holder.messageTv.setVisibility(View.VISIBLE);
            holder.imageIv.setVisibility(View.GONE);

            //set text message to TextView i.e. messageTv
            holder.messageTv.setText(message);
        } else {
            //Message type is IMAGE. Hide messageTv and show imageIv
            holder.messageTv.setVisibility(View.GONE);
            holder.imageIv.setVisibility(View.VISIBLE);

            //set image to ImageView i.e. imageIv
            try {
                Glide.with(context)
                        .load(message)
                        .placeholder(R.drawable.image_gray)
                        .error(R.drawable.image_broken_gray)
                        .into(holder.imageIv);
            } catch (Exception e){
                Log.e(TAG, "onBindViewHolder: ", e);
            }
        }

        holder.timeTv.setText(formattedDate);
    }

    @Override
    public int getItemCount() {
        //return the size of list | number of items in list
        return chatArrayList.size();
    }

    @Override
    public int getItemViewType(int position) {
        //if the fromUid == current_user_uid then message is by currently logged-in user otherwise message is from receipt
        if (chatArrayList.get(position).getFromUid().equals(firebaseUser.getUid())){
            //fromUid == current_user_uid, message is by currently logged-in user, will show row_chat_right.xml
            return MSG_TYPE_RIGHT;
        } else {
            //fromUid != current_user_uid, message is by receipt user, will show row_chat_left.xml
            return MSG_TYPE_LEFT;
        }
    }

    class HolderChat extends RecyclerView.ViewHolder{
        //UI Views of the row_chat_left.xml & row_chat_right.xml
        TextView messageTv, timeTv;
        ImageView imageIv;

        public HolderChat(@NonNull View itemView) {
            super(itemView);
            //init UI Views of the row_chat_left.xml & row_chat_right.xml
            messageTv = itemView.findViewById(R.id.messageTv);
            imageIv = itemView.findViewById(R.id.imageIv);
            timeTv = itemView.findViewById(R.id.timeTv);
        }
    }
}
