package com.gcs.core;


import com.gcs.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class Icon {

    Resources res;
	
	private Bitmap AC_Icon;

    private boolean firstTimeDrawing = true;
	
	private float scalingFactor = 1.0f;
	private int circleColor = Color.WHITE;

    private int resolution;
    private int protectedZoneAlpha;
    private int sideOffsetArrow;

	private int batteryVertLocation;
	private int batteryHorLocation;
	private int batteryScaling;
	private int commVertLocation;
	private int commHorLocation;
	private int commScaling;

    private void setIconDrawingSettings() {
        resolution          = res.getInteger(R.integer.IconResolution);
        protectedZoneAlpha  = res.getInteger(R.integer.IconAlpha);
        sideOffsetArrow = (int) (0.25*resolution);

        batteryVertLocation = res.getInteger(R.integer.BatteryVertLocation);
        batteryHorLocation  = res.getInteger(R.integer.BatteryHorLocation);
        batteryScaling      = res.getInteger(R.integer.BatteryScaling);
        commVertLocation    = res.getInteger(R.integer.CommVertLocation);
        commHorLocation     = res.getInteger(R.integer.CommHorLocation);
        commScaling			= res.getInteger(R.integer.CommScaling);
    }
	
	public void generateIcon(ConflictStatus conflictStatus, float heading, int batLevel, int communicationSignal, Resources res){

        this.res = res;

        if(firstTimeDrawing) {
            setIconDrawingSettings();
            firstTimeDrawing = false;
        }

		Bitmap baseIcon, batteryIcon, communicationIcon;

        switch (conflictStatus) {
            case BLUE:
                baseIcon = BitmapFactory.decodeResource(res, R.drawable.aircraft_icon_blue);
                break;
            case GRAY:
                baseIcon = BitmapFactory.decodeResource(res, R.drawable.aircraft_icon_gray);
                break;
            case RED:
                baseIcon = BitmapFactory.decodeResource(res, R.drawable.aircraft_icon_red);
                break;
            default:
                baseIcon = BitmapFactory.decodeResource(res, R.drawable.aircraft_icon_blue);
        }
		
		//Place the icon on a white circle
		baseIcon = addCircle(baseIcon);
    	
		//Rotate the base icon
		baseIcon = RotateBitmap(baseIcon, heading);
		
		//Get the battery icon (full,half,low)
		int halfBat = res.getInteger(R.integer.HalfBatteryLevel);
		int lowBat  = res.getInteger(R.integer.LowBatteryLevel);
		/* TODO set the integer values to the correct orders to comply with the provided battery values */
		
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
		int halfComm = res.getInteger(R.integer.HalfCommunicationSignal);
		int lowComm  = res.getInteger(R.integer.LowBatteryLevel);
		int NoComm   = res.getInteger(R.integer.NoCommunicationSignal);
		
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
		baseIcon = stackIcons(baseIcon,batteryIcon,communicationIcon);
		
		//TODO add speedvector to icon

		AC_Icon = baseIcon;
	}
	
	//Add the base circle
	private Bitmap addCircle(Bitmap source) {
		
		Bitmap mutableBitmap = Bitmap.createBitmap(resolution, resolution, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(mutableBitmap);
		
		Paint paint = new Paint();
		paint.setColor(circleColor);
		paint.setAlpha(protectedZoneAlpha);
		paint.setFlags(Paint.ANTI_ALIAS_FLAG);
		
		canvas.drawCircle(resolution/2, resolution/2, resolution/2, paint);

		Drawable icon = new BitmapDrawable(res, source);
		icon.setBounds(sideOffsetArrow, sideOffsetArrow, resolution-sideOffsetArrow, resolution-sideOffsetArrow);
		icon.draw(canvas);
		
		return mutableBitmap;
	}
	
	//Rotate a bitmap
	private Bitmap RotateBitmap(Bitmap source, float angle){
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
	
    private Bitmap stackIcons(Bitmap baseIcon, Bitmap batteryIcon, Bitmap communicationIcon){

    	//Create bitmap to work with
    	Bitmap mutableBitmap = baseIcon.copy(Bitmap.Config.ARGB_8888, true);
    	Canvas c = new Canvas(mutableBitmap);
        int center = mutableBitmap.getWidth()/2;
    	
    	//Set scaling factor to avoid icon size changing with heading while printing the aircraft icon on the map
    	setScalingFactor(mutableBitmap.getWidth());

        //Add battery icon to the base icon
    	int batVert = (resolution/2)*batteryVertLocation/100;
    	int batHor  = (resolution/2)*batteryHorLocation/100;
    	int batHalfWidth = (batteryScaling/2)*batteryIcon.getWidth()/100;	
		int batHalfHeight = (batteryScaling/2)*batteryIcon.getHeight()/100;
		
        Drawable bat = new BitmapDrawable(res, batteryIcon);
        //(int left, int top, int right, int bottom)
        bat.setBounds(center+batHor-batHalfWidth, center-batVert-batHalfHeight, center+batHor+batHalfWidth, center-batVert+batHalfHeight);
        bat.draw(c);
        
        //Add communication icon to the base icon
        int commVert = (resolution/2)*commVertLocation/100;
    	int commHor  = (resolution/2)*commHorLocation/100;
    	int commHalfWidth = (commScaling/2)*communicationIcon.getWidth()/100;	
		int commHalfHeight = (commScaling/2)*communicationIcon.getHeight()/100;	
        
        Drawable comm = new BitmapDrawable(res, communicationIcon);
        //(int left, int top, int right, int bottom)
        comm.setBounds(center-commHor-commHalfWidth,center-commVert-commHalfHeight,center-commHor+commHalfWidth,center-commVert+commHalfHeight);
        comm.draw(c);

        return mutableBitmap;
    }
    
    private void setScalingFactor(int widthActual) {
    	scalingFactor = ((float)widthActual)/resolution;
    }
	
	public Bitmap getIcon(){
		return AC_Icon;
	}
	
	public float getIconScalingFactor() {
		return scalingFactor;
	}

	public float getIconBoundOffset() { return ((AC_Icon.getHeight()-resolution)/2.0f)/AC_Icon.getHeight(); }
	
	public void setCircleColor(int color) {
		circleColor = color;
	}
}