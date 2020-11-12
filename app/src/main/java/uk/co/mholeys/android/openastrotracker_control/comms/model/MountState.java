package uk.co.mholeys.android.openastrotracker_control.comms.model;

import java.util.ArrayList;

public class MountState {

    static ArrayList<MountChangeListener> listeners = new ArrayList<MountChangeListener>();

    private static  boolean tracking;
    public static boolean isTracking() { return tracking; }
    public static void setTracking(boolean isTracking) {
        tracking = isTracking;
        for (MountChangeListener listener : listeners) {
            listener.isTrackingChanged();
        }
    }

    private static boolean slewing;
    public static boolean isSlewing() { return slewing; }
    public static void setSlewing(boolean isSlewing) {
        slewing = isSlewing;
        for (MountChangeListener listener : listeners) {
            listener.isSlewingChanged();
        }
    }

    private static boolean slewingRA;
    public static boolean isSlewingRA() { return slewingRA; }
    public static void setSlewingRA(boolean isSlewingRA) {
        slewingRA = isSlewingRA;
        for (MountChangeListener listener : listeners) {
            listener.isSlewingRAChanged();
        }
    }

    private static boolean slewingDec;
    public static boolean isSlewingDec() { return slewingDec; }
    public static void setSlewingDec(boolean isSlewingDec) {
        slewingDec = isSlewingDec;
        for (MountChangeListener listener : listeners) {
            listener.isSlewingDecChanged();
        }
    }

    private static double rightAscension;
    public static double getRightAscension() { return rightAscension; }
    public static void setRightAscension(double rightAscension) {
        MountState.rightAscension = rightAscension;
        for (MountChangeListener listener : listeners) {
            listener.rightAscensionChanged();
        }
    }

    private static double declination;
    public static double getDeclination() { return  declination; }
    public static void setDeclination(double declination) {
        MountState.declination = declination;
        for (MountChangeListener listener : listeners) {
            listener.declinationChanged();
        }
    }



}
