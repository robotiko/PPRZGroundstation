package com.gcs.helpers;

import android.location.Location;
import android.util.Log;
import android.util.SparseArray;

import com.gcs.core.Aircraft;
import com.gcs.core.ConflictStatus;
import com.gcs.core.TaskStatus;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class PerformanceCalcHelper {

    private static double totalScore = 0;

    private static double coverageScore, commScore, conflictScore, score;

    public static final List<Double> calcPerformance(List<Integer> ROIradiiList, int acCoverageRadius, List<LatLng> ROIList, SparseArray<Aircraft> mAircraft) {

        //Calculate the score components
        coverageScore = 1 * ROIcovered(ROIradiiList,acCoverageRadius,ROIList,mAircraft);
        commScore     = 2 * LossOfCommunicationCheck(mAircraft);
        conflictScore = 1 * ConflictCheck(mAircraft);

        //Summation of the score components
        score = coverageScore-commScore-conflictScore;
        totalScore += score;

        //Prepare to output the scores
        ArrayList<Double> returnList = new ArrayList<>();
        returnList.add(score);
        returnList.add(totalScore);

        return returnList;
    }

    //Calculate the percentage of the Region of Interest (ROI) that is covered by surveillance aircraft
    private final static double ROIcovered(List<Integer> ROIradiiList, int acCoverageRadius, List<LatLng> ROIList, SparseArray<Aircraft> mAircraft){
        //Area that can be covered by an aircraft
        double coverArea = acCoverageRadius*acCoverageRadius*Math.PI;

        //Calculate the overlap between covered region by aircraft and the ROI area
        double overlapArea = 0;
        for(int i=0; i<mAircraft.size(); i++) { //Loop over all aircraft
            int iKey = mAircraft.keyAt(i);
            if(mAircraft.get(iKey).getCommunicationSignal()>0 && mAircraft.get(iKey).getTaskStatus() == TaskStatus.SURVEILLANCE) { //Only calculate coverage if the aircraft can communicate with the ground station, has a surveillance status (at correct altitude) and the distance between aircraft and waypoint is less than the survey circle radius

                LatLng loc1, loc2;
                if(mAircraft.get(iKey).getDistanceToWaypoint() <= acCoverageRadius) {
                    loc1 = mAircraft.get(iKey).getWpLatLng(0);
                } else {
                    loc1 = mAircraft.get(iKey).getLatLng();
                }

                //Add overlap between aircraft coverage and ROI
                for (int k=0; k< ROIradiiList.size(); k++) {
                    double overlap = circleOverlap(ROIradiiList.get(k), acCoverageRadius, ROIList.get(k), loc1);
                    double doubleOverlap = 0;
                    //NOTE THAT THE OVERLAP OF 3+ CIRCLES IS NOT COVERED!!
                    if (overlap > 0) { //If not outside the ROI
                        for (int j = i + 1; j < mAircraft.size(); j++) {
                            int jKey = mAircraft.keyAt(j);
                            if (mAircraft.get(jKey).getDistanceToWaypoint() <= acCoverageRadius) {
                                loc2 = mAircraft.get(jKey).getWpLatLng(0);
                            } else {
                                loc2 = mAircraft.get(jKey).getLatLng();
                            }
                            //Account for overlap of the two UAVs
                            doubleOverlap += circleOverlap(acCoverageRadius, acCoverageRadius, loc1, loc2);
                        }
                    }
                    //Calculate the total coverage ove the ROI
                    overlapArea += (overlap - doubleOverlap)*(ROIradiiList.get(k)/120.0);
                }
            }
        }
        //Coverage percentage (percentage of area that is maximally possible to be covered)
        double score = overlapArea/coverArea;
        if (Double.isNaN(score)) score = 0;
        return score;
    }

    //Calculate the overlap area of two (coverage) circles
    private final static double circleOverlap(double radius1, double radius2, LatLng c1, LatLng c2){
        //Calculation of distance between two LatLng coordinates
        float[] distance = new float[1];
        Location.distanceBetween(c1.latitude, c1.longitude, c2.latitude, c2.longitude, distance);

        //Define the used radii
        double R = radius1;
        double r = radius2;

        //Make sure R is the largest of the two circles
        if(R < r) {
            R = radius2;
            r = radius1;
        }

        //Check whether the circles overlap, do not intersect or are inside each other. Then calculate accordingly
        double overlapArea;
        if(distance[0] > (R+r)) {                  //No overlap
            overlapArea = 0;
        } else if((distance[0]+r) <= R) {  //inside
            //Entire area of the small circle
            overlapArea = r*r*Math.PI;
        } else {                                   //Overlap
            double part1 = r*r*Math.acos((distance[0]*distance[0] + r*r - R*R)/(2*distance[0]*r));
            double part2 = R*R*Math.acos((distance[0]*distance[0] + R*R - r*r)/(2*distance[0]*R));
            double part3 = 0.5*Math.sqrt((-distance[0]+r+R)*(distance[0]+r-R)*(distance[0]-r+R)*(distance[0]+r+R));
            //Subtract the triangle areas from the cone areas to end up with the overlap area
            overlapArea = part1 + part2 - part3;
        }
        return overlapArea;
    }

    //Determine the number of aircraft without communication connection with the ground station
    private final static int LossOfCommunicationCheck(SparseArray<Aircraft> mAircraft) {
        int numberOfAircraft = mAircraft.size();
        int activeAircraft = 0;

        //Loop over aircraft in system
        for(int i=0; i<mAircraft.size(); i++) {
            int acNumber = mAircraft.keyAt(i);
            if(mAircraft.get(acNumber).getCommunicationSignal()>0) {
                activeAircraft++;
            }
        }
        return numberOfAircraft-activeAircraft;
    }

    //Calculate the number of aircraft that are in conflict
    private final static int ConflictCheck(SparseArray<Aircraft> mAircraft) {
        int noConflictAircraft = mAircraft.size();

        //Loop over aircraft in system
        for(int i=0; i<mAircraft.size(); i++) {
            int acNumber = mAircraft.keyAt(i);
            if(mAircraft.get(acNumber).getConflictStatus() != ConflictStatus.RED) {
                noConflictAircraft--;
            }
        }
        return noConflictAircraft;
    }

    private final static double BatteryScore(SparseArray<Aircraft> mAircraft,int halfBatteryVoltage, int lowBatteryVoltage) {
        double batteryScore = 1.0;
        int halfCount = 0;
        int lowCount  = 0;

        for(int i=0; i<mAircraft.size(); i++) {
            int acNumber = mAircraft.keyAt(i);
            if(mAircraft.get(acNumber).getBattVolt() <= lowBatteryVoltage) {
                lowCount++;
            } else if(mAircraft.get(acNumber).getBattVolt() <= halfBatteryVoltage) {
                halfCount++;
            }
        }

        if (lowCount>0) {
            batteryScore = 0.5;
        } else if (halfCount>0) {
            batteryScore = 0.0;
        }

        return batteryScore;
    }

    public static double getTotalScore() {
        return totalScore;
    }
}