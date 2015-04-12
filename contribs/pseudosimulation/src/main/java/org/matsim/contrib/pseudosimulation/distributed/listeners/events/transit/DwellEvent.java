package org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit;

import java.io.Serializable;

class DwellEvent implements Comparable, Serializable {


    private final VehicleTracker vehicle;
    private final int indexInVehicleDwellEventList;
    private String stopId;
    private double arrivalTime;
    private double departureTime;
    private double occupancyAtDeparture;

    DwellEvent(double arrivalTime, String facilityId, VehicleTracker vehicle, int indexInVehicleDwellEventList) {
        this.arrivalTime = arrivalTime;
        this.departureTime = Double.POSITIVE_INFINITY;
        this.stopId=facilityId;
        this.vehicle = vehicle;
        this.indexInVehicleDwellEventList = indexInVehicleDwellEventList;
    }

    public int getIndexInVehicleDwellEventList() {
        return indexInVehicleDwellEventList;
    }

    public String getStopId() {
        return stopId;
    }

    public VehicleTracker getVehicle() {
        return vehicle;
    }

    public double getArrivalTime() {
        return arrivalTime;
    }

    public double getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(double departureTime) {
        this.departureTime = departureTime;
    }

    public double getOccupancyAtDeparture() {
        return occupancyAtDeparture;
    }

    public void setOccupancyAtDeparture(double occupancyAtDeparture) {
        this.occupancyAtDeparture = occupancyAtDeparture;
    }

    @Override
    public int compareTo(Object o) {
        return (int) (this.arrivalTime - ((DwellEvent) o).getArrivalTime());
    }
}
