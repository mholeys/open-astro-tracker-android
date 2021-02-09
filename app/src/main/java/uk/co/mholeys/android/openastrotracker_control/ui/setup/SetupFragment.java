package uk.co.mholeys.android.openastrotracker_control.ui.setup;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Messenger;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import uk.co.mholeys.android.openastrotracker_control.MountViewModel;
import uk.co.mholeys.android.openastrotracker_control.R;
import uk.co.mholeys.android.openastrotracker_control.mount.Mount;
import uk.co.mholeys.android.openastrotracker_control.mount.MountMessages;

public class SetupFragment extends Fragment {

    TextView titleTextView;
    private MountViewModel mountViewModel;
    private Messenger serviceMessenger;

    private TextView mLongitudeText;
    private TextView mLatitudeText;
    private TextView mSpeedFactorText;
    private TextView mRaStepsText;
    private TextView mDecStepsText;

    private Button mSpeedFactorButton;
    private Button mManualLocationButton;
    private Button mAutoLocationButton;
    private float lastLat, lastLon, lastSpeedFactor;

    private EnterLatLonDialogFragment.Listener manualLocationListener = new EnterLatLonDialogFragment.Listener() {
        @Override
        public void onSet(Dialog dialog, Editable latS, Editable lonS) {
            try {
                float lat = Float.parseFloat(latS.toString());
                float lon = Float.parseFloat(lonS.toString());

                MountMessages.setLocation(serviceMessenger, lat, lon, 0, 0);

                MountMessages.getSiteLatitude(serviceMessenger);
                MountMessages.getSiteLongitude(serviceMessenger);
            } catch (NumberFormatException e) {
                // TODO: handle bad format
            }
        }

        @Override
        public void onCancel(Dialog dialog) {
            MountMessages.getSiteLatitude(serviceMessenger);
            MountMessages.getSiteLongitude(serviceMessenger);
        }
    };

    private EnterSpeedFactorDialogFragment.Listener speedFactorListener = new EnterSpeedFactorDialogFragment.Listener() {
        @Override
        public void onSet(Dialog dialog, Editable speedFactorS) {
            try {
                float speedFactor = Float.parseFloat(speedFactorS.toString());
                MountMessages.setSpeedFactor(serviceMessenger, speedFactor);
                MountMessages.getSpeedFactor(serviceMessenger);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCancel(Dialog dialog) {
            MountMessages.getSpeedFactor(serviceMessenger);
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_setup, container, false);

        mManualLocationButton = root.findViewById(R.id.set_location_manual_btn);
        mManualLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EnterLatLonDialogFragment dialog = new EnterLatLonDialogFragment(lastLat, lastLon);
                dialog.setListener(manualLocationListener);
                dialog.show(getParentFragmentManager(), "EnterLatLonDialogFragment");
            }
        });
        mAutoLocationButton = root.findViewById(R.id.set_location_gps_btn);
        mAutoLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Not implemented", Toast.LENGTH_SHORT).show();
//                MountMessages.setLocation();
            }
        });

        mSpeedFactorButton = root.findViewById(R.id.set_speed_factor_btn);
        mSpeedFactorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EnterSpeedFactorDialogFragment dialog = new EnterSpeedFactorDialogFragment(lastSpeedFactor);
                dialog.setListener(speedFactorListener);
                dialog.show(getParentFragmentManager(), "EnterSpeedFactorDialogFragment");
            }
        });

        mLongitudeText = root.findViewById(R.id.site_lon_text);
        mLatitudeText = root.findViewById(R.id.site_lat_text);

        mSpeedFactorText = root.findViewById(R.id.speed_factor_text);
        mRaStepsText = root.findViewById(R.id.ra_steps_text);
        mDecStepsText = root.findViewById(R.id.dec_steps_text);

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
                lastLat = latitude;
            }
        });
        mountViewModel.getSiteLongitude().observe(getViewLifecycleOwner(), new Observer<Float>() {
            @Override
            public void onChanged(Float longitude) {
                mLongitudeText.setText(String.valueOf(longitude));
                lastLon = longitude;
            }
        });
        mountViewModel.getSpeedFactor().observe(getViewLifecycleOwner(), new Observer<Float>() {
            @Override
            public void onChanged(Float speedFactor) {
                mSpeedFactorText.setText(String.valueOf(speedFactor));
                lastSpeedFactor = speedFactor;
            }
        });
        mountViewModel.getRaStepsPerDeg().observe(getViewLifecycleOwner(), new Observer<Float>() {
            @Override
            public void onChanged(Float steps) {
                mRaStepsText.setText(String.valueOf(steps));
            }
        });
        mountViewModel.getDecStepsPerDeg().observe(getViewLifecycleOwner(), new Observer<Float>() {
            @Override
            public void onChanged(Float steps) {
                mDecStepsText.setText(String.valueOf(steps));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mountViewModel.isBound()) {
            MountMessages.getSiteLatitude(serviceMessenger);
            MountMessages.getSiteLongitude(serviceMessenger);
            MountMessages.getSpeedFactor(serviceMessenger);
            MountMessages.getRaStepsPerDeg(serviceMessenger);
            MountMessages.getDecStepsPerDeg(serviceMessenger);
        }
    }
}
