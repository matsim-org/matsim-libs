package org.matsim.contrib.drt.extension.operations.shifts.optimizer;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.optimizer.StopWaypoint;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.DvrpLoadType;

import java.util.Optional;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftChangeoverStopWaypoint implements StopWaypoint {

    private final ShiftChangeOverTask shiftChangeOverTask;
    private final double latestArrivalTime;
    private final double earliestArrivalTime;
    private final double latestDepartureTime;

    private final DvrpLoad emptyLoad;


    public ShiftChangeoverStopWaypoint(ShiftChangeOverTask shiftChangeOverTask, DvrpLoadType loadType) {
        this.shiftChangeOverTask = shiftChangeOverTask;
        // A scheduled changeover is a hard commitment ("be at the hub at this time") that insertions must not push
        // back. We anchor the arrival window to the changeover's own planned begin time, not to shift.getEndTime().
        // For a regular end-of-shift changeover the two coincide (the changeover begins at the shift end), so this is
        // behaviourally identical. For a changeover that was pulled forward (e.g. a remote-guidance recall, whose shift
        // end is the far-off simulation horizon) this keeps the earlier commitment fixed instead of letting new
        // requests stretch it back towards the horizon.
        this.latestArrivalTime = this.shiftChangeOverTask.getBeginTime();
        this.earliestArrivalTime = this.shiftChangeOverTask.getBeginTime();
        this.latestDepartureTime = Double.POSITIVE_INFINITY;
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
    public double getEarliestArrivalTime() {
        return earliestArrivalTime;
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
    public Optional<DvrpLoad> getChangedCapacity() {
        return Optional.empty();
    }

    @Override
    public boolean scheduleWaitBeforeDrive() {
        return true;
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
}

