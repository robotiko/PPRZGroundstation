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
import com.gcs.fragments.PerformanceScoreFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
//import com.sharedlib.model.State; //TODO change this to com.aidl.core.model.State once available in the aidl lib;
import com.gcs.core.Aircraft;
import com.gcs.fragments.AltitudeTape;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, OnMarkerClickListener, OnInfoWindowClickListener, OnMarkerDragListener {
	
	private static final String TAG = MainActivity.class.getSimpleName();

    //Declaration of handlers and definition of time and time steps
	private Handler handler, interfaceUpdateHandler;
	private final int mInterval        = 1000;                       // milliseconds
    private final int blockUpdateDelay = 500;                        // milliseconds
    private final long initTime        = System.currentTimeMillis(); // milliseconds

    //Logging
    Calendar cal=Calendar.getInstance(); //Note that January is 0 in JAVA
    private final String logFileName = "log" + String.format("%4d%02d%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1 ,cal.get(Calendar.DAY_OF_MONTH))
            + String.format("%02d%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)) + ".txt";

    //Declaration of the service client
	IMavLinkServiceClient mServiceClient;

    //Declaration of the connection button
	private Button connectButton;

    //Declaration of booleans
	private boolean isConnected       = false;
	private boolean isAltitudeUpdated = false;
    private boolean aircraftSelected  = false;
    private boolean showCommRange     = false;
    private boolean showCoverage      = false;

    //Declaration of the fragments
	private TelemetryFragment        telemetryFragment;
	private PerformanceScoreFragment performanceScoreFragment;
	private AltitudeTape             altitudeTapeFragment;
    private SupportMapFragment       mapFragment;
    private MissionButtonFragment    missionButtons;

    //Declaration of the lists/arrays that are used
    private SparseArray<Aircraft> mAircraft           = new SparseArray<>();
    private List<Polyline>      mConnectingLines      = new ArrayList<>();
    private ArrayList<Integer>  conflictingAircraft   = new ArrayList<>();
    private ArrayList<Integer>  sameLevelAircraft     = new ArrayList<>();
    private ArrayList<Integer>  groupList             = new ArrayList<>();
    private ArrayList<String>   conflictGroupList     = new ArrayList<>();
    private ArrayList<String>   sameLevelGroupList    = new ArrayList<>();
    private List<Integer>       groupSelectedAircraft = new ArrayList<>();;

	private Home home;
    private Circle homeCommCircle, relayCommCircle;
    private float verticalSeparationStandard, horizontalSeparationStandard, surveillanceCircleRadius;
    private int commMaxRange, commRelayRange, singleLabelVisibility, acCoverageRadius, ROIRadius;
    private String surveyWpName;
    private int selectedAc = 0;                     //Set to 0 if none serves as relay (yet)
    private int relayUAV   = 0;                     //Set to 0 if none serves as relay (yet)
    private double performanceScore = 0f;
    private int selectedWp = 0;

    //Declaration of items needed for mission blocks display
    private MenuItem menuBlockSpinner = null, menuAircraftSpinner = null;
    private Spinner blockSpinner, aircraftSpinner;
    Menu menu;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        //Instantiate the handlers
		handler = new Handler();                    //Handler for updating parameters from service
		interfaceUpdateHandler = new Handler();     //Handler for updating the user interface

        //Create a handle to the connect button
		connectButton = (Button) findViewById(R.id.connectButton);

        //Obtain values from resources
        verticalSeparationStandard = getResources().getInteger(R.integer.verticalSeparationStandard)/10f;       //(Divided by 10 to convert to meters)
        horizontalSeparationStandard = getResources().getInteger(R.integer.horizontalSeparationStandard)/10f;   //(Divided by 10 to convert to meters)
        surveillanceCircleRadius     = getResources().getInteger(R.integer.surveillanceCircleRadius)/10f;   //(Divided by 10 to convert to meters)
        commMaxRange = getResources().getInteger(R.integer.commMaxRange);                      //meters
        commRelayRange = getResources().getInteger(R.integer.commRelayRange);                    //meters
        acCoverageRadius = getResources().getInteger(R.integer.acCoverageRadius);                  //meters
        ROIRadius = getResources().getInteger(R.integer.ROIRadius);                         //meters
        surveyWpName =  getResources().getString(R.string.survey_wp);

        /* TODO Move the aircraft instantiation to a more suitable location once the service provides data of multiple aircraft (first Heartbeat?)*/
		// Instantiate aircraft object
        mAircraft.put(1, new Aircraft(getApplicationContext()));

		// Instantiate home object
		home = new Home();

        /* TODO Move home location setting to a better place when service provides the home location */
        // TEMPORARY SETTING OF HOME LOCATION
//        LatLng homeLocation = new LatLng(51.990826, 4.378248); //AE
//        LatLng homeLocation = new LatLng(43.563967, 1.481951); //ENAC
        LatLng homeLocation = new LatLng(43.46223, 1.27289); //airfield ENAC
        home.setHomeLocation(homeLocation);

        //TEMPORARY DUMMY AIRCRAFT (Remove this once the service provides data of multiple aircraft)
        mAircraft.put(2, new Aircraft(getApplicationContext()));
//        mAircraft.get(2).setLlaHdg(519925740, 43775620, 0, (short) 180);
        mAircraft.get(2).setLlaHdg(434622300, 12728900, 0, (short) 180);
        mAircraft.get(2).setAltitude(10);
        mAircraft.get(2).setBatteryState(10000, 45, 1);
        mAircraft.get(2).setDistanceHome(homeLocation);
		mAircraft.get(2).setRollPitchYaw(0, 0, 180);

		mAircraft.put(3, new Aircraft(getApplicationContext()));
		mAircraft.get(3).setLlaHdg(519910540, 43794130, 0, (short) 300);
//        mAircraft.get(3).setLlaHdg(519925740, 43775620, 0, (short) 180);
		mAircraft.get(3).setAltitude(10.3);
		mAircraft.get(3).setBatteryState(9000, 1, 1);
		mAircraft.get(3).setDistanceHome(homeLocation);
		mAircraft.get(3).setRollPitchYaw(0, 0, 300);

        mAircraft.put(4, new Aircraft(getApplicationContext()));
//        mAircraft.get(4).setLlaHdg(519920900, 43796160, 0, (short) 270);
        mAircraft.get(4).setLlaHdg(519880620, 43793190, 0, (short) 180);
        mAircraft.get(4).setAltitude(9.7);
        mAircraft.get(4).setBatteryState(12000, 90, 1);
        mAircraft.get(4).setDistanceHome(homeLocation);
        mAircraft.get(4).setRollPitchYaw(0, 0, 270);

		//Create a handles to the fragments
		telemetryFragment        = (TelemetryFragment) getSupportFragmentManager().findFragmentById(R.id.telemetryFragment);               //Telemetry fragment
        performanceScoreFragment = (PerformanceScoreFragment) getSupportFragmentManager().findFragmentById(R.id.performanceScoreFragment); //Battery fragment
		altitudeTapeFragment     = (AltitudeTape) getSupportFragmentManager().findFragmentById(R.id.altitudeTapeFragment);                 //AltitudeTape fragment
		missionButtons           = (MissionButtonFragment) getSupportFragmentManager().findFragmentById(R.id.missionButtonFragment);       //MissionButton fragment
		
		// Get the map and register for the ready callback
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Start the interface update handler
		interfaceUpdater.run();
	}

    //Menu instantiation
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

        ////////////BLOCKS SPINNER////////////
        //Set up the spinner in the action bar for the mission block which can be loaded from the service and create a handle
        menuBlockSpinner = menu.findItem(R.id.menu_block_spinner);
        blockSpinner = (Spinner) MenuItemCompat.getActionView(menuBlockSpinner);

        //Listener on item selection in the block spinner
        blockSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            //Define what should happen when an item in the spinner is selected
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    //Take the selected block index and send it to the service
                    Bundle carrier = new Bundle();
                    carrier.putString("TYPE", "BLOCK_SELECTED");
                    carrier.putShort("SEQ",(short)position);
                    mServiceClient.onCallback(carrier);
                } catch (RemoteException e) {
                    Log.e(TAG,"Error while sending mission block spinner selection to the service");
                }
            }

            //Define what should happen if no item is selected in the spinner
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Do nothing
            }
        });

        ////////////AIRCRAFT SPINNER////////////
        //Set up the spinner in the action bar for the aircraft and create a handle
        menuAircraftSpinner = menu.findItem(R.id.aircraft_selection_spinner);
        aircraftSpinner     = (Spinner) MenuItemCompat.getActionView(menuAircraftSpinner);

        //Listener on item selection in the spinner
        aircraftSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            //Define what should happen when an item in the spinner is selected
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Item selection implementation
            if(position>0) {                        //Select an aircraft
                    setIsSelected(position, true);
                } else {                            //Deselect aircraft
                    deselectAllAircraft();
                }
            }

            //Define what should happen if no item is selected in the spinner
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Do nothing
            }
        });

        //Temporarily fill the aircraft spinner here
        updateAircraftSpinner();

        this.menu = menu;
        return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.show_comm_range:
                //Show/hide the communication range on screen
                showCommRange = !showCommRange;
                return true;
            case R.id.show_coverage:
                showCoverage = !showCoverage;
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
            Log.e(TAG, "The service could not be bound");
        } else {
            Log.d(TAG, "Service was bound");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //Disconnect from service
        try{
            if (isConnected) {
                mServiceClient.disconnectDroneClient();
            }
        } catch(RemoteException e) {
            Log.e(TAG, "Failed to disconnect from service while closing application", e);
        }
    }

	@Override
    public void onDestroy() {
		super.onDestroy();
        //Stop the eventlistener
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

            //Draw the communication range on the map
            drawCommunicationRange(relayUAV);

            //check for altitude and course conflicts
            checkConflicts();

            //Update the altitude tape (if altitude is available)
            if (isAltitudeUpdated){
                updateAltitudeTape();
                isAltitudeUpdated = false;
            }

            //Clear the list of aircraft that are on the same level (after the altitude tape is updated)
            sameLevelAircraft.clear();

            //Update aircraft icons and display them on the map
            aircraftMarkerUpdater();

            //Draw the connecting lines on the map that indicate conflicts
            drawConnectingLines();

            //Calculate the current performance score
            calcPerformance();

            //Log data
            dataLogger();

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

    //Listener to the service for information updates
    private IEventListener.Stub listener = new IEventListener.Stub() {
    	@Override
		public void onConnectionFailed() {
    		Toast.makeText(getApplicationContext(), "Connection Failed!", Toast.LENGTH_SHORT).show();
    	}
    	
    	@Override
        public void onEvent(String type) {
    		switch(type) {
    			case "CONNECTED": {
    				isConnected = true;
    				updateConnectButton();
	    			break;
    			}   			
	
	    		case "HEARTBEAT_FIRST": {
                    updateHeartbeat();
	    			break;
	    		}
	    			
	    		case "DISCONNECTED": {
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

	    		/* TODO Enable the state service case once available or remove this */
//	    		case "STATE_UPDATED": {
//	    			updateState();
//	    			break;
//	    		}
	    		
	    		case "WAYPOINTS_UPDATED": {
	    			updateWaypoints();
	    			break;
	    		}

                case "MISSION_BLOCKS_UPDATED": {
                    updateMissionBlocks();
                    break;
                }

                case "CURRENT_BLOCK_UPDATED": {
                    updateMissionBlocksSelection();
                    break;
                }

                default:
	    			break;
    		}
    	}
    };
    
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
                    /* TODO HEARTBEAT use dynamic aircraft number once available in service */
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
                    /* TODO ATTITUDE use dynamic aircraft number once available in service */
                    mAircraft.get(1).setRollPitchYaw(Math.toDegrees(mAttitude.getRoll()), Math.toDegrees(mAttitude.getPitch()), Math.toDegrees(mAttitude.getYaw()));
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
                    /* TODO ALTITUDE use dynamic aircraft number once available in service */
                    //Note that in paparazzi the z-axis is defined pointing downwards, so a minus sign is applied to all incoming altitude values
                    mAircraft.get(1).setAltitude(-mAltitude.getAltitude());
                    mAircraft.get(1).setTargetAltitude(-mAltitude.getTargetAltitude());
//					mAircraft.get(1).setAGL(-mAltitude.getAGL());

                    telemetryFragment.setText(String.format("%.1f", -mAltitude.getAltitude()));
					
					/* Set isAltitudeUpdated to true at first update of altitude (used for altitude tape updates) */
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
                    /* TODO SPEED use dynamic aircraft number once available in service */
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
                    /* TODO BATTERY use dynamic aircraft number once available in service */
                    mAircraft.get(1).setBatteryState(mBattery.getBattVolt(), -1, mBattery.getBattCurrent());

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
                    /* TODO POSITION use dynamic aircraft number once available in service */
                    mAircraft.get(1).setSatVisible(mPosition.getSatVisible());
                    mAircraft.get(1).setLlaHdg(mPosition.getLat(), mPosition.getLon(), mPosition.getAlt(), (short) (mPosition.getHdg()/100));
                    mAircraft.get(1).setDistanceHome(home.getHomeLocation());
//                    Log.d("DATATESTlat",String.valueOf(mPosition.getLat()));
//                    Log.d("DATATESTlng",String.valueOf(mPosition.getLon()));
//                    Log.d("DATATESTalt",String.valueOf(mPosition.getAlt()));
                } catch (Throwable t) {
					Log.e(TAG, "Error while updating position", t);
				}
			}
		});
	}
	
//	/**
//	 * This runnable object is created to update state
//	 */
//	private void updateState() {
//		handler.post(new Runnable() {
//			@Override
//			public void run() {
//				try {
/* TODO STATE use dynamic aircraft number once available in service */
//					State mState = getAttribute("STATE");
//                    mAircraft.get(1).setIsFlying(mState.isFlying());
//                    mAircraft.get(1).setArmed(mState.isArmed());
//				} catch (Throwable t) {
//					Log.e(TAG, "Error while updating state", t);
//				}
//			}
//		});
//	}
	
	/**
	 * This runnable object is created to update waypoints
	 */
	private void updateWaypoints() {
		handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    //Get a list of waypoint objects from the service
                    Bundle carrier = mServiceClient.getAttribute("WAYPOINTS");
                    carrier.setClassLoader(Waypoint.class.getClassLoader());
                    List<Waypoint> waypoints = carrier.getParcelableArrayList("WAYPOINTS");

                     /* TODO WAYPOINTS use dynamic aircraft number once available in service */
                    //Clear the waypoint list if the aircraft already has waypoint data
                    if (mAircraft.get(1).getNumberOfWaypoints() > 0) {
                        mAircraft.get(1).clearWpList();
                    }

                    //Loop over the newly received waypoint list to add them individually to the list of the corresponding aircraft
                    for (int i = 0; i < waypoints.size(); i++) {
                        mAircraft.get(1).addWaypoint(Math.toDegrees(waypoints.get(i).getLat()), Math.toDegrees(waypoints.get(i).getLon()), waypoints.get(i).getAlt(), (short) waypoints.get(i).getSeq(), waypoints.get(i).getTargetSys(), waypoints.get(i).getTargetComp());

                        //Filter survey waypoints
                        //If wpName contains(surveyWpName), save it in a surveyWpList in the aircraft class
                    }

                    //Call the method that shows the waypoints on the map
                    waypointUpdater(1);

                    /* TODO update the waypoint button after the waypoints of all aircraft are handled */
                    missionButtons.updateWaypointsButton();

                } catch (Throwable t) {
                    Log.e(TAG, "Error while updating waypoints", t);
                }
            }
        });
	}

    /**
     * This runnable object is created to update mission blocks
     */
    private void updateMissionBlocks() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    /* TODO BLOCKS use dynamic aircraft number once available in service */
                    //Store the mission block list
                    Bundle carrier = mServiceClient.getAttribute("BLOCKS");
                    mAircraft.get(1).missionBlocks = carrier.getStringArrayList("BLOCKS");

                    //Update the blocks request button
                    missionButtons.updateBlocksButton();

                    //Update the dropdown menu with the block names
                    updateMissionBlocksSpinner();
                } catch (RemoteException e) {
                    Log.e(TAG, "Error while updating mission blocks");
                }
            }
        });
    }

    //Method to update the mission block dropdown menu
    private void updateMissionBlocksSpinner() {
        //Create an array adapter with the mission block names
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mAircraft.get(1).missionBlocks);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Apply the array adapter to the block spinner to update the blocks in the dropdown menu
        blockSpinner.setAdapter(spinnerArrayAdapter);
    }

    //Method to update the selected block in the dropdown menu to the active one
    private void updateMissionBlocksSelection() {

        if (mAircraft.get(1).missionBlocks.size() > 0) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        /* TODO BLOCKS use dynamic aircraft number once available in service */
                        /* TODO make sure that the initial block selection is the actual one and not the initial block (0) of the declaration in the service */
                        // Get current block
                        Bundle carrier = mServiceClient.getAttribute("CURRENT_BLOCK");
                        int currentBlock = carrier.getInt("CURRENT_BLOCK");

                        //Update the Mission block spinner selection
                        blockSpinner.setSelection(currentBlock);

                        //Set current block to aircraft
                        mAircraft.get(1).setCurrentBlock(currentBlock);

                        //Update the status of the mission buttons
                        missionButtons.updateExecutedMissionButton(mAircraft.get(1).missionBlocks.get(currentBlock));
                    } catch (RemoteException e) {
                        Log.e(TAG, "Error while trying to update the mission block selection in the spinner");
                    }
                }
            });
        }
    }

    //Method to update the mission block dropdown menu
    private void updateAircraftSpinner() {
        List<String> aircraftList = new ArrayList<>();

        //Add the initial value to the spinner
        aircraftList.add(getResources().getString(R.string.no_aircraft_selected));

        for(int i=1; i<=mAircraft.size(); i++) {
            aircraftList.add("Aircraft " + mAircraft.get(i).getLabelCharacter());
        }

        //Create an array adapter with the mission block names
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, aircraftList);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Apply the array adapter to the block spinner to update the blocks in the dropdown menu
        aircraftSpinner.setAdapter(spinnerArrayAdapter);
    }


	////////OTHER COMMUNICATION FUNCTIONS
	
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
	        	Log.e(TAG, "Error while connecting to service", e);
	        }
        }
    }
    //Connect to the service on button click if no connection was established yet
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
            Log.e(TAG,"Error while loading parcel from service");
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
            	
//            case "STATE":
//            	return (T) new State();
            	
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

//			case "STATE":
//				return State.class.getClassLoader();

			default:
				return null;
		}
	 }
	
	/////////////////////MISSION BUTTONS/////////////////////

    //Method to handle land button clicks
	public void onLandRequest(View v) {
        if(selectedAc == 0 ) {
            //Notify the user that no aircraft is selected
            Toast.makeText(getApplicationContext(), "No aircraft selected!", Toast.LENGTH_SHORT).show();
            //request to land if connected and the mission blocks are loaded
        } else if(isConnected && mAircraft.get(selectedAc).missionBlocks != null) {
            //Select the land block and request the service to execute it
            try {
                Bundle carrier = new Bundle();
                carrier.putString("TYPE", "BLOCK_SELECTED");
                carrier.putShort("SEQ", (short) mAircraft.get(selectedAc).missionBlocks.indexOf(getResources().getString(R.string.land_block)));
                mServiceClient.onCallback(carrier);
            } catch (RemoteException e) {
                Log.e(TAG, "Error while requesting the service to execute the land block");
            }
        }
	}

    //Method to handle take-off button clicks
	public void onTakeOffRequest(View v) {
        if(selectedAc == 0 ) {
            //Notify the user that no aircraft is selected
            Toast.makeText(getApplicationContext(), "No aircraft selected!", Toast.LENGTH_SHORT).show();
        //request takeoff if connected and the mission blocks are loaded
        } else if(isConnected && mAircraft.get(selectedAc).missionBlocks != null) {
            //Select the takeoff block and request the service to execute it
            try {
                Bundle carrier = new Bundle();
                carrier.putString("TYPE","BLOCK_SELECTED");
                carrier.putShort("SEQ",(short) mAircraft.get(selectedAc).missionBlocks.indexOf(getResources().getString(R.string.takeoff_block)));
                mServiceClient.onCallback(carrier);

            } catch (RemoteException e) {
                Log.e(TAG,"Error while requesting the service to execute the takeoff block");
            }
        }
	}

    //Method to handle home button clicks
	public void onGoHomeRequest(View v) {
        /* TODO Try to execute one of the go-home/landing blocks. Check if the drone is going home and update the button that is displayed.*/
        if(selectedAc == 0 ) {
            //Notify the user that no aircraft is selected
            Toast.makeText(getApplicationContext(), "No aircraft selected!", Toast.LENGTH_SHORT).show();
            //request to go home if connected and the mission blocks are loaded
        } else if(isConnected && mAircraft.get(selectedAc).missionBlocks != null) {
            //Select the go home block and request the service to execute it
//            try {
//                Bundle carrier = new Bundle();
//                carrier.putString("TYPE","BLOCK_SELECTED");
//                carrier.putShort("SEQ",(short) mAircraft.get(selectedAc).missionBlocks.indexOf(getResources().getString(R.string.GOHOME)));
//                mServiceClient.onCallback(carrier);

//            } catch (RemoteException e) {
//                Log.e(TAG,"Error while requesting the service to execute the go home block");
//            }
        }
	}


    public void onBlocksRequest(View v) {
        missionButtons.onBlocksRequest(v);

        //Request mission blocks if connected to the service
        if (isConnected) {
            try {
                Bundle carrier = new Bundle();
                carrier.putString("TYPE", "REQUEST_BLOCK_LIST");
                mServiceClient.onCallback(carrier);

                Toast.makeText(getApplicationContext(), "Requesting blocks", Toast.LENGTH_SHORT).show();
            } catch (RemoteException e) {
                Log.e(TAG,"Error while requesting mission blocks");
            }
        }
    }

    //Method to handle waypoint and block request button clicks
    public void onWaypointsRequest(View v) {
        missionButtons.onWaypointsRequest(v);

        //Request the service to send (updated) waypoints if connected
        if (isConnected) {
            try {
                Bundle carrier = new Bundle();
                carrier.putString("TYPE", "REQUEST_WP_LIST");
                mServiceClient.onCallback(carrier);

                Toast.makeText(getApplicationContext(), "Requesting Waypoints.", Toast.LENGTH_SHORT).show();
            } catch (RemoteException e) {
                Log.e(TAG, "Error while requesting waypoints");
            }
        }
    }

    //Method to update the mission button appearance to selected aircraft properties
    private void updateMissionButtons() {
        if(selectedAc==0 || mAircraft.get(selectedAc).getCurrentBlock()==null) {
            missionButtons.updateExecutedMissionButton("");
        } else {
            //Get the block name that is currently active for the selected aircraft and send this to the mission button fragment to update the buttons
            missionButtons.updateExecutedMissionButton(mAircraft.get(selectedAc).getCurrentBlock());
        }
    }

	/////////////////////////MAPS/////////////////////////
	
	/* First time the map is ready: Initial settings. */
	@Override
	public void onMapReady(GoogleMap map) {
				
		//Change the map type to satellite
		map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
		
		//Enable the go to my location button
		map.setMyLocationEnabled(true);

        /* TODO: make the initial zoom level to correspond with the Region of Interest size (so it does not fill the entire screen) */
        //Move camera to the home location
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(home.getHomeLocation(),18.0f));
		
		//Disable rotation and tilt gestures
		map.getUiSettings().setRotateGesturesEnabled(false);
		map.getUiSettings().setTiltGesturesEnabled(false);
		
		//Show my location button
		map.getUiSettings().setMyLocationButtonEnabled(true);
		
		//Enable marker/infowindow listeners
		map.setOnMarkerClickListener(this);     //Click listener on markers
        map.setOnMarkerDragListener(this);      //Drag listener on markers
        map.setOnInfoWindowClickListener(this); //Click listener on infowindows

        /* TODO move home marker drawing to the switch for incoming data when the service sends home information */
        drawHomeMarker();

        //Draw the Region Of Interest on the map (Used for test scenario)
        drawROI();
	}

	/* Marker listener for (de)selecttion aircraft icons, waypoint marker actions and home marker selection */
	@Override
    public boolean onMarkerClick(final Marker marker) {
//        if(marker.getSnippet().contains("-") && aircraftSelected){      //Waypoint marker clicked
        if(marker.getSnippet().contains("-")){      //Waypoint marker clicked
            String[] numbers = marker.getSnippet().split("-");
            int acNumber = Integer.parseInt(numbers[0]);
            int wpNumber = Integer.parseInt(numbers[1]);

            //Set the number of the selected waypoint (0 for nothing selected)
            if((wpNumber+1)==selectedWp) { //Deselect waypoint
                selectedWp = 0;
                //Set the selection status of the waypoint
                mAircraft.get(acNumber).setWpSelected(wpNumber, false);
            } else {
                if(selectedWp!=0) {
                    //If another waypoint is selected, deselect it
                    mAircraft.get(acNumber).setWpSelected(selectedWp-1, false);
                }
                selectedWp = wpNumber + 1;
                //Set the selection status of
                mAircraft.get(acNumber).setWpSelected(wpNumber, true);
            }

            //Redraw the waypoints (important for selection status)
            waypointUpdater(acNumber);

            //Activate the block that corresponds with the chosen waypoint
//            executeWaypointBlock(wpNumber);
        } else if(marker.getSnippet().equals("HOME")) {                 //Home marker clicked
            //Do nothing (yet)
        } else if(!marker.getSnippet().contains("-")) {                 //Aircraft marker clicked
            int acNumber = Integer.parseInt(marker.getSnippet());

            //When the aircraft icon is clicked, select it or deselect it
            if(!mAircraft.get(acNumber).isSelected()) {
                if(aircraftSelected) {
                    //Set all aircraft on not selected
                    deselectAllAircraft();
                }
                mAircraft.get(acNumber).setIsSelected(true);
                //Keep track of which aircraft is selected
                selectedAc = acNumber;
                aircraftSelected = true;
                //Set the aircraft spinner to the selected aircraft
                aircraftSpinner.setSelection(acNumber);
                //Set info window show status for next map draw iteration
                mAircraft.get(acNumber).setShowInfoWindow(true);
            } else {
                mAircraft.get(acNumber).setIsSelected(false);
                selectedAc = 0;
                aircraftSelected = false;
                //Set the aircraft spinner to nothing selected
                aircraftSpinner.setSelection(0);
            }
            //Update the mission buttons
            updateMissionButtons();
            //Update the map
            aircraftMarkerUpdater();
        }
		return true;
	}

    /* Marker drag listener action implementations (mainly for moving waypoints) */
    @Override
    public void onMarkerDragStart(Marker marker) {
        //Do nothing
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        //Do nothing
    }

    //Action implementation on end of marker (waypoint) drag
    @Override
    public void onMarkerDragEnd(Marker marker) {
        //Get the marker snippet to extract the aircraft- and waypoint number
        String[] numbers = marker.getSnippet().split("-");
        int acNumber = Integer.parseInt(numbers[0]);
        int wpNumber = Integer.parseInt(numbers[1]);
        //Get the drop location
        LatLng newPosition = marker.getPosition();

        try {
            Bundle carrier = new Bundle();
            carrier.putString("TYPE", "WRITE_WP");
            carrier.putParcelable("WP", new Waypoint((float) Math.toRadians(newPosition.latitude), (float) Math.toRadians(newPosition.longitude), mAircraft.get(acNumber).getWpAlt(wpNumber), mAircraft.get(acNumber).getWpSeq(wpNumber), mAircraft.get(acNumber).getWpTargetSys(wpNumber), mAircraft.get(acNumber).getWpTargetComp(wpNumber)));
            mServiceClient.onCallback(carrier);
        } catch(RemoteException e) {
                Log.e(TAG, "Error while sending waypoint to the service");
        }

        /* TODO Remove these lines when the service sends the updated waypoints after setting one */
        mAircraft.get(acNumber).setWpLatLon((float) newPosition.latitude, (float) newPosition.longitude, wpNumber);
        //Update the waypoints on the map
        waypointUpdater(acNumber);
        //Set the waypoint status to be updating
        mAircraft.get(acNumber).setWpUpdating(wpNumber);
    }

    //Info window click listener to hide it
    @Override
    public void onInfoWindowClick(final Marker marker) {

        //Get the aircraft number that corresponds with the infowindow
        int clickedAircraft = Integer.parseInt(marker.getSnippet());

        //Hide the window immediately
        marker.hideInfoWindow();
        //Set info window show status for next map draw iteration
        mAircraft.get(clickedAircraft).setShowInfoWindow(false);
    }

	/* Update the objects that are displayed on the map */
	public void aircraftMarkerUpdater(){

        // Loop over all aircraft to generate their markers
        for(int acNumber=1; acNumber<mAircraft.size()+1; acNumber++) {
            //Determine the color of the aircraft icon based on selection status
            if(mAircraft.get(acNumber).isSelected()) {
                mAircraft.get(acNumber).setCircleColor(getResources().getColor(R.color.yellow));
            } else {
                mAircraft.get(acNumber).setCircleColor(Color.WHITE);
            }

            //Generate an icon
            mAircraft.get(acNumber).generateIcon();
        }

        //Call GoogleMaps
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {

                //Loop over all aircraft
                for(int aircraftNumber=1; aircraftNumber<mAircraft.size()+1; aircraftNumber++) {

                    //Clear marker from map (if it exists)
                    if (mAircraft.get(aircraftNumber).acMarker != null) {
                        mAircraft.get(aircraftNumber).acMarker.remove();
                    }

                    //Add marker to map with the following settings and save it in the aircraft object
                    mAircraft.get(aircraftNumber).acMarker = map.addMarker(new MarkerOptions()
                                    .position(mAircraft.get(aircraftNumber).getLatLng())
                                    .anchor((float) 0.5, (float) 0.5)
                                    .icon(BitmapDescriptorFactory.fromBitmap(mAircraft.get(aircraftNumber).getIcon()))
                                    .title(" " + mAircraft.get(aircraftNumber).getLabelCharacter())
                                    .snippet(String.valueOf(aircraftNumber))
                                    .infoWindowAnchor(0.5f, mAircraft.get(aircraftNumber).getIconBoundOffset())
                                    .flat(true)
                                    .draggable(false)
                    );

                    //Either show the label or the detailed information window of the aircraft based on selection status
                    if (mAircraft.get(aircraftNumber).isSelected()) {
                        //Make aircraft number final to use in inner class
                        final int acNumber = aircraftNumber;

                        //Adapt the information window
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

                                //Get handles to the textviews
                                TextView infoAirtime  = (TextView) v.findViewById(R.id.info_airtime);
                                TextView infoDistHome = (TextView) v.findViewById(R.id.info_dist_home);
                                TextView infoAlt      = (TextView) v.findViewById(R.id.info_alt);
                                TextView infoMode     = (TextView) v.findViewById(R.id.info_mode);
                                TextView infoBattery  = (TextView) v.findViewById(R.id.info_battery);

                                //Set the values in the information windows
                                infoAirtime.setText("Airtime: " + "AIRTIME HERE!");
                                infoDistHome.setText("Distance Home: " + String.format("%.1f", mAircraft.get(acNumber).getDistanceHome()) + "m");
                                infoAlt.setText("Altitude: " + String.format("%.1f", mAircraft.get(acNumber).getAltitude()) + "m");
                                infoBattery.setText("Battery voltage: " + String.valueOf(mAircraft.get(acNumber).getBattVolt()) + "mV");
                                if(mAircraft.get(acNumber).getCurrentBlock()==null) {
                                    infoMode.setText("Current block: " + getResources().getString(R.string.no_blocks_loaded));
                                } else {
                                    infoMode.setText("Current block: " + mAircraft.get(acNumber).getCurrentBlock());
                                }

                                return v;
                            }
                        });

                        //Set the marker to show the information window
                        if(mAircraft.get(aircraftNumber).getShowInfoWindow()) {
                            mAircraft.get(aircraftNumber).acMarker.showInfoWindow();
                        }
                    }

                    ////COVERAGE INDICATION
                    //Remove the coverage circle if it was drawn before
                    if (mAircraft.get(aircraftNumber).coverageCircle != null) {
                        mAircraft.get(aircraftNumber).coverageCircle.remove();
                    }

                    if (showCoverage) {
    //                    if(mAircraft.get(aircraftNumber).getCurrentSurveyLoc()!= null) {

                        //Exclude relay UAVs (filter based on altitude or status)
                        //Make dynamic (multiple aircraft)

                        // Draw the relay communication range circle
                        //Bitmap and canvas to draw a circle on
                        Bitmap baseIcon = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
                        Canvas circleCanvas = new Canvas(baseIcon);
                        Paint circlePaint = new Paint();
                        circlePaint.setColor(0x4000ff00);
                        circlePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
                        //Circle fill
                        circleCanvas.drawCircle(200, 200, 200, circlePaint);

                        //Circle stroke
                        circlePaint.setStyle(Paint.Style.STROKE);
                        circlePaint.setColor(Color.BLACK);
                        circleCanvas.drawCircle(200, 200, 200, circlePaint);

                        GroundOverlayOptions ROI = new GroundOverlayOptions()
                                .image(BitmapDescriptorFactory.fromBitmap(baseIcon))
    //                            .position(mAircraft.get(1).getCurrentSurveyLoc(), acCoverageRadius * 2, acCoverageRadius * 2); //m
                                .position(mAircraft.get(aircraftNumber).getLatLng(), acCoverageRadius*2, acCoverageRadius*2); //m

                        // Get back the relay Circle object
                        mAircraft.get(aircraftNumber).coverageCircle = map.addGroundOverlay(ROI);
    //                    }
                    }
                }
            }
        });
	}

    /* Update the waypoint markers that are displayed on the map */
    private void waypointUpdater(final int acNumber) {

        //Call GoogleMaps
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {

                //If the waypoints are already initiated, remove them from the map and clear the list that holds them
                if (!mAircraft.get(acNumber).wpMarkers.isEmpty()) {
                    //Remove markers from map
                    for (int i = 0; i < mAircraft.get(acNumber).getNumberOfWaypoints(); i++) {
                        mAircraft.get(acNumber).wpMarkers.get(i).remove();
                    }

                    //Clear the marker list (not the marker data)
                    mAircraft.get(acNumber).wpMarkers.clear();
                }

                //(Re)generate waypoint markers
                for (int i = 0; i < mAircraft.get(acNumber).getNumberOfWaypoints(); i++) {
                    //Custom wp marker with text (aircraft label + wp number)
                    Bitmap wpMarkerBitmap;
                    if(mAircraft.get(acNumber).isWpSelected(i)) {
                        wpMarkerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.wp_map_selected).copy(Bitmap.Config.ARGB_8888, true);
                    } else if (mAircraft.get(acNumber).isWpUpdating(i)) {
                        wpMarkerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.wp_map_updating).copy(Bitmap.Config.ARGB_8888, true);
                    } else {
                        wpMarkerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.wp_map).copy(Bitmap.Config.ARGB_8888, true);
                    }

                    Canvas markerCanvas = new Canvas(wpMarkerBitmap);
                    Paint markerPaint = new Paint();
                    markerPaint.setTextSize(22);
                    markerPaint.setFakeBoldText(true);
                    markerPaint.setColor(Color.BLACK);
                    markerPaint.setTextAlign(Paint.Align.CENTER);
//                    markerCanvas.drawText(mAircraft.get(acNumber).getLabelCharacter() + String.valueOf(i + 1), wpMarkerBitmap.getWidth() / 2, wpMarkerBitmap.getHeight() / 2 + 4, markerPaint);
                    markerCanvas.drawText(mAircraft.get(acNumber).getLabelCharacter() + String.valueOf(mAircraft.get(acNumber).getWpSeq(i)), wpMarkerBitmap.getWidth() / 2, wpMarkerBitmap.getHeight() / 2 + 4, markerPaint);

//                    LatLng pos = new LatLng(mAircraft.get(acNumber).getWpLat(i)* 1e-7, mAircraft.get(acNumber).getWpLon(i)* 1e-7);

                    //Add waypoint marker to map
                    Marker wpMarker = map.addMarker(new MarkerOptions()
                                    .position(mAircraft.get(acNumber).getWpLatLng(i))
                                    .flat(true)
                                    .icon(BitmapDescriptorFactory.fromBitmap(wpMarkerBitmap))
                                    .snippet(String.valueOf(acNumber) + "-" + String.valueOf(mAircraft.get(acNumber).getWpSeq(i)))
                                    .draggable(true)
                    );
                    //Add the newly generated waypoint marker to the list to keep reference to it
                    mAircraft.get(acNumber).wpMarkers.add(wpMarker);
                }

                ///* FLIGHT PATH (lines between waypoints) *///

            /* TODO Distinguish pattern- and relay waypoints using color and draw circles around the waypoints to show the flightpath of the aircraft

            // If the flight path has been drawn before, remove it to be updated
            if (mAircraft.get(aircraftNumber).flightPath != null) {
                mAircraft.get(aircraftNumber).flightPath.remove();
            }

            // Draw the flight path with the specified characteristics
            mAircraft.get(aircraftNumber).flightPath = map.addPolyline(new PolylineOptions()
                    .addAll(mAircraft.get(aircraftNumber).getWpLatLngList())
                    .width(4)
                    .color(Color.WHITE));
            */
            }
        });
    }

    /* Draw home marker on the map */
    private void drawHomeMarker() {
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
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
        });
    }

    /* Draw the area in which communication is possible */
    private void drawCommunicationRange(final int relayAc) {

        //Only call the map if something needs to be drawn or if something needs to be removed from the map
        if(showCommRange) {
            //Call GoogleMaps
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap map) {

                    //Remove the communication range circle around home if it was drawn before
                    if (homeCommCircle != null) {
                        homeCommCircle.remove();
                    }
                    //Remove the communication range circle around the relay aircraft if it was drawn before
                    if (relayCommCircle != null) {
                        relayCommCircle.remove();
                    }

                    //Add the home communication range circle to the map
                    CircleOptions homeCircleOptions = new CircleOptions()
                            .center(home.getHomeLocation())
                            .strokeWidth(5)
                            .strokeColor(0x5500ff00)
                            .radius(commMaxRange); // In meters

                    // Get back the home Circle object
                    homeCommCircle = map.addCircle(homeCircleOptions);

                    //If a relay UAV is active
                    if (relayAc != 0) {

                        // Draw the relay commincation range circle
                        CircleOptions relayCircleOptions = new CircleOptions()
                                .center(mAircraft.get(relayAc).getLatLng())
                                .strokeWidth(5)
                                .strokeColor(0x5500ff00)
                                .radius(commRelayRange); // In meters

                        // Get back the relay Circle object
                        relayCommCircle = map.addCircle(relayCircleOptions);
                    }
                }
            });
        //If no communication ranges need to be drawn, remove them
        } else {
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap map) {

                    //Remove the communication range circle around home if it was drawn before
                    if (homeCommCircle != null) {
                        homeCommCircle.remove();
                    }
                    //Remove the communication range circle around the relay aircraft if it was drawn before
                    if (relayCommCircle != null) {
                        relayCommCircle.remove();
                    }
                }
            });
        }
    }

    ///////* Indicate a region of interest on the map *///////
    private void drawROI() {

        //Call GoogleMaps
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {

                LatLng ROIloc = home.getHomeLocation();

                //Bitmap and canvas to draw a circle on
                int circleSize = 300;
                Bitmap baseIcon = Bitmap.createBitmap(circleSize, circleSize, Bitmap.Config.ARGB_8888);
                Canvas circleCanvas = new Canvas(baseIcon);
                Paint circlePaint = new Paint();
                circlePaint.setColor(0x400099CC);
                circlePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
                circleCanvas.drawCircle(circleSize/2, circleSize/2, circleSize/2, circlePaint);

                GroundOverlayOptions ROI = new GroundOverlayOptions()
                        .image(BitmapDescriptorFactory.fromBitmap(baseIcon))
                        .position(ROIloc, ROIRadius*2, ROIRadius*2); //m

                map.addGroundOverlay(ROI);
            }
        });
    }

    ///////* Draw the area of the map that is covered by the aircraft *///////


    ///////* Connecting lines to indicate conflicting aircraft pairs *///////
    private void drawConnectingLines() {

        //Call GoogleMaps
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {

            //If lines have been drawn before, remove them from the map and clear the list with references to them
            if (!mConnectingLines.isEmpty()) {
                for (int i = 0; i < mConnectingLines.size(); i++) {
                    mConnectingLines.get(i).remove();
                }
                mConnectingLines.clear();
            }

            //If conflicts exist, draw the conflict lines
            if(conflictingAircraft != null) {
                for (int i = 0; i < conflictingAircraft.size(); i += 2) {

                    //Draw a connecting line on the map with the following settings
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

    /////////////////////////CLASS METHODS/////////////////////////

    private void checkConflicts() {
        //check for altitude and course conflicts
        for(int i = 1; i < mAircraft.size()+1; i++) {
            for(int j = 1; j < mAircraft.size()+1; j++) {
                if(i!=j) {
                    if(Math.abs(mAircraft.get(i).getAltitude() - mAircraft.get(j).getAltitude()) <= verticalSeparationStandard) {
                        //Check for conflict course
                        if(isOnconflictCourse(i,j)){ //On conflict course
                            conflictingAircraft.add(i);
                            conflictingAircraft.add(j);
                        } else { //Not on conflict course
                            sameLevelAircraft.add(i);
                            sameLevelAircraft.add(j);
                        }
                    } else {
                        mAircraft.get(i).setConflictStatusNew(ConflictStatus.GRAY);
                        mAircraft.get(j).setConflictStatusNew(ConflictStatus.GRAY);
                    }
                }
            }
        }

        // Remove double couples from the conflictlist
        if (conflictingAircraft.size()>2) {
            //Loop starting at the end of the list to be able to remove double pairs
            for (int p=conflictingAircraft.size()-2; p>1; p-=2) {
                for (int q = 0; q < p; q += 2) {
                    if (conflictingAircraft.get(p+1) == conflictingAircraft.get(q) && conflictingAircraft.get(p) == conflictingAircraft.get(q+1)) {
                        conflictingAircraft.remove(p+1);
                        conflictingAircraft.remove(p);
                        break;
                    }
                }
            }
        }

        // Remove double couples from the samelevellist
        if (sameLevelAircraft.size()>2) {
            //Loop starting at the end of the list to be able to remove double pairs
            for (int r=sameLevelAircraft.size()-2; r>1; r-=2) {
                for (int s=0; s<r; s+=2) {
                    if (sameLevelAircraft.get(r+1) == sameLevelAircraft.get(s) && sameLevelAircraft.get(r) == sameLevelAircraft.get(s+1)) {
                        sameLevelAircraft.remove(r+1);
                        sameLevelAircraft.remove(r);
                        break;
                    }
                }
            }
        }

        //Set the color of the aircraft that have the "red" conflict status
        if(!conflictingAircraft.isEmpty()) {
            for (int k = 0; k < conflictingAircraft.size(); k+=2) {
                mAircraft.get(conflictingAircraft.get(k)).setConflictStatusNew(ConflictStatus.RED);
                mAircraft.get(conflictingAircraft.get(k + 1)).setConflictStatusNew(ConflictStatus.RED);
            }

            //Get unique aircraft (remove duplicates)
            Set<Integer> uniqueAcRed = new HashSet<Integer>(conflictingAircraft);
            for (Integer l : uniqueAcRed) {
                groupList.add(l);
                for (Integer m : uniqueAcRed) {
                    if(l!=m) {
                        if(Math.abs(mAircraft.get(l).getAltitude() - mAircraft.get(m).getAltitude()) <= verticalSeparationStandard) {
                            groupList.add(m);
                        }
                    }
                }
                //Sort the list of conflicting aircraft
                Collections.sort(groupList);
                //Generate a string containing the aircraftnumbers of a conflict group
                String set = "";
                for(int d=0; d<groupList.size(); d++) {
                    set = set + String.valueOf(groupList.get(d));
                }

                //Check if the conflict group already exists in the list, if not add it (this code creates groups for all seperate conflict groups, the result is a huge load of labels in some cases)
                if(!conflictGroupList.contains(set)) {
                    conflictGroupList.add(set);
                }

//                    if(conflictGroupList.isEmpty()) {
//                        conflictGroupList.add(set);
//                    } else {
//                        Boolean inGroup = false;
//                        for(int i=0; i<conflictGroupList.size(); i++) {
//                            if(conflictGroupList.get(i).contains(set)) {
//                                inGroup = true;
//                                break;
//                            }
//                        }
//                        if(!inGroup) {
//                            conflictGroupList.add(set);
//                        }
//                    }
//                    Log.d("test",String.valueOf(conflictGroupList.size()));

                //Clear the temporary list
                groupList.clear();
            }
        }

        //Set the color of the aircraft that have the "blue" conflict status
        if(!sameLevelAircraft.isEmpty()) {
            for (int k = 0; k < sameLevelAircraft.size(); k+=2) {
                mAircraft.get(sameLevelAircraft.get(k)).setConflictStatusNew(ConflictStatus.BLUE);
                mAircraft.get(sameLevelAircraft.get(k+1)).setConflictStatusNew(ConflictStatus.BLUE);
            }

            //Get unique aircraft (remove duplicates)
            Set<Integer> uniqueAcBlue = new HashSet<Integer>(sameLevelAircraft);
            for (Integer l : uniqueAcBlue) {
                groupList.add(l);
                for (Integer m : uniqueAcBlue) {
                    if(l!=m) {
                        if(Math.abs(mAircraft.get(l).getAltitude() - mAircraft.get(m).getAltitude()) <= verticalSeparationStandard) {
                            groupList.add(m);
                        }
                    }
                }
                //Sort the list of conflicting aircraft
                Collections.sort(groupList);
                //Generate a string containing the aircraftnumbers of a conflict group
                String set = "";
                for(int d=0; d<groupList.size(); d++) {
                    set = set + String.valueOf(groupList.get(d));
                }
                //Check if the conflict group already exists in the list, if not add it
//                    if(!sameLevelGroupList.contains(set)) {
//                        sameLevelGroupList.add(set);
//                    }

                ///////////////////////////////////////////////////////
                if(sameLevelGroupList.isEmpty()) {
                    sameLevelGroupList.add(set);
                } else {
                    Boolean inGroup = false;
                    int i;
                    mainLoop:   //Label for breaking out of two nested loops
                    for(i=0; i<sameLevelGroupList.size(); i++) {
                        for(int j=0; j<set.length(); j++) {
                            if(sameLevelGroupList.get(i).contains(Character.toString(set.charAt(j)))) {
//                                    Log.d("AAP1",set);
//                                    set.replace(Character.toString(set.charAt(j)), "");
//                                    Log.d("AAP2",set);
//                                    Log.d("aapje",sameLevelGroupList.get(i));
                                inGroup = true;
                                break mainLoop;
                            }
                        }
                    }

                    if(inGroup) { //If one of the aircraft in the set is already present in another group, add the missing one to this group
//                            Log.d("AAP",sameLevelGroupList.get(i));
                    } else {
                        sameLevelGroupList.add(set);
                    }
                }
                for(int i=0; i< sameLevelGroupList.size(); i++) {
//                    Log.d("test", sameLevelGroupList.get(i));
                }

                //Clear the temporary list
                groupList.clear();
            }
        }
    }

    //Method to determine if a couple of aircraft needs to get conflict status
    private boolean isOnconflictCourse(int ac1, int ac2) {
        /* TODO make sure the conflict status is given for small coverage overlaps */

        boolean isInconflictcourse = false;

//        if(mAircraft.get(ac1).getCurrentSurveyLoc() != null && mAircraft.get(ac2).getCurrentSurveyLoc() != null) {
//
//            //Calculate the distance between the two survey waypoints
//            float[] distance = new float[1];
//            Location.distanceBetween(mAircraft.get(ac1).getCurrentSurveyLoc().latitude, mAircraft.get(ac1).getCurrentSurveyLoc().longitude, mAircraft.get(ac2).getCurrentSurveyLoc().latitude, mAircraft.get(ac2).getCurrentSurveyLoc().longitude, distance);
//
//            //Detect conflict if the distance between the waypoints is less than the survey circle diameter + the horizontal separation standard
//            if(distance[0] <= (surveillanceCircleRadius*2 + horizontalSeparationStandard)) { isInconflictcourse = true; };
//        }

        return isInconflictcourse;
    }

	/////////////////////////ALTITUDE TAPE/////////////////////////

    //Method to update the labels on the altitude tape
	public void updateAltitudeTape() {

        /* TODO move the targetalttiude indication to the for loop */
        /* TODO make the aircraft number dynamic */
		/* Set the location of the target label on the altitude tape and check wether to 
		 * show the target label or not (aka is the aircraft already on target altitude?) */
        if (Math.abs(mAircraft.get(1).getTargetAltitude() - mAircraft.get(1).getAltitude()) > 0.001) {
//            altitudeTapeFragment.setTargetLabel(mAircraft.get(1).getTargetAltitude(), mAircraft.get(1).getTargetLabelId());
        } else {
            altitudeTapeFragment.deleteTargetLabel(mAircraft.get(1).getTargetLabelId());
        }

        //Boolean to check if group labels are drawn and if they need to be removed from the tape
        boolean groupLabelsDrawn = false;

        //Check if a certain conflict is selected. If yes, do not draw the group label but draw single labels on the left side of the tape.
        //If a group is selected
        if(!groupSelectedAircraft.isEmpty()) {
            for(int i=0; i< groupSelectedAircraft.size(); i++) {
                altitudeTapeFragment.drawGroupSelection(mAircraft.get(groupSelectedAircraft.get(i)).getAltitude(),mAircraft.get(groupSelectedAircraft.get(i)).getLabelCharacter(),i,groupSelectedAircraft.size(),groupSelectedAircraft.get(i));
            }
        } else {
            // Put the grouped red labels on the altitude tape
            if (!conflictGroupList.isEmpty()) {
                for (int j = 0; j < conflictGroupList.size(); j++) { //Loop over conflict groups
                    int[] conflict = new int[conflictGroupList.get(j).length()];
                    double altSum = 0;
                    String characters = "";
                    for(int h = 0; h<conflictGroupList.get(j).length();h++){//Loop over conflict group string
                        conflict[h] = Character.getNumericValue(conflictGroupList.get(j).charAt(h));
                        altSum = altSum + mAircraft.get(conflict[h]).getAltitude();
                        characters = characters + mAircraft.get(conflict[h]).getLabelCharacter() + " ";
                    }
                    characters = characters.substring(0,conflict.length+2);
                    //Calculate the mean altitude of the aircraft that are in the group
                    double meanAlt = altSum/conflict.length;
                    //Draw the group label on the tape
                    altitudeTapeFragment.drawGroupLabel(true, meanAlt, characters, conflict);
                }
                conflictGroupList.clear();
                groupLabelsDrawn = true;
            }

            // Put the grouped blue labels on the altitude tape
            if (!sameLevelGroupList.isEmpty()) {
                for (int k = 0; k < sameLevelGroupList.size(); k++) { //Loop over conflict groups
                    int[] conflict = new int[sameLevelGroupList.get(k).length()];
                    double altSum = 0;
                    String characters = "";
                    for(int b = 0; b<sameLevelGroupList.get(k).length();b++){//Loop over conflict group string
                        conflict[b] = Character.getNumericValue(sameLevelGroupList.get(k).charAt(b));
                        altSum = altSum + mAircraft.get(conflict[b]).getAltitude();
                        characters = characters + mAircraft.get(conflict[b]).getLabelCharacter() + " ";
                    }
                    characters = characters.substring(0,conflict.length+2);
                    //Calculate the mean altitude of the aircraft that are in the group
                    double meanAlt = altSum/conflict.length;
                    //Draw the group label on the tape
                    altitudeTapeFragment.drawGroupLabel(false, meanAlt, characters, conflict);
                }
                sameLevelGroupList.clear();
                groupLabelsDrawn = true;
            }
        }

        /* Put the single labels on the altitude tape.*/
        for (int i = 1; i < mAircraft.size() + 1; i++) {
            //Check if the aircraft is in a group, if yes don show the individual label
            if (sameLevelAircraft.contains(i) || conflictingAircraft.contains(i)) {
                singleLabelVisibility = View.GONE;
            } else {
                singleLabelVisibility = View.VISIBLE;
            }
            //Set the individual label on the tape
            altitudeTapeFragment.setLabel(mAircraft.get(i).getAltitude(), mAircraft.get(i).getAltLabelId(), mAircraft.get(i).getLabelCharacter(), mAircraft.get(i).isSelected(), mAircraft.get(i).isLabelCreated(), i, singleLabelVisibility);
        }

        //If no group labels were drawn in this iteration, remove any existing ones because ther are no groups anymore
        if(!groupLabelsDrawn) {
            altitudeTapeFragment.removeGroupLabels();
        }
	}

    //Method to be called by the altitude tape fragment to change selection status and update the interface
    public void setIsSelected(int aircraftNumber, boolean isSelected){
        //If the aircraft will be set to selected, deselect all aircraft first
        if(isSelected) {
            deselectAllAircraft();
        }

        //Set the aircraft selection status
        mAircraft.get(aircraftNumber).setIsSelected(isSelected);

        //Keep track of which aircraft is selected
        if(isSelected) {
            selectedAc = aircraftNumber;
            aircraftSpinner.setSelection(aircraftNumber);
        } else {
            selectedAc = 0;
            aircraftSpinner.setSelection(0);
        }

        //Update the mission buttons
        updateMissionButtons();

        //Clear the group selection
        groupSelectedAircraft.clear();
        //Update the altitude tape to show the new selection setting
        updateAltitudeTape();
        //Update the aircraft markers to display the new selection
        aircraftMarkerUpdater();
    }

    //Method to make sure that none of the aircraft is selected
    public void deselectAllAircraft() {
        for(int i=1; i<mAircraft.size()+1; i++) {
            mAircraft.get(i).setIsSelected(false);
        }
        selectedAc = 0;
        //Update the mission buttons
        updateMissionButtons();
        groupSelectedAircraft.clear();

        //Set the aircraft spinner to nothing selected
        aircraftSpinner.setSelection(0);
    }

    //Set a group of aircraft to selected status
    public void setGroupSelected(int[] acNumbers) {
        //Deselect all aircraft first
        deselectAllAircraft();

        //Set the selection status of all involved aircraft
        for(int i = 0; i< acNumbers.length; i++) {
            mAircraft.get(acNumbers[i]).setIsSelected(true);
            groupSelectedAircraft.add(acNumbers[i]);
        }
    }

    //Method to register that a label is drawn on the tape to be sure not a new is created
    public void setIsLabelCreated(boolean isLabelCreated,int acNumber) {
        mAircraft.get(acNumber).setIsLabelCreated(isLabelCreated);
    }

    //Method for the altitude tape to check if an aircraft is selected
    public boolean isAircraftIconSelected(int aircraftNumber) {
        return mAircraft.get(aircraftNumber).isSelected();
    }

    //Method for the altitude tape to check the conflict status of an aircraft
    public ConflictStatus getConflictStatus(int acNumber){
        return mAircraft.get(acNumber).getConflictStatus();
    }

    //Show the altitude of a dragged label on the altitude instrument
    public void setDragAltitude(int acNumber, double altitude, boolean endDrag) {
        if(endDrag) {
            telemetryFragment.setTextColor(Color.WHITE);
            telemetryFragment.setText(String.format("%.1f",mAircraft.get(acNumber).getAltitude()));
        } else {
            telemetryFragment.setTextColor(Color.YELLOW);
            telemetryFragment.setText(String.format("%.1f", altitude));
        }
    }

    //////////////////////////MISSION COMMANDS/////////////////////////
    private void executeWaypointBlock(int wpNumber){
        //Continue only if the selected aircraft has blocks available
        if(mAircraft.get(selectedAc).missionBlocks != null) {
            //Determine the name of the block that belongs to the clicked waypoint and get the corresponding index number from the list
            String blockName = getResources().getString(R.string.survey_block) + String.valueOf(wpNumber + 1); //"SURV"
            int blockIndex = mAircraft.get(selectedAc).missionBlocks.indexOf(blockName);

            //Request execution of the block
//            try {
//                Bundle carrier = new Bundle();
//                carrier.putString("TYPE","BLOCK_SELECTED");
//                carrier.putShort("SEQ",(short) blockIndex);
//                mServiceClient.onCallback(carrier);
//
//            } catch (RemoteException e) {
//                Log.e(TAG, "Error while sending mission block selection to the service");
//            }
        }
    }

    public void changeCurrentWpAltitude(int acNumber, double altitude) {
        if(selectedWp!=0) {
            double wpAltitude = altitude;
            int wpNumber = selectedWp - 1;

            Toast.makeText(getApplicationContext(), "Altitude of WP " + String.valueOf(wpNumber) + " to " + String.format("%.1f", wpAltitude), Toast.LENGTH_SHORT).show();

            //Send update waypoint data to the service (same location, different altitude)
            try {
                Bundle carrier = new Bundle();
                carrier.putString("TYPE", "WRITE_WP");
                carrier.putParcelable("WP", new Waypoint((float) Math.toRadians(mAircraft.get(acNumber).getWpLat(wpNumber)), (float) Math.toRadians(mAircraft.get(acNumber).getWpLon(wpNumber)), (float) (wpAltitude), mAircraft.get(acNumber).getWpSeq(wpNumber), mAircraft.get(acNumber).getWpTargetSys(wpNumber), mAircraft.get(acNumber).getWpTargetComp(wpNumber)));
                mServiceClient.onCallback(carrier);
            } catch (RemoteException e) {
                Log.e(TAG, "Error while sending waypoint to the service");
            }

            //Deselect the waypoint
            selectedWp = 0;
            mAircraft.get(acNumber).setWpSelected(wpNumber, false);
            //Set the waypoint status to be updating
            mAircraft.get(acNumber).setWpUpdating(wpNumber);
            //Update the waypoints again
            waypointUpdater(acNumber);
        } else {
            Toast.makeText(getApplicationContext(), "No waypoint selected!", Toast.LENGTH_SHORT).show();
        }
    }

    /////////////////////////PERFORMANCE SCORE/////////////////////////
    private void calcPerformance() {
        /* TODO finish the performance score calculation */
//        performanceScore = (ROIcovered() + conflict time score + collissions + loss of control(UAV out of comm) )/4;
        performanceScore = ROIcovered();

        //Set the performance score to the text view
        performanceScoreFragment.setText(String.format("%.1f", performanceScore));
    }

    private double ROIcovered(){
        //Region of interest parameters
        double AREA = ROIRadius*ROIRadius*Math.PI;
        LatLng ROIcenter = home.getHomeLocation();

        //Calculate the overlap between covered region by aircraft and the ROI area
        double overlapArea = 0;

        for(int i = 1; i<=mAircraft.size(); i++) { //Loop over all aircraft
            //Add overlap between aircraft coverage and ROI
            double overlap = circleOverlap(ROIRadius, acCoverageRadius, ROIcenter, mAircraft.get(i).getLatLng());
            double doubleOverlap = 0;
            /* TODO: account for overlap by decreasing performance score in case of overlap/violation of the separation standard instead of calculating overlap */
            if(overlap>0) { //If not outside the ROI
                for (int j = i + 1; j <= mAircraft.size(); j++) {
                //Account for overlap of the two UAVs
                doubleOverlap+= circleOverlap(acCoverageRadius, acCoverageRadius, mAircraft.get(i).getLatLng(), mAircraft.get(j).getLatLng());
                }
            }
            //Calculate the total coverage ove the ROI
            overlapArea += overlap - doubleOverlap;
        }
        //NOTE THAT THE OVERLAP OF 3+ CIRCLES IS NOT COVERED!!
        //Coverage percentage
        return (overlapArea/AREA)*100;
    }

    private double circleOverlap(double radius1, double radius2, LatLng c1, LatLng c2){
        //Calculation of distance between two LatLng coordinates
        float[] distance = new float[1];
        Location.distanceBetween(c1.latitude, c1.longitude, c2.latitude, c2.longitude, distance);

        //Define the used radii
        double R = radius1;
        double r = radius2;

        //Make sure R is the largest of the two circles
        if(R < r) {
            R = radius2;
            r = radius1;
        }

        //Check whether the circles overlap, do not intersect or are inside each other. Then calculate accordingly
        double overlapArea;
        if(distance[0] > (R+r)) {                  //No overlap
            overlapArea = 0;
        } else if((distance[0]+r) <= R) {  //inside
            //Entire area of the small circle
            overlapArea = r*r*Math.PI;
        } else {                                   //Overlap
            double part1 = r*r*Math.acos((distance[0]*distance[0] + r*r - R*R)/(2*distance[0]*r));
            double part2 = R*R*Math.acos((distance[0]*distance[0] + R*R - r*r)/(2*distance[0]*R));
            double part3 = 0.5*Math.sqrt((-distance[0]+r+R)*(distance[0]+r-R)*(distance[0]-r+R)*(distance[0]+r+R));
            //Subtract the triangle areas from the cone areas to end up with the overlap area
            overlapArea = part1 + part2 - part3;
        }
        return overlapArea;
    }

    /////////////////////////LOGGING/////////////////////////
    private void dataLogger() {

        //Get time and date
        Calendar cal = Calendar.getInstance();
        int hours    = cal.get(Calendar.HOUR_OF_DAY);
        int minutes  = cal.get(Calendar.MINUTE);
        int seconds  = cal.get(Calendar.SECOND);

        //Make a time string to include in the log file
        String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        //Calculate the time the application has been active (milliSeconds)
        int uptime = (int)(System.currentTimeMillis() - initTime);

        try {
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File (sdCard.getAbsolutePath() + "/gcsData");
            dir.mkdirs();
            File file = new File(dir, logFileName);
            FileOutputStream f = new FileOutputStream(file, true);

            OutputStreamWriter myOutWriter = new OutputStreamWriter(f);
            //First columns are [Time, Uptime, Performance score]
            myOutWriter.append(time + ", " + String.format("%.1f", uptime*1e-3) + ", " + performanceScore);
            //Loop over all aircraft to write a line to the log file with the following data of all aircraft: [Altitude, Latitude, Longitude]
            for(int i=1; i<mAircraft.size()+1; i++) {
                /* TODO log waypoint location instead of aircraft location */
                myOutWriter.append(", " + mAircraft.get(i).getAltitude() + ", " + mAircraft.get(i).getLat() + ", " + mAircraft.get(i).getLon());
            }
            //End the line and close the file
            myOutWriter.append("\r\n");
            myOutWriter.close();

        } catch(IOException e){
            Log.e(TAG, "Error while writing to logfile");
        }
    }
}