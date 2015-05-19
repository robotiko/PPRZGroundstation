package com.gcs;

import com.aidl.IEventListener;
import com.aidl.IMavLinkServiceClient;
import com.aidl.core.ConnectionParameter;
import com.aidl.core.model.Altitude;
import com.aidl.core.model.Attitude;
import com.aidl.core.model.Heartbeat;
import com.aidl.core.model.Speed;
import com.aidl.core.model.Battery;
import com.aidl.core.model.Position;
import com.model.State; //TODO change this to com.aidl.core.model.State once available in the aidl lib;
import com.gcs.core.Aircraft;
import com.gcs.fragments.AltitudeTape;
import com.gcs.fragments.BatteryFragment;
import com.gcs.fragments.MissionButtonFragment;
import com.gcs.fragments.TelemetryFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
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
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, OnMapClickListener, OnMarkerClickListener {
	
	private static final String TAG = MainActivity.class.getSimpleName();
	
	private Handler handler, interfaceUpdateHandler;
	private int mInterval = 100; // seconds * 1000
	
	IMavLinkServiceClient mServiceClient;
	MissionButtonFragment missionButtons;
	Intent intent;
	
	private Button connectButton;
	private boolean isConnected;
	private boolean isAircraftIconSelected = false;
	private boolean isAltitudeUpdated = false;

	private TelemetryFragment telemetryFragment;
	private BatteryFragment batteryFragment;
	private AltitudeTape altitudeTapeFragment;
	
	Aircraft aircraft;
	Marker acMarker, wpMarker, infoWindow;
	GroundOverlay mapOverlay;
	
	SupportMapFragment mapFragment;
	
	private float protectedZoneDiameter;
	  
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		handler = new Handler();
		interfaceUpdateHandler = new Handler();
		
		connectButton = (Button) findViewById(R.id.connectButton);
		isConnected = false;
		
		// Instantiate aircraft object
		aircraft = new Aircraft(this);
		aircraft.setIconSettings();
		
		//Add some waypoints (TEMPORARY)
		/*                  [lat,        lon,        alt,   index,     targetSys,targetComp] */
		aircraft.addWaypoint(51.990968f, 4.375053f, 5.0f,  (short) 0, (byte) 0, (byte) 0);
		aircraft.addWaypoint(51.990968f, 4.377670f, 10.0f, (short) 1, (byte) 0, (byte) 0);
		aircraft.addWaypoint(51.990074f, 4.377670f, 15.0f, (short) 2, (byte) 0, (byte) 0);
		aircraft.addWaypoint(51.990074f, 4.375053f, 20.0f, (short) 3, (byte) 0, (byte) 0);
		
		// Create a handle to the telemetry fragment
		telemetryFragment = (TelemetryFragment) getSupportFragmentManager().findFragmentById(R.id.telemetryFragment);
		
		// Create a handle to the battery fragment
		batteryFragment = (BatteryFragment) getSupportFragmentManager().findFragmentById(R.id.batteryFragment);
		
		// Create a handle to the altitudeTape fragment
		altitudeTapeFragment = (AltitudeTape) getSupportFragmentManager().findFragmentById(R.id.altitudeTapeFragment);
		
		// Create a handle to the MissionButton fragment
		missionButtons = (MissionButtonFragment) getSupportFragmentManager().findFragmentById(R.id.missionButtonFragment);
		
		// Get the map and register for the ready callback
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Value for the size of the protected zone around the aircraft icon
        protectedZoneDiameter = (float) getResources().getInteger(R.integer.ProtectedZoneDiameter);
        
        // Start the interface update handler
		interfaceUpdater.run();	/* TODO check if there is a better moment to start this handler (on first heartbeat?) */
	}
	
	/* TODO Make a course extrapolation class to determine the conflictStatus of a drone */

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
			Log.e(TAG, "Failed to remove event listener", e);
		}
    	unbindService(serviceConnection);
    }
	
	////////////INTERFACE UPDATER//////////
	
	
	/* TODO make sure information window is synchonously updated with tha aircraft icon */
	
	Runnable interfaceUpdater = new Runnable() {
	    @Override 
	    public void run() {
	    	//Update map (icons, waypoints and information windows)
	    	updateMap();
	    	
			//Update altitude tape
	    	if (isAltitudeUpdated){
	    		updateAltitudeTape();
	    	}
	    	
	    	//Restart this updater after the set interval
	    	interfaceUpdateHandler.postDelayed(interfaceUpdater, mInterval);
	    }
	  };
	
	/////////////////////////COMMUNICATION/////////////////////////
	
	////////SERVICE CONNECTION
	
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
    		Toast.makeText(getApplicationContext(), "Connection Failed!", Toast.LENGTH_SHORT).show();
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
	    			updateAttitude();
	    			break;
	    		}
	    		
	    		case "ALTITUDE_SPEED_UPDATED": {
	    			updateAltitude();
	    			updateSpeed();
	    			break;
	    		}
	    		
	    		case "BATTERY_UPDATED": {
	    			updateBattery();
	    			break;
	    		}
	    		
	    		case "POSITION_UPDATED": {
	    			updatePosition();
	    			break;
	    		}
	    		
	    		case "SATELLITES_VISIBLE_UPDATED": {
	    			break;
	    		}

	    		/* TODO Enable the state service case once available */
//	    		case "STATE_UPDATED": {
//	    			updateState();
//	    			break;
//	    		}
	    		
	    		/* TODO Enable the waypoint service case once available */
//	    		case "WAYPOINT_UPDATED": {
//	    			updateWaypoints();
//	    			break;
//	    		}
	    		
	    		default:
	    			break;
    		}
    	}
    };
    
    ////////UPDATE METHODS FOR AIRCRAFT DATA
    
	/**
	 * This runnable object is created to update the attitude
	 */
	private void updateAttitude() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				try {
					Attitude mAttitude = getAttribute("ATTITUDE");
					aircraft.setRollPitchYaw(mAttitude.getRoll(), mAttitude.getPitch(), Math.toDegrees(mAttitude.getYaw()));
				} catch (Throwable t) {
					Log.e(TAG, "Error while updating the attitude", t);
				}
			}
		});
	}
	
	/**
	 * This runnable object is created to update the altitude
	 */
	private void updateAltitude() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				try {
					Altitude mAltitude = getAttribute("ALTITUDE");
					aircraft.setAltitude(mAltitude.getAltitude());
					aircraft.setTargetAltitude(mAltitude.getTargetAltitude());
					
					telemetryFragment.setText(String.valueOf(mAltitude.getAltitude()));
					
					/* Set isAltitudeUpdated to be true at first update of altitude */
					if(!isAltitudeUpdated) isAltitudeUpdated = true;

				} catch (Throwable t) {
					Log.e(TAG, "Error while updating the altitude", t);
				}
			}
		});
	}
	
	/**
	 * This runnable object is created to update the ground- and airspeeds
	 */
	private void updateSpeed() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				try {
					Speed mSpeed = getAttribute("SPEED");
					aircraft.setGroundAndAirSpeeds(mSpeed.getGroundSpeed(),mSpeed.getAirspeed(),mSpeed.getTargetSpeed());
					aircraft.setTargetSpeed(mSpeed.getTargetSpeed());
				} catch (Throwable t) {
					Log.e(TAG, "Error while updating the speed", t);
				}
			}
		});
	}

	/**
	 * This runnable object is created to update the battery information
	 */
	private void updateBattery() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				try {
					Battery mBattery = getAttribute("BATTERY");
					aircraft.setBatteryState(mBattery.getBattVolt(), mBattery.getBattLevel(), mBattery.getBattCurrent());
					batteryFragment.setText(String.valueOf(mBattery.getBattVolt()));
				} catch (Throwable t) {
					Log.e(TAG, "Error while updating the battery information", t);
				}
			}
		});
	}
	
	/**
	 * This runnable object is created to update position
	 */
	private void updatePosition() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				try {
					Position mPosition = getAttribute("POSITION");
					aircraft.setSatVisible(mPosition.getSatVisible());
					aircraft.setLlaHdg(mPosition.getLat(),mPosition.getLon(),mPosition.getAlt(),(short) mPosition.getHdg());
					//TODO check if heading should be an int or short (and make changes accordingly)
				} catch (Throwable t) {
					Log.e(TAG, "Error while updating position", t);
				}
			}
		});
	}
	
	/**
	 * This runnable object is created to update state
	 */
	private void updateState() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				try {
					State mState = getAttribute("STATE");
					aircraft.setIsFlying(mState.isFlying());
					aircraft.setArmed(mState.isArmed());
				} catch (Throwable t) {
					Log.e(TAG, "Error while updating state", t);
				}
			}
		});
	}
	
	/**
	 * This runnable object is created to update waypoints
	 */
	private void updateWaypoints() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				try {
					/* TODO finish the setting of received waypoint data from service */
				} catch (Throwable t) {
					Log.e(TAG, "Error while updating waypoints", t);
				}
			}
		});
	}


	////////OTHER COMMUNICATION FUNCTIONS
	
	private ConnectionParameter retrieveConnectionParameters() {
		
		/* TODO: Fetch connection type */
		
        final int connectionType = 0; // UDP connection
        
        /* TODO: Fetch server port */
        
        final int serverPort = 8888; 
        
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
	        	Log.e(TAG, "Error while connecting to service", e);
	        }
        }
        
        /* TODO: Update the text of the connect button */
        
    }
	
	/* TODO solve bugs with the connect button */
    public void onButtonRequest(View view) {
    	if (!isConnected)
    		connectToDroneClient();
		else
			try {
				mServiceClient.disconnectDroneClient();
			} catch (RemoteException e) {
				Log.e(TAG, "Error while disconnecting", e);
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
            	
            case "BATTERY":
            	return (T) new Battery();
            	
            case "POSITION":
            	return (T) new Position();
            	
            case "STATE":
            	return (T) new State();
            	
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

			case "BATTERY":
				return Battery.class.getClassLoader();
				
			case "POSITION":
				return Position.class.getClassLoader();

			case "STATE":
				return State.class.getClassLoader();			

			default:
				return null;
		}
	 }
	
	/////////////////////MISSION BUTTONS/////////////////////
	
	public void onLandRequest(View v) {
		missionButtons.onLandRequest(v);
	}
	
	public void onTakeOffRequest(View v) {
		missionButtons.onTakeOffRequest(v);
	}
	
	public void onGoHomeRequest(View v) {
		missionButtons.onGoHomeRequest(v);
	}

	/////////////////////////MAPS/////////////////////////
	
	/* First time the map is ready, set options */
	@Override
	public void onMapReady(GoogleMap map) {
				
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
		
		//Disable rotation and tilt gestures
		map.getUiSettings().setRotateGesturesEnabled(false);
		map.getUiSettings().setTiltGesturesEnabled(false);
		
		//Show my location button
		map.getUiSettings().setMyLocationButtonEnabled(true);
		
		//Enable clicklistener on the map
		map.setOnMapClickListener(this);
		
		//Enable clicklistener on markers
		map.setOnMarkerClickListener(this);
		
		/* TODO limit zooming for groundoverlays */ //http://stackoverflow.com/questions/14977078/limit-scrolling-and-zooming-google-maps-android-api-v2
		
		//Enable a custom information window for the aircraft icons
		map.setInfoWindowAdapter(new InfoWindowAdapter() {
			
			// Use default InfoWindow frame
			@Override
			public View getInfoWindow(Marker marker) {
				return(null);
			}
			
			// Defines the contents of the InfoWindow
            @Override
            public View getInfoContents(Marker marker) {
            	
            	View v = getLayoutInflater().inflate(R.layout.info_window, null);
            	
            	/* TODO add content to infowindow */
            	
            	TextView infoAirtime  = (TextView) v.findViewById(R.id.info_airtime);
            	TextView infoDistHome = (TextView) v.findViewById(R.id.info_dist_home);
            	TextView infoAlt      = (TextView) v.findViewById(R.id.info_alt);
            	TextView infoMode     = (TextView) v.findViewById(R.id.info_mode);
            	TextView infoSats     = (TextView) v.findViewById(R.id.info_sats);

	        	//Setting the values in the information window
            	infoAirtime.setText("Airtime: " + "AIRTIME HERE!");
            	infoDistHome.setText("Distance Home: " + "DISTANCE HERE!");
            	infoAlt.setText("Altitude: "+ aircraft.getAltitude());
            	infoMode.setText("Mode: " + "MODE HERE!");
            	infoSats.setText("#Sats: " + "#SATS HERE!");

            	return v;
            }
		});
	}
	
	/* Map listener for clicks (might be changed to OnMapLongClickListener) */
	@Override
	public void onMapClick(LatLng point) {
		
		/* TODO fix bugs with aircraft icon selection */
		 
		//Calculate the distance from clicklocation to the aircraft location
		float[] distance = new float[1];
		Location.distanceBetween(aircraft.getLat()*1e-7,aircraft.getLon()*1e-7,point.latitude,point.longitude,distance);
		 
		//If the clicklocation is within the protected zone, excecute the following code
		if(distance[0] <= protectedZoneDiameter/2) {
			/* TODO implement actions for clicked aircraft icons */
			
			/* Show or remove the info window (note that an additional onmarkerlistener is used because 
			 * an extra (hidden) marker is used for the info window, which can also be clicked */
			if(isAircraftIconSelected) {
				isAircraftIconSelected = false;
				infoWindow.remove();
				Log.d("icon","deselected");
			} else {
				isAircraftIconSelected = true;
				setInfoWindow();
				Log.d("icon","selected");
			}
		}
	}
	
	/* Marker listener to unselect an aircraft icon*/
	@Override
    public boolean onMarkerClick(final Marker marker) {
		
		//If the infowindow marker is clicked, remove it
		if(marker.equals(infoWindow)) {
			isAircraftIconSelected = false;
			infoWindow.remove();
		}
		return true;
	}
	
	/* Set the information window for an aircraft icon */
	private void setInfoWindow() {

		//Call GoogleMaps
		mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
            	
//            	if(infoWindow != null) {
//            		infoWindow.remove();
//            	}
//            	
//            	infoWindow = map.addMarker(new MarkerOptions()
//            	.position(aircraft.getLatLng())
//                .alpha(1)
//                .draggable(false)
////	                .infoWindowAnchor(0.5f,0.5f)  //Determine the location of the info window
//    			);
//            	infoWindow.showInfoWindow();
            }
		}); 
	}
	
	/* Update the objects that are displayed on the map */
	public void updateMap(){
		
		//Determine the color of the aicraft icon depoendent on selection status
		if(isAircraftIconSelected) {
			aircraft.setCircleColor(Color.YELLOW);
		} else {
			aircraft.setCircleColor(Color.WHITE);
		}
		
		//Generate an icon
		aircraft.generateIcon();
		final BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(aircraft.getIcon());
		final LatLng aircraftLocation = new LatLng(aircraft.getLat()*1e-7, aircraft.getLon()*1e-7);
		
		//Call GoogleMaps
		mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {

				///////* Marker for display of aircraft icon on map *///////

                //Clear marker from map (if it exists)
            	if(acMarker != null) {
            		acMarker.remove();
            	}

                /* TODO use setZIndex to make sure the aircraft icons are drawn on top of polylines for flight plan (largest number goes on top)*/
                //Add marker to map
                acMarker = map.addMarker(new MarkerOptions()
                                .position(aircraft.getLatLng())
                                .anchor((float) 0.5, (float) 0.5)
                                .icon(icon)
                                .flat(true)
                                .title("Aircraft")
                                .draggable(false)
                );


            	///////* Set the information window for an aircraft icon *///////
            	
            	if(isAircraftIconSelected) {
                	if(infoWindow != null) {
                		infoWindow.remove();
                	}
                	
                	infoWindow = map.addMarker(new MarkerOptions()
                	.position(aircraft.getLatLng())
                    .alpha(1)
                    .draggable(false)
//    	                .infoWindowAnchor(0.5f,0.5f)  //Determine the location of the info window
        			);
                	infoWindow.showInfoWindow();
    			}

            	///////* Marker for display of waypoints on map *///////
            	
            	//Clear marker from map (if it exists)
            	if(wpMarker != null) {
            		wpMarker.remove();
            	}
            	            	
            	//Add marker to map (size remains constant with zooming)
            	wpMarker = map.addMarker(new MarkerOptions()
                .position(aircraft.getWpLatLng(0))
                .anchor((float) 0.5, (float) 0.5)
                .flat(true)
                .title("Waypoint")
                .draggable(false)
            	);	

            }
        });  
	}
	
	/////////////////////////ALTITUDE TAPE/////////////////////////
	
	public void updateAltitudeTape(){
		
		/* Set the location of the target label on the altitude tape and check wether to 
		 * show the target label or not (aka is the aircraft already on target altitude?) */
		if (Math.abs(aircraft.getTargetAltitude()-aircraft.getAltitude()) > 0.001){
			altitudeTapeFragment.setTargetLabel(aircraft.getTargetAltitude(), aircraft.getTargetLabelId());
		} else {
			altitudeTapeFragment.deleteTargetLabel(aircraft.getTargetLabelId());
		}
		
		/* Set the location (actual) altitude label on the altitude tape */ 
		altitudeTapeFragment.setLabel(aircraft.getAltitude(),aircraft.getAltLabelId());
	}
	
}