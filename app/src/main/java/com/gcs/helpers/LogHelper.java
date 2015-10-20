package com.gcs.helpers;

import android.os.Environment;
import android.util.Log;
import android.util.SparseArray;

import com.gcs.MainActivity;
import com.gcs.core.Aircraft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;

public class LogHelper {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static String logFileName = "";

    //Method called from onCreate to instantiate a file name
    public static final void createLogfileName() {
        Calendar cal=Calendar.getInstance(); //Note that January is 0 in JAVA
        logFileName = "log" + String.format("%4d%02d%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1 ,cal.get(Calendar.DAY_OF_MONTH))
                + String.format("%02d%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)) + ".txt";
    }

    public static final void dataLogger(long initTime, SparseArray<Aircraft> mAircraft) {
        //Get time and date
        Calendar cal = Calendar.getInstance();
        int hours    = cal.get(Calendar.HOUR_OF_DAY);
        int minutes  = cal.get(Calendar.MINUTE);
        int seconds  = cal.get(Calendar.SECOND);

        //Make a time string to include in the log file
        String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        //Calculate the time the application has been active (milliSeconds)
        int uptime = (int)(System.currentTimeMillis() - initTime);

        try {
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File (sdCard.getAbsolutePath() + "/gcsData");
            dir.mkdirs();
            File file = new File(dir, logFileName);
            FileOutputStream f = new FileOutputStream(file, true);

            OutputStreamWriter myOutWriter = new OutputStreamWriter(f);
            //First columns are [Time, Uptime]
            myOutWriter.append(time + ", " + String.format("%.1f", uptime*1e-3));
            //Loop over all aircraft to write a line to the log file with the following data of all aircraft: [Aircraft number ,Altitude, Latitude, Longitude, wpLatitude, wpLongitude, Communication signal, Status/task (0=none,1=surveillance,2=relay), Conflictstatus, batVoltage]
            for(int i=0; i<mAircraft.size(); i++) {
                int acNumber = mAircraft.keyAt(i);
                myOutWriter.append(", " + acNumber + ", " + mAircraft.get(acNumber).getAGL() + ", " + mAircraft.get(acNumber).getLat() + ", " + mAircraft.get(acNumber).getLon() + ", " + mAircraft.get(acNumber).getWpLat(0) + ", " + mAircraft.get(acNumber).getWpLon(0) + ", " + mAircraft.get(acNumber).getCommunicationSignal() + ", "  + mAircraft.get(acNumber).getConflictStatus().getValue() + ", " + mAircraft.get(acNumber).getBattVolt());
            }
            //End the line and close the file
            myOutWriter.append("\r\n");
            myOutWriter.close();

        } catch(IOException e){
            Log.e(TAG, "Error while writing to logfile");
        }
    }

}
