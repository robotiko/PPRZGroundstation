package com.gcs;

import com.aidllib.IEventListener;
import com.aidllib.IMavLinkServiceClient;
import com.aidllib.core.ConnectionParameter;
import com.aidllib.core.mavlink.waypoints.Waypoint;
import com.aidllib.core.model.Altitude;
import com.aidllib.core.model.Attitude;
import com.aidllib.core.model.Heartbeat;
import com.aidllib.core.model.Speed;
import com.aidllib.core.model.Battery;
import com.aidllib.core.model.Position;
import com.gcs.core.ConflictStatus;
import com.gcs.core.Home;
import com.google.android.gms.maps.model.PolylineOptions;
import com.sharedlib.model.State; //TODO change this to com.aidl.core.model.State once available in the aidl lib;
import com.gcs.core.Aircraft;
import com.gcs.fragments.AltitudeTape;
import com.gcs.fragments.BatteryFragment;
import com.gcs.fragments.MissionButtonFragment;
import com.gcs.fragments.TelemetryFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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

import java.util.List;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, OnMarkerClickListener, OnInfoWindowClickListener, OnMarkerDragListener {
	
	private static final String TAG = MainActivity.class.getSimpleName();
	
	private Handler handler, interfaceUpdateHandler;
	private int mInterval = 100; // seconds * 1000
	
	IMavLinkServiceClient mServiceClient;
	MissionButtonFragment missionButtons;
	Intent intent;
	
	private Button connectButton;
	private boolean isConnected;
	private boolean isAltitudeUpdated = false;

	private TelemetryFragment telemetryFragment;
	private BatteryFragment batteryFragment;
	private AltitudeTape altitudeTapeFragment;

	private Aircraft aircraft;
	private Home home;

    SupportMapFragment mapFragment;
	  
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
//		aircraft.setIconSettings(); //Fix to instantiate the icon class

		// Instantiate home object
		home = new Home();

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

        /* TODO remove this when service provides the home location */
        // TEMPORARY SETTING OF HOME LOCATION
        LatLng homeLocation = new LatLng(51.990826, 4.378248);
		home.setHomeLocation(homeLocation);
        
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

        ComponentName componentName = new ComponentName("com.pprzservices", "com.pprzservices.service.MavLinkService");
        Intent intent = new Intent();
        intent.setComponent(componentName);
        if (!bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)) {

            // TODO: Handle service not binding problems

            Log.e(TAG, "The service could not be bound");
        } else {
            Log.d(TAG, "Service was bound");
        }
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
	
	Runnable interfaceUpdater = new Runnable() {
	    @Override 
	    public void run() {

	    	//Update aircraft icons on map
            aircraftMarkerUpdater();
	    	
			//Update altitude tape
	    	if (isAltitudeUpdated){
	    		updateAltitudeTape();
                isAltitudeUpdated = false;
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

			if (mServiceClient == null)
				Log.d(TAG, "mServiceClient is null");

			try {
				mServiceClient.addEventListener(TAG, listener);

                Log.d(TAG, "Listener has been added");

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
                    updateHeartbeat();
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
//                    isConnected = true;
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
	    		
	    		case "WAYPOINTS_UPDATED": {
	    			updateWaypoints();
	    			break;
	    		}
	    		
	    		default:
	    			break;
    		}
    	}
    };

    public void requestWps() {
        if (isConnected) {
            try {
                mServiceClient.requestWpList();
                Log.d("message","Requesting waypoints");
            } catch (RemoteException e) {
                Log.e(TAG, "Error while requesting waypoints");
            }
        }
    }
    
    ////////UPDATE METHODS FOR AIRCRAFT DATA

    /**
     * This runnable object is created to update the heartbeat
     */
    private void updateHeartbeat() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Heartbeat mHeartbeat = getAttribute("HEARTBEAT");
                    aircraft.setHeartbeat(mHeartbeat.getSysId(),mHeartbeat.getCompId());
                } catch (Throwable t){

                }
            }
        });
    }

    
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

//					telemetryFragment.setText(String.valueOf(mAltitude.getAltitude()));
                    telemetryFragment.setText(String.format("%.2f", mAltitude.getAltitude()));
					
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
					aircraft.setGroundAndAirSpeeds(mSpeed.getGroundSpeed(), mSpeed.getAirspeed(), mSpeed.getTargetSpeed());
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
                    //TODO Change heading to int when this is changed in the service
                    aircraft.setLlaHdg(mPosition.getLat(), mPosition.getLon(), mPosition.getAlt(), (short) mPosition.getHdg());
                    aircraft.setDistanceHome(home.getHomeLocation());
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
					List<Waypoint> waypoints = mServiceClient.getWpList();

                    if(aircraft.getNumberOfWaypoints()>0) {
                        aircraft.clearWpList();
                    }

                    for (int i = 0; i < waypoints.size(); i++) {
                        /* TODO add the dynamic setting of the waypoint sequence number, targetSys and targetComp  */
                        aircraft.addWaypoint(waypoints.get(i).getLat(),waypoints.get(i).getLon(),waypoints.get(i).getAlt(),(short) i,(byte) 0, (byte) 0);
                    }
                    waypointUpdater();
                    Toast.makeText(getApplicationContext(), "Waypoints updated.", Toast.LENGTH_SHORT).show();

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

    public void onWaypointRequest(View v) {
        missionButtons.onWaypointRequest(v);
        requestWps();
    }

	/////////////////////////MAPS/////////////////////////
	
	/* First time the map is ready, set options */
	@Override
	public void onMapReady(GoogleMap map) {
				
		//Change the map type to satellite
		map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
		
		//Go to current location
//		map.setMyLocationEnabled(true);
//		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
//		Criteria criteria = new Criteria();
//		String provider = locationManager.getBestProvider(criteria, true);
//		Location myLocation = locationManager.getLastKnownLocation(provider);
//		LatLng currentLocation =  new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
//        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 18.0f));

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(home.getHomeLocation(),18.0f));
		
		//Disable rotation and tilt gestures
		map.getUiSettings().setRotateGesturesEnabled(false);
		map.getUiSettings().setTiltGesturesEnabled(false);
		
		//Show my location button
		map.getUiSettings().setMyLocationButtonEnabled(true);
		
		//Enable clicklistener on markers
		map.setOnMarkerClickListener(this);

        //Enable drag listener on markers
        map.setOnMarkerDragListener(this);

        //Enable clicklistener on infowindows
        map.setOnInfoWindowClickListener(this);

		//Draw home marker on map
        home.homeMarker = map.addMarker(new MarkerOptions()
				.position(home.getHomeLocation())
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.home_icon))
				.flat(true)
				.title("HOME")
				.draggable(false)
        );
	}


	/* Marker listener to unselect an aircraft icon*/
	@Override
    public boolean onMarkerClick(final Marker marker) {

        //If the aircraft icon is clicked, select it or unselect it
        if(marker.equals(aircraft.acMarker)) {
            if(!aircraft.isSelected()) {
                aircraft.setIsSelected(true);
                Log.d("infowindow","markerclick-ON");
            } else {
                aircraft.setIsSelected(false);
                Log.d("infowindow","markerclick-OFF");
            }
        }
		return true;
	}

    /* Marker drag listener for moving waypoint */

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        int wpNumber =  Integer.parseInt(marker.getSnippet());
        LatLng pos = marker.getPosition();

        /* TODO implement waypoint location change setfunction for the service */
        aircraft.setWpLatLon((float) pos.latitude, (float) pos.longitude,wpNumber);
        waypointUpdater();
    }

    /* Info window click listener to hide it*/
    @Override
    public void onInfoWindowClick(final Marker marker) {

        //If the infowindow marker is clicked, remove it
        if(marker.equals(aircraft.infoWindow)) {
            aircraft.setIsSelected(false);
            Log.d("infowindow", "windowclick-OFF");
        }
    }

	/* Update the objects that are displayed on the map */
	public void aircraftMarkerUpdater(){

		//Determine the color of the aicraft icon based on selection status
		if(aircraft.isSelected()) {
			aircraft.setCircleColor(Color.YELLOW);
		} else {
			aircraft.setCircleColor(Color.WHITE);
		}

		//Generate an icon
		aircraft.generateIcon();
		final BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(aircraft.getIcon());
		final LatLng aircraftLocation = new LatLng(aircraft.getLat(), aircraft.getLon());

		//Call GoogleMaps
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {

                ///////* Marker for display of aircraft icon on map *///////

                //Clear marker from map (if it exists)
                if (aircraft.acMarker != null) {
                    aircraft.acMarker.remove();
                }

                //Add marker to map
                aircraft.acMarker = map.addMarker(new MarkerOptions()
                                .position(aircraft.getLatLng())
                                .anchor((float) 0.5, (float) 0.5)
                                .icon(icon)
                                .flat(true)
                                .title(" " + aircraft.getLabelCharacter())
                                .infoWindowAnchor(0.5f, aircraft.getIconBoundOffset())
                                .draggable(false)
                );

                //Show either the label or the detailed information window of the aicraft based on selection status
                if(aircraft.isSelected()) {

					map.setInfoWindowAdapter(new InfoWindowAdapter() {

                        // Use default InfoWindow frame
                        @Override
                        public View getInfoWindow(Marker marker) {
                            return (null);
                        }

                        // Defines the contents of the InfoWindow
                        @Override
                        public View getInfoContents(Marker marker) {

                            View v = getLayoutInflater().inflate(R.layout.info_window_detail, null);

                            /* TODO add content to the detailed infowindow */

                            TextView infoAirtime = (TextView) v.findViewById(R.id.info_airtime);
                            TextView infoDistHome = (TextView) v.findViewById(R.id.info_dist_home);
                            TextView infoAlt = (TextView) v.findViewById(R.id.info_alt);
                            TextView infoMode = (TextView) v.findViewById(R.id.info_mode);
                            TextView infoSats = (TextView) v.findViewById(R.id.info_sats);

                            //Setting the values in the information window
                            infoAirtime.setText("Airtime: " + "AIRTIME HERE!");
                            infoDistHome.setText("Distance Home: " + String.format("%.1f", aircraft.getDistanceHome()) + "m");
                            infoAlt.setText("Altitude: " + String.format("%.1f", aircraft.getAltitude()) + "m");
                            infoMode.setText("Mode: " + "MODE HERE!");
                            infoSats.setText("#Sats: " + "#SATS HERE!");

                            return v;
                        }
                    });
                } else {

                    map.setInfoWindowAdapter(new InfoWindowAdapter() {

                        // Use default InfoWindow frame
                        @Override
                        public View getInfoWindow(Marker marker) {
                            return(null);
                        }

                        // Defines the contents of the InfoWindow
                        @Override
                        public View getInfoContents(Marker marker) {

                            View v = getLayoutInflater().inflate(R.layout.info_window_label, null);

                            TextView label = (TextView) v.findViewById(R.id.label);
                            label.setText(aircraft.getLabelCharacter());

                            return v;
                        }
                    });
                }
                aircraft.acMarker.showInfoWindow();
            }
        });
	}

    /* Update the waypoint markers that are displayed on the map */
    private void waypointUpdater() {

        //Call GoogleMaps
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {

                //If the wps are already initiated, remove them from the map and clear the list that holds them
                if(!aircraft.wpMarkers.isEmpty()) {
                    //Remove markers from map
                    for (int i = 0; i < aircraft.getNumberOfWaypoints(); i++) {
                        aircraft.wpMarkers.get(i).remove();
                    }

                    //Clear the marker list
                    aircraft.wpMarkers.clear();
                }

                //(Re)generate waypoint markers
                for (int i = 0; i < aircraft.getNumberOfWaypoints(); i++) {

                    //Add waypoint marker to map
                    Marker wpMarker = map.addMarker(new MarkerOptions()
										.position(aircraft.getWpLatLng(i))
										.flat(true)
										.snippet(String.valueOf(aircraft.getWpSeq(i)))
										.draggable(true)
						);
                    aircraft.wpMarkers.add(wpMarker);
                }

                ///* FLIGHT PATH *///
                // If the flight path has been drawn before, remove it to be updated
                if(aircraft.flightPath != null) {
                    aircraft.flightPath.remove();
                }

                // Draw the flight path with the specified characteristics
                aircraft.flightPath = map.addPolyline(new PolylineOptions()
                        .addAll(aircraft.getWpLatLngList())
                        .width(4)
                        .color(Color.WHITE));
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
		altitudeTapeFragment.setLabel(aircraft.getAltitude(),aircraft.getAltLabelId(),aircraft.getLabelCharacter(),aircraft.isSelected());
	}

    public void setIsSelected(boolean isSelected){
        aircraft.setIsSelected(isSelected);
    }

    public boolean isAircraftIconSelected() {
        return aircraft.isSelected();
    }

    public ConflictStatus getConflictStatus(){
        return aircraft.getConflictStatus();
    }
}