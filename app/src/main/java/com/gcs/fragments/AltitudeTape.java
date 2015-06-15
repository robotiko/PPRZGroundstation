package com.gcs.fragments;

import com.gcs.MainActivity;
import com.gcs.R;
import com.gcs.core.ConflictStatus;

import android.content.ClipData;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.DragEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class AltitudeTape extends Fragment {
	
	private FrameLayout framelayout;
	private View rootView;

    final SparseArray<Integer> labelList = new SparseArray<>();
    private ArrayList<Integer> aircraftInGroupList = new ArrayList<>();
    private ConcurrentHashMap<String, Integer> stringToLabelIdList = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer,String> labelIdToStringList = new ConcurrentHashMap<>();
    private String selectedGroup = null;

    private int draggedLabel;

    private boolean groupUnselected = false;
    private boolean groupSelected = false;
	private boolean targetCreated = false;
	
	/* TODO Determine altitude label location based on the height of the bar and the the dynamic vertical range of the drones (flight ceiling - ground level) */
	private final int groundLevelTape   = 900; //0 meter
	private final int flightCeilingTape = 35;  //20 m

    private double flightCeiling;              //[m]
	private double groundLevel;                //[m]

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
		
        // Inflate the layout for this fragment
		rootView = inflater.inflate(R.layout.altitude_tape, container, false);
        
        return rootView;    
    }
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //Only used for on-click listener
        ImageView altitudeTape = (ImageView) getView().findViewById(R.id.altitudeTapeView);

        //OnCLickListener on the altitude tape
        altitudeTape.setOnClickListener(new View.OnClickListener() {
        	@Override
            public void onClick(View v) {
                //Set the group selection to null go back to normal display of the labels
                selectedGroup = null;
            }
        });

        framelayout = (FrameLayout) rootView.findViewById(R.id.altitudeTapeFragment);

        flightCeiling  = getResources().getInteger(R.integer.flightCeiling);
        groundLevel    = getResources().getInteger(R.integer.groundLevel);
    }
	
	//OnCLickListener for individual altitude labels
	View.OnClickListener onLabelClick(final View tv) {
	    return new View.OnClickListener() {
	        public void onClick(View v) {
                if(groupSelected) {
                        selectedGroup = null;
                        groupSelected = false;
                        groupUnselected = true;
                }

                int aircraftNumber = labelList.get(v.getId());
                boolean isAircraftSelected = ((MainActivity) getActivity()).isAircraftIconSelected(aircraftNumber);

                if (isAircraftSelected) {    // Deselect
                    ((MainActivity) getActivity()).setIsSelected(aircraftNumber, false);
                } else {                    // Select
                    ((MainActivity) getActivity()).setIsSelected(aircraftNumber, true);
                }
        	}
	    };
	}

    //OnCLickListener for the group labels
    View.OnClickListener onGroupLabelClick(final View tv) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                    groupSelected = true;
                    selectedGroup = labelIdToStringList.get(v.getId());
                    drawGroupSelection(v.getId());
            }
        };
    }
	
	//OnLongClickListener for the altitude labels
	View.OnLongClickListener onLabelLongClick(final View tv) {
		return new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				
	            ClipData data = ClipData.newPlainText("", "");
	            View.DragShadowBuilder myShadow = new DragShadowBuilder(tv);
	            /* TODO Offset the label that is dragged to be able to see it */

                //Reference for the ondrag method to know which label is being dragged
                draggedLabel = v.getId();
	            
	            // Starts the drag
	            v.startDrag(data,  		// the data to be dragged
	                        myShadow,  	// the drag shadow builder
	                        null,      	// no need to use local data
	                        0          	// flags (not currently used, set to 0)
	            );
				return true;
			}
		};
	}

    View.OnDragListener labelDragListener(final View tv) {
        return new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {

                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        break;
                    case DragEvent.ACTION_DRAG_LOCATION :
                        break;
                    case DragEvent.ACTION_DROP:
                        //Send the drop location to the method that implements the command
            	        setTargetAltitude(labelList.get(draggedLabel), event.getY());
                        break;
                    default:
                        break;
                }
            return true;
            }
        };
    }

    //Method to draw single labels on the altitude tape
	public void setLabel(double altitude, int labelId, String labelCharacter, boolean isAircraftIconSelected, boolean labelCreated, int acNumber, int visibility){
        if(!aircraftInGroupList.contains(acNumber)) {
            if (labelList.get(labelId) == null) {
                labelList.put(labelId, acNumber);
            }

            int backgroundImg;

            if (isAircraftIconSelected) {
                backgroundImg = R.drawable.altitude_label_small_yellow_flipped;

            } else {
                ConflictStatus conflictStatus = ((MainActivity) getActivity()).getConflictStatus(acNumber);

                switch (conflictStatus) {
                    case BLUE:
                        backgroundImg = R.drawable.altitude_label_small_blue;
                        break;
                    case GRAY:
                        backgroundImg = R.drawable.altitude_label_small_gray;
                        break;
                    case RED:
                        backgroundImg = R.drawable.altitude_label_small_red;
                        break;
                    default:
                        backgroundImg = R.drawable.altitude_label_small_blue;
                }
            }

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(80, 70);
            params.topMargin = altitudeToLabelLocation(altitude);
            int textGravity;

            //Set alignment of the label based on the selection status
            if (!((MainActivity) getActivity()).isAircraftIconSelected(acNumber)) {
                params.gravity = Gravity.RIGHT;
                textGravity = Gravity.CENTER;
            } else {
                params.gravity = Gravity.LEFT;
                textGravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
            }

            TextView label;
            if (!labelCreated) {
                label = new TextView(getActivity());
                label.setId(labelId);
                label.setBackgroundResource(backgroundImg);
                label.setVisibility(visibility);
                label.setMinWidth(20);
                label.setText("   " + labelCharacter);
                label.setTypeface(null, Typeface.BOLD);
                label.setGravity(textGravity);
                label.setOnClickListener(onLabelClick(label));
                label.setOnLongClickListener(onLabelLongClick(label));
                rootView.setOnDragListener(labelDragListener(label));
                framelayout.addView(label, params);

                ((MainActivity) getActivity()).setIsLabelCreated(true, acNumber);
            } else {
                label = (TextView) getView().findViewById(labelId);
                label.setBackgroundResource(backgroundImg);
                label.setVisibility(visibility);
                label.setGravity(textGravity);
                framelayout.updateViewLayout(label, params);
            }
        }
	}

    //Method to draw group labels on the altitude tape
    public void drawGroupLabel(boolean inConflict, double altitude, String labelCharacters, int ac1, int ac2) {
        /* TODO change to allow groups larger than 2 */
//        String group = String.valueOf(ac1)+String.valueOf(ac2);
        //Add the numbers of aircraft that are in a group to the list to avoid that they also get an individual label
        aircraftInGroupList.add(ac1);
        aircraftInGroupList.add(ac2);

        groupUnselected = false;

        if(selectedGroup == null || !selectedGroup.equals(labelCharacters)) {
            int backgroundImg;
            if (inConflict) {
                backgroundImg = R.drawable.altitude_label_large_red;
            } else {
                backgroundImg = R.drawable.altitude_label_large_blue;
            }

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(80, 70);
            params.topMargin = altitudeToLabelLocation(altitude);
            params.gravity = Gravity.RIGHT;
//        textGravity = Gravity.CENTER;

            TextView groupLabel;
            if (!stringToLabelIdList.containsKey(labelCharacters)) {
                int labelId = TextView.generateViewId();
                groupLabel = new TextView(getActivity());
                groupLabel.setText("   " + labelCharacters);
                groupLabel.setTypeface(null, Typeface.BOLD);
                groupLabel.setGravity(Gravity.CENTER);
                groupLabel.setId(labelId);
                groupLabel.setBackgroundResource(backgroundImg);
                groupLabel.setOnClickListener(onGroupLabelClick(groupLabel));
                framelayout.addView(groupLabel,params);
                stringToLabelIdList.put(labelCharacters,labelId);
                labelIdToStringList.put(labelId,labelCharacters);
            } else {
                groupLabel = (TextView) getView().findViewById(stringToLabelIdList.get(labelCharacters));
                groupLabel.setBackgroundResource(backgroundImg);
                groupLabel.setGravity(Gravity.CENTER);
                groupLabel.setVisibility(View.VISIBLE);
                framelayout.updateViewLayout(groupLabel, params);
            }
        }
    }

    private void drawGroupSelection(int groupViewId) {
        //Hide group label from view
        TextView groupLabel = (TextView) getView().findViewById(groupViewId);
        groupLabel.setVisibility(View.GONE);

        //Obtain references to involved aircraft
        String[] acCharacters = labelIdToStringList.get(groupViewId).split(" ");
        for(int i = 0; i< acCharacters.length; i++) {
//            Log.d("test",String.valueOf(Character.getNumericValue(acCharacters[i].charAt(0))-9));
        }

        ///////////////////////////////////////
        int backgroundImg = R.drawable.altitude_label_small_yellow_flipped;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(80, 70);
        params.gravity = Gravity.LEFT;
        int textGravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
    }

    //Method called from mainactivity if no group labels are drawn. Then if there are still labelId's in the list, remove them from the view and list.
    public void removeGroupLabels() {
        if(!stringToLabelIdList.isEmpty() && groupUnselected) {
            for (String key : stringToLabelIdList.keySet()) {
                framelayout.removeView(getView().findViewById(stringToLabelIdList.get(key)));
            }
            stringToLabelIdList.clear();
            labelIdToStringList.clear();
        }
    }

    //Method to draw the target altitude on the altitude tape
	public void setTargetLabel(double targetAltitude, int targetLabelId) {
		
		/* TODO make a better indicating icon/bug for the target altitude */
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(80, 70);
        params.topMargin = altitudeToLabelLocation(targetAltitude);
        params.gravity = Gravity.RIGHT;

		View target;
		if(!targetCreated) {
			target = new View(getActivity());
			target.setBackgroundResource(R.drawable.altitude_label_small_red);
            target.setId(targetLabelId);
            target.setVisibility(View.VISIBLE);
            framelayout.addView(target,params);
			targetCreated = true;
		} else {
			target = getView().findViewById(targetLabelId);
			target.setVisibility(View.VISIBLE);
            framelayout.updateViewLayout(target,params);
		}
	}
	
	//Method to remove the target label from the altitude tape
	public void deleteTargetLabel(int targetLabelId) {
		View targetLabel = getView().findViewById(targetLabelId);

		if(targetLabel!=null) {
			targetLabel.setVisibility(View.GONE);
		}
	}
	
	//Convert altitude to a label location on the tape
	private int altitudeToLabelLocation(double altitude) {
		
		int lengthBar = groundLevelTape - flightCeilingTape;
		double verticalRange = flightCeiling - groundLevel;
		int labelLocation = (int) (groundLevelTape-((altitude/verticalRange)*lengthBar));
		
		return labelLocation;
	}
	
	//Convert label location on the tape to an altitude 
	private double labelLocationToAltitude(float labelLocation) {
		
		int lengthBar = groundLevelTape - flightCeilingTape;
		double verticalRange = flightCeiling - groundLevel;
		double altitude = verticalRange*((double) groundLevelTape-labelLocation)/lengthBar;
		
		return altitude;
	}

	//Set the target altitude to the service
	private void setTargetAltitude(int aircraftNumber,float dropLocation) {
		
		double dropAltitude = labelLocationToAltitude(dropLocation);

		//If the label is dropped outside the altitude tape, set the target altitude at the bounds.
		if (dropAltitude < groundLevel) {
			dropAltitude = groundLevel;
		} else if (dropAltitude > flightCeiling) {
			dropAltitude = flightCeiling;
		}

        Log.d("DROP",String.valueOf(aircraftNumber));
		/* TODO Set the target altitude to the service once this function is available */
//		setTargetLabel(dropAltitude, 10); //Temporary setfunction to show a label
	}
}