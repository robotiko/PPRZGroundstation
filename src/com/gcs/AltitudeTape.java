package com.gcs;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
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

//        ImageView altitudeTape = (ImageView) getView().findViewById(R.id.altitudeTapeView);
//
//        //OnCLickListener on the altitude tape
//        altitudeTape.setOnClickListener(new View.OnClickListener() {
//            //Start new list activity
//            public void onClick(View v) {
//
//            }
//        });
        
        relativelayout = (RelativeLayout) rootView.findViewById(R.id.relativelayout);
    }
	
	public void setLabel(double altitude, int labelId){
		
		/* TODO Determine altitude label location based on the height of the bar and the the vertical range of the drones (flight ceiling - ground level) */
		
		int groundLevel = 783; //0 meter
		int flightCeiling = -30; //20 m
		
		int lengthBar = groundLevel - flightCeiling;
		int labelLocation = (int) (groundLevel-((altitude/20)*lengthBar));
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(80, 100);
        params.leftMargin = 50;
        params.topMargin = labelLocation;
        
        //Test for labels on altitude tape
//        ImageView label;
//		if(!labelCreated){
//	        label = new ImageView(getActivity());
//	        label.setImageResource(R.drawable.altitude_label_small_blue);
//	        label.setId(1);
//	        relativelayout.addView(label,params);
//	        labelCreated = true;
//		} else {
//			label = (ImageView)  getView().findViewById(1);
//			relativelayout.updateViewLayout(label,params);
//		}
        
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
	        relativelayout.addView(label,params);
	        labelCreated = true;
		} else {
			label = (TextView)  getView().findViewById(labelId);
			relativelayout.updateViewLayout(label,params);
		}
	}
}
