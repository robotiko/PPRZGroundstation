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
import com.google.android.gms.maps.model.Polyline;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, OnMarkerClickListener, OnInfoWindowClickListener, OnMarkerDragListener {
	
	private static final String TAG = MainActivity.class.getSimpleName();
	
	private Handler handler, interfaceUpdateHandler;
	private int mInterval = 900; // seconds * 1000
	
	IMavLinkServiceClient mServiceClient;
	MissionButtonFragment missionButtons;
	
	private Button connectButton;
	private boolean isConnected;
	private boolean isAltitudeUpdated = false;
    private boolean aircraftSelected = false;

	private TelemetryFragment telemetryFragment;
	private BatteryFragment batteryFragment;
	private AltitudeTape altitudeTapeFragment;

    final SparseArray<Aircraft> mAircraft = new SparseArray<>();
    private List<Polyline> mConnectingLines  = new ArrayList<>();
    private ArrayList<Integer> conflictingAircraft = new ArrayList<>();
    private ArrayList<Integer> sameLevelAircraft = new ArrayList<>();

	private Home home;

    private float verticalSeparationStandard;

    SupportMapFragment mapFragment;

	/* Find memory leaks: https://developer.android.com/tools/debugging/debugging-memory.html */
	  
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		handler = new Handler();
		interfaceUpdateHandler = new Handler();
		
		connectButton = (Button) findViewById(R.id.connectButton);
		isConnected = false;
        verticalSeparationStandard = getResources().getInteger(R.integer.verticalSeparationStandard)/10f;
		
		// Instantiate aircraft object
        mAircraft.put(1, new Aircraft(getApplicationContext()));

		// Instantiate home object
		home = new Home();

                /* TODO remove this when service provides the home location */
        // TEMPORARY SETTING OF HOME LOCATION
        LatLng homeLocation = new LatLng(51.990826, 4.378248);
        home.setHomeLocation(homeLocation);

        //TEMPORARY DUMMY AIRCRAFT
        mAircraft.put(2, new Aircraft(getApplicationContext()));
        mAircraft.get(2).setLlaHdg(519925740, 43775620, 10, (short) 180);
        mAircraft.get(2).setAltitude(10);
        mAircraft.get(2).setBatteryState(10, 1, 1);
        mAircraft.get(2).setDistanceHome(homeLocation);
		mAircraft.get(2).setRollPitchYaw(0, 0, 180);

		mAircraft.put(3, new Aircraft(getApplicationContext()));
		mAircraft.get(3).setLlaHdg(519910540, 43794130, 17, (short) 300);
		mAircraft.get(3).setAltitude(17);
		mAircraft.get(3).setBatteryState(10, 1, 1);
		mAircraft.get(3).setDistanceHome(homeLocation);
		mAircraft.get(3).setRollPitchYaw(0,0,300);


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

        // Start the interface update handler
		interfaceUpdater.run();	/* TODO check if there is a better moment to start this handler (on first heartbeat?) */
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
		super.onDestroy();

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

			//Update altitude tape
	    	if (isAltitudeUpdated){
	    		updateAltitudeTape();
                isAltitudeUpdated = false;
	    	}

            //check for altitude and course conflicts
            for(int i = 1; i < mAircraft.size()+1; i++) {
                for(int j = 1; j < mAircraft.size()+1; j++) {
                    if(i!=j) {
                        if(Math.abs(mAircraft.get(i).getAltitude() - mAircraft.get(j).getAltitude()) <= verticalSeparationStandard) {
                            //Check for conflict course
                            if(isOnconflictCourse(i,j)){ //On conflict course

                                /* TODO make sure no double couples will be present in the conflictingAircraft list */
                                conflictingAircraft.add(i);
                                conflictingAircraft.add(j);

                            } else { //Not on conflict course
                                sameLevelAircraft.add(i);
                                sameLevelAircraft.add(j);

                                //Clear connecting lines if they still exist
                                removeConnectingLines();
                            }
                        } else {
                            mAircraft.get(i).setConflictStatusNew(ConflictStatus.GRAY);
                            mAircraft.get(j).setConflictStatusNew(ConflictStatus.GRAY);

                            //Clear connecting lines if they still exist
                            removeConnectingLines();
                        }
                    }
                }
            }

            //Set the color of the aircraft that have the "red" conflict status
            if(!conflictingAircraft.isEmpty()) {
                for (int k = 0; k < conflictingAircraft.size()-1; k++) {
                    mAircraft.get(conflictingAircraft.get(k)).setConflictStatusNew(ConflictStatus.RED);
                    mAircraft.get(conflictingAircraft.get(k + 1)).setConflictStatusNew(ConflictStatus.RED);
                }
            }

            //Set the color of the aircraft that have the "blue" conflict status
            if(!sameLevelAircraft.isEmpty()) {
                for (int k = 0; k < sameLevelAircraft.size()-1; k++) {
                    mAircraft.get(sameLevelAircraft.get(k)).setConflictStatusNew(ConflictStatus.BLUE);
                    mAircraft.get(sameLevelAircraft.get(k+1)).setConflictStatusNew(ConflictStatus.BLUE);
                }
                sameLevelAircraft.clear();
            }

            //Update aircraft icons on map
            for(int i = 1; i<mAircraft.size()+1;i++) {
                aircraftMarkerUpdater(i);
            }

            drawConnectingLines();

            Log.d(TAG,"Updating the interface");

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

    //Method to request the service to send (updated) waypoints
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
                    mAircraft.get(1).setHeartbeat(mHeartbeat.getSysId(), mHeartbeat.getCompId());
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
                    mAircraft.get(1).setRollPitchYaw(mAttitude.getRoll(), mAttitude.getPitch(), Math.toDegrees(mAttitude.getYaw()));
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
                    mAircraft.get(1).setAltitude(mAltitude.getAltitude());
                    mAircraft.get(1).setTargetAltitude(mAltitude.getTargetAltitude());

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
                    mAircraft.get(1).setGroundAndAirSpeeds(mSpeed.getGroundSpeed(), mSpeed.getAirspeed(), mSpeed.getTargetSpeed());
                    mAircraft.get(1).setTargetSpeed(mSpeed.getTargetSpeed());
				} catch (Throwable t) {
					Log.e(TAG, "Error while updating the speed", t);
				}
			}
		});
	}

	/**
	 * This runnable object is created to update the battery information
	 */
	private void  updateBattery() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				try {
					Battery mBattery = getAttribute("BATTERY");
                    mAircraft.get(1).setBatteryState(mBattery.getBattVolt(), mBattery.getBattLevel(), mBattery.getBattCurrent());
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
                    mAircraft.get(1).setSatVisible(mPosition.getSatVisible());
                    //TODO Change heading to int when this is changed in the service
                    mAircraft.get(1).setLlaHdg(mPosition.getLat(), mPosition.getLon(), mPosition.getAlt(), (short) mPosition.getHdg());
                    mAircraft.get(1).setDistanceHome(home.getHomeLocation());
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
                    mAircraft.get(1).setIsFlying(mState.isFlying());
                    mAircraft.get(1).setArmed(mState.isArmed());
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

                    if(mAircraft.get(1).getNumberOfWaypoints()>0) {
                        mAircraft.get(1).clearWpList();
                    }

                    for (int i = 0; i < waypoints.size(); i++) {
                        /* TODO add the dynamic setting of the waypoint sequence number, targetSys and targetComp  */
                        mAircraft.get(1).addWaypoint(waypoints.get(i).getLat(), waypoints.get(i).getLon(), waypoints.get(i).getAlt(), (short) i, (byte) 0, (byte) 0);
                    }
                    waypointUpdater(1);
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
		
		//Enable the go to my location button
		map.setMyLocationEnabled(true);

		//Go to current location
//		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
//		Criteria criteria = new Criteria();
//		String provider = locationManager.getBestProvider(criteria, true);
//		Location myLocation = locationManager.getLastKnownLocation(provider);
//		LatLng currentLocation =  new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
//      map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 18.0f));

        //Move camera to the home location
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
                        .anchor(0.5f, 0.5f)
                        .flat(true)
                        .title("HOME")
						.snippet("HOME")
                        .draggable(false)
        );
	}

	/* Marker listener to unselect an aircraft icon*/
	@Override
    public boolean onMarkerClick(final Marker marker) {
        if(marker.getSnippet().contains("-")){                          //Waypoint marker clicked
            String[] numbers = marker.getSnippet().split("-");
            int acNumber = Integer.parseInt(numbers[0]);
            int wpNumber = Integer.parseInt(numbers[1]);
        } else if(marker.getSnippet().equals("HOME")) {                 //Home marker clicked
            //Do nothing yet
        } else {                                                        //Aircraft marker clicked
                int acNumber = Integer.parseInt(marker.getSnippet());

                //When the aircraft icon is clicked, select it or unselect it
                if(!mAircraft.get(acNumber).isSelected()) {
                    if(aircraftSelected) {
                        //Set all aircraft on not selected
                        unselectAllAircraft();
                    }
                    mAircraft.get(acNumber).setIsSelected(true);
                    aircraftSelected = true;
                    Log.d("infowindow","markerclick-ON");
                } else {
                    mAircraft.get(acNumber).setIsSelected(false);
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

    //Drag method for waypoints
    @Override
    public void onMarkerDragEnd(Marker marker) {
        String[] numbers = marker.getSnippet().split("-");
        int acNumber = Integer.parseInt(numbers[0]);
        int wpNumber = Integer.parseInt(numbers[1]);
        LatLng pos = marker.getPosition();

        /* TODO implement waypoint location change setfunction for the service */
        mAircraft.get(acNumber).setWpLatLon((float) pos.latitude, (float) pos.longitude, wpNumber);
        waypointUpdater(acNumber);
    }

    /* Info window click listener to hide it*/
    @Override
    public void onInfoWindowClick(final Marker marker) {
        int clickedAircraft = Integer.parseInt(marker.getSnippet());

        //If the infowindow marker is clicked, remove it
        if(marker.equals(mAircraft.get(clickedAircraft).infoWindow)) {
            mAircraft.get(clickedAircraft).setIsSelected(false);
            Log.d("infowindow", "windowclick-OFF");
        }
    }

	/* Update the objects that are displayed on the map */
	public void aircraftMarkerUpdater(int acNumber){

        final int aircraftNumber = acNumber;

		//Determine the color of the aircraft icon based on selection status
		if(mAircraft.get(aircraftNumber).isSelected()) {
            mAircraft.get(aircraftNumber).setCircleColor(Color.YELLOW);
		} else {
            mAircraft.get(aircraftNumber).setCircleColor(Color.WHITE);
		}

		//Generate an icon
        mAircraft.get(aircraftNumber).generateIcon();

		//Call GoogleMaps
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {

                ///////* Marker for display of aircraft icon on map *///////

                //Clear marker from map (if it exists)
                if (mAircraft.get(aircraftNumber).acMarker != null) {
                    mAircraft.get(aircraftNumber).acMarker.remove();
                }

                //Add marker to map
                mAircraft.get(aircraftNumber).acMarker = map.addMarker(new MarkerOptions()
                                .position(mAircraft.get(aircraftNumber).getLatLng())
                                .anchor((float) 0.5, (float) 0.5)
                                .icon(BitmapDescriptorFactory.fromBitmap(mAircraft.get(aircraftNumber).getIcon()))
                                .flat(true)
                                .title(" " + mAircraft.get(aircraftNumber).getLabelCharacter())
                                .snippet(String.valueOf(aircraftNumber))
                                .infoWindowAnchor(0.5f, mAircraft.get(aircraftNumber).getIconBoundOffset())
                                .draggable(false)
                );

                //Show either the label or the detailed information window of the aicraft based on selection status
                if (mAircraft.get(aircraftNumber).isSelected()) {

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
                            infoDistHome.setText("Distance Home: " + String.format("%.1f", mAircraft.get(aircraftNumber).getDistanceHome()) + "m");
                            infoAlt.setText("Altitude: " + String.format("%.1f", mAircraft.get(aircraftNumber).getAltitude()) + "m");
                            infoMode.setText("Mode: " + "MODE HERE!");
                            infoSats.setText("#Sats: " + "#SATS HERE!");

                            return v;
                        }
                    });
                    mAircraft.get(aircraftNumber).acMarker.showInfoWindow();
                }
            }
        });
	}

    /* Update the waypoint markers that are displayed on the map */
    private void waypointUpdater(int acNumber) {

        final int aircraftNumber = acNumber;

        //Call GoogleMaps
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {

                //If the wps are already initiated, remove them from the map and clear the list that holds them
                if (!mAircraft.get(aircraftNumber).wpMarkers.isEmpty()) {
                    //Remove markers from map
                    for (int i = 0; i < mAircraft.get(1).getNumberOfWaypoints(); i++) {
                        mAircraft.get(aircraftNumber).wpMarkers.get(i).remove();
                    }

                    //Clear the marker list
                    mAircraft.get(aircraftNumber).wpMarkers.clear();
                }

                //(Re)generate waypoint markers
                for (int i = 0; i < mAircraft.get(1).getNumberOfWaypoints(); i++) {

                    //Add waypoint marker to map
                    Marker wpMarker = map.addMarker(new MarkerOptions()
                                    .position(mAircraft.get(aircraftNumber).getWpLatLng(i))
                                    .flat(true)
                                    .snippet(String.valueOf(aircraftNumber) + "-" + String.valueOf(mAircraft.get(aircraftNumber).getWpSeq(i)))
                                    .draggable(true)
                    );
                    mAircraft.get(aircraftNumber).wpMarkers.add(wpMarker);
                }

                ///* FLIGHT PATH *///
                // If the flight path has been drawn before, remove it to be updated
                if (mAircraft.get(aircraftNumber).flightPath != null) {
                    mAircraft.get(aircraftNumber).flightPath.remove();
                }

                // Draw the flight path with the specified characteristics
                mAircraft.get(aircraftNumber).flightPath = map.addPolyline(new PolylineOptions()
                        .addAll(mAircraft.get(aircraftNumber).getWpLatLngList())
                        .width(4)
                        .color(Color.WHITE));
            }
        });
    }

    ///////* Connecting lines to indicate conflicting aircraft pairs *///////
    private void drawConnectingLines() {

        //Call GoogleMaps
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {

                //If lines have been drawn before, remove them from the map
                if (!mConnectingLines.isEmpty()) {
                    for(int i=0; i< mConnectingLines.size(); i++) {
                        mConnectingLines.get(i).remove();
                    }
                }

                if(conflictingAircraft != null) {
                    for (int i = 0; i < conflictingAircraft.size(); i += 2) {

                        //Draw a connecting line on the map
                        Polyline connectingLine = map.addPolyline(new PolylineOptions()
                                .add(mAircraft.get(conflictingAircraft.get(i)).getLatLng(), mAircraft.get(conflictingAircraft.get(i+1)).getLatLng())
                                .width(6)
                                .color(Color.RED));

                        //Save polyline object in a list to have a reference to it
                        mConnectingLines.add(connectingLine);
                    }
                    //Clear all points for which the lines were drawn
                    conflictingAircraft.clear();
                }
            }
        });
    }

    private void removeConnectingLines() {
        if (!mConnectingLines.isEmpty()) {
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap map) {
                    for (int i = 0; i < mConnectingLines.size(); i++) {
                        mConnectingLines.get(i).remove();
                    }
                }
            });
        }
    }

    /////////////////////////CLASS METHODS/////////////////////////

    private void unselectAllAircraft() {
        for(int i=1; i<mAircraft.size()+1; i++) {
            mAircraft.get(i).setIsSelected(false);
        }
    }

    private boolean isOnconflictCourse(int ac1, int ac2) {
        /* TODO make algorithm to check conflict courses (extrapolation) */
        boolean isInconflictcourse = true;
        return isInconflictcourse;
    }

	/////////////////////////ALTITUDE TAPE/////////////////////////

	public void updateAltitudeTape(){

        /* TODO move the targetalttiude indication to the for loop */
		/* Set the location of the target label on the altitude tape and check wether to 
		 * show the target label or not (aka is the aircraft already on target altitude?) */
		if (Math.abs(mAircraft.get(1).getTargetAltitude()-mAircraft.get(1).getAltitude()) > 0.001){
			altitudeTapeFragment.setTargetLabel(mAircraft.get(1).getTargetAltitude(), mAircraft.get(1).getTargetLabelId());
		} else {
			altitudeTapeFragment.deleteTargetLabel(mAircraft.get(1).getTargetLabelId());
		}
		
		/* Set the location (actual) altitude labels on the altitude tape */
		for(int i = 1; i<mAircraft.size()+1;i++) {
            altitudeTapeFragment.setLabel(mAircraft.get(i).getAltitude(), mAircraft.get(i).getAltLabelId(), mAircraft.get(i).getLabelCharacter(), mAircraft.get(i).isSelected(), mAircraft.get(i).isLabelCreated(), i);
		}
	}

    public void setIsSelected(int aircraftNumber, boolean isSelected){
        if(isSelected) {
            unselectAllAircraft();
        }

        mAircraft.get(aircraftNumber).setIsSelected(isSelected);
        updateAltitudeTape();
    }

    public void setIsLabelCreated(boolean isLabelCreated,int acNumber) {
        mAircraft.get(acNumber).setIsLabelCreated(isLabelCreated);
    }

    public boolean isAircraftIconSelected(int aircraftNumber) {
        return mAircraft.get(aircraftNumber).isSelected();
    }

    public ConflictStatus getConflictStatus(int acNumber){
        return mAircraft.get(acNumber).getConflictStatus();
    }
}