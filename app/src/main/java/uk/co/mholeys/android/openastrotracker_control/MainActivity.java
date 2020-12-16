package uk.co.mholeys.android.openastrotracker_control;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.HashSet;
import java.util.ServiceConfigurationError;
import java.util.Set;

import kotlin.NotImplementedError;
import uk.co.mholeys.android.openastrotracker_control.comms.model.TelescopePosition;
import uk.co.mholeys.android.openastrotracker_control.mount.Mount;
import uk.co.mholeys.android.openastrotracker_control.mount.OTAState;
import uk.co.mholeys.android.openastrotracker_control.ui.connection.IDevice;

public class MainActivity extends AppCompatActivity implements ISearcherControl {

    private static final String TAG = "MainAct";

    private EConnectionType connectionType = EConnectionType.UNKNOWN;
    private BluetoothSearch bluetoothSearch;
    private Handler handler;
    private MutableLiveData<Set<IDevice>> bluetoothDevices = new MutableLiveData<>();
    private MountViewModel mountViewModel;

    private OTAState state;

    enum ConnectionState { CONNECTING, CONNECTED, DISCONNECTED, FAILED, UNKNOWN };
    ConnectionState connectionState = ConnectionState.UNKNOWN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_control, R.id.navigation_polar_align, R.id.navigation_connection)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        mountViewModel = new ViewModelProvider(this).get(MountViewModel.class);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (state == null) state = OTAState.getInstance();
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    StringBuilder result = new StringBuilder();
                    Object o = msg.obj;
                    switch (msg.what) {
                        case BluetoothSearch.DEVICE_CONNECTING:
//                        Toast.makeText(MainActivity.this, "Connecting to " + msg.obj, Toast.LENGTH_SHORT).show();
                            setConnectionState(ConnectionState.CONNECTING);
                            break;
                        case BluetoothSearch.DEVICE_CONNECTED:
                            Toast.makeText(MainActivity.this, "Connected to " + msg.obj, Toast.LENGTH_SHORT).show();
                            setConnectionState(ConnectionState.CONNECTED);
                            // TODO: Start Service? That handles control/threads?
                            startMountService(bluetoothSearch.getClient());
                            break;
                        case BluetoothSearch.DEVICE_DISCONNECTED:
                            Toast.makeText(MainActivity.this, "Disconnected from " + msg.obj, Toast.LENGTH_SHORT).show();
                            setConnectionState(ConnectionState.DISCONNECTED);
                            break;
                        case BluetoothSearch.DEVICE_FAILED_DISCONNECT:
//                        Toast.makeText(MainActivity.this, "Failed to disconnect from " + msg.obj, Toast.LENGTH_SHORT).show();
                            setConnectionState(ConnectionState.UNKNOWN);
                            break;
                        case BluetoothSearch.DEVICE_FAILED:
                            Toast.makeText(MainActivity.this, "Failed to connect to " + msg.obj, Toast.LENGTH_SHORT).show();
                            setConnectionState(ConnectionState.FAILED);
                            break;
                    }
                }
            };
        }
    }

    private void startMountService(BluetoothSocket client) {
        Intent intent = new Intent(this, OTAService.class);
        intent.putExtra("bluetooth-socket", client);
        startService(intent);
    }

    private void setConnectionState(ConnectionState newState) {
        connectionState = newState;
        // TODO: show little state ui object
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothSearch.receiver);
    }

    @Override
    public void setup(EConnectionType type) {
        switch (type) {
            case BLUETOOTH:
                if (bluetoothSearch != null) {
//                    bluetoothSearch.disconnect();
                } else {
                    bluetoothSearch = new BluetoothSearch();
                }
                bluetoothSearch.setup(this, handler);
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(bluetoothSearch.receiver, filter);
                if (bluetoothDevices == null) {
                    bluetoothDevices = new MutableLiveData<>();
                }
                if (!bluetoothSearch.obsDevices.hasActiveObservers()) {
                    bluetoothSearch.obsDevices.observe(this, new Observer<Set<BluetoothDevice>>() {
                        @Override
                        public void onChanged(Set<BluetoothDevice> devices) {
                            Set<IDevice> newDevices = new HashSet<>();
                            if (!bluetoothSearch.isBluetoothEnabled()) {
                                bluetoothDevices.postValue(newDevices);
                                return;
                            }
                            for (final BluetoothDevice d : devices) {
                                IDevice id = new IDevice() {
                                    @Override
                                    public String getName() {
                                        return d.getName();
                                    }

                                    @Override
                                    public String getAddress() {
                                        return d.getAddress();
                                    }

                                    @Override
                                    public String getType() {
                                        return "Bluetooth";
                                    }

                                    @Override
                                    public Object getDevice() {
                                        return d;
                                    }
                                };
                                String c = "";
                                if (d.getBluetoothClass() != null) {
                                    c = String.valueOf(d.getBluetoothClass().getDeviceClass());
                                }
                                Log.d(TAG, "Found " + d.getName() + " " + d.getAddress() + " " + c);
                                newDevices.add(id);
                            }
                            bluetoothDevices.postValue(newDevices);
                        }
                    });
                }
                break;
            default:
                throw new NotImplementedError();
        }
        connectionType = type;
    }

    @Override
    public void connect(IDevice device) {
        switch (connectionType) {
            case BLUETOOTH:
                if (device != null && bluetoothSearch != null) {
                    if (!bluetoothSearch.hasBluetooth()) return;
                    Log.d(TAG, "onClick: attempting to connect");
                    if (device.getType().equalsIgnoreCase("bluetooth")) {
                        bluetoothSearch.connectClient((BluetoothDevice) device.getDevice());

                    } else {
                        Log.d(TAG, "onClick: Unknown device type");
                    }
                }
                break;
            default:
//                Log.w(TAG, "connect: Not implemented " + connectionType);
                Log.w(TAG, "connect: " + connectionType + "Not implemented");
                break;
        }
    }

    @Override
    public LiveData<Set<IDevice>> getDevices() {
        switch (connectionType) {
            case BLUETOOTH:
                return bluetoothDevices;
            default:
                return null;
        }
    }

    @Override
    public void disconnect() {
        switch (connectionType) {
            case BLUETOOTH:
                bluetoothSearch.disconnect();
                break;
            default:
                break;
        }
    }

    @Override
    public EConnectionType getType() {
        return connectionType;
    }

    @Override
    public Handler getHandler() {
        return this.handler;
    }

    @Override
    public void discover() {
        switch (connectionType) {
            case BLUETOOTH:
                bluetoothSearch.disconnect();
                bluetoothSearch.discoverDevices();
                break;
            default:
                Log.w(TAG, "discover: " + connectionType + " Not implemented");
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case BluetoothSearch.REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    // Start searching/connect
                    bluetoothSearch.enableBluetoothUse();
                    discover();
                } else {
                    // Handle disabled bluetooth
                    bluetoothSearch.disableBluetoothUse();
                }
                break;
            default:
                break;
        }
    }

}
