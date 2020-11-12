package uk.co.mholeys.android.openastrotracker_control.ui.poi;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import uk.co.mholeys.android.openastrotracker_control.R;

public class PointsOfInterestFragment extends Fragment {

    TextView titleTextView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_poi, container, false);

//        titleTextView = root.findViewById(R.id.polar_align_text_view);


        return root;
    }
}