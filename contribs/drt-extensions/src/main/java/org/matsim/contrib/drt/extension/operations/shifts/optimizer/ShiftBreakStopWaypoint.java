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


    public ShiftBreakStopWaypoint(ShiftBreakTask shiftBreakTask, DvrpLoadType loadType) {
        this.shiftBreakTask = shiftBreakTask;
        this.earliestArrivalTime = calcEarliestArrivalTime();
        this.latestArrivalTime = calcLatestArrivalTime();
        this.latestDepartureTime = calcLatestDepartureTime();
        this.emptyLoad = loadType.getEmptyLoad();
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
        return emptyLoad;
    }

    private double calcEarliestArrivalTime() {
        return shiftBreakTask.getShiftBreak().getEarliestBreakStartTime();
    }

    private double calcLatestArrivalTime() {
        DrtShiftBreak shiftBreak = shiftBreakTask.getShiftBreak();
        return shiftBreak.getLatestBreakEndTime() - shiftBreak.getDuration();
    }

    private double calcLatestDepartureTime() {
        return shiftBreakTask.getShiftBreak().getLatestBreakEndTime();
    }
}
