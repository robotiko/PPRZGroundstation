package com.gcs.core;

import com.sharedlib.model.State;

public class CustomState extends State {

	private boolean isInConflict = false;
	private boolean isOnUniqueAltitude = false;

    private ConflictStatus conflictStatus = ConflictStatus.GRAY;

    public void updateConflictStatus() {
        if(isOnUniqueAltitude){
            conflictStatus = ConflictStatus.GRAY;
        } else {
            if (isInConflict){
                conflictStatus = ConflictStatus.RED;
            } else {
                conflictStatus = ConflictStatus.BLUE;
            }
        }
    }

    public void setConflictStatusNew(ConflictStatus NewStatus) {
        conflictStatus = NewStatus;
    }

    public ConflictStatus getConflictStatus() {
        return conflictStatus;
    }

//    public void setConflictStatus(boolean newIsInConflict, boolean newIsOnUniqueAltitude) {
//
//        setIsInConflict(newIsInConflict);
//        setIsOnUniqueAltitude(newIsOnUniqueAltitude);
//
//        if(isOnUniqueAltitude){
//           conflictStatus = ConflictStatus.GRAY;
//        } else {
//            if (isInConflict){
//                conflictStatus = ConflictStatus.RED;
//            } else {
//                conflictStatus = ConflictStatus.BLUE;
//            }
//        }
//    }
//
//	public boolean isInConflict() {
//        return this.isInConflict;
//    }
//
//    public void setIsInConflict(boolean newState) {
//        if (this.isInConflict != newState) {
//            this.isInConflict = newState;
//        }
//    }
//
//    public boolean isOnUniqueAltitude() {
//        return this.isOnUniqueAltitude;
//    }
//
//    public void setIsOnUniqueAltitude(boolean newState) {
//        if (this.isOnUniqueAltitude != newState) {
//            this.isOnUniqueAltitude = newState;
//        }
//    }
	
}