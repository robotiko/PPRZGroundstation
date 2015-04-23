package com.gcs;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

	private TelemetryFragment telemetryFragment;
	private BatteryFragment batteryFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Create a handle to the telemetry fragment
		telemetryFragment = (TelemetryFragment) getSupportFragmentManager().findFragmentById(R.id.telemetryFragment);
		
		// Create a handle to the battery fragment
		batteryFragment = (BatteryFragment) getSupportFragmentManager().findFragmentById(R.id.batteryFragment);
		
		// Get the map and register for the ready callback
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onMapReady(GoogleMap map) {
//		map.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
		
		//Change the map type to satellite
		map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
		
		//Change the map location
		final LatLng LR = new LatLng(51.990694, 4.375749);
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(LR, 18.0f));
	}
}
