package com.gcs.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gcs.R;

public class ScenarioEndFragment extends Fragment {
    private LinearLayout linearLayout;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        linearLayout = (LinearLayout) inflater.inflate(R.layout.scenario_end, container, false);
		return linearLayout;
	}

    public void setVisibility(int visibility) {
        linearLayout.setVisibility(visibility);
    }

    public void setEndScore(double score) {
        TextView textview = (TextView) getView().findViewById(R.id.endScore);
        textview.setText("Average score: " + String.format("%.1f",score));
    }
}