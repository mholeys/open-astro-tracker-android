package uk.co.mholeys.android.openastrotracker_control;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class OTAService extends Service {
    public OTAService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}