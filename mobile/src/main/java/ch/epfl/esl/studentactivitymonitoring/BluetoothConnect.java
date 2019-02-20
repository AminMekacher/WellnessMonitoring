package ch.epfl.esl.studentactivitymonitoring;

/**
 * Created by aminmekacher on 04.03.18.
 */

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ch.epfl.esl.studentactivitymonitoring.R;


/**
 * A simple {@link Fragment} subclass.
 */
public class BluetoothConnect extends Fragment {


    public BluetoothConnect() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.bluetooth_connect, container, false);
    }

}

