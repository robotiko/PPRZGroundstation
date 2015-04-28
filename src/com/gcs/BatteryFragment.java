package com.gcs;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class BatteryFragment extends Fragment {
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.battery, container, false);
		return view;
	}

	public void setText(String item) {
		TextView textView = (TextView) getView().findViewById(R.id.batteryValue);
	    textView.setText(item);
	}
	
	public void setBackground(Color color) {
		// TODO: Dynamically change the background color
	}
}