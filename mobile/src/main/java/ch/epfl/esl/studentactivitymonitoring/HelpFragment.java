package ch.epfl.esl.studentactivitymonitoring;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

/**
 * Created by Lara on 3/9/2018.
 * Dialog displaying help messages
 */

public class HelpFragment extends DialogFragment {
    private static final String TAG = "NotificationDialogFragment";

    private static String mMessageToDisplay;

    public static HelpFragment newInstance(
            String message) {

        HelpFragment infoDialog = new HelpFragment();
        mMessageToDisplay = message;
        return infoDialog;

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setMessage(mMessageToDisplay);
        return alertDialog.create();
    }

}
