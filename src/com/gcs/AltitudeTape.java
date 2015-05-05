package com.gcs;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class AltitudeTape extends Fragment {
	
	LinearLayout linearlayout;
	View rootView;
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ImageView altitudeTape = (ImageView) getView().findViewById(R.id.altitudeTapeView);

        altitudeTape.setOnClickListener(new View.OnClickListener() {
            //Start new list activity
            public void onClick(View v) {
                //Toast to test clickable altitudetape
                Toast.makeText(getActivity(), "CLICK!!",Toast.LENGTH_LONG).show();
                addLabel(0.0f);
                addLabel(6.0f);
            }
        });
        linearlayout = (LinearLayout) rootView.findViewById(R.id.linearlayout);
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
		
        // Inflate the layout for this fragment
		rootView = inflater.inflate(R.layout.altitude_tape, container, false);
        
        return rootView;    
    }
	
	public void addLabel(float altitude){
        
        //Test for labels on altitude tape
        ImageView label = new ImageView(getActivity());
        label.setImageResource(R.drawable.altitude_label_small_blue);
        label.setY(altitude);

        linearlayout.addView(label);
	}
}
