package com.gcs.core;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.model.Altitude;
import com.model.Attitude;
import com.model.Battery;
import com.model.Speed;
import com.model.Position;

public class Aircraft {
	
	private Context context;
	
	public Aircraft(Context context){
	    this.context = context;
	}
	
	private Attitude       mAttitude = new Attitude();
	private Altitude       mAltitude = new Altitude();
	private Speed          mSpeed    = new Speed();
	private Battery        mBattery  = new Battery();
	private CustomState    mState    = new CustomState(); 
	private Position       mPosition = new Position();
	private Icon		   mIcon     = new Icon();
	private List<Waypoint> waypoints = new ArrayList<Waypoint>();
	
	private int communicationSignal   = 0;
	private final int AltitudeLabelId = TextView.generateViewId();
	private final int targetLabelId   = TextView.generateViewId();
	
	//Set and get functions for attitude
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
	
	//Set and get functions for Altitude
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
    
  //Set and get functions for Speed
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
    
  //Set and get functions for Battery
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
    
    //Set and get functions for State
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
    
  //Set and get functions for position
    public byte getSatVisible() {
    	return mPosition.getSatVisible();
	}
	
	public int getTimeStamp() {
		return mPosition.getTimeStamp();
	}
	
	public int getLat() {
		return mPosition.getLat();
	}
	
	public int getLon() {
		return mPosition.getLon();
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
		mPosition.setLlaHdg(lat,lon,alt,hdg);
	}
	
	//Set and get functions for icon
    public void generateIcon() {
    	mIcon.generateIcon(isOnUniqueAltitude(), isInConflict(), (float) mAttitude.getYaw(), getBattLevel(), communicationSignal);
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
    
    //Set and get functions for waypoints
    public void addWaypoint(float lat, float lon, float alt, short seq, byte targetSys, byte targetComp) {
    	Waypoint wp = new Waypoint(lat, lon, alt, seq, targetSys, targetComp);
        waypoints.add(wp);
    }
    
    public void setWpLat(float lat, int wpNumber) {
    	Waypoint wp = waypoints.get(wpNumber);
    	wp.setLat(lat);
    	waypoints.set(wpNumber,wp);
	}
    
    public void setWpLon(float lon, int wpNumber) {
    	Waypoint wp = waypoints.get(wpNumber);
    	wp.setLon(lon);
    	waypoints.set(wpNumber,wp);
	}
    
    public void setWpAlt(float alt, int wpNumber) {
    	Waypoint wp = waypoints.get(wpNumber);
    	wp.setAlt(alt);
    	waypoints.set(wpNumber,wp);
    }
    
	public void setWpSeq(short seq, int wpNumber) {
		Waypoint wp = waypoints.get(wpNumber);
    	wp.setSeq(seq);
    	waypoints.set(wpNumber,wp);
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
	
    //Set and get functions for class attributes
    public int getCommunicationSignal(){
    	return communicationSignal;
    }
    
    public void setCommunicationSignal(int communicationSignal){
    	this.communicationSignal = communicationSignal;
    }
    
    public int getAltLabelId(){
    	return AltitudeLabelId;
    }
    
    public int getTargetLabelId(){
    	return targetLabelId;
    }
    
    //Other
    public void setIconSettings(){
    	mIcon.setIconSettings(context.getResources());
    }
}