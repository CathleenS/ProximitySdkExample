package com.hugleberry.proximitysdk.example.controller;

import com.hugleberry.proximitysdk.example.model.UserModel;
import com.hugleberry.proximitysdk.sdk.task.ILtedTask;
import com.hugleberry.proximitysdk.sdk.model.LtedMatchKey;
import com.hugleberry.proximitysdk.sdk.model.LtedTaskResult;
import com.qualcomm.qdiscoverysdk.api.FailureReason;


public interface OnDiscoveryUpdateListener {

    void onLtedInitFailure();

    void onLtedInitSuccess();

    void onLtedTerminated();

    void onUpdateUser(UserModel user);

    void onNewUser(UserModel user);

    void onRemoveUser(int userId);

    void onCompleteTask(ILtedTask ltedTask, LtedTaskResult result);

    void onUpdateTask(ILtedTask ltedTask, LtedMatchKey key, FailureReason reason);
}