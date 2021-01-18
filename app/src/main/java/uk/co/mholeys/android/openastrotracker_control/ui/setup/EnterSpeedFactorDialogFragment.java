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

public class EnterSpeedFactorDialogFragment extends DialogFragment {

    private Listener listener;
    private EditText mSpeedFactorText;
    private float lastSpeed;

    public EnterSpeedFactorDialogFragment(float lastSpeed) {
        this.lastSpeed = lastSpeed;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View root = getLayoutInflater().inflate(R.layout.speed_factor_alert, null);
        mSpeedFactorText = root.findViewById(R.id.editSpeedFactor);

        mSpeedFactorText.setText(String.valueOf(lastSpeed));

        builder.setView(root);

        builder
                .setMessage(R.string.set_location)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onSet(getDialog(), mSpeedFactorText.getText());
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

    public void setListener(EnterSpeedFactorDialogFragment.Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onSet(Dialog dialog, Editable speedFactor);
        void onCancel(Dialog dialog);
    }

}
