package com.gcs.core;


import com.gcs.R;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class Icon {
	
	private Bitmap AC_Icon;
	
	private float scalingFactor = 1.0f;
	private int circleColor = Color.WHITE;
	
	/* TODO make the size of the icon dynamic */
	private int dimensions = 200;
	
	public void generateIcon(boolean isOnUniqueAltitude, boolean isInConflict, float heading, int batLevel, int communicationSignal, Resources res){
		
		Bitmap baseIcon, batteryIcon, communicationIcon;
		
		//Get the base icon (conflictStatus:red, blue, gray)
		if(isOnUniqueAltitude){
			baseIcon = BitmapFactory.decodeResource(res, R.drawable.aircraft_icon_gray);
		} else {
			if (isInConflict){
				baseIcon = BitmapFactory.decodeResource(res, R.drawable.aircraft_icon_red);
			} else {
				baseIcon = BitmapFactory.decodeResource(res, R.drawable.aircraft_icon_blue);
			}
		}
		
		//Place the icon on a white circle
		baseIcon = addCircle(baseIcon,dimensions,res);
    	
		//Rotate the base icon
		baseIcon = RotateBitmap(baseIcon,(float) heading);
		
		//Get the battery icon (full,half,low)
//		int batLevel = getBattLevel();
		int halfBat = res.getInteger(R.integer.HalfBatteryLevel);
		int lowBat  = res.getInteger(R.integer.LowBatteryLevel);
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
		baseIcon = stackIcons(baseIcon,batteryIcon,communicationIcon,res);
		
		//TODO add speedvector to icon

		AC_Icon = baseIcon;
	}
	
	//Add the base circle
	private Bitmap addCircle(Bitmap source, int dimensions, Resources res) {
		
		Bitmap mutableBitmap = Bitmap.createBitmap(dimensions, dimensions, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(mutableBitmap);
		
		/* TODO Make the color of the circle dynamic */
		Paint paint = new Paint();
		paint.setColor(circleColor);
		paint.setFlags(Paint.ANTI_ALIAS_FLAG);
		
		canvas.drawCircle(dimensions/2, dimensions/2, dimensions/2, paint);

		Drawable icon = new BitmapDrawable(res, source);
		int sideOffset = (int) (0.25*dimensions);
		icon.setBounds(sideOffset, sideOffset, dimensions-sideOffset, dimensions-sideOffset);
		icon.draw(canvas);
		
		return mutableBitmap;
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
//    	Bitmap mutableBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
    	Canvas c = new Canvas(mutableBitmap);
//    	Drawable icon = new BitmapDrawable(res, baseIcon);
    	int center = mutableBitmap.getWidth()/2;
//    	int offSet = center-100;
//    	icon.setBounds(offSet, offSet, offSet+200, offSet+200);
//    	icon.draw(c);
    	
    	setScalingFactor(mutableBitmap.getWidth());

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
    
    private void setScalingFactor(int widthActual) {
//    	Log.d("Actual width",String.valueOf(((float)widthActual)/dimensions));

    	scalingFactor = ((float)widthActual)/dimensions;
    }
	
	public Bitmap getIcon(){
		return AC_Icon;
	}
	
	public float getIconScalingFactor() {
		return scalingFactor;
	}
	
	public void setCircleColor(int color) {
		circleColor = color;
	}
}
