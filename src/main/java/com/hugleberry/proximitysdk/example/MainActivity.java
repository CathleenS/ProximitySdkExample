package com.hugleberry.proximitysdk.example;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.TextView;

import com.hugleberry.proximitysdk.R;
import com.hugleberry.proximitysdk.example.controller.DiscoveryController;
import com.hugleberry.proximitysdk.example.controller.OnDiscoveryUpdateListener;
import com.hugleberry.proximitysdk.example.dataprovider.DataProvider;
import com.hugleberry.proximitysdk.example.model.UserModel;
import com.hugleberry.proximitysdk.example.view.LogFragment;
import com.hugleberry.proximitysdk.example.view.MatchKeyListFragment;
import com.hugleberry.proximitysdk.example.view.UserListFragment;
import com.hugleberry.proximitysdk.sdk.model.LtedDeviceMetaData;
import com.hugleberry.proximitysdk.sdk.model.LtedMatchKey;
import com.hugleberry.proximitysdk.sdk.model.LtedTaskResult;
import com.hugleberry.proximitysdk.sdk.task.ILtedTask;
import com.qualcomm.qdiscoverysdk.api.FailureReason;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends FragmentActivity implements OnDiscoveryUpdateListener {

    private DiscoveryController mDiscoveryController;

    private MatchKeyListFragment mKeyListFragment;
    private UserListFragment mUserListFragment;
    private LogFragment mLogFragment;

    private ViewPager mViewPager;
    private CustomFragmentPagerAdapter mSectionsPagerAdapter;

    private TextView mTabMatches;
    private TextView mTabKeys;
    private TextView mTabLog;

    /**
     * A {@link android.support.v4.app.FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class CustomFragmentPagerAdapter extends FragmentPagerAdapter {

        public CustomFragmentPagerAdapter(FragmentManager fm, Bundle args) {
            super(fm);

            mUserListFragment = new UserListFragment();
            mKeyListFragment = new MatchKeyListFragment();
            mLogFragment = LogFragment.newInstance(MainActivity.this);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return mKeyListFragment;
                case 1:
                    return mUserListFragment;
                case 2:
                    mLogFragment.updateLog();
                    return mLogFragment;
            }
            return new Fragment();
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return null;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initViewPager(savedInstanceState);

        // init discovery controller that controls  LTE Direct functionalitty
        mDiscoveryController = new DiscoveryController(this, getApplicationContext());
        mDiscoveryController.initLted();
    }

    @Override
    protected void onDestroy() {
        mDiscoveryController.stop();
        super.onDestroy();
    }


    /**
     * Sets up the tab bar
     */
    private void initViewPager(Bundle args) {

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new CustomFragmentPagerAdapter(getSupportFragmentManager(), args);
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        // Bind the tabs to the ViewPager
        mTabMatches = (TextView) findViewById(R.id.tab_people);
        mTabKeys = (TextView) findViewById(R.id.tab_keys);
        mTabLog = (TextView) findViewById(R.id.tab_log);

        mTabKeys.setTag(0);
        mTabMatches.setTag(1);
        mTabLog.setTag(2);

    }

    private void initTabs() {
        final List<TextView> tabs = Arrays.asList(mTabMatches, mTabKeys, mTabLog);
        View.OnClickListener mTabListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (View tab : tabs) {
                    tab.setSelected(v == tab);
                }
                mViewPager.setCurrentItem((Integer) v.getTag());
            }
        };

        mTabLog.setOnClickListener(mTabListener);
        mTabKeys.setOnClickListener(mTabListener);
        mTabMatches.setOnClickListener(mTabListener);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                for (View tab : tabs) {
                    mLogFragment.updateLog();
                    tab.setSelected(tab.getTag().equals(position));
                }
            }
        });

        // start at first tab
        mViewPager.setCurrentItem(0);

        // set first tab icon
        mTabKeys.setSelected(true);
    }

    /**
     * callback method from MatchKeyListFragment to apply match key changes
     *
     * @param keys
     */
    public void onApplyKeys(List<LtedMatchKey> keys) {
        if (keys == null) {
            return;
        }

        mKeyListFragment.updateButtonState("EXECUTING...", false);
        for (LtedMatchKey key : keys) {
            mDiscoveryController.udpateMatchKey(key);
            DataProvider.getInstance().appUser.updateMatchKey(key);
        }

        mDiscoveryController.start();
    }

    /**
     * Creates alert dialog to retry LTED service initialization
     *
     * @param title
     * @param msg
     */
    private void displayLtedErrorAlert(String title, String msg) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);

        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setMessage(msg);

        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                mDiscoveryController.initLted();
            }
        });

        alertDialogBuilder.show();
    }


    ///----------------------
    // discovery controller callbacks
    ///----------------------

    @Override
    public void onLtedInitSuccess() {

        initTabs();

        // get meta data for own LTED device
        LtedDeviceMetaData device = mDiscoveryController.getLtedDeviceData();

        // create user model for LTED device to display in UserListFragment
        UserModel appUser = new UserModel(device.getId(), device.getImei(), device.getGoogleUserName());
        DataProvider.getInstance().appUser = appUser;
    }

    @Override
    public void onLtedInitFailure() {
        displayLtedErrorAlert("LTE Direct Init Failed", "Please restart QDiscovery Service.");
    }


    @Override
    public void onLtedTerminated() {
        displayLtedErrorAlert("LTE Direct Terminated", "Please restart QDiscovery Service.");
    }

    @Override
    public void onNewUser(UserModel user) {
        DataProvider.getInstance().putUser(user);
        updateUserListView();
    }

    @Override
    public void onUpdateUser(UserModel user) {
        DataProvider.getInstance().putUser(user);
        updateUserListView();
    }

    @Override
    public void onRemoveUser(int userId) {
        DataProvider.getInstance().removeUser(userId);
        updateUserListView();
    }

    /**
     * Update tab and ListFragment that display matching users
     */
    private void updateUserListView() {

        // update tab
        int userCount = 0;
        if (DataProvider.getInstance().getUserList() != null) {
            userCount = DataProvider.getInstance().getUserList().size();
        }
        mTabMatches.setText("MATCHES (" + userCount + ")");

        // update list
        mUserListFragment.updateView();
    }

    public void onCompleteTask(ILtedTask ltedTask, LtedTaskResult result) {
        if (result.success) {
            mKeyListFragment.updateButtonState("DISCOVERING...", false);
        } else {
            mKeyListFragment.updateButtonState("ERROR - PLEASE TRY AGAIN", true);
        }
    }

    public void onUpdateTask(ILtedTask ltedTask, LtedMatchKey key, FailureReason reason) {
        if (reason == null) {
            DataProvider.getInstance().appUser.updateMatchKey(key);
        }
    }
}
