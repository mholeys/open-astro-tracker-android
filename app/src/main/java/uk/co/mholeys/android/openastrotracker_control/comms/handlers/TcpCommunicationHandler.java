package uk.co.mholeys.android.openastrotracker_control.comms.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;

import uk.co.mholeys.android.openastrotracker_control.comms.ICommunicationHandler;
import uk.co.mholeys.android.openastrotracker_control.comms.Log;

public class TcpCommunicationHandler implements ICommunicationHandler {

    private InetAddress _ip;
    private int _port;
    private Socket _client;

    public TcpCommunicationHandler(String spec) {
        Log.writeLine(String.format("COMMFACTORY: Creating Wifi handler at %s ...", spec));

        String ip = "";
        String port = "";

        int colon = spec.indexOf(':');
        if (colon > 0)
        {
            try
            {
                ip = spec.substring(0, colon);
                port = spec.substring(colon + 1);

                Log.writeLine(String.format("COMMFACTORY: Wifi handler will monitor at %s:%s ..."), ip, port);

                _ip = Inet4Address.getByName(ip);
                _port = Integer.valueOf(port);
                _client = new Socket();
            }
            catch (Exception ex)
            {
                Log.writeLine(String.format("COMMFACTORY: Failed to create TCP client. %s", ex.getMessage()));
            }
        }
    }

    public TcpCommunicationHandler(Inet4Address ip, int port)
    {
        _ip = ip;
        _port = port;
        _client = new Socket();
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

    private CommandResponse sendCommand(String command, ResponseType needsResponse)
    {
        if (_client == null)
        {
            Log.writeLine(String.format("TCP: Configuration error, IP [%s] or port [%s] is invalid.", _ip, _port));
            return new CommandResponse("", false, String.format("Configuration error, IP [%s] or port [%s] is invalid.", _ip, _port));
        }

        int attempt = 1;
        String respString = "";

        while ((attempt < 4) && (_client != null))
        {
            Log.writeLine("TCP: [%s] Attempt %s to send command.", command, attempt);
            if (!_client.isConnected())
            {
                try
                {
                    _client = new Socket(_ip, _port);
                    _client.setSoTimeout(500);
                }
                catch (Exception e)
                {
                    Log.writeLine("TCP: [%s] Failed To connect or create client for command: %s", command, e.getMessage());
                    return new CommandResponse("", false, String.format("Failed To Connect to Client: %s", e.getMessage()));
                }
            }

            String error = "";


            byte[] bytes = command.getBytes();
            try  {
                OutputStream out = _client.getOutputStream();
                out.write(bytes, 0, bytes.length);
                Log.writeLine("TCP: [%s] Sent command!", command);
            }
            catch (Exception e)
            {
                Log.writeLine("TCP: [%s] Unable to write command to stream: %s", command, e.getMessage());
                return new CommandResponse("", false, String.format("Failed to send message: %s", e.getMessage()));
            }

            try
            {
                InputStream in = _client.getInputStream();
                switch (needsResponse)
                {
                    case NoResponse:
                        attempt = 10;
                        Log.writeLine("TCP: [%s] No reply needed to command", command);
                        break;

                    case DigitResponse:
                    case FullResponse:
                    {
                        Log.writeLine("TCP: [%s] Expecting a reply needed to command, waiting...", command);
                        byte[] response = new byte[256];
                        int respCount = in.read(response, 0, response.length);
                        if (respCount < 256) {
                            response[respCount] = 0;
                        }
                        respString = new String(response);
                        respString = respString.substring(0, respString.lastIndexOf('#'));
                        Log.writeLine("TCP: [%s] Received reply to command -> [%s]", command, respString);
                        attempt = 10;
                    }
                    break;
                }
            }
            catch (Exception e)
            {
                Log.writeLine("TCP: [%s] Failed to read reply to command. %s thrown", command, e.getClass().getSimpleName());
                if (needsResponse != ResponseType.NoResponse)
                {
                    respString = "0#";
                }
            }
            attempt++;
        }

        return new CommandResponse(respString);
    }


    @Override
    public boolean connected() {
        return _client != null && _client.isConnected();
    }

    @Override
    public void disconnect() {
        if (connected()) {
            Log.writeLine("TCP: Closing port.");
            try {
                _client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            _client = null;
        }
    }
}
