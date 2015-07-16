package com.gcs.core;

public class Waypoint {
	private double lat;
	
	private double lon;
	
	private float alt;
	
	private short seq; // WP index
	
	private byte targetSys;
	
	private byte targetComp;

	private boolean isSelected = false;

    private boolean isUpdating = false;
	
	public Waypoint(double lat, double lon, float alt, short seq, byte targetSys, byte targetComp) {
		this.lat = lat;
		this.lon = lon;
		this.alt = alt;
		this.seq = seq;
		this.targetSys = targetSys;
		this.targetComp = targetComp;
	}
	
	public void setLat(double lat) {
		this.lat = lat;
	}
	
	public void setLon(double lon) {
		this.lon = lon;
	}
	
	public void setAlt(float alt) {
		this.alt = alt;
	}
	
	public void setSeq(short seq) {
		this.seq = seq;
	}
	
	public void setTargetSys(byte targetSys) {
		this.targetSys = targetSys;
	}
	
	public void setTargetComp(byte targetComp) {
		this.targetComp = targetComp;
	}

    public void setSelected(boolean isSelected) { this.isSelected = isSelected; }

    public void setUpdating() {this.isUpdating = true;}
	
	public double getLat() {
		return this.lat;
	}
	
	public double getLon() {
		return this.lon;
	}
	
	public float getAlt() {
		return this.alt;
	}
	
	public short getSeq() {
		return this.seq;
	}
	
	public byte getTargetSys() {
		return this.targetSys;
	}
	
	public byte getTargetComp() {
		return this.targetComp;
	}

    public boolean isSelected() {return this.isSelected; }

    public boolean isUpdating() {return this.isUpdating; }
}