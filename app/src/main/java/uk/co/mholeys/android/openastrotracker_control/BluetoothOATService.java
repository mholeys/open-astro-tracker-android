package uk.co.mholeys.android.openastrotracker_control;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import uk.co.mholeys.android.openastrotracker_control.comms.model.TelescopePosition;
import uk.co.mholeys.android.openastrotracker_control.mount.Mount;
import uk.co.mholeys.android.openastrotracker_control.mount.MountMessages;

public class BluetoothOATService extends Service {

    public static final String CHANNEL_ID = "OATConnectionServiceF";
    private static final String TAG = "OATServBl";

    public static final int DEVICE_CONNECTED = 0x222001;
    public static final int DEVICE_DISCONNECTED = 0x222002;
    public static final int DEVICE_FAILED = 0x222003;
    public static final int DEVICE_FAILED_DISCONNECT = 0x222004;
    public static final int DEVICE_CONNECTING = 0x222005;

    private BluetoothAdapter adapter;

    Handler mountHandler;
    private BluetoothSocket client;
    private Mount mount;
    private boolean bluetooth;
    private BluetoothDevice device;
    private ConnectThread connectThread;
    public Messenger mMessenger = new Messenger(new MessageHandler());
    private Messenger clientMessenger;
    private StatusRunnable statusRunnable;
    private PositionRunnable positionRunnable;
    private ScheduledFuture<?> statusRunnableFuture;
    private ScheduledFuture<?> positionRunnableFuture;

    @Override
    public int onStartCommand(Intent intent, final int flags, int startId) {
//        return super.onStartCommand(intent, flags, startId);
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("OpenAstroTracker Connected")
                .setContentText("")
                .setSmallIcon(R.drawable.ic_home_black_24dp)
                .setContentIntent(pendingIntent)
                .build();

        mountHandler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                Message toClient = Message.obtain(msg);
                toClient.setTarget(null);
                try {
                    clientMessenger.send(toClient);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
//                Log.d(TAG, "mountHandler: " + msg);
//                Object o = msg.obj;
//                switch (msg.what) {
//                    case Mount.REFRESH_MOUNT_STATE:
//                        break;
//                    case Mount.GET_POSITION:
//                        Log.d(TAG, "handleMessage: GET_POSITION");
//                        if (o instanceof TelescopePosition) {
//                            TelescopePosition pos = (TelescopePosition) o;
//                            Log.d(TAG, "handleMessage: RA " + pos.RightAscension + " DEC " + pos.Declination + " EPOCH " + pos.epoch);
//                        }
//                        break;
//                    case Mount.GET_SITE_LATITUDE:
//                        break;
//                    case Mount.GET_SITE_LONGITUDE:
//                        break;
//                    case Mount.SET_SITE_LATITUDE:
//                        break;
//                    case Mount.SET_SITE_LONGITUDE:
//                        break;
//                    case Mount.START_MOVING:
//                        break;
//                    case Mount.SLEW:
//                        break;
//                    case Mount.SYNC:
//                        break;
//                    case Mount.GO_HOME:
//                        break;
//                    case Mount.SET_HOME:
//                        break;
//                    case Mount.GET_HA:
//                        break;
//                    case Mount.SET_TRACKING:
//                        break;
//                    case Mount.SET_LOCATION:
//                        break;
//                    case Mount.PARK:
//                        break;
//                    case Mount.UNPARK:
//                        break;
//                    case Mount.STOP_SLEWING:
//                        break;
//                    case Mount.START_SLEWING:
//                        break;
//                    case Mount.GET_RA_STEPS_PER_DEG:
//                        break;
//                    case Mount.GET_DEC_STEPS_PER_DEG:
//                        break;
//                    case Mount.GET_SPEED_FACTOR:
//                        break;
//                    default:
//                        break;
//                }

            }
        };

        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            // Handle no bluetooth on device
            bluetooth = false;
            stopSelf();
            return START_STICKY;
        }
        bluetooth = true;

        if (!adapter.isEnabled()) {
            // Bluetooth is disabled, so prompt/request them to turn it on
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // TODO: handle what we want to do if bluetooth is turned off again?
//            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            bluetooth = false;
        }
        if (!bluetooth) {
            stopSelf();
            return START_STICKY;
        }

        device = intent.getParcelableExtra("bluetooth-device");

        if (connectThread != null) {
            connectThread.cancel();
        }
        startForeground(1, notification);

        Log.d(TAG, "connectClient: Connecting");
        UUID id = UUID.fromString(getString(R.string.bluetooth_sdp_uuid));
        connectThread = new ConnectThread(device, id);
        connectThread.start();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "OTA Communication Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public void onConnected(BluetoothSocket client) {
        if (clientMessenger != null) {
            Log.d(TAG, "");
            Message writtenMsg = Message.obtain(null, DEVICE_CONNECTED, -0, -1, device.getName());
            try {
                clientMessenger.send(writtenMsg);
            } catch (RemoteException e) {
                Log.e(TAG, "run: Failed to send message to client");
                // Failed to send message to activity, its probably dead
                e.printStackTrace();
            }
        }

        try {
            mount = new Mount(client, mountHandler);

            positionRunnable = new PositionRunnable(mount);
            statusRunnable = new StatusRunnable(mount);
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            positionRunnableFuture = executor.scheduleAtFixedRate(positionRunnable, 0, 100, TimeUnit.MILLISECONDS);
            statusRunnableFuture = executor.scheduleAtFixedRate(statusRunnable, 0, 200, TimeUnit.MILLISECONDS);

            // TODO: to communicate from now on, this needs to be a BoundService
            // TODO: See https://developer.android.com/guide/components/bound-services
            mount.getSiteLatitude();
            mount.getSiteLongitude();
//            // Set LST?
            mount.getTrackingState();
            mount.getPosition();
            mount.getRAStepsPerDegree();
            mount.getDecStepsPerDegree();
            mount.getSpeedFactor();
            mount.getHA();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Thread keeping the connection refreshed and the position/state up to date
    private class PositionRunnable implements Runnable {
        private static final String TAG = "BOATServ.PosThr";

        private Mount mount;

        public PositionRunnable(Mount mount) {
            this.mount = mount;
        }

        public void run() {
            mount.getPosition();
        }
    }
    private class StatusRunnable implements Runnable {
        private static final String TAG = "BOATServ.StatusThr";

        private Mount mount;

        public StatusRunnable(Mount mount) {
            this.mount = mount;
        }

        public void run() {
            mount.getTrackingState();
            mount.getSlewingState();
        }
    }

    private class ConnectThread extends Thread {
        private static final String TAG = "BOATServ.ConThr";

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
                if (clientMessenger != null) {
                    Log.d(TAG, "");
                    Message writtenMsg = Message.obtain(null, DEVICE_FAILED, -0, -1, device.getName());
                    try {
                        clientMessenger.send(writtenMsg);
                    } catch (RemoteException e) {
                        Log.e(TAG, "run: Failed to send message to client");
                        // Failed to send message to activity, its probably dead
                        e.printStackTrace();
                    }
                }
                stopSelf();
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
                if (clientMessenger != null) {
                    Log.d(TAG, "");
                    Message writtenMsg = Message.obtain(null, DEVICE_DISCONNECTED, -0, -1, device.getName());
                    try {
                        clientMessenger.send(writtenMsg);
                    } catch (RemoteException e) {
                        Log.e(TAG, "run: Failed to send message to client");
                        // Failed to send message to activity, its probably dead
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
                if (clientMessenger != null) {
                    Log.d(TAG, "");
                    Message writtenMsg = Message.obtain(null, DEVICE_FAILED_DISCONNECT, -0, -1, device.getName());
                    try {
                        clientMessenger.send(writtenMsg);
                    } catch (RemoteException e1) {
                        Log.e(TAG, "run: Failed to send message to client");
                        // Failed to send message to activity, its probably dead
                        e1.printStackTrace();
                    }
                }
            }
            stopSelf();
        }
    }

    public class MessageHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
//            BluetoothOATService.this.clientMessenger = msg.replyTo;
            Log.d(TAG, "ServiceHandler: " + msg);
            Object o = msg.obj;
            Bundle b = msg.getData();
            TelescopePosition pos = null;
            switch (msg.what) {
                case MountViewModel.SETUP_CLIENT_MESSENGER:
                    clientMessenger = msg.replyTo;
                    Log.d(TAG, "handleMessage: Setup client messenger");
                    break;
                case Mount.REFRESH_MOUNT_STATE:
                    if (mount == null) {return;}
                    mount.refreshMountState();
                    break;
                case Mount.GET_POSITION:
                    if (mount == null) {return;}
                    mount.getPosition();
                    break;
                case Mount.GET_SITE_LATITUDE:
                    if (mount == null) {return;}
                    mount.getSiteLatitude();
                    break;
                case Mount.GET_SITE_LONGITUDE:
                    if (mount == null) {return;}
                    mount.getSiteLongitude();
                    break;
                case Mount.SET_SITE_LATITUDE:
                    if (mount == null) {return;}
                    mount.setSiteLatitude(b.getFloat(MountMessages.LATITUDE));
                    break;
                case Mount.SET_SITE_LONGITUDE:
                    if (mount == null) {return;}
                    mount.setSiteLongitude(b.getFloat(MountMessages.LONGITUDE));
                    break;
                case Mount.START_MOVING:
                    if (mount == null) {return;}
                    Log.e(TAG, "handleMessage: START_MOVING not implemented");
                    //mount.startMoving();
                    break;
                case Mount.STOP_MOVING:
                    if (mount == null) {return;}
                    Log.e(TAG, "handleMessage: STOP_MOVING not implemented");
                    //mount.stopMoving();
                    break;
                case Mount.SLEW:
                    if (mount == null) {return;}
                    pos = (TelescopePosition) o;
                    mount.slew(pos);
                    break;
                case Mount.SYNC:
                    if (mount == null) {return;}
                    pos = (TelescopePosition) o;
                    mount.sync(pos);
                    break;
                case Mount.SYNC_POLARIS:
                    if (mount == null) {return;}
                    pos = (TelescopePosition) o;
                    mount.syncPolaris(pos);
                    break;
                case Mount.GO_HOME:
                    if (mount == null) {return;}
                    mount.goHome();
                    break;
                case Mount.SET_HOME:
                    if (mount == null) {return;}
                    mount.setHome();
                    break;
                case Mount.GET_HA:
                    if (mount == null) {return;}
                    mount.getHA();
                    break;
                case Mount.SET_TRACKING:
                    if (mount == null) {return;}
                    mount.setTracking(MountMessages.toBool(msg.arg1));
                    break;
                case Mount.SET_LOCATION:
                    if (mount == null) {return;}
                    mount.setLocation(
                            b.getFloat(MountMessages.LATITUDE, 100f),
                            b.getFloat(MountMessages.LONGITUDE, 0f),
                            b.getInt(MountMessages.ALTITUDE, 100),
                            b.getInt(MountMessages.LST, 0)
                    );
                    break;
                case Mount.PARK:
                    if (mount == null) {return;}
                    mount.park();
                    break;
                case Mount.UNPARK:
                    if (mount == null) {return;}
                    mount.unpark();
                    break;
                case Mount.STOP_SLEWING:
                    if (mount == null) {return;}
                    Log.e(TAG, "handleMessage: STOP_SLEWING not implemented");
                    //mount.stopSlewing();
                    break;
                case Mount.START_SLEWING:
                    if (mount == null) {return;}
                    Log.e(TAG, "handleMessage: START_SLEWING not implemented");
                    //mount.toggleSlewing();
                    break;
                case Mount.GET_RA_STEPS_PER_DEG:
                    if (mount == null) {return;}
                    mount.getRAStepsPerDegree();
                    break;
                case Mount.GET_DEC_STEPS_PER_DEG:
                    if (mount == null) {return;}
                    mount.getDecStepsPerDegree();
                    break;
                case Mount.GET_SPEED_FACTOR:
                    if (mount == null) {return;}
                    mount.getSpeedFactor();
                    break;
                case Mount.MOVE_SLIGHTLY:
                    if (mount == null) {return;}
                    mount.moveSlightly((char) msg.arg1, msg.arg2);
                case Mount.SET_RA_STEPS_PER_DEG:
                    if (mount == null) {return;}
                    mount.setRaStepsPerDeg(msg.arg1);
                    break;
                case Mount.SET_DEC_STEPS_PER_DEG:
                    if (mount == null) {return;}
                    mount.setDecStepsPerDeg(msg.arg1);
                    break;
                case Mount.SET_SPEED_FACTOR:
                    if (mount == null) {return;}
                    mount.setSpeedFactor((float) msg.obj);
                    break;
                case Mount.GET_TRACKING_STATE:
                    if (mount == null) {return;}
                    mount.getTrackingState();
                    break;
                case Mount.GET_SLEWING_STATE:
                    if (mount == null) {return;}
                    mount.getSlewingState();
                    break;
                case Mount.GET_MOUNT_VERSION:
                    if (mount == null) {return;}
                    mount.getMountVersion();
                    break;
                default:
                    break;
            }
            Log.d(TAG, "handleMessage: Got message " + msg.what);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Service ended");
        // Stop comms
        if (mount != null) {
            if (statusRunnableFuture != null) {
                // Cancel gracefully as to not cause reconnection issues
                statusRunnableFuture.cancel(false);
            }
            if (positionRunnableFuture != null) {
                // Cancel gracefully as to not cause reconnection issues
                positionRunnableFuture.cancel(false);
            }

            mount.oat.end();
        }
    }
}