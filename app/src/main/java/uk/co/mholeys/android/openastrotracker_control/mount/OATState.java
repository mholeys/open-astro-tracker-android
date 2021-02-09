package uk.co.mholeys.android.openastrotracker_control.mount;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.lifecycle.MutableLiveData;

import uk.co.mholeys.android.openastrotracker_control.comms.model.TelescopePosition;

public class OATState implements Parcelable  {

    private static OATState instance;
    public static OATState getInstance() {
        if (instance == null) {
            instance = new OATState();
        }
        return instance;
    }

    public MutableLiveData<TelescopePosition> position = new MutableLiveData<>();
    public MutableLiveData<Boolean> tracking = new MutableLiveData<>();
    public MutableLiveData<Boolean> slewing = new MutableLiveData<>();
    public MutableLiveData<Boolean> slewingRA = new MutableLiveData<>();
    public MutableLiveData<Boolean> slewingDEC = new MutableLiveData<>();

    public OATState() {

    }

    protected OATState(Parcel in) {
        instance = this;
        // Has position stored
        if (in.readInt() == 1) {
            TelescopePosition pos = in.readParcelable(getClass().getClassLoader());
            this.position.postValue(pos);
        }
        this.tracking.postValue(in.readInt() == 1);
        instance = this;
    }

    public static final Creator<OATState> CREATOR = new Creator<OATState>() {
        @Override
        public OATState createFromParcel(Parcel in) {
            return new OATState(in);
        }

        @Override
        public OATState[] newArray(int size) {
            return new OATState[size];
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
        dest.writeInt(tracking.getValue() ? 1 : 0);
    }
}
