package com.platinumacre.realestateapp.activities;

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

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import com.platinumacre.realestateapp.adapters.AdapterChat;
import com.platinumacre.realestateapp.databinding.ActivityChatBinding;
import com.platinumacre.realestateapp.models.ModelChat;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    //View Binding
    private ActivityChatBinding binding;
    //TAG for logs in logcat
    private static final String TAG = "CHAT_TAG";
    //Progress dialog to show while sending message
    private ProgressDialog progressDialog;
    //Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;
    //UID of the receipt, will get from intent
    private String receiptUid = "";
    private String receiptFcmToke = "";
    //UID of the  current user
    private String myUid = "";
    private String myName = "";

    //Will generate using UIDs of current user and recipt
    private String chatPath = "";
    //Uri of the image picked from  Camera/Gallery
    private Uri imageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        //init view binding... activity_chat.xml = ActivityChatBinding
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        //Firebase Auth for auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        //init/setup ProgressDialog to show while sending message
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //get the uid of the receipt (as we passed in ChatActivity class while starting this activity)
        receiptUid = getIntent().getStringExtra("receiptUid");
        //get uid of current signed-in user
        myUid = firebaseAuth.getUid();
        //chat path
        chatPath = MyUtils.chatPath(receiptUid, myUid);
        Log.d(TAG, "onCreate: receiptUid: " + receiptUid);
        Log.d(TAG, "onCreate: myUid: " + myUid);
        Log.d(TAG, "onCreate: chatPath: " + chatPath);

        loadMyInfo();
        loadReceiptDetails();
        loadMessages();

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //handle attachFab click, show image pick dialog
        binding.attachFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imagePickDialog();
            }
        });

        //handle sendBtn click, validate data before sending text message
        binding.sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });

    }

    private void loadMyInfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child("" + firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        myName = "" + snapshot.child("name").getValue();
                        Log.d(TAG, "onDataChange: myName: " + myName);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadReceiptDetails() {
        Log.d(TAG, "loadReceiptDetails: ");
        //Database reference to load receipt user info
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(receiptUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        try {
                            //get user data, Note: Spellings must be same as in firebase db
                            String name = "" + snapshot.child("name").getValue();
                            String profileImageUrl = "" + snapshot.child("profileImageUrl").getValue();
                            receiptFcmToke = "" + snapshot.child("fcmToken").getValue();

                            Log.d(TAG, "onDataChange: name: " + name);
                            Log.d(TAG, "onDataChange: profileImageUrl: " + profileImageUrl);

                            //set user name
                            binding.toolbarTitleTv.setText(name);
                            //set user profile image
                            try {
                                Glide.with(ChatActivity.this)
                                        .load(profileImageUrl)
                                        .placeholder(R.drawable.person_white)
                                        .error(R.drawable.image_broken_gray)
                                        .into(binding.toolbarProfileIv);
                            } catch (Exception e) {
                                Log.e(TAG, "onDataChange: ", e);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "onDataChange: ", e);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


    private void loadMessages() {
        Log.d(TAG, "loadMessages: ");
        //init chat arraylist
        ArrayList<ModelChat> chatArrayList = new ArrayList<>();

        //Db reference to load chat messages
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Chats");
        ref.child(chatPath)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //clear adArrayList each time starting adding data into it
                        chatArrayList.clear();
                        //load messages list
                        for (DataSnapshot ds : snapshot.getChildren()) {

                            try {
                                //Prepare ModelChat with all data from Firebase DB
                                ModelChat modelChat = ds.getValue(ModelChat.class);
                                //add prepared model to adArrayList
                                chatArrayList.add(modelChat);
                            } catch (Exception e) {
                                Log.e(TAG, "loadMessages:onDataChange: ", e);
                            }
                        }

                        //init/setup AdapterChat class and set to recyclerview
                        AdapterChat adapterChat = new AdapterChat(ChatActivity.this, chatArrayList);
                        binding.chatRv.setAdapter(adapterChat);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void imagePickDialog() {
        //init popup menu param 1 is context and param 2 is the UI View (attachFab) to above/below we need to show popup menu
        PopupMenu popupMenu = new PopupMenu(this, binding.attachFab);
        //add menu items to our popup menu Param#1 is GroupID, Param#2 is ItemID, Param#3 is OrderID, Param#4 is Menu Item Title
        popupMenu.getMenu().add(Menu.NONE, 1, 1, "Camera");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "Gallery");
        //Show Popup Menu
        popupMenu.show();
        //handle popup menu item click
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                //get the id of the menu item clicked
                int itemId = item.getItemId();
                if (itemId == 1) {
                    //Camera is clicked we need to check if we have permission of Camera, Storage before launching Camera to Capture image
                    Log.d(TAG, "onMenuItemClick: Camera click,  check if camera  permissions  are  granted  or not");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        //Device version is TIRAMISU or above. We only need Camera permission
                        requestCameraPermissions.launch(new String[]{Manifest.permission.CAMERA});
                    } else {
                        //Device version is below TIRAMISU. We need Camera & Storage permissions
                        requestCameraPermissions.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE});
                    }
                } else if (itemId == 2) {
                    //Gallery is clicked we need to check if we have permission of Storage before launching Gallery to Pick image
                    Log.d(TAG, "onMenuItemClick: Check if storage permission is granted or not");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        //Device version is TIRAMISU or above. We don't need Storage permission to launch Gallery
                        pickImageGallery();
                    } else {
                        //Device version is below TIRAMISU. We need Storage permission to launch Gallery
                        requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }
                }

                return true;
            }
        });
    }

    private ActivityResultLauncher<String[]> requestCameraPermissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    //let's check if permissions are granted or not
                    Log.d(TAG, "onActivityResult: " + result);

                    boolean areAllGranted = true;
                    for (Boolean isGranted : result.values()) {

                        areAllGranted = areAllGranted && isGranted;
                    }

                    if (areAllGranted) {
                        //All Permissions Camera, Storage are granted, we can now launch camera to capture image
                        Log.d(TAG, "onActivityResult: ");
                        pickImageCamera();
                    } else {
                        //Camera or Storage or Both permissions are denied, Can't launch camera to capture image
                        Log.d(TAG, "onActivityResult: Camera or Storage or both permissions denied...!");
                        MyUtils.toast(ChatActivity.this, "Camera or Storage or both permissions denied...!");
                    }
                }
            }
    );

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
                        //Storage Permission dennied, we can't launch gallery to pick image
                        MyUtils.toast(ChatActivity.this, "Permission denied...!");
                    }
                }
            }
    );

    private void pickImageCamera() {
        Log.d(TAG, "pickImageCamera: ");
        //Setup Content values, MediaStore to capture high quality image using camera intent
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "CHAT_IMAGE_TEMP");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "CHAT_IMAGE_TEMP_DESCRIPTION");
        //store the camera image in imageUri variable
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        //Intent to launch camera
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);

    }

    private ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //Check if image is captured or not
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        //Image Captured, we have image in imageUri as asigned in pickImageCamera()
                        Log.d(TAG, "onActivityResult: imageUri: " + imageUri);
                        //image picked, let's upload/send
                        uploadToFirebaseStorage();
                    } else {
                        //Cancelled
                        MyUtils.toast(ChatActivity.this, "Cancelled...!");
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

    private ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //Check if image is picked or not
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        //get data
                        Intent data = result.getData();
                        //get uri of image picked
                        imageUri = data.getData();
                        Log.d(TAG, "onActivityResult: imageUri: " + imageUri);
                        //image picked, let's upload/send
                        uploadToFirebaseStorage();
                    } else {
                        //Cancelled
                        MyUtils.toast(ChatActivity.this, "Cancelled...!");
                    }
                }
            }
    );

    private void uploadToFirebaseStorage() {
        Log.d(TAG, "uploadToFirebaseStorage: ");
        //show progress
        progressDialog.setMessage("Uploading image...!");
        progressDialog.show();
        //get timestamp for image name, and timestamp of message
        long timestamp = MyUtils.getTimestamp();
        //file path and name
        String filePathAndName = "ChatImages/" + timestamp;
        //Storage refrence to upload image
        StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
        storageReference.putFile(imageUri)
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                        //get current progress of image being uploaded
                        double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                        //set/update image upload progress to progress dialog
                        progressDialog.setMessage("Uploading image. Progress: " + (int) progress + "%");
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //Image uploaded successfully, get url of uploaded image
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();

                        while (!uriTask.isSuccessful()) ;
                        //Url of image uploaded to firebase storage
                        String imageUrl = uriTask.getResult().toString();

                        if (uriTask.isSuccessful()) {
                            sendMessage(MyUtils.MESSAGE_TYPE_IMAGE, imageUrl, timestamp);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(ChatActivity.this, "Failed to upload due to " + e.getMessage());
                    }
                });
    }

    private void validateData() {
        Log.d(TAG, "validateData: ");
        //input data
        String message = binding.messageEt.getText().toString().trim();
        long timestamp = MyUtils.getTimestamp();

        //validate data
        if (message.isEmpty()) {
            //No message entered, can't send
            MyUtils.toast(this, "Enter message to send...");
        } else {
            //Message entered, send
            sendMessage(MyUtils.MESSAGE_TYPE_TEXT, message, timestamp);
        }
    }

    private void sendMessage(String messageType, String message, long timestamp) {
        Log.d(TAG, "sendMessage: messageType: " + messageType);
        Log.d(TAG, "sendMessage: message: " + message);
        Log.d(TAG, "sendMessage: timestamp: " + timestamp);

        //show progress
        progressDialog.setMessage("Sending message...!");
        progressDialog.show();

        //Database reference of Chats
        DatabaseReference refChat = FirebaseDatabase.getInstance().getReference("Chats");
        //key id to be used as message id
        String keyId = "" + refChat.push().getKey();

        //setup chat data in hashmap to add in firebase db
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("messageId", "" + keyId);
        hashMap.put("messageType", "" + messageType);
        hashMap.put("message", "" + message);
        hashMap.put("fromUid", "" + myUid);
        hashMap.put("toUid", "" + receiptUid);
        hashMap.put("timestamp", timestamp);

        //add chat data to firebase db
        refChat.child(chatPath)
                .child(keyId)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //Message successfully sent/added
                        binding.messageEt.setText("");
                        progressDialog.dismiss();

                        //If message type is TEXT, pass the actual message to show as Notification description/body. If message type is IMAGE then pass "Sent an attachment"
                        if (messageType.equals(MyUtils.MESSAGE_TYPE_TEXT)) {
                            prepareNotification(message);
                        } else {
                            prepareNotification("Sent an attachment");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Failed to send/add message
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        MyUtils.toast(ChatActivity.this, "Failed to send due to " + e.getMessage());
                    }
                });
    }

    private void prepareNotification(String message) {
        Log.d(TAG, "prepareNotification: ");
        //prepare json what to send, and where to send
        JSONObject notificationJo = new JSONObject();
        JSONObject notificationDataJo = new JSONObject();
        JSONObject notificationNotificationJo = new JSONObject();


        try {
            //extra/custom data
            notificationDataJo.put("notificationType", "" + MyUtils.NOTIFICATION_TYPE_NEW_MESSAGE);
            notificationDataJo.put("senderUid", "" + firebaseAuth.getUid());
            //title, description, sound
            notificationNotificationJo.put("title", "" + myName); //"title" is reserved name in FCM API so be careful while typing
            notificationNotificationJo.put("body", "" + message); //"body" is reserved name in FCM API so be careful while typing
            notificationNotificationJo.put("sound", "default"); //"sound" is reserved name in FCM API so be careful while typing
            //combine all data in single JSON object
            notificationJo.put("to", "" + receiptFcmToke); //"to" is reserved name in FCM API so be careful while typing
            notificationJo.put("notification", notificationNotificationJo); //"notification" is reserved name in FCM API so be careful while typing
            notificationJo.put("data", notificationDataJo);  //"data" is reserved name in FCM API so be careful while typing
        } catch (Exception e) {
            Log.e(TAG, "prepareNotification: ", e);
        }

        sendFcmNotification(notificationJo);
    }

    private void sendFcmNotification(JSONObject notificationJo) {
        //Prepare JSON Object Request to enqueue
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                "https://fcm.googleapis.com/fcm/send",
                notificationJo,
                response -> {
                    //Notification sent
                    Log.d(TAG, "sendFcmNotification: " + response.toString());
                },
                error -> {
                    //Notification failed to send
                    Log.e(TAG, "sendFcmNotification: ", error);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                //put required headers
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json"); //"Content-Type" is reserved name in Volley Networking API/Library
                headers.put("Authorization", "key=" + MyUtils.FCM_SERVER_KEY); //"Authorization" is reserved name in Volley Networking API/Library, value against it must be like "key=fcm_server_key_here"

                return headers;
            }
        };

        //enqueue the JSON Object Request
        Volley.newRequestQueue(this).add(jsonObjectRequest);

    }


}