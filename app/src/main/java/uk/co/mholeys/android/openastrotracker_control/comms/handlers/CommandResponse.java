package uk.co.mholeys.android.openastrotracker_control.comms.handlers;

public class CommandResponse {

    public String data;
    public boolean success;
    public String statusMessage;



    public CommandResponse(String data) {
        this(data, true, "");
    }

    public CommandResponse(String data, boolean success) {
        this(data, success, "");
    }

    public CommandResponse(String data, boolean success, String statusMessage) {
        this.data = data;
        this.success = success;
        this.statusMessage = statusMessage;
    }

}
