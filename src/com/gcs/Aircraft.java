package com.gcs;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.Toast;


public class Aircraft {
	
	private Context context;
	
	public Aircraft(Context context){
	    this.context = context;
	}
	
	private final Attitude mAttitude = new Attitude();
	
	public void setRollPitchYaw(double roll, double pitch, double yaw) {
    	mAttitude.setRollPitchYaw(roll, pitch, yaw);
    }
	
	//TODO add battery attribute for battery status
	String battery = "full";
	
	//TODO add conflict status attribute
	String conflictStatus = "gray";
	
	//TODO add communication status attribute
	String comm = "low";
	
	////////////////////Icon////////////////////
	
	Bitmap AC_Icon;
			
	void generateIcon(){
		
		Bitmap baseIcon, batteryIcon, communicationIcon;
		Resources res = context.getResources();
		
		//Get the base icon (conflictStatus:red, blue, gray)
		switch (conflictStatus){
			case "red": {
				baseIcon = BitmapFactory.decodeResource(res, R.drawable.uav_icon_red);
				break;
			}
			case "blue": {
				baseIcon = BitmapFactory.decodeResource(res, R.drawable.uav_icon_blue);
				break;
			}
			case "gray": {
				baseIcon = BitmapFactory.decodeResource(res, R.drawable.uav_icon_gray);
				break;
			}
			default: {
				baseIcon = BitmapFactory.decodeResource(res, R.drawable.uav_icon_gray);
				break;
			}
		}
    	
		//Rotate the base icon
		baseIcon = RotateBitmap(baseIcon,(float) mAttitude.getYaw());
		
//		Toast.makeText(context.getApplicationContext(), String.valueOf(baseIcon.getHeight()), Toast.LENGTH_SHORT).show();
		
		//Get the battery icon (full,half,low,empty)
		switch (battery){
			case "full": {
				batteryIcon = BitmapFactory.decodeResource(res, R.drawable.battery_icon_green);
				break;
			}
			case "half": {
				batteryIcon = BitmapFactory.decodeResource(res, R.drawable.battery_icon_yellow);
				break;
			}
			case "empty": {
				batteryIcon = BitmapFactory.decodeResource(res, R.drawable.battery_icon_red);
				break;
			}
			default: {
				batteryIcon = BitmapFactory.decodeResource(res, R.drawable.battery_icon_green);
				break;
			}
		}
		
		//Get the communication icon (full,mid,low,empty)
		switch (comm){
			case "full": {
				communicationIcon = BitmapFactory.decodeResource(res, R.drawable.communication_icon_full);
				break;
			}
			case "half": {
				communicationIcon = BitmapFactory.decodeResource(res, R.drawable.communication_icon_mid);
				break;
			}
			case "low": {
				communicationIcon = BitmapFactory.decodeResource(res, R.drawable.communication_icon_low);
				break;
			}
			case "empty": {
				communicationIcon = BitmapFactory.decodeResource(res, R.drawable.communication_icon_empty);
				break;
			}
			default: {
				communicationIcon = BitmapFactory.decodeResource(res, R.drawable.communication_icon_full);
				break;
			}
		}
		
		//Place battery- and communication icons
		baseIcon = stackIcons(baseIcon,batteryIcon,communicationIcon,res);
		
		//TODO add speedvector to icon

		AC_Icon = baseIcon;
	}
	
	//Rotate a bitmap
	private Bitmap RotateBitmap(Bitmap source, float angle){
        Matrix matrix = new Matrix();
//        matrix.postRotate(angle);
        
        matrix.postRotate(angle, source.getWidth()/2, source.getHeight()/2);
        
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
	
    private Bitmap stackIcons(Bitmap baseIcon, Bitmap batteryIcon, Bitmap communicationIcon, Resources res){
    	
    	//TODO solve issue of icons being placed outside base icon due to the rotation of a square icon

    	//Create bitmap to work with
    	Bitmap mutableBitmap = baseIcon.copy(Bitmap.Config.ARGB_8888, true);
    	Canvas c = new Canvas(mutableBitmap);

    	//(int left, int top, int right, int bottom)
        //Add battery icon to the base icon
        Drawable bat = new BitmapDrawable(res, batteryIcon);
        bat.setBounds(75, 10, 85, 30);
        bat.draw(c);
        
        //Add communication icon to the base icon
        Drawable comm = new BitmapDrawable(res, communicationIcon);
        comm.setBounds(40, 11, 60, 29);
        comm.draw(c);

        return mutableBitmap;
    };
	

	public Bitmap getIcon(){
		return AC_Icon;
	}
	    
}
