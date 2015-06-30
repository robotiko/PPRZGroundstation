package com.gcs.core;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.util.Log;
import android.widget.TextView;

import com.gcs.R;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.sharedlib.model.Altitude;
import com.sharedlib.model.Attitude;
import com.sharedlib.model.Battery;
import com.sharedlib.model.Heartbeat;
import com.sharedlib.model.Speed;
import com.sharedlib.model.Position;

public class Aircraft {
	
	private Context context;
    private final String labelCharacter;
    private static int aircraftCount = 0;

    //Constructor in which the total aircraft count is updated and the label character is determined
	public Aircraft(Context context){
	    this.context = context;
        aircraftCount++;
        labelCharacter = String.valueOf((char) (64 + aircraftCount));
	}

    //Instatntiation of class attributes
	private Heartbeat	   mHeartbeat = new Heartbeat();
	private Attitude       mAttitude  = new Attitude();
	private Altitude       mAltitude  = new Altitude();
	private Speed          mSpeed     = new Speed();
	private Battery        mBattery   = new Battery();
	private CustomState    mState     = new CustomState();
	private Position       mPosition  = new Position();
	private List<Waypoint> waypoints  = new ArrayList<>();

    public Marker acMarker;
    public List<Marker> wpMarkers  = new ArrayList<>();
    public List<String> missionBlocks;
    public Circle CoverageCircle;
	public Polyline flightPath;
	
	private final int AltitudeLabelId   = TextView.generateViewId();
	private final int targetLabelId     = TextView.generateViewId();
	private boolean isSelected          = false;
    private boolean isLabelCreated      = false;
    private boolean showInfoWindow      = true;
    private float distanceHome          = 0f;
    private String currentBlock;
    private LatLng currentSurveyLoc;

    //////////// HEARTBEAT ////////////
    public byte getSysid() {
        return mHeartbeat.getSysid();
    }

    public byte getCompid() {
        return mHeartbeat.getCompid();
    }

    public byte getMavlinkVersion() {
        return mHeartbeat.getMavlinkVersion();
    }

    public void setHeartbeat(byte sysid, byte compid) {
        mHeartbeat.setSysid(sysid);
        mHeartbeat.setCompid(compid);
    }

    public void setSysid(byte sysid) {
        mHeartbeat.setSysid(sysid);
    }

    public void setCompid(byte compid) {
        mHeartbeat.setCompid(compid);
    }

    public void setMavlinkVersion(byte mavlinkVersion) {
        mHeartbeat.setMavlinkVersion(mavlinkVersion);
    }

    public boolean hasHeartbeat() {
        return mHeartbeat.heartbeatState != Heartbeat.HeartbeatState.FIRST_HEARTBEAT;
    }

    public boolean isConnectionAlive() {
        return mHeartbeat.heartbeatState != Heartbeat.HeartbeatState.LOST_HEARTBEAT;
    }

    //////////// ATTITUDE ////////////
	public void setRollPitchYaw(double roll, double pitch, double yaw) {
    	mAttitude.setRollPitchYaw(roll, pitch, yaw);
    }
	
	public double getRoll() {
    	return mAttitude.getRoll();
    }
    
    public double getPitch() {
    	return mAttitude.getPitch();
    }
    
    public double getYaw() {
    	return mAttitude.getYaw();
    }

    //////////// ALTITUDE ////////////
    public void setAltitude(double altitude) {
    	mAltitude.setAltitude(altitude);
    }

    public void setTargetAltitude(double targetAltitude) {
    	mAltitude.setTargetAltitude(targetAltitude);
    }
    
    public void setAGL(double AGL) {
    	mAltitude.setTargetAltitude(AGL);
    }
    
    public double getAltitude() {
    	return mAltitude.getAltitude();
    }
    
    public double getTargetAltitude() {
    	return mAltitude.getTargetAltitude();
    }
    
    public double getAGL() {
    	return mAltitude.getAGL();
    }

    //////////// SPEED ////////////
    public void setGroundAndAirSpeeds(double groundSpeed, double airSpeed, double climbSpeed) {
    	mSpeed.setGroundAndAirSpeeds(groundSpeed, airSpeed, climbSpeed);
    }
    
    public void setTargetSpeed(double targetSpeed) {
    	mSpeed.setTargetSpeed(targetSpeed);
    }
    
    public double getGroundSpeed() {
    	return mSpeed.getGroundSpeed();
    }
    
    public double getAirSpeed() {
    	return mSpeed.getAirSpeed();
    }
    
    public double getClimbSpeed() {
    	return mSpeed.getClimbSpeed();
    }
    
    public double getTargetSpeed() {
    	return mSpeed.getTargetSpeed();
    }

    //////////// BATTERY ////////////
    public int getBattVolt() {
        return mBattery.getBattVolt();
    }

    public int getBattLevel() {
        return mBattery.getBattLevel();
    }

    public int getBattCurrent() {
        return mBattery.getBattCurrent();
    }

    public double getBattDischarge() {
        return mBattery.getBattDischarge();
    }

    public void setBatteryState(int battVolt, int battLevel, int battCurrent) {
    	mBattery.setBatteryState(battVolt,battLevel,battCurrent);
    }

    //////////// STATE ////////////
    public boolean isArmed() {
        return mState.isArmed();
    }

    public boolean isFlying() {
        return mState.isFlying();
    }

    public void setIsFlying(boolean newState) {
    	mState.setIsFlying(newState);
    }

    public void setArmed(boolean newState) {
    	mState.setArmed(newState);
    }

    public void updateConflictStatus() { mState.updateConflictStatus(); }

    public void setConflictStatusNew(ConflictStatus NewStatus) {
        mState.setConflictStatusNew(NewStatus);
    }

    public ConflictStatus getConflictStatus() {
        return mState.getConflictStatus();
    }

    //////////// POSITION ////////////
    public byte getSatVisible() {
    	return mPosition.getSatVisible();
	}
	
	public int getTimeStamp() {
		return mPosition.getTimeStamp();
	}

    /* TODO remove *1e-7 terms once the service has been corrected for this */
	public double getLat() {
		return mPosition.getLat()*1e-7;
	}
	
	public double getLon() {
		return mPosition.getLon()*1e-7;
	}
	
	public LatLng getLatLng() {
		LatLng latLng = new LatLng(mPosition.getLat()*1e-7, mPosition.getLon()*1e-7);
		return latLng;
	}
	
	/* TODO Remove either this function of getAltitude() */
	public int getAlt() {
		return mPosition.getAlt();
	}
	
	public int getHdg() {
		return mPosition.getHdg();
	}
	
	public void setSatVisible(byte satVisible) {
		mPosition.setSatVisible(satVisible);
	}
	
	public void setLlaHdg(int lat, int lon, int alt, short hdg) {
		mPosition.setLlaHdg(lat, lon, alt, hdg);
	}

    //////////// WAYPOINTS ////////////
    public void addWaypoint(double lat, double lon, float alt, short seq, byte targetSys, byte targetComp) {
    	Waypoint wp = new Waypoint(lat, lon, alt, seq, targetSys, targetComp);
        waypoints.add(wp);
    }
    
    public void setWpLat(double lat, int wpNumber) {
    	Waypoint wp = waypoints.get(wpNumber);
    	wp.setLat(lat);
    	waypoints.set(wpNumber, wp);
	}
    
    public void setWpLon(double lon, int wpNumber) {
    	Waypoint wp = waypoints.get(wpNumber);
        wp.setLon(lon);
    	waypoints.set(wpNumber, wp);
	}

    public void setWpLatLon(double lat, double lon, int wpNumber) {
        setWpLat(lat, wpNumber);
        setWpLon(lon, wpNumber);
    }
    
    public void setWpAlt(float alt, int wpNumber) {
    	Waypoint wp = waypoints.get(wpNumber);
        wp.setAlt(alt);
    	waypoints.set(wpNumber, wp);
    }
    
	public void setWpSeq(short seq, int wpNumber) {
		Waypoint wp = waypoints.get(wpNumber);
        wp.setSeq(seq);
    	waypoints.set(wpNumber, wp);
	}
	
	public void setWpTargetSys(byte targetSys, int wpNumber) {
		Waypoint wp = waypoints.get(wpNumber);
    	wp.setTargetSys(targetSys);
    	waypoints.set(wpNumber,wp);
	}
	
	public void setWpTargetComp(byte targetComp, int wpNumber) {
		Waypoint wp = waypoints.get(wpNumber);
    	wp.setTargetComp(targetComp);
    	waypoints.set(wpNumber,wp);
	}
	
	public double getWpLat(int wpNumber) {
		return waypoints.get(wpNumber).getLat();
	}
	
	public double getWpLon(int wpNumber) {
		return waypoints.get(wpNumber).getLon();
	}
	
	public LatLng getWpLatLng(int wpNumber) {
		LatLng latLng = new LatLng(waypoints.get(wpNumber).getLat(),waypoints.get(wpNumber).getLon());
		return latLng;
	}

    public List<LatLng> getWpLatLngList() {
        List<LatLng> points  = new ArrayList<LatLng>();

        for (int i = 0; i < getNumberOfWaypoints(); i++) {
            points.add(getWpLatLng(i));
        }
        return points;
    }
    
    public float getWpAlt(int wpNumber) {
		return waypoints.get(wpNumber).getAlt();
	}
    
    public short getWpSeq(int wpNumber) {
    	return waypoints.get(wpNumber).getSeq();
	}

	public byte getWpTargetSys(int wpNumber) {
		return waypoints.get(wpNumber).getTargetSys();
	}
	
	public byte getWpTargetComp(int wpNumber) {
		return waypoints.get(wpNumber).getTargetComp();
	}

	public int getNumberOfWaypoints() { return waypoints.size(); }

	public void clearWpList() { waypoints.clear();}

    //////////// CLASS ATTRIBUTES ////////////
    public int getCommunicationSignal(){

        double boundaryLevel = 0.01;
        int maxRange         = context.getResources().getInteger(R.integer.commMaxRange);

        double scalingFactor = (1f/maxRange)*(Math.pow((1/boundaryLevel),(1f/4))-1);
        int signalStrength = (int) (1/Math.pow(scalingFactor*distanceHome+1,4)*100);

        return signalStrength;
    }
    
    public int getAltLabelId(){
    	return AltitudeLabelId;
    }
    
    public int getTargetLabelId(){
    	return targetLabelId;
    }

	public String getLabelCharacter() { return labelCharacter;}

	public boolean isSelected() { return isSelected; }

	public void setIsSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

    public boolean isLabelCreated() {
        return isLabelCreated;
    }

    public void setIsLabelCreated(boolean isLabelCreated) {
        this.isLabelCreated = isLabelCreated;
    }

    public void setDistanceHome(LatLng homeLocation) {
        float[] distance = new float[1];
        /* TODO change getLat() and getLon() to mPosition.getLat() and mPosition.getLon() once the service has been corrected*/
        Location.distanceBetween(getLat(),getLon(),homeLocation.latitude,homeLocation.longitude,distance);
        distanceHome = distance[0];
    }

    public float getDistanceHome() {
        return distanceHome;
    }

    public int getAircraftCount() {
        return aircraftCount;
    }

    public void setCurrentBlock(int currentSelection) {
        if(!missionBlocks.isEmpty()) {
            currentBlock = missionBlocks.get(currentSelection);

//            //If the block is a survey block
//            String surveyBlockName = context.getResources().getString(R.string.survey_block);
//            Log.d("CLICK1",surveyBlockName);
//            Log.d("CLICK2",currentBlock);
//            if(currentBlock.contains(surveyBlockName)) {
//                currentSurveyLoc = getWpLatLng(missionBlocks.indexOf(currentBlock));
//            } else {
//                currentSurveyLoc = null;
//            }
//            Log.d("CLICK3",String.valueOf(currentSurveyLoc));
        }
    }

    public String getCurrentBlock() {return currentBlock; }

    public LatLng getCurrentSurveyLoc() { return currentSurveyLoc; }

    public void setShowInfoWindow(boolean showInfoWindow) {this.showInfoWindow = showInfoWindow;}

    public boolean getShowInfoWindow() {return showInfoWindow;}

    //////////////////////////////////////////////////////
    ////////////////////// ICON //////////////////////////
    //////////////////////////////////////////////////////
    private Bitmap AC_Icon, baseIcon;
    private static BitmapDrawable  arrowRed, arrowBlue, arrowGray, batteryGreen, batteryYellow, batteryRed, commFull, commMid, commLow, commEmpty;
    private static int resolution, protectedZoneAlpha, sideOffsetArrow, batteryVertLocation, batteryHorLocation, batteryScaling, commVertLocation,
                       commHorLocation, commScaling, halfBat, lowBat, halfComm, lowComm, NoComm, colorRED, colorBLUE, colorGRAY, colorYELLOW;
    private static boolean firstTimeDrawing = true;
    private BitmapDrawable iconArrow, batteryIcon, communicationIcon;

    private int circleColor = Color.WHITE;

    Paint circlePaint     = new Paint();
    Paint labelFillPaint  = new Paint();
    Paint labelFramePaint = new Paint();
    Paint labelTextPaint  = new Paint();

    // Matrix for rotation of an aircraft icon
    Matrix rotationMatrix = new Matrix();

    // Method that is called the first time the icon generation method is called to obtain valuies and drawables from resources
    private void setIconDrawingSettings() {
        resolution          = context.getResources().getInteger(R.integer.IconResolution);
        protectedZoneAlpha  = context.getResources().getInteger(R.integer.IconAlpha);
        sideOffsetArrow     = (int) (0.25*resolution);

        batteryVertLocation = context.getResources().getInteger(R.integer.BatteryVertLocation);
        batteryHorLocation  = context.getResources().getInteger(R.integer.BatteryHorLocation);
        batteryScaling      = context.getResources().getInteger(R.integer.BatteryScaling);
        commVertLocation    = context.getResources().getInteger(R.integer.CommVertLocation);
        commHorLocation     = context.getResources().getInteger(R.integer.CommHorLocation);
        commScaling			= context.getResources().getInteger(R.integer.CommScaling);

        //Get the battery icon levels
        halfBat = context.getResources().getInteger(R.integer.HalfBatteryVoltage);
        lowBat  = context.getResources().getInteger(R.integer.LowBatteryVoltage);

        //Get the communication icon levels
        halfComm = context.getResources().getInteger(R.integer.HalfCommunicationSignal);
        lowComm  = context.getResources().getInteger(R.integer.LowBatteryLevel);
        NoComm   = context.getResources().getInteger(R.integer.NoCommunicationSignal);

        //Arrow icons
        arrowRed  = new BitmapDrawable(context.getResources(), BitmapFactory.decodeResource(context.getResources(),R.drawable.aircraft_icon_red));
        arrowBlue = new BitmapDrawable(context.getResources(), BitmapFactory.decodeResource(context.getResources(),R.drawable.aircraft_icon_blue));
        arrowGray = new BitmapDrawable(context.getResources(), BitmapFactory.decodeResource(context.getResources(),R.drawable.aircraft_icon_gray));

        //Battery icons
        batteryGreen  = new BitmapDrawable(context.getResources(), BitmapFactory.decodeResource(context.getResources(), R.drawable.battery_icon_green));
        batteryYellow = new BitmapDrawable(context.getResources(), BitmapFactory.decodeResource(context.getResources(), R.drawable.battery_icon_yellow));
        batteryRed    = new BitmapDrawable(context.getResources(), BitmapFactory.decodeResource(context.getResources(), R.drawable.battery_icon_red));

        //Communication icons
        commFull  = new BitmapDrawable(context.getResources(), BitmapFactory.decodeResource(context.getResources(),R.drawable.communication_icon_full));
        commMid   = new BitmapDrawable(context.getResources(), BitmapFactory.decodeResource(context.getResources(),R.drawable.communication_icon_mid));
        commLow   = new BitmapDrawable(context.getResources(), BitmapFactory.decodeResource(context.getResources(),R.drawable.communication_icon_low));
        commEmpty = new BitmapDrawable(context.getResources(), BitmapFactory.decodeResource(context.getResources(),R.drawable.communication_icon_empty));

        //Colors for labels
        colorRED    = context.getResources().getColor(R.color.red);
        colorBLUE   = context.getResources().getColor(R.color.blue);
        colorGRAY   = context.getResources().getColor(R.color.gray);
        colorYELLOW = context.getResources().getColor(R.color.yellow);
    }

    // Method to generate an aircraft icon
    public void generateIcon(){

        //If it is the first time this method is called, set all resources
        if(firstTimeDrawing) {
            setIconDrawingSettings();
            firstTimeDrawing = false;
        }

        //Determine which color heading indicator (arrow) to draw
        switch (mState.getConflictStatus()) {
            case BLUE:
                iconArrow = arrowBlue;
                labelFillPaint.setColor(colorBLUE);
                break;
            case GRAY:
                iconArrow = arrowGray;
                labelFillPaint.setColor(colorGRAY);
                break;
            case RED:
                iconArrow = arrowRed;
                labelFillPaint.setColor(colorRED);
                break;
            default:
                iconArrow = arrowBlue;
                labelFillPaint.setColor(colorBLUE);
        }

////////////////////////////////////////////////
////Place the icon(arrow) on a white circle/////

        //Recycle the previous base icon in order to safe memory and avoid garbage collection
        if (baseIcon != null) {
            baseIcon.recycle();
            baseIcon = null;
        }

        //Bitmap and canvas to draw a circle on
        baseIcon = Bitmap.createBitmap(resolution, resolution, Bitmap.Config.ARGB_8888);
        Canvas circleCanvas = new Canvas(baseIcon);

        //Paint settings
        circlePaint.setColor(circleColor);
        circlePaint.setAlpha(protectedZoneAlpha);
        circlePaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        //Draw the circle on the canvas
        circleCanvas.drawCircle(resolution/2, resolution/2, resolution/2, circlePaint);

        //Place the heading indicating arrow on the circle
        iconArrow.setBounds(sideOffsetArrow, sideOffsetArrow, resolution - sideOffsetArrow, resolution - sideOffsetArrow);
        iconArrow.draw(circleCanvas);

////////////////////////////////////////////////
//////////Rotate the base icon//////////////////

        //Set the rotation of the icon
        rotationMatrix.postRotate((float) mAttitude.getYaw());

        //Apply the rotation matrix to the base icon (set boolean on true for smoother edges)
        baseIcon =  Bitmap.createBitmap(baseIcon, 0, 0, baseIcon.getWidth(), baseIcon.getHeight(), rotationMatrix, true);

        //Set the rotation matrix back to identity for next iteration
        rotationMatrix.reset();

////////////////////////////////////////////////

        //Determine which battery icon to draw
        if(mBattery.getBattVolt()  > halfBat) { //high battery level
            batteryIcon = batteryGreen;
        }
        else if (halfBat >= mBattery.getBattVolt() && mBattery.getBattVolt()  > lowBat) { //middle battery level
            batteryIcon = batteryYellow;
        }
        else { //low battery level
            batteryIcon = batteryRed;
        }

        //Determine which communication icon to draw
        int communicationSignal = getCommunicationSignal();
        if (communicationSignal > halfComm) { //High signal strength
            communicationIcon = commFull;
        }
        else if (halfComm >= communicationSignal && communicationSignal > lowComm) { //Middle signal strength
            communicationIcon = commMid;
        }
        else if (lowComm >= communicationSignal && communicationSignal > NoComm) { //Low signal strength
            communicationIcon = commLow;
        }
        else { //No signal
            communicationIcon = commEmpty;
        }

////////////////////////////////////////////////
        //Place battery- and communication icons

        //Canvas to work with for placing the battery and communication icons
        Canvas iconCanvas = new Canvas(baseIcon);
        int center = baseIcon.getWidth()/2;

        //Add battery icon to the base icon
        int batVert = (resolution/2)*batteryVertLocation/100;
        int batHor  = (resolution/2)*batteryHorLocation/100;
        int batHalfWidth = (batteryScaling/2)*batteryIcon.getIntrinsicWidth()/100;
        int batHalfHeight = (batteryScaling/2)*batteryIcon.getIntrinsicHeight()/100;
        //(int left, int top, int right, int bottom)
        batteryIcon.setBounds(center+batHor-batHalfWidth, center-batVert-batHalfHeight, center+batHor+batHalfWidth, center-batVert+batHalfHeight);

        //Add communication icon to the base icon
        int commVert = (resolution/2)*commVertLocation/100;
        int commHor  = (resolution/2)*commHorLocation/100;
        int commHalfWidth = (commScaling/2)*communicationIcon.getIntrinsicWidth()/100;
        int commHalfHeight = (commScaling/2)*communicationIcon.getIntrinsicHeight()/100;
        //(int left, int top, int right, int bottom)
        communicationIcon.setBounds(center - commHor - commHalfWidth, center - commVert - commHalfHeight, center - commHor + commHalfWidth, center - commVert + commHalfHeight);

        //Icon background
        circlePaint.setColor(Color.WHITE);
        iconCanvas.drawRect(center-commHor-commHalfWidth, center-batVert-batHalfHeight, center+batHor+batHalfWidth, center-batVert+batHalfHeight, circlePaint);
        iconCanvas.drawCircle(center+batHor+batHalfWidth, center-batVert, batHalfHeight, circlePaint);
        iconCanvas.drawCircle(center-commHor-commHalfWidth, center-batVert, batHalfHeight, circlePaint);

        //Draw the icons to the canvas
        batteryIcon.draw(iconCanvas);
        communicationIcon.draw(iconCanvas);

////////////////////////////////////////////////
        //Add label (left, top, right, bottom, Paint paint)

        //Fill
        labelFillPaint.setStyle(Paint.Style.FILL);
        if(isSelected) { labelFillPaint.setColor(colorYELLOW);}
        iconCanvas.drawRect(center + 47, center - 100, center + 97, center - 50, labelFillPaint);
        //Frame
        labelFramePaint.setColor(Color.BLACK);
        labelFramePaint.setStyle(Paint.Style.STROKE);
        labelFramePaint.setStrokeWidth(3);
        iconCanvas.drawRect(center+47, center-100, center+97, center-50, labelFramePaint);
        //Text
        labelTextPaint.setColor(Color.BLACK);
        labelTextPaint.setTextSize(30);
        labelTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        labelTextPaint.setTextAlign(Paint.Align.CENTER);
        iconCanvas.drawText(labelCharacter, center+72, center-65, labelTextPaint);

////////////////////////////////////////////////

        //TODO add speedvector to icon

        //Recycle the previous aircraft icon in order to safe memory and avoid garbage collection
        if (AC_Icon != null) {
            AC_Icon.recycle();
            AC_Icon = null;
        }

        //Update with the newly generated icon
        AC_Icon = baseIcon;
    }

    //Get and set methods for the icon
    public Bitmap getIcon(){
        return AC_Icon;
    }

    //Method that provides information about the bound offset of the icon (used for information window positioning because the marker is a square bitmap that is rotated)
    public float getIconBoundOffset() { return ((AC_Icon.getHeight()-resolution)/2.0f)/AC_Icon.getHeight(); }

    public void setCircleColor(int color) {
        circleColor = color;
    }
}