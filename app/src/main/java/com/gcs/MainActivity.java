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
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, OnMarkerClickListener, OnInfoWindowClickListener, OnMarkerDragListener {
	
	private static final String TAG = MainActivity.class.getSimpleName();

    //Declaration of handlers and definition of time steps
	private Handler handler, interfaceUpdateHandler;
	private final int mInterval        = 900;   // milliseconds
    private final int blockUpdateDelay = 500;   // milliseconds

    //Declaration of the service client
	IMavLinkServiceClient mServiceClient;
	
	private Button connectButton;
    //Declaration of booleans
	private boolean isConnected       = false;
	private boolean isAltitudeUpdated = false;
    private boolean aircraftSelected  = false;
    private boolean showCommRange     = false;

    //Declaration of the fragments
	private TelemetryFragment     telemetryFragment;
	private BatteryFragment       batteryFragment;
	private AltitudeTape          altitudeTapeFragment;
    private SupportMapFragment    mapFragment;
    private MissionButtonFragment missionButtons;

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
    private float verticalSeparationStandard, horizontalSeparationStandard;
    private int commMaxRange, commRelayRange, singleLabelVisibility;
    private int relayUAV = 0;                       //Set to 0 if none serves as relay (yet)

    //Declaration of items needed for missionblocks
    private MenuItem menuBlockSpinner = null;
    Spinner blockSpinner;
    private List<String> missionBlocks;
	  
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
        verticalSeparationStandard = getResources().getInteger(R.integer.verticalSeparationStandard)/10f; //(Divided by 10 to convert to meters)
        horizontalSeparationStandard = getResources().getInteger(R.integer.horizontalSeparationStandard)/10f;                          //(Divided by 10 to convert to meters)
        commMaxRange = getResources().getInteger(R.integer.commMaxRange);                                             //meters
        commRelayRange = getResources().getInteger(R.integer.commRelayRange);                                           //meters

        /* TODO Move the aircraft instantiation to a more suitable location once the service provides data of multiple aircraft (first Heartbeat?)*/
		// Instantiate aircraft object
        mAircraft.put(1, new Aircraft(getApplicationContext()));

		// Instantiate home object
		home = new Home();

        /* TODO Move this to a better place when service provides the home location */
        // TEMPORARY SETTING OF HOME LOCATION
        LatLng homeLocation = new LatLng(51.990826, 4.378248);
        home.setHomeLocation(homeLocation);

        //TEMPORARY DUMMY AIRCRAFT (Remove this once the service provides data of multiple aircraft)
        mAircraft.put(2, new Aircraft(getApplicationContext()));
        mAircraft.get(2).setLlaHdg(519925740, 43775620, 0, (short) 180);
        mAircraft.get(2).setAltitude(10);
        mAircraft.get(2).setBatteryState(10000, 45, 1);
        mAircraft.get(2).setDistanceHome(homeLocation);
		mAircraft.get(2).setRollPitchYaw(0, 0, 180);

		mAircraft.put(3, new Aircraft(getApplicationContext()));
		mAircraft.get(3).setLlaHdg(519910540, 43794130, 0, (short) 300);
		mAircraft.get(3).setAltitude(10.3);
		mAircraft.get(3).setBatteryState(9000, 1, 1);
		mAircraft.get(3).setDistanceHome(homeLocation);
		mAircraft.get(3).setRollPitchYaw(0, 0, 300);

        mAircraft.put(4, new Aircraft(getApplicationContext()));
        mAircraft.get(4).setLlaHdg(519920900, 43796160, 0, (short) 270);
        mAircraft.get(4).setAltitude(9.7);
        mAircraft.get(4).setBatteryState(12000, 90, 1);
        mAircraft.get(4).setDistanceHome(homeLocation);
        mAircraft.get(4).setRollPitchYaw(0, 0, 270);

		//Create a handles to the fragments
		telemetryFragment = (TelemetryFragment) getSupportFragmentManager().findFragmentById(R.id.telemetryFragment);      //Telemetry fragment
		batteryFragment = (BatteryFragment) getSupportFragmentManager().findFragmentById(R.id.batteryFragment);            //Battery fragment
		altitudeTapeFragment = (AltitudeTape) getSupportFragmentManager().findFragmentById(R.id.altitudeTapeFragment);     //AltitudeTape fragment
		missionButtons = (MissionButtonFragment) getSupportFragmentManager().findFragmentById(R.id.missionButtonFragment); //MissionButton fragment
		
		// Get the map and register for the ready callback
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Start the interface update handler
		interfaceUpdater.run();	/* TODO check if there is a better moment to start this handler (on first heartbeat?) */
	}

    //Menu instantiation
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

        //Set up the spinner in the action bar for the mission block which can be loaded from the service and create a handle
        menuBlockSpinner = menu.findItem(R.id.menu_block_spinner);
        blockSpinner = (Spinner) MenuItemCompat.getActionView(menuBlockSpinner);

        //Listener on item selection in the spinner
        blockSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            //Define what should happen when an item in the spinner is selected
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    mServiceClient.onBlockSelected(position);
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
        return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.menu_request_block_button:
                //Try to request mission blocks if connected to the service
                if (isConnected) {
                    try {
                        mServiceClient.requestMissionBlockList();
                    } catch (RemoteException e) {
                        Log.e(TAG,"Error while requesting mission blocks");
                    }
                }
                return true;
            case R.id.show_comm_range:
                //Show/hide the communication range on screen
                showCommRange = !showCommRange;
                /* TODO move this to the interface updater for continous up-to-date information?? */
                drawCommunicationRange(relayUAV);
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

            //Draw the communication range on the map
//            drawCommunicationRange(relayUAV);

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
                        Log.d("test", sameLevelGroupList.get(i));
                    }

                    //Clear the temporary list
                    groupList.clear();
                }
            }

            //Update the altitude tape
            if (isAltitudeUpdated){
                updateAltitudeTape();
                isAltitudeUpdated = false;
            }

            //Clear the list of aircraft that are on the same level
            sameLevelAircraft.clear();

            //Update aircraft icons and display them on the map
            aircraftMarkerUpdater();

            //Draw the connecting lines on the map that indicate conflicts
            drawConnectingLines();

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

	    		/* TODO Enable the state service case once available */
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
                    updateMissionBlocksSpinnerSelection();
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
                    /* TODO HEARTBEAT use dynamic aicraft number once available in service */
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
                    /* TODO ATTITUDE use dynamic aicraft number once available in service */
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
                    /* TODO ALTITUDE use dynamic aicraft number once available in service */
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
                    /* TODO SPEED use dynamic aicraft number once available in service */
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
                    /* TODO BATTERY use dynamic aicraft number once available in service */
                    /* TODO remove battery level from the aicraft class??*/
                    mAircraft.get(1).setBatteryState(mBattery.getBattVolt(), -1, mBattery.getBattCurrent());

                    /* TODO remove the battery text view when done debugging */
                    //Set the battery text view
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
                    /* TODO POSITION use dynamic aicraft number once available in service */
                    mAircraft.get(1).setSatVisible(mPosition.getSatVisible());
                    //TODO Change heading to int when this is changed in the service
                    mAircraft.get(1).setLlaHdg(519907790, 43774220, mPosition.getAlt(), (short) (mPosition.getHdg()/100));
//                    mAircraft.get(1).setLlaHdg(mPosition.getLat(), mPosition.getLon(), mPosition.getAlt(), (short) (mPosition.getHdg()/100));
                    mAircraft.get(1).setDistanceHome(home.getHomeLocation());
//                    Log.d("DATATESTlat",String.valueOf(mPosition.getLat()));
//                    Log.d("DATATESTlng",String.valueOf(mPosition.getLon()));
//                    Log.d("DATATESTalt",String.valueOf(mPosition.getAlt()/1000));

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
                    /* TODO STATE use dynamic aicraft number once available in service */
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
                    //Get a list of waypoint objects from the service
					List<Waypoint> waypoints = mServiceClient.getWpList();

                     /* TODO WAYPOINTS use dynamic aicraft number once available in service */
                    //Clear the waypoint list if the aircraft already has waypoint data
                    if(mAircraft.get(1).getNumberOfWaypoints()>0) {
                        mAircraft.get(1).clearWpList();
                    }

                    //Loop over the newly received waypoint list to add them individually to the list of the corresponding aircraft
                    for (int i = 0; i < waypoints.size(); i++) {
                        /* TODO add the dynamic setting of the waypoint sequence number, targetSys and targetComp  */
                        mAircraft.get(1).addWaypoint(waypoints.get(i).getLat(), waypoints.get(i).getLon(), waypoints.get(i).getAlt(), (short) i, (byte) 0, (byte) 0);
                    }

                    //Call the method that shows the waypoints on the map
                    waypointUpdater(1);

                    /* TODO show the waypoint updated message toaster after the waypoints of all aircraft are handled */
                    Toast.makeText(getApplicationContext(), "Waypoints updated.", Toast.LENGTH_SHORT).show();

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
                    /* TODO BLOCKS use dynamic aicraft number once available in service */
                    Log.d("BLOCKS","Updated");
                    //Store the mission block list
                    missionBlocks = mServiceClient.getMissionBlockList();

                    //Update the dropdown menu with the block names
                    updateMissionBlocksSpinner();
                } catch (RemoteException e) {
                    Log.e(TAG,"Error while updating mission blocks");
                }
            }
        });
    }

    //Method to update the mission block dropdown menu
    private void updateMissionBlocksSpinner() {
        //Create an array adapter with the mission block names
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, missionBlocks);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Apply the array adapter to the block spinner to update the blocks in the dropdown menu
        blockSpinner.setAdapter(spinnerArrayAdapter);
    }

    //Method to update the selected block in the dropdown menu to the active one
    private void updateMissionBlocksSpinnerSelection() {
        if (missionBlocks.size() > 0) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "Block: " + mServiceClient.getCurrentBlock());
                        blockSpinner.setSelection(mServiceClient.getCurrentBlock());
                    } catch (RemoteException e) {
                        Log.e(TAG,"Error while trying to update the mission block selection in the spinner");
                    }
                }
            }, blockUpdateDelay);
        }
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

    //Method to handle land button clicks
	public void onLandRequest(View v) {
        /* TODO Try to execute one of the landing blocks. Check if landing is executed and update the button that is displayed.*/
//        if(isConnected && !missionBlocks.isEmpty()) {
            //Notify the mission button fragment that the land button is clicked
            missionButtons.onLandRequest(v);
//        }

        //Temporary
        mAircraft.get(3).setAltitude(mAircraft.get(3).getAltitude()-0.1);
	}

    //Method to handle take-off button clicks
	public void onTakeOffRequest(View v) {
        /* TODO Try to execute the take-off block(s). Check if take-off is executed and update the button that is displayed.*/
//        if(isConnected && !missionBlocks.isEmpty()) {
            //Notify the mission button fragment that the take-off button is clicked
            missionButtons.onTakeOffRequest(v);
//        }

        //Temporary
        mAircraft.get(3).setAltitude(mAircraft.get(3).getAltitude()+0.1);
	}

    //Method to handle home button clicks
	public void onGoHomeRequest(View v) {
        /* TODO Try to execute one of the go-home/landing blocks. Check if the drone is going home and update the button that is displayed.*/
//        if(isConnected && !missionBlocks.isEmpty()) {
            //Notify the mission button fragment that the go home button is clicked
            missionButtons.onGoHomeRequest(v);
//        }
	}

    //Method to handle waypoint request button clicks
    public void onWaypointRequest(View v) {
        missionButtons.onWaypointRequest(v);

        //Request the service to send (updated) waypoints if connected
        if (isConnected) {
            try {
                mServiceClient.requestWpList();
            } catch (RemoteException e) {
                Log.e(TAG, "Error while requesting waypoints");
            }
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
		
		//Enable marker/infowindow listeners
		map.setOnMarkerClickListener(this);     //Click listener on markers
        map.setOnMarkerDragListener(this);      //Drag listener on markers
        map.setOnInfoWindowClickListener(this); //Click listener on infowindows

        /* TODO move home marker drawing to a better place once the service send home information */
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

        //Draw the Region Of Interest on the map (Used for test scenario)
        drawROI();
	}

	/* Marker listener for (de)selecttion aircraft icons, waypoint marker actions and home marker selection */
	@Override
    public boolean onMarkerClick(final Marker marker) {
        if(marker.getSnippet().contains("-")){                          //Waypoint marker clicked
            String[] numbers = marker.getSnippet().split("-");
            int acNumber = Integer.parseInt(numbers[0]);
            int wpNumber = Integer.parseInt(numbers[1]);

            /* TODO implement code that commands a selected aircraft to execute the block that corresponds to the clicked waypoint */
        } else if(marker.getSnippet().equals("HOME")) {                 //Home marker clicked
            //Do nothing (yet)
        } else {                                                        //Aircraft marker clicked
            int acNumber = Integer.parseInt(marker.getSnippet());

            //When the aircraft icon is clicked, select it or deselect it
            if(!mAircraft.get(acNumber).isSelected()) {
                if(aircraftSelected) {
                    //Set all aircraft on not selected
                    deselectAllAircraft();
                }
                mAircraft.get(acNumber).setIsSelected(true);
                aircraftSelected = true;
            } else {
                mAircraft.get(acNumber).setIsSelected(false);
                aircraftSelected = false;
            }
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

        /* TODO implement waypoint location change setfunction for the service */
        mAircraft.get(acNumber).setWpLatLon((float) newPosition.latitude, (float) newPosition.longitude, wpNumber);
        /* TODO see if the service automatically sends the updated waypoint information or if this must be requested. In any way, remove the call to the map updater here once this is solved. */
        //Update the waypoints on the map
        waypointUpdater(acNumber);
    }

    //Info window click listener to hide it
    @Override
    public void onInfoWindowClick(final Marker marker) {
        //Get the aircraft number that corresponds with the infowindow
        int clickedAircraft = Integer.parseInt(marker.getSnippet());

        //Set selection status to false
        mAircraft.get(clickedAircraft).setIsSelected(false);
        aircraftSelected = false;
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
                            infoMode.setText("Mode: " + "MODE HERE!");
                            infoBattery.setText("Battery voltage: " + String.valueOf(mAircraft.get(acNumber).getBattVolt()) + "mV");

                            return v;
                        }
                    });
                    //Set the marker to show the information window
                    mAircraft.get(aircraftNumber).acMarker.showInfoWindow();
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
                for (int i = 0; i < mAircraft.get(1).getNumberOfWaypoints(); i++) {
                    mAircraft.get(acNumber).wpMarkers.get(i).remove();
                }

                //Clear the marker list (not the marker data)
                mAircraft.get(acNumber).wpMarkers.clear();
            }

            //(Re)generate waypoint markers
            for (int i=0; i<mAircraft.get(1).getNumberOfWaypoints(); i++) {

                //Add waypoint marker to map
                Marker wpMarker = map.addMarker(new MarkerOptions()
                                .position(mAircraft.get(acNumber).getWpLatLng(i))
                                .flat(true)
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

                    // Get back the mutable Circle
                    homeCommCircle = map.addCircle(homeCircleOptions);

                    //If a relay UAV is active
                    if (relayAc != 0) {

                        // Draw the relay commincation range circle
                        CircleOptions relayCircleOptions = new CircleOptions()
                                .center(mAircraft.get(relayAc).getLatLng())
                                .strokeWidth(5)
                                .strokeColor(0x5500ff00)
                                .radius(commRelayRange); // In meters

                        // Get back the mutable Circle
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

                // Instantiates a new Polyline object and adds points to define a rectangle
                PolygonOptions rectOptions = new PolygonOptions()
                        .fillColor(0x400099CC)
                        .strokeColor(0x400099CC)
                        .add(new LatLng(51.995262, 4.371907))
                        .add(new LatLng(51.995906, 4.374330))
                        .add(new LatLng(51.994248, 4.375434))
                        .add(new LatLng(51.993647, 4.373003))
                        .add(new LatLng(51.995262, 4.371907));

                // Get back the mutable Polyline
                Polygon polygon = map.addPolygon(rectOptions);
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

    //Method to determine if a couple of aircraft needs to get conflict status
    private boolean isOnconflictCourse(int ac1, int ac2) {
        /* TODO make more extensive algorithm to check conflict courses (extrapolation)?? */

        //Calculate the distance between the two aircraft
        float[] distance = new float[1];
        Location.distanceBetween(mAircraft.get(ac1).getLat(), mAircraft.get(ac1).getLon(), mAircraft.get(ac2).getLat(), mAircraft.get(ac2).getLon(), distance);

        //If the distance between the aircraft is larger than the horizontal separation standard, return false, else true
        boolean isInconflictcourse = false;
        if(distance[0] <= horizontalSeparationStandard) { isInconflictcourse = true; };

        return isInconflictcourse;
    }

	/////////////////////////ALTITUDE TAPE/////////////////////////

    //Method to update the labels on the altitude tape
	public void updateAltitudeTape() {

        /* TODO move the targetalttiude indication to the for loop */
		/* Set the location of the target label on the altitude tape and check wether to 
		 * show the target label or not (aka is the aircraft already on target altitude?) */
        if (Math.abs(mAircraft.get(1).getTargetAltitude() - mAircraft.get(1).getAltitude()) > 0.001) {
            altitudeTapeFragment.setTargetLabel(mAircraft.get(1).getTargetAltitude(), mAircraft.get(1).getTargetLabelId());
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
        groupSelectedAircraft.clear();
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
}