package uk.co.mholeys.android.openastrotracker_control.ui.connection;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import uk.co.mholeys.android.openastrotracker_control.R;

public class DevicesAdapter extends RecyclerView.Adapter<DeviceViewHolder> {

    ArrayList<IDevice> devices = new ArrayList<IDevice>();
    private ArrayList<OnItemClickListener> clickListeners = new ArrayList<>();

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);

        return new DeviceViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull final DeviceViewHolder holder, int position) {
        final IDevice d = devices.get(position);
        holder.setup(d);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifyListeners(holder, d);
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void updateSet(Set<IDevice> devices) {
        this.devices.clear();
        this.devices.addAll(devices);
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        clickListeners.add(listener);
    }

    public void removeOnItemClickListener(OnItemClickListener listener) {
        clickListeners.remove(listener);
    }

    public interface OnItemClickListener {
        void onClick(DeviceViewHolder vh, IDevice device);
    }

    private void notifyListeners(DeviceViewHolder vh, IDevice device) {
        for (OnItemClickListener listener : clickListeners) {
            listener.onClick(vh, device);
        }
    }

}
