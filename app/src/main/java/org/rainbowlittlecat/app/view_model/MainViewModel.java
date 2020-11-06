package org.rainbowlittlecat.app.view_model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.rainbowlittlecat.app.database.Data;
import org.rainbowlittlecat.app.database.DataRepository;
import org.rainbowlittlecat.app.chart.ChartTimeLineType;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for MainActivity.
 */
public class MainViewModel extends AndroidViewModel {

    private DataRepository mDataRepository;

    private LiveData<List<Data>> mAllData;

    private MutableLiveData<Integer> mChartTimeLineType;
    private int mChartTimeLineTypeInt;

    public MainViewModel(@NonNull Application application) {
        super(application);
        mDataRepository = new DataRepository(application);
        mAllData = mDataRepository.getAllData();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDataRepository.shutdownBluetooth();
    }


    /*
     * ----------------
     * Chart
     * ----------------
     */

    public LiveData<Boolean> getChartSwitchable() {
        return mDataRepository.getChartSwitchable();
    }

    public LiveData<Integer> getChartTimeLineType() {
        if (mChartTimeLineType == null) {
            mChartTimeLineType = new MutableLiveData<>();
            mChartTimeLineType.setValue(mChartTimeLineTypeInt);
        }

        return mChartTimeLineType;
    }

    public void changeChartTimeLineType(int type) {
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(() -> mChartTimeLineType.postValue(type));
    }

    public LiveData<List<Data>> getAllData() {
        return mAllData;
    }

    public void changeChartTimeLineType() {
        mChartTimeLineTypeInt = mChartTimeLineTypeInt == ChartTimeLineType.ALL_DAY
                ? ChartTimeLineType.REALTIME : ChartTimeLineType.ALL_DAY;

        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(() -> mChartTimeLineType.postValue(mChartTimeLineTypeInt));
    }


    /*
     * -------------------
     * Bluetooth
     * -------------------
     */

    public boolean isBluetoothEnabled() {
        return mDataRepository.isBluetoothEnabled();
    }

    public boolean isBluetoothServiceNull() {
        return mDataRepository.isBluetoothServiceNull();
    }

    public boolean isBluetoothConnected() {
        return mDataRepository.isBluetoothConnected();
    }

    public void setupBluetoothConnection() {
        mDataRepository.setupBluetoothConnection();
    }

    public void connectDevice(String address) {
        mDataRepository.connectDevice(address);
    }

    public void onResumeCheck() {
        mDataRepository.onResumeCheck();
    }

    public void sendMessageViaBluetooth(String message) {
        mDataRepository.sendMessage(message);
    }

    public LiveData<Boolean> isRefreshing() {
        return mDataRepository.isRefreshing();
    }

    public LiveData<List<Integer>> getRealtimeValues() {
        return mDataRepository.getRealtimeValues();
    }

    /**
     * MainActivity call it when swipe refresh.
     */
    public void refresh() {
        mDataRepository.refreshBluetoothConnection();
    }
}
