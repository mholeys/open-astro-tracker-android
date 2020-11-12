package uk.co.mholeys.android.openastrotracker_control;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

public class PolarAlignmentDialogFragment extends DialogFragment {

    //    return inflater.inflate(R.layout.fragment_polar_alignment, container, false);

    PolarAlignListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.at_home_question)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onAlign(getDialog());
                    }
                })
                .setNeutralButton(R.string.go_home, null) // See onResume
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
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

    @Override
    public void onResume() {
        super.onResume();

        AlertDialog alertDialog = (AlertDialog) getDialog();
        Button homeButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onHome(getDialog());
            }
        });

    }

    public void setPolarAlignmentListener(PolarAlignListener listener) {
        this.listener = listener;
    }

    public interface PolarAlignListener {
        void onHome(Dialog dialog);
        void onAlign(Dialog dialog);
        void onCancel(Dialog dialog);
    }

}