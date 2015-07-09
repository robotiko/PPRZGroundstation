package com.gcs.core;

import com.sharedlib.model.State;

public class CustomState extends State {

    /* TODO Remove State extend */

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
}