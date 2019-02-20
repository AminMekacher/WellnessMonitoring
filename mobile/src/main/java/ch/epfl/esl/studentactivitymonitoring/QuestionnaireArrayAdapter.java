package ch.epfl.esl.studentactivitymonitoring;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Lara on 2/22/2018.
 */

public class QuestionnaireArrayAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final ArrayList<String> questions;

    //Constructor
    public QuestionnaireArrayAdapter(Context context, ArrayList<String> strings) {
        super(context, R.layout.question_item, strings);
        this.context = context;
        this.questions = strings;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.question_item, parent, false);
        TextView questionTV = (TextView) rowView.findViewById(R.id.question);
        SeekBar answerSeekBar = (SeekBar) rowView.findViewById(R.id.seekBar);
        questionTV.setText(questions.get(position));


        return rowView;
    }
}
