package uk.co.mholeys.android.openastrotracker_control.comms.model;

public abstract class IReadOnlyMountState {

    private boolean tracking;
    public boolean isTracking() { return tracking; }

    private boolean slewing;
    public boolean isSlewing() { return slewing; }

    private boolean slewingRA;
    public boolean isSlewingRA() { return slewingRA; }

    private boolean slewingDec;
    public boolean isSlewingDec() { return slewingDec; }

    private double rightAscension;
    public double getRightAscension() { return rightAscension; }

    private double declination;
    public double getDeclination() { return  declination; }

}
