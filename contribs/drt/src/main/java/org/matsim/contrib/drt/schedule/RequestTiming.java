package org.matsim.contrib.drt.schedule;

import org.matsim.core.utils.misc.OptionalTime;

public final class RequestTiming {

    public static final double UNDEFINED_TIME = Double.NEGATIVE_INFINITY;

    private double plannedPickupTime = UNDEFINED_TIME;
    private double plannedDropoffTime = UNDEFINED_TIME;

    public RequestTiming(double plannedPickupTime, double plannedDropoffTime) {
        this.plannedPickupTime = plannedPickupTime;
        this.plannedDropoffTime = plannedDropoffTime;
    }

    public RequestTiming() {
    }

    public OptionalTime getPlannedPickupTime() {
        return plannedPickupTime == UNDEFINED_TIME ? OptionalTime.undefined(): OptionalTime.defined(plannedPickupTime);
    }

    public OptionalTime getPlannedDropoffTime() {
        return plannedDropoffTime == UNDEFINED_TIME ? OptionalTime.undefined(): OptionalTime.defined(plannedDropoffTime);
    }

    /**
     * deliberately package private to restrict access to the timing updater
     */
    void updatePlannedPickupTime(Double plannedPickupTime) {
        this.plannedPickupTime = plannedPickupTime;
    }

    /**
     * deliberately package private to restrict access to the timing updater
     */
    void updatePlannedDropoffTime(Double plannedDropoffTime) {
        this.plannedDropoffTime = plannedDropoffTime;
    }
}
