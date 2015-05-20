package com.gcs.fragments;

import com.gcs.MainActivity;
import com.gcs.R;

import android.content.ClipData;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.DragEvent;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AltitudeTape extends Fragment {
	
	private RelativeLayout relativelayout;
	private View rootView;
	
	private boolean labelCreated = false;
	private boolean targetCreated = false;
	
	/* TODO Determine altitude label location based on the height of the bar and the the dynamic vertical range of the drones (flight ceiling - ground level) */
	private int groundLevelTape   = 848; //0 meter
	private int flightCeilingTape = 35; //20 m
	private double flightCeiling  = 20; //[m]
	private double MSA 			  = 0;//[m]
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
		
        // Inflate the layout for this fragment
		rootView = inflater.inflate(R.layout.altitude_tape, container, false);
        
        return rootView;    
    }
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ImageView altitudeTape = (ImageView) getView().findViewById(R.id.altitudeTapeView);

        //OnCLickListener on the altitude tape
        altitudeTape.setOnClickListener(new View.OnClickListener() {
        	@Override
            public void onClick(View v) {
            	Log.d("test","altitude tape clicked!!");
            }
        });
        
        relativelayout = (RelativeLayout) rootView.findViewById(R.id.relativelayout);
    }
	
	//OnCLickListener for the altitude labels
	View.OnClickListener onLabelClick(final View tv) {
	    return new View.OnClickListener() {
	        public void onClick(View v) {

                boolean isSelected = ((MainActivity)getActivity()).isAircraftIconSelected();

                if(isSelected) {    //Deselect
                    ((MainActivity)getActivity()).setIsSelected(false);
                } else {            //Select
                    ((MainActivity)getActivity()).setIsSelected(true);
                }

	        	/* TODO implement code for selection of aircraft when multiple are available (colors etc.) */
//	        	switch (v.getId()){
//	        	case 1:
//	        		Log.d("Test", "Click dynamic label!!");
//	        		break;
//	        	case 2:
//	        		Log.d("Test", "Click static label!!");
//	        		break;
//	        	}
        	}
	    };
	}
	
	//OnLongClickListener for the altitude labels
	View.OnLongClickListener onLabelLongClick(final View tv) {
		return new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				
	            ClipData data = ClipData.newPlainText("","");
	            View.DragShadowBuilder myShadow = new DragShadowBuilder(tv);
	            /* TODO Hide dragshadow and show custom indicator on altitude tape */
	            /* TODO Offset the label that is dragged to be able to see it */
	            
	            // Starts the drag
	            v.startDrag(data,  		// the data to be dragged
	                        myShadow,  	// the drag shadow builder
	                        null,      	// no need to use local data
	                        0          	// flags (not currently used, set to 0)
	            );
				return true;
			}
		};
		
	}

    class MyDragListener implements View.OnDragListener {

        public boolean onDrag(View v, DragEvent event) {
        	v = rootView;
            switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                break;
            case DragEvent.ACTION_DRAG_LOCATION :
            	Log.d("Current y-location",String.valueOf(event.getY()));
                break;
            case DragEvent.ACTION_DROP:
            	//Send the drop location to the method that implements the command
            	setTargetAltitude(event.getY());
            	break;
            default:
                break;
            }
            return true;
        }
    }
	
    //Method to draw aircraft labels on the altitude tape
	public void setLabel(double altitude, int labelId, String labelCharacter, boolean isAircraftIconSelected){

        int backgroundImg;

        if(isAircraftIconSelected) {
            /* TODO add yellow label for selection */
            backgroundImg = R.drawable.altitude_label_small_gray;
        } else {
            backgroundImg =  R.drawable.altitude_label_small_blue;
        }
		
		/* TODO change the horizontal location of the altitude labels and flip them around */
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(80, 60);
        params.leftMargin = 50;
        params.topMargin = altitudeToLabelLocation(altitude);

        TextView label;
		if(!labelCreated){
	        label = new TextView(getActivity());
            label.setId(labelId);
            label.setBackgroundResource(backgroundImg);

	        /* On the basis of the label number (first label is generated to be 1) a string is
	        made to display on the altitude label */
//	        String labelCharacter = String.valueOf((char)(64+labelId));
	        label.setText("      "+ labelCharacter);
	        label.setTypeface(null, Typeface.BOLD);
	        label.setGravity(Gravity.CENTER_VERTICAL);
	        label.setOnClickListener(onLabelClick(label));
	        label.setOnLongClickListener(onLabelLongClick(label));
	        rootView.setOnDragListener(new MyDragListener());
	        relativelayout.addView(label,params);
	        labelCreated = true;
		} else {
			label = (TextView)  getView().findViewById(labelId);
            label.setBackgroundResource(backgroundImg);
			relativelayout.updateViewLayout(label, params);
		}
	}

    //Method to draw the target altitude on the altitude tape
	public void setTargetLabel(double targetAltitude, int targetLabelId) {
		
		/* TODO make a better indicating icon/bug for the target altitude */
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(80, 50);
        params.leftMargin = 50;
        params.topMargin = altitudeToLabelLocation(targetAltitude);

		View target;
		if(!targetCreated) {
			target = new View(getActivity());
			target.setBackgroundResource(R.drawable.altitude_label_small_red);
			target.setId(targetLabelId);
			target.setVisibility(View.VISIBLE);
			relativelayout.addView(target,params);
			targetCreated = true;
		} else {
			target = (View) getView().findViewById(targetLabelId);
			target.setVisibility(View.VISIBLE);
			relativelayout.updateViewLayout(target,params);
		}
	}
	
	//Method to remove the target label from the altitude tape
	public void deleteTargetLabel(int targetLabelId) {
		View targetLabel = (View) getView().findViewById(targetLabelId);

		if(targetLabel!=null) {
			targetLabel.setVisibility(View.GONE);
		}
	}
	
	//Convert altitude to a label location on the tape
	private int altitudeToLabelLocation(double altitude) {
		
		int lengthBar = groundLevelTape - flightCeilingTape;
		double verticalRange = flightCeiling - MSA; 	
		int labelLocation = (int) (groundLevelTape-((altitude/verticalRange)*lengthBar));
		
		return labelLocation;
	}
	
	//Convert label location on the tape to an altitude 
	private double labelLocationToAltitude(float labelLocation) {
		
		int lengthBar = groundLevelTape - flightCeilingTape;
		double verticalRange = flightCeiling - MSA;
		double altitude = verticalRange*((double) groundLevelTape-labelLocation)/lengthBar;
		
		return altitude;
	}

	//Set the target altitude to the service
	private void setTargetAltitude(float dropLocation) {
		
		double dropAltitude = labelLocationToAltitude(dropLocation);

		//If the label is dropped outside the altitude tape, set the target altitude at the bounds.
		if (dropAltitude < MSA) {
			dropAltitude = MSA;
		} else if (dropAltitude > flightCeiling) {
			dropAltitude = flightCeiling;
		}
		
		/* TODO Set the target altitude to the service once this function is available */
//		setTargetLabel(dropAltitude, 10); //Temporary setfunction to show a label
	}
}