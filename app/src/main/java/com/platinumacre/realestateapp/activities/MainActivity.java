package com.platinumacre.realestateapp.activities;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.platinumacre.realestateapp.MyUtils;
import com.platinumacre.realestateapp.R;
import com.platinumacre.realestateapp.databinding.ActivityMainBinding;
import com.platinumacre.realestateapp.fragments.ChatsListFragment;
import com.platinumacre.realestateapp.fragments.FavoritListFragment;
import com.platinumacre.realestateapp.fragments.HomeFragment;
import com.platinumacre.realestateapp.fragments.ProfileFragment;

public class MainActivity extends AppCompatActivity {

    //View Binding
    private ActivityMainBinding binding;

    //Tag to show logs in logcat
    private static final String TAG = "MAIN_TAG";

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        //activity_main.xml = ActivityMainBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() == null) {
            //user is not logged in, move to LoginOptionsActivity
            startLoginOptions();
        }

        //By default (when app open) show showHomeFragment
        showHomeFragment();

        //handle bottomNv item clicks to navigate between fragments
        binding.bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                //get id of the menu item clicked
                int itemId = item.getItemId();
                if (itemId == R.id.item_home) {
                    //Home item clicked, show HomeFragment

                    showHomeFragment();
                } else if (itemId == R.id.item_chats) {
                    //Chats item clicked, show ChatsListFragment

                    if (firebaseAuth.getCurrentUser() == null){
                        //Not, Logged-In, Show message toast
                        MyUtils.toast(MainActivity.this, "Login Required...");
                        startLoginOptions();
                        //Return false so bottom navigation menu item (item_chats) doesn't become selected
                        return false;
                    } else {
                        //Logged-In, Open ChatsListFragment
                        showChatsListFragment();
                        //Return true so bottom navigation menu item (item_chats) become selected
                        return true;
                    }
                } else if (itemId == R.id.item_favorite) {
                    //Favorites item clicked, show FavoritListtFragment

                    if (firebaseAuth.getCurrentUser() == null){
                        //Not, Logged-In, Show message toast
                        MyUtils.toast(MainActivity.this, "Login Required...");
                        startLoginOptions();
                        //Return false so bottom navigation menu item (item_chats) doesn't become selected
                        return false;
                    } else {
                        //Logged-In, Open ChatsListFragment
                        showFavoriteListFragment();
                        //Return true so bottom navigation menu item (item_chats) become selected
                        return true;
                    }
                } else if (itemId == R.id.item_profile) {
                    //Profile item clicked, show ProfileFragment

                    if (firebaseAuth.getCurrentUser() == null){
                        //Not, Logged-In, Show message toast
                        MyUtils.toast(MainActivity.this, "Login Required...");
                        startLoginOptions();
                        //Return false so bottom navigation menu item (item_chats) doesn't become selected
                        return false;
                    } else {
                        //Logged-In, Open ChatsListFragment
                        showProfileFragment();
                        //Return true so bottom navigation menu item (item_chats) become selected
                        return true;
                    }
                }

                return true;
            }
        });
    }


    private void showHomeFragment() {
        //change toolbar textView text/title to Home
        binding.toolbarTitleTv.setText("Home");

        //Show HomeFragment
        HomeFragment fragment = new HomeFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFl.getId(), fragment, "HomeFragment");
        fragmentTransaction.commit();
    }

    private void showChatsListFragment() {
        //change toolbar textView text/title to Home
        binding.toolbarTitleTv.setText("Chats");

        //Show ChatsListFragment
        ChatsListFragment fragment = new ChatsListFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFl.getId(), fragment, "ChatsListFragment");
        fragmentTransaction.commit();
    }

    private void showFavoriteListFragment() {
        //change toolbar textView text/title to Home
        binding.toolbarTitleTv.setText("Favorits");

        //Show FavoritListFragment
        FavoritListFragment fragment = new FavoritListFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFl.getId(), fragment, "FavoritListFragment");
        fragmentTransaction.commit();
    }

    private void showProfileFragment() {
        //change toolbar textView text/title to Home
        binding.toolbarTitleTv.setText("Profile");

        //Show ProfileFragment
        ProfileFragment fragment = new ProfileFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFl.getId(), fragment, "ProfileFragment");
        fragmentTransaction.commit();
    }


    private void startLoginOptions() {
        startActivity(new Intent(this, LoginOptionsActivity.class));
    }
}