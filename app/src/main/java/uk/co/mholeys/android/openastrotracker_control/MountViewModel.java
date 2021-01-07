package uk.co.mholeys.android.openastrotracker_control;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import kotlin.NotImplementedError;
import uk.co.mholeys.android.openastrotracker_control.comms.model.TelescopePosition;
import uk.co.mholeys.android.openastrotracker_control.mount.Mount;
import uk.co.mholeys.android.openastrotracker_control.mount.MountMessages;
import uk.co.mholeys.android.openastrotracker_control.ui.connection.IDevice;

public class MountViewModel extends AndroidViewModel implements ISearcherControl {
    private static final String TAG = "MountVM";

    private static MountViewModel instance;

    public static MountViewModel getInstance(ViewModelStoreOwner owner) {
        if (instance == null) {
             instance = new ViewModelProvider(owner).get(MountViewModel.class);
        }
        return instance;
    }

    private boolean registeredBluetooth;

    enum ConnectionState { CONNECTING, CONNECTED, DISCONNECTED, FAILED, UNKNOWN }
    MutableLiveData<ConnectionState> state = new MutableLiveData<>(ConnectionState.UNKNOWN);

    MutableLiveData<TelescopePosition> currentPosition = new MutableLiveData<>();
    MutableLiveData<TelescopePosition> objectPosition = new MutableLiveData<>();

    public enum TrackingState { TRACKING, NOT_TRACKING, PARKED, UNPARKED, UNKNOWN }
    MutableLiveData<TrackingState> trackingState = new MutableLiveData<>(TrackingState.UNKNOWN);
    MutableLiveData<Float> siteLatitude = new MutableLiveData<>();
    MutableLiveData<Float> siteLongitude = new MutableLiveData<>();
    MutableLiveData<Integer> raStepsPerDeg = new MutableLiveData<>();
    MutableLiveData<Integer> decStepsPerDeg = new MutableLiveData<>();
    MutableLiveData<Float> speedFactor = new MutableLiveData<>();
    MutableLiveData<String> ha = new MutableLiveData<>();
    MutableLiveData<Boolean> slewingState = new MutableLiveData<>();


    private boolean shouldUnbind = false;
    private boolean bound = false;
    Messenger incomingMessenger;

    private ISearcherControl.EConnectionType connectionType = ISearcherControl.EConnectionType.UNKNOWN;
    private BluetoothSearch bluetoothSearch;
//    private Handler handler;
    private MutableLiveData<Set<IDevice>> bluetoothDevices = new MutableLiveData<>();


    public MountViewModel(@NonNull Application application) {
        super(application);
    }

    public void startBluetoothService(BluetoothDevice device) {
        Intent intent = new Intent(getApplication(), BluetoothOTAService.class);
        intent.putExtra("bluetooth-device", device);

        getApplication().startService(intent);
        doBindService();
        incomingMessenger = new Messenger(new OTAHandler(this));
    }

    public void doBindService() {
        Intent bindIntent = new Intent(getApplication(), BluetoothOTAService.class);
        if (getApplication().bindService(bindIntent, connection, Context.BIND_AUTO_CREATE)) {
            shouldUnbind = true;
        }
    }
    public void doUnbindService() {
        if (shouldUnbind) {
            getApplication().unbindService(connection);
            shouldUnbind = false;
        }
    }

    public Messenger serviceMessenger;
    public static final int SETUP_CLIENT_MESSENGER = 512345;
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

    static class OTAHandler extends Handler {

        private static final String TAG = "MvmOTAh";
        private WeakReference<MountViewModel> viewModel;

        public OTAHandler(MountViewModel viewModel) {
            this.viewModel  = new WeakReference<>(viewModel);
        }

        @Override
        public void handleMessage(Message msg) {
            MountViewModel vm = this.viewModel.get();
            Object o = msg.obj;
            switch (msg.what) {
                case BluetoothOTAService.DEVICE_CONNECTING:
//                        Toast.makeText(MainActivity.this, "Connecting to " + msg.obj, Toast.LENGTH_SHORT).show();
                    vm.setConnectionState(MountViewModel.ConnectionState.CONNECTING);
                    break;
                case BluetoothOTAService.DEVICE_CONNECTED:
                    // TODO: Toast.makeText(mActivity.get(), "Connected to " + msg.obj, Toast.LENGTH_SHORT).show();
                    vm.setConnectionState(MountViewModel.ConnectionState.CONNECTED);
                    break;
                case BluetoothOTAService.DEVICE_DISCONNECTED:
                    Log.d(TAG, "handleMessage: Failed to connect msg from service");
                    // TODO: Toast.makeText(mActivity.get(), "Disconnected from " + msg.obj, Toast.LENGTH_SHORT).show();
                    vm.unbindAndChangeState(MountViewModel.ConnectionState.DISCONNECTED);
                    break;
                case BluetoothOTAService.DEVICE_FAILED_DISCONNECT:
//                        Toast.makeText(MainActivity.this, "Failed to disconnect from " + msg.obj, Toast.LENGTH_SHORT).show();
                    vm.unbindAndChangeState(MountViewModel.ConnectionState.UNKNOWN);
                    break;
                case BluetoothOTAService.DEVICE_FAILED:
                    // TODO:Toast.makeText(mActivity.get(), "Failed to connect to " + msg.obj, Toast.LENGTH_SHORT).show();
                    vm.unbindAndChangeState(MountViewModel.ConnectionState.FAILED);
                    break;
                case Mount.REFRESH_MOUNT_STATE:
                    break;
                case Mount.GET_POSITION:
//                    Log.d(TAG, "handleMessage: GET_POSITION");
                    if (o instanceof TelescopePosition) {
                        TelescopePosition pos = (TelescopePosition) o;
                        vm.currentPosition.postValue(pos);
//                        Log.d(TAG, "handleMessage: RA " + pos.RightAscension + " DEC " + pos.Declination + " EPOCH " + pos.epoch);
                    }
                    break;
                case Mount.GET_SITE_LATITUDE:
                case Mount.SET_SITE_LATITUDE:
                    vm.siteLatitude.postValue((float) o);
                    break;
                case Mount.SET_SITE_LONGITUDE:
                case Mount.GET_SITE_LONGITUDE:
                    vm.siteLongitude.postValue((float) o);
                    break;
                case Mount.START_MOVING:
                case Mount.STOP_SLEWING:
                    vm.slewingState.postValue(true);
                    break;
                case Mount.STOP_MOVING:
                case Mount.START_SLEWING:
                    vm.slewingState.postValue(false);
                    break;
                case Mount.SLEW:
                    if (msg.arg1 != 0) {
                        // TODO: handle
                        String r = (String) o;
                        if (r.equals("0")) {
                            // Telescope can complete slew
                        } else if (r.equals("1")) {
                            // Object is below horizon
                        } else if (r.equals("2")) {
                            // Object is below "higher limit"
                        }
                    }
                    break;
                case Mount.SYNC:
                    if (msg.arg1 == 1 && o instanceof TelescopePosition) {
                        TelescopePosition pos = (TelescopePosition) o;
                        vm.objectPosition.postValue(pos);
                    }
                    break;
                case Mount.GO_HOME:
                    break;
                case Mount.SET_HOME:
                    break;
                case Mount.GET_HA:
                    if (msg.arg1 == 1) {
                        String newHa = (String) o;
                        vm.ha.postValue(newHa);
                    }
                    break;
                case Mount.SET_TRACKING:
                    if (msg.arg1 == 1) {
                        if (MountMessages.toBool(msg.arg2)) {
                            vm.trackingState.postValue(TrackingState.TRACKING);
                        } else {
                            vm.trackingState.postValue(TrackingState.NOT_TRACKING);
                        }
                    }
                    break;
                case Mount.SET_LOCATION:
                    // Unfinished See @Mount
                    break;
                case Mount.PARK:
                    vm.trackingState.postValue(TrackingState.PARKED);
                    break;
                case Mount.UNPARK:
                    vm.trackingState.postValue(TrackingState.UNPARKED);
                    break;
                case Mount.GET_RA_STEPS_PER_DEG:
                    vm.raStepsPerDeg.postValue(msg.arg2);
                    break;
                case Mount.GET_DEC_STEPS_PER_DEG:
                    vm.decStepsPerDeg.postValue(msg.arg2);
                    break;
                case Mount.GET_SPEED_FACTOR:
                    vm.speedFactor.postValue((float) o);
                    break;
                case Mount.SYNC_POLARIS:
                    if (msg.arg1 == 1 && o instanceof TelescopePosition) {
                        TelescopePosition pos = (TelescopePosition) o;
                        // TODO:
                    }
                default:
                    break;
            }
        }

    }

    private void unbindAndChangeState(MountViewModel.ConnectionState state) {
        doUnbindService();
        setConnectionState(state);
    }

    private void setConnectionState(MountViewModel.ConnectionState state) {
        this.state.postValue(state);
    }

    /***
     *
     * @param type
     * @param activity - Used for callback on bluetooth results
     */
    @Override
    public void setup(ISearcherControl.EConnectionType type, Activity activity) {
        switch (type) {
            case BLUETOOTH:
                if (bluetoothSearch != null) {
//                    bluetoothSearch.disconnect();
                } else {
                    bluetoothSearch = new BluetoothSearch();
                }
                bluetoothSearch.setup(activity);
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registeredBluetooth = true;
                activity.registerReceiver(bluetoothSearch.receiver, filter);
                if (bluetoothDevices == null) {
                    bluetoothDevices = new MutableLiveData<>();
                }
                if (!bluetoothSearch.obsDevices.hasActiveObservers()) {
                    bluetoothSearch.obsDevices.observe((LifecycleOwner) activity, new Observer<Set<BluetoothDevice>>() {
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
                        startBluetoothService((BluetoothDevice) device.getDevice());
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
    public void disconnect(Activity activity) {
        switch (connectionType) {
            case BLUETOOTH:
                Intent intent = new Intent(getApplication(), BluetoothOTAService.class);
                getApplication().stopService(intent);
                doUnbindService();
                unregisterReceivers(activity);
                break;
            default:
                break;
        }
    }

    @Override
    public ISearcherControl.EConnectionType getType() {
        return connectionType;
    }

    @Override
    public void discover() {
        switch (connectionType) {
            case BLUETOOTH:
                Intent intent = new Intent(getApplication(), BluetoothOTAService.class);
                getApplication().stopService(intent);
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

    public void unregisterReceivers(Activity activity) {
        if (registeredBluetooth) {
            activity.unregisterReceiver(bluetoothSearch.receiver);
            registeredBluetooth = false;
        }
    }

    public void enableBluetoothUse() {
        bluetoothSearch.enableBluetoothUse();
    }

    public void disableBluetoothUse() {
        bluetoothSearch.disableBluetoothUse();
    }


    public LiveData<ConnectionState> getConnectionState() {
        return state;
    }

    public LiveData<TelescopePosition> getCurrentPosition() {
        return currentPosition;
    }

    public LiveData<TelescopePosition> getObjectPosition() {
        return objectPosition;
    }

    public LiveData<TrackingState> getTrackingState() {
        return trackingState;
    }

    public LiveData<Float> getSiteLatitude() {
        return siteLatitude;
    }

    public LiveData<Float> getSiteLongitude() {
        return siteLongitude;
    }

    public LiveData<Integer> getRaStepsPerDeg() {
        return raStepsPerDeg;
    }

    public LiveData<Integer> getDecStepsPerDeg() {
        return decStepsPerDeg;
    }

    public LiveData<Float> getSpeedFactor() {
        return speedFactor;
    }

    public LiveData<String> getHa() {
        return ha;
    }

    public LiveData<Boolean> getSlewingState() {
        return slewingState;
    }

}