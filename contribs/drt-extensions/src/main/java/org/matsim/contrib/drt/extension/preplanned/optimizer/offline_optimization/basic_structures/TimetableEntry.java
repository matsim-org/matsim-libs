package org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.basic_structures;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

public class TimetableEntry {

    public enum StopType {PICKUP, DROP_OFF}

    private final GeneralRequest request;
    private final StopType stopType;
    private double arrivalTime;
    private double departureTime;
    private int occupancyBeforeStop;
    private final double stopDuration;
    private final int capacity;
    private double slackTime;

    public TimetableEntry(GeneralRequest request, StopType stopType, double arrivalTime,
                          double departureTime, int occupancyBeforeStop, double stopDuration,
                          DvrpVehicle vehicle) {
        this.request = request;
        this.stopType = stopType;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.occupancyBeforeStop = occupancyBeforeStop;
        this.stopDuration = stopDuration;
        this.capacity = vehicle.getCapacity();
        this.slackTime = departureTime - (stopDuration + arrivalTime);
    }

    /**
     * Make a copy of the object
     */
    public TimetableEntry(TimetableEntry timetableEntry) {
        this.request = timetableEntry.request;
        this.stopType = timetableEntry.stopType;
        this.arrivalTime = timetableEntry.arrivalTime;
        this.departureTime = timetableEntry.departureTime;
        this.occupancyBeforeStop = timetableEntry.occupancyBeforeStop;
        this.stopDuration = timetableEntry.stopDuration;
        this.capacity = timetableEntry.capacity;
        this.slackTime = timetableEntry.slackTime;
    }

    @Deprecated
    double delayTheStop(double delay) {
        double effectiveDelay = getEffectiveDelayIfStopIsDelayedBy(delay);
        arrivalTime += delay;
        departureTime += effectiveDelay;
        return effectiveDelay;
    }

    public void increaseOccupancyByOne() {
        occupancyBeforeStop += 1;
    }

    public void decreaseOccupancyByOne() {
        occupancyBeforeStop -= 1;
    }

    public double getEffectiveDelayIfStopIsDelayedBy(double delay) {
        double departureTimeAfterAddingDelay = Math.max(arrivalTime + delay, getEarliestDepartureTime()) + stopDuration;
        return departureTimeAfterAddingDelay - departureTime;
    }

    public void delayTheStopBy(double delay) {
        // Note: delay can be negative (i.e., bring forward)
        arrivalTime += delay;
        departureTime = Math.max(arrivalTime, getEarliestDepartureTime()) + stopDuration;
        slackTime = departureTime - (arrivalTime + stopDuration);
    }

    public void updateArrivalTime(double newArrivalTime) {
        arrivalTime = newArrivalTime;
        departureTime = Math.max(arrivalTime, getEarliestDepartureTime()) + stopDuration;
        slackTime = departureTime - (arrivalTime + stopDuration);
    }

    // Checking functions
    public boolean isTimeConstraintViolated(double delay) {
        return arrivalTime + delay > getLatestArrivalTime();
    }

    @Deprecated
    double checkDelayFeasibilityAndReturnEffectiveDelay(double delay) {
        double effectiveDelay = getEffectiveDelayIfStopIsDelayedBy(delay);
        if (isTimeConstraintViolated(delay)) {
            return -1; // if not feasible, then return -1 //TODO do not use this anymore, as delay can now be negative also!!!
        }
        return effectiveDelay;
    }

    public boolean isVehicleFullBeforeThisStop() {
        return occupancyBeforeStop >= capacity;
    }

    public boolean isVehicleOverloaded() {
        return stopType == StopType.PICKUP ? occupancyBeforeStop >= capacity : occupancyBeforeStop > capacity;
    }

    // Getter functions
    public GeneralRequest getRequest() {
        return request;
    }

    public double getArrivalTime() {
        return arrivalTime;
    }

    public double getDepartureTime() {
        return departureTime;
    }

    public Id<Link> getLinkId() {
        if (stopType == StopType.PICKUP) {
            return request.getFromLinkId();
        }
        return request.getToLinkId();
    }

    public int getOccupancyBeforeStop() {
        return occupancyBeforeStop;
    }

    public double getEarliestDepartureTime() {
        return request.getEarliestDepartureTime();
    }

    public double getSlackTime() {
        return slackTime;
    }

    public double getLatestArrivalTime() {
        return stopType == StopType.PICKUP ? request.getLatestDepartureTime() : request.getLatestArrivalTime();
    }

    public StopType getStopType() {
        return stopType;
    }

    @Override
    public String toString() {
        return "TimetableEntry{" +
                "request=" + request.getPassengerIds().toString() +
                ", stopType=" + stopType +
                ", arrivalTime=" + arrivalTime +
                ", departureTime=" + departureTime +
                ", occupancyBeforeStop=" + occupancyBeforeStop +
                ", stopDuration=" + stopDuration +
                ", capacity=" + capacity +
                ", slackTime=" + slackTime +
                '}';
    }
}
