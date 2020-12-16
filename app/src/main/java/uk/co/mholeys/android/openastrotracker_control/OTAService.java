package uk.co.mholeys.android.openastrotracker_control;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.IOException;

import uk.co.mholeys.android.openastrotracker_control.comms.model.TelescopePosition;
import uk.co.mholeys.android.openastrotracker_control.mount.Mount;

public class OTAService extends Service {

    public static final String CHANNEL_ID = "OTAConnectionServiceF";

    Handler handler;
    private BluetoothSocket client;
    private Mount mount;

    public OTAService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
        client = intent.getParcelableExtra("bluetooth-socket");
       try {
            mount = new Mount(client, handler);
//            mount.getSiteLatitude();
//            mount.getSiteLongitude();
//            // Set LST?
//            mount.getPosition();
//            mount.getRAStepsPerDegree();
//            mount.getDecStepsPerDegree();
//            mount.getSpeedFactor();
//            mount.getHA();
        } catch (IOException e) {
            e.printStackTrace();
        }
        startForeground(1, notification);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
//        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
        return null;
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


















//    case Mount.REFRESH_MOUNT_STATE:
//            break;
//                        case Mount.GET_POSITION:
//            Log.d(TAG, "handleMessage: GET_POSITION");
//                            if (o instanceof TelescopePosition) {
//        TelescopePosition pos = (TelescopePosition) o;
//        Log.d(TAG, "handleMessage: RA " + pos.RightAscension + " DEC " + pos.Declination + " EPOCH " + pos.epoch);
//    }
//                            break;
//                        case Mount.GET_SITE_LATITUDE:
//            break;
//                        case Mount.GET_SITE_LONGITUDE:
//            break;
//                        case Mount.SET_SITE_LATITUDE:
//            break;
//                        case Mount.SET_SITE_LONGITUDE:
//            break;
//                        case Mount.START_MOVING:
//            break;
//                        case Mount.SLEW:
//            break;
//                        case Mount.SYNC:
//            break;
//                        case Mount.GO_HOME:
//            break;
//                        case Mount.SET_HOME:
//            break;
//                        case Mount.GET_HA:
//            break;
//                        case Mount.SET_TRACKING:
//            break;
//                        case Mount.SET_LOCATION:
//            break;
//                        case Mount.PARK:
//            break;
//                        case Mount.UNPARK:
//            break;
//                        case Mount.STOP_SLEWING:
//            break;
//                        case Mount.START_SLEWING:
//            break;
//                        case Mount.GET_RA_STEPS_PER_DEG:
//            break;
//                        case Mount.GET_DEC_STEPS_PER_DEG:
//            break;
//                        case Mount.GET_SPEED_FACTOR:
//            break;

}