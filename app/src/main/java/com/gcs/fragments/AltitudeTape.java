package com.gcs.fragments;

import com.gcs.MainActivity;
import com.gcs.R;
import com.gcs.core.ConflictStatus;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.DragEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AltitudeTape extends Fragment {

    //Declaration of layouts and views
	private FrameLayout framelayout;
	private View rootView, targetLabel;
    private TextView label, groupLabel, groupSelectionLabel;

    //Declaration of lists and arrays
    private SparseArray<Integer>                labelList            = new SparseArray<>();
    private ArrayList<Integer>                  aircraftInGroupList  = new ArrayList<>();
    private ConcurrentHashMap<String, Integer>  stringToLabelIdList  = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer,String>   labelIdToStringList  = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Integer>  groupSelectionIdList = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Integer> groupSelectLabelList = new ConcurrentHashMap<>();

    //Declaration of variables
    private String selectedGroup = null;
    private boolean groupSelected = false, targetCreated = false;
    private double flightCeiling, groundLevel, MSA; //[m]
    private int draggedLabel, textGravity, backgroundImg, yellowLabel, blueLabel, grayLabel, redLabel, LargeBlueLabel, LargeRedLabel
             ,groundLevelTape, flightCeilingTape, relayAltitude, surveillanceAltitude;

//    //Hardcode the altitude tape endpoints
//    private final int groundLevelTape   = 890; //0 meter
//    private final int flightCeilingTape = 0;  //20 m

    //Define the size of the labels and the dragshadow offset
    private static final Point smallLabelDimensions = new Point (80,70);
    private static final int dragShadowVerticalOffset = smallLabelDimensions.y*2;

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {

        //Initial loading of the resources to be used later
        yellowLabel    = R.drawable.altitude_label_small_yellow_flipped;
        blueLabel      = R.drawable.altitude_label_small_blue;
        grayLabel      = R.drawable.altitude_label_small_gray;
        redLabel       = R.drawable.altitude_label_small_red;
        LargeBlueLabel = R.drawable.altitude_label_large_blue;
        LargeRedLabel  = R.drawable.altitude_label_large_red;

        // Inflate the layout for this fragment
		rootView = inflater.inflate(R.layout.altitude_tape, container, false);

        return rootView;    
    }
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //Load airspace data from resources (Settings are done there: numerical.xml)
        flightCeiling = getResources().getInteger(R.integer.flightCeiling);
        groundLevel   = getResources().getInteger(R.integer.groundLevel);
        MSA           = getResources().getInteger(R.integer.MSA);

        relayAltitude        = getResources().getInteger(R.integer.relayAltitude);
        surveillanceAltitude = getResources().getInteger(R.integer.surveillanceAltitude);

        //Handle to the altitude tape view
        ImageView altitudeTape = (ImageView) getView().findViewById(R.id.altitudeTapeView);

        ////////////////Programmatically draw the altitude tape/////////////////
        WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int outerHeight = (int)(size.y*0.6);
        int outerWidth  = (int)(size.x*0.04);
        int vertOffset  = (int)(outerHeight/27.4);
        int horOffset   = (int)(0.2*outerWidth);

        int MSAheight = (int)((1-(float)MSA/(flightCeiling-groundLevel))*outerHeight);

        Bitmap bitmap = Bitmap.createBitmap(100,outerHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();

        //Blue fill
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getResources().getColor(R.color.blueTape));
        canvas.drawRect(horOffset,outerHeight-vertOffset,outerWidth-horOffset,vertOffset,paint);
        //Fill brown
        paint.setColor(getResources().getColor(R.color.brownTape));
        canvas.drawRect(horOffset,outerHeight-vertOffset,outerWidth-horOffset,MSAheight,paint);
        //Frame
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);
        canvas.drawRect(horOffset,outerHeight-vertOffset,outerWidth-horOffset,vertOffset,paint);
        //Text
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(20);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("MSA", outerWidth/2, MSAheight, textPaint);

        BitmapDrawable drawable = new BitmapDrawable(getResources(), bitmap);
        altitudeTape.setBackground(drawable);
        /////////////////////////////////

        //Set the location of the bounds of the tape to be able to determine label locations
        flightCeilingTape = 0;
        groundLevelTape   = outerHeight-(2*vertOffset);

        //OnCLickListener on the altitude tape (used for deselection of all labels)
        altitudeTape.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Set the group selection to null, go back to normal display of the labels
                selectedGroup = null;
                //Remove selected group labels from tape
                removeGroupSelectedAircraft();
                //Clear the list holding the id's of the group selection labels
                groupSelectLabelList.clear();
                //Change the selection status of all aircraft to not selected
                ((MainActivity) getActivity()).deselectAllAircraft();
            }
        });

        //Create a handle to the (frame)layout of the altitude tape to be able to place labels on it
        framelayout = (FrameLayout) rootView.findViewById(R.id.altitudeTapeFragment);
    }
	
	//OnCLickListener for individual labels
	View.OnClickListener onLabelClick(final View tv) {
	    return new View.OnClickListener() {
	        public void onClick(View v) {
                int aircraftNumber;

                //If a group is selected, deselect it
                if(groupSelected) {
                    //Get the id of the selected aircraft and get its selection status from mainactivity
                    aircraftNumber = groupSelectLabelList.get(v.getId());
                    //Deselect all aircraft and reselect the clicked aircraft
                    ((MainActivity) getActivity()).deselectAllAircraft();
                    ((MainActivity) getActivity()).setIsSelected(aircraftNumber,true);

                    selectedGroup = null;
                    groupSelected = false;
                    groupSelectLabelList.clear();
                    removeGroupSelectedAircraft();
                } else {
                    //Get the id of the selected aircraft and get its selection status from mainactivity
                    aircraftNumber = labelList.get(v.getId());
                    boolean isAircraftSelected = ((MainActivity) getActivity()).isAircraftIconSelected(aircraftNumber);
                    //Invert the selection status
                    ((MainActivity) getActivity()).setIsSelected(aircraftNumber, !isAircraftSelected);
                }
        	}
	    };
	}

    //OnCLickListener for the group labels
    View.OnClickListener onGroupLabelClick(final View tv) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                //Remove selected group labels from tape
                removeGroupSelectedAircraft();
                //Clear the list holding the id's of the group selection labels
                groupSelectLabelList.clear();

                //Set (boolean) that a group is selected and store its labelcharacter string to later check if the group label should be drawn again or if it is selected
                groupSelected = true;
                selectedGroup = labelIdToStringList.get(v.getId());

                //Hide group label from view
                getView().findViewById(v.getId()).setVisibility(View.GONE);

                //Set the selection status aircraft icons(get all separate aircraft numbers from the characters and send them to mainactivity to set them selected as group)
                String[] acCharacters = labelIdToStringList.get(v.getId()).split(" ");
                int[] acNumbers = new int[ acCharacters.length ];
                for(int i = 0; i< acCharacters.length; i++) {
                    acNumbers[i] = Character.getNumericValue(acCharacters[i].charAt(0)) - 9;
                }
                ((MainActivity) getActivity()).setGroupSelected(acNumbers);
            }
        };
    }
	
	//OnLongClickListener for the altitude labels (A long click starts the drag feature)
	View.OnLongClickListener onLabelLongClick(final View tv) {
		return new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {

                //Create a DragShadowBuilder
	            ClipData data = ClipData.newPlainText("", "");
                myDragShadowBuilder myShadow = new myDragShadowBuilder(tv);

                //Reference for the ondrag method to know which label is being dragged (used in the onDragListener)
                draggedLabel = v.getId();
	            
	            // Start the drag
	            v.startDrag(data,        // the data to be dragged
                        myShadow,    // the drag shadow builder
                        null,        // no need to use local data
                        0            // flags (not currently used, set to 0)
                );
				return true;
			}
		};
	}

    //Listener to the drag that was started using a long click
    View.OnDragListener labelDragListener(final View tv) {
        return new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        break;
                    case DragEvent.ACTION_DRAG_LOCATION:
                        //Give the user feedback (on the altitude instrument) about the altitude of the label while dragging
                        if (groupSelectLabelList.containsKey(draggedLabel)) {   //Group selection label
                            ((MainActivity) getActivity()).setDragAltitude(groupSelectLabelList.get(draggedLabel), labelLocationToAltitude(event.getY() - dragShadowVerticalOffset), false);
                        } else{                                                 //Normal label
                            ((MainActivity) getActivity()).setDragAltitude(labelList.get(draggedLabel), labelLocationToAltitude(event.getY() - dragShadowVerticalOffset), false);
                        }
                        break;
                    case DragEvent.ACTION_DROP:
//                        //Set the altitude instrument back to normal
//                        ((MainActivity) getActivity()).setDragAltitude(labelList.get(draggedLabel),labelLocationToAltitude(event.getY() - dragShadowVerticalOffset),true);
                        //Send the drop location to the method that implements the command (Note that an offset value was used to be able to see the label while dragging)
                        if (groupSelectLabelList.containsKey(draggedLabel)) {   //Group selection label
                            setTargetAltitude(groupSelectLabelList.get(draggedLabel), event.getY() - dragShadowVerticalOffset);
                            //Set the altitude instrument back to normal
                            /* TODO: Change this to an appropriate number or remove if the instrument is removed (only show it for drag??) */
                            ((MainActivity) getActivity()).setDragAltitude(1, labelLocationToAltitude(event.getY() - dragShadowVerticalOffset), true);
                        } else{                                                 //Normal label
                            setTargetAltitude(labelList.get(draggedLabel), event.getY() - dragShadowVerticalOffset);
                            //Set the altitude instrument back to normal
                            ((MainActivity) getActivity()).setDragAltitude(labelList.get(draggedLabel), labelLocationToAltitude(event.getY() - dragShadowVerticalOffset), true);
                        }
                        break;
                    default:
                        break;
                }
            return true;
            }
        };
    }

    //Custom dragshadow builder, specifically used to offset the dragshadow from touchpoint because it otherwise would be under the user's finger
    public static class myDragShadowBuilder extends View.DragShadowBuilder {
        public myDragShadowBuilder(View view) {
            super(view);
        }

        @Override
        public void onProvideShadowMetrics(Point shadowSize, Point touchPoint) {
            super.onProvideShadowMetrics(shadowSize, touchPoint);
            //Offset the dragshadow for better visibility
            touchPoint.set(smallLabelDimensions.x / 2, dragShadowVerticalOffset);
        }
    }

    ////////////LABEL DRAWING////////// (Mainactivity determines which labels it wants to draw, here colors are determined and it is checked if labels should be drawn again etc.)

    //Method to draw individual labels on the altitude tape
	public void setLabel(double altitude, int labelId, String labelCharacter, boolean isAircraftIconSelected, boolean labelCreated, int acNumber, int visibility){
        //Determine if a certain label(id) is already present in the list  that keeps track of all individual labels
        if (labelList.get(labelId) == null) {
            labelList.put(labelId, acNumber);
        }

        //If the aircraft is selected, use a yellow label, otherwise a label based on the conflict status should be used
        if (isAircraftIconSelected) {
            backgroundImg = yellowLabel;
        } else {
            //Get conflict status
            ConflictStatus conflictStatus = ((MainActivity) getActivity()).getConflictStatus(acNumber);
            switch (conflictStatus) {
                case BLUE:
                    backgroundImg = blueLabel;
                    break;
                case GRAY:
                    backgroundImg = grayLabel;
                    break;
                case RED:
                    backgroundImg = redLabel;
                    break;
                default:
                    backgroundImg = blueLabel;
            }
        }

        //Set the size of the label that will be drawn and add a topmargin to place it vertically on the tape
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(smallLabelDimensions.x,smallLabelDimensions.y);
        params.topMargin = altitudeToLabelLocation(Math.max(0, altitude)); //Value cannot become negative

        //Set alignment of the label based on the selection status (selected ones left, unselected ones right)
        if (!((MainActivity) getActivity()).isAircraftIconSelected(acNumber)) {
            params.gravity = Gravity.RIGHT;
            textGravity = Gravity.CENTER;
        } else {
            params.gravity = Gravity.LEFT;
            textGravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
        }

        //Create a label if it is the first time it will be drawn on the tape, else only update it
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

            //Set the isLabelCreated status of the aircraft to prevent it will be created again in the upcoming loops
            ((MainActivity) getActivity()).setIsLabelCreated(true, acNumber);
        } else {
            label = (TextView) getView().findViewById(labelId);
            label.setBackgroundResource(backgroundImg);
            label.setVisibility(visibility);
            label.setGravity(textGravity);
            framelayout.updateViewLayout(label, params);
        }
	}

    //Method to draw group labels on the altitude tape
    public void drawGroupLabel(boolean inConflict, double altitude, String labelCharacters, List<Integer> ac) {

        //Add the numbers of aircraft that are in a group to the list to avoid that they also get an individual label
        for(int i=0; i<ac.size(); i++) {
            if(!aircraftInGroupList.contains(ac.get(i))) {
                aircraftInGroupList.add(ac.get(i));
            }
        }

        //If no group is selected or it does not involve the a group that was already drawn
        if(selectedGroup == null || !selectedGroup.equals(labelCharacters)) {
            //Use a red label for conflict groups, else a blue one
            if (inConflict) {
                backgroundImg = LargeRedLabel;
            } else {
                backgroundImg = LargeBlueLabel;
            }

            //Set the parameters of the label (size, vertical location on the tape and its horizontal location)
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(smallLabelDimensions.x,smallLabelDimensions.y);
            params.topMargin = altitudeToLabelLocation(altitude);
            params.gravity = Gravity.RIGHT;

            //Create a label if it is the first time it will be drawn on the tape, else only update it
            if (!stringToLabelIdList.containsKey(labelCharacters)) {
                int labelId = TextView.generateViewId();
                groupLabel = new TextView(getActivity());
                groupLabel.setText("   " + labelCharacters);
                groupLabel.setTypeface(null, Typeface.BOLD);
                groupLabel.setGravity(Gravity.CENTER);
                groupLabel.setId(labelId);
                groupLabel.setBackgroundResource(backgroundImg);
                groupLabel.setOnClickListener(onGroupLabelClick(groupLabel));
                framelayout.addView(groupLabel, params);
                stringToLabelIdList.put(labelCharacters, labelId);
                labelIdToStringList.put(labelId, labelCharacters);
            } else {
                groupLabel = (TextView) getView().findViewById(stringToLabelIdList.get(labelCharacters));
                groupLabel.setBackgroundResource(backgroundImg);
                groupLabel.setGravity(Gravity.CENTER);
                groupLabel.setVisibility(View.VISIBLE);
                framelayout.updateViewLayout(groupLabel, params);
            }
        }
    }

    //Method to draw individual labels of aircraft that are in a group selection
    public void drawGroupSelection(double altitude, String labelCharacter, int i, int numberOfLabels, int acNumber) {

        //Set the parameters of the label (size, vertical location on the tape and its horizontal location)
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(smallLabelDimensions.x,smallLabelDimensions.y);
        params.topMargin = altitudeToLabelLocation(altitude);
        params.gravity = Gravity.LEFT;

        //Add a margin to the right of a label if it will overlap with another label
        if(i==0) {
            params.leftMargin = 75 + (numberOfLabels-1)*smallLabelDimensions.x;
        } else {
            params.rightMargin = 75 + (smallLabelDimensions.x*i);
            params.leftMargin = (numberOfLabels-i)*smallLabelDimensions.x;
        }

        //Create a label if it is the first time it will be drawn on the tape, else only update it
        if (!groupSelectionIdList.containsKey(labelCharacter)) {
            int labelId = TextView.generateViewId();
            groupSelectionLabel = new TextView(getActivity());
            groupSelectionLabel.setText("   " + labelCharacter);
            groupSelectionLabel.setTypeface(null, Typeface.BOLD);
            groupSelectionLabel.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            groupSelectionLabel.setId(labelId);
            groupSelectionLabel.setBackgroundResource(yellowLabel);
            groupSelectionLabel.setOnClickListener(onLabelClick(groupSelectionLabel));///////////////
            groupSelectionLabel.setOnLongClickListener(onLabelLongClick(groupSelectionLabel));
            rootView.setOnDragListener(labelDragListener(groupSelectionLabel));
            framelayout.addView(groupSelectionLabel, params);
            groupSelectionIdList.put(labelCharacter,labelId);
            groupSelectLabelList.put(labelId, acNumber);
        } else {
            groupSelectionLabel = (TextView) getView().findViewById(groupSelectionIdList.get(labelCharacter));
            framelayout.updateViewLayout(groupSelectionLabel, params);
        }
    }

    //Method to remove the labels of a group selection
    private void removeGroupSelectedAircraft() {
        for (String key : groupSelectionIdList.keySet()) {
            framelayout.removeView(getView().findViewById(groupSelectionIdList.get(key)));
        }
        groupSelectionIdList.clear();
    }

    //Method called from mainactivity if no group labels are drawn. Then if there are still labelId's in the list, remove them from the view and list.
    public void removeGroupLabels() {
        if(!stringToLabelIdList.isEmpty()) {
            for (String key : stringToLabelIdList.keySet()) {
                framelayout.removeView(getView().findViewById(stringToLabelIdList.get(key)));
            }

            //Clear the lists that contain information about aircraft groups
            stringToLabelIdList.clear();
            labelIdToStringList.clear();
            aircraftInGroupList.clear();
        }
    }

    public void removeSingleLabels() {
//        if(labelList.size()!=0) {
            for(int i=0; i<labelList.size(); i++) {
                framelayout.removeView(getView().findViewById(labelList.get(labelList.keyAt(i))));
            }
            labelList.clear();
//        }
    }

    //Method to remove all labels from the tape
    public void clearTape() {
        //Remove all group-, groupselected- and single labels
        removeSingleLabels();
        removeGroupLabels();
        removeGroupSelectedAircraft();
    }

    //Method to draw the target altitude on the altitude tape
	public void setTargetLabel(double targetAltitude, int targetLabelId) {

		/* TODO make a better indicating icon/bug for the target altitude */
        //Set the parameters of the label (size, vertical location on the tape and its horizontal location)
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(smallLabelDimensions.x,smallLabelDimensions.y);
        params.topMargin = altitudeToLabelLocation(targetAltitude);
        params.gravity = Gravity.RIGHT;

        //Create a label if it is the first time it will be drawn on the tape, else only update it
		if(!targetCreated) {
            targetLabel = new View(getActivity());
            targetLabel.setBackgroundResource(R.drawable.altitude_label_small_red);
            targetLabel.setId(targetLabelId);
            targetLabel.setVisibility(View.VISIBLE);
            framelayout.addView(targetLabel,params);
			targetCreated = true;
		} else {
            targetLabel = getView().findViewById(targetLabelId);
            targetLabel.setVisibility(View.VISIBLE);
            framelayout.updateViewLayout(targetLabel,params);
		}
	}
	
	//Method to remove the target label from the altitude tape
	public void deleteTargetLabel(int targetLabelId) {
		targetLabel = getView().findViewById(targetLabelId);

		if(targetLabel!=null) {
            framelayout.removeView(targetLabel);
		}
	}
	
	//Convert altitude to a label location on the tape
	private int altitudeToLabelLocation(double altitude) {

        //Calculate the label location based on the length of the bar and the vertical flight range airspace
		int lengthBar = groundLevelTape - flightCeilingTape;
		double verticalRange = flightCeiling - groundLevel;

		return (int) (groundLevelTape-((altitude/verticalRange)*lengthBar)); //labelLocation
	}
	
	//Convert label location on the tape to an altitude 
	private double labelLocationToAltitude(float labelLocation) {

        //Calculate the aircraft altitude based on length of the bar, label location and the vertical flight range airspace
		int lengthBar = groundLevelTape - flightCeilingTape;
		double verticalRange = flightCeiling - groundLevel;
		
		return verticalRange*((double) groundLevelTape-labelLocation)/lengthBar; //altitude
	}

	//Set the target altitude to the service
	private void setTargetAltitude(int aircraftNumber,float dropLocation) {

        //The altitude to which the user commands the aircraft to go to
		double dropAltitude = labelLocationToAltitude(dropLocation);

        //Set the new target altitude to the service
        if(dropAltitude > ((MainActivity) getActivity()).getAircraftAltitude(aircraftNumber)) {
            ((MainActivity) getActivity()).changeCurrentWpAltitude(aircraftNumber,relayAltitude);
        } else {
            ((MainActivity) getActivity()).changeCurrentWpAltitude(aircraftNumber,surveillanceAltitude);
        }
	}
}