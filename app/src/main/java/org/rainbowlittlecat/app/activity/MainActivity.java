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

package org.rainbowlittlecat.app.activity;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.rainbowlittlecat.app.R;
import org.rainbowlittlecat.app.chart.ChartTimeLineType;
import org.rainbowlittlecat.app.database.Data;
import org.rainbowlittlecat.app.view_model.ChartViewModel;
import org.rainbowlittlecat.app.voice.LanguageTeacher;
import org.rainbowlittlecat.app.chart.ChartPagerAdapter;
import org.rainbowlittlecat.app.voice.VoiceOutput;
import org.rainbowlittlecat.app.view_model.MainViewModel;

/**
 * Main activity of this app.
 */
public class MainActivity extends AppCompatActivity implements
        TabLayout.BaseOnTabSelectedListener,
        SwipeRefreshLayout.OnRefreshListener {

    /**
     * Tag for debugging.
     */
    public static final String TAG = "MainActivity";

    /**
     * Intent request codes.
     */
    private static final int REQUEST_CONNECT_DEVICE = 6;
    private static final int REQUEST_ENABLE_BT = 7;
    private static final int AUDIO_RECORD_PERMISSION_REQUEST = 8;
    private static final int AUDIO_RECORDING = 9;

    private List<Integer> mRealtimeValues = new ArrayList<>();

    private List<String> mTimeLineList = new ArrayList<>();
    private List<Float> mAirQualityAllDayList = new ArrayList<>();
    private List<Float> mTemperatureAllDayList = new ArrayList<>();
    private List<Float> mHumidityAllDayList = new ArrayList<>();

    private String[] mTabInitStrings;

    private String[] mValueUnits;

    /**
     * New device to connect to.
     */
    private String mNewDeviceAddress = "";

    /**
     * Result from speech recognition.
     */
    private String mAudioResultString = null;

    /**
     * Text to speech service.
     */
    private VoiceOutput mVoiceOutput;

    private int mCurrentThemeColor;

    private TabLayout mTabLayout;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private ViewPager mViewPager;

    private FloatingActionButton mRecordAudioButton;

    private MainViewModel mMainViewModel;
    private ChartViewModel mChartViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkBluetoothSupported();

        // view model
        mMainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        mChartViewModel = new ViewModelProvider(this).get(ChartViewModel.class);

        mTabInitStrings = new String[]{
                getString(R.string.init_text_air_quality),
                getString(R.string.init_text_temperature),
                getString(R.string.init_text_humidity)
        };

        mValueUnits = new String[]{
                getString(R.string.unit_air_quality),
                getString(R.string.unit_temperature),
                getString(R.string.unit_humidity),
        };

        mCurrentThemeColor = getResources().getColor(R.color.air_quality_drawable_end_color);

        ChartPagerAdapter mChartPagerAdapter = new ChartPagerAdapter(getSupportFragmentManager());

        mViewPager = findViewById(R.id.pager);
        mViewPager.setAdapter(mChartPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {

            }

            @Override
            public void onPageScrollStateChanged(int i) {
                // Enable or disable mSwipeRefreshLayout, thanks to stack overflow!
                // To fix bug between mSwipeRefreshLayout and mViewPager
                enableDisableSwipeRefresh(i == ViewPager.SCROLL_STATE_IDLE);
            }
        });

        mTabLayout = findViewById(R.id.tabs);
        mTabLayout.setFillViewport(true);
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.addOnTabSelectedListener(this);

        mSwipeRefreshLayout = findViewById(R.id.refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mRecordAudioButton = findViewById(R.id.voice_input_button);
        mRecordAudioButton.setOnClickListener(v -> {
            //start speech recognition.
            if (mMainViewModel.isBluetoothConnected()) {
                promptSpeechRecognition();
            } else {
                mVoiceOutput.speak(LanguageTeacher.deviceNotFoundReply(getApplicationContext()));
                Toast.makeText(getApplicationContext(), LanguageTeacher.deviceNotFoundReply(getApplicationContext()), Toast.LENGTH_SHORT).show();
            }
        });

        initTabString();
        changeTabIndicatorColor();

        // Initialize text to speech
        mVoiceOutput = new VoiceOutput(getApplicationContext());

        mMainViewModel.getAllData().observe(this, dataSet -> {
            if (dataSet.size() > 0) {
                mTimeLineList.clear();
                mAirQualityAllDayList.clear();
                mTemperatureAllDayList.clear();
                mHumidityAllDayList.clear();
                for (int i = 0; i < dataSet.size(); i++) {
                    Data data = dataSet.get(i);
                    mTimeLineList.add(data.getDateTime());
                    mAirQualityAllDayList.add(data.getAirQuality());
                    mTemperatureAllDayList.add(data.getTemperature());
                    mHumidityAllDayList.add(data.getHumidity());
                }
                passAllDayDataToCurrentChart();
            }
        });

        mMainViewModel.getRealtimeValues().observe(this, values -> {
            if (values.size() > 0) {
                mRealtimeValues.clear();
                mRealtimeValues.addAll(values);

                for (int i = 0; i < values.size(); i++) {
                    TabLayout.Tab tab = mTabLayout.getTabAt(i);
                    if (tab != null)
                        tab.setText(values.get(i) + mValueUnits[i]);
                }

                List<Float> valuesToChart = new ArrayList<>();
                for (int value : values)
                    valuesToChart.add((float) value);
                passRealtimeDataToChart(valuesToChart);
            }
        });

        mMainViewModel.isRefreshing().observe(this, state -> mSwipeRefreshLayout.setRefreshing(state));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.open_bluetooth_device_dialog:
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            case R.id.app_info:
                // TODO: Launch app information dialog.
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        // If bluetooth is not on, request that it be enabled.
        // setupBluetoothConnection() will the be called during onActivityResult.
        if (!mMainViewModel.isBluetoothEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            //Otherwise, setup the connect section.
        } else if (mMainViewModel.isBluetoothServiceNull()) {
            mMainViewModel.setupBluetoothConnection();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mMainViewModel.onResumeCheck();

        // Swipe refresh layout
        mSwipeRefreshLayout.post(() -> {
            if (!mMainViewModel.isBluetoothServiceNull())
                mMainViewModel.connectDevice(mNewDeviceAddress);
        });

        // when speech recognition have result
        if (mAudioResultString != null) {
            Log.d(TAG, "result : " + mAudioResultString);
            mAudioResultString = mAudioResultString.replace(" ", "");
            LanguageTeacher chineseTeacher = new LanguageTeacher();
            String toSend = chineseTeacher.searchKeyWords(mAudioResultString);
            String toSay = "";
            mMainViewModel.sendMessageViaBluetooth(toSend);

            TabLayout.Tab tab = null;

            switch (toSend) {
                case "o":
                case "f":
                    toSay = LanguageTeacher.IUnderstandReply();
                    break;
                case "a":
                    tab = mTabLayout.getTabAt(0);

                    toSay = getString(R.string.text_to_speech_air_quality_prefix)
                            + mRealtimeValues.get(0)
                            + getString(R.string.text_to_speech_air_quality_suffix);

                    mMainViewModel.changeChartTimeLineType(ChartTimeLineType.REALTIME);
                    break;
                case "t":
                    tab = mTabLayout.getTabAt(1);

                    toSay = getString(R.string.text_to_speech_temperature_prefix)
                            + mRealtimeValues.get(1)
                            + getString(R.string.text_to_speech_temperature_suffix);

                    mMainViewModel.changeChartTimeLineType(ChartTimeLineType.REALTIME);
                    break;
                case "w":
                    tab = mTabLayout.getTabAt(2);

                    toSay = getString(R.string.text_to_speech_humidity_prefix)
                            + mRealtimeValues.get(2)
                            + getText(R.string.unit_humidity);

                    mMainViewModel.changeChartTimeLineType(ChartTimeLineType.REALTIME);
                    break;
                case "h":
                    toSay = LanguageTeacher.sayHello();
                    break;
                case "m":
                case "b":
                    toSay = LanguageTeacher.getSeveralCatMews();
                    break;

                case "d":
                    toSay = LanguageTeacher.IDoNotKnowReply();
            }

            mVoiceOutput.speak(toSay);

            if (tab != null)
                tab.select();

            mAudioResultString = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // shutdown text to speech
        mVoiceOutput.shutdown();
    }

    public void checkBluetoothSupported() {
        // Get local bluetooth adapter to check if this device supports bluetooth or not.
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // if the adapter is null, then bluetooth is not supported
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), getString(R.string.bt_not_supported), Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }

    /**
     * After permission request...
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @Nullable String[] permissions, @Nullable int[] grantResults) {
        // If the request is canceled, the result array will be empty
        if (requestCode == AUDIO_RECORD_PERMISSION_REQUEST) {
            assert grantResults != null;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Congratulation! We have the permission.
                startRecognition();
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);  // here

        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    mSwipeRefreshLayout.setRefreshing(true);
                    // Get the device MAC address.
                    mNewDeviceAddress = Objects.requireNonNull(data.getExtras()).getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                }
                break;

            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a bluetooth connection.
                    mMainViewModel.setupBluetoothConnection();
                } else {
                    // User did not enable Bluetooth or an error occurred.
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    this.finish();
                }
                break;  // Need to check if this break will cause a bug or not.

            case AUDIO_RECORDING:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    mAudioResultString = result.get(0);
                }

        }
    }

    /**
     * On refresh layout refresh
     */
    @Override
    public void onRefresh() {
        initTabString();
        mMainViewModel.refresh();
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        mViewPager.setCurrentItem(tab.getPosition());
        // change status bar color
        changeTabIndicatorColor();
        passAllDayDataToCurrentChart();
        mChartViewModel.switchPage(tab.getPosition());
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    private void passAllDayDataToCurrentChart() {
        int chartIndex = mViewPager.getCurrentItem();
        Map<String, Float> dataToChart = new TreeMap<>();
        List<Float> values = new ArrayList<>();
        switch (chartIndex) {
            case 0:
                values.addAll(mAirQualityAllDayList);
                break;
            case 1:
                values.addAll(mTemperatureAllDayList);
                break;
            case 2:
                values.addAll(mHumidityAllDayList);
        }
        for (int i = 0; i < mTimeLineList.size(); i++)
            dataToChart.put(mTimeLineList.get(i), values.get(i));
        mChartViewModel.setAllDayData(dataToChart);
    }

    private void passRealtimeDataToChart(List<Float> values) {
        int chartIndex = mViewPager.getCurrentItem();
        mChartViewModel.updateRealtimeData(values, chartIndex);
    }

    /**
     * Enable or disable mSwipeRefreshLayout, thanks to stack overflow!
     * To fix bug between mSwipeRefreshLayout and mViewPager
     */
    private void enableDisableSwipeRefresh(boolean enable) {
        if ((mSwipeRefreshLayout != null) && !mSwipeRefreshLayout.isRefreshing()) {
            mSwipeRefreshLayout.setEnabled(enable);
        }
    }

    private void initTabString() {
        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = mTabLayout.getTabAt(i);
            if (tab != null)
                tab.setText(mTabInitStrings[i]);
        }
    }

    /**
     * Change status bar color
     */
    private void changeTabIndicatorColor() {
        final int colorIndex = mViewPager.getCurrentItem();
        final int newColor;

        switch (colorIndex) {
            case 1:
                newColor = getResources().getColor(R.color.temperature_drawable_end_color);
                break;
            case 2:
                newColor = getResources().getColor(R.color.humidity_drawable_end_color);
                break;
            default:
                newColor = getResources().getColor(R.color.air_quality_drawable_end_color);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
            animator.addUpdateListener(animation -> {
                // Use animation position to blend colors
                float position = animation.getAnimatedFraction();

                // Apply blended color to the status bar
                mCurrentThemeColor = blendColors(mCurrentThemeColor, newColor, position);

                // tab pager indicator
                mTabLayout.setSelectedTabIndicatorColor(mCurrentThemeColor);

                // button's tint color
                mRecordAudioButton.getDrawable().mutate().setTint(mCurrentThemeColor);
            });

            animator.setDuration(256).start();
        }
    }

    /**
     * Blend colors for changing status bar color
     */
    private int blendColors(int from, int to, float ratio) {
        final float inverseRatio = 1f - ratio;

        final float r = Color.red(to) * ratio + Color.red(from) * inverseRatio;
        final float g = Color.green(to) * ratio + Color.green(from) * inverseRatio;
        final float b = Color.blue(to) * ratio + Color.blue(from) * inverseRatio;

        return Color.rgb((int) r, (int) g, (int) b);
    }

    /**
     * Check record audio permission, if so, start speech recognition.
     */
    private void promptSpeechRecognition() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted.
            // Should we show a explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.permission_message_record_audio))
                        .setTitle(getString(R.string.permission_required));
                builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(
                                new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_RECORD_PERMISSION_REQUEST);
                    }
                })
                        .setNegativeButton(getString(R.string.no), null).show();
            } else {
                // No explanation needed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(
                            new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_RECORD_PERMISSION_REQUEST);
                }
            }
        } else {
            // Permission has been granted.
            startRecognition();
        }
    }

    /**
     * Start speech recognition
     */
    private void startRecognition() {
        // Voice recognition start recording
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, R.string.start_recognition_prompt);

        try {
            startActivityForResult(intent, AUDIO_RECORDING);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(this, getString(R.string.speech_recognition_not_supported), Toast.LENGTH_SHORT).show();
        }
    }
}
