package com.gcs;

import android.os.Bundle;
import android.support.v4.app.Fragment;
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
	
	public void addLabel(double altitude){
		
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
//	        generateViewId () //TODO Generate this in teh aircraft class
	        label.setId(1);
	        label.setText("      A");
	        relativelayout.addView(label,params);
	        labelCreated = true;
		} else {
			label = (TextView)  getView().findViewById(1);
			relativelayout.updateViewLayout(label,params);
		}
	}
}
