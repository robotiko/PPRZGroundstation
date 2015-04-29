package com.gcs;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ConnectionFragment extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.connect, container, false);
		return view;
	}

	public void setText(String item) {
		TextView textView = (TextView) getView().findViewById(R.id.connectButton);
	    textView.setText(item);
	}
}