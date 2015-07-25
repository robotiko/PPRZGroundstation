package com.gcs.core;

public enum TaskStatus {
    NONE(0), SURVEILLANCE(1), RELAY(2);

    private final int value;

    TaskStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}