package com.hugleberry.proximitysdk.example.controller;

import android.content.Context;

import com.hugleberry.proximitysdk.example.constant.LtedApplicationConfig;
import com.hugleberry.proximitysdk.example.dataprovider.DataProvider;
import com.hugleberry.proximitysdk.example.model.UserModel;
import com.hugleberry.proximitysdk.sdk.internal.handler.ILtedManagerCallbackHandler;
import com.hugleberry.proximitysdk.sdk.internal.task.LtedPublicManagedMatchListener;
import com.hugleberry.proximitysdk.sdk.manager.LtedManager;
import com.hugleberry.proximitysdk.sdk.model.LtedDeviceMetaData;
import com.hugleberry.proximitysdk.sdk.model.LtedMatchKey;
import com.hugleberry.proximitysdk.sdk.model.LtedPublicManagedConfigModel;
import com.hugleberry.proximitysdk.sdk.task.LtedPublicManagedPublishTask;
import com.hugleberry.proximitysdk.sdk.task.LtedPublicManagedSubscribeTask;

import java.util.List;

/**
 * Inits LTED to discover and get discovered by LTED devices within proximity.
 */
public class DiscoveryController implements LtedPublicManagedMatchListener {

    LtedPublicManagedPublishTask mPublishTask; // to get detected by other LTE Direct Devices
    LtedPublicManagedSubscribeTask mSubscribeTask; // to detect other LTE Direct Devices

    private Context mContext;
    private OnDiscoveryUpdateListener mCallbackHandler;

    public DiscoveryController(final OnDiscoveryUpdateListener listener, Context context) {
        mCallbackHandler = listener;
        mContext = context;
    }

    /**
     * Get specific data about the device using LTED
     *
     * @return LtedDeviceMetaData data model
     */
    public LtedDeviceMetaData getLtedDeviceData() {
        if (LtedManager.getInstance().isInitialized()) {
            return LtedManager.getLtedDeviceMetaData();
        }
        return null;
    }

    /**
     * Init Discovery Service and LTED tasks to execute LTED expressions needed for subscribe/publish
     */
    public void initLted() {

        LtedPublicManagedConfigModel config = new LtedPublicManagedConfigModel(LtedApplicationConfig.LTED_APP_ID);
        config.setEnsProjectAddress(LtedApplicationConfig.getEnsProjectAddress());
        config.setExpressionRetryAmount(3);

        LtedManager.initialize(config, mContext, new ILtedManagerCallbackHandler() {
            @Override
            public void onInitSuccess() {
                mCallbackHandler.onLtedInitSuccess();
                initLtedTasks();
            }

            @Override
            public void onInitFailure() {
                mCallbackHandler.onLtedInitFailure();
            }

            @Override
            public void onTerminated() {
                mCallbackHandler.onLtedTerminated();
            }
        });
    }

    /**
     * Creates susbcribe task to discover other devices with matching keys.
     * Creates publish task to publish own keys and meta data to other LTE direct devices.
     */
    private void initLtedTasks() {

        mSubscribeTask = new LtedPublicManagedSubscribeTask(LtedApplicationConfig.LTED_DISCOVERY_TASK);
        mSubscribeTask.setOnTaskStateChangeListener(new LtedPublicManagedTaskStateListener(mSubscribeTask, mCallbackHandler));
        mSubscribeTask.registerMatchListener(this);

        mPublishTask = new LtedPublicManagedPublishTask(LtedApplicationConfig.LTED_DISCOVERY_TASK);
        mPublishTask.setOnTaskStateChangeListener(new LtedPublicManagedTaskStateListener(mPublishTask, mCallbackHandler));
    }

    public void start() {
        mSubscribeTask.start();
        mPublishTask.start();
    }

    public void stop() {
        if (mSubscribeTask != null) {
            mSubscribeTask.stop();
        }
        if (mPublishTask != null) {
            mPublishTask.stop();
        }

        LtedManager.getInstance().terminate();
    }

    public void udpateMatchKey(LtedMatchKey key) {
        // remove key if no labels are selected
        if (key.getLabels() == null || key.getLabels().isEmpty()) {
            mPublishTask.removeMatchKey(key.getId());
            mSubscribeTask.removeMatchKey(key.getId());
        } else {
            // update key
            mPublishTask.setMatchKey(key);
            mSubscribeTask.setMatchKey(key);
        }
    }


   /* -------------------------
      LTE DIRECT DISCOVERY MATCH CALLBACKS
     ------------------------- */

    @Override
    public void onNewMatchingDevice(LtedDeviceMetaData device, List<LtedMatchKey> matchKeys) {

        // create user model for detected device
        UserModel user = new UserModel(device.getId(), device.getImei(), device.getGoogleUserName());
        user.setInterests(matchKeys);

        mCallbackHandler.onNewUser(user);
    }

    @Override
    public void onUpdateDeviceKey(LtedDeviceMetaData device, LtedMatchKey matchKey) {
        UserModel user = DataProvider.getInstance().getUserForId(device.getId());
        if (user != null) {
            user.setKey(matchKey);
            mCallbackHandler.onUpdateUser(user);
        }
    }

    @Override
    public void onRemoveMatchingDevice(int deviceId) {
        mCallbackHandler.onRemoveUser(deviceId);
    }


    @Override
    public void onRemoveKeyForDevice(LtedDeviceMetaData device, LtedMatchKey matchKey) {
        if (matchKey == null){
            return;
        }
        UserModel user = DataProvider.getInstance().getUserForId(device.getId());
        if (user != null) {
            user.removeMatchKey(matchKey.getId());
            mCallbackHandler.onUpdateUser(user);
        }
    }


    @Override
    public void onNewMatchData(int deviceId, String classType, Object object) {
    }

    @Override
    public void onUpdateMatchData(int deviceId, String classType, Object object) {
    }

    @Override
    public void onRemoveMatchData(int deviceId, String classType, Object object) {
    }
}
