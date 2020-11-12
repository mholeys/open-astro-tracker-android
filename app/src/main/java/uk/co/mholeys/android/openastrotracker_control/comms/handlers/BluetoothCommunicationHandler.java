package uk.co.mholeys.android.openastrotracker_control.comms.handlers;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import uk.co.mholeys.android.openastrotracker_control.comms.ICommunicationHandler;
import uk.co.mholeys.android.openastrotracker_control.comms.Log;

public class BluetoothCommunicationHandler implements ICommunicationHandler {

    BluetoothSocket socket;
    BluetoothDevice device;

    BufferedInputStream in;
    BufferedOutputStream out;

    long requestIndex = 1;

    public BluetoothCommunicationHandler(BluetoothDevice device, BluetoothSocket socket) {
        this.device = device;
        this.socket = socket;
        try {
            in = new BufferedInputStream(socket.getInputStream());
            out = new BufferedOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            // TODO:
            e.printStackTrace();
        }
    }

    @Override
    public CommandResponse sendBlind(String command) {
        return sendCommand(command, ResponseType.NoResponse);
    }

    @Override
    public CommandResponse sendCommand(String command) {
        return sendCommand(command, ResponseType.FullResponse);
    }

    @Override
    public CommandResponse sendCommandConfirm(String command) {
        return sendCommand(command, ResponseType.DigitResponse);
    }

    private CommandResponse sendCommand(String command, ResponseType needsResponse) {
        requestIndex++;
        requestIndex++;
        try {
            Log.writeLine("[{0:0000}] SERIAL: [%s] Sending command", requestIndex, command);
            out.write(command.getBytes());
        } catch (Exception e) {
            Log.writeLine("[{0:0000}] SERIAL: [%s] Failed to send command. %s", requestIndex, command, e.getMessage());
            return new CommandResponse("", false, String.format("Unable to write to %s. ", device.getName()) + e.getMessage());
        }

        try {
            String response;
            switch (needsResponse) {
                case NoResponse:
                    Log.writeLine("[{0:0000}] SERIAL: [%s] No response needed for command", requestIndex, command);
                    return new CommandResponse("", true);
                case DigitResponse:
                    Log.writeLine("[{0:0000}] SERIAL: [%s] Expecting single digit response for command, waiting...", requestIndex, command);
                    response = String.valueOf((char) in.read());
                    Log.writeLine("[{0:0000}] SERIAL: [%s] Received single digit response '%s' for command", requestIndex, command, response);
                    return new CommandResponse(response, true);
                case FullResponse:
                    Log.writeLine("[{0:0000}] SERIAL: [%s] Expecting #-delimited response for Command, waiting...", requestIndex, command);
                    response = readUntil('#');
                    Log.writeLine("[{0:0000}] SERIAL: [%s] Received response '%s' for command", requestIndex, command, response);
                    return new CommandResponse(response, true);

            }
        } catch (Exception e) {
            Log.writeLine("[{0:0000}] SERIAL: [%s] Failed to receive response to command. %s", requestIndex, command, e.getMessage());
            return new CommandResponse("", false, String.format("Unable to read response to %s from %s. %s", command, device.getName(), e.getMessage()));
        }

        return new CommandResponse("", false, "Something weird going on...");
    }

    @Override
    public boolean connected() {
        return socket.isConnected();
    }

    @Override
    public void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readUntil(char target) throws IOException {
        StringBuilder sb = new StringBuilder(3);
        int r = -1;
        while ((r = in.read()) != -1) {
            char c = (char) r;
            if (c == target) {
                return sb.toString();
            }
            sb.append(c);
        }
        return sb.toString();
    }

}
