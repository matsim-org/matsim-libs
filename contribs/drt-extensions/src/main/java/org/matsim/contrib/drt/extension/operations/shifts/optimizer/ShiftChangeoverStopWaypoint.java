package org.matsim.contrib.drt.extension.operations.shifts.optimizer;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.optimizer.StopWaypoint;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.DvrpLoadType;

public class ShiftChangeoverStopWaypoint implements StopWaypoint {

    private final ShiftChangeOverTask shiftChangeOverTask;
    private final double latestArrivalTime;
    private final double latestDepartureTime;

    private final DvrpLoad emptyLoad;


    public ShiftChangeoverStopWaypoint(ShiftChangeOverTask shiftChangeOverTask, DvrpLoadType loadType) {
        this.shiftChangeOverTask = shiftChangeOverTask;
        this.latestArrivalTime = calcLatestArrivalTime();
        this.latestDepartureTime = calcLatestDepartureTime();
        this.emptyLoad = loadType.getEmptyLoad();
    }

    @Override
    public double getLatestArrivalTime() {
        return latestArrivalTime;
    }

    @Override
    public double getLatestDepartureTime() {
        return latestDepartureTime;
    }

    @Override
    public DrtStopTask getTask() {
        return shiftChangeOverTask;
    }

    @Override
    public DvrpLoad getOccupancyChange() {
        DvrpLoad pickedUp = shiftChangeOverTask.getPickupRequests().values().stream().map(AcceptedDrtRequest::getLoad).reduce(DvrpLoad::add).orElse(emptyLoad);
        DvrpLoad droppedOff = shiftChangeOverTask.getDropoffRequests().values().stream().map(AcceptedDrtRequest::getLoad).reduce(DvrpLoad::add).orElse(emptyLoad);
        return pickedUp.subtract(droppedOff);
    }

    @Override
    public DvrpLoad getChangedCapacity() {
        return null;
    }

    @Override
    public Link getLink() {
        return shiftChangeOverTask.getLink();
    }

    @Override
    public double getArrivalTime() {
        return shiftChangeOverTask.getBeginTime();
    }

    @Override
    public double getDepartureTime() {
        return shiftChangeOverTask.getEndTime();
    }

    @Override
    public DvrpLoad getOutgoingOccupancy() {
        return emptyLoad;
    }


    private double calcLatestArrivalTime() {
        return shiftChangeOverTask.getShift().getEndTime();
    }

    private double calcLatestDepartureTime() {
        return Double.POSITIVE_INFINITY;
    }
}

