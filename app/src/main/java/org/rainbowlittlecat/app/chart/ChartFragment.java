package org.rainbowlittlecat.app.chart;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.utils.EntryXComparator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.rainbowlittlecat.app.R;
import org.rainbowlittlecat.app.view_model.ChartViewModel;
import org.rainbowlittlecat.app.view_model.MainViewModel;

/**
 * Single chart for single page.
 */
public class ChartFragment extends Fragment implements OnChartGestureListener {

    private View mLayout;
    private LineChart mLineChart;
    private int mChartContent;
    private boolean mEnableSwitch = false;  // Switch between all-day-chart to realtime-chart.
    private int mTimeLineType;

    private List<String> mTimeLineList = new ArrayList<>();
    private List<Float> mAllDayData = new ArrayList<>();
    private List<Float> mRealtimeData = new ArrayList<>();
    private List<Entry> mEntries = new ArrayList<>();

    private MainViewModel mMainViewModel;
    private ChartViewModel mChartViewModel;

    private SetupChartAsyncTask mSetupChartAsyncTask;

    public ChartFragment() {
    }

    static ChartFragment newInstance(int content) {
        Bundle args = new Bundle();
        args.putInt("content", content);
        ChartFragment f = new ChartFragment();
        f.setArguments(args);
        return f;
    }

    // Store instance variables based on arguments passed
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
            mChartContent = getArguments().getInt("content");

        mMainViewModel = new ViewModelProvider(getActivity()).get(MainViewModel.class);
        mChartViewModel = new ViewModelProvider(getActivity()).get(ChartViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle saveInstanceState) {
        mLayout = inflater.inflate(R.layout.chart_layout, container, false);
        return mLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMainViewModel.getChartSwitchable().observe(getViewLifecycleOwner(), state -> mEnableSwitch = state);

        mMainViewModel.getChartTimeLineType().observe(getViewLifecycleOwner(), type -> {
            mTimeLineType = type;
            updateChart();
        });

        mChartViewModel.getAllDayData().observe(getViewLifecycleOwner(), data -> {
            if (data.size() > 0) {
                Map<String, Float> dataMap = new TreeMap<>(data);
                mTimeLineList.clear();
                mAllDayData.clear();
                for (String key : dataMap.keySet()) {
                    mTimeLineList.add(key);
                    mAllDayData.add(dataMap.get(key));
                }
                updateChart();
            }
        });

        mChartViewModel.getRealtimeData().observe(getViewLifecycleOwner(), data -> {
            if (data.size() > 0) {
                mRealtimeData.clear();
                mRealtimeData.addAll(data);
                updateChart();
            }
        });

        setupUI();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSetupChartAsyncTask != null)
            if (mSetupChartAsyncTask.getStatus() == AsyncTask.Status.RUNNING)
                mSetupChartAsyncTask.cancel(true);
    }

    private void setupUI() {
        // create a line chart
        mLineChart = mLayout.findViewById(R.id.line_chart);
        mLineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        mLineChart.getAxisRight().setEnabled(false);
        // set description
        Description description = new Description();
        description.setText("");
        mLineChart.setDescription(description);
        mLineChart.setDrawGridBackground(false);
        mLineChart.setPinchZoom(true);
        mLineChart.setMaxVisibleValueCount(8);
        mLineChart.setDragDecelerationEnabled(false);
        mLineChart.getXAxis().setLabelCount(5);
        mLineChart.setScaleEnabled(false);
        mLineChart.setDragEnabled(false);
        mLineChart.setNoDataText(getString(R.string.no_chart_data_available));
        mLineChart.setNoDataTextColor(R.color.black);
    }

    private void updateChart() {
        if (mSetupChartAsyncTask != null)
            if (mSetupChartAsyncTask.getStatus() == AsyncTask.Status.RUNNING)
                mSetupChartAsyncTask.cancel(true);

        mSetupChartAsyncTask = new SetupChartAsyncTask();

        switch (mTimeLineType) {
            case 0:
                if (mAllDayData.size() > 0) {
                    mSetupChartAsyncTask.execute();
                }
                break;
            case 1:
                if (mRealtimeData.size() > 0) {
                    mSetupChartAsyncTask.execute();
                }
        }
    }

    /**
     * Add data to mEntries.
     */
    private void addDataToEntries() {
        mEntries.clear();

        if ((mTimeLineType == ChartTimeLineType.ALL_DAY) && (mAllDayData.size() == 48)) {
            for (int i = 0; i < mAllDayData.size(); i++)
                mEntries.add(new Entry(getDateTime(i), mAllDayData.get(i)));
        } else if ((mTimeLineType == ChartTimeLineType.REALTIME) && (mRealtimeData.size() == 60)) {
            for (int i = 0; i < mRealtimeData.size(); i++)
                mEntries.add(new Entry(i, mRealtimeData.get(i)));
        }

        // fix big bug of MPAndroidChart
        Collections.sort(mEntries, new EntryXComparator());
    }

    /**
     * Format x axis.
     */
    private long getDateTime(int index) {
        String dateTimeString = mTimeLineList.get(index);
        SimpleDateFormat format = new SimpleDateFormat("dd HH:mm", Locale.getDefault());
        Date date = null;

        try {
            date = format.parse(dateTimeString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        assert date != null;
        return date.getTime();
    }

    /**
     * Display chart.
     */
    private void display() {
        String chartLabel = "";
        if (isAdded())
            switch (mChartContent) {
                case 1:
                    chartLabel = getString(R.string.init_text_air_quality) + " (" + getString(R.string.unit_air_quality) + ")";
                    break;
                case 2:
                    chartLabel = getString(R.string.init_text_temperature) + " (" + getString(R.string.unit_temperature) + ")";
                    break;
                case 3:
                    chartLabel = getString(R.string.init_text_humidity) + " (" + getString(R.string.unit_humidity) + ")";
                    break;
            }

        LineDataSet dataSet = new LineDataSet(mEntries, chartLabel);  // add mEntries to dataSet
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setDrawCircles(false);
        dataSet.setDrawFilled(true);

        float max, min;

        if (mTimeLineType == ChartTimeLineType.REALTIME) {
            mLineChart.getXAxis().setEnabled(false);
            max = Collections.max(mRealtimeData) * 1.1f;
            min = Collections.min(mRealtimeData) * 0.9f;
        } else {
            mLineChart.getXAxis().setValueFormatter(new XAxisValueFormatter());
            mLineChart.getXAxis().setEnabled(true);
            max = Collections.max(mAllDayData) * 1.1f;
            min = Collections.min(mAllDayData) * 0.9f;
        }
        mLineChart.getAxisLeft().setAxisMaximum(max);
        mLineChart.getAxisLeft().setAxisMinimum(min);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2)
            mLineChart.setHardwareAccelerationEnabled(false);

        LineData lineData = new LineData(dataSet);

        // draw different kind of chart
        if (isAdded()) {
            switch (mChartContent) {
                case 1: // air chart
                    dataSet.setColor(getResources().getColor(R.color.air_quality_drawable_end_color));
                    dataSet.setFillDrawable(getResources().getDrawable(R.drawable.air_quality_drawable));
                    break;
                case 2: // temperature chart
                    dataSet.setColor(getResources().getColor(R.color.temperature_drawable_end_color));
                    dataSet.setFillDrawable(getResources().getDrawable(R.drawable.temperature_drawable));
                    break;
                case 3: // wet chart
                    dataSet.setColor(getResources().getColor(R.color.humidity_drawable_end_color));
                    dataSet.setFillDrawable(getResources().getDrawable(R.drawable.humidity_drawable));
                    break;
            }
        }

        mLineChart.setOnChartGestureListener(this);
        mLineChart.setData(lineData);
        mLineChart.notifyDataSetChanged();
        mLineChart.invalidate();
    }


    /*
     * -----------------------
     * OnChartGestureListener
     * -----------------------
     */

    @Override
    public void onChartDoubleTapped(MotionEvent me) {
        // switch chart from 24hr to 60s or 60s to 24hr
        if (mEnableSwitch)
            mMainViewModel.changeChartTimeLineType();
    }

    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
    }

    @Override
    public void onChartLongPressed(MotionEvent me) {
    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {
    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {
    }

    /**
     * Executed in the beginning of the view created.
     * To make the UI smoother.
     */
    @SuppressLint("StaticFieldLeak")
    private class SetupChartAsyncTask extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            addDataToEntries();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            display();
        }
    }
}
