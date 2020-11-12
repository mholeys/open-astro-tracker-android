package uk.co.mholeys.android.openastrotracker_control.comms.handlers;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentMap;

import uk.co.mholeys.android.openastrotracker_control.comms.ICommunicationHandler;
import uk.co.mholeys.android.openastrotracker_control.comms.model.MountState;
import uk.co.mholeys.android.openastrotracker_control.comms.model.OTAEpoch;
import uk.co.mholeys.android.openastrotracker_control.comms.model.TelescopePosition;

public class OatmealTelescopeCommandHandler implements ITelescopeCommandHandler {

    public class HMS {
        double h, m, s;
    }

    private final ICommunicationHandler _commHandler;

    private int _moveState = 0;

    public MountState getMountState() {
        return new MountState();
    }

    public OatmealTelescopeCommandHandler(ICommunicationHandler commHandler) {
        _commHandler = commHandler;
    }

    public boolean connected() {
        return _commHandler.connected();
    }

    public synchronized boolean refreshMountState() {
        String[] _slewingStates = new String[]{
            "SlewToTarget", "FreeSlew", "ManualSlew"
        };
        List<String> slewingStates = Arrays.asList(_slewingStates);

        CommandResponse status = sendCommand(":GX#,#");
        if (!status.success) {
            return false;
        }

        String[] parts = status.data.split(",");
        MountState.setTracking(parts[1].charAt(2) == 'T');
        MountState.setSlewing(slewingStates.contains(parts[0]));
        MountState.setRightAscension(getCompactRA(parts[5]));
        MountState.setDeclination(getCompactDec(parts[6]));

        return status.success;

    }

    private double getCompactDec(String part) {
        int d = Integer.parseInt(part.substring(0, 3));
        int m = Integer.parseInt(part.substring(3, 2));
        int s = Integer.parseInt(part.substring(5, 2));

        return d + m / 60.0 + s / 3600.0;
    }

    private double getCompactRA(String part) {
        int h = Integer.parseInt(part.substring(0, 2));
        int m = Integer.parseInt(part.substring(2, 2));
        int s = Integer.parseInt(part.substring(4, 2));

        return h + m / 60.0 + s / 3600.0;
    }

    // Was Async
    public TelescopePosition getPosition() {
        CommandResponse ra = sendCommand(":GR#,#");
        CommandResponse dec = sendCommand(":GD#,#");

        if (ra.success &&dec.success) {
            double dRa, dDec;
            try {
                dRa = tryParseRA(ra.data);
                dDec = tryParseDec(dec.data);
                MountState.setRightAscension(dRa);
                MountState.setDeclination(dDec);
                return new TelescopePosition(dRa, dDec, OTAEpoch.JNOW);
            } catch (Exception e) { }
        }

        MountState.setRightAscension(0);
        MountState.setDeclination(0);
        return TelescopePosition.Invalid;
    }

    // Was Async
    public float getSiteLatitude() {
        CommandResponse lat = sendCommand(":Gt#,#");

        if (lat.success ) {
            try {
                return (float) tryParseDec(lat.data);
            } catch (Exception e) {}
        }

        return 0;
    }

    // Was Async
    public float getSiteLongitude() {
        CommandResponse lon = sendCommand(":Gg#,#");

        if (lon.success) {
            try {
                double dec = tryParseDec(lon.data);
                if (dec > 180) {
                    dec -= 360;
                }
                return (float) dec;
            } catch (Exception e) {}
        }

        return 0;
    }

    // Was Async
    public String setSiteLatitude(float latitude) {
        char sgn = latitude < 0 ? '-' : '+';
        int latInt = (int) Math.abs(latitude);
        int latMin = (int) ((Math.abs(latitude) - latInt) * 60.0f);
        CommandResponse lat = sendCommand(String.format(":St%c%02d*%02d#,n", sgn, latInt, latMin));
        if (lat.success) {
            return lat.data;
        }

        return "0";
    }

    // Was Async
    public String setSiteLongitude(float longitude) {
        longitude = longitude < 0 ? longitude + 360 : longitude;
        int lonInt = (int) longitude;
        int lonMin = (int) ((longitude - lonInt) * 60.0f);
        CommandResponse lon = sendCommand(String.format(":Sg%03d*%02d#,n", lonInt, lonMin));
        if (lon.success) {
            return lon.data;
        }

        return "0";
    }

    private void floatToHMS(double val, HMS hms) {
        hms.h = (int) Math.floor(val);
        val = (val - hms.h) * 60;
        hms.m = (int) Math.floor(val);
        val = (val - hms.m) * 60;
        hms.s = (int) Math.round(val);
    }

    private double tryParseRA(String ra) {
        String[] parts = ra.split(":");
        return Integer.parseInt(parts[0]) + Integer.parseInt(parts[1]) / 60.0 + Integer.parseInt(parts[2]) / 3600.0;
    }

    private double tryParseDec(String dec) {
        String[] parts = dec.split("\\*|\\\\");
        double dDec = Integer.parseInt(parts[0]) + Integer.parseInt(parts[1]) / 60.0;
        if (parts.length > 2) {
            dDec += Integer.parseInt(parts[2]) / 3600.0;
        }

        return dDec;
    }

    // Was Async
    public boolean startMoving(String dir) {
        CommandResponse status = sendCommand(String.format(":M%s#", dir));
        MountState.setSlewing(true);
        ++_moveState;
        return status.success;
    }

    // Was Async
    public boolean stopMoving(String dir) {
        CommandResponse status = sendCommand(String.format(":Q%s#", dir));
        --_moveState;
        if (_moveState <= 0) {
            _moveState = 0;
            MountState.setSlewing(false);
        }
        return status.success;
    }

    // Was Async
    public boolean slew(TelescopePosition position) {
        int deg, hour, min, sec;
        HMS hms = new HMS();
        floatToHMS(Math.abs(position.Declination), hms);
        deg = (int) hms.h;
        min = (int) hms.m;
        sec = (int) hms.s;
        char sign = position.Declination < 0 ? '-' : '+';
        CommandResponse result = sendCommand(String.format(":Sd%c%02d*%02d:%02d#,n", sign, deg, min, sec));
        if (!result.success || result.data != "1") return false;
        floatToHMS(Math.abs(position.RightAscension), hms);
        hour = (int) hms.h;
        min = (int) hms.m;
        sec = (int) hms.s;
        result = sendCommand(String.format(":Sr%02d:%02d:%02d#,n", hour, min, sec));
        if (!result.success || result.data != "1") return false;
        result = sendCommand(String.format(":MS#,n"));
        return result.success;
    }

    // Was Async
    public boolean sync(TelescopePosition position) {
        int deg, hour, min, sec;
        HMS hms = new HMS();
        floatToHMS(Math.abs(position.Declination), hms);
        deg = (int) hms.h;
        min = (int) hms.m;
        sec = (int) hms.s;
        char sign = position.Declination < 0 ? '-' : '+';
        CommandResponse result = sendCommand(String.format(":Sd%c%02d*%02d:%02d#,n", sign, deg, min, sec));
        if (!result.success || result.data != "1") return false;
        floatToHMS(Math.abs(position.RightAscension), hms);
        hour = (int) hms.h;
        min = (int) hms.m;
        sec = (int) hms.s;
        result = sendCommand(String.format(":Sr%02d:%02d:%02d#,n", hour, min, sec));
        if (!result.success || result.data != "1") return false;
        result = sendCommand(":CM#,#");
        return result.success;
    }

    // Was Async
    public boolean goHome() {
        CommandResponse status = sendCommand(":hP#");
        return status.success;
    }

    // Was Async
    public boolean setHome() {
        CommandResponse status = sendCommand(":hP#");
        return status.success;
    }

    // Was Async
    public boolean setTracking(boolean enabled) {
        int b = enabled ? 1 : 0;
        CommandResponse status = sendCommand(":MT%d#,n");
        if (status.success) {
            MountState.setTracking(enabled);
        }
        return status.success;
    }

    // Was Async
    public boolean setLocation(double lat, double lon, double altitudeInMeters, double lstInHours) {

        // Longitude

        if (lon < 0) {
            lon = 360 + lon;
        }
        int lonFront = (int) lon;
        int lonBack = (int) ((lon - lonFront) * 60);
        String lonCmd = String.format(":Sg%03d*%02d#,n", lonFront, lonBack);
        CommandResponse status = sendCommand(lonCmd);
        if (!status.success) return false;


        // Latitude
        char latSign = lat > 0 ? '+' : '-';
        double absLat = Math.abs(lat);
        int latFront = (int) absLat;
        int latBack = (int) ((absLat - latFront) * 60.0);
        String latCmd = String.format(":St%c%02d*%02d#,n", latSign, latFront, latBack);
        status = sendCommand(latCmd);
        if (!status.success) return false;


        // GMT Offset
        Calendar c = Calendar.getInstance();
        TimeZone tz = c.getTimeZone();
        int hours = (tz.getDSTSavings() + tz.getRawOffset()) / 3600000;
        char offsetSign = hours > 0 ? '+' : '-';
        int offset = Math.abs(hours);
        status = sendCommand(String.format(":SG%c%02d#,n", offsetSign, offset));
        if (!status.success) return false;


        // Local Time and Date
        int h = c.get(Calendar.HOUR_OF_DAY);
        int m = c.get(Calendar.MINUTE);
        int s = c.get(Calendar.SECOND);
        int mm = c.get(Calendar.MONTH) + 1;
        int dd = c.get(Calendar.DAY_OF_MONTH);
        int yy = c.get(Calendar.YEAR);
        status = sendCommand(String.format(":SL:%02d:%02d:%02d#,n", h, m, s));
        if (!status.success) return false;
        status = sendCommand(String.format(":SC:%02d/%02d/%02d#,#"));
        return status.success;
    }

    // Was Async
    public CommandResponse sendCommand(String cmd) {
        if (!cmd.startsWith(":")) {
            cmd = ":"+cmd;
        }

        String substring = cmd.substring(0, cmd.length() - 2);
        if (cmd.endsWith("#,#")) {
            // Was Async
            return _commHandler.sendCommand(substring);
        } else if (cmd.endsWith("#,n")) {
            // Was Async
            return _commHandler.sendCommandConfirm(substring);
        }

        if (!cmd.endsWith("#")) {
            cmd += "#";
        }
        return _commHandler.sendBlind(cmd);
    }

}
