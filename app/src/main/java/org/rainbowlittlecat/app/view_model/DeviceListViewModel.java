package org.rainbowlittlecat.app.view_model;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.rainbowlittlecat.app.R;
import org.rainbowlittlecat.app.bluetooth.Device;

import java.util.ArrayList;
import java.util.Set;

/**
 * ViewModel for handling configuration changed.
 */
public class DeviceListViewModel extends AndroidViewModel {
    private BluetoothAdapter mBtAdapter;

    private MutableLiveData<String> title;
    private MutableLiveData<Boolean> enableScanning;

    private MutableLiveData<ArrayList<Device>> devices;
    private ArrayList<String> names = new ArrayList<>();
    private ArrayList<String> addresses = new ArrayList<>();

    public DeviceListViewModel(@NonNull Application application) {
        super(application);
    }

    private void setTitle(String title) {
        this.title.setValue(title);
    }

    public LiveData<String> getTitle() {
        if (title == null) {
            title = new MutableLiveData<>();
            String text = getApplication().getResources().getString(R.string.select_device);
            title.setValue(text);
        }
        return title;
    }

    private void setEnableScanning(boolean state) {
        enableScanning.setValue(state);
    }

    public LiveData<Boolean> isAbleToScan() {
        if (enableScanning == null) {
            enableScanning = new MutableLiveData<>();
            setEnableScanning(true);
        }
        return enableScanning;
    }

    public LiveData<ArrayList<Device>> getData() {
        if (devices == null) {
            devices = new MutableLiveData<>();
            loadData();
        }

        return devices;
    }

    private void loadData() {
        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getApplication().registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        getApplication().registerReceiver(mReceiver, filter);

        //Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                names.add(device.getName());
                addresses.add(device.getAddress());
            }
        } else {
            String noDevices = getApplication().getResources().getText(R.string.none_paired).toString();
            names.add(noDevices);
            addresses.add("");
        }

        makeData();
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null)
            mBtAdapter.cancelDiscovery();

        //Unregister broadcast listeners
        getApplication().unregisterReceiver(mReceiver);
    }

    /**
     * Start device discovery with the BluetoothAdapter
     */
    public void doDiscovery() {
        setEnableScanning(false);

        // Indicate scanning in the title
        setTitle(getApplication().getResources().getString(R.string.scanning));

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering())
            mBtAdapter.cancelDiscovery();

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    public void cancelDiscovery() {
        mBtAdapter.cancelDiscovery();
    }

    /**
     * The BroadcastReceiver that listens for discovered devices and changes
     * the title when discovery is finished
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED && !addresses.contains(device.getAddress())) {
                    names.add(device.getName());
                    addresses.add(device.getAddress());
                    Toast.makeText(getApplication(), device.getName(), Toast.LENGTH_SHORT).show();
                }

                makeData();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setTitle(getApplication().getResources().getString(R.string.select_device));
                setEnableScanning(true);
            }
        }
    };

    private void makeData() {
        ArrayList<Device> list = new ArrayList<>();
        for (int i = 0; i < names.size(); i++)
            list.add(new Device(names.get(i), addresses.get(i)));
        devices.setValue(list);
    }
}
