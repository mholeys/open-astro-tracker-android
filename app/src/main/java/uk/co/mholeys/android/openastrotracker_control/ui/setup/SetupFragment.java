package uk.co.mholeys.android.openastrotracker_control.ui.setup;

import android.os.Bundle;
import android.os.Messenger;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import uk.co.mholeys.android.openastrotracker_control.MountViewModel;
import uk.co.mholeys.android.openastrotracker_control.R;
import uk.co.mholeys.android.openastrotracker_control.mount.MountMessages;

public class SetupFragment extends Fragment {

    TextView titleTextView;
    private MountViewModel mountViewModel;
    private Messenger serviceMessenger;

    private TextView mLongitudeText;
    private TextView mLatitudeText;
    private Button mManualLocationButton;
    private Button mAutoLocationButton;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_setup, container, false);

        mManualLocationButton = root.findViewById(R.id.set_location_manual_btn);
        mManualLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        mAutoLocationButton = root.findViewById(R.id.set_location_gps_btn);
        mAutoLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                MountMessages.setLocation();
            }
        });

        mLongitudeText = root.findViewById(R.id.site_lon_text);
        mLatitudeText = root.findViewById(R.id.site_lat_text);

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mountViewModel = MountViewModel.getInstance(this);
        this.serviceMessenger = mountViewModel.getMessenger();

        mountViewModel.getSiteLatitude().observe(getViewLifecycleOwner(), new Observer<Float>() {
            @Override
            public void onChanged(Float latitude) {
                mLatitudeText.setText(String.valueOf(latitude));
            }
        });
        mountViewModel.getSiteLongitude().observe(getViewLifecycleOwner(), new Observer<Float>() {
            @Override
            public void onChanged(Float longitude) {
                mLongitudeText.setText(String.valueOf(longitude));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();


    }
}