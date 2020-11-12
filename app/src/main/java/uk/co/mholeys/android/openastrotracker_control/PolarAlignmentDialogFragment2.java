package uk.co.mholeys.android.openastrotracker_control;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class PolarAlignmentDialogFragment2 extends DialogFragment {

    //    return inflater.inflate(R.layout.fragment_polar_alignment, container, false);

    PolarAlignmentDialogFragment2.PolarAlignListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.move_mount_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onDone(getDialog());
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        listener.onCancel(getDialog());
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                listener.onCancel(getDialog());
            }
        });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    public void setPolarAlignmentListener(PolarAlignmentDialogFragment2.PolarAlignListener listener) {
        this.listener = listener;
    }

    public interface PolarAlignListener {
        void onDone(Dialog dialog);
        void onCancel(Dialog dialog);
    }

}
