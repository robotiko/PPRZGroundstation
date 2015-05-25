package com.gcs.fragments;

import com.gcs.MainActivity;
import com.gcs.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MissionButtonFragment extends Fragment {
	
	private View rootView;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
		
		// Inflate the layout for this fragment
		rootView = inflater.inflate(R.layout.mission_buttons, container, false);
        return rootView; 
	}
	
	/* TODO implement actions for clicks on the mission buttons */
	/* TODO make custom buttons to replace the current textbuttons, if clicked color changes */
	
	public void onLandRequest(View v) {
        Log.d("COMMAND","LAND");
    }
	
	public void onTakeOffRequest(View v) {
		Log.d("COMMAND","TAKE-OFF");
	}

	public void onGoHomeRequest(View v) {
		Log.d("COMMAND","GO HOME");
	}
}
