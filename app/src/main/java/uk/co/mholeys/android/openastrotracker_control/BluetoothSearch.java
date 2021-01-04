package uk.co.mholeys.android.openastrotracker_control;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import uk.co.mholeys.android.openastrotracker_control.comms.model.OTAEpoch;
import uk.co.mholeys.android.openastrotracker_control.comms.model.TelescopePosition;
import uk.co.mholeys.android.openastrotracker_control.mount.Mount;
import uk.co.mholeys.android.openastrotracker_control.ui.connection.ConnectionFragment;

public class BluetoothSearch {

    public static final int REQUEST_ENABLE_BT = 0x19FF;

    private static final String TAG = "BluetoothSearch";
    private boolean bluetooth = false;
    private Activity activity;
    private BluetoothAdapter adapter;

    public MutableLiveData<Set<BluetoothDevice>> obsDevices = new MutableLiveData<>();
    private Set<BluetoothDevice> devices = new HashSet<>();


    public void setup(Activity activity) {
        this.activity = activity;
        obsDevices.postValue(devices);

        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            // Handle no bluetooth on phone
            bluetooth = false;
            return;
        }
        bluetooth = true;

        if (!adapter.isEnabled()) {
            // Bluetooth is disabled, so prompt/request them to turn it on
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            bluetooth = false;
        }
    }

    public Set<BluetoothDevice> getPairedDevices() {
        if (!bluetooth) return null;
        Set<BluetoothDevice> devices = adapter.getBondedDevices();
        this.devices.addAll(devices);
        obsDevices.postValue(this.devices);
        return devices;
    }

    public void discoverDevices() {
        if (adapter.isEnabled()) {
            bluetooth = true;
        }
        if (!bluetooth) return;
        getPairedDevices();
        if (!adapter.startDiscovery()) {
            Toast.makeText(activity, "Failed to discover devices", Toast.LENGTH_SHORT).show();
        }
    }

//    private void onConnected(BluetoothSocket client) {
//        this.client = client;
//        Log.d(TAG, "onConnected: Connected");
//        Message writtenMsg = handler.obtainMessage(DEVICE_CONNECTED, -0, -1, device.getName());
//        writtenMsg.sendToTarget();
//
//        // Start processing data
////        service = new MountBluetoothConnectionService(client, bluetoothHandler);
////        disconnect();
//    }

//    public void disconnect() {
//        if (client != null) {
//            boolean wasConnected = client.isConnected();
//            try {
//                client.close();
//                Log.d(TAG, "disconnected");
//                if (wasConnected) {
//                    Message writtenMsg = handler.obtainMessage(DEVICE_DISCONNECTED, -0, -1, device.getName());
//                    writtenMsg.sendToTarget();
//                }
//            } catch (IOException e) {
//                Message writtenMsg = handler.obtainMessage(DEVICE_FAILED_DISCONNECT, -0, -1, device.getName());
//                writtenMsg.sendToTarget();
//                e.printStackTrace();
//            }
//        }
//    }

    public final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                devices.add(device);
                obsDevices.postValue(devices);
            }
        }
    };

    public void enableBluetoothUse() {
        bluetooth = true;
    }

    public void disableBluetoothUse() {
        bluetooth = false;
    }

    /** Does not actually check bluetooth state! Just if we are allowing use, based on cached state */
    public boolean hasBluetooth() {
        return bluetooth;
    }

    public void refresh() {
        obsDevices.postValue(devices);
    }

    public void addSavedDevices(ArrayList<Parcelable> parcelables) {
        for (Parcelable p : parcelables) {
            if (p instanceof BluetoothDevice) {
                devices.add((BluetoothDevice) p);
            }
        }
        obsDevices.postValue(devices);
    }

    public boolean isBluetoothEnabled() {
        if (adapter == null) return false;
        return adapter.isEnabled();
    }

}
