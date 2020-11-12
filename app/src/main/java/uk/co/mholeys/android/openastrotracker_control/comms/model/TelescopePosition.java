package uk.co.mholeys.android.openastrotracker_control.comms.model;

public class TelescopePosition {

    public static TelescopePosition Invalid = new TelescopePosition(-1, 0, OTAEpoch.JNOW);
    public double RightAscension;
    public double Declination;
    public OTAEpoch epoch;


    public TelescopePosition(double rightAscension, double declination, OTAEpoch epoch) {
        RightAscension = rightAscension;
        Declination = declination;
        this.epoch = epoch;
    }

    public boolean IsValid() {
        return !this.equals(Invalid) &&
                RightAscension >= 0 && RightAscension < 24 &&
                Declination >= -90 && Declination <= 90;
    }

}
