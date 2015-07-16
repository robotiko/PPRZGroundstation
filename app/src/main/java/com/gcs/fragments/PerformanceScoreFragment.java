package com.gcs.fragments;

import com.gcs.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class PerformanceScoreFragment extends Fragment {
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.performance_score, container, false);
		return view;
	}

	public void setText(String item) {
		TextView textView = (TextView) getView().findViewById(R.id.performanceScoreValue);
	    textView.setText(item);
	}
}