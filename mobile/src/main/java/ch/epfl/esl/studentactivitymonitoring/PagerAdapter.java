package ch.epfl.esl.studentactivitymonitoring;

/**
 * Created by aminmekacher on 04.03.18.
 */

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class PagerAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs;
    public PagerAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new ch.epfl.esl.studentactivitymonitoring.BluetoothReconnect();
            case 1:
                return new ch.epfl.esl.studentactivitymonitoring.BluetoothConnect();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}

