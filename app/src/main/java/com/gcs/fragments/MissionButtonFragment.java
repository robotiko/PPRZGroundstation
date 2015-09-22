package com.gcs.fragments;

import com.gcs.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class MissionButtonFragment extends Fragment {

    private static final String TAG = MissionButtonFragment.class.getName();

    private Button homeButton, landButton, takeOffButton, loadWaypointsButton, loadBlocksButton;

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
		
		// Inflate the layout for this fragment
        return inflater.inflate(R.layout.mission_buttons, container, false);
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Create handles for the buttons
        homeButton          = (Button) getView().findViewById(R.id.goHomeButton);
        landButton          = (Button) getView().findViewById(R.id.landButton);
        takeOffButton       = (Button) getView().findViewById(R.id.takeOffButton);
        loadWaypointsButton = (Button) getView().findViewById(R.id.loadWaypointsButton);
        loadBlocksButton    = (Button) getView().findViewById(R.id.loadBlocksButton);
    }

	public void onLandRequest(View v) {
        //Nothing
    }
	
	public void onTakeOffRequest(View v) {
        //Nothing
	}

	public void onGoHomeRequest(View v) {
        //Nothing
	}


	public void onWaypointsRequest(View v) {
        //Nothing
    }

    public void onBlocksRequest(View v) {
        //Nothing
    }

    public void updateWaypointsButton(boolean allWpsLoaded) {
        if(allWpsLoaded) {
            loadWaypointsButton.setBackgroundResource(R.drawable.wp_button_green);
        } else {
            loadWaypointsButton.setBackgroundResource(R.drawable.wp_button_blackwhite);
        }
    }

    public void updateBlocksButton(boolean allBlocksLoaded) {
        if(allBlocksLoaded) {
            loadBlocksButton.setBackgroundResource(R.drawable.blocks_button_green);
        } else {
            loadBlocksButton.setBackgroundResource(R.drawable.blocks_button_blackwhite);
        }
    }

    public void deactivateButtons() {
        loadWaypointsButton.setBackgroundResource(R.drawable.wp_button_blackwhite);
        loadBlocksButton.setBackgroundResource(R.drawable.blocks_button_blackwhite);
    }

    public void updateExecutedMissionButton(String currentBlock) {
        switch (currentBlock) {
            case "Takeoff":
                takeOffButton.setBackgroundResource(R.drawable.take_off_button_active);
                homeButton.setBackgroundResource(R.drawable.home_button_inactive);
                landButton.setBackgroundResource(R.drawable.land_button_inactive);
                break;
            case "land":
                landButton.setBackgroundResource(R.drawable.land_button_active);
                homeButton.setBackgroundResource(R.drawable.home_button_inactive);
                takeOffButton.setBackgroundResource(R.drawable.take_off_button_inactive);
                break;
            case "HOME":
                homeButton.setBackgroundResource(R.drawable.home_button_active);
                takeOffButton.setBackgroundResource(R.drawable.take_off_button_inactive);
                landButton.setBackgroundResource(R.drawable.land_button_inactive);
                break;
            default:
                landButton.setBackgroundResource(R.drawable.land_button_inactive);
                takeOffButton.setBackgroundResource(R.drawable.take_off_button_inactive);
                homeButton.setBackgroundResource(R.drawable.home_button_inactive);
                break;
        }
    }
}