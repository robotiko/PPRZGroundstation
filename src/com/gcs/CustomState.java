package com.gcs;

import com.model.State;

public class CustomState extends State {

	private String conflictState;
	
	
	public String getConflictState() {
        return this.conflictState;
    }

    public void setConflictState(String newState) {
        if (this.conflictState != newState) {
            this.conflictState = newState;
        }
    }
	
	
	
}
