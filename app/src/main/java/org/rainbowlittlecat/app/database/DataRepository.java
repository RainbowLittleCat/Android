/*
 * Changed from
 *     https://github.com/googlecodelabs/android-room-with-a-view/blob/master/app/src/main/java/com/example/android/roomwordssample/WordRepository.java
 *
 * Tutorial
 *     https://codelabs.developers.google.com/codelabs/android-room-with-a-view/#7
 *
 * Copyright (C) 2017 Google Inc.
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

package org.rainbowlittlecat.app.database;

import android.app.Application;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.rainbowlittlecat.app.R;
import org.rainbowlittlecat.app.bluetooth.BluetoothHelper;
import org.rainbowlittlecat.app.bluetooth.DataUpdateHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Data repository.
 * Handle remote device connections and local database.
 */
public class DataRepository implements BluetoothHelper.BluetoothStateListener {

    private Application mApplication;

    private DataDao mDataDao;
    private LiveData<List<Data>> mAllData;

    private BluetoothHelper mBluetoothHelper;
    private MutableLiveData<Boolean> mIsRefreshing;

    private MutableLiveData<List<Integer>> mRealtimeValues;  // Storage 3 values totally.
    private List<Integer> mRealtimeValuesList = new ArrayList<>();

    private MutableLiveData<Boolean> mChartSwitchable;
    private boolean mSwitchable = false;

    private static final int ALL_DAY_DATA_MAX = 48;
    private static final int REALTIME_DATA_MAX = 60;

    private List<String> mTimeLineList = new ArrayList<>();
    private List<Float> mAirQualityAllDayList = new ArrayList<>();
    private List<Float> mTemperatureAllDayList = new ArrayList<>();
    private List<Float> mHumidityAllDayList = new ArrayList<>();

    private List<Float> mAirQualityRealtimeList = new ArrayList<>();
    private List<Float> mTemperatureRealtimeList = new ArrayList<>();
    private List<Float> mHumidityRealtimeList = new ArrayList<>();

    // Note that in order to unit test the DataRepository, you have to remove the Application
    // dependency. This adds complexity and much more code.
    // See the BasicSample in the android-architecture-components repository at
    // https://github.com/googlesamples
    public DataRepository(Application application) {
        mApplication = application;
        DataRoomDatabase db = DataRoomDatabase.getDatabase(mApplication);
        mDataDao = db.dataDao();
        mAllData = mDataDao.getAllData();

        mBluetoothHelper = new BluetoothHelper(mApplication.getApplicationContext());
        mBluetoothHelper.setBluetoothListener(this);
    }

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    public LiveData<List<Data>> getAllData() {
        return mAllData;
    }

    // You must call this on a non-UI thread or your app will throw an exception. Room ensures
    // that you're not doing any long running operations on the main thread, blocking the UI.
    private void updateAllDatabaseData(List<Data> data) {
        DataRoomDatabase.databaseWriteExecutor.execute(() -> {
            mDataDao.deleteAll();
            mDataDao.updateAll(data);
        });
    }


    /*
     * Bluetooth.
     */

    public boolean isBluetoothEnabled() {
        return mBluetoothHelper.isBluetoothEnabled();
    }

    public boolean isBluetoothServiceNull() {
        return mBluetoothHelper.isServiceNull();
    }

    public boolean isBluetoothConnected() {
        return mBluetoothHelper.isConnected();
    }

    /**
     * Initialize BluetoothService.
     */
    public void setupBluetoothConnection() {
        mBluetoothHelper.setupConnection();
    }

    public void connectDevice(String address) {
        // First of all check if this address is currently connected
        // before trying to connect to it.
        mBluetoothHelper.checkIfDuplicatedThenConnect(address);
    }

    /**
     * onResumeCheck
     * Performing this check in onResume() covers the case in which BT was
     * not enabled during onStart(), so we were paused to enable it...
     * onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
     */
    public void onResumeCheck() {
        mBluetoothHelper.onResumeCheck();
    }

    public void refreshBluetoothConnection() {
        // Check if bluetooth is connected.
        if (mBluetoothHelper.isConnected()) {
            mBluetoothHelper.stopUpdatingData();
            mBluetoothHelper.startUpdatingData();
        } else {
            Toast.makeText(mApplication.getApplicationContext(),
                    mApplication.getString(R.string.not_connected),
                    Toast.LENGTH_SHORT).show();
            setRefreshing(false);
        }
    }

    public void sendMessage(String message) {
        mBluetoothHelper.sendMessage(message);
    }

    public void shutdownBluetooth() {
        mBluetoothHelper.shutdown();
    }

    public LiveData<Boolean> isRefreshing() {
        if (mIsRefreshing == null) {
            mIsRefreshing = new MutableLiveData<>();
            mIsRefreshing.setValue(false);
        }

        return mIsRefreshing;
    }

    private synchronized void setRefreshing(boolean state) {
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(() -> mIsRefreshing.postValue(state));
    }

    public LiveData<Boolean> getChartSwitchable() {
        if (mChartSwitchable == null) {
            mChartSwitchable = new MutableLiveData<>();
            mChartSwitchable.setValue(mSwitchable);
        }

        return mChartSwitchable;
    }

    private void setChartSwitchable(boolean state) {
        if (mChartSwitchable == null)
            mChartSwitchable = new MutableLiveData<>();
        mSwitchable = state;
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(() -> mChartSwitchable.postValue(mSwitchable));
    }

    public LiveData<List<Integer>> getRealtimeValues() {
        if (mRealtimeValues == null) {
            mRealtimeValues = new MutableLiveData<>();
            mRealtimeValues.setValue(mRealtimeValuesList);
        }

        return mRealtimeValues;
    }

    private void formatAllDayDataAndSave(ArrayList<Integer> data) {
        mAirQualityAllDayList.clear();
        mTemperatureAllDayList.clear();
        mHumidityAllDayList.clear();

        for (int i = 0; i < ALL_DAY_DATA_MAX; i++) {
            mAirQualityAllDayList.add((float) data.get(i * 3));
            mTemperatureAllDayList.add((float) data.get(i * 3 + 1));
            mHumidityAllDayList.add((float) data.get(i * 3 + 2));
        }

        // get the time in the device
        int nowDeviceTime = data.get(144);
        calculateChartTimeLine(nowDeviceTime);

        // save data to SQLite
        ArrayList<Data> dataSavedToDatabase = new ArrayList<>();
        for (int i = 0; i < ALL_DAY_DATA_MAX; i++) {
            dataSavedToDatabase.add(
                    new Data(
                            mTimeLineList.get(i),
                            mAirQualityAllDayList.get(i),
                            mTemperatureAllDayList.get(i),
                            mHumidityAllDayList.get(i)
                    )
            );
        }
        updateAllDatabaseData(dataSavedToDatabase);
    }

    /**
     * calculate time line, and save it to timeLineList
     */
    private void calculateChartTimeLine(int timeGivenByRemoteDevice) {
        mTimeLineList.clear();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd HH:mm", Locale.getDefault());
        Calendar currentTime = Calendar.getInstance();
        long lastTime = currentTime.getTimeInMillis() - DataUpdateHelper.HALF_AN_HOUR + timeGivenByRemoteDevice;

        for (int i = ALL_DAY_DATA_MAX; i > -1; i--) {
            long eachTime = lastTime - DataUpdateHelper.HALF_AN_HOUR * i;
            String eachDateTime = dateFormat.format(eachTime);
            mTimeLineList.add(eachDateTime);
        }
    }

    private void updateChartRealtimeData(List<Integer> values) {
        if (mAirQualityRealtimeList.size() < REALTIME_DATA_MAX)
            initChartRealtimeList();

        mAirQualityRealtimeList.add((float) values.get(0));
        mTemperatureRealtimeList.add((float) values.get(1));
        mHumidityRealtimeList.add((float) values.get(2));

        while (mAirQualityRealtimeList.size() > REALTIME_DATA_MAX)
            mAirQualityRealtimeList.remove(0);
        while (mTemperatureRealtimeList.size() > REALTIME_DATA_MAX)
            mTemperatureRealtimeList.remove(0);
        while (mHumidityRealtimeList.size() > REALTIME_DATA_MAX)
            mHumidityRealtimeList.remove(0);
    }

    private void initChartRealtimeList() {
        for (int i = mAirQualityRealtimeList.size() - 1; i < REALTIME_DATA_MAX; i++) {
            mAirQualityRealtimeList.add(0f);
            mTemperatureRealtimeList.add(0f);
            mHumidityRealtimeList.add(0f);
        }
    }

    /*
     * BluetoothStateListener.
     */

    @Override
    public void onUnableToConnectDevice() {
        setRefreshing(false);
    }

    @Override
    public void onConnecting() {
        setRefreshing(true);
    }

    @Override
    public void onConnected() {
        setChartSwitchable(true);
    }

    @Override
    public void onDeviceConnectionLost() {
        setRefreshing(false);
    }

    @Override
    public void onRealtimeDataCome(ArrayList<Integer> data) {
        mRealtimeValuesList.clear();
        mRealtimeValuesList.addAll(data);
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(() -> mRealtimeValues.postValue(mRealtimeValuesList));
        updateChartRealtimeData(data);
        setRefreshing(false);
    }

    @Override
    public void onAllDayDataCome(ArrayList<Integer> data) {
        formatAllDayDataAndSave(data);
    }
}
