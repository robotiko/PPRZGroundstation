package com.gcs;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

public class AltitudeTape extends Fragment {
	
	ImageView altitudeTape;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.altitude_tape, container, false);
    }
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        altitudeTape = (ImageView) getView().findViewById(R.id.altitudeTapeView);

        altitudeTape.setOnClickListener(new View.OnClickListener() {
            //Start new list activity
            public void onClick(View v) {
                //Toast to test clickable altitudetape
                Toast.makeText(getActivity(), "CLICK!!",Toast.LENGTH_LONG).show();
            }
        });

    }
}
