package uk.co.mholeys.android.openastrotracker_control;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import uk.co.mholeys.android.openastrotracker_control.comms.handlers.BluetoothCommunicationHandler;

public class BluetoothSearch {

    public static final int REQUEST_ENABLE_BT = 0x19FF;
    private static final String TAG = "BluetoothSearch";
    private boolean bluetooth = false;
    private Activity activity;
    private BluetoothAdapter adapter;

    public MutableLiveData<Set<BluetoothDevice>> obsvDevices = new MutableLiveData<>();
    private Set<BluetoothDevice> devices = new HashSet<>();
    private ConnectThread connectThread;
    private BluetoothSocket client;
    private BluetoothDevice device;

    public Handler bluetoothHandler;
    public MountBluetoothConnectionService service;

    public void setup(Activity activity) {
        this.activity = activity;
        obsvDevices.postValue(devices);

        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            // Handle no bluetooth on device
            bluetooth = false;
            return;
        }
        bluetooth = true;

        if (!adapter.isEnabled()) {
            // Bluetooth is disabled, so prompt/request them to turn it on
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    public Set<BluetoothDevice> getPairedDevices() {
        if (!bluetooth) return null;
        Set<BluetoothDevice> devices = adapter.getBondedDevices();
        this.devices.addAll(devices);
        obsvDevices.postValue(this.devices);
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

    private void onConnected(BluetoothSocket client) {
        this.client = client;
        Log.d(TAG, "onConnected: Connected");
        // Start processing data
        service = new MountBluetoothConnectionService(client, bluetoothHandler);
//        disconnect();
    }

    public void disconnect() {
        if (client != null) {
            try {
                client.close();
                Log.d(TAG, "disconnected");
            } catch (IOException e) {
                // ?
                e.printStackTrace();
            }
        }
    }

    public final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                devices.add(device);
                obsvDevices.postValue(devices);
            }
        }
    };

    public void disableBluetoothUse() {
        bluetooth = false;
    }

    public boolean hasBluetooth() {
        return bluetooth;
    }

    public void connectClient(BluetoothDevice device) {
        if (!bluetooth) return;
        Log.d(TAG, "connectClient: Connecting");
        UUID id = UUID.fromString(activity.getString(R.string.bluetooth_sdp_uuid));
        connectThread = new ConnectThread(device, id);
        connectThread.start();
        this.device = device;
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device, UUID id) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(id);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            adapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                Log.d(TAG, "run: Could not connect");
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            Log.d(TAG, "run: Connected! got socket");
            onConnected(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

}
