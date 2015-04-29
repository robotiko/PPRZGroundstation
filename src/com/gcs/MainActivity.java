package com.gcs;

import com.aidl.IEventListener;
import com.aidl.IMavLinkServiceClient;
import com.aidl.core.ConnectionParameter;
import com.aidl.core.model.Altitude;
import com.aidl.core.model.Attitude;
import com.aidl.core.model.Heartbeat;
import com.aidl.core.model.Speed;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
	
	private static final String TAG = MainActivity.class.getSimpleName();
	
	private Handler handler;
	
	IMavLinkServiceClient mServiceClient;
	
	private Button connectButton;
	private boolean isConnected;
	
	Intent intent;

	private TelemetryFragment telemetryFragment;
	private BatteryFragment batteryFragment;
	
	/**
     * Create service connection
     */
    ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder serviceClient) {
			mServiceClient = IMavLinkServiceClient.Stub.asInterface(serviceClient);
			try {
				mServiceClient.addEventListener(TAG, listener);
			} catch (RemoteException e) {
				Log.e(TAG, "Failed to add listener", e);
			}
			Log.i(TAG, "Service connection established.");
		}
		
        @Override
        public void onServiceDisconnected(ComponentName name) {
        	Log.i(TAG, "Service connection lost.");
        }
    };
    
    private IEventListener.Stub listener = new IEventListener.Stub() {
    	@Override
		public void onConnectionFailed() {
    		/* TODO: Handle connection failure */
    	}
    	
    	@Override
        public void onEvent(String type) {
    		switch(type) {
    			case "CONNECTED": {
    				Log.d(TAG, "Connected!");
    				isConnected = true;
    				updateConnectButton();  				
	    			break;
    			}   			
	
	    		case "HEARTBEAT_FIRST": {
	    			break;
	    		}
	    			
	    		case "DISCONNECTED": {
	    			Log.d(TAG, "Disconnected!");
	    			isConnected = false;
	    			updateConnectButton();	    			
	    			break;
	    		}
	    		
	    		case "ATTITUDE_UPDATED": {
	    			batteryFragment.setText("1.1");
//	    			updateAttitude();
	    			break;
	    		}
	    		
	    		case "ALTITUDE_SPEED_UPDATED": {
//	    			updateAltitude();
//	    			updateSpeed();
	    			break;
	    		}
	    		
	    		case "BATTERY_UPDATED": {
	    			
	    		}
	    		
	    		default:
	    			break;
    		}
    	}
    };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		handler = new Handler();
		
		connectButton = (Button) findViewById(R.id.connectButton);
		isConnected = false;
		
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
    public void onStart() {
        super.onStart();
        
        Intent intent = new Intent(IMavLinkServiceClient.class.getName());
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
	
	@Override
    public void onDestroy() {
    	try {
			mServiceClient.removeEventListener(TAG);
		} catch (RemoteException e) {
			// TODO Catch exception
		}
    	unbindService(serviceConnection);
    }
	
	private ConnectionParameter retrieveConnectionParameters() {
		
		/* TODO: Fetch connection type */
		
        final int connectionType = 0; // UDP connection
        
        /* TODO: Fetch server port */
        
        final int serverPort = 5000; 
        
        Bundle extraParams = new Bundle();

        ConnectionParameter connParams;
        switch (connectionType) {

            case 0:
                extraParams.putInt("udp_port", serverPort);
                connParams = new ConnectionParameter(connectionType, extraParams);
                break;
                
            default:
            	connParams = null;
                break;
        }

        return connParams;
    }
	
	public void connectToDroneClient(){
        final ConnectionParameter connParams = retrieveConnectionParameters();
        if (!(connParams == null)) {
	        try {
	        	mServiceClient.connectDroneClient(connParams);
	        } catch (RemoteException e) {
	            /* TODO: Handle remote exception */
	        }
        }
        
        /* TODO: Update the text of the connect button */
        
    }
	
    public void onButtonRequest(View view) {
    	if (!isConnected)
    		connectToDroneClient();
		else
			try {
				mServiceClient.disconnectDroneClient();
			} catch (RemoteException e) {
				// TODO: Handle exception
			}
    }
    
    /**
     * This runnable object is created such that the update is performed
     * by the UI handler. This is a design requirement posed by the Android SDK
     */
	private void updateConnectButton() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				try {
					if (isConnected) {
						connectButton.setText(R.string.disconnect_button);
					} else {
						connectButton.setText(R.string.connect_button);
					}
				} catch (Throwable t) {
					Log.e(TAG, "Error while updating the connect button", t);
				}
			}
		});
	}
	
	public <T extends Parcelable> T getAttribute(String type) {
        if (type == null)
            return null;

        T attribute = null;
        Bundle carrier = null;
        try {
            carrier = mServiceClient.getAttribute(type);   
        } catch (RemoteException e) {
            /* TODO: Handle remote exception */
        }

        if (carrier != null) {
            ClassLoader classLoader = getAttributeClassLoader(type);
            if (classLoader != null) {
                carrier.setClassLoader(classLoader);
                attribute = carrier.getParcelable(type);
            }
        }

        return attribute == null ? this.<T>getAttributeDefaultValue(type) : attribute;
    }
	
	@SuppressWarnings("unchecked")
	private <T extends Parcelable> T getAttributeDefaultValue(String type) {
        switch (type) {
            case "HEARTBEAT":
                return (T) new Heartbeat();
                
            case "ALTITUDE":
            	return (T) new Altitude();
            	
            case "SPEED":
            	return (T) new Speed();
            	
            case "ATTITUDE":
            	return (T) new Attitude();
                
            default:
            	return null;
        }
	}
	
	private ClassLoader getAttributeClassLoader(String type) {
		switch (type) {
			case "HEARTBEAT":
				return Heartbeat.class.getClassLoader();
				
			case "ALTITUDE":
				return Altitude.class.getClassLoader();
				
			case "SPEED":
				return Speed.class.getClassLoader();
				
			case "ATTITUDE":
				return Attitude.class.getClassLoader();

			default:
				return null;
		}
	 }

	/////////////////////////MAPS/////////////////////////
	
	@Override
	public void onMapReady(GoogleMap map) {
		// TODO: Work on available map gestures and maps-buttons on the side of the map
		
//		map.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
		
		//Change the map type to satellite
		map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
		
		//Go to current location
		map.setMyLocationEnabled(true);
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		String provider = locationManager.getBestProvider(criteria, true);
		Location myLocation = locationManager.getLastKnownLocation(provider);
		LatLng currentLocation =  new LatLng(myLocation.getLatitude(),myLocation.getLongitude());
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 18.0f));
		
		//Disable rotation gestures
		map.getUiSettings().setRotateGesturesEnabled(false);
		
		//Show zoom controls
		map.getUiSettings().setZoomControlsEnabled(true);
		
		//Show my location button
		map.getUiSettings().setMyLocationButtonEnabled(true);
		
	}
}
