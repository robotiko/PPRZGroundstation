package com.gcs;

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
import android.view.View.OnDragListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AltitudeTape extends Fragment {
	
	private RelativeLayout relativelayout;
	private View rootView;
	
	private boolean labelCreated = false;
	
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
	        	/* TODO implement code for selection of aircraft (colors etc.) */
	        	switch (v.getId()){
	        	case 1:
	        		Log.d("Test", "Click dynamic label!!");
	        		break;
	        	case 2:
	        		Log.d("Test", "Click static label!!");
	        		break;
	        	}
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
	            /* TODO improve the dragshadow */
	            /* TODO only allow vertical dragging: http://stackoverflow.com/questions/20307246/how-do-i-restrict-drag-and-drop-along-the-y-axis-only-in-android */
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
            	//send y location of the label to the dragshadow
            	showDragShadow(event.getY());
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
	
	public void setLabel(double altitude, int labelId){

		/* TODO Determine altitude label location based on the height of the bar and the the vertical range of the drones (flight ceiling - ground level) */
		int groundLevel = 848; //0 meter
		int flightCeiling = 35; //20 m
		
		int lengthBar = groundLevel - flightCeiling;
		int labelLocation = (int) (groundLevel-((altitude/20)*lengthBar));
		
		/* TODO change the horizontal location of the altitude labels and flip them around */
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(80, 60);
        params.leftMargin = 50;
        params.topMargin = labelLocation;
             
        TextView label;
		if(!labelCreated){
	        label = new TextView(getActivity());
	        label.setBackgroundResource(R.drawable.altitude_label_small_blue);
	        label.setId(labelId);
	        
	        /* On the basis of the label number (first label is generated to be 1) a string is
	        made to display on the altitude label */
	        String labelCharacter = String.valueOf((char)(64+labelId));
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
			relativelayout.updateViewLayout(label,params);
		}
	}
	
	private void showDragShadow(float y_loc) {
		Log.d("Current y-location",String.valueOf(y_loc));
	}
	
	private void setTargetAltitude(float dropLocation) {
		
		Log.d("Drag","Dropped at:"+String.valueOf(dropLocation));
		/* TODO use the set function of the service to set the target altitude */
		
		//1. Input to this method is the drop location of the dragged label.
		//2. Determine the corresponding altitude based on the flightceiling/groundlevel and droplocation
		//3. Set the target altitude in the service
	}
}