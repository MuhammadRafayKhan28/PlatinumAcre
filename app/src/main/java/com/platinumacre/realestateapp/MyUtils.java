package com.platinumacre.realestateapp;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.text.format.DateFormat;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class MyUtils {

    //Constants: User Types
    public static final String USER_TYPE_GOOGLE = "Google"; //If user creates account using Google Login/SignIn method
    public static final String USER_TYPE_EMAIL = "Email"; //If user creates account using Email method
    public static final String USER_TYPE_PHONE = "Phone"; //If user creates account using Phone Login/SignIn

    public static final String MESSAGE_TYPE_TEXT = "TEXT";
    public static final String MESSAGE_TYPE_IMAGE = "IMAGE";

    public static final String NOTIFICATION_TYPE_NEW_MESSAGE = "NEW_MESSAGE";
    //TODO Change FCM SERVER KEY
    public static final String FCM_SERVER_KEY = "AAAAw_XBpjM:APA91bFLIeLnS7X7UqWDj845UjOraTGUk0ryG_FfDulKN_SgTjhweepdjQzj6ve6jJmpROG5Dx_73AoRKLbtvG4DqJCb6niu7tPGlaLzJDnpViB2nowlgk-TI_vWPniRx0CNuqRQ-6Qn";


    public static final String[] propertyTypes = {"Homes", "Plots", "Commercial"};
    public static final String[] propertyTypesHome = {"House", "Flat", "Upper Portion", "Lower Portion", "Farm House", "Room", "Penthouse"};
    public static final String[] propertyTypesPlots = {"Residential Plot", "Commercial Plot", "Agricultural Plot", "Industrial Plot", "Plot File", "Plot Form"};
    public static final String[] propertyTypesCommercial = {"Office", "Shop", "Warehouse", "Factory", "Building", "Other"};

    public static final String[] propertyAreaSizeUnit = {"Square Feet", "Square Yards", "Square Meters", "Marla", "Kanal"};

    public static final String PROPERTY_PURPOSE_ANY = "Any";
    public static final String PROPERTY_PURPOSE_SELL = "Sell";
    public static final String PROPERTY_PURPOSE_RENT = "Rent";

    public static final String AD_STATUS_AVAILABLE = "AVAILABLE";
    public static final String AD_STATUS_SOLD = "SOLD";
    public static final String AD_STATUS_RENTED = "RENTED";

    public static final int MAX_DISTANCE_TO_LOAD_PROPERTIES_KM = 100;

    /**
     * A Function to show Toast
     *
     * @param context the context of activity/fragment from where this function will be called
     * @param message the message to be shown in the Toast
     */
    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }


    /**
     * A Function to get current timestamp
     *
     * @return Return the current timestamp as long datatype
     */
    public static long getTimestamp() {
        return System.currentTimeMillis();
    }


    /**
     * A Function to show Toast
     *
     * @param timestamp the timestamp of type Long that we need to format to dd/MM/yyyy
     * @return timestamp formatted to date dd/MM/yyyy
     */
    public static String formatTimestampDate(Long timestamp) {
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(timestamp);

        String date = DateFormat.format("dd/MM/yyyy", calendar).toString();

        return date;
    }

    /**
     * A Function to show Toast
     *
     * @param timestamp the timestamp of type Long that we need to format to dd/MM/yyyy hh:mm:a
     * @return timestamp formatted to date dd/MM/yyyy hh:mm:a
     */
    public static String formatTimestampDateTime(Long timestamp) {
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(timestamp);

        String date = DateFormat.format("dd/MM/yyyy hh:mm:a", calendar).toString();

        return date;
    }


    /**
     * Currency Formatter. Formats a given number into a currency-style string,
     * respecting the user's locale forgrouping and decimal separators, but without
     * applying a currency symbol.
     *
     * @param currency number in double form to be converted
     * @return return the formatted currency e.g 1592.342 to 1,592.34
     */
    public static String formatCurrency(double currency) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(); // Get a general number formatter

        // Set minimum and maximum fraction digits for currency-style formatting
        //numberFormat.minimumFractionDigits = 2
        numberFormat.setMaximumFractionDigits(2);

        return numberFormat.format(currency);
    }

    /**
     * Generate Chat Path
     * This will generate chat path by sorting these UIDs and concatenate sorted array of UIDs having _ in between
     * All messages of these 2  users will be saved in this path
     *
     * @param receiptUid The UID of the receipt
     * @param yourUid    The UID of the current logged-in user
     */
    public static String chatPath(String receiptUid, String yourUid) {
        //Array of UIDs
        String[] arrayUids = new String[]{receiptUid, yourUid};
        //Sort Array
        Arrays.sort(arrayUids);
        //Concatenate both UIDs (after sorting) having _ between
        String chatPath = arrayUids[0] + "_" + arrayUids[1];
        //return chat path e.g.  if receiptUid = mfVrv1c1U6goV5sbHjvXpn2moUj1 and yourUid = hQknm8IBoAZkqUqkPDzPTK4UzBX2 then chatPath = hQknm8IBoAZkqUqkPDzPTK4UzBX2_mfVrv1c1U6goV5sbHjvXpn2moUj1
        return chatPath;
    }

    public static double calculateDistanceKm(double currentLatitude, double currentLongitude, double adLatitude, double adLongitude) {
        //Source Location i.e. user's current location
        Location startPoint = new Location(LocationManager.NETWORK_PROVIDER);
        startPoint.setLatitude(currentLatitude);
        startPoint.setLongitude(currentLongitude);

        //Destination Location i.e. Ad's location
        Location endPoint = new Location(LocationManager.NETWORK_PROVIDER);
        endPoint.setLatitude(adLatitude);
        endPoint.setLongitude(adLongitude);

        //calculate distance
        double distanceInMeters = startPoint.distanceTo(endPoint); //distance in meters
        double distanceInKm = distanceInMeters / 1000; //e.g. 1km = 1000m so km = m/1000

        return distanceInKm;
    }

    /**
     * Add the add to favorite
     *
     * @param context    the context of activity/fragment from where this function will be called
     * @param propertyId the Id of the property to be added to favorite of current user
     */
    public static void addToFavorite(Context context, String propertyId) {
        //we can add only if user is logged in
        //1)Check if user is logged in
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            //not logged in, can't add to favorite
            MyUtils.toast(context, "You're not logged in!");
        } else {
            //logged in, can add to favorite
            //get timestamp
            long timestamp = MyUtils.getTimestamp();

            //setup data to add in firebase database
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("propertyId", propertyId);
            hashMap.put("timestamp", timestamp);

            //Add data to db. Users > uid > Favorites > adId > favoriteDataObj
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).child("Favorites").child(propertyId)
                    .setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            //success
                            MyUtils.toast(context, "Added to favorite...!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //failed
                            MyUtils.toast(context, "Failed to add to favorite due to " + e.getMessage());
                        }
                    });
        }
    }

    /**
     * Remove the add from favorite
     *
     * @param context    the context of activity/fragment from where this function will be called
     * @param propertyId the Id of the property to be removed from favorite of current user
     */
    public static void removeFromFavorite(Context context, String propertyId) {
        //we can add only if user is logged in
        //1)Check if user is logged in
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            //not logged in, can't remove from favorite
            MyUtils.toast(context, "You're not logged in!");
        } else {
            //logged in, can remove from favorite //Remove data from db. Users > uid > Favorites > adId
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).child("Favorites").child(propertyId)
                    .removeValue()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            //Success
                            MyUtils.toast(context, "Removed from favorite");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //Failed
                            MyUtils.toast(context, "Failed to remove from favorite due to " + e.getMessage());
                        }
                    });
        }
    }

    /**
     * Launch Call Intent with phone number
     *
     * @param context the context of activity/fragment from where this function will be called
     * @param phone   the phone number that will be opened in call intent
     */
    public static void callIntent(Context context, String phone) {

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:" + Uri.encode(phone)));
        context.startActivity(intent);
    }

    /**
     * Launch Sms Intent with phone number
     *
     * @param context the context of activity/fragment from where this function will be called
     * @param phone   the phone number that will be opened in sms intent
     */
    public static void smsIntent(Context context, String phone) {

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + Uri.encode(phone)));
        context.startActivity(intent);
    }

    /**
     * Launch Google Map with input location
     *
     * @param context   the context of activity/fragment from where this function will be called
     * @param latitude  the latitude of the location to be shown in google map
     * @param longitude the longitude of the location to be shown in google map
     */
    public static void mapIntent(Context context, double latitude, double longitude) {
        // Create a Uri from an intent string. Use the result to create an Intent.


        Uri gmmIntentUri = Uri.parse("http://maps.google.com/maps?daddr=" + latitude + "," + longitude);

        // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        // Make the Intent explicit by setting the Google Maps package
        mapIntent.setPackage("com.google.android.apps.maps");
        // Attempt to start an activity that can handle the Intent e.g. Google Map
        if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
            //Google Map installed, start
            context.startActivity(mapIntent);
        } else {
            //Google Map not installed, can't start
            MyUtils.toast(context, "Google MAP Not installed!");
        }
    }


}
