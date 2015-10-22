package com.gcs.fragments;

import com.gcs.R;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class PerformanceScoreFragment extends Fragment {
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.performance_score, container, false);
	}

	public void setScore(double performanceScore) {

		TextView textView = (TextView) getView().findViewById(R.id.performanceScoreValue);
	    textView.setText(String.format("%.1f", performanceScore));

		//Show the score in red if it is lower than 50
		if(performanceScore<0) {
			textView.setTextColor(Color.RED);
		} else {
			textView.setTextColor(Color.WHITE);
		}
	}
}