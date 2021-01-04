package uk.co.mholeys.android.openastrotracker_control;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.midi.MidiOutputPort;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
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

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kotlin.NotImplementedError;
import uk.co.mholeys.android.openastrotracker_control.mount.Mount;
import uk.co.mholeys.android.openastrotracker_control.mount.OTAState;
import uk.co.mholeys.android.openastrotracker_control.ui.connection.IDevice;

public class MainActivity extends AppCompatActivity implements ISearcherControl {

    private static final String TAG = "MainAct";

    public static final int SETUP_CLIENT_MESSENGER = 512345;

    private EConnectionType connectionType = EConnectionType.UNKNOWN;
    private BluetoothSearch bluetoothSearch;
    private Handler handler;
    private MutableLiveData<Set<IDevice>> bluetoothDevices = new MutableLiveData<>();
    private MountViewModel mountViewModel;

    private OTAState state;
    Messenger incomingMessenger;
    public Messenger serviceMessenger;
    public boolean bound = false;


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
    protected void onStop() {
        super.onStop();
        if (bound && connection != null) {
            unbindService(connection);
        }
        bound = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (state == null) state = OTAState.getInstance();

        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);
        boolean running = false;
        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals(BluetoothOTAService.class.getName())){
                running = true;
            }
        }
        if (running) {
            Intent bindIntent = new Intent(this, BluetoothOTAService.class);
            bindService(bindIntent, connection, Context.BIND_AUTO_CREATE);
        }
        incomingMessenger = new Messenger(new OTAHandler(this));
    }

    private void startMountService(BluetoothDevice device) {
        Intent intent = new Intent(this, BluetoothOTAService.class);
        intent.putExtra("bluetooth-device", device);
        startService(intent);
        Intent bindIntent = new Intent(this, BluetoothOTAService.class);
        bindService(bindIntent, connection, Context.BIND_AUTO_CREATE);
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
                bluetoothSearch.setup(this);
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
                        // Start service to connect to OTA
                        startMountService((BluetoothDevice) device.getDevice());
//                        bluetoothSearch.connectClient((BluetoothDevice) device.getDevice());
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
                Intent intent = new Intent(this, BluetoothOTAService.class);
                stopService(intent);
                unbindService(connection);
                unregisterReceiver(bluetoothSearch.receiver);
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
                Intent intent = new Intent(this, BluetoothOTAService.class);
                stopService(intent);
                bluetoothSearch.discoverDevices();
                break;
            default:
                Log.w(TAG, "discover: " + connectionType + " Not implemented");
                break;
        }
    }

    @Override
    public Messenger getMessenger() {
        if (bound) {
            return serviceMessenger;
        }
        return null;
    }

    @Override
    public boolean isBound() {
        return bound;
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

    private final ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            serviceMessenger = new Messenger(service);
            bound = true;
            Message msg = Message.obtain(null, SETUP_CLIENT_MESSENGER,0,0);
            try {
                // Important bit
                msg.replyTo = incomingMessenger;
                serviceMessenger.send(msg);
            } catch (RemoteException e) {
                // Service ended before setup
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };


    // TODO: implement all the mount messages
    // EXAMPLE
//    public void sendMoveRequest() {
//        if (!bound) return;
//        Bundle b = new Bundle();
//        Message msg = Message.obtain(null, Mount.GET_DEC_STEPS_PER_DEG,0,0);
//        try {
//            // Important bit
//            msg.replyTo = serviceMessenger;
//            serviceMessenger.send(msg);
//        } catch (RemoteException e) {
//            // In this case the service has crashed before we could even
//            // do anything with it; we can count on soon being
//            // disconnected (and then reconnected if it can be restarted)
//            // so there is no need to do anything here.
//            setConnectionState(ConnectionState.DISCONNECTED);
//        }
//    }
    static class OTAHandler extends Handler {

        private final WeakReference<MainActivity> mActivity;

        public OTAHandler(MainActivity activity) {
            this.mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "OTAHandler: " + msg);
            switch (msg.what) {
                case BluetoothOTAService.DEVICE_CONNECTING:
//                        Toast.makeText(MainActivity.this, "Connecting to " + msg.obj, Toast.LENGTH_SHORT).show();
                    mActivity.get().setConnectionState(ConnectionState.CONNECTING);
                    break;
                case BluetoothOTAService.DEVICE_CONNECTED:
                    Toast.makeText(mActivity.get(), "Connected to " + msg.obj, Toast.LENGTH_SHORT).show();
                    mActivity.get().setConnectionState(ConnectionState.CONNECTED);
                    break;
                case BluetoothOTAService.DEVICE_DISCONNECTED:
                    Log.d(TAG, "handleMessage: Failed to connect msg from service");
                    Toast.makeText(mActivity.get(), "Disconnected from " + msg.obj, Toast.LENGTH_SHORT).show();
                    unbindAndChangeState(ConnectionState.DISCONNECTED);
                    break;
                case BluetoothOTAService.DEVICE_FAILED_DISCONNECT:
//                        Toast.makeText(MainActivity.this, "Failed to disconnect from " + msg.obj, Toast.LENGTH_SHORT).show();
                    unbindAndChangeState(ConnectionState.UNKNOWN);
                    break;
                case BluetoothOTAService.DEVICE_FAILED:
                    Toast.makeText(mActivity.get(), "Failed to connect to " + msg.obj, Toast.LENGTH_SHORT).show();
                    unbindAndChangeState(ConnectionState.FAILED);
                    break;
                // Mount updates
                case Mount.REFRESH_MOUNT_STATE:

            }
        }

        private void unbindAndChangeState(ConnectionState state) {
            if (mActivity.get().bound && mActivity.get().connection != null) {
                mActivity.get().unbindService(mActivity.get().connection);
            }
            mActivity.get().bound = false;
            mActivity.get().setConnectionState(state);
        }

    }

}
