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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.widget.TextView;

import com.gcs.R;
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
    private static int aircraftCount;
    private int aircraftNumber;

	public Aircraft(Context context){
	    this.context = context;
        aircraftCount++;
        aircraftNumber = aircraftCount;
	}

	private Heartbeat	   mHeartbeat = new Heartbeat();
	private Attitude       mAttitude  = new Attitude();
	private Altitude       mAltitude  = new Altitude();
	private Speed          mSpeed     = new Speed();
	private Battery        mBattery   = new Battery();
	private CustomState    mState     = new CustomState();
	private Position       mPosition  = new Position();
	private List<Waypoint> waypoints  = new ArrayList<Waypoint>();

    public Marker acMarker, infoWindow;
    public List<Marker> wpMarkers  = new ArrayList<Marker>();
	public Polyline flightPath;
	
	private final int AltitudeLabelId   = TextView.generateViewId();
	private final int targetLabelId     = TextView.generateViewId();
	private final String labelCharacter = String.valueOf((char) (64 + AltitudeLabelId));
	private boolean isSelected          = false;
    private boolean isLabelCreated      = false;
    private float distanceHome          = 0f;

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
        return mBattery.getBattVolt();
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
    
    public boolean isInConflict() {
        return mState.isInConflict();
    }

    public void setIsInConflict(boolean newState) {
    	mState.setIsInConflict(newState);
    }
    
    public boolean isOnUniqueAltitude() {
        return mState.isOnUniqueAltitude();
    }
	
    public void setIsOnUniqueAltitude(boolean newState) {
    	mState.setIsOnUniqueAltitude(newState);
    }

    public void updateConflictStatus() { mState.updateConflictStatus(); }

    public void setConflictStatus(boolean newIsInConflict, boolean newIsOnUniqueAltitude) {
        mState.setConflictStatus(newIsInConflict, newIsOnUniqueAltitude);
    }

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
    public void addWaypoint(float lat, float lon, float alt, short seq, byte targetSys, byte targetComp) {
    	Waypoint wp = new Waypoint(lat, lon, alt, seq, targetSys, targetComp);
        waypoints.add(wp);
    }
    
    public void setWpLat(float lat, int wpNumber) {
    	Waypoint wp = waypoints.get(wpNumber);
    	wp.setLat(lat);
    	waypoints.set(wpNumber, wp);
	}
    
    public void setWpLon(float lon, int wpNumber) {
    	Waypoint wp = waypoints.get(wpNumber);
        wp.setLon(lon);
    	waypoints.set(wpNumber, wp);
	}

    public void setWpLatLon(float lat, float lon, int wpNumber) {
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
	
	public float getWpLat(int wpNumber) {
		return waypoints.get(wpNumber).getLat();
	}
	
	public float getWpLon(int wpNumber) {
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
        int maxRange         = context.getResources().getInteger(R.integer.maxRange);

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

    public int getAircraftNumber() {
        return aircraftNumber;
    }


    //////////// ICON ////////////
    //////////////////////////////////////////////////////
    private Bitmap AC_Icon, batteryIcon, communicationIcon;
    private Drawable iconArrow;
    private static boolean firstTimeDrawing = true;

    //Set color of circle
    private int circleColor = Color.WHITE;
    Paint paint = new Paint();

    private static int resolution, protectedZoneAlpha, sideOffsetArrow, batteryVertLocation, batteryHorLocation,
            batteryScaling, commVertLocation, commHorLocation, commScaling, halfBat, lowBat, halfComm, lowComm, NoComm;
    private int shownBaseIcon = 0;

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

        //Get the battery icon (full,half,low)
        halfBat = context.getResources().getInteger(R.integer.HalfBatteryLevel);
        lowBat  = context.getResources().getInteger(R.integer.LowBatteryLevel);

        //Get the communication icon (full,mid,low,empty)
        halfComm = context.getResources().getInteger(R.integer.HalfCommunicationSignal);
        lowComm  = context.getResources().getInteger(R.integer.LowBatteryLevel);
        NoComm   = context.getResources().getInteger(R.integer.NoCommunicationSignal);
    }

    public void generateIcon(){

        //If it is the first time this method is called, set all resources
        if(firstTimeDrawing) {
            setIconDrawingSettings();
            firstTimeDrawing = false;
        }

        //Determine which color heading indicator (arrow) to draw
        int baseIconRef;
        switch (mState.getConflictStatus()) {
            case BLUE:
                baseIconRef = R.drawable.aircraft_icon_blue;
                break;
            case GRAY:
                baseIconRef = R.drawable.aircraft_icon_gray;
                break;
            case RED:
                baseIconRef = R.drawable.aircraft_icon_red;
                break;
            default:
                baseIconRef = R.drawable.aircraft_icon_blue;
        }

////////////////////////////////////////////////
        //Place the icon(arrow) on a white circle

        //Bitmap and canvas to draw a circle on
        Bitmap baseIcon = Bitmap.createBitmap(resolution, resolution, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(baseIcon);

        //Paint settings
        paint.setColor(circleColor);
        paint.setAlpha(protectedZoneAlpha);
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);

        //Draw the circle on the canvas
        canvas.drawCircle(resolution/2, resolution/2, resolution/2, paint);

        //Check if the arrow icon should be changed, else keep the one that was already loaded
        if(shownBaseIcon == 0 || shownBaseIcon != baseIconRef) {
            iconArrow = new BitmapDrawable(context.getResources(), BitmapFactory.decodeResource(context.getResources(),baseIconRef));
            shownBaseIcon = baseIconRef;
        }

        //Place the heading indicating arrow on the circle
        iconArrow.setBounds(sideOffsetArrow, sideOffsetArrow, resolution - sideOffsetArrow, resolution - sideOffsetArrow);
        iconArrow.draw(canvas);

////////////////////////////////////////////////
////////////////////////////////////////////////
        //Rotate the base icon

        //Rotation matrix
        Matrix matrix = new Matrix();
        matrix.postRotate((float) mAttitude.getYaw());

        //Apply the rotation matrix to the base icon
        baseIcon =  Bitmap.createBitmap(baseIcon, 0, 0, baseIcon.getWidth(), baseIcon.getHeight(), matrix, true);

////////////////////////////////////////////////

        /* TODO set the integer values to the correct orders to comply with the provided battery values */
        //Determine which battery icon to draw
        int batteryIconRef;
        if(mBattery.getBattLevel() > halfBat) { //high battery level
            batteryIconRef = R.drawable.battery_icon_green;
        }
        else if (halfBat >= mBattery.getBattLevel() && mBattery.getBattLevel() > lowBat) { //middle battery level
            batteryIconRef = R.drawable.battery_icon_yellow;
        }
        else { //low battery level
            batteryIconRef = R.drawable.battery_icon_red;
        }

        //Determine which communication icon to draw
        int communicationIconRef;
        int communicationSignal = getCommunicationSignal();
        if (communicationSignal > halfComm) { //High signal strength
            communicationIconRef = R.drawable.communication_icon_full;
        }
        else if (halfComm >= communicationSignal && communicationSignal > lowComm) { //Middle signal strength
            communicationIconRef = R.drawable.communication_icon_mid;
        }
        else if (lowComm >= communicationSignal && communicationSignal > NoComm) { //Low signal strength
            communicationIconRef = R.drawable.communication_icon_low;
        }
        else { //No signal
            communicationIconRef = R.drawable.communication_icon_empty;
        }

////////////////////////////////////////////////
        //Place battery- and communication icons

        //Canvas to work with for placing the battery and communication icons
        Canvas c = new Canvas(baseIcon);
        int center = baseIcon.getWidth()/2;

        //Get the battery- and communication icons from resources
        batteryIcon = BitmapFactory.decodeResource(context.getResources(), batteryIconRef);
        communicationIcon = BitmapFactory.decodeResource(context.getResources(), communicationIconRef);

        //Add battery icon to the base icon
        int batVert = (resolution/2)*batteryVertLocation/100;
        int batHor  = (resolution/2)*batteryHorLocation/100;
        int batHalfWidth = (batteryScaling/2)*batteryIcon.getWidth()/100;
        int batHalfHeight = (batteryScaling/2)*batteryIcon.getHeight()/100;

        Drawable bat = new BitmapDrawable(context.getResources(), batteryIcon);
        //(int left, int top, int right, int bottom)
        bat.setBounds(center+batHor-batHalfWidth, center-batVert-batHalfHeight, center+batHor+batHalfWidth, center-batVert+batHalfHeight);
        bat.draw(c);

        //Add communication icon to the base icon
        int commVert = (resolution/2)*commVertLocation/100;
        int commHor  = (resolution/2)*commHorLocation/100;
        int commHalfWidth = (commScaling/2)*communicationIcon.getWidth()/100;
        int commHalfHeight = (commScaling/2)*communicationIcon.getHeight()/100;

        Drawable comm = new BitmapDrawable(context.getResources(), communicationIcon);
        //(int left, int top, int right, int bottom)
        comm.setBounds(center - commHor - commHalfWidth, center - commVert - commHalfHeight, center - commHor + commHalfWidth, center - commVert + commHalfHeight);
        comm.draw(c);

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

    public Bitmap getIcon(){
        return AC_Icon;
    }

    public float getIconBoundOffset() { return ((AC_Icon.getHeight()-resolution)/2.0f)/AC_Icon.getHeight(); }

    public void setCircleColor(int color) {
        circleColor = color;
    }
}