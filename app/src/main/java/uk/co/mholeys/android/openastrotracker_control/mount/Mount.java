package uk.co.mholeys.android.openastrotracker_control.mount;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import uk.co.mholeys.android.openastrotracker_control.comms.model.MountState;
import uk.co.mholeys.android.openastrotracker_control.comms.model.OTAEpoch;
import uk.co.mholeys.android.openastrotracker_control.comms.model.TelescopePosition;

public class Mount {

    private static final String TAG = "OTA_MOUNT";
    OTAComms ota;
    private Handler handler;

    public Mount(BluetoothSocket socket, Handler handler) throws IOException {
        ota = new OTAComms(socket.getInputStream(), socket.getOutputStream());
        this.handler = handler;
        ota.start();
    }

    public Mount(Socket socket) throws IOException {
        ota = new OTAComms(socket.getInputStream(), socket.getOutputStream());
        ota.start();
    }

    // TODO: implement handler that takes results from all of these callbacks
    // TODO:^possibly abstract to allow bluetooth/wifi/serial differences?

    // TODO: implement all these messages in handler
    // TODO: finish parsing data in callbacks!

    public static final int REFRESH_MOUNT_STATE    = 1;
    public static final int GET_POSITION           = 2;
    public static final int GET_SITE_LATITUDE      = 3;
    public static final int GET_SITE_LONGITUDE     = 4;
    public static final int SET_SITE_LATITUDE      = 5;
    public static final int SET_SITE_LONGITUDE     = 6;
    public static final int START_MOVING           = 7;
    public static final int SLEW                   = 8;
    public static final int SYNC                   = 9;
    public static final int GO_HOME                = 10;
    public static final int SET_HOME               = 11;
    public static final int GET_HA                 = 12;
    public static final int SET_TRACKING           = 13;
    public static final int SET_LOCATION           = 14;
    public static final int PARK                   = 15;
    public static final int UNPARK                 = 16;
    public static final int STOP_SLEWING           = 17;
    public static final int START_SLEWING          = 18;
    public static final int GET_RA_STEPS_PER_DEG   = 19;
    public static final int GET_DEC_STEPS_PER_DEG  = 20;
    public static final int GET_SPEED_FACTOR       = 21;

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
                            MountState.setRightAscension(dRa);
                            MountState.setDeclination(dDec);
                            Message writtenMsg = handler.obtainMessage(Mount.GET_POSITION, -0, -1, new TelescopePosition(dRa, dDec, OTAEpoch.JNOW));
                            writtenMsg.sendToTarget();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
            }
        };
        // Full response
        ota.sendCommand(":GD#", decCallback);

//
//        MountState.setRightAscension(0);
//        MountState.setDeclination(0);
//        return TelescopePosition.Invalid;
    }

    public void getSiteLatitude() {
        // Full response
        ota.sendCommand(":Gt#", new OTAComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                if (success) {
                    Log.d(TAG, "getSiteLat: got" + result);
                    float lat = (float) tryParseDec(result);
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
                } else {
                    // 0
                    Log.e(TAG, "getSiteLon: failed");
                }
            }
        });
    }

    public void setSiteLatitude(float latitude) {
        char sgn = latitude < 0 ? '-' : '+';
        int latInt = (int) Math.abs(latitude);
        int latMin = (int) ((Math.abs(latitude) - latInt) * 60.0f);
        // Numerical response treat as full
        String command = String.format(":St%c%02d*%02d#", sgn, latInt, latMin);
        ota.sendCommand(command, new OTAComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                if (success) {
//                    return lat.data;
                } else {
                    // 0
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
        ota.sendCommand(command, new OTAComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                if (success) {
//                    return lon.data;
                } else {
                    // 0
                }
            }
        });
    }

    public class HMS {
        double h, m, s;
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
//        MountState.setSlewing(true);
//        ++_moveState;

    }

    public void stopMoving(String dir) {
        // Blind, no response
        String command = String.format(":Q%s#", dir);
        ota.sendBlindCommand(command);
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
        ota.sendCommand(command, new OTAComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                if (!success || !result.equals("1")) {
                    // TODO:
                    //return false;
                }

                int hour, min, sec;
                floatToHMS(Math.abs(position.RightAscension), hms);
                hour = (int) hms.h;
                min = (int) hms.m;
                sec = (int) hms.s;
                // Numerical response treat as full
                String command2 = String.format(":Sr%02d:%02d:%02d#", hour, min, sec);
                ota.sendCommand(command2, new OTAComms.CommandResponse() {
                    @Override
                    public void result(boolean success2, String result2) {
                        if (!success2 || !result2.equals("1")) {
                            // TODO:
                            //return false;
                        }
                        // Numerical response treat as full
                        String command3 = String.format(":MS#");
                        ota.sendCommand(command3, new OTAComms.CommandResponse() {
                            @Override
                            public void result(boolean success3, String result3) {
                                // return success3
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
        ota.sendCommand(command, new OTAComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                if (!success || result.equals("1")) {
                    // TODO: return false;
                }
                HMS hms = new HMS();
                floatToHMS(Math.abs(position.RightAscension), hms);
                int hour, min, sec;
                hour = (int) hms.h;
                min = (int) hms.m;
                sec = (int) hms.s;
                // Numerical response treat as full
                String command2 = String.format(":Sr%02d:%02d:%02d#", hour, min, sec);
                ota.sendCommand(command2, new OTAComms.CommandResponse() {
                    @Override
                    public void result(boolean success2, String result2) {
                        if (!success2 || result2.equals("1")) {
                            // TODO: return false;
                        }
                        // Blind, no response
                        boolean success3 = ota.sendBlindCommand(":CM#,#");
                        // TODO: return success3;
                    }
                });

            }
        });
    }

    public void goHome() {
        boolean success = ota.sendBlindCommand(":hP#");
        // TODO return success;
    }

    public void setHome() {
        // Numerical response, treat as full
        ota.sendCommand(":SHP#", new OTAComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                // Todo:
                Log.d(TAG, "setHomeResult: "  + success + " " + result);
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
                    // TODO:?
                } else {
                    Log.e(TAG, "getHA: failed");
                }
            }
        });
    }

    public void setTracking(final boolean enabled) {
        int b = enabled ? 1 : 0;
        // Numerical response, treat as full
        ota.sendCommand(":MT%d#", new OTAComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                if (success) {
                    MountState.setTracking(enabled);
                }
                // TODO: return success;
            }
        });
    }

    public void setLocation(double lat, double lon, double altitudeInMeters, double lstInHours) {

        // Longitude

        if (lon < 0) {
            lon = 360 + lon;
        }
        int lonFront = (int) lon;
        int lonBack = (int) ((lon - lonFront) * 60);
        // Numerical response, treat as full
        String lonCmd = String.format(":Sg%03d*%02d#", lonFront, lonBack);
        ota.sendCommand(lonCmd, new OTAComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {

            }
        });
        // TODO: if (!status.success) return false;


        // Latitude
        char latSign = lat > 0 ? '+' : '-';
        double absLat = Math.abs(lat);
        int latFront = (int) absLat;
        int latBack = (int) ((absLat - latFront) * 60.0);
        // Numerical response, treat as full
        String latCmd = String.format(":St%c%02d*%02d#", latSign, latFront, latBack);
        ota.sendCommand(latCmd, new OTAComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {

            }
        });
        // TODO: if (!status.success) return false;


        // GMT Offset
        Calendar c = Calendar.getInstance();
        TimeZone tz = c.getTimeZone();
        int hours = (tz.getDSTSavings() + tz.getRawOffset()) / 3600000;
        char offsetSign = hours > 0 ? '+' : '-';
        int offset = Math.abs(hours);
        // Numerical response, treat as full
        String tzCommand = String.format(":SG%c%02d#", offsetSign, offset);
        ota.sendCommand(tzCommand, new OTAComms.CommandResponse() {
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
        ota.sendCommand(timeCommand, new OTAComms.CommandResponse() {
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
            }
        });
    }

    public void unpark() {
        // Numerical response, treating as full
        ota.sendCommand(":hU#", new OTAComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                Log.d(TAG, "unparkResult: " + success + " " + result);
            }
        });
    }

    public void stopSlewing(char dir) {
        // No response
        ota.sendBlindCommand(":Q"+dir+"#");
    }

    public void startSlewing(char dir) {
        // No response
        ota.sendBlindCommand(":M"+dir+"#");
    }

    public void startSlewing(String direction) {
        boolean turnOn = direction.charAt(0) == '+';
        char dir = Character.toLowerCase(direction.charAt(1));
        if (turnOn) {
            startSlewing(dir);
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
            }
        });
    }

    public void getDecStepsPerDegree() {
        // Full response
        ota.sendCommand(":XGD#", new OTAComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                Log.d(TAG, "getDECStepsPerDegreeResult: " + success + " " + result);
            }
        });
    }

    public void getSpeedFactor() {
        // Full response
        ota.sendCommand(":XGS#", new OTAComms.CommandResponse() {
            @Override
            public void result(boolean success, String result) {
                Log.d(TAG, "getSpeedFactorResult: " + success + " " + result);
            }
        });
    }

}
