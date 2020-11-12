package uk.co.mholeys.android.openastrotracker_control.comms.handlers;

import uk.co.mholeys.android.openastrotracker_control.comms.model.MountState;
import uk.co.mholeys.android.openastrotracker_control.comms.model.TelescopePosition;

public interface ITelescopeCommandHandler {

    TelescopePosition getPosition();
    boolean slew(TelescopePosition position);
    boolean sync(TelescopePosition position);
    boolean goHome();
    boolean setTracking(boolean enabled);
    boolean setLocation(double lat, double lon, double altitudeInMeters, double lstInHours);
    boolean startMoving(String dir);
    boolean stopMoving(String dir);
    MountState getMountState();
    boolean refreshMountState();

}
