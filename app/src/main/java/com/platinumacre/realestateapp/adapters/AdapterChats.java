package com.platinumacre.realestateapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.platinumacre.realestateapp.filters.FilterChats;
import com.platinumacre.realestateapp.R;
import com.platinumacre.realestateapp.MyUtils;
import com.platinumacre.realestateapp.activities.ChatActivity;
import com.platinumacre.realestateapp.databinding.RowChatsBinding;
import com.platinumacre.realestateapp.models.ModelChats;

import java.util.ArrayList;

public class AdapterChats extends RecyclerView.Adapter<AdapterChats.HolderChats> implements Filterable {

    //View Binding
    private RowChatsBinding binding;

    private static final String TAG = "ADAPTER_CHATS_TAG";
    //Context of activity/fragment from where instance of AdapterChats class is created
    private Context context;
    //chatsArrayList The list of the chats
    public ArrayList<ModelChats> chatsArrayList;
    private ArrayList<ModelChats> filterList;

    private FilterChats filter;


    private FirebaseAuth firebaseAuth;

    private String myUid;

    /**
     * Constructor*
     *
     * @param context     The context of activity/fragment from where instance of AdapterChats class is created *
     * @param chatsArrayList The list of chats
     */
    public AdapterChats(Context context, ArrayList<ModelChats> chatsArrayList) {
        this.context = context;
        this.chatsArrayList = chatsArrayList;
        this.filterList = chatsArrayList;


        firebaseAuth = FirebaseAuth.getInstance();

        myUid = firebaseAuth.getUid();
    }

    @NonNull
    @Override
    public HolderChats onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate/bind the row_chats.xml
        binding = RowChatsBinding.inflate(LayoutInflater.from(context), parent, false);

        return new HolderChats(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderChats holder, int position) {
        //get data from particular position of list and set to the UI Views of row_chats.xml and Handle clicks
        ModelChats modelChats = chatsArrayList.get(position);


        loadLastMessage(modelChats, holder);

        //handle chat item click, open ChatActivity
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String receiptUid  = modelChats.getReceiptUid();

                if (receiptUid != null){
                    Intent intent = new Intent(context, ChatActivity.class);
                    intent.putExtra("receiptUid", receiptUid);
                    context.startActivity(intent);
                }
            }
        });
    }

    private void loadLastMessage(ModelChats modelChats, HolderChats holder){
        String chatKey = modelChats.getChatKey();

        Log.d(TAG, "loadLastMessage: chatKey: "+chatKey);
        //Database reference to load last message info e.g. Chats > ChatKey > LastMessage
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Chats");
        ref.child(chatKey).limitToLast(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        for (DataSnapshot ds: snapshot.getChildren()){
                            //Get message data, spellings and data type must be same as in firebase db
                            String fromUid = ""+ds.child("fromUid").getValue();
                            String message = ""+ds.child("message").getValue();
                            String messageId = ""+ds.child("messageId").getValue();
                            String messageType = ""+ds.child("messageType").getValue();
                            long timestamp = (Long) ds.child("timestamp").getValue();
                            String toUid = ""+ ds.child("toUid").getValue();
                            //format message timestamp to proper date and time format e.g. 19/08/2023 09:30 AM
                            String formattedDate = MyUtils.formatTimestampDateTime(timestamp);

                            //set data to current instance of ModelChats using setters
                            modelChats.setMessage(message);
                            modelChats.setMessageId(messageId);
                            modelChats.setMessageType(messageType);
                            modelChats.setFromUid(fromUid);
                            modelChats.setTimestamp(timestamp);
                            modelChats.setToUid(toUid);

                            //set formatted date and time
                            holder.dateTimeTv.setText(formattedDate);

                            //check message type
                            if (messageType.equals(MyUtils.MESSAGE_TYPE_TEXT)){
                                //message type is TEXT, set last message
                                holder.lastMessageTv.setText(message);
                            } else {
                                //message type is IMAGE, just set hardcoded string e.g. Sends Attachment
                                holder.lastMessageTv.setText("Sends Attachment");
                            }
                        }

                        loadReceiptUserInfo(modelChats, holder);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }

    private void loadReceiptUserInfo(ModelChats modelChats, HolderChats holder) {
        String fromUid = modelChats.getFromUid();
        String toUid = modelChats.getToUid();
        //To identify either fromUid or toUid is the UID of the receipt we need to validate e.g. if fromUid == UID_OF_CURRENT_USER then receiptUid = toUid
        String receiptUid;
        if (fromUid.equals(myUid)){
            //fromUid = UID_OF_CURRENT_USER
            receiptUid = toUid;
        } else {
            //fromUid != UID_OF_CURRENT_USER
            receiptUid = fromUid;
        }
        //set receiptUid to current instance of ModelChats using setters
        modelChats.setReceiptUid(receiptUid);


        Log.d(TAG, "loadReceiptUserInfo: fromUid: "+fromUid);
        Log.d(TAG, "loadReceiptUserInfo: toUid: "+toUid);
        Log.d(TAG, "loadReceiptUserInfo: receiptUid: "+receiptUid);

        //Database reference to load receipt user info
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(receiptUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //Get message data, spellings and data type must be same as in firebase db
                        String name = ""+ snapshot.child("name").getValue();
                        String profileImageUrl = ""+ snapshot.child("profileImageUrl").getValue();

                        //set data to current instance of ModelChats using setters
                        modelChats.setName(name);
                        modelChats.setProfileImageUrl(profileImageUrl);

                        //set/show receipt name and profile image to UI
                        holder.nameTv.setText(name);
                        try {
                            Glide.with(context)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.person_white)
                                    .into(holder.profileIv);
                        } catch (Exception e){
                            Log.e(TAG, "onDataChange: ", e);
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
        return chatsArrayList.size();
    }

    @Override
    public Filter getFilter() {
        //init the filter obj only if it is null
        if (filter == null){
            //filter obj is null, init
            filter = new FilterChats(this, filterList);
        }

        return filter;
    }

    class HolderChats extends RecyclerView.ViewHolder{
        //UI Views of the row_chats.xml
        ShapeableImageView profileIv;
        TextView nameTv, lastMessageTv, dateTimeTv;

        public HolderChats(@NonNull View itemView) {
            super(itemView);

            //init UI Views of the row_chats.xml
            profileIv = binding.profileIv;
            nameTv = binding.nameTv;
            lastMessageTv = binding.lastMessageTv;
            dateTimeTv = binding.dateTimeTv;
        }

    }
}
