package uk.co.mholeys.android.openastrotracker_control.mount;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;

import uk.co.mholeys.android.openastrotracker_control.comms.handlers.CommandResponse;
import uk.co.mholeys.android.openastrotracker_control.comms.model.MountState;
import uk.co.mholeys.android.openastrotracker_control.comms.model.OTAEpoch;
import uk.co.mholeys.android.openastrotracker_control.comms.model.TelescopePosition;

public class OTAComms extends Thread {

    private static final String TAG = "OTAComms";
    private DataInputStream in;
    private DataOutputStream out;
    private boolean running = false;

    ConcurrentLinkedQueue<CommandResponse> responseStack = new ConcurrentLinkedQueue<>();

    public OTAComms(InputStream in, OutputStream out) {
        this.in = new DataInputStream(in);
        this.out = new DataOutputStream(out);
        running = true;
    }

    public void run() {
        StringBuilder sb = new StringBuilder(30);
        while (running) {
            try {
                if (in.available() == 0) {
                    continue;
                }
                int r = -1;
                while ((r = in.readByte()) != -1) {
                    char c = (char) r;
                    if (c == '#') {
                        break;
                    } else {
                        sb.append(c);
                    }
                }
                CommandResponse cr = responseStack.remove();
                Log.d(TAG, "got " + sb.toString());
                cr.result(true, sb.toString());

                sb.delete(0, sb.length());
            } catch (IOException e) {
                if (e.getMessage().equals("socket closed")) {
                    running = false;
                    break;
                }
                e.printStackTrace();
            }
        }
    }

    public synchronized void sendCommand(String command, CommandResponse callback) {
        try {
            byte[] bytes = command.getBytes(StandardCharsets.UTF_8);
            out.write(bytes);
            out.flush();
            responseStack.add(callback);
        } catch (IOException e) {
            callback.result(false, null);
        }
    }

    public synchronized  boolean sendBlindCommand(String command) {
        try {
            out.writeChars(command);
            out.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void end() {
        running = false;
    }

    public interface CommandResponse {

        public void result(boolean success, String result);

    }

}
