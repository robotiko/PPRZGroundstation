package com.gcs.fragments;

import com.gcs.MainActivity;
import com.gcs.R;
import com.gcs.core.Aircraft;
import com.gcs.core.ConflictStatus;

import android.app.Activity;
import android.content.ClipData;
import android.graphics.Color;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.concurrent.ConcurrentHashMap;

public class AltitudeTape extends Fragment {
	
	private FrameLayout framelayout;
	private View rootView;

    final ConcurrentHashMap<Integer, Integer> labelList = new ConcurrentHashMap<>();

    private int draggedLabel;
	
	private boolean targetCreated = false;
	
	/* TODO Determine altitude label location based on the height of the bar and the the dynamic vertical range of the drones (flight ceiling - ground level) */
	private final int groundLevelTape   = 900; //0 meter
	private final int flightCeilingTape = 35;  //20 m

    private double flightCeiling;              //[m]
	private double groundLevel;                //[m]

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

        //Only used for on-click listener
        ImageView altitudeTape = (ImageView) getView().findViewById(R.id.altitudeTapeView);

        //OnCLickListener on the altitude tape
        altitudeTape.setOnClickListener(new View.OnClickListener() {
        	@Override
            public void onClick(View v) {
            	Log.d("test","altitude tape clicked!!");
            }
        });

        framelayout = (FrameLayout) rootView.findViewById(R.id.altitudeTapeFragment);

        flightCeiling  = getResources().getInteger(R.integer.flightCeiling);
        groundLevel    = getResources().getInteger(R.integer.groundLevel);
    }
	
	//OnCLickListener for the altitude labels
	View.OnClickListener onLabelClick(final View tv) {
	    return new View.OnClickListener() {
	        public void onClick(View v) {

                int aircraftNumber = labelList.get(v.getId());
                boolean isAircraftSelected = ((MainActivity)getActivity()).isAircraftIconSelected(aircraftNumber);

                if(isAircraftSelected) {    //Deselect
                    ((MainActivity)getActivity()).setIsSelected(aircraftNumber,false);
                } else {                    //Select
                    ((MainActivity)getActivity()).setIsSelected(aircraftNumber,true);
                }

	        	/* TODO implement code for selection of aircraft when multiple are available (colors etc.) */
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
	            /* TODO Offset the label that is dragged to be able to see it */

                //Reference for the ondrag method to know which label is being dragged
                draggedLabel = v.getId();
	            
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

    View.OnDragListener labelDragListener(final View tv) {
        return new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {

                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        break;
                    case DragEvent.ACTION_DRAG_LOCATION :
                        break;
                    case DragEvent.ACTION_DROP:
                        //Send the drop location to the method that implements the command
            	        setTargetAltitude(labelList.get(draggedLabel), event.getY());
                        break;
                    default:
                        break;
                }
            return true;
            }
        };
    }

    //Method to draw aircraft labels on the altitude tape
	public void setLabel(double altitude, int labelId, String labelCharacter, boolean isAircraftIconSelected, boolean labelCreated, int acNumber){

        if(labelList.get(labelId) == null) {
            labelList.put(labelId,acNumber);
        }

        int backgroundImg;

        if(isAircraftIconSelected) {
            backgroundImg = R.drawable.altitude_label_small_yellow_flipped;

        } else {
            ConflictStatus conflictStatus = ((MainActivity) getActivity()).getConflictStatus(acNumber);

            switch (conflictStatus) {
                case BLUE:
                    backgroundImg =  R.drawable.altitude_label_small_blue;
                    break;
                case GRAY:
                    backgroundImg =  R.drawable.altitude_label_small_gray;
                    break;
                case RED:
                    backgroundImg =  R.drawable.altitude_label_small_red;
                    break;
                default:
                    backgroundImg =  R.drawable.altitude_label_small_blue;
            }
        }

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(80, 70);
        params.topMargin = altitudeToLabelLocation(altitude);
        int textGravity;

        //Set alignment of the label based on the selection status
        if(!((MainActivity)getActivity()).isAircraftIconSelected(acNumber)) {
            params.gravity = Gravity.RIGHT;
            textGravity = Gravity.CENTER;
        } else {
            params.gravity = Gravity.LEFT;
            textGravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
        }

		TextView label;
		if(!labelCreated){
            label = new TextView(getActivity());
            label.setId(labelId);
            label.setBackgroundResource(backgroundImg);
            label.setMinWidth(20);
            label.setText("   " + labelCharacter);
	        label.setTypeface(null, Typeface.BOLD);
            label.setGravity(textGravity);
            label.setOnClickListener(onLabelClick(label));
	        label.setOnLongClickListener(onLabelLongClick(label));
	        rootView.setOnDragListener(labelDragListener(label));
	        framelayout.addView(label, params);

			((MainActivity)getActivity()).setIsLabelCreated(true,acNumber);
		} else {
			label = (TextView)  getView().findViewById(labelId);
            label.setBackgroundResource(backgroundImg);
            label.setGravity(textGravity);
            framelayout.updateViewLayout(label, params);
		}
	}

    //Method to draw the target altitude on the altitude tape
	public void setTargetLabel(double targetAltitude, int targetLabelId) {
		
		/* TODO make a better indicating icon/bug for the target altitude */
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(80, 70);
        params.topMargin = altitudeToLabelLocation(targetAltitude);
        params.gravity = Gravity.RIGHT;

		View target;
		if(!targetCreated) {
			target = new View(getActivity());
			target.setBackgroundResource(R.drawable.altitude_label_small_red);
            target.setId(targetLabelId);
            target.setVisibility(View.VISIBLE);
            framelayout.addView(target,params);
			targetCreated = true;
		} else {
			target = getView().findViewById(targetLabelId);
			target.setVisibility(View.VISIBLE);
            framelayout.updateViewLayout(target,params);
		}
	}
	
	//Method to remove the target label from the altitude tape
	public void deleteTargetLabel(int targetLabelId) {
		View targetLabel = getView().findViewById(targetLabelId);

		if(targetLabel!=null) {
			targetLabel.setVisibility(View.GONE);
		}
	}
	
	//Convert altitude to a label location on the tape
	private int altitudeToLabelLocation(double altitude) {
		
		int lengthBar = groundLevelTape - flightCeilingTape;
		double verticalRange = flightCeiling - groundLevel;
		int labelLocation = (int) (groundLevelTape-((altitude/verticalRange)*lengthBar));
		
		return labelLocation;
	}
	
	//Convert label location on the tape to an altitude 
	private double labelLocationToAltitude(float labelLocation) {
		
		int lengthBar = groundLevelTape - flightCeilingTape;
		double verticalRange = flightCeiling - groundLevel;
		double altitude = verticalRange*((double) groundLevelTape-labelLocation)/lengthBar;
		
		return altitude;
	}

	//Set the target altitude to the service
	private void setTargetAltitude(int aircraftNumber,float dropLocation) {
		
		double dropAltitude = labelLocationToAltitude(dropLocation);

		//If the label is dropped outside the altitude tape, set the target altitude at the bounds.
		if (dropAltitude < groundLevel) {
			dropAltitude = groundLevel;
		} else if (dropAltitude > flightCeiling) {
			dropAltitude = flightCeiling;
		}

        Log.d("DROP",String.valueOf(aircraftNumber));
		/* TODO Set the target altitude to the service once this function is available */
//		setTargetLabel(dropAltitude, 10); //Temporary setfunction to show a label
	}
}