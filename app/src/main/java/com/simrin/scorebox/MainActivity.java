package com.simrin.scorebox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import de.hdodenhof.circleimageview.CircleImageView;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.simrin.scorebox.Fragments.HomeFragment;
import com.simrin.scorebox.Fragments.NewPostFragment;
import com.simrin.scorebox.Fragments.NotificationFragment;
import com.simrin.scorebox.Fragments.ProfileFragment;
import com.simrin.scorebox.Fragments.UsersFragment;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    CircleImageView profile_image;
    TextView username;

    FirebaseUser firebaseUser;
    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadFragment(new HomeFragment());

        BottomNavigationView bottomNavigationView =  findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                Fragment fragment = null;
                switch (menuItem.getItemId()) {
                    case R.id.action_home:
                        fragment = new HomeFragment();
                        break;
                    case R.id.action_search:
                        fragment = new UsersFragment();
                        break;
                    case R.id.action_posts:
                        fragment = new NewPostFragment();
                        break;
                    case R.id.action_notification:
                        fragment = new NotificationFragment();
                        break;
                    case R.id.action_profile:
                        fragment = new ProfileFragment();
                        break;
                }
                return loadFragment(fragment);
            }
        });

        bottomNavigationView.setOnNavigationItemReselectedListener(new BottomNavigationView.OnNavigationItemReselectedListener() {
            @Override
            public void onNavigationItemReselected(@NonNull MenuItem menuItem) {

            }
        });

//        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
//        if(!notificationManagerCompat.areNotificationsEnabled()){
//            Intent intent = new Intent();
//            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
//
//// for Android 8 and above
//            intent.putExtra("android.provider.extra.APP_PACKAGE", getPackageName());
//
//            startActivity(intent);
//        }

//        Toolbar toolbar = findViewById(R.id.toolbar);
//        if(toolbar!=null){
//            setSupportActionBar(toolbar);
//            getSupportActionBar().setTitle("");
//        }
//        toolbar.inflateMenu(R.menu.menu);
//
//        profile_image = findViewById(R.id.profile_image);
//        username = findViewById(R.id.username);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
//
//        reference.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                User user = dataSnapshot.getValue(User.class);
//                username.setText(user.getName());
//                if(user.getImageURL().equals("default")){
//                    profile_image.setImageResource(R.mipmap.ic_launcher);
//                } else {
//                    Glide.with(getApplicationContext()).load(user.getImageURL()).into(profile_image);
//                }
//
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//
//            }
//        });

        //final TabLayout tabLayout = findViewById(R.id.tabLayout);
        //final ViewPager viewPager = findViewById(R.id.view_pager);


//        reference = FirebaseDatabase.getInstance().getReference("Chats");
//        reference.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
//                int unread=0;
//                for(DataSnapshot snapshot :dataSnapshot.getChildren()){
//                    Chat chat = snapshot.getValue(Chat.class);
//                    if(chat.getReceiver().equals(firebaseUser.getUid()) && !chat.isIsseen()){
//                        unread++;
//                    }
//                }
//                if(unread==0){
//                    viewPagerAdapter.addFragment(new ChatsFragment(), "Chats");
//                }else {
//                    viewPagerAdapter.addFragment(new ChatsFragment(), "Chats ("+unread+")");
//                }
//                viewPagerAdapter.addFragment(new UsersFragment(), "Users");
//                viewPagerAdapter.addFragment(new ProfileFragment(), "Profile");
//
//                viewPager.setAdapter(viewPagerAdapter);
//                tabLayout.setupWithViewPager(viewPager);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//
//            }
//        });
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.top_nav_menu, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        Fragment fragment = null;
//        switch(item.getItemId()){
//            case R.id.chat:
//                fragment = new ChatsFragment();
//                break;
//        }
//
//        return loadFragment(fragment);
//    }


//    class ViewPagerAdapter extends FragmentPagerAdapter {
//
//        private ArrayList<Fragment> fragments;
//        private ArrayList<String> titles;
//
//        ViewPagerAdapter(FragmentManager fm){
//            super(fm);
//            this.fragments=new ArrayList<>();
//            this.titles=new ArrayList<>();
//        }
//        @Override
//        public Fragment getItem(int position) {
//            return fragments.get(position);
//        }
//
//        @Override
//        public int getCount() {
//            return fragments.size();
//        }
//
//        public void addFragment(Fragment fragment, String title){
//            fragments.add(fragment);
//            titles.add(title);
//        }
//
//        @Nullable
//        @Override
//        public CharSequence getPageTitle(int position) {
//            return titles.get(position);
//        }
//    }

    private void status(String status){
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        reference.updateChildren(hashMap);
    }

    private boolean loadFragment(Fragment fragment) {
        //switching fragment
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        status("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        status("offline");
    }

}
