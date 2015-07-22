package com.gcs.core;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class Home {

    private LatLng homeLocation;
    public Marker homeMarker;
    public Circle homeCommCircle;

    public void setHomeLocation(LatLng homeLocation) {
        this.homeLocation = homeLocation;
    }

    public LatLng getHomeLocation() {
        return homeLocation;
    }
}
