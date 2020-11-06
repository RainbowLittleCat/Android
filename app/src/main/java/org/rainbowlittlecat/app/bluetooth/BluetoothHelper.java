/*
 * Changed from
 *     https://github.com/googlearchive/android-BluetoothChat/blob/master/Application/src/main/java/com/example/android/bluetoothchat/BluetoothChatFragment.java
 *
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rainbowlittlecat.app.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

import org.rainbowlittlecat.app.R;

public class BluetoothHelper implements DataUpdateHelper.UpdateListener {

    private static final String TAG = "BluetoothHelper";

    /**
     * Local Bluetooth adapter.
     */
    private BluetoothAdapter mBluetoothAdapter;

    /**
     * Member object for bluetooth services.
     */
    private BluetoothService mBluetoothService = null;

    /**
     * Data comes from bluetooth.
     */
    private ArrayList<Integer> mInData = new ArrayList<>();

    private static final int ALL_DAY_DATA_MAX_SIZE = 145;
    private static final int REAL_TIME_DATA_MAX_SIZE = 3;

    /**
     * Data request command.
     */
    private static final String REQUEST_ALL_DAY_DATA = "r";
    private static final String REQUEST_REALTIME_DATA = "n";
    private String mNowRequestCommand = REQUEST_ALL_DAY_DATA;

    /**
     * To count transmit time.
     */
    private long mStartReceivingTime = 0L;

    private DataUpdateHelper mDataUpdateHelper;

    private BluetoothStateListener mBluetoothListener;

    private String mNewDeviceAddress;

    private Context mContext;

    public BluetoothHelper(Context context) {
        mContext = context;

        // Get local bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then bluetooth is not supported
        // Destroy app on MainActivity.java
        if (mBluetoothAdapter == null)
            shutdown();
    }

    /**
     * Initialize the BluetoothService to perform bluetooth connections.
     */
    public void setupConnection() {
        mBluetoothService = new BluetoothService(mHandler);
    }

    public void shutdown() {
        stopUpdatingData();

        if (mBluetoothService != null)
            mBluetoothService.stop();
    }

    public boolean isBluetoothEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    public boolean isConnected() {
        return mBluetoothService.getState() == BluetoothService.STATE_CONNECTED;
    }

    public void startUpdatingData() {
        mDataUpdateHelper = new DataUpdateHelper();
        mDataUpdateHelper.setUpdateListener(this);
        mDataUpdateHelper.start();
    }

    public void stopUpdatingData() {
        if (mDataUpdateHelper != null)
            mDataUpdateHelper.stop();
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothConstants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_NONE:
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_CONNECTING:
                        case BluetoothService.STATE_CONNECTED:
                            break;
                    }

                case BluetoothConstants.MESSAGE_WRITE:
                    break;

                case BluetoothConstants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;

                    if (mDataUpdateHelper != null) {
                        if (!mDataUpdateHelper.isWaitingForUnexpectedData()) {
                            long receive_end_time;

                            switch (mNowRequestCommand) {
                                case REQUEST_ALL_DAY_DATA:
                                    for (byte aReadBuf : readBuf) {
                                        if (mInData.size() < ALL_DAY_DATA_MAX_SIZE) {
                                            int singleInput = getUnsignedByte(aReadBuf);
                                            mInData.add(singleInput);
                                            System.out.print(singleInput + ",");
                                        } else
                                            break;
                                    }

                                    if (mInData.size() == ALL_DAY_DATA_MAX_SIZE) {
                                        mBluetoothListener.onAllDayDataCome(mInData);
                                        receive_end_time = System.currentTimeMillis();
                                        System.out.println("receive big data time cost : " + (receive_end_time - mStartReceivingTime));
                                        mInData.clear();
                                    }

                                    break;

                                case REQUEST_REALTIME_DATA:
                                    for (byte b : readBuf) {
                                        if (mInData.size() < REAL_TIME_DATA_MAX_SIZE) {
                                            int singleInput = getUnsignedByte(b);
                                            mInData.add(singleInput);
                                            System.out.print(singleInput + ",");
                                        } else
                                            break;
                                    }

                                    if (mInData.size() == REAL_TIME_DATA_MAX_SIZE) {
                                        mBluetoothListener.onRealtimeDataCome(mInData);
                                        receive_end_time = System.currentTimeMillis();
                                        System.out.println("receive latest data time cost : " + (receive_end_time - mStartReceivingTime));
                                        mInData.clear();
                                    }

                                    break;
                            }
                        }
                    }

                    break;

                case BluetoothConstants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    String mConnectedDeviceName = msg.getData().getString(BluetoothConstants.DEVICE_NAME);
                    // save address to SharedPreferences.
                    String connectedDeviceAddress = msg.getData().getString(BluetoothConstants.DEVICE_ADDRESS);
                    saveDeviceAddressToSharedPreferences(connectedDeviceAddress);
                    mNewDeviceAddress = "";

                    // initialize DataUpdateHelper
                    mDataUpdateHelper = new DataUpdateHelper();
                    mDataUpdateHelper.setUpdateListener(BluetoothHelper.this);
                    mDataUpdateHelper.start();

                    Toast.makeText(getContext(), mContext.getString(R.string.connect_to) + mConnectedDeviceName,
                            Toast.LENGTH_SHORT).show();
                    mBluetoothListener.onConnected();
                    break;

                case BluetoothConstants.MESSAGE_TOAST:
                    String msgText = msg.getData().getString(BluetoothConstants.TOAST);

                    if (msgText != null && msgText.equals("Unable to connect device"))
                        mBluetoothListener.onUnableToConnectDevice();
                    else if (msgText != null && msgText.equals("Device connection was lost")) {
                        Log.d(TAG, msgText);
                        stopUpdatingData();
                        mBluetoothListener.onDeviceConnectionLost();
                    }

                    Toast.makeText(getContext(), msg.getData().getString(BluetoothConstants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    public void onRequestAllDayData() {
        Log.d(TAG, "On request past 24 hour data.");
        mStartReceivingTime = System.currentTimeMillis();
        mNowRequestCommand = REQUEST_ALL_DAY_DATA;
        sendMessage(mNowRequestCommand);
    }

    @Override
    public void onRequestRealtimeData() {
        Log.d(TAG, "On request real time data.");
        mStartReceivingTime = System.currentTimeMillis();
        mNowRequestCommand = REQUEST_REALTIME_DATA;
        sendMessage(mNowRequestCommand);
    }

    public void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (!isConnected()) {
            Toast.makeText(getContext(), getContext().getResources().getText(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothService to write
            byte[] send = message.getBytes();
            mBluetoothService.write(send);
        }
    }

    public boolean isServiceNull() {
        return mBluetoothService == null;
    }

    public void setBluetoothListener(BluetoothStateListener listener) {
        mBluetoothListener = listener;
    }

    /**
     * onResumeCheck
     * Performing this check in onResume() covers the case in which BT was
     * not enabled during onStart(), so we were paused to enable it...
     * onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
     */
    public void onResumeCheck() {
        if (mBluetoothService != null)
            // Only if the state is STATE_NONE, do we know that we haven't started already.
            if (mBluetoothService.getState() == BluetoothService.STATE_NONE)
                // Start the Bluetooth services.
                mBluetoothService.start();
    }

    /**
     * Call this in onResume() to refresh connection.
     */
    public void checkIfDuplicatedThenConnect(String address) {
        mNewDeviceAddress = address;

        // Get last device MAC address.
        String lastDeviceAddress = getDeviceAddressFromSharedPreferences();

        if (mNewDeviceAddress.equals("")) {  // no new device.
            if (lastDeviceAddress.equals("")) {  // no last device address.
                mBluetoothListener.onUnableToConnectDevice();
            } else {  // Has last device address
                if (isServiceStateNone()) {
                    // Connect to last device
                    mBluetoothListener.onConnecting();
                    stopUpdatingData();
                    connectDevice(lastDeviceAddress);
                }
            }
        } else {  // Has new device.
            // equals to last one.
            if (mNewDeviceAddress.equals(lastDeviceAddress)) {
                // And the device is not connected now.
                if (isServiceStateNone()) {
                    mBluetoothListener.onConnecting();
                    stopUpdatingData();
                    connectDevice(lastDeviceAddress);
                } else if (isConnected()) {
                    // Still connected
                    mBluetoothListener.onConnected();
                }
            } else {  // Not equals to last one.
                mBluetoothListener.onConnecting();
                stopUpdatingData();
                connectDevice(mNewDeviceAddress);
            }
        }
    }

    private boolean isServiceStateNone() {
        return mBluetoothService.getState() == BluetoothService.STATE_NONE;
    }

    /**
     * Establish connection with other device.
     *
     * @param address MAC address to connect to.
     */
    private void connectDevice(String address) {
        // Get the BluetoothDevice object.
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to device.
        mBluetoothService.connect(device);
    }

    private Context getContext() {
        return mContext;
    }

    private int getUnsignedByte(byte b) {
        return b & 0xff;
    }

    private void saveDeviceAddressToSharedPreferences(String address) {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("information", Context.MODE_PRIVATE);
        sharedPreferences.edit().putString("mac_address", address).apply();
    }

    private String getDeviceAddressFromSharedPreferences() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("information", Context.MODE_PRIVATE);
        return sharedPreferences.getString("mac_address", "");
    }

    /**
     * Bluetooth state listener
     */
    public interface BluetoothStateListener {
        void onUnableToConnectDevice();

        void onConnecting();

        void onConnected();

        void onDeviceConnectionLost();

        void onRealtimeDataCome(ArrayList<Integer> data);

        void onAllDayDataCome(ArrayList<Integer> data);
    }
}
