package org.rainbowlittlecat.app.chart;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class ChartPagerAdapter extends FragmentPagerAdapter {
    private ChartFragment mAirQualityChart;
    private ChartFragment mTemperatureChart;
    private ChartFragment mHumidityChart;

    public ChartPagerAdapter(FragmentManager fm) {
        super(fm);
        mAirQualityChart = ChartFragment.newInstance(ChartContent.AIR_QUALITY);
        mTemperatureChart = ChartFragment.newInstance(ChartContent.TEMPERATURE);
        mHumidityChart = ChartFragment.newInstance(ChartContent.HUMIDITY);
    }

    @Override
    public Fragment getItem(int index) {
        switch (index) {
            case 0:
                return mAirQualityChart;
            case 1:
                return mTemperatureChart;
            case 2:
                return mHumidityChart;
        }

        return null;
    }

    @Override
    public int getCount() {
        return 3;
    }
}
