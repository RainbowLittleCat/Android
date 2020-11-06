package org.rainbowlittlecat.app.view_model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChartViewModel extends ViewModel {
    private MutableLiveData<Map<String, Float>> mAllDayData;
    private MutableLiveData<List<Float>> mRealtimeData;
    private Map<String, Float> mAllDayDataMap = new TreeMap<>();
    private List<Float> mRealtimeList = new ArrayList<>();

    private static final int REALTIME_DATA_MAX = 60;
    private List<Float> mAirQualityRealtimeList = new ArrayList<>();
    private List<Float> mTemperatureRealtimeList = new ArrayList<>();
    private List<Float> mHumidityRealtimeList = new ArrayList<>();

    public void setAllDayData(Map<String, Float> data) {
        if (data.size() > 0) {
            mAllDayDataMap.clear();
            mAllDayDataMap.putAll(data);
            ExecutorService service = Executors.newSingleThreadExecutor();
            service.submit(() -> mAllDayData.postValue(mAllDayDataMap));
        }
    }

    public LiveData<Map<String, Float>> getAllDayData() {
        if (mAllDayData == null) {
            mAllDayData = new MutableLiveData<>();
            mAllDayData.setValue(mAllDayDataMap);
        }
        return mAllDayData;
    }

    public void switchPage(int pageIndex) {
        mRealtimeList.clear();
        switch (pageIndex) {
            case 0:
                mRealtimeList.addAll(mAirQualityRealtimeList);
                break;
            case 1:
                mRealtimeList.addAll(mTemperatureRealtimeList);
                break;
            case 2:
                mRealtimeList.addAll(mHumidityRealtimeList);
        }

        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(() -> mRealtimeData.postValue(mRealtimeList));
    }

    public void updateRealtimeData(List<Float> values, int pageIndex) {
        if (values.size() > 0 && pageIndex > -1) {
            while (mAirQualityRealtimeList.size() < REALTIME_DATA_MAX) {
                mAirQualityRealtimeList.add(0f);
                mTemperatureRealtimeList.add(0f);
                mHumidityRealtimeList.add(0f);
            }

            mAirQualityRealtimeList.add(values.get(0));
            mTemperatureRealtimeList.add(values.get(1));
            mHumidityRealtimeList.add(values.get(2));

            mAirQualityRealtimeList.remove(0);
            mTemperatureRealtimeList.remove(0);
            mHumidityRealtimeList.remove(0);


            mRealtimeList.clear();
            switch (pageIndex) {
                case 0:
                    mRealtimeList.addAll(mAirQualityRealtimeList);
                    break;
                case 1:
                    mRealtimeList.addAll(mTemperatureRealtimeList);
                    break;
                case 2:
                    mRealtimeList.addAll(mHumidityRealtimeList);
            }

            ExecutorService service = Executors.newSingleThreadExecutor();
            service.submit(() -> mRealtimeData.postValue(mRealtimeList));
        }
    }

    public LiveData<List<Float>> getRealtimeData() {
        if (mRealtimeData == null) {
            mRealtimeData = new MutableLiveData<>();
            mRealtimeData.setValue(mRealtimeList);
        }
        return mRealtimeData;
    }
}
