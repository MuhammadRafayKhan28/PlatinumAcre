package com.platinumacre.realestateapp.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.platinumacre.realestateapp.adapters.AdapterChats;
import com.platinumacre.realestateapp.databinding.FragmentChatsListBinding;
import com.platinumacre.realestateapp.models.ModelChats;

import java.util.ArrayList;
import java.util.Collections;

public class ChatsListFragment extends Fragment {
    //View Binding
    private FragmentChatsListBinding binding;
    //TAG to show logs in logcat
    private static final String TAG = "CHATS_TAG";
    //Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;

    private String myUid;
    //Context for this fragment class
    private Context mContext;
    //chatsArrayList to hold chats list by currently logged-in user to show in RecyclerView
    private ArrayList<ModelChats> chatsArrayList;
    //AdapterChats class instance to set to Recyclerview to show chats list
    private AdapterChats adapterChats;

    @Override
    public void onAttach(@NonNull Context context) {
        //get and init the context for this fragment class
        this.mContext = context;
        super.onAttach(context);
    }

    public ChatsListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate/bind the layout (fragment_chats_list.xml) for this fragment
        binding = FragmentChatsListBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        //Firebase Auth for auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();
        //uid of the currently logged-in user
        myUid = firebaseAuth.getUid();

        Log.d(TAG, "onViewCreated: myUid: "+myUid);

        loadChats();

        //add text change listener to searchEt to search chats using filter applied in AdapterChats class
        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //this function is called whenever user type a letter, search based on what user typed
                try {

                    String query = s.toString();
                    adapterChats.getFilter().filter(query);
                } catch (Exception e){
                    Log.e(TAG, "onTextChanged: ", e);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void loadChats(){
        //init chatsArrayList before starting adding data into it
        chatsArrayList = new ArrayList<>();

        //Firebase DB listener to get the chats of logged-in user.
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Chats");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //clear chatsArrayList each time starting adding data into it
                chatsArrayList.clear();
                //load chats, we only need chatKey e.g. uid1_uid2 here, we have to get (already done) the chat data, and receipt user data in adapter class
                for (DataSnapshot ds: snapshot.getChildren()){
                    //The chat key e.g. uid1_uid2
                    String chatKey = "" + ds.getKey();

                    Log.d(TAG, "onDataChange: chatKey: "+chatKey);
                    //if chat key uid1_uid2 contains the uid of currently logged-in user will be considered as chat of currently logged-in user
                    if (chatKey.contains(myUid)){
                        Log.d(TAG, "onDataChange: Contains");
                        //Create instance of ModelChats and add the chatKey in it
                        ModelChats modelChats = new ModelChats();
                        modelChats.setChatKey(chatKey);

                        //add the instance of ModelChats in chatsArrayList
                        chatsArrayList.add(modelChats);
                    } else {
                        Log.d(TAG, "onDataChange: Not Contains");
                    }
                }

                //init/setup adapter class and set to recyclerview
                adapterChats = new AdapterChats(mContext, chatsArrayList);
                binding.chatsRv.setAdapter(adapterChats);

                //after loading data in list we  will sort the list using timestamp of each last message of chat, to show the newest chat first
                sort();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sort(){
        //Delay of 1 second before sorting the list
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //sort chatsArrayList
                Collections.sort(chatsArrayList, (model1, model2) -> Long.compare(model2.getTimestamp(), model1.getTimestamp()));
                //notify changes
                adapterChats.notifyDataSetChanged();
            }
        }, 1000);

    }

}