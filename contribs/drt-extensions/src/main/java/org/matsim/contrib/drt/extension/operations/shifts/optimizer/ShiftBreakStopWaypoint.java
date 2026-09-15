package org.matsim.contrib.drt.extension.operations.shifts.optimizer;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftBreakTask;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.optimizer.StopWaypoint;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.DvrpLoadType;

import java.util.Optional;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftBreakStopWaypoint implements StopWaypoint {

    private final ShiftBreakTask shiftBreakTask;
    private final double latestArrivalTime;
    private final double earliestArrivalTime;
    private final double latestDepartureTime;

    private final DvrpLoad emptyLoad;
    private final DvrpLoad outgoingOccupancy;


    public ShiftBreakStopWaypoint(ShiftBreakTask shiftBreakTask, DvrpLoadType loadType, DvrpLoad outgoingOccupancy) {
        this.shiftBreakTask = shiftBreakTask;
        this.earliestArrivalTime = shiftBreakTask.calcEarliestArrivalTime();
        this.latestArrivalTime = shiftBreakTask.calcLatestArrivalTime();
        this.latestDepartureTime = shiftBreakTask.calcLatestDepartureTime();
        this.emptyLoad = loadType.getEmptyLoad();
        this.outgoingOccupancy = outgoingOccupancy;
    }

    @Override
    public double getEarliestArrivalTime() {
        return earliestArrivalTime;
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
        return shiftBreakTask;
    }

    @Override
    public DvrpLoad getOccupancyChange() {
        DvrpLoad pickedUp = shiftBreakTask.getPickupRequests().values().stream().map(AcceptedDrtRequest::getLoad).reduce(DvrpLoad::add).orElse(emptyLoad);
        DvrpLoad droppedOff = shiftBreakTask.getDropoffRequests().values().stream().map(AcceptedDrtRequest::getLoad).reduce(DvrpLoad::add).orElse(emptyLoad);
        return pickedUp.subtract(droppedOff);
    }

    @Override
    public Optional<DvrpLoad> getChangedCapacity() {
        return Optional.empty();
    }

    @Override
    public boolean scheduleWaitBeforeDrive() {
        return true;
    }

    @Override
    public Link getLink() {
        return shiftBreakTask.getLink();
    }

    @Override
    public double getArrivalTime() {
        return shiftBreakTask.getBeginTime();
    }

    @Override
    public double getDepartureTime() {
        return shiftBreakTask.getEndTime();
    }

    @Override
    public DvrpLoad getOutgoingOccupancy() {
        return outgoingOccupancy;
    }
}
