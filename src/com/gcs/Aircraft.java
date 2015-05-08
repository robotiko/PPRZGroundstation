package com.gcs;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.TextView;

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
	
	private Attitude    mAttitude = new Attitude();
	private Altitude    mAltitude = new Altitude();
	private Speed       mSpeed    = new Speed();
	private Battery     mBattery  = new Battery();
	private CustomState mState    = new CustomState(); 
	private Position    mPosition = new Position();
	
	private int communicationSignal   = 0;
	private final int AltitudeLabelId = TextView.generateViewId();
	
	/* TODO Implement the setting of the attributes in the service connection */
	
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
    
	////////////////////Icon////////////////////
	
	Bitmap AC_Icon;
			
	void generateIcon(){
		
		Bitmap baseIcon, batteryIcon, communicationIcon;
		Resources res = context.getResources();
		
		//Get the base icon (conflictStatus:red, blue, gray)
		if(isOnUniqueAltitude()){
			baseIcon = BitmapFactory.decodeResource(res, R.drawable.uav_icon_gray);
		} else {
			if (isInConflict()){
				baseIcon = BitmapFactory.decodeResource(res, R.drawable.uav_icon_red);
			} else {
				baseIcon = BitmapFactory.decodeResource(res, R.drawable.uav_icon_blue);
			}
		}
    	
		//Rotate the base icon
		baseIcon = RotateBitmap(baseIcon,(float) mAttitude.getYaw());
		
		//Get the battery icon (full,half,low)
		int batLevel = getBattLevel();
		int halfBat = context.getResources().getInteger(R.integer.HalfBatteryLevel);
		int lowBat  = context.getResources().getInteger(R.integer.LowBatteryLevel);
		/* TODO set the integer values to the correct orders to comply with the provide battery values */
		
		if(batLevel > halfBat) { //high battery level
			batteryIcon = BitmapFactory.decodeResource(res, R.drawable.battery_icon_green);
		}
		else if (halfBat >= batLevel && batLevel > lowBat) { //middle battery level
			batteryIcon = BitmapFactory.decodeResource(res, R.drawable.battery_icon_yellow);
		}
		else { //low battery level
			batteryIcon = BitmapFactory.decodeResource(res, R.drawable.battery_icon_red);
		}

		//Get the communication icon (full,mid,low,empty)
		int halfComm = context.getResources().getInteger(R.integer.HalfCommunicationSignal);
		int lowComm  = context.getResources().getInteger(R.integer.LowBatteryLevel);
		int NoComm   = context.getResources().getInteger(R.integer.NoCommunicationSignal);
		
		if (communicationSignal > halfComm) { //High signal strength
			communicationIcon = BitmapFactory.decodeResource(res, R.drawable.communication_icon_full);
		}
		else if (halfComm >= communicationSignal && communicationSignal > lowComm) { //Middle signal strength
			communicationIcon = BitmapFactory.decodeResource(res, R.drawable.communication_icon_mid);
		}
		else if (lowComm >= communicationSignal && communicationSignal > NoComm) { //Low signal strength
			communicationIcon = BitmapFactory.decodeResource(res, R.drawable.communication_icon_low);
		}
		else { //No signal
			communicationIcon = BitmapFactory.decodeResource(res, R.drawable.communication_icon_empty);
		}

		//Place battery- and communication icons
		baseIcon = stackIcons(baseIcon,batteryIcon,communicationIcon,res);
		
		//TODO add speedvector to icon

		AC_Icon = baseIcon;
	}
	
	//Rotate a bitmap
	private Bitmap RotateBitmap(Bitmap source, float angle){
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
	
    private Bitmap stackIcons(Bitmap baseIcon, Bitmap batteryIcon, Bitmap communicationIcon, Resources res){
    	
    	//TODO enable markers to scale with zoom for a constant keepout region (or use GroundOverlays)
    	
    	//TODO Make white base-cirle that is drawn in code to prevent hardcoding numbers for location of battery/communication icons

    	//Create bitmap to work with
    	Bitmap mutableBitmap = baseIcon.copy(Bitmap.Config.ARGB_8888, true);
    	Canvas c = new Canvas(mutableBitmap);
    	
    	int center = mutableBitmap.getWidth()/2;

    	//(int left, int top, int right, int bottom)
        //Add battery icon to the base icon
        Drawable bat = new BitmapDrawable(res, batteryIcon);
        bat.setBounds(center+11, center-54, center+21, center-34);
//        bat.setBounds(75, 10, 85, 30);
        bat.draw(c);
        
        //Add communication icon to the base icon
        Drawable comm = new BitmapDrawable(res, communicationIcon);
        comm.setBounds(center-24,center-53,center-4,center-35);
//        comm.setBounds(40, 11, 60, 29);
        comm.draw(c);

        return mutableBitmap;
    }
	
	public Bitmap getIcon(){
		return AC_Icon;
	}	    
}
