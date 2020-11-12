package uk.co.mholeys.android.openastrotracker_control.comms;

import uk.co.mholeys.android.openastrotracker_control.comms.handlers.CommandResponse;

public interface ICommunicationHandler {

    // TODO: Async/Promise?

    // Send a command, no response expected
    CommandResponse sendBlind(String command);

    // Send a command, expect a '#' terminated response
    CommandResponse sendCommand(String command);

    // Send a command, expect a single digit response
    CommandResponse sendCommandConfirm(String command);

    boolean connected();

    void disconnect();

}
