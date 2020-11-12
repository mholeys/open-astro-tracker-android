//package uk.co.mholeys.android.openastrotracker_control.comms.handlers;
//
//import uk.co.mholeys.android.openastrotracker_control.comms.ICommunicationHandler;
//import android.databinding.ObservableList;
//
//import androidx.databinding.ObservableArrayList;
//import androidx.databinding.ObservableList;
//
//public class CommunicationHandlerFactory {
//
//    static ObservableList<String> _available = new ObservableArrayList<String>();
//    public static void DiscoverDevices() {
//        _available.clear();
//        //foreach (var port in SerialPort.GetPortNames())
//        //{
//        //	_available.Add("Serial : " + port);
//        //}
//
//        UdpClientAdapter searcher = new UdpClientAdapter("OAT", 4031);
//        searcher.clientFound += onWifiClientFound;
//        searcher.startClientSearch();
//    }
//
//    private static void onWifiClientFound(Object sender, ClientFoundEventArgs e) {
//        _available.add(0, String.format("WiFi : {0} ({1}:4030)", e.name, e.address));
//    }
//
//    public static ObservableCollection<String> availableDevices {
//        return _available;
//    }
//
//    public static ICommunicationHandler ConnectToDevice(String device) {
//        //if (device.StartsWith("Serial : "))
//        //{
//        //	string comPort = device.Substring("Serial : ".Length);
//        //	return new SerialCommunicationHandler(comPort);
//        //}
//        //else
//        if (device.startsWith("WiFi : "))
//        {
//            String[] parts = device.split("()");
//            String ipAddress = parts[1];
//            return new TcpCommunicationHandler(ipAddress);
//        }
//
//        return null;
//    }
//
//}
