package com.gcs;

import com.aidllib.IEventListener;
import com.aidllib.IMavLinkServiceClient;
import com.aidllib.core.ConnectionParameter;
import com.aidllib.core.mavlink.waypoints.Waypoint;
import com.aidllib.core.model.Altitude;
import com.aidllib.core.model.Attitude;
import com.aidllib.core.model.Heartbeat;
import com.aidllib.core.model.Speed;
import com.aidllib.core.model.State;

import com.aidllib.core.model.Battery;
import com.aidllib.core.model.Position;
import com.gcs.core.ConflictStatus;
import com.gcs.core.Home;
import com.gcs.core.TaskStatus;
import com.gcs.fragments.PerformanceScoreFragment;
import com.gcs.fragments.ScenarioEndFragment;
import com.gcs.fragments.ScenarioTimeFragment;
import com.gcs.helpers.LogHelper;
import com.gcs.helpers.PerformanceCalcHelper;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, OnMarkerClickListener, OnInfoWindowClickListener, OnMarkerDragListener {

	private static final String TAG = MainActivity.class.getSimpleName();

    //Declaration of handlers and definition of time and time steps
	private Handler handler, interfaceUpdateHandler;
	private final int mInterval        = 1000;                       // milliseconds
    private final long initTime        = System.currentTimeMillis(); // milliseconds
    private long timeLeft;
    private long scenarioStartTime;

    //Declaration of the service client
	IMavLinkServiceClient mServiceClient;

    //Declaration of the connection button
	private Button connectButton;

    //Declaration of booleans
	private boolean isConnected           = false;
	private boolean isAltitudeUpdated     = false;
    private boolean aircraftSelected      = false;
    private boolean showCommRange         = false;
    private boolean showMinCommRange      = false;
    private boolean showCoverage          = false;
    private boolean updateAircraftSpinner = true;
    private boolean allWpsLoaded          = false;
    private boolean batteryFailureOccured = false;
    private boolean scenarioStarted       = false;

    //Declaration of the fragments
	private TelemetryFragment        telemetryFragment;
	private PerformanceScoreFragment performanceScoreFragment;
	private ScenarioTimeFragment     scenarioTimeFragment;
	private AltitudeTape             altitudeTapeFragment;
    private SupportMapFragment       mapFragment;
    private MissionButtonFragment    missionButtons;
    private ScenarioEndFragment      scenarioEndFragment;

    //Declaration of the lists/arrays that are used
    private SparseArray<Aircraft>     mAircraft             = new SparseArray<>();
    private List<Polyline>            mConnectingLines      = new ArrayList<>();
    private ArrayList<Integer>        conflictingAircraft   = new ArrayList<>();
    private ArrayList<Integer>        sameLevelAircraft     = new ArrayList<>();
    private ArrayList<List<Integer>>  groupList             = new ArrayList<>();
    private List<Integer>             groupSelectedAircraft = new ArrayList<>();
    private List<Circle>              relayCommCircles      = new ArrayList<>();
    private List<LatLng>              ROIlist               = new ArrayList<>();
    private List<Integer>             ROIradiiList          = new ArrayList<>();
    private List<GroundOverlay>       ROIOverlayList        = new ArrayList<>();
    private List<Integer>             initialBatList        = new ArrayList<>();

	public Home home;

    private float verticalSeparationStandard, horizontalSeparationStandard, surveillanceCircleRadius;
    private int commMaxRange, commRelayRange, singleLabelVisibility, acCoverageRadius, surveillanceAltitude, relayAltitude,altitudeAccuracyDistance, halfBatteryVoltage, lowBatteryVoltage;
    private int selectedAc = 0;                     //Set to 0 if none serves as relay (yet)
    private double performanceScore = 0f;
    private int selectedWp = 0;
    private int activeScenario = 0;
    private int noAircraftScenario = 0;
    private int batteryFailureTime = 0;
    private int batteryFailureAircraft = 0;
    private int scenarioRuntime = 0;
    private int surveyCountScore = 0;
    private float initialZoomLevel = 16.0f;
    private LatLng origMarkerPosition;
    private Circle flightPath, surveillanceBound;

    //Declaration of items needed for mission blocks display
    private MenuItem menuBlockSpinner = null, menuAircraftSpinner = null, menuScenarioSpinner = null;
    private Spinner blockSpinner, aircraftSpinner, scenarioSpinner;
    Menu menu;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        //Instantiate the handlers
		handler = new Handler();                    //Handler for updating parameters from service
		interfaceUpdateHandler = new Handler();     //Handler for updating the user interface

        //Create filename for datalogging
        LogHelper.createLogfileName();

        //Create a handle to the connect button
		connectButton = (Button) findViewById(R.id.connectButton);

        //Obtain values from resources
        verticalSeparationStandard = getResources().getInteger(R.integer.verticalSeparationStandard)/10f;       //(Divided by 10 to convert to meters)
        horizontalSeparationStandard = getResources().getInteger(R.integer.horizontalSeparationStandard)/10f;                                //(Divided by 10 to convert to meters)
        surveillanceCircleRadius     = getResources().getInteger(R.integer.surveillanceCircleRadius)/10f;                                //(Divided by 10 to convert to meters)
        commMaxRange = getResources().getInteger(R.integer.commMaxRange);                                                   //meters
        commRelayRange = getResources().getInteger(R.integer.commRelayRange);                                                 //meters
        acCoverageRadius = getResources().getInteger(R.integer.acCoverageRadius);                                               //meters         //meters
        surveillanceAltitude = getResources().getInteger(R.integer.surveillanceAltitude);                                            //meters
        relayAltitude = getResources().getInteger(R.integer.relayAltitude);                                                   //meters
        altitudeAccuracyDistance = getResources().getInteger(R.integer.altitudeAccuracyDistance);               //meters
        halfBatteryVoltage = getResources().getInteger(R.integer.HalfBatteryVoltage);
        lowBatteryVoltage = getResources().getInteger(R.integer.LowBatteryVoltage);

        // Instantiate home object
		home = new Home();

		//Create a handles to the fragments
		telemetryFragment        = (TelemetryFragment) getSupportFragmentManager().findFragmentById(R.id.telemetryFragment);               //Telemetry fragment
        performanceScoreFragment = (PerformanceScoreFragment) getSupportFragmentManager().findFragmentById(R.id.performanceScoreFragment); //Performance score fragment
        scenarioTimeFragment     = (ScenarioTimeFragment) getSupportFragmentManager().findFragmentById(R.id.scenarioTimeFragment);         //Scenario time fragment
		altitudeTapeFragment     = (AltitudeTape) getSupportFragmentManager().findFragmentById(R.id.altitudeTapeFragment);                 //AltitudeTape fragment
		missionButtons           = (MissionButtonFragment) getSupportFragmentManager().findFragmentById(R.id.missionButtonFragment);       //MissionButton fragment
        scenarioEndFragment      = (ScenarioEndFragment) getSupportFragmentManager().findFragmentById(R.id.scenarioEndFragment);               //MissionButton fragment

        //Set timer
        scenarioRuntime = getResources().getInteger(R.integer.scenarioRuntime);
        timeLeft = scenarioRuntime;
        scenarioTimeFragment.setTimeLeft(timeLeft);

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

//        //Temporary setting of aircraft number
//        final int acNumber = 1;

//        ////////////BLOCKS SPINNER////////////
//        //Set up the spinner in the action bar for the mission block which can be loaded from the service and create a handle
//        menuBlockSpinner = menu.findItem(R.id.menu_block_spinner);
//        blockSpinner = (Spinner) MenuItemCompat.getActionView(menuBlockSpinner);
//
//        //Listener on item selection in the block spinner
//        blockSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//
//            //Define what should happen when an item in the spinner is selected
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                try {
//                    //Take the selected block index and send it to the service
//                    Bundle carrier = new Bundle();
//                    carrier.putString("TYPE", "BLOCK_SELECTED");
//                    carrier.putShort("SEQ",(short)position);
//                    mServiceClient.onCallback(carrier,acNumber);
//                } catch (RemoteException e) {
//                    Log.e(TAG,"Error while sending mission block spinner selection to the service");
//                }
//            }
//
//            //Define what should happen if no item is selected in the spinner
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                //Do nothing
//            }
//        });

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

        ////////////SCENARIO SPINNER////////////
        //Set up the spinner in the action bar for the scenarios and create a handle
        menuScenarioSpinner = menu.findItem(R.id.scenario_spinner);
        scenarioSpinner     = (Spinner) MenuItemCompat.getActionView(menuScenarioSpinner);

        //Listener on item selection in the spinner
        scenarioSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            //Define what should happen when an item in the spinner is selected
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Set the scenario (ROI location(s) and aircraft battery settings) if one is selected
                setScenario(position);
            }

            //Define what should happen if no item is selected in the spinner
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Do nothing
            }
        });

        //Fill the scenario spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.scenario_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scenarioSpinner.setAdapter(adapter);

        ////////////////
        this.menu = menu;
        return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here.
        if(isConnected) {
            switch (item.getItemId()) {
                case R.id.action_settings:
                    return true;
                case R.id.show_comm_range:
                    //Show/hide the communication range on screen
                    showCommRange = !showCommRange;
                    //Update map
                    drawCommunicationRange(Aircraft.relayAircraft);
                    //Change button appearance for status indication
                    TextView commTextView = (TextView) findViewById(item.getItemId());
                    if (showCommRange) {
                        commTextView.setTextColor(getResources().getColor(R.color.green));
                    } else {
                        commTextView.setTextColor(Color.WHITE);
                    }
                    return true;
                case R.id.show_min_comm_range:
                    //Show/hide the minimum communication range on screen
                    showMinCommRange = !showMinCommRange;
                    //Change button appearance for status indication
                    TextView minCommTextView = (TextView) findViewById(item.getItemId());
                    if (showMinCommRange) {
                        minCommTextView.setTextColor(getResources().getColor(R.color.green));
                    } else {
                        minCommTextView.setTextColor(Color.WHITE);
                    }
                    return true;
                case R.id.show_coverage:
                    showCoverage = !showCoverage;
                    //Update map
                    aircraftMarkerUpdater();
                    //Change button appearance for status indication
                    TextView coverageTextView = (TextView) findViewById(item.getItemId());
                    if (showCoverage) {
                        coverageTextView.setTextColor(getResources().getColor(R.color.green));
                    } else {
                        coverageTextView.setTextColor(Color.WHITE);
                    }
                    return true;
            }
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
            if (isConnected) mServiceClient.disconnectDroneClient();
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
            //Calculate the runtime left for the experiment
            if(isConnected && allWpsLoaded) {
                if(!scenarioStarted) {
                    //Set the scenario start time
                    scenarioStartTime = System.currentTimeMillis();
                    scenarioStarted = true;
                }

                timeLeft = scenarioRuntime - ((System.currentTimeMillis() - scenarioStartTime) / 1000);
            }

            //Show scenario runtime that is left on interface
            scenarioTimeFragment.setTimeLeft(timeLeft);

            //Update aircraft spinner
            if (updateAircraftSpinner && aircraftSpinner != null) {
                updateAircraftSpinner();
                updateAircraftSpinner = false;
            }

            //Update the battery values (reducing with app values for experiment scenarios, external input should be overridden)
            updateScenarioBatteryValues();

            //Check if aircraft is landing to force final approach
            for (int i = 0; i < mAircraft.size(); i++) {
                int acNumber = mAircraft.keyAt(i);
                //Update current block every 10 seconds
                if (timeLeft%10==0 && isConnected) {
                    updateMissionBlocksSelection(acNumber);
                }

                if (mAircraft.get(acNumber).getCurrentBlock() != null && mAircraft.get(acNumber).getCurrentBlock().equals(getResources().getString(R.string.land_block)) && mAircraft.get(acNumber).getAltitude()<16 && (mAircraft.get(acNumber).getLat()-51.9715828)<0) {
                    forceFinal(acNumber);
                }
            }

            //Check for aircraft relay status
            checkAircraftTaskStatus();

            //Draw the communication range on the map
            if (isConnected) {
                drawCommunicationRange(Aircraft.relayAircraft);
            }

            //check for altitude and course conflicts
            checkConflicts();

            //Update the altitude tape (if altitude is available)
            if (isAltitudeUpdated && isConnected) {
                updateAltitudeTape();
                isAltitudeUpdated = false;
            }

            //Clear the list of aircraft that are on the same level (after the altitude tape is updated)
            sameLevelAircraft.clear();

            //Update aircraft icons and display them on the map
            if (isConnected) {
                aircraftMarkerUpdater();
            }

            //Draw the connecting lines on the map that indicate conflicts
            drawConnectingLines();

            /////PERFORMANCE SCORE
            //Calculate the current performance score
            if (ROIlist.size() != 0 && allWpsLoaded && isConnected) {
                performanceScore = PerformanceCalcHelper.calcPerformance(ROIradiiList, acCoverageRadius, ROIlist, mAircraft, surveyCountScore,halfBatteryVoltage,lowBatteryVoltage);
            }
            //Set the performance score to the text view
            performanceScoreFragment.setScore(performanceScore);

            /////DATA LOGGING
            if (allWpsLoaded && (timeLeft < scenarioRuntime)) {
                LogHelper.dataLogger(initTime, timeLeft, scenarioRuntime, activeScenario, performanceScore, mAircraft);
            }

            //Restart this updater after the set interval (only if scenario time is left)
            if (timeLeft > 0) {
                interfaceUpdateHandler.postDelayed(interfaceUpdater, mInterval);
            } else if (timeLeft <= 0) {
                //Show dialog that scenario is finished
                scenarioEndFragment.setVisibility(View.VISIBLE);
            }
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
        public void onEvent(String type, int acNumber) {
            //Instantiate a new aircraft object if the aircraft
            // number does not exist in the list
            if(acNumber!=-1 && mAircraft.get(acNumber)==null) {
                mAircraft.put(acNumber, new Aircraft(getApplicationContext(),acNumber));
                //Set the initial battery values (dependent on scenario number)
                mAircraft.get(acNumber).setBatteryState(initialBatList.get(acNumber-1),-1,-1);
                //Make sure the aircraft spinner will be updated
                updateAircraftSpinner = true;
            }

            //Switch that catches messages that contain a sysId of -1, to prevent nullpointers while using getAttribute()
            if(acNumber==-1) {
                switch (type) {
                    case "CONNECTED": {
                        isConnected = true;
                        updateConnectButton();
                        break;
                    }

                    case "DISCONNECTED": {
                        isConnected = false;
                        updateConnectButton();
                        break;
                    }

                    default:
                        break;
                }

            } else if(isConnected){
                switch (type) {
                    case "CONNECTED": {
                        isConnected = true;
                        updateConnectButton();
                        break;
                    }

                    case "HEARTBEAT_FIRST": {
                        updateHeartbeat(acNumber);
                        break;
                    }

                    case "DISCONNECTED": {
                        isConnected = false;
                        updateConnectButton();
                        break;
                    }

                    case "ATTITUDE_UPDATED": {
                        updateAttitude(acNumber);
                        break;
                    }

                    case "ALTITUDE_SPEED_UPDATED": {
                        updateAltitude(acNumber);
                        updateSpeed(acNumber);
                        break;
                    }

                    case "BATTERY_UPDATED": {
                        if (activeScenario == 0)
                            updateBattery(acNumber);
                        break;
                    }

                    case "POSITION_UPDATED": {
                        updatePosition(acNumber);
                        break;
                    }

                    case "SATELLITES_VISIBLE_UPDATED": {
                        break;
                    }

                    case "STATE_UPDATED": {
                        updateState(acNumber);
                        break;
                    }

                    case "WAYPOINTS_UPDATED": {
                        updateWaypoints(acNumber);
                        break;
                    }

                    case "WAYPOINT_RECEIVED": {
                        onWaypointReceived(acNumber);
                        break;
                    }

                    case "MISSION_BLOCKS_UPDATED": {
                        updateMissionBlocks(acNumber);
                        break;
                    }

                    case "CURRENT_BLOCK_UPDATED": {
                        updateMissionBlocksSelection(acNumber);
                        break;
                    }

                    default:
                        break;
                }
            }
    	}
    };

    ////////UPDATE METHODS FOR AIRCRAFT DATA

    /**
     * This runnable object is created to update the waypoint list on waypoint received message
     */
    private void onWaypointReceived(int acNumber) {
        if (isConnected) {
            try {
                Bundle carrier = new Bundle();
                carrier.putString("TYPE", "REQUEST_WP_LIST");
                mServiceClient.onCallback(carrier, acNumber);
            } catch (RemoteException e) {
                Log.e(TAG, "Error while requesting individual waypoint list");
            }
        }
    }

    /**
     * This runnable object is created to update the heartbeat
     */
    private void updateHeartbeat(final int acNumber) {
        handler.post(new Runnable() {
            @Override
            public void run() {
            try {
                Heartbeat mHeartbeat = getAttribute("HEARTBEAT",acNumber);
                mAircraft.get(acNumber).setHeartbeat(mHeartbeat.getSysId(), mHeartbeat.getCompId());
            } catch (Throwable t){

            }
            }
        });
    }

	/**
	 * This runnable object is created to update the attitude
	 */
	private void updateAttitude(final int acNumber) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Attitude mAttitude = getAttribute("ATTITUDE", acNumber);
                    mAircraft.get(acNumber).setRollPitchYaw(Math.toDegrees(mAttitude.getRoll()), Math.toDegrees(mAttitude.getPitch()), Math.toDegrees(mAttitude.getYaw()));
                } catch (Throwable t) {
                    Log.e(TAG, "Error while updating the attitude", t);
                }
            }
        });
	}

	/**
	 * This runnable object is created to update the altitude
	 */
	private void updateAltitude(final int acNumber) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Altitude mAltitude = getAttribute("ALTITUDE", acNumber);
                    //Note that in paparazzi the z-axis is defined pointing downwards, so a minus sign is applied to all incoming altitude values
                    mAircraft.get(acNumber).setAltitude(mAltitude.getAGL());
                    mAircraft.get(acNumber).setTargetAltitude(-mAltitude.getTargetAltitude());
                    mAircraft.get(acNumber).setAGL(-mAltitude.getAltitude());

                    //Set aircraft to flying status if altitude is higher than 0.5m
                    mAircraft.get(acNumber).setIsFlying((-mAltitude.getAltitude()) > 0.5);

                    /* Set isAltitudeUpdated to true at first update of altitude (used for altitude tape updates) */
                    if (!isAltitudeUpdated) isAltitudeUpdated = true;

                } catch (Throwable t) {
                    Log.e(TAG, "Error while updating the altitude", t);
                }
            }
        });
	}

	/**
	 * This runnable object is created to update the ground- and airspeeds
	 */
	private void updateSpeed(final int acNumber) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Speed mSpeed = getAttribute("SPEED", acNumber);
                    mAircraft.get(acNumber).setGroundAndAirSpeeds(mSpeed.getGroundSpeed(), mSpeed.getAirspeed(), mSpeed.getTargetSpeed());
                    mAircraft.get(acNumber).setTargetSpeed(mSpeed.getTargetSpeed());
                } catch (Throwable t) {
                    Log.e(TAG, "Error while updating the speed", t);
                }
            }
        });
	}

	/**
	 * This runnable object is created to update the battery information
	 */
	private void  updateBattery(final int acNumber) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Battery mBattery = getAttribute("BATTERY", acNumber);
                    mAircraft.get(acNumber).setBatteryState(mBattery.getBattVolt(), -1, mBattery.getBattCurrent());
                } catch (Throwable t) {
                    Log.e(TAG, "Error while updating the battery information", t);
                }
            }
        });
	}

	/**
	 * This runnable object is created to update position
	 */
	private void updatePosition(final int acNumber) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Position mPosition = getAttribute("POSITION", acNumber);
                    mAircraft.get(acNumber).setSatVisible(mPosition.getSatVisible());
                    mAircraft.get(acNumber).setLlaHdg(mPosition.getLat(), mPosition.getLon(), mPosition.getAlt(), (short) (mPosition.getHdg() / 100));
                    if (home.getHomeLocation() != null) {
                        mAircraft.get(acNumber).setDistanceHome(home.getHomeLocation());
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Error while updating position", t);
                }
            }
        });
	}

	/**
	 * This runnable object is created to update state
	 */
	private void updateState(final int acNumber) {
		handler.post(new Runnable() {
			@Override
			public void run() {
            try {
                State mState = getAttribute("STATE",acNumber);
                mAircraft.get(acNumber).setIsFlying(mState.isFlying());
                mAircraft.get(acNumber).setArmed(mState.isArmed());
            } catch (Throwable t) {
                Log.e(TAG, "Error while updating state", t);
            }
			}
		});
	}

	/**
	 * This runnable object is created to update waypoints
	 */
	private void updateWaypoints(final int acNumber) {
		handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    //Get a list of waypoint objects from the service
                    Bundle carrier = mServiceClient.getAttribute("WAYPOINTS",acNumber);
                    carrier.setClassLoader(Waypoint.class.getClassLoader());
                    List<Waypoint> waypoints = carrier.getParcelableArrayList("WAYPOINTS");

                    //Clear the waypoint list if the aircraft already has waypoint data
                    if (mAircraft.get(acNumber).getNumberOfWaypoints() > 0) {
                        mAircraft.get(acNumber).clearWpList();
                    }

                    //Only take the survey waypoint (wp number 3)
                    for(int i=0; i<waypoints.size(); i++) {
                        if(waypoints.get(i).getSeq()==3) {
                            mAircraft.get(acNumber).addWaypoint(Math.toDegrees(waypoints.get(i).getLat()), Math.toDegrees(waypoints.get(i).getLon()), waypoints.get(i).getAlt(), (short) waypoints.get(i).getSeq(), waypoints.get(i).getTargetSys(), waypoints.get(i).getTargetComp());
                        }
                    }

                    //Call the method that shows the waypoints on the map
                    waypointUpdater(acNumber);

                    // Define the home location based on the home waypoint (standard the second waypoint)
                    if(home.getHomeLocation() == null) {
                        LatLng newHome = new LatLng(Math.toDegrees(waypoints.get(acNumber).getLat()),Math.toDegrees(waypoints.get(acNumber).getLon()));
                        home.setHomeLocation(newHome);
                        drawHomeMarker();
                    }

                    //Check if the waypoints of all aircraft have been received and update waypoint button accordingly
                    for(int i=0; i<mAircraft.size(); i++) {
                        int key = mAircraft.keyAt(i);
                        if(mAircraft.get(key).getWpLatLngList().isEmpty()) break;
                        if(i==mAircraft.size()-1) {
                            missionButtons.updateWaypointsButton(true);
                            allWpsLoaded = true;
                        }
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Error while updating waypoints", t);
                }
            }
        });
	}

    /**
     * This runnable object is created to update mission blocks
     */
    private void updateMissionBlocks(final int acNumber) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    //Store the mission block list
                    Bundle carrier = mServiceClient.getAttribute("BLOCKS", acNumber);
                    mAircraft.get(acNumber).missionBlocks = carrier.getStringArrayList("BLOCKS");

                    //Check if the blocks of all aircraft have been received and update blocks button accordingly
                    for(int i=0; i<mAircraft.size(); i++) {
                        int key = mAircraft.keyAt(i);
                        if(mAircraft.get(key).missionBlocks == null) break;
                        if(i==mAircraft.size()-1) missionButtons.updateBlocksButton(true);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Error while updating mission blocks");
                }
            }
        });
    }

    //Method to update the mission block dropdown menu
//    private void updateMissionBlocksSpinner(int acNumber) {
//        if(mAircraft.get(acNumber).missionBlocks != null) {
//            //Create an array adapter with the mission block names
//            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mAircraft.get(acNumber).missionBlocks);
//            spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//            //Apply the array adapter to the block spinner to update the blocks in the dropdown menu
//            blockSpinner.setAdapter(spinnerArrayAdapter);
//        }
//    }

    //Method to update the selected block in the dropdown menu to the active one
    private void updateMissionBlocksSelection(final int acNumber) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    // Get current block
                    Bundle carrier = mServiceClient.getAttribute("CURRENT_BLOCK", acNumber);
                    int currentBlock = carrier.getInt("CURRENT_BLOCK");
                    Log.d("CURRENT_BLOCK", String.valueOf(currentBlock));

                    //Update the Mission block spinner selection
                    //                    blockSpinner.setSelection(currentBlock);

                    //Set current block to aircraft
                    mAircraft.get(acNumber).setCurrentBlock(currentBlock);

                    //Update the status of the mission buttons
                    if (mAircraft.get(acNumber).missionBlocks != null)
                        missionButtons.updateExecutedMissionButton(mAircraft.get(acNumber).missionBlocks.get(currentBlock));
                } catch (RemoteException e) {
                    Log.e(TAG, "Error while trying to update the mission block selection in the spinner");
                }
            }
        });
    }

    //Method to update the aircraft dropdown menu
    private void updateAircraftSpinner() {
        List<String> aircraftList = new ArrayList<>();

        //Add the initial value to the spinner
        aircraftList.add(getResources().getString(R.string.no_aircraft_selected));

        for(int i=0; i<mAircraft.size(); i++) {
            int acNumber = mAircraft.keyAt(i);
            aircraftList.add("Aircraft " + mAircraft.get(acNumber).getLabelCharacter());
        }

        //Create an array adapter with the mission block names
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, aircraftList);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Apply the array adapter to the aircraft spinner to update the aircraft in the dropdown menu
        aircraftSpinner.setAdapter(spinnerArrayAdapter);
    }

    //Method to set the settings for the executed test scenario
    private void setScenario(int scenarioNumber) {
        activeScenario = scenarioNumber;

        //Get the location(s) of the ROI(s)
        String[] latLng = null;
        int[] batValues = null;
        int[] ROIradii   = null;
        List<LatLng> ROIlist= new ArrayList<>();
        List<Integer> batList= new ArrayList<>();

        if(scenarioNumber != 0) {
            if(scenarioNumber == 1) {
                latLng    = getResources().getStringArray(R.array.latLng_scenario1);
                batValues = getResources().getIntArray(R.array.iniBatVolt_scenario1);
                ROIradii  = getResources().getIntArray(R.array.ROIRadii_scenario1);
            } else if(scenarioNumber == 2) {
                latLng    = getResources().getStringArray(R.array.latLng_scenario2);
                batValues = getResources().getIntArray(R.array.iniBatVolt_scenario2);
                ROIradii  = getResources().getIntArray(R.array.ROIRadii_scenario2);
            } else if(scenarioNumber == 3) {
                latLng    = getResources().getStringArray(R.array.latLng_scenario3);
                batValues = getResources().getIntArray(R.array.iniBatVolt_scenario3);
                ROIradii  = getResources().getIntArray(R.array.ROIRadii_scenario3);
            } else if(scenarioNumber == 4) {
                latLng    = getResources().getStringArray(R.array.latLng_scenario4);
                batValues = getResources().getIntArray(R.array.iniBatVolt_scenario4);
                ROIradii  = getResources().getIntArray(R.array.ROIRadii_scenario4);
            } else if(scenarioNumber == 5) {
                latLng    = getResources().getStringArray(R.array.latLng_scenario5);
                batValues = getResources().getIntArray(R.array.iniBatVolt_scenario5);
                ROIradii  = getResources().getIntArray(R.array.ROIRadii_scenario5);
            } else if (scenarioNumber == 6) {
                latLng    = getResources().getStringArray(R.array.latLng_scenario6);
                batValues = getResources().getIntArray(R.array.iniBatVolt_scenario6);
                ROIradii  = getResources().getIntArray(R.array.ROIRadii_scenario6);
            } else if (scenarioNumber == 7) {
                latLng    = getResources().getStringArray(R.array.latLng_scenario7);
                batValues = getResources().getIntArray(R.array.iniBatVolt_scenario7);
                ROIradii  = getResources().getIntArray(R.array.ROIRadii_scenario7);
            }

            //Loop over ROIs to put the values in lists
            for(int i=0; i < latLng.length; i++) {
                //Parse the LatLong values in string form to a LatLng object
                String[] latLngSeparated = latLng[i].split(",");
                ROIlist.add(new LatLng(Double.parseDouble(latLngSeparated[0]),Double.parseDouble(latLngSeparated[1])));
            }
            //Loop over battery values to put in list
            for(int j=0; j < batValues.length; j++) {
                //Convert battery value array to a list
                batList.add(batValues[j]);
            }

            //Set battery failure time (0=no failure)
            int[] batFailTime = getResources().getIntArray(R.array.batteryFailureTime);
            batteryFailureTime = batFailTime[scenarioNumber-1];
            //Set battery failure aircraft (0=none)
            int[] batFailAc = getResources().getIntArray(R.array.batteryFailureAircraft);
            batteryFailureAircraft = batFailAc[scenarioNumber-1];

            //Set the number of aircraft in the scenario
            int[] noAircraft = getResources().getIntArray(R.array.noAircraftInScenario);
            noAircraftScenario = noAircraft[scenarioNumber-1];

            //Set the number of aircraft that is expected to contribute to survey coverage (influences the performance score)
            int[] surveyCountScores = getResources().getIntArray(R.array.surveyCountScoreScenario);
            surveyCountScore = surveyCountScores[scenarioNumber-1];

            //Set the initial zoom level
            int[] zoomLevels = getResources().getIntArray(R.array.initialZoomLevels);
            initialZoomLevel = zoomLevels[scenarioNumber-1]/10.0f;

            //Loop over ROI radii to put in list
            for(int k=0; k < ROIradii.length; k++) {
                ROIradiiList.add(ROIradii[k]);
            }
        } else {
            noAircraftScenario = 0;
        }

        //Set the ROI list for performance calculation
        if(!ROIlist.isEmpty()) {
            this.ROIlist = ROIlist;
        } else {
            this.ROIlist.clear();
        }

        //Draw the Region Of Interest (ROI) on the map
        drawROI(ROIlist);
        //Set battery values of the aircraft
        initialBatList = batList;
    }

	////////OTHER COMMUNICATION FUNCTIONS

	private ConnectionParameter retrieveConnectionParameters() {
		//Fetch connection type from resources
        final int connectionType = getResources().getInteger(R.integer.connectionType);

        //Fetch server ports and system id numbers from resources
        ArrayList<Integer> serverPortList = new ArrayList<>(); //List of the udp port used
        ArrayList<Integer> sysIdList = new ArrayList<>();      //List of system ids used (in the same order as the udp ports)

        int ports[] =  Arrays.copyOfRange(getResources().getIntArray(R.array.udp_ports), 0, noAircraftScenario);
        int ids[] = Arrays.copyOfRange(getResources().getIntArray(R.array.system_ids), 0, noAircraftScenario);
        for(int i=0; i<ports.length; i++) {
            serverPortList.add(ports[i]);
            sysIdList.add(ids[i]);
        }

        Bundle extraParams = new Bundle();
        ConnectionParameter connParams;
        switch (connectionType) {
            case 0:
                extraParams.putIntegerArrayList("udp_port", serverPortList);
                extraParams.putIntegerArrayList("sysIds", sysIdList);
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
        if(activeScenario>0) {
            if (!isConnected) {
                connectToDroneClient();
            } else {
                try {
                    mServiceClient.disconnectDroneClient();
                    deselectAllAircraft();
                    noAircraftScenario = 0;
//                    altitudeTapeFragment.clearTape();
                    clearMap();
                    home.clear();
                    for (int i = 0; i < mAircraft.size(); i++) {
                        mAircraft.get(mAircraft.keyAt(i)).clearFlightPath();
                    }
                    missionButtons.deactivateButtons();
                } catch (RemoteException e) {
                    Log.e(TAG, "Error while disconnecting", e);
                }
            }
        } else {
            Toast.makeText(getApplicationContext(), "Please select a scenario!", Toast.LENGTH_SHORT).show();
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
	
	public <T extends Parcelable> T getAttribute(String type, int sysId) {
        if (type == null)
            return null;

        T attribute = null;
        Bundle carrier = null;
        try {
            carrier = mServiceClient.getAttribute(type,sysId);
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

    //Method to handle land button clicks
	public void onLandRequest(View v) {
        if(selectedAc == 0) {
            //Notify the user that no aircraft is selected
            Toast.makeText(getApplicationContext(), "No aircraft selected!", Toast.LENGTH_SHORT).show();
            //request to land if connected and the mission blocks are loaded
        } else if(isConnected && mAircraft.get(selectedAc).hasCommConnection() && mAircraft.get(selectedAc).missionBlocks != null && !mAircraft.get(selectedAc).getBatteryCriticalState()) {
            try {
                Bundle carrier = new Bundle();
                carrier.putString("TYPE", "BLOCK_SELECTED");
                carrier.putShort("SEQ", (short) mAircraft.get(selectedAc).missionBlocks.indexOf(getResources().getString(R.string.land_block)));
                mServiceClient.onCallback(carrier,selectedAc);
            } catch (RemoteException e) {
                Log.e(TAG, "Error while requesting the service to execute the land block");
            }
        }
	}

    //Method to handle take-off button clicks
	public void onTakeOffRequest(View v) {
        if(selectedAc == 0) {
            //Notify the user that no aircraft is selected
            Toast.makeText(getApplicationContext(), "No aircraft selected!", Toast.LENGTH_SHORT).show();
        //request takeoff if connected and the mission blocks are loaded
        } else if(isConnected && mAircraft.get(selectedAc).hasCommConnection() && mAircraft.get(selectedAc).missionBlocks != null && !mAircraft.get(selectedAc).getBatteryCriticalState()) {
            try {
                //Set launch button to active, select the takeoff block and request the service to execute it
                Bundle carrier = new Bundle();
                carrier.putString("TYPE", "REQUEST_TAKE_OFF");
                carrier.putShort("SEQ", (short) mAircraft.get(selectedAc).missionBlocks.indexOf(getResources().getString(R.string.takeoff_block)));
                mServiceClient.onCallback(carrier, selectedAc);
            } catch (RemoteException e) {
                Log.e(TAG,"Error while requesting the service to execute the takeoff block");
            }
        }
	}

    //Method to handle home button clicks
	public void onGoHomeRequest(View v) {
        if(selectedAc == 0) {
            //Notify the user that no aircraft is selected
            Toast.makeText(getApplicationContext(), "No aircraft selected!", Toast.LENGTH_SHORT).show();
            //request to go home if connected and the mission blocks are loaded
        } else if(isConnected && mAircraft.get(selectedAc).hasCommConnection() && mAircraft.get(selectedAc).missionBlocks != null && !mAircraft.get(selectedAc).getBatteryCriticalState()) {
            try {
                Bundle carrier = new Bundle();
                carrier.putString("TYPE", "BLOCK_SELECTED");
                carrier.putShort("SEQ", (short) mAircraft.get(selectedAc).missionBlocks.indexOf(getResources().getString(R.string.go_home_block)));
                mServiceClient.onCallback(carrier,selectedAc);
            } catch (RemoteException e) {
                Log.e(TAG,"Error while requesting the service to execute the go home block");
            }
        }
	}

    public void onBlocksRequest(View v) {
        missionButtons.onBlocksRequest(v);

        //Request mission blocks if connected to the service
        if (isConnected) {
            try {
                Bundle carrier = new Bundle();
                carrier.putString("TYPE", "REQUEST_BLOCK_LIST");
                mServiceClient.onCallback(carrier,-1); //acNumber input is set to -1 as no specific input is needed: blocklists of all aircraft will be sent

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
                carrier.putString("TYPE", "REQUEST_ALL_WP_LISTS");
                mServiceClient.onCallback(carrier,-1); //acNumber input is set to -1 as no specific input is needed: waypointlists of all aircraft will be sent

                Toast.makeText(getApplicationContext(), "Refreshing Waypoints.", Toast.LENGTH_SHORT).show();
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
		
		//Disable rotation and tilt gestures
		map.getUiSettings().setRotateGesturesEnabled(false);
		map.getUiSettings().setTiltGesturesEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
		
		//Show my location button
		map.getUiSettings().setMyLocationButtonEnabled(false);
		
		//Enable marker/infowindow listeners
		map.setOnMarkerClickListener(this);     //Click listener on markers
        map.setOnMarkerDragListener(this);      //Drag listener on markers
        map.setOnInfoWindowClickListener(this); //Click listener on infowindows
	}

	/* Marker listener for (de)selection aircraft icons, waypoint marker actions and home marker selection */
	@Override
    public boolean onMarkerClick(final Marker marker) {
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
        } else if(marker.getSnippet().equals("HOME")) {                 //Home marker clicked
            //Do nothing (yet)
        } else if(!marker.getSnippet().contains("-")) {                 //Aircraft marker clicked
            int acNumber = Integer.parseInt(marker.getSnippet());
            //When the aircraft icon is clicked, select it or deselect it
            if(!mAircraft.get(acNumber).isSelected()) {
                if(aircraftSelected) { //If another aircraft is selected
                                        //Set all aircraft to not selected
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
                //Update the blocks spinner
//                updateMissionBlocksSpinner(acNumber);
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
        //Save original position
        origMarkerPosition = marker.getPosition();
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        //Show a circle that indicates the path the aircraft will follow
        drawSurveillancePath(marker.getPosition(), false);
    }

    //Action implementation on end of marker (waypoint) drag
    @Override
    public void onMarkerDragEnd(Marker marker) {
        //Hide the flight path circle
        drawSurveillancePath(marker.getPosition(),true);
        //Get the marker snippet to extract the aircraft- and waypoint number
        String[] numbers = marker.getSnippet().split("-");
        int acNumber = Integer.parseInt(numbers[0]);
        int wpNumber = Integer.parseInt(numbers[1]);
        //Get the drop location
        LatLng newPosition = marker.getPosition();

        if(mAircraft.get(acNumber).hasCommConnection()) {
            try {
                Bundle carrier = new Bundle();
                carrier.putString("TYPE", "WRITE_WP");
                carrier.putParcelable("WP", new Waypoint((float) Math.toRadians(newPosition.latitude), (float) Math.toRadians(newPosition.longitude), mAircraft.get(acNumber).getWpAlt(wpNumber), mAircraft.get(acNumber).getWpSeq(wpNumber), mAircraft.get(acNumber).getWpTargetSys(wpNumber), mAircraft.get(acNumber).getWpTargetComp(wpNumber)));
                mServiceClient.onCallback(carrier, acNumber);
            } catch (RemoteException e) {
                Log.e(TAG, "Error while sending waypoint to the service");
            }
        } else {
            marker.setPosition(origMarkerPosition);
        }

        //Set the waypoint position, status to updating and redraw the marker
        mAircraft.get(acNumber).setWpLatLon((float) newPosition.latitude, (float) newPosition.longitude, wpNumber);
        mAircraft.get(acNumber).setWpUpdating(wpNumber);
        waypointUpdater(acNumber);
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
        for(int i=0; i<mAircraft.size(); i++) {
            int acNumber = mAircraft.keyAt(i);
            //Determine the color of the aircraft icon based on selection status
            if(mAircraft.get(acNumber).isSelected()) {
                mAircraft.get(acNumber).setCircleColor(getResources().getColor(R.color.yellow));
            } else {
                mAircraft.get(acNumber).setCircleColor(Color.WHITE);
            }

            //Update the flight tracks with the current positions
            mAircraft.get(acNumber).updateFlightPath();

            //Generate an icon
            mAircraft.get(acNumber).generateIcon();
        }

        //Call GoogleMaps
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                //Loop over all aircraft
                for (int i = 0; i < mAircraft.size(); i++) {
                    int acNumber = mAircraft.keyAt(i);

                    //Clear marker from map (if it exists)
                    if (mAircraft.get(acNumber).acMarker != null) {
                        mAircraft.get(acNumber).acMarker.remove();
                    }

                    Log.d("nullPointerTEST",String.valueOf(mAircraft.get(acNumber).acMarker==null));

                    //Add marker to map with the following settings and save it in the aircraft object
                    mAircraft.get(acNumber).acMarker = map.addMarker(new MarkerOptions()
                                    .position(mAircraft.get(acNumber).getLatLng())
                                    .anchor((float) 0.5, (float) 0.5)
                                    .icon(BitmapDescriptorFactory.fromBitmap(mAircraft.get(acNumber).getIcon()))
                                    .title(" " + mAircraft.get(acNumber).getLabelCharacter())
                                    .snippet(String.valueOf(acNumber))
                                    .infoWindowAnchor(0.5f, mAircraft.get(acNumber).getIconBoundOffset())
                                    .flat(true)
                                    .draggable(false)
                    );

                    //Either show the label or the detailed information window of the aircraft based on selection status
                    if (mAircraft.get(acNumber).isSelected()) {
                        //Make aircraft number final to use in inner class
                        final int acNumb = acNumber;

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

                                //Get handles to the textviews
                                TextView infoDistHome = (TextView) v.findViewById(R.id.info_dist_home);
                                TextView infoAlt = (TextView) v.findViewById(R.id.info_alt);
                                TextView infoTask = (TextView) v.findViewById(R.id.info_task);
                                TextView infoMode = (TextView) v.findViewById(R.id.info_mode);
                                TextView infoBattery = (TextView) v.findViewById(R.id.info_battery);

                                //Set the values in the information windows
                                infoDistHome.setText("Distance Home: " + String.format("%.0f", mAircraft.get(acNumb).getDistanceHome()) + "m");
                                infoAlt.setText("Altitude: " + String.format("%.1f", mAircraft.get(acNumb).getAGL()) + "m");
                                infoBattery.setText("Battery voltage: " + String.format("%.1f", mAircraft.get(acNumb).getBattVolt() / 1000.0) + "V");
                                //Current block
                                if (mAircraft.get(acNumb).getCurrentBlock() != null) {
                                    if (mAircraft.get(acNumb).getCurrentBlock().equals(getResources().getString(R.string.land_block))) {
                                        infoMode.setText("Status: " + "LANDING");
                                    } else if (mAircraft.get(acNumb).getCurrentBlock().equals(getResources().getString(R.string.takeoff_block))) {
                                        infoMode.setText("Status: " + "TAKE-OFF");
                                    } else if(mAircraft.get(acNumb).getCurrentBlock().equals(getResources().getString(R.string.go_home_block))) {
                                        infoMode.setText("Status: " + "GOING HOME");
                                    } else if (mAircraft.get(acNumb).getCurrentBlock().equals("final")) {
                                        infoMode.setText("Status: " + "LANDING");
                                    } else if(mAircraft.get(acNumb).getCurrentBlock().equals("flare")) {
                                        infoMode.setText("Status: " + "LANDED");
                                    } else {
                                        infoMode.setText("Status: " + "normal");
                                    }
                                } else {
                                    infoMode.setText("Status: " + "");
                                }

                                //Task of aircraft
                                infoTask.setText("Task: " + String.valueOf(mAircraft.get(acNumb).getTaskStatus()));

                                return v;
                            }
                        });

                        //Set the marker to show the information window
                        if (mAircraft.get(acNumber).getShowInfoWindow()) {
                            mAircraft.get(acNumber).acMarker.showInfoWindow();
                        }
                    }

                    ///* FLIGHT PATH  *///
                    if (getResources().getInteger(R.integer.flightPathOnOff) == 1 && !mAircraft.get(acNumber).getFlightPath().isEmpty()) {
                        // If the flight path has been drawn before, remove it to be updated
                        if (mAircraft.get(acNumber).flightTrackPoly != null) {
                            mAircraft.get(acNumber).flightTrackPoly.remove();
                        }

                        // Draw the flight path with the specified characteristics
                        mAircraft.get(acNumber).flightTrackPoly = map.addPolyline(new PolylineOptions()
                                .addAll(mAircraft.get(acNumber).getFlightPath())
                                .width(4)
                                .color(Color.WHITE));
                    }

                    ////COVERAGE INDICATION
                    //Remove the coverage circle if it was drawn before
                    if (mAircraft.get(acNumber).coverageCircle != null) {
                        mAircraft.get(acNumber).coverageCircle.remove();
                    }

                    //If the operator wants to see coverage, only for aircraft with the surveillance task status
                    // and detect if the distance between the aircraft and waypoint is less than the survey circle radius
                    if (showCoverage && (mAircraft.get(acNumber).getTaskStatus() == TaskStatus.SURVEILLANCE || mAircraft.get(acNumber).getTaskStatus() == TaskStatus.SURVEILLOSTCOMM)) {
                        //Bitmap and canvas to draw a circle on
                        Bitmap baseIcon = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
                        Canvas circleCanvas = new Canvas(baseIcon);
                        Paint circlePaint = new Paint();
                        if(mAircraft.get(acNumber).getCommunicationSignal() > 0) {
                            if(mAircraft.get(acNumber).getDistanceToWaypoint() <= acCoverageRadius) {
                                circlePaint.setColor(getResources().getColor(R.color.coverageGreen));
                            } else {
                                circlePaint.setColor(getResources().getColor(R.color.coverageYellow));
                            }
                        } else {
                            circlePaint.setColor(getResources().getColor(R.color.coverageRed));
                        }

                        circlePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
                        //Circle fill
                        circleCanvas.drawCircle(200, 200, 200, circlePaint);

                        //Circle stroke
                        circlePaint.setStyle(Paint.Style.STROKE);
                        circlePaint.setColor(Color.BLACK);
                        circleCanvas.drawCircle(200, 200, 200, circlePaint);

                        GroundOverlayOptions ROI = new GroundOverlayOptions()
                                .image(BitmapDescriptorFactory.fromBitmap(baseIcon))
                                .position(mAircraft.get(acNumber).getWpLatLng(0), acCoverageRadius * 2, acCoverageRadius * 2); //m

                        // Get back the relay Circle object
                        mAircraft.get(acNumber).coverageCircle = map.addGroundOverlay(ROI);
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
                for (int i = 0; i < mAircraft.get(acNumber).wpMarkers.size(); i++) {
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

                //Draw the aircraft label on the waypoint marker
                markerCanvas.drawText(mAircraft.get(acNumber).getLabelCharacter(), wpMarkerBitmap.getWidth() / 2, wpMarkerBitmap.getHeight() / 2 + 4, markerPaint);

                //Add waypoint marker to map
                Marker wpMarker = map.addMarker(new MarkerOptions()
                                .position(mAircraft.get(acNumber).getWpLatLng(i))
                                .flat(true)
                                .anchor((float) 0.5, (float) 0.5)
                                .icon(BitmapDescriptorFactory.fromBitmap(wpMarkerBitmap))
                                .snippet(String.valueOf(acNumber) + "-" + String.valueOf(i))
                                .draggable(true)
                );
                //Add the newly generated waypoint marker to the list to keep reference to it
                mAircraft.get(acNumber).wpMarkers.add(wpMarker);
            }
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

                /* TODO: make the initial zoom level correspond with the Region of Interest size (so it does not fill the entire screen) */
                //Move camera to the home location
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(home.getHomeLocation(), initialZoomLevel));
            }
        });
    }

    /* Draw the area in which communication is possible */
    private void drawCommunicationRange(final List<Integer> relayAc) {
        //Only call the map if something needs to be drawn or if something needs to be removed from the map
        if(showCommRange) {
            //Call GoogleMaps
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap map) {

                    //Remove the communication range circle around home if it was drawn before
                    if (home.homeCommCircle != null) {
                        home.homeCommCircle.remove();
                    }
                    //Remove the communication range circle around the relay aircraft if it was drawn before
                    if (!relayCommCircles.isEmpty()) {
                        //Remove all circles
                        for(int i=0; i< relayCommCircles.size(); i++) {
                            relayCommCircles.get(i).remove();
                        }
                        //Clear the list
                        relayCommCircles.clear();
                    }

                    //Add the home communication range circle to the map
                    CircleOptions homeCircleOptions = new CircleOptions()
                            .center(home.getHomeLocation())
                            .strokeWidth(getResources().getInteger(R.integer.circleStrokeWidth))
                            .strokeColor(getResources().getColor(R.color.commRange))
                            .radius(commMaxRange); // In meters

                    // Get back the home Circle object
                    home.homeCommCircle = map.addCircle(homeCircleOptions);

                    //If one or more relay UAVs are active
                    if (!relayAc.isEmpty()) {
                        for (int i = 0; i < relayAc.size(); i++) {
//                            if(mAircraft.get(relayAc.get(i)).getDistanceToWaypoint() <= acCoverageRadius) {
                                // Draw the relay communication range circle
                                CircleOptions relayCircleOptions = new CircleOptions()
                                        .strokeWidth(getResources().getInteger(R.integer.circleStrokeWidth))
                                        .strokeColor(getResources().getColor(R.color.commRange));
                                if (showMinCommRange && mAircraft.get(relayAc.get(i)).getDistanceToWaypoint() <= acCoverageRadius) {
                                    relayCircleOptions.center(mAircraft.get(relayAc.get(i)).getWpLatLng(0));    //Around waypoint
                                    relayCircleOptions.radius(commRelayRange - (surveillanceCircleRadius));     //In meters
                                } else {
                                    relayCircleOptions.center(mAircraft.get(relayAc.get(i)).getLatLng());       //Around aircraft
                                    relayCircleOptions.radius(commRelayRange); // In meters
                                }
                                // Get back the relay Circle object
                                Circle relayCommCircle = map.addCircle(relayCircleOptions);
                                relayCommCircles.add(relayCommCircle);
//                            }
                        }
                    }
                }
            });
            //If no communication ranges need to be drawn, remove them
        } else {
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap map) {

                    //Remove the communication range circle around home if it was drawn before
                    if (home.homeCommCircle != null) {
                        home.homeCommCircle.remove();
                    }
                    //Remove the communication range circle around the relay aircraft if it was drawn before
                    if (!relayCommCircles.isEmpty()) {
                        //Remove all circles
                        for(int i=0; i< relayCommCircles.size(); i++) {
                            relayCommCircles.get(i).remove();
                        }
                        //Clear the list
                        relayCommCircles.clear();
                    }
                }
            });
        }
    }

    ///////* Indicate a region of interest on the map *///////
    private void drawROI(final List<LatLng> ROIlocs) {
        //Call GoogleMaps
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {

                if(!ROIOverlayList.isEmpty()) {
                    for(int i=0; i<ROIOverlayList.size(); i++) {
                        ROIOverlayList.get(i).remove();
                    }
                }
                Log.d("TEST", String.valueOf(ROIlocs.size()));
                for(int n = 0; n < ROIlocs.size(); n++) {
                    //Bitmap and canvas to draw a circle on
                    int circleSize = 300;
                    Bitmap baseIcon = Bitmap.createBitmap(circleSize, circleSize, Bitmap.Config.ARGB_8888);
                    Canvas circleCanvas = new Canvas(baseIcon);
                    Paint circlePaint = new Paint();
                    circlePaint.setColor(getResources().getColor(R.color.regionOfInterest));
                    circlePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
                    circleCanvas.drawCircle(circleSize / 2, circleSize / 2, circleSize / 2, circlePaint);

                    GroundOverlayOptions ROI = new GroundOverlayOptions()
                            .image(BitmapDescriptorFactory.fromBitmap(baseIcon))
                            .position(ROIlocs.get(n), ROIradiiList.get(n) * 2, ROIradiiList.get(n) * 2); //m
                    GroundOverlay ROIOverlay = map.addGroundOverlay(ROI);
                    //Save groundoverlay in a list
                    ROIOverlayList.add(ROIOverlay);
                }
            }
        });
    }

    ///////* Indicate the path the surveilance UAV will fly on the map *///////
    private void drawSurveillancePath(final LatLng location, final boolean endDraw) {
        //Call GoogleMaps
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                //Remove circles
                if(flightPath != null) {
                    flightPath.remove();
                    flightPath = null;
                }
                if(surveillanceBound != null) {
                    surveillanceBound.remove();
                    surveillanceBound = null;
                }
                //Draw circles
                if(!endDraw) {
                    CircleOptions flightPathOptions = new CircleOptions()
                            .center(location)
                            .strokeWidth(getResources().getInteger(R.integer.circleStrokeWidth))
                            .strokeColor(getResources().getColor(R.color.surveillancePath))
                            .radius(surveillanceCircleRadius+2); // In meters

                    // Get back the relay Circle object
                    flightPath = map.addCircle(flightPathOptions);

                    CircleOptions boundOptions = new CircleOptions()
                            .center(location)
                            .strokeWidth(getResources().getInteger(R.integer.circleStrokeWidth))
                            .strokeColor(getResources().getColor(R.color.surveillancePath))
                            .radius(acCoverageRadius+2); // In meters

                    // Get back the relay Circle object
                    surveillanceBound = map.addCircle(boundOptions);
                }
            }
        });
    }

    //Method to clear all objects from the map
    private void clearMap() {
        //Call GoogleMaps
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                map.clear();
            }
        });
    }

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
                if (conflictingAircraft != null) {
                    for (int i = 0; i < conflictingAircraft.size(); i += 2) {

                        //Draw a connecting line on the map with the following settings
                        Polyline connectingLine = map.addPolyline(new PolylineOptions()
                                .add(mAircraft.get(conflictingAircraft.get(i)).getLatLng(), mAircraft.get(conflictingAircraft.get(i + 1)).getLatLng())
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

    //Method to update the battery values based UAV tasks and scenario settings
    private void updateScenarioBatteryValues() {
        if (activeScenario != 0) {
            for (int i = 0; i < mAircraft.size(); i++) {
                int acNumber = mAircraft.keyAt(i);
                int battLoss = 0;
                if (mAircraft.get(acNumber).getTaskStatus().equals(TaskStatus.RELAY)) {
                    battLoss = getResources().getInteger(R.integer.relayBatRate) + mAircraft.get(acNumber).getCommLossBatteryLoss();
                    mAircraft.get(acNumber).resetCommLossBattery();
                } else if (mAircraft.get(acNumber).hasCommConnection() && mAircraft.get(acNumber).isFlying()) {
                    battLoss = getResources().getInteger(R.integer.surveillanceBatRate) + mAircraft.get(acNumber).getCommLossBatteryLoss();
                    mAircraft.get(acNumber).resetCommLossBattery();
                } else if (mAircraft.get(acNumber).isFlying()){
                    mAircraft.get(acNumber).setCommLossBattery(getResources().getInteger(R.integer.surveillanceBatRate));
                }

                //Set the new battery value
                mAircraft.get(acNumber).setBatteryState(mAircraft.get(acNumber).getBattVolt() - battLoss, -1, -1);

                //Assign critical battery status if this level is reached and command aircraft to land
                if(mAircraft.get(acNumber).getBattVolt()<=getResources().getInteger(R.integer.CriticalBatteryVoltage) && !mAircraft.get(acNumber).missionAborted) {
                    mAircraft.get(acNumber).setBatteryCriticalState();
                    //Abort mission (command aircraft to land)
                    abortMission(acNumber);
                    mAircraft.get(acNumber).missionAborted = true;
                }
            }

            //Timed malfunction of the battery of a certain aircraft
            if (mAircraft.get(batteryFailureAircraft) != null && timeLeft <= batteryFailureTime && batteryFailureAircraft != 0 && !batteryFailureOccured) {
                mAircraft.get(batteryFailureAircraft).setBatteryState(getResources().getInteger(R.integer.batteryFailureVoltage), -1, -1);
                batteryFailureOccured = true;
            }
        }
    }

    //Method to check which aircraft have the relay status in order to keep track of which communication range circles have to be drawn
    private void checkAircraftTaskStatus() {
        //Check if aircraft should be flagged for a certain task (Relay, surveillance or none)
        for(int i=0; i < mAircraft.size(); i++) {
            int key = mAircraft.keyAt(i);
            //Relay status:If the difference between the altitude and the surveillance altitude is less than 2m and it is within the home comm range
            if(Math.abs(relayAltitude-mAircraft.get(key).getAGL()) < altitudeAccuracyDistance && mAircraft.get(key).hasCommConnection()) {
                mAircraft.get(key).setTaskStatus(TaskStatus.RELAY);
            //Surveillance status
            } else if(Math.abs(surveillanceAltitude-mAircraft.get(key).getAGL()) < altitudeAccuracyDistance && mAircraft.get(key).hasCommConnection()) {
                mAircraft.get(key).setTaskStatus(TaskStatus.SURVEILLANCE);
            } else if(Math.abs(surveillanceAltitude-mAircraft.get(key).getAGL()) < altitudeAccuracyDistance && !mAircraft.get(key).hasCommConnection()) {
                mAircraft.get(key).setTaskStatus(TaskStatus.SURVEILLOSTCOMM);
            } else {
                mAircraft.get(key).setTaskStatus(TaskStatus.NONE);
            }
        }

        //Clear the list holding the relay aircraft numbers
        if(!Aircraft.relayAircraft.isEmpty()) {
            Aircraft.relayAircraft.clear();
        }

        //Update the list
        for(int i=0; i < mAircraft.size(); i++) {
            int acNumber = mAircraft.keyAt(i);
            if(mAircraft.get(acNumber).getTaskStatus() == TaskStatus.RELAY) {
                Aircraft.relayAircraft.add(acNumber);
            }
        }

        //Send the relay locations to aircraft class
        List<LatLng> relayLocations = new ArrayList<>();
        for(int i=0; i < Aircraft.relayAircraft.size(); i++) {
            relayLocations.add(mAircraft.get(Aircraft.relayAircraft.get(i)).getLatLng());
        }
        Aircraft.setRelayList(relayLocations);
    }

    //Method to check for conflicts between aircraft
    private void checkConflicts() {
        //check for altitude and course conflicts and put the aircraft numbers in lists of aircraft that have a conflict or are on the same level
        for(int i = 0; i < mAircraft.size(); i++) {
            int iKey = mAircraft.keyAt(i);
            for(int j = 0; j < mAircraft.size(); j++) {
                int jKey = mAircraft.keyAt(j);
                if(iKey!=jKey) {
                    if(Math.abs(mAircraft.get(iKey).getAGL() - mAircraft.get(jKey).getAGL()) <= verticalSeparationStandard) {
                        //Check for conflict course
                        if(isOnconflictCourse(iKey,jKey)){ //Pait is on conflict course, so add to conflict list
                            conflictingAircraft.add(iKey);
                            conflictingAircraft.add(jKey);
                        }
                        //Pair is not on conflict course, so add to same level list
                        sameLevelAircraft.add(iKey);
                        sameLevelAircraft.add(jKey);

                    } else { //Pair is not at the same level, so they are safe and a "GRAY" status can be given
                        mAircraft.get(iKey).setConflictStatusNew(ConflictStatus.GRAY);
                        mAircraft.get(jKey).setConflictStatusNew(ConflictStatus.GRAY);
                    }
                }
            }
        }

        //Remove double couples from the conflictlist
        if (conflictingAircraft.size()>2) {
            conflictingAircraft = removeDoubleCouples(conflictingAircraft);
        }

        //Remove double couples from the samelevellist
        if (sameLevelAircraft.size()>2) {
            sameLevelAircraft = removeDoubleCouples(sameLevelAircraft);
        }

        //Assign the "BLUE" status to all aircraft that fly on the same level
        if(!sameLevelAircraft.isEmpty()) {
            for (int k = 0; k < sameLevelAircraft.size(); k += 2) {
                mAircraft.get(sameLevelAircraft.get(k)).setConflictStatusNew(ConflictStatus.BLUE);
                mAircraft.get(sameLevelAircraft.get(k + 1)).setConflictStatusNew(ConflictStatus.BLUE);
            }
        }

        //Assign the "RED" status to all aircraft that have a conflict
        if(!conflictingAircraft.isEmpty()) {
            for (int k = 0; k < conflictingAircraft.size(); k += 2) {
                if(mAircraft.get(conflictingAircraft.get(k)).isFlying() && mAircraft.get(conflictingAircraft.get(k+1)).isFlying()) {
                    mAircraft.get(conflictingAircraft.get(k)).setConflictStatusNew(ConflictStatus.RED);
                    mAircraft.get(conflictingAircraft.get(k + 1)).setConflictStatusNew(ConflictStatus.RED);
                } else { //If the aircraft do not fly (yet)
                    mAircraft.get(conflictingAircraft.get(k)).setConflictStatusNew(ConflictStatus.BLUE);
                    mAircraft.get(conflictingAircraft.get(k + 1)).setConflictStatusNew(ConflictStatus.BLUE);
                }
            }
        }

        //First clear the grouplist to reset
        groupList.clear();

        //Create groups of the pairs
        if(!sameLevelAircraft.isEmpty()) {
            //Fill the group list with the first pair there is
            List<Integer> couple = new ArrayList<>();
            couple.add(sameLevelAircraft.get(0));
            couple.add(sameLevelAircraft.get(1));
            groupList.add(couple);

            //In case more than more couple is present, check if they should be merged are put as a separate one
            if(sameLevelAircraft.size()>2) {
                for(int i = 2; i < sameLevelAircraft.size(); i += 2) { //Loop over the couples
                    Boolean isPairInList = false;
                    for(int j = 0; j < groupList.size(); j++){ //Loop over the sets already in the grouplist
                        if(groupList.get(j).contains(sameLevelAircraft.get(i)) || groupList.get(j).contains(sameLevelAircraft.get(i+1))){//If one of the two aircraft is already present in another set
                            if(!groupList.get(j).contains(sameLevelAircraft.get(i))) {
                                groupList.get(j).add(sameLevelAircraft.get(i));
                            }
                            if(!groupList.get(j).contains(sameLevelAircraft.get(i+1))) {
                                groupList.get(j).add(sameLevelAircraft.get(i+1));
                            }
                            isPairInList = true;
                        }
                    }
                    if(!isPairInList) {
                        List<Integer> newCouple = new ArrayList<>();
                        newCouple.add(sameLevelAircraft.get(i));
                        newCouple.add(sameLevelAircraft.get(i+1));
                        groupList.add(newCouple);
                    }
                }
            }
            //Sort all sets
            for(int k = 0; k < groupList.size(); k++) {
                Collections.sort(groupList.get(k));
            }
        }
    }

    //Remove double couples from the aircraft list
    private ArrayList<Integer> removeDoubleCouples(ArrayList<Integer> list) {
        //Loop starting at the end of the list to be able to remove double pairs
        for (int p = list.size() - 2; p > 1; p -= 2) {
            for (int q = 0; q < p; q += 2) {
                if (list.get(p + 1) == list.get(q) && list.get(p) == list.get(q + 1)) {
                    list.remove(p + 1);
                    list.remove(p);
                    break;
                }
            }
        }
        return list;
    }

    //Method to determine if a couple of aircraft needs to get conflict status
    private boolean isOnconflictCourse(int ac1, int ac2) {
        /*Steps taken:
        * Check if both aircraft have active surveillance waypoints
        * If yes, the distance should be more than 2 times the surveillance circle radius
        * If not, the distance between the aircraft should be more than the horizontal separation standard
        */

        boolean isInconflictcourse = false;

        if(!mAircraft.get(ac1).getWpLatLngList().isEmpty() && !mAircraft.get(ac2).getWpLatLngList().isEmpty() && (mAircraft.get(ac1).getTaskStatus()==TaskStatus.SURVEILLANCE || mAircraft.get(ac1).getTaskStatus()==TaskStatus.RELAY)
        && (mAircraft.get(ac2).getTaskStatus()==TaskStatus.SURVEILLANCE || mAircraft.get(ac2).getTaskStatus()==TaskStatus.RELAY )) {
            LatLng wp1 = mAircraft.get(ac1).getWpLatLng(0);
            LatLng wp2 = mAircraft.get(ac2).getWpLatLng(0);

            //Calculate the distance between the two survey waypoints
            float[] distance = new float[1];
            Location.distanceBetween(wp1.latitude, wp1.longitude, wp2.latitude, wp2.longitude, distance);
            //Detect conflict if the distance between the waypoints is less than the survey circle diameter + the horizontal separation standard
            if(distance[0] <= (2*acCoverageRadius)) isInconflictcourse = true;
        } else {
            //Calculate the distance between the two aircraft
            float[] distance = new float[1];
            Location.distanceBetween(mAircraft.get(ac1).getLat(), mAircraft.get(ac1).getLon(), mAircraft.get(ac2).getLat(), mAircraft.get(ac2).getLon(), distance);

            //Detect conflict if the distance between the waypoints is less than the survey circle diameter + the horizontal separation standard
            if(distance[0] <= horizontalSeparationStandard) isInconflictcourse = true;
        }
        return isInconflictcourse;
    }

	/////////////////////////ALTITUDE TAPE/////////////////////////

    //Method to update the labels on the altitude tape
	private void updateAltitudeTape() {
        //Boolean to check if group labels are drawn and if they need to be removed from the tape
        boolean groupLabelsDrawn = false;

        //Check if all aircraft in the group selection are still in the group
        for(int k=0; k<groupSelectedAircraft.size(); k++) {
            boolean inGroup = false;
            int ac = groupSelectedAircraft.get(k);

            for(int l=0; l<groupList.size(); l++) {
                if(groupList.get(l).contains(ac)) {
                    inGroup = true;
                    break;
                }
            }
            if(inGroup==false) {
                deselectAllAircraft();
                altitudeTapeFragment.removeGroupSelectedAircraft();
                break;
            }
        }

        //Check if the group selection is smaller than the actual group
        for(int q=0; q<groupList.size(); q++) {
            boolean wrong = false;
            for(int w=0; w<groupSelectedAircraft.size(); w++) {
                Log.d("tret1",String.valueOf(groupList.get(q).contains(groupSelectedAircraft.get(w))));
                Log.d("tret2",String.valueOf(!groupSelectedAircraft.containsAll(groupList.get(q))));
                if(groupList.get(q).contains(groupSelectedAircraft.get(w)) && !groupSelectedAircraft.containsAll(groupList.get(q))) {
                    wrong = true;
                    break;
                }
            }
            Log.d("tret*",String.valueOf(wrong));
            if(wrong) {
                deselectAllAircraft();
                altitudeTapeFragment.removeGroupSelectedAircraft();
                break;
            }
        }

        //Check if a certain conflict group is selected. If yes, do not draw the group label but draw single labels on the left side of the tape.
        for(int i=0; i< groupSelectedAircraft.size(); i++) {
            altitudeTapeFragment.drawGroupSelection(mAircraft.get(groupSelectedAircraft.get(i)).getAGL(),mAircraft.get(groupSelectedAircraft.get(i)).getLabelCharacter(),i,groupSelectedAircraft.size(),groupSelectedAircraft.get(i));
        }

        //Put the grouped labels on the altitude tape
        if (!groupList.isEmpty()) {
            for (int k = 0; k < groupList.size(); k++) { //Loop over conflict groups
                Boolean drawRedLabel = false;
                double altSum = 0;
                String characters = "";
                for(int b = 0; b<groupList.get(k).size(); b++){//Loop over conflict group list
                    altSum = altSum + mAircraft.get(groupList.get(k).get(b)).getAGL();
                    characters = characters + mAircraft.get(groupList.get(k).get(b)).getLabelCharacter() + " ";
                }

                for(int a = 0; a<groupList.get(k).size(); a++) {
                    if(mAircraft.get(groupList.get(k).get(a)).getConflictStatus().equals(ConflictStatus.RED)) {
                        drawRedLabel = true;
                        break;
                    }
                }

                characters = characters.substring(0,characters.length()-1);
                //Calculate the mean altitude of the aircraft that are in the group
                double meanAlt = altSum/groupList.get(k).size();
                //Draw the group label on the tape
                altitudeTapeFragment.drawGroupLabel(drawRedLabel, meanAlt, characters, groupList.get(k));
            }
            groupLabelsDrawn = true;
        }

        /* Put the single labels on the altitude tape (if they are not in a group).*/
        for (int i=0; i < mAircraft.size(); i++) {
            int acNumber = mAircraft.keyAt(i);
            //Check if the aircraft is in a group, if so, don't show the individual label
            if (!mAircraft.get(acNumber).getConflictStatus().equals(ConflictStatus.GRAY)) { //If the aircraft has a "GRAY" conflict status it can be drawn as individual label
                singleLabelVisibility = View.GONE;
            } else {
                singleLabelVisibility = View.VISIBLE;
            }
            //Set the individual label on the tape
            altitudeTapeFragment.setLabel(mAircraft.get(acNumber).getAGL(), mAircraft.get(acNumber).getAltLabelId(), mAircraft.get(acNumber).getLabelCharacter(), mAircraft.get(acNumber).isSelected(), mAircraft.get(acNumber).isLabelCreated(), acNumber, singleLabelVisibility);
        }

        //If no group labels were drawn in this iteration, remove any existing ones because there are no groups anymore
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

        //Update the blocks spinner
//        updateMissionBlocksSpinner(aircraftNumber);

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
        //Loop over all aircraft to deselect them
        for(int i=0; i<mAircraft.size(); i++) {
            int acNumber = mAircraft.keyAt(i);
            mAircraft.get(acNumber).setIsSelected(false);
        }

        //Set the selected aircraft number to 0 (=none)
        selectedAc = 0;

        //Update the mission buttons
        updateMissionButtons();
        groupSelectedAircraft.clear();

        //Update the altitude tape (set all labels back to unselected)
        updateAltitudeTape();

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

        //Update the altitude tape
        updateAltitudeTape();
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
            telemetryFragment.setVisible(false);
        } else {
            telemetryFragment.setVisible(true);
            telemetryFragment.setTextColor(Color.YELLOW);
            telemetryFragment.setText(String.format("%.1f", altitude));
        }
    }

    public double getAircraftAltitude(int acNumber) {
        return mAircraft.get(acNumber).getAltitude();
    }

    //////////////////////////MISSION COMMANDS/////////////////////////
    //Method used by the altitude tape fragment to change the altitude of a waypoint (send to service)
    public void changeCurrentWpAltitude(int acNumber, double AGL) {
        if(mAircraft.get(acNumber).hasCommConnection() && !mAircraft.get(acNumber).getWpLatLngList().isEmpty()) {
            double groundLevel = mAircraft.get(acNumber).getAltitude() - mAircraft.get(acNumber).getAGL();
            double wpAltitude = groundLevel + AGL;
            int wpNumber = 0;

            Toast.makeText(getApplicationContext(), "Altitude of WP " + String.valueOf(wpNumber) + " to " + String.format("%.1f", wpAltitude) + " m", Toast.LENGTH_SHORT).show();

            //Send update waypoint data to the service (same location, different altitude)
            try {
                Bundle carrier = new Bundle();
                carrier.putString("TYPE", "WRITE_WP");
                carrier.putParcelable("WP", new Waypoint((float) Math.toRadians(mAircraft.get(acNumber).getWpLat(wpNumber)), (float) Math.toRadians(mAircraft.get(acNumber).getWpLon(wpNumber)), (float) (wpAltitude + 14), mAircraft.get(acNumber).getWpSeq(wpNumber), mAircraft.get(acNumber).getWpTargetSys(wpNumber), mAircraft.get(acNumber).getWpTargetComp(wpNumber)));
                mServiceClient.onCallback(carrier, acNumber);
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
        }
    }

    private void abortMission(int acNumber) {
        if(isConnected && mAircraft.get(acNumber).missionBlocks != null) {
            try {
                Bundle carrier = new Bundle();
                carrier.putString("TYPE", "BLOCK_SELECTED");
                carrier.putShort("SEQ", (short) mAircraft.get(acNumber).missionBlocks.indexOf(getResources().getString(R.string.land_block)));
                mServiceClient.onCallback(carrier, acNumber);
            } catch (RemoteException e) {
                Log.e(TAG, "Error while requesting the service to execute the land block (ABORT MISSION)");
            }
        }
    }

    private void forceFinal(int acNumber) {
        Log.d("FORCE FINAL","ac #"+String.valueOf(acNumber));
        //Activate the final block
        try {
            Bundle carrier = new Bundle();
            carrier.putString("TYPE", "BLOCK_SELECTED");
            carrier.putShort("SEQ", (short) mAircraft.get(acNumber).missionBlocks.indexOf(getResources().getString(R.string.final_block)));
            mServiceClient.onCallback(carrier, acNumber);
        } catch (RemoteException e) {
            Log.e(TAG, "Error while requesting the service to execute the final block");
        }
    }
}