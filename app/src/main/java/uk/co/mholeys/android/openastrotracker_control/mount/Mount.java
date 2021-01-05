package uk.co.mholeys.android.openastrotracker_control.mount;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import uk.co.mholeys.android.openastrotracker_control.comms.model.OTAEpoch;
import uk.co.mholeys.android.openastrotracker_control.comms.model.TelescopePosition;

public class Mount {

    private static final String TAG = "OTA_MOUNT";
    public OTAComms ota;
    private Handler handler;

    public Mount(Socket socket, Handler handler) throws IOException {
        this(socket.getInputStream(), socket.getOutputStream(), handler);
    }

    public Mount(BluetoothSocket socket, Handler handler) throws IOException {
        this(socket.getInputStream(), socket.getOutputStream(), handler);
    }

    public Mount(InputStream in, OutputStream out, Handler handler) {
        ota = new OTAComms(in, out);
        this.handler = handler;
        ota.start();
    }

    MutableLiveData<TelescopePosition> currentPosition = new MutableLiveData<>();
    MutableLiveData<TelescopePosition> objectPosition = new MutableLiveData<>();
    MutableLiveData<Boolean> trackingState = new MutableLiveData<>();
    MutableLiveData<Float> siteLatitude = new MutableLiveData<>();
    MutableLiveData<Float> siteLongitude = new MutableLiveData<>();
    MutableLiveData<Integer> raStepsPerDeg = new MutableLiveData<>();
    MutableLiveData<Integer> decStepsPerDeg = new MutableLiveData<>();
    MutableLiveData<Float> speedFactor = new MutableLiveData<>();

    // TODO: store current state here now!
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

    public static final int SYNC_POLARIS          = 23;
    public static final int MOVE_SLIGHTLY          = 24;

//    private static final Handler bluetoothHandler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            StringBuilder result = new StringBuilder();
////            switch (msg.what) {
////
////            }
//        }
//    };


    public void refreshMountState() {
        // Full response
        ota.sendCommand(":GX#", new OTAComms.CommandResponse() {
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
        ota.end();
    }

    static class RACommandResponse implements OTAComms.CommandResponse {
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
        ota.sendCommand(":GR#", raCallback);
        OTAComms.CommandResponse decCallback = new OTAComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                String newRA = raCallback.newRA();
                String newDEC = result;
                if (!success || (newRA == null) || (newDEC == null)) {
                    Log.e(TAG, "getPositionDEC: failed");
                } else {
                    // Parse RA/DEC
                        double dRa, dDec;
                        try {
                            dRa = tryParseRA(newRA);
                            dDec = tryParseDec(newDEC);
                            TelescopePosition pos = new TelescopePosition(dRa, dDec, OTAEpoch.JNOW);
                            currentPosition.postValue(pos);
                            Message writtenMsg = handler.obtainMessage(Mount.GET_POSITION, -0, -1, pos);
                            writtenMsg.sendToTarget();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
            }
        };
        // Full response
        ota.sendCommand(":GD#", decCallback);
    }

    public void getSiteLatitude() {
        // Full response
        ota.sendCommand(":Gt#", new OTAComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                if (success) {
                    Log.d(TAG, "getSiteLat: got" + result);
                    float lat = (float) tryParseDec(result);
                    siteLatitude.postValue(lat);
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
        ota.sendCommand(":Gg#", new OTAComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                if (success) {
                    Log.d(TAG, "getSiteLon: got" + result);
                    double lon = tryParseDec(result);
                    if (lon > 180) {
                        lon -= 360;
                    }
                    siteLongitude.postValue((float) lon);
                    Message writtenMsg = handler.obtainMessage(Mount.GET_SITE_LONGITUDE, success ? 1 : 0, 0, lon);
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
        int latInt = (int) Math.abs(latitude);
        int latMin = (int) ((Math.abs(latitude) - latInt) * 60.0f);
        // Numerical response treat as full
        String command = String.format(":St%c%02d*%02d#", sgn, latInt, latMin);
        ota.sendCommand(command, new OTAComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {
                if (success && result.equals("1")) {
                    Message writtenMsg = handler.obtainMessage(Mount.SET_SITE_LATITUDE, success ? 1 : 0, 0, result);
                    writtenMsg.sendToTarget();
                    siteLatitude.postValue(latitude);
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
        ota.sendCommand(command, new OTAComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {
                if (success && result.equals("1")) {
                    siteLongitude.postValue(finalLongitude);
                    Message writtenMsg = handler.obtainMessage(Mount.SET_SITE_LONGITUDE, success ? 1 : 0, 0, result);
                    writtenMsg.sendToTarget();
                } else {
                    // Unknown state so get the value the mount is using
                    getSiteLongitude();
                }
            }
        });
    }

    public static class HMS {
        double h, m, s;

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
        return Integer.parseInt(parts[0]) + Integer.parseInt(parts[1]) / 60.0 + Integer.parseInt(parts[2]) / 3600.0;
    }

    public static double tryParseDec(String dec) {
        String[] parts = dec.split("\\*|\'");
        double dDec = Integer.parseInt(parts[0]) + Integer.parseInt(parts[1]) / 60.0;
        if (parts.length > 2) {
            dDec += Integer.parseInt(parts[2]) / 3600.0;
        }

        return dDec;
    }

    public void startMoving(String dir) {
        // Blind, no response
        String command = String.format(":M%s#", dir);
        ota.sendBlindCommand(command);
        Message writtenMsg = handler.obtainMessage(Mount.START_MOVING, 1, 0, null);
        writtenMsg.sendToTarget();
//        MountState.setSlewing(true);
//        ++_moveState;
    }

    public void stopMoving(String dir) {
        // Blind, no response
        String command = String.format(":Q%s#", dir);
        ota.sendBlindCommand(command);
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
        ota.sendCommand(command, new OTAComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {
                Log.d(TAG, "slew result: " + result);
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
                ota.sendCommand(command2, new OTAComms.NumericCommandResponse() {
                    @Override
                    public void result(boolean success2, String result2) {
                        Log.d(TAG, "slew ra result: " + result2);
                        if (!success2 || !result2.equals("1")) {
                            Message writtenMsg = handler.obtainMessage(Mount.SLEW, result2.equals("1") ? 1 : 0, 1, result2);
                            writtenMsg.sendToTarget();
                            getPosition();
                            return;
                        }
                        // Numerical response treat as full
                        String command3 = String.format(":MS#");
                        ota.sendCommand(command3, new OTAComms.NumericCommandResponse() {
                            @Override
                            public void result(boolean success3, String result3) {
                                // Result is 0 is returned if the telescope can complete the slew,
                                //        1 is returned if the object is below the horizon, and 2 is returned if the object is below the 'higher'
                                //        limit.
                                //        Hopefully the following doesn't happen
                                //        If 1 or 2 is returned, a string containing an appropriate message is also returned.
                                Message writtenMsg = handler.obtainMessage(Mount.SLEW, success3 ? 1 : 0, 2, result3);
                                writtenMsg.sendToTarget();
                                Log.d(TAG, "slewslew result: " + result3);
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
        ota.sendCommand(command, new OTAComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {
                if (!success || result.equals("1")) {
                    Message writtenMsg = handler.obtainMessage(Mount.SYNC, success ? 1 : 0, 0, result);
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
                ota.sendCommand(command2, new OTAComms.NumericCommandResponse() {
                    @Override
                    public void result(boolean success2, String result2) {
                        if (!success2 || result2.equals("1")) {
                            Message writtenMsg = handler.obtainMessage(Mount.SYNC, success2 ? 1 : 0, 0, result2);
                            writtenMsg.sendToTarget();
                            return;
                        }
                        // Blind, no response
                        boolean success3 = ota.sendBlindCommand(":CM#");
                        objectPosition.postValue(position);
                        Message writtenMsg = handler.obtainMessage(Mount.SYNC, success3 ? 1 : 0, 0, null);
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
        ota.sendCommand(command, new OTAComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {
                if (!success || result.equals("1")) {
                    Message writtenMsg = handler.obtainMessage(Mount.SYNC_POLARIS, success ? 1 : 0, 0, result);
                    writtenMsg.sendToTarget();
                    return;
                }
            }
        });
    }

    public void goHome() {
        boolean success = ota.sendBlindCommand(":hP#");
        Message writtenMsg = handler.obtainMessage(Mount.GO_HOME, success ? 1 : 0, 0, null);
        writtenMsg.sendToTarget();
    }

    public void setHome() {
        // Numerical response, treat as full
        ota.sendCommand(":SHP#", new OTAComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {
                // Todo:
                Log.d(TAG, "setHomeResult: "  + success + " " + result);
                Message writtenMsg = handler.obtainMessage(Mount.SET_HOME, success ? 1 : 0, 0, result);
                writtenMsg.sendToTarget();
                // Unknown result
            }
        });
    }

    public void getHA() {
        // Get HA?
        // Full response
        ota.sendCommand(":XGH#", new OTAComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                if (success && result.length() >= 6) {
                    Log.d(TAG, "getHA: got " + result);
                    String newHa = String.format("%sh %sm %ss", result.substring(0, 2), result.substring(2, 4), result.substring(4, 6));
                    Log.d(TAG, "getHAPostHome: " + newHa);
                    Message writtenMsg = handler.obtainMessage(Mount.GET_HA, success ? 1 : 0, 0, newHa);
                    writtenMsg.sendToTarget();
                } else {
                    Log.e(TAG, "getHA: failed");
                    Message writtenMsg = handler.obtainMessage(Mount.GET_HA, success ? 1 : 0, 0, result);
                    writtenMsg.sendToTarget();
                }
            }
        });
    }

    public void setTracking(final boolean enabled) {
        int b = enabled ? 1 : 0;
        // Numerical response, treat as full
        ota.sendCommand(":MT" + b + "#", new OTAComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {
//              MountState.setTracking(enabled);
                Log.d(TAG, "result: " + result);
                if (result.equals("1")) {
                    trackingState.postValue(enabled);
                }
                Message writtenMsg = handler.obtainMessage(Mount.SET_TRACKING, success ? 1 : 0, 0, result);
                writtenMsg.sendToTarget();
                // TODO: return success;
            }
        });
    }

    public void setLocation(double lat, double lon, double altitudeInMeters, double lstInHours) {

        // Longitude
        setSiteLongitude((float) lon);
        setSiteLatitude((float) lat);
        Log.e(TAG, "setLocation: Not finished will cause issues");

//        if (lon < 0) {
//            lon = 360 + lon;
//        }
//        int lonFront = (int) lon;
//        int lonBack = (int) ((lon - lonFront) * 60);
//        // Numerical response, treat as full
//        String lonCmd = String.format(":Sg%03d*%02d#", lonFront, lonBack);
//        ota.sendCommand(lonCmd, new OTAComms.NumericCommandResponse() {
//            @Override
//            public void result(boolean success, String result) {
//
//            }
//        });
//        // TODO: if (!status.success) return false;

//
//        // Latitude
//        char latSign = lat > 0 ? '+' : '-';
//        double absLat = Math.abs(lat);
//        int latFront = (int) absLat;
//        int latBack = (int) ((absLat - latFront) * 60.0);
//        // Numerical response, treat as full
//        String latCmd = String.format(":St%c%02d*%02d#", latSign, latFront, latBack);
//        ota.sendCommand(latCmd, new OTAComms.NumericCommandResponse() {
//            @Override
//            public void result(boolean success, String result) {
//
//            }
//        });
//        // TODO: if (!status.success) return false;


        // GMT Offset
        Calendar c = Calendar.getInstance();
        TimeZone tz = c.getTimeZone();
        int hours = (tz.getDSTSavings() + tz.getRawOffset()) / 3600000;
        char offsetSign = hours > 0 ? '+' : '-';
        int offset = Math.abs(hours);
        // Numerical response, treat as full
        String tzCommand = String.format(":SG%c%02d#", offsetSign, offset);
        ota.sendCommand(tzCommand, new OTAComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {

            }
        });
        // TODO: if (!status.success) return false;


        // Local Time and Date
        int h = c.get(Calendar.HOUR_OF_DAY);
        int m = c.get(Calendar.MINUTE);
        int s = c.get(Calendar.SECOND);
        int mm = c.get(Calendar.MONTH) + 1;
        int dd = c.get(Calendar.DAY_OF_MONTH);
        int yy = c.get(Calendar.YEAR);
        // Numerical response, treat as full
        String timeCommand = String.format(":SL:%02d:%02d:%02d#", h, m, s);
        ota.sendCommand(timeCommand, new OTAComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {

            }
        });
        // TODO: if (!status.success) return false;
        // Full response
        String dateCommand = String.format(":SC:%02d/%02d/%02d#,#");
        ota.sendCommand(dateCommand, new OTAComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {

            }
        });
        // TODO: after all return status.success;
    }

    // Other controls?

    public void park() {
        // No response
        ota.sendCommand(":hP#", new OTAComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                Log.d(TAG, "parkResult: " + success + " " + result);
                Message writtenMsg = handler.obtainMessage(Mount.PARK, success ? 1 : 0, 0, result);
                writtenMsg.sendToTarget();
            }
        });
    }

    public void unpark() {
        // Numerical response, treating as full
        ota.sendCommand(":hU#", new OTAComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {
                Log.d(TAG, "unparkResult: " + success + " " + result);
                Message writtenMsg = handler.obtainMessage(Mount.UNPARK, success ? 1 : 0, 0, result);
                writtenMsg.sendToTarget();
            }
        });
    }

    public void stopSlewing(char dir) {
        // No response
        ota.sendBlindCommand(":Q"+dir+"#");
        Message writtenMsg = handler.obtainMessage(Mount.STOP_SLEWING, 1, 0, null);
        writtenMsg.sendToTarget();
    }

    public void toggleSlewing(char dir) {
        // No response
        ota.sendBlindCommand(":M"+dir+"#");
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
        ota.sendCommand(":XGR#", new OTAComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                // TODO:
                Log.d(TAG, "getRAStepsPerDegreeResult: " + success + " " + result);
                try {
                    raStepsPerDeg.postValue(Integer.parseInt(result));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "getRaStepsPerDegree: Failed to parse ra steps " + result);
                }
                Message writtenMsg = handler.obtainMessage(Mount.GET_RA_STEPS_PER_DEG, success ? 1 : 0, 0, result);
                writtenMsg.sendToTarget();
            }
        });
    }

    public void getDecStepsPerDegree() {
        // Full response
        ota.sendCommand(":XGD#", new OTAComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                Log.d(TAG, "getDECStepsPerDegreeResult: " + success + " " + result);
                try {
                    decStepsPerDeg.postValue(Integer.parseInt(result));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "getDecStepsPerDegree: Failed to parse dec steps " + result);
                }
                Message writtenMsg = handler.obtainMessage(Mount.GET_DEC_STEPS_PER_DEG, success ? 1 : 0, 0, result);
                writtenMsg.sendToTarget();
            }
        });
    }

    public void getSpeedFactor() {
        // Full response
        ota.sendCommand(":XGS#", new OTAComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                Log.d(TAG, "getSpeedFactorResult: " + success + " " + result);
                try {
                    speedFactor.postValue(Float.parseFloat(result));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "getSpeedFactor: Failed to parse speed factor " + result);
                }
                Message writtenMsg = handler.obtainMessage(Mount.GET_SPEED_FACTOR, success ? 1 : 0, 0, result);
                writtenMsg.sendToTarget();
            }
        });
    }

    public void moveSlightly(char direction, int duration) {
        direction = Character.toUpperCase(direction);
        if (duration < 0 || duration > 9999) {
            Log.w(TAG, "moveSlightly: Not moving duration out of bounds");
            return;
        }
        ota.sendCommand(String.format(":MG%c%04d#", direction, duration), new OTAComms.NumericCommandResponse() {
            @Override
            public void result(boolean success, String result) {
                Log.d(TAG, "moveSlightlyResult: " + success + " " + result);
//                Message writtenMsg = handler.obtainMessage(Mount.GET_SPEED_FACTOR, success ? 1 : 0, 0, result);
//                writtenMsg.sendToTarget();
            }
        });
    }


}
