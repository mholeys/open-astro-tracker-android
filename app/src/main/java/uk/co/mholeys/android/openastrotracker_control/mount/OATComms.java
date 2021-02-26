package uk.co.mholeys.android.openastrotracker_control.mount;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;


public class OATComms extends Thread {

    private static final String TAG = "OATComms";
    private DataInputStream in;
    private DataOutputStream out;
    private boolean running = false;
    DisconnectListener disconnectListener;

    ConcurrentLinkedQueue<CommandResponse> responseStack;

    public OATComms(InputStream in, OutputStream out) {
        this.in = new DataInputStream(in);
        this.out = new DataOutputStream(out);
        responseStack = new ConcurrentLinkedQueue<>();
        running = true;
    }

    public void run() {
        StringBuilder sb = new StringBuilder(30);
        StringBuilder sb2 = new StringBuilder(30);
        while (running) {
            try {
                if (in.available() == 0) {
                    continue;
                }
//                Log.v(TAG, "run: Got some bytes " + in.available());
                CommandResponse cr = responseStack.remove();
                if (cr instanceof NumericCommandResponse) {
                    // Single number no #
                    sb.append((char)in.readByte());
                } else {
                    // String ended by #
                    readHashIntoSB(sb);
                }

                if (cr instanceof DoubleCommandResponse) {
                    readHashIntoSB(sb2);
//                    Log.v(TAG, "got second\" " + sb2.toString() + "\"");
                    ((DoubleCommandResponse) cr).secondResult(sb2.toString());
                    sb2.delete(0, sb2.length());
                }

//                Log.v(TAG, "got\" " + sb.toString() + "\"");
                cr.result(true, sb.toString());
                sb.delete(0, sb.length());
            } catch (IOException e) {
                if (e.getMessage().equals("socket closed")) {
                    running = false;
                    if (disconnectListener != null) {
                        disconnectListener.disconnected();
                    }
                    break;
                }
                e.printStackTrace();
            }
        }
        if (disconnectListener != null) {
            disconnectListener.disconnected();
        }
    }

    /***
     * Reads from the stream uptill the '#' char, and appends to the given string builder
     * @param sb Output location for the result
     * @throws IOException (InputStream.readByte())
     */
    private void readHashIntoSB(StringBuilder sb) throws IOException {
        int r = -1;
        while ((r = in.readByte()) != -1) {
            char c = (char) r;
            if (c == '#') {
//                Log.v(TAG, "run: End of answer '#'");
                break;
            } else {
//                Log.v(TAG, "run: got '" + c + "'");
                sb.append(c);
            }
        }
    }

    public synchronized void sendCommand(String command, CommandResponse callback) {
        try {
            byte[] bytes = command.getBytes(StandardCharsets.UTF_8);
            responseStack.add(callback);
            out.write(bytes);
            out.flush();
        } catch (IOException e) {
            callback.result(false, null);
        }
    }

    public synchronized boolean sendBlindCommand(String command) {
        try {
            byte[] bytes = command.getBytes(StandardCharsets.UTF_8);
            out.write(bytes);
//            out.writeChars(command);
            out.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void end() {
        running = false;
        Log.d(TAG, "end()");
    }




    public interface CommandResponse {
        public void result(boolean success, String result);
    }
    public interface NumericCommandResponse extends CommandResponse {}
    /**
     * Useful for skipping extra lines like "1Updating Planetary Data#                              #"
     */
    public interface DoubleCommandResponse extends CommandResponse {
        public void secondResult( String result);
    }
    public interface DisconnectListener {
        public void disconnected();
    }

}
