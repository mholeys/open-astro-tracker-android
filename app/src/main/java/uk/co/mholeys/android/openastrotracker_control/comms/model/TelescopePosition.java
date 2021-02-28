package uk.co.mholeys.android.openastrotracker_control.comms.model;

import android.os.Parcel;
import android.os.Parcelable;

import uk.co.mholeys.android.openastrotracker_control.comms.model.OATEpoch;
import uk.co.mholeys.android.openastrotracker_control.mount.Mount;

public class TelescopePosition implements Parcelable {

    public int raH, raM, raS, decH, decM, decS;
    public OATEpoch epoch;

    public TelescopePosition(Mount.HMS ra, Mount.HMS dec, OATEpoch epoch) {
        raH = ra.h;
        raM = ra.m;
        raS = ra.s;
        decH = dec.h;
        decM = dec.m;
        decS = dec.s;
        this.epoch = epoch;
    }

    protected TelescopePosition(Parcel in) {
        raH = in.readInt();
        raM = in.readInt();
        raS = in.readInt();
        decH = in.readInt();
        decM = in.readInt();
        decS = in.readInt();
        epoch = OATEpoch.valueOf(in.readString());
    }

    public Mount.HMS RightAscension() {
        return new Mount.HMS(raH, raM, raS);
    }
    public Mount.HMS Declination() {
        return new Mount.HMS(decH, decM, decS);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(raH);
        dest.writeInt(raM);
        dest.writeInt(raS);
        dest.writeInt(decH);
        dest.writeInt(decM);
        dest.writeInt(decS);
        String e  = epoch.toString();
        dest.writeString(e);
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

}
