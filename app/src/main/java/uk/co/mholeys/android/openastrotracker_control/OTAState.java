package uk.co.mholeys.android.openastrotracker_control;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.lifecycle.MutableLiveData;

import uk.co.mholeys.android.openastrotracker_control.comms.model.TelescopePosition;

public class OTAState implements Parcelable  {

    public MutableLiveData<TelescopePosition> position = new MutableLiveData<>();

    protected OTAState(Parcel in) {
        // Has position stored
        if (in.readInt() == 1) {
            TelescopePosition pos = in.readParcelable(getClass().getClassLoader());
            this.position.postValue(pos);
        }
    }

    public static final Creator<OTAState> CREATOR = new Creator<OTAState>() {
        @Override
        public OTAState createFromParcel(Parcel in) {
            return new OTAState(in);
        }

        @Override
        public OTAState[] newArray(int size) {
            return new OTAState[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        TelescopePosition pos = position.getValue();
        if (pos == null) {
            dest.writeInt(1);
            dest.writeParcelable(pos, flags);
        } else {
            dest.writeInt(0);
        }
    }
}
