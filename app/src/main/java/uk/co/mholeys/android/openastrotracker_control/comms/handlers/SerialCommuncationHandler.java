package uk.co.mholeys.android.openastrotracker_control.comms.handlers;

import uk.co.mholeys.android.openastrotracker_control.comms.ICommunicationHandler;

public class SerialCommuncationHandler implements ICommunicationHandler {

    @Override
    public CommandResponse sendBlind(String command) {
        return null;
    }

    @Override
    public CommandResponse sendCommand(String command) {
        return null;
    }

    @Override
    public CommandResponse sendCommandConfirm(String command) {
        return null;
    }

    @Override
    public boolean connected() {
        return false;
    }

    @Override
    public void disconnect() {

    }
}
