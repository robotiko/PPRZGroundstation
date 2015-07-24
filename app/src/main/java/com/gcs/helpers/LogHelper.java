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

    public static final void dataLogger(long initTime, String logFileName, double performanceScore, SparseArray<Aircraft> mAircraft) {

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
            //First columns are [Time, Uptime, Performance score]
            myOutWriter.append(time + ", " + String.format("%.1f", uptime*1e-3) + ", " + performanceScore);
            //Loop over all aircraft to write a line to the log file with the following data of all aircraft: [Altitude, Latitude, Longitude]
            for(int i=1; i<mAircraft.size()+1; i++) {
                /* TODO log waypoint location instead of aircraft location */
                myOutWriter.append(", " + mAircraft.get(i).getAGL() + ", " + mAircraft.get(i).getLat() + ", " + mAircraft.get(i).getLon());
            }
            //End the line and close the file
            myOutWriter.append("\r\n");
            myOutWriter.close();

        } catch(IOException e){
            Log.e(TAG, "Error while writing to logfile");
        }
    }

}
