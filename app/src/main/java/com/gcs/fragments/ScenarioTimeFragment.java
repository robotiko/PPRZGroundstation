package com.gcs.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gcs.R;

public class ScenarioTimeFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.scenario_time, container, false);
	}

	public void setTimeLeft(long timeLeft) {
		TextView textView = (TextView) getView().findViewById(R.id.scenarioTimeValue);
        if(timeLeft < getResources().getInteger(R.integer.orangeTime) && timeLeft >= getResources().getInteger(R.integer.redTime)) {
            textView.setTextColor(getResources().getColor(R.color.orange));
        } else if(timeLeft<getResources().getInteger(R.integer.redTime)) {
            textView.setTextColor(Color.RED);
        } else if (timeLeft<=0){
            textView.setTextColor(Color.YELLOW);
        }
        //Set value to textview
		textView.setText(String.valueOf(timeLeft/60) + ":" + String.format("%02d", timeLeft%60));
	}

    public void setColor(int color) {
        TextView textView = (TextView) getView().findViewById(R.id.scenarioTimeValue);
        textView.setTextColor(color);
    }
}