package uk.co.mholeys.android.openastrotracker_control.mount;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import uk.co.mholeys.android.openastrotracker_control.comms.model.OATEpoch;
import uk.co.mholeys.android.openastrotracker_control.comms.model.TelescopePosition;

public class Mount {

    private static final String TAG = "OAT_MOUNT";
    public OATComms oat;
    private Handler handler;

    public Mount(Socket socket, Handler handler) throws IOException {
        this(socket.getInputStream(), socket.getOutputStream(), handler);
    }

    public Mount(BluetoothSocket socket, Handler handler) throws IOException {
        this(socket.getInputStream(), socket.getOutputStream(), handler);
    }

    public Mount(InputStream in, OutputStream out, Handler handler) {
        oat = new OATComms(in, out);
        this.handler = handler;
        oat.start();
    }

    // TODO: finish parsing data in callbacks!

    public static final int REFRESH_MOUNT_STATE    = 1;
    public static final int GET_POSITION           = 2;
    public static final int GET_SITE_LATITUDE      = 3;
    public static final int GET_SITE_LONGITUDE     = 4;
    public static final int SET_SITE_LATITUDE      = 5;
    public static final int SET_SITE_LONGITUDE     = 6;
    public static final int START_MOVING           = 7;
    public static final int STOP_MOVING            = 8;
    public static final int SLEW                   = 9;
    public static final int SYNC                   = 10;
    public static final int GO_HOME                = 11;
    public static final int SET_HOME               = 12;
    public static final int GET_HA                 = 13;
    public static final int SET_TRACKING           = 14;
    public static final int SET_LOCATION           = 15;
    public static final int PARK                   = 16;
    public static final int UNPARK                 = 17;
    public static final int STOP_SLEWING           = 18;
    public static final int START_SLEWING          = 19;
    public static final int GET_RA_STEPS_PER_DEG   = 20;
    public static final int GET_DEC_STEPS_PER_DEG  = 21;
    public static final int GET_SPEED_FACTOR       = 22;

    public static final int SYNC_POLARIS           = 23;
    public static final int MOVE_SLIGHTLY          = 24;
    public static final int SET_RA_STEPS_PER_DEG   = 25;
    public static final int SET_DEC_STEPS_PER_DEG  = 26;
    public static final int SET_SPEED_FACTOR       = 27;
    public static final int GET_TRACKING_STATE     = 28;
    public static final int GET_SLEWING_STATE      = 29;
    public static final int GET_MOUNT_VERSION      = 30;

//    private static final Handler bluetoothHandler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            StringBuilder result = new StringBuilder();
////            switch (msg.what) {
////
////            }
//        }
//    };


    // TODO: incomplete
    public void refreshMountState() {
        // Full response
        oat.sendCommand(":GX#", new OATComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                String[] _slewingStates = new String[]{
                        "SlewToTarget", "FreeSlew", "ManualSlew"
                };
                List<String> slewingStates = Arrays.asList(_slewingStates);
                // TODO:
//                String[] parts = status.data.split(",");
//                MountState.setTracking(parts[1].charAt(2) == 'T');
//                MountState.setSlewing(slewingStates.contains(parts[0]));
//                MountState.setRightAscension(getCompactRA(parts[5]));
//                MountState.setDeclination(getCompactDec(parts[6]));
            }
        });
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

    public void close() {
        oat.end();
    }

    static class RACommandResponse implements OATComms.CommandResponse {
        public String newRA = null;
        public String newRA() {
            return newRA;
        }
        @Override
        public void result(boolean success, String result) {
            if (!success) {
                Log.e(TAG, "getPositionRA: failed");
            } else {
//                String[] parts = result.split(":");
                newRA = result;// = Integer.parseInt(parts[0]) + Integer.parseInt(parts[1]) / 60.0 + Integer.parseInt(parts[2]) / 3600.0;
            }
        }
    }
    public void getPosition() {
        // GET RA
        // Full response
        final RACommandResponse raCallback = new RACommandResponse();
        oat.sendCommand(":GR#", raCallback);
        OATComms.CommandResponse decCallback = new OATComms.CommandResponse() {
            @Override
            public void result(boolean success, String newDEC) {
                String newRA = raCallback.newRA();
                if (!success || (newRA == null) || (newDEC == null)) {
                    Log.e(TAG, "getPositionDEC: failed");
                } else {
                    // Parse RA/DEC
                        double dRa, dDec;
                        try {
                            dRa = tryParseRA(newRA);
                            dDec = tryParseDec(newDEC);
                            TelescopePosition pos = new TelescopePosition(dRa, dDec, OATEpoch.JNOW);
                            Message writtenMsg = handler.obtainMessage(Mount.GET_POSITION, -0, -1, pos);
                            writtenMsg.sendToTarget();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
            }
        };
        // Full response
        oat.sendCommand(":GD#", decCallback);
    }

    public void getSiteLatitude() {
        // Full response
        oat.sendCommand(":Gt#", new OATComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                if (success) {
                    float lat = (float) tryParseDec(result);
                    Message writtenMsg = handler.obtainMessage(Mount.GET_SITE_LATITUDE, success ? 1 : 0, 0, lat);
                    writtenMsg.sendToTarget();
                } else {
                    // 0
                    Log.e(TAG, "getSiteLat: failed");
                }
            }
        });
    }

    public void getSiteLongitude() {
        // Full response
        oat.sendCommand(":Gg#", new OATComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                if (success) {
                    double lon = tryParseDec(result);
                    if (lon > 180) {
                        lon -= 360;
                    }
                    Message writtenMsg = handler.obtainMessage(Mount.GET_SITE_LONGITUDE, success ? 1 : 0, 0, (float) lon);
                    writtenMsg.sendToTarget();
                } else {
                    // 0
                    Log.e(TAG, "getSiteLon: failed");
                }
            }
        });
    }

    public void setSiteLatitude(final float latitude) {
        char sgn = latitude < 0 ? '-' : '+';
        final int latInt = (int) Math.abs(latitude);
        int latMin = (int) ((Math.abs(latitude) - latInt) * 60.0f);
        // Numerical response treat as full
        String command = String.format(":St%c%02d*%02d#", sgn, latInt, latMin);
        oat.sendCommand(command, new OATComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {
                if (success && result.equals("1")) {
                    Message writtenMsg = handler.obtainMessage(Mount.SET_SITE_LATITUDE, success ? 1 : 0, 0, latitude);
                    writtenMsg.sendToTarget();
//                    return lat.data;
                } else {
                    // Unknown state so get the value the mount is using
                    getSiteLatitude();
                }
            }
        });
    }

    public void setSiteLongitude(float longitude) {
        longitude = longitude < 0 ? longitude + 360 : longitude;
        int lonInt = (int) longitude;
        int lonMin = (int) ((longitude - lonInt) * 60.0f);
        // Numerical response treat as full
        String command = String.format(":Sg%03d*%02d#", lonInt, lonMin);
        final float finalLongitude = longitude;
        oat.sendCommand(command, new OATComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {
                if (success && result.equals("1")) {
                    Message writtenMsg = handler.obtainMessage(Mount.SET_SITE_LONGITUDE, success ? 1 : 0, 0, finalLongitude);
                    writtenMsg.sendToTarget();
                } else {
                    // Unknown state so get the value the mount is using
                    getSiteLongitude();
                }
            }
        });
    }

    public static class HMS {
        public double h, m, s;

        public HMS() {}
        public HMS(double h, double m, double s) {
            this.h = h;
            this.m = m;
            this.s = s;
        }
    }

    public static void floatToHMS(double val, HMS hms) {
        hms.h = (int) Math.floor(val);
        val = (val - hms.h) * 60;
        hms.m = (int) Math.floor(val);
        val = (val - hms.m) * 60;
        hms.s = (int) Math.round(val);
    }

    public static double HMSToFloat(HMS hms) {
        return hms.h + hms.m / 60d + hms.s / 60d;
    }

    public static double tryParseRA(String ra) {
        String[] parts = ra.split(":");
        try {
            return Integer.parseInt(parts[0]) + Integer.parseInt(parts[1]) / 60.0 + Integer.parseInt(parts[2]) / 3600.0;
        } catch (Exception e) {
            return -1d;
        }
    }

    public static double tryParseDec(String dec) {
        String[] parts = dec.split("\\*|\'");
        try {
            double dDec = Integer.parseInt(parts[0]) + Integer.parseInt(parts[1]) / 60.0;
            if (parts.length > 2) {
                dDec += Integer.parseInt(parts[2]) / 3600.0;
            }

            return dDec;
        } catch (Exception e) {
            return -1d;
        }
    }

    public void startMoving(String dir) {
        // Blind, no response
        String command = String.format(":M%s#", dir);
        oat.sendBlindCommand(command);
        Message writtenMsg = handler.obtainMessage(Mount.START_MOVING, 1, 0, null);
        writtenMsg.sendToTarget();
//        MountState.setSlewing(true);
//        ++_moveState;
    }

    public void stopMoving(String dir) {
        // Blind, no response
        String command = String.format(":Q%s#", dir);
        oat.sendBlindCommand(command);
        Message writtenMsg = handler.obtainMessage(Mount.STOP_MOVING, 1, 0, null);
        writtenMsg.sendToTarget();
//        --_moveState;
//        if (_moveState <= 0) {
//            _moveState = 0;
//            MountState.setSlewing(false);
    }

    public void slew(final TelescopePosition position) {
        int deg, hour, min, sec;
        final HMS hms = new HMS();
        floatToHMS(Math.abs(position.Declination), hms);
        deg = (int) hms.h;
        min = (int) hms.m;
        sec = (int) hms.s;
        char sign = position.Declination < 0 ? '-' : '+';
        // Numerical response treat as full
        String command = String.format(":Sd%c%02d*%02d:%02d#", sign, deg, min, sec);
        oat.sendCommand(command, new OATComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {
                if (!success || !result.equals("1")) {
                    Message writtenMsg = handler.obtainMessage(Mount.SLEW, 0, 0, result);
                    writtenMsg.sendToTarget();
                    getPosition();
                    return;
                }

                int hour, min, sec;
                floatToHMS(Math.abs(position.RightAscension), hms);
                hour = (int) hms.h;
                min = (int) hms.m;
                sec = (int) hms.s;
                // Numerical response treat as full
                String command2 = String.format(":Sr%02d:%02d:%02d#", hour, min, sec);
                oat.sendCommand(command2, new OATComms.NumericCommandResponse() {
                    @Override
                    public void result(boolean success2, String result2) {
                        if (!success2 || !result2.equals("1")) {
                            Message writtenMsg = handler.obtainMessage(Mount.SLEW, 0, 1, result2);
                            writtenMsg.sendToTarget();
                            getPosition();
                            return;
                        }
                        // Numerical response treat as full
                        oat.sendCommand(":MS#", new OATComms.NumericCommandResponse() {
                            @Override
                            public void result(boolean success3, String result3) {
                                // Result is 0 is returned if the telescope can complete the slew,
                                //        1 is returned if the object is below the horizon, and 2 is returned if the object is below the 'higher'
                                //        limit.
                                //        Hopefully the following doesn't happen
                                //        If 1 or 2 is returned, a string containing an appropriate message is also returned.
                                Message writtenMsg = handler.obtainMessage(Mount.SLEW, success3 ? 1 : 0, 2, result3);
                                writtenMsg.sendToTarget();
                                getPosition();
                            }
                        });
                    }
                });
            }
        });
    }

    public void sync(final TelescopePosition position) {
        int deg, hour, min, sec;
        HMS hms = new HMS();
        floatToHMS(Math.abs(position.Declination), hms);
        deg = (int) hms.h;
        min = (int) hms.m;
        sec = (int) hms.s;
        char sign = position.Declination < 0 ? '-' : '+';
        // Numerical response, treat as full
        String command = String.format(":Sd%c%02d*%02d:%02d#", sign, deg, min, sec);
        oat.sendCommand(command, new OATComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {
                if (!success || result.equals("1")) {
                    Message writtenMsg = handler.obtainMessage(Mount.SYNC, 0, 0, result);
                    writtenMsg.sendToTarget();
                    return;
                }
                HMS hms = new HMS();
                floatToHMS(Math.abs(position.RightAscension), hms);
                int hour, min, sec;
                hour = (int) hms.h;
                min = (int) hms.m;
                sec = (int) hms.s;
                // Numerical response treat as full
                String command2 = String.format(":Sr%02d:%02d:%02d#", hour, min, sec);
                oat.sendCommand(command2, new OATComms.NumericCommandResponse() {
                    @Override
                    public void result(boolean success2, String result2) {
                        if (!success2 || result2.equals("1")) {
                            Message writtenMsg = handler.obtainMessage(Mount.SYNC, 0, 0, result2);
                            writtenMsg.sendToTarget();
                            return;
                        }
                        // Blind, no response
                        boolean success3 = oat.sendBlindCommand(":CM#");
                        Message writtenMsg = handler.obtainMessage(Mount.SYNC, success3 ? 1 : 0, 0, position);
                        writtenMsg.sendToTarget();
                    }
                });

            }
        });
    }

    public void syncPolaris(final TelescopePosition position) {
        int ddeg, dmin, dsec;
        int rhour, rmin, rsec;
        HMS hms = new HMS();
        floatToHMS(Math.abs(position.Declination), hms);
        ddeg = (int) hms.h;
        dmin = (int) hms.m;
        dsec = (int) hms.s;

        floatToHMS(Math.abs(position.RightAscension), hms);
        rhour = (int) hms.h;
        rmin = (int) hms.m;
        rsec = (int) hms.s;

        char dsign = position.Declination < 0 ? '-' : '+';
        // Numerical response, treat as full
        String command = String.format(":SY%c%02d*%02d:%02d.%02d:%02d:%02d#", dsign, ddeg, dmin, dsec, rhour, rmin, rsec);
        oat.sendCommand(command, new OATComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {
                if (!success || result.equals("1")) {
                    Message writtenMsg = handler.obtainMessage(Mount.SYNC_POLARIS, 0, 0, result);
                    writtenMsg.sendToTarget();
                    return;
                }
                Message writtenMsg = handler.obtainMessage(Mount.SYNC_POLARIS, success ? 1 : 0, 0, position);
                writtenMsg.sendToTarget();
            }
        });
    }

    public void goHome() {
        boolean success = oat.sendBlindCommand(":hP#");
        Message writtenMsg = handler.obtainMessage(Mount.GO_HOME, success ? 1 : 0, 0, null);
        writtenMsg.sendToTarget();
    }

    public void setHome() {
        // Numerical response, treat as full
        oat.sendCommand(":SHP#", new OATComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {
                Message writtenMsg = handler.obtainMessage(Mount.SET_HOME, success ? 1 : 0, 0, result);
                writtenMsg.sendToTarget();
                // Unknown result
            }
        });
    }

    public void getHA() {
        // Get HA?
        // Full response
        oat.sendCommand(":XGH#", new OATComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                if (success && result.length() >= 6) {
                    String newHa = String.format("%sh %sm %ss", result.substring(0, 2), result.substring(2, 4), result.substring(4, 6));
                    Message writtenMsg = handler.obtainMessage(Mount.GET_HA, success ? 1 : 0, 0, newHa);
                    writtenMsg.sendToTarget();
                } else {
                    Log.e(TAG, "getHA: failed");
                    Message writtenMsg = handler.obtainMessage(Mount.GET_HA, 0, 0, result);
                    writtenMsg.sendToTarget();
                }
            }
        });
    }

    public void setTracking(final boolean enabled) {
        final int b = enabled ? 1 : 0;
        // Numerical response, treat as full
        oat.sendCommand(":MT" + b + "#", new OATComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {
//              MountState.setTracking(enabled);
                int state = 0;
                if (result.equals("1")) {
                    state = b;
                }
                Message writtenMsg = handler.obtainMessage(Mount.SET_TRACKING, success ? 1 : 0, state, result);
                writtenMsg.sendToTarget();
            }
        });
    }

    public void setLocation(final double lat, final double lon, final double altitudeInMeters, double lstInHours) {

        // Longitude
        setSiteLongitude((float) lon);
        setSiteLatitude((float) lat);
        Log.e(TAG, "setLocation: Not finished will cause issues");
//        // TODO: if (!status.success) return false;
//        // TODO: if (!status.success) return false;

        // GMT Offset
        Calendar c = Calendar.getInstance();
        TimeZone tz = c.getTimeZone();
        int hours = (tz.getDSTSavings() + tz.getRawOffset()) / 3600000;
        char offsetSign = hours > 0 ? '+' : '-';
        int offset = Math.abs(hours);
        // Numerical response, treat as full
        String tzCommand = String.format(":SG%c%02d#", offsetSign, offset);
        oat.sendCommand(tzCommand, new OATComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {

            }
        });
        // TODO: if (!status.success) return false;


        // Local Time and Date
        final int h = c.get(Calendar.HOUR_OF_DAY);
        final int m = c.get(Calendar.MINUTE);
        final int s = c.get(Calendar.SECOND);
        final int mm = c.get(Calendar.MONTH) + 1;
        final int dd = c.get(Calendar.DAY_OF_MONTH);
        final int yy = c.get(Calendar.YEAR);
        // Numerical response, treat as full
        String timeCommand = String.format(":SL%02d:%02d:%02d#", h, m, s);
        oat.sendCommand(timeCommand, new OATComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {

            }
        });
        // TODO: if (!status.success) return false;
        // Full response
        String dateCommand = String.format(":SC%02d:%02d:%02d#", mm, dd, yy-2000);
        oat.sendCommand(dateCommand, new OATComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {

            }
        });


        oat.sendCommand(":GVN#", new OATComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                int version = parseVersion(result);

                if (version < 10864) {
                    // Set LST manually
                    double decimalTime = (h + m / 60.0) + (s / 3600.0);
                    double jd = calculateJulianDay(dd, mm, yy, decimalTime);
                    double lst = calculateSiderealTimeLM(jd, lon);
                    String command = ":SHL" + doubleToHMS(lst) + "#";
                    Log.d(TAG, "Set LST: " + command);
                    oat.sendCommand(command, new OATComms.NumericCommandResponse() {
                        @Override
                        public void result(boolean success, String result) {

                        }
                    });
                }
            }
        });

//        // Set LST
//        float lst = AstroTimeUtil.calculateSiderealTime(c.getTime(), (float) lon);
//        HMS lstHms = new HMS();
//        floatToHMS(lst, lstHms);
//        Log.e(TAG, String.format("New LST %02d:%02d", (int)lstHms.h, (int)lstHms.m));
//        String lstCommand = String.format(":SHL%02d:%02d#", (int)lstHms.h, (int)lstHms.m);
//        oat.sendCommand(lstCommand, new OATComms.NumericCommandResponse() {
//            @Override
//            public void result(boolean success, String result) {
//
//            }
//        });

    }

    // Mostly taken from OpenAstroTracker - OAT desktop
    private double frac(double x) {
        x = x - Math.floor(x);
        if (x < 0) x = x + 1.0;
        return x;
    }

    // Get the Julian Day as double
    private double calculateJulianDay(int day, int month, int year, double u) {
        if (month <= 2) {
            month += 12;
            year -= 1;
        }
        return Math.floor(365.25 * (year + 4716.0)) + Math.floor(30.6001 * (month + 1)) + day - 13.0 - 1524.5 + u / 24.0;
    }

    // Calculate Local Sidereal Time
    // Reference https://greenbankobservatory.org/education/great-resources/lst-clock/
    private double calculateSiderealTimeGM(double jd) {
        double t_eph, ut, MJD0, MJD;

        MJD = jd - 2400000.5;
        MJD0 = Math.floor(MJD);
        ut = (MJD - MJD0) * 24.0;
        t_eph = (MJD0 - 51544.5) / 36525.0;
        return 6.697374558 + 1.0027379093 * ut + (8640184.812866 + (0.093104 - 0.0000062 * t_eph) * t_eph) * t_eph / 3600.0;
    }

    private double calculateSiderealTimeLM(double jd, double longitude) {
        double GMST = calculateSiderealTimeGM(jd);
        double LMST = 24.0 * frac((GMST + longitude / 15.0) / 24.0);
        return LMST;
    }

    // Convert decimal time to HH:MM:SS
    private String doubleToHMS(double time)  {
        return doubleToHMS(time, "", "", "");
    }

    private String doubleToHMS(double time, String delimiter1, String delimiter2, String delimiter3)  {
        int h = (int) Math.floor(time);
        int min = (int) Math.floor(60.0 * frac(time));
        int secs = (int) Math.floor(60.0 * (60.0 * frac(time) - min));

        return String.format("%02d%s%02d%s%02d%s", h, delimiter1, min, delimiter2, secs, delimiter3);
    }


    // Other controls?

    public void park() {
        // No response
        oat.sendBlindCommand(":hP#");
        Message writtenMsg = handler.obtainMessage(Mount.PARK, 1, 0, null);
        writtenMsg.sendToTarget();
    }

    public void unpark() {
        // Numerical response, treating as full
        oat.sendCommand(":hU#", new OATComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {
                Message writtenMsg = handler.obtainMessage(Mount.UNPARK, success ? 1 : 0, 0, result);
                writtenMsg.sendToTarget();
            }
        });
    }

    public void stopSlewing(char dir) {
        // No response
        oat.sendBlindCommand(":Q"+dir+"#");
        Message writtenMsg = handler.obtainMessage(Mount.STOP_SLEWING, 1, 0, null);
        writtenMsg.sendToTarget();
    }

    public void toggleSlewing(char dir) {
        // No response
        oat.sendBlindCommand(":M"+dir+"#");
        Message writtenMsg = handler.obtainMessage(Mount.START_SLEWING, 1, 0, null);
        writtenMsg.sendToTarget();
    }

    public void toggleSlewing(String direction) {
        boolean turnOn = direction.charAt(0) == '+';
        char dir = Character.toLowerCase(direction.charAt(1));
        if (turnOn) {
            toggleSlewing(dir);
        } else {
            stopSlewing(dir);
        }
    }

    public void getRAStepsPerDegree() {
        // Full response
        oat.sendCommand(":XGR#", new OATComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                float steps = -1;
                try {
                    steps = Float.parseFloat(result);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "getRaStepsPerDegree: Failed to parse ra steps " + result);
                }
                Message writtenMsg = handler.obtainMessage(Mount.GET_RA_STEPS_PER_DEG, success ? 1 : 0, 0, steps);
                writtenMsg.sendToTarget();
            }
        });
    }

    public void getDecStepsPerDegree() {
        // Full response
        oat.sendCommand(":XGD#", new OATComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                float steps = -1;
                try {
                    steps = Float.parseFloat(result);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "getDecStepsPerDegree: Failed to parse dec steps " + result);
                }
                Message writtenMsg = handler.obtainMessage(Mount.GET_DEC_STEPS_PER_DEG, success ? 1 : 0, 0, steps);
                writtenMsg.sendToTarget();
            }
        });
    }

    public void getSpeedFactor() {
        // Full response
        oat.sendCommand(":XGS#", new OATComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                float speedFactor = -1;
                try {
                    speedFactor = Float.parseFloat(result);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "getSpeedFactor: Failed to parse speed factor " + result);
                }
                Message writtenMsg = handler.obtainMessage(Mount.GET_SPEED_FACTOR, success ? 1 : 0, 0, speedFactor);
                writtenMsg.sendToTarget();
            }
        });
    }

    public void setSpeedFactor(float speedFactor) {
        // No response
        int sfInt = (int) Math.abs(speedFactor);
        int sfDec = (int) ((Math.abs(speedFactor) - sfInt) * 1000);
        if (sfInt > 9) {
            sfInt = 9;
        } else if (sfInt < 0) {
            sfInt = 0;
        }

        String command = String.format(":XSS%02d.%03d#", sfInt, sfDec);
        oat.sendBlindCommand(command);
        getSpeedFactor();
    }

    public void setRaStepsPerDeg(int steps) {
        // No response
        oat.sendBlindCommand(":XSR" + steps + "#");
        getRAStepsPerDegree();
    }

    public void setDecStepsPerDeg(int steps) {
        // No response
        oat.sendBlindCommand(":XSD" + steps + "#");
        getDecStepsPerDegree();
    }


    public void moveSlightly(char direction, int duration) {
        direction = Character.toUpperCase(direction);
        if (duration < 0 || duration > 9999) {
            Log.w(TAG, "moveSlightly: Not moving duration out of bounds");
            return;
        }
        oat.sendCommand(String.format(":MG%c%04d#", direction, duration), new OATComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {
//                Log.d(TAG, "moveSlightlyResult: " + success + " " + result);
//                Message writtenMsg = handler.obtainMessage(Mount.GET_SPEED_FACTOR, success ? 1 : 0, 0, result);
//                writtenMsg.sendToTarget();
            }
        });
    }

    public void getTrackingState() {
        // Full response
        oat.sendCommand(":GIT#", new OATComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                int state = 0;
                if (result.equals("1")) {
                    state = 1;
                }
                Message writtenMsg = handler.obtainMessage(Mount.GET_TRACKING_STATE, success ? 1 : 0, state);
                writtenMsg.sendToTarget();
            }
        });
    }

    public void getSlewingState() {
        // Full response
        oat.sendCommand(":GIS#", new OATComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                int state = 0;
                if (result.equals("1")) {
                    state = 1;
                }
                Message writtenMsg = handler.obtainMessage(Mount.GET_SLEWING_STATE, success ? 1 : 0, state);
                writtenMsg.sendToTarget();
            }
        });
    }

    public void getMountVersion() {
        oat.sendCommand(":GVN#", new OATComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                int firmwareVersion = parseVersion(result);
                Message writtenMsg = handler.obtainMessage(Mount.GET_MOUNT_VERSION, success ? 1 : 0, firmwareVersion);
                writtenMsg.sendToTarget();
            }
        });
    }

    private int parseVersion(String s) {
        String[] version = s.substring(1).split("\\.");
        Integer[] v = new Integer[version.length];
        for (int i = 0 ; i < version.length; i++) {
            try {
                v[i] = Integer.parseInt(version[i]);
            } catch (NumberFormatException e) {
                v[i] = 0;
                Log.e(TAG, "Failed to parse MountVersion " + i + " " + version[i]);
            }
        }
        return v[0] * 10000 + v[1] * 100 + v[2];
    }

}
