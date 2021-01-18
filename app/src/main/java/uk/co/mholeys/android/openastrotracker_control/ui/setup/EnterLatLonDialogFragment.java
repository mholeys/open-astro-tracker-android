package uk.co.mholeys.android.openastrotracker_control.ui.setup;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import uk.co.mholeys.android.openastrotracker_control.R;

public class EnterLatLonDialogFragment extends DialogFragment {

    private Listener listener;
    private EditText mLatText;
    private EditText mLonText;
    private float lastLat;
    private float lastLon;

    public EnterLatLonDialogFragment(float lastLat, float lastLon) {
        this.lastLat = lastLat;
        this.lastLon = lastLon;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View root = getLayoutInflater().inflate(R.layout.location_alert, null);
        mLatText = root.findViewById(R.id.editSpeedFactor);
        mLonText = root.findViewById(R.id.editLongitude);
        mLatText.setText(String.valueOf(lastLat));
        mLonText.setText(String.valueOf(lastLon));

        builder.setView(root);

        builder
                .setMessage(R.string.set_location)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onSet(getDialog(), mLatText.getText(), mLonText.getText());
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        listener.onCancel(getDialog());
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                listener.onCancel(getDialog());
            }
        });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onResume() {
        super.onResume();
        AlertDialog alertDialog = (AlertDialog) getDialog();
    }

    public void setListener(EnterLatLonDialogFragment.Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onSet(Dialog dialog, Editable lat, Editable lon);
        void onCancel(Dialog dialog);
    }

}
