package com.gcs.core;

public enum ConflictStatus {
    GRAY(1), BLUE(2), RED(3);

    private final int value;

    ConflictStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
