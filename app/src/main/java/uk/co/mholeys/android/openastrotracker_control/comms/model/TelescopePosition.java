package uk.co.mholeys.android.openastrotracker_control.comms.model;

import android.os.Parcel;
import android.os.Parcelable;

public class TelescopePosition implements Parcelable {

    public static TelescopePosition Invalid = new TelescopePosition(-1, 0, OATEpoch.JNOW);
    public double RightAscension;
    public double Declination;
    public OATEpoch epoch;


    public TelescopePosition(double rightAscension, double declination, OATEpoch epoch) {
        RightAscension = rightAscension;
        Declination = declination;
        this.epoch = epoch;
    }

    protected TelescopePosition(Parcel in) {
        RightAscension = in.readDouble();
        Declination = in.readDouble();
        epoch = OATEpoch.valueOf(in.readString());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(RightAscension);
        dest.writeDouble(Declination);
        dest.writeString(epoch.toString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TelescopePosition> CREATOR = new Creator<TelescopePosition>() {
        @Override
        public TelescopePosition createFromParcel(Parcel in) {
            return new TelescopePosition(in);
        }

        @Override
        public TelescopePosition[] newArray(int size) {
            return new TelescopePosition[size];
        }
    };

    public boolean IsValid() {
        return !this.equals(Invalid) &&
                RightAscension >= 0 && RightAscension < 24 &&
                Declination >= -90 && Declination <= 90;
    }

}
