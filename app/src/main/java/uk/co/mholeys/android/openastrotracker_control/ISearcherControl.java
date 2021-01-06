package uk.co.mholeys.android.openastrotracker_control;

import android.app.Activity;
import android.database.Observable;
import android.os.Handler;
import android.os.Messenger;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;

import java.util.Set;

import uk.co.mholeys.android.openastrotracker_control.ui.connection.IDevice;

public interface ISearcherControl {

    enum EConnectionType { WIFI, BLUETOOTH, USB, UNKNOWN;}

    public void setup(EConnectionType type, Activity activity);
    public void connect(IDevice d);
    public LiveData<Set<IDevice>> getDevices();
    public void disconnect(Activity activity);
    public EConnectionType getType();
    public void discover();

    public Messenger getMessenger();
    public boolean isBound();

}
