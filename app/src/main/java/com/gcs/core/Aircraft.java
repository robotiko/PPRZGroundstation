package com.gcs.core;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
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
	
	public Aircraft(Context context){
	    this.context = context;
	}

	private Heartbeat	   mHeartbeat = new Heartbeat();
	private Attitude       mAttitude  = new Attitude();
	private Altitude       mAltitude  = new Altitude();
	private Speed          mSpeed     = new Speed();
	private Battery        mBattery   = new Battery();
	private CustomState    mState     = new CustomState();
	private Position       mPosition  = new Position();
	private Icon		   mIcon      = new Icon();
	private List<Waypoint> waypoints  = new ArrayList<Waypoint>();

    public Marker acMarker, infoWindow;
    public List<Marker> wpMarkers  = new ArrayList<Marker>();
	public Polyline flightPath;
	
	private final int AltitudeLabelId   = TextView.generateViewId();
	private final int targetLabelId     = TextView.generateViewId();
	private final String labelCharacter = String.valueOf((char)(64+AltitudeLabelId));
	private boolean isSelected          = false;
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
    	mSpeed.setGroundAndAirSpeeds(groundSpeed,airSpeed,climbSpeed);
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

    public void setConflictStatus(boolean newIsInConflict, boolean newIsOnUniqueAltitude) {
        mState.setConflictStatus(newIsInConflict, newIsOnUniqueAltitude);
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

    //////////// ICON ////////////
    public void generateIcon() {
    	mIcon.generateIcon(mState.getConflictStatus(), (float) mAttitude.getYaw(), getBattLevel(), getCommunicationSignal());
    }
    
    public void setCircleColor(int color) {
    	mIcon.setCircleColor(color);
    }
    
    public Bitmap getIcon() {
		return mIcon.getIcon();
	}
    
    public float getIconScalingFactor() {
    	return mIcon.getIconScalingFactor();
    }

    public float getIconBoundOffset() {return mIcon.getIconBoundOffset(); }

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

    public void setDistanceHome(LatLng homeLocation) {
        float[] distance = new float[1];
        Location.distanceBetween(mPosition.getLat(),mPosition.getLon(),homeLocation.latitude,homeLocation.longitude,distance);
        distanceHome = distance[0];
    }

    public float getDistanceHome() {
        return distanceHome;
    }

    //////////// OTHER ////////////
    public void setIconSettings(){
    	mIcon.setIconSettings(context.getResources());
    }
}