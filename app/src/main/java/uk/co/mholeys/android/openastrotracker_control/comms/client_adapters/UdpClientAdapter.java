//package uk.co.mholeys.android.openastrotracker_control.comms.client_adapters;
//
//import android.util.Xml;
//
//public class UdpClientAdapter implements AutoCloseable {
//
//    // TODO listener
//    public event EventHandler<ClientFoundEventArgs> ClientFound;
//
//    private final String hostToFind;
//    private int port;
//    private UdpClient udpClient;
//    private boolean disposedValue;
//
//    public UdpClientAdapter(String hostToFind, int port)
//    {
//        this.hostToFind = hostToFind;
//        this.port = port;
//        // create a udp client to listen for requests
//        udpClient = new UdpClient();
//    }
//
//    public void StartClientSearch()
//    {
//        udpClient = new UdpClient();
//        udpClient.Client.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);
//        // listen for computers other than localhost
//        udpClient.ExclusiveAddressUse = false;
//
//        // IPAddress.Any allows us to listen to broadcast UDP packets
//        udpClient.Client.Bind(new IPEndPoint(IPAddress.Any, port));
//
//        var data = Xml.Encoding.ASCII.GetBytes($"skyfi:{_hostToFind}?");
//        udpClient.Send(data, data.Length, new IPEndPoint(IPAddress.Broadcast, port));
//        udpClient.BeginReceive(new AsyncCallback(this.ReceiveCallback), null);
//    }
//
//    private void ReceiveCallback(IAsyncResult ar)
//    {
//        IPEndPoint remoteIpEndPoint = new IPEndPoint(IPAddress.Any, port);
//        byte[] received = udpClient.EndReceive(ar, ref remoteIpEndPoint);
//        string result = Encoding.UTF8.GetString(received);
//        var parts = result.Split(":@".ToCharArray(),StringSplitOptions.RemoveEmptyEntries);
//        if ((parts.Length == 3) && parts[0].Equals("skyfi"))
//        {
//            OnClientFound(new ClientFoundEventArgs(parts[1], IPAddress.Parse(parts[2])));
//        }
//        udpClient.BeginReceive(new AsyncCallback(this.ReceiveCallback), null);
//    }
//
//    public string SendCommand(string command)
//    {
//        throw new NotImplementedException();
//    }
//
//    protected virtual void OnClientFound(ClientFoundEventArgs e)
//    {
//        var handler = ClientFound;
//        handler?.Invoke(this, e);
//    }
//
//    public class ClientFoundEventArgs : EventArgs
//    {
//			public ClientFoundEventArgs(string name, IPAddress ip)
//        {
//            Name = name;
//            Address = ip;
//        }
//
//        public String Name { get; set; }
//        public IPAddress Address { get; set; }
//    }
//
//    @Override
//    public void close() throws Exception {
//        if (!disposedValue)
//        {
//            if (disposing)
//            {
//                udpClient ?.Dispose();
//            }
//
//            // TODO: free unmanaged resources (unmanaged objects) and override finalizer
//            // TODO: set large fields to null
//            disposedValue = true;
//        }
//    }
//
//}
