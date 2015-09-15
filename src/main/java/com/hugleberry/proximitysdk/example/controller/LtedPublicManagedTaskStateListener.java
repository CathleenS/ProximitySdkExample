package com.hugleberry.proximitysdk.example.controller;

import com.hugleberry.proximitysdk.sdk.internal.job.ILtedExpressionJob;
import com.hugleberry.proximitysdk.sdk.internal.job.ILtedPublicManagedExpressionJob;
import com.hugleberry.proximitysdk.sdk.model.LtedTaskResult;
import com.hugleberry.proximitysdk.sdk.task.ILtedPublicManagedTask;
import com.hugleberry.proximitysdk.sdk.task.ILtedTaskStateChangeListener;
import com.qualcomm.qdiscoverysdk.api.FailureReason;


public class LtedPublicManagedTaskStateListener implements ILtedTaskStateChangeListener {

    private OnDiscoveryUpdateListener mCallbackHandler;
    private ILtedPublicManagedTask mTask;

    public LtedPublicManagedTaskStateListener(ILtedPublicManagedTask task, OnDiscoveryUpdateListener handler) {
        mTask = task;
        mCallbackHandler = handler;
    }

    @Override
    public void onUpdate(ILtedExpressionJob job, FailureReason reason) {
        mCallbackHandler.onUpdateTask(mTask, ((ILtedPublicManagedExpressionJob) job).getMatchKey(), reason);
    }

    @Override
    public void onComplete(LtedTaskResult result) {
        mCallbackHandler.onCompleteTask(mTask, result);
    }
}