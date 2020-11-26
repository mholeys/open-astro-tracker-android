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

    public static final int DEVICE_CONNECTED = 0x222001;
    public static final int DEVICE_DISCONNECTED = 0x222002;
    public static final int DEVICE_FAILED = 0x222003;
    public static final int DEVICE_FAILED_DISCONNECT = 0x222004;
    public static final int DEVICE_CONNECTING = 0x222005;

    private static final String TAG = "BluetoothSearch";
    private boolean bluetooth = false;
    private Activity activity;
    private BluetoothAdapter adapter;

    public MutableLiveData<Set<BluetoothDevice>> obsDevices = new MutableLiveData<>();
    private Set<BluetoothDevice> devices = new HashSet<>();
    private ConnectThread connectThread;
    private BluetoothSocket client;
    private BluetoothDevice device;
    private Mount mount;

//    public Handler bluetoothHandler;
    public Handler handler;
    //    public MountBluetoothConnectionService service;
    private static final int GET_RA_STEPS_PER_DEG = 1;

    public void setup(Activity activity, Handler handler) {
        this.activity = activity;
        this.handler = handler;
        obsDevices.postValue(devices);

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

    private void onConnected(BluetoothSocket client) {
        this.client = client;
        Log.d(TAG, "onConnected: Connected");
        // Start processing data
//        service = new MountBluetoothConnectionService(client, bluetoothHandler);
//        disconnect();
        try {
            mount = new Mount(client, handler);
            mount.getSiteLatitude();
            mount.getSiteLongitude();
            // Set LST?
            mount.getPosition();
            mount.getRAStepsPerDegree();
            mount.getDecStepsPerDegree();
            mount.getSpeedFactor();
            mount.getHA();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (mount != null) {
            mount.close();
        }
        if (client != null) {
            boolean wasConnected = client.isConnected();
            try {
                client.close();
                Log.d(TAG, "disconnected");
                if (wasConnected) {
                    Message writtenMsg = handler.obtainMessage(DEVICE_DISCONNECTED, -0, -1, device.getName());
                    writtenMsg.sendToTarget();
                }
            } catch (IOException e) {
                Message writtenMsg = handler.obtainMessage(DEVICE_FAILED_DISCONNECT, -0, -1, device.getName());
                writtenMsg.sendToTarget();
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

    public void connectClient(BluetoothDevice device) {
        if (!bluetooth) return;
        if (connectThread != null) {
            connectThread.cancel();
        }
        Log.d(TAG, "connectClient: Connecting");
        UUID id = UUID.fromString(activity.getString(R.string.bluetooth_sdp_uuid));
        connectThread = new ConnectThread(device, id);
        connectThread.start();
        this.device = device;
    }

    public void refresh() {
        obsDevices.postValue(devices);
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
                Message writtenMsg = handler.obtainMessage(DEVICE_FAILED, -0, -1, device.getName());
                writtenMsg.sendToTarget();
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            Log.d(TAG, "run: Connected! got socket");
            Message writtenMsg = handler.obtainMessage(DEVICE_CONNECTED, -0, -1, device.getName());
            writtenMsg.sendToTarget();
            onConnected(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
                Message writtenMsg = handler.obtainMessage(DEVICE_DISCONNECTED, -0, -1, device.getName());
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
                Message writtenMsg = handler.obtainMessage(DEVICE_FAILED_DISCONNECT, -0, -1, device.getName());
                writtenMsg.sendToTarget();
            }
        }
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
