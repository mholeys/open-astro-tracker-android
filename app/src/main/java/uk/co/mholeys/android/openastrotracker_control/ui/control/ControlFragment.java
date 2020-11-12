package uk.co.mholeys.android.openastrotracker_control.ui.control;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import uk.co.mholeys.android.openastrotracker_control.PolarAlignmentDialogFragment;
import uk.co.mholeys.android.openastrotracker_control.PolarAlignmentDialogFragment2;
import uk.co.mholeys.android.openastrotracker_control.R;

public class ControlFragment extends Fragment {

    Button mStopTrackingButton;
    Button mStartTrackingButton;
    TextView mTrackingStateView;

    Button mMoveUpButton;
    Button mMoveLeftButton;
    Button mMoveDownButton;
    Button mMoveRightButton;

    Button mHomeButton;
    Button mSetHomeButton;
    Button mSlewToButton;
    Button mPolarAlignButton;

    TextView mCurrentRAText;
    TextView mCurrentDECText;
    private PolarAlignmentDialogFragment.PolarAlignListener polarAlignmentListener = new PolarAlignmentDialogFragment.PolarAlignListener() {

        boolean movedHome = false;

        @Override
        public void onHome(Dialog dialog) {
            // Move to home position
            // TODO:
            Toast.makeText(getContext(), R.string.moving_to_home_wait, Toast.LENGTH_LONG).show();
            movedHome = true;
            // Tell user to wait for homing
            AlertDialog alertDialog = (AlertDialog) dialog;
            Button homeButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
            homeButton.setEnabled(false);
            homeButton.setText(R.string.homing);
            alertDialog.setTitle(R.string.wait_for_home_then_align);
        }

        @Override
        public void onAlign(Dialog dialog) {
            // Move to polar position
            // TODO:
            // Show next dialog
            dialog.dismiss();
            PolarAlignmentDialogFragment2 secondDialog = new PolarAlignmentDialogFragment2();
            secondDialog.show(getParentFragmentManager(), "PolarAlignmentDialogFragment2");
            secondDialog.setPolarAlignmentListener(polarAlignmentListener2);
            reset();
        }

        @Override
        public void onCancel(Dialog dialog) {
            // User has cancelled the alignment, probably due to home not being set
            // If they have clicked move home followed by cancel the home position may be unset
            if (movedHome) {
                Toast.makeText(getContext(), R.string.home_may_be_unset, Toast.LENGTH_LONG).show();
            }
            dialog.dismiss();
            reset();
        }

        public void reset() {
            movedHome = false;
        }

    };

    private PolarAlignmentDialogFragment2.PolarAlignListener polarAlignmentListener2 = new PolarAlignmentDialogFragment2.PolarAlignListener() {

        @Override
        public void onDone(Dialog dialog) {
            // User has stated that polaris is now in the center of the screen
            // TODO:
        }

        @Override
        public void onCancel(Dialog dialog) {
            Toast.makeText(getContext(), R.string.home_may_be_unset, Toast.LENGTH_LONG).show();
            dialog.dismiss();
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_control, container, false);

        mStopTrackingButton = root.findViewById(R.id.tracking_off_btn);
        mStartTrackingButton = root.findViewById(R.id.tracking_on_btn);
        mTrackingStateView = root.findViewById(R.id.tracking_text);

        mMoveUpButton = root.findViewById(R.id.move_up_btn);
        mMoveLeftButton = root.findViewById(R.id.move_left_btn);
        mMoveDownButton = root.findViewById(R.id.move_down_btn);
        mMoveRightButton = root.findViewById(R.id.move_right_btn);

        mHomeButton = root.findViewById(R.id.home_btn);
        mSetHomeButton = root.findViewById(R.id.set_home_btn);
        mSlewToButton = root.findViewById(R.id.slew_to_btn);
        mPolarAlignButton = root.findViewById(R.id.polar_align_btn);

        mCurrentRAText = root.findViewById(R.id.current_ra_text);
        mCurrentDECText = root.findViewById(R.id.current_dec_text);

        mPolarAlignButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PolarAlignmentDialogFragment dialog = new PolarAlignmentDialogFragment();
                dialog.show(getParentFragmentManager(), "PolarAlignmentDialogFragment");
                dialog.setPolarAlignmentListener(polarAlignmentListener);
            }
        });

        return root;
    }

}