package uk.co.mholeys.android.openastrotracker_control.ui.connection;

import android.os.Bundle;
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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Set;

import uk.co.mholeys.android.openastrotracker_control.ISearcherControl;
import uk.co.mholeys.android.openastrotracker_control.MountViewModel;
import uk.co.mholeys.android.openastrotracker_control.R;

public class ConnectionFragment extends Fragment {

    private static final String TAG = "ConnFrag";

    private DevicesAdapter deviceAdapter;
    private MountViewModel mountViewModel;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mountViewModel = MountViewModel.getInstance(this);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_connection, container, false);

        final Spinner connectionTypeSpinner = root.findViewById(R.id.connection_type_spinner);
        ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(getContext(), R.array.connection_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        connectionTypeSpinner.setAdapter(adapter);
        connectionTypeSpinner.setSelection(1);

        final Button searchButton = root.findViewById(R.id.search_btn);
        Button manualButton = root.findViewById(R.id.manual_btn);

        manualButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Not implemented", Toast.LENGTH_LONG).show();
            }
        });
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findDevices();
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
                connectTo(device);
            }
        });

        return root;
    }
    @Override
    public void onResume() {
        super.onResume();
        if (mountViewModel != null && mountViewModel.getType() != ISearcherControl.EConnectionType.UNKNOWN) {
            addNewDevices(mountViewModel.getDevices().getValue());
        }
    }

    private void connectTo(IDevice device) {
        mountViewModel.connect(device);
    }

    private void setupConnectionSearch(int pos) {
        switch (pos) {
            case 0:
                // Wifi
                mountViewModel.setup(ISearcherControl.EConnectionType.WIFI, getActivity());
                break;
            case 1:
                // Bluetooth
                mountViewModel.setup(ISearcherControl.EConnectionType.BLUETOOTH, getActivity());
                break;
            case 2:
                // USB/Serial?
                mountViewModel.setup(ISearcherControl.EConnectionType.USB, getActivity());
                break;
            default:
                mountViewModel.setup(ISearcherControl.EConnectionType.UNKNOWN, getActivity());
                Toast.makeText(getContext(), "This is not yet supported, sorry", Toast.LENGTH_SHORT).show();
        }
        mountViewModel.getDevices().observe(this, new Observer<Set<IDevice>>() {
            @Override
            public void onChanged(Set<IDevice> devices) {
                addNewDevices(devices);
            }
        });
    }

    private void findDevices() {
        mountViewModel.setup(ISearcherControl.EConnectionType.BLUETOOTH, getActivity());
        mountViewModel.discover();
    }

    private void addNewDevices(Set<IDevice> devices) {
        deviceAdapter.updateSet(devices);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
    }
}