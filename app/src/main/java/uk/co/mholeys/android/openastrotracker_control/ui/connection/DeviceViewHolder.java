package uk.co.mholeys.android.openastrotracker_control.ui.connection;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import uk.co.mholeys.android.openastrotracker_control.R;

public class DeviceViewHolder extends RecyclerView.ViewHolder {

    TextView mNameView;
    TextView mAddressView;

    IDevice device;

    public DeviceViewHolder(@NonNull View view) {
        super(view);

        mNameView = view.findViewById(R.id.device_name_view);
        mAddressView = view.findViewById(R.id.device_address_view);
    }

    public void setup(IDevice device) {
        this.device = device;
        mNameView.setText(device.getName());
        mAddressView.setText(device.getAddress());
    }
}
