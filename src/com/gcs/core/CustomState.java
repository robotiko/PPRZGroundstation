package com.gcs.core;

import com.model.State;

public class CustomState extends State {

	private boolean isInConflict = false;
	private boolean isOnUniqueAltitude = true;
	
	
	public boolean isInConflict() {
        return this.isInConflict;
    }

    public void setIsInConflict(boolean newState) {
        if (this.isInConflict != newState) {
            this.isInConflict = newState;
        }
    }
    
    public boolean isOnUniqueAltitude() {
        return this.isOnUniqueAltitude;
    }
	
    public void setIsOnUniqueAltitude(boolean newState) {
        if (this.isOnUniqueAltitude != newState) {
            this.isOnUniqueAltitude = newState;
        }
    }
	
}
