package uk.co.mholeys.android.openastrotracker_control.ui.connection;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.Set;

import uk.co.mholeys.android.openastrotracker_control.BluetoothSearch;
import uk.co.mholeys.android.openastrotracker_control.MountBluetoothConnectionService;
import uk.co.mholeys.android.openastrotracker_control.R;

public class ConnectionFragment extends Fragment {


    private static final String TAG = "ConnFrag";
    private EConnectionType connectionType = EConnectionType.WIFI;
    BluetoothSearch searcher;
    private DevicesAdapter deviceAdapter;

    enum EConnectionType {
        WIFI, BLUETOOTH, USB, UNKNOWN;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_connection, container, false);

        Spinner connectionTypeSpinner = root.findViewById(R.id.connection_type_spinner);
        ArrayAdapter adapter =
                ArrayAdapter.createFromResource(getContext(), R.array.connection_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        connectionTypeSpinner.setAdapter(adapter);

        final Button searchButton = root.findViewById(R.id.search_btn);
        Button manaualButton = root.findViewById(R.id.manual_btn);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findBluetoothDevices();
            }
        });

        connectionTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setupConnectionSearch(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        RecyclerView devicesRecycler = root.findViewById(R.id.devices_recycler);
        devicesRecycler.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        deviceAdapter = new DevicesAdapter();
        devicesRecycler.setAdapter(deviceAdapter);

        deviceAdapter.setOnItemClickListener(new DevicesAdapter.OnItemClickListener() {
            @Override
            public void onClick(DeviceViewHolder vh, IDevice device) {
                Log.d(TAG, "onClick: Chosen " + device.getName());
                if (device != null && searcher != null) {
                    if (!searcher.hasBluetooth()) return;
                    Log.d(TAG, "onClick: attempting to connect");
                    if (device.getType().equalsIgnoreCase("bluetooth")) {
                        searcher.bluetoothHandler = bluetoothHandler;
                        searcher.connectClient((BluetoothDevice) device.getDevice());
                    } else {
                        Log.d(TAG, "onClick: Unknown device type");
                    }
                }
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (searcher != null) {
            addNewDevices(searcher.obsDevices.getValue());
        }
    }

    private void setupConnectionSearch(int pos) {
        switch (pos) {
            case 1:
                // Bluetooth
                connectionType = EConnectionType.BLUETOOTH;
                break;

            default:
                Toast.makeText(getContext(), "This is not yet supported, sorry", Toast.LENGTH_SHORT).show();
        }
    }

    private void findBluetoothDevices() {
        Log.d(TAG, "findBluetoothDevices: ");
        if (searcher != null) {
            searcher.disconnect();
        } else {
            Log.d(TAG, "findBluetoothDevices: Creating new searcher");
            searcher = new BluetoothSearch();
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            getContext().registerReceiver(searcher.receiver, filter);
        }
        searcher.setup(getActivity());
        Log.d(TAG, "findBluetoothDevices: setup searcher");
        if (searcher.hasBluetooth()) {
            Log.d(TAG, "findBluetoothDevices: discoveringDevices");
            searcher.discoverDevices();
        }
        searcher.obsDevices.observe(this, new Observer<Set<BluetoothDevice>>() {
            @Override
            public void onChanged(Set<BluetoothDevice> devices) {
                addNewDevices(devices);
            }
        });
    }

    private void addNewDevices(Set<BluetoothDevice> devices) {
        Set<IDevice> newDevices = new HashSet<>();
        for (final BluetoothDevice d : devices) {
            IDevice id = new IDevice() {
                @Override
                public String getName() {
                    return d.getName();
                }

                @Override
                public String getAddress() {
                    return d.getAddress();
                }

                @Override
                public String getType() {
                    return "Bluetooth";
                }

                @Override
                public Object getDevice() {
                    return d;
                }
            };
            Log.d(TAG,"Found "+d.getName()+" "+d.getAddress()+" "+d.getBluetoothClass().getDeviceClass());
            newDevices.add(id);
        }
        deviceAdapter.updateSet(newDevices);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BluetoothSearch.REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    // Start searching/connect
                    searcher.discoverDevices();
                } else {
                    // Handle disabled bluetooth
                    searcher.disableBluetoothUse();
                }
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (searcher != null) {
            getContext().unregisterReceiver(searcher.receiver);
            searcher.disconnect();
        }
//        if (searcher.service != null) {
//            searcher.service.stop();
//        }

    }

    @SuppressLint("HandlerLeak")
    private final Handler bluetoothHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            StringBuilder result = new StringBuilder();
            switch (msg.what) {
                case MountBluetoothConnectionService.MessageConstants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // Parse bluetooth data, or not
                    for (byte bb : writeBuf) {
                        result.append(String.format("%02X", bb));
                    }
                    Log.d(TAG, "handleMessage: Sent " + result.toString());
                    break;
                case MountBluetoothConnectionService.MessageConstants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // Parse bluetooth data
                    for (byte bb : readBuf) {
                        result.append(String.format("%02X", bb));
                    }
                    Log.d(TAG, "handleMessage: received " + result.toString());
                    break;
                case MountBluetoothConnectionService.MessageConstants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString("toast"),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

}