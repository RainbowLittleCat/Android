/*
 * Changed from
 *     https://github.com/googlearchive/android-BluetoothChat/blob/master/Application/src/main/java/com/example/android/bluetoothchat/DeviceListActivity.java
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import org.rainbowlittlecat.app.recyclerview.RecyclerViewAdapter;
import org.rainbowlittlecat.app.R;
import org.rainbowlittlecat.app.recyclerview.ItemDivider;
import org.rainbowlittlecat.app.bluetooth.Device;
import org.rainbowlittlecat.app.view_model.DeviceListViewModel;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * activity in the result Intent.
 */
public class DeviceListActivity extends AppCompatActivity implements RecyclerViewAdapter.OnItemClickedListener {

    /**
     * Return Intent extra
     */
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    /**
     * Member fields
     */
    private ArrayList<String> deviceNameList = new ArrayList<>();
    private ArrayList<String> deviceAddressList = new ArrayList<>();

    private RecyclerViewAdapter mPairedDeviceAdapter;

    private Toolbar mToolbar;

    private DeviceListViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.device_list_layout);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        mToolbar = findViewById(R.id.toolbar);
        mToolbar.inflateMenu(R.menu.bluetooth_device_list_menu);
        setSupportActionBar(mToolbar);

        TextView title = findViewById(R.id.toolbar_title);

        mPairedDeviceAdapter = new RecyclerViewAdapter(deviceNameList);
        mPairedDeviceAdapter.setOnItemClickedListener(this);

        RecyclerView.LayoutManager mPairedDeviceManager = new LinearLayoutManager(getApplicationContext());
        RecyclerView mDevicesList = findViewById(R.id.device_list);
        mDevicesList.setHasFixedSize(true);  // There might need to change, cause the list is not fixed size.
        mDevicesList.setLayoutManager(mPairedDeviceManager);
        mDevicesList.addItemDecoration(new ItemDivider(getApplicationContext(), ItemDivider.VERTICAL_LIST, 16));
        mDevicesList.setAdapter(mPairedDeviceAdapter);

        viewModel = new ViewModelProvider(this).get(DeviceListViewModel.class);
        viewModel.getTitle().observe(this, title::setText);
        viewModel.getData().observe(this, devices -> {
            mDevicesList.setHasFixedSize(false);

            deviceNameList.clear();
            deviceAddressList.clear();

            for (Device device : devices) {
                deviceNameList.add(device.getName());
                deviceAddressList.add(device.getAddress());
            }

            mPairedDeviceAdapter.notifyDataSetChanged();
            mDevicesList.setHasFixedSize(true);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.bluetooth_device_list_menu, menu);
        viewModel.isAbleToScan().observe(this, visibility -> mToolbar.getMenu().getItem(0).setVisible(visibility));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.open_bluetooth_device_dialog) {
            viewModel.doDiscovery();
            item.setVisible(false);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * The on-click listener for all devices in the RecyclerView.
     */
    @Override
    public void onItemClicked(int position) {
        // Cancel discovery because it's costly and we're about to connect
        viewModel.cancelDiscovery();

        // Get the device MAC address, which is the last 17 chars in the View
        String info = deviceAddressList.get(position);
        Toast.makeText(this, deviceNameList.get(position) + "\n" + info, Toast.LENGTH_SHORT).show();

        // Prevent the error from click the "no other device" list item.
        if (!info.equals(getResources().getString(R.string.none_found))) {
            String address = info.substring(info.length() - 17);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }
}
