package com.gcs.fragments;

import com.gcs.MainActivity;
import com.gcs.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class MissionButtonFragment extends Fragment {
	
	private View rootView;
    private Button homeButton, landButton, takeOffButton;

    private enum ButtonName {
        HOME, LAND, TAKEOFF
    }

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
		
		// Inflate the layout for this fragment
		rootView = inflater.inflate(R.layout.mission_buttons, container, false);

        return rootView;
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Create handles for the buttons
        homeButton    = (Button) getView().findViewById(R.id.goHomeButton);
        landButton    = (Button) getView().findViewById(R.id.landButton);
        takeOffButton = (Button) getView().findViewById(R.id.takeOffButton);
    }
	
	/* TODO implement actions for clicks on the mission buttons */
	/* TODO make custom buttons to replace the current text buttons, if clicked color changes */
	
	public void onLandRequest(View v) {

        //Set button to active
        setButtonAppearance(true, ButtonName.LAND);
    }
	
	public void onTakeOffRequest(View v) {

        //Set button to active
        setButtonAppearance(true, ButtonName.TAKEOFF);
	}

	public void onGoHomeRequest(View v) {

		//Set button to active
        setButtonAppearance(true, ButtonName.HOME);
	}

	public void onWaypointRequest(View v) {

        Log.d("COMMAND", "Update waypoints");
    }

    // Method to change button appearance (active/inactive)
    public void setButtonAppearance(Boolean active, ButtonName buttonName) {

        switch (buttonName) {
            case HOME:
                if(active) {
                    homeButton.setBackgroundResource(R.drawable.home_button_active);
                } else {
                    homeButton.setBackgroundResource(R.drawable.home_button_inactive);
                }
                break;
            case LAND:
                if(active) {
                    landButton.setBackgroundResource(R.drawable.home_button_active);
                } else {
                    landButton.setBackgroundResource(R.drawable.home_button_inactive);
                }
                break;
            case TAKEOFF:
                if(active) {
                    takeOffButton.setBackgroundResource(R.drawable.home_button_active);
                } else {
                    takeOffButton.setBackgroundResource(R.drawable.home_button_inactive);
                }
                break;
        }
    }
}