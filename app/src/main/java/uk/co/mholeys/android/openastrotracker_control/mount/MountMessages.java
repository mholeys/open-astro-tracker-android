package uk.co.mholeys.android.openastrotracker_control.mount;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import uk.co.mholeys.android.openastrotracker_control.comms.model.TelescopePosition;

public class MountMessages {

    public static void refreshMountState(Messenger target) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.REFRESH_MOUNT_STATE,0,0);
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void getPosition(Messenger target) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.GET_POSITION,0,0);
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void getSiteLatitude(Messenger target) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.GET_SITE_LATITUDE,0,0);
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void getSiteLongitude(Messenger target) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.GET_SITE_LONGITUDE,0,0);
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void setSiteLatitude(Messenger target) {
        // TODO: Value
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.SET_SITE_LATITUDE,0,0);
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void setSiteLongitude(Messenger target) {
        // TODO: value
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.SET_SITE_LONGITUDE,0,0);
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void startMoving(Messenger target) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.START_MOVING,0,0);
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void stopMoving(Messenger target) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.STOP_MOVING,0,0);
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void slew(Messenger target, TelescopePosition targetPos) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.SLEW,0,0);
        msg.obj = targetPos;
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void sync(Messenger target, TelescopePosition targetPos) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.SYNC,0,0);
        msg.obj = targetPos;
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void syncPolaris(Messenger target, TelescopePosition targetPos) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.SYNC_POLARIS,0,0);
        msg.obj = targetPos;
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void goHome(Messenger target) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.GO_HOME,0,0);
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void setHome(Messenger target) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.SET_HOME,0,0);
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void getHA(Messenger target) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.GET_HA,0,0);
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void setTracking(Messenger target, boolean tracking) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.SET_TRACKING, fromBool(tracking),0);
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void setLocation(Messenger target) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.SET_LOCATION,0,0);
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void park(Messenger target) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.PARK,0,0);
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void unpark(Messenger target) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.UNPARK,0,0);
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void stopSlewing(Messenger target) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.STOP_SLEWING,0,0);
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void startSlewing(Messenger target) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.START_SLEWING,0,0);
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void getRaStepsPerDeg(Messenger target) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.GET_RA_STEPS_PER_DEG,0,0);
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void getDecStepsPerDeg(Messenger target) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.GET_DEC_STEPS_PER_DEG,0,0);
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }
    public static void getSpeedFactor(Messenger target) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.GET_SPEED_FACTOR,0,0);
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }

    public static void moveSlightly(Messenger target, char direction, int duration) {
        Bundle b = new Bundle();
        Message msg = Message.obtain(null, Mount.MOVE_SLIGHTLY, direction, duration);
        try {
            target.send(msg);
        } catch (RemoteException e) {
        }
    }

    private static int fromBool(boolean v) {
        return v ? 1 : 0;
    }

    public static boolean toBool(int v) {
        return v == 1;
    }

}
