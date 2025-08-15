package org.matsim.contrib.drt.optimizer;

import jakarta.annotation.Nullable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.schedule.DrtCapacityChangeTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.DvrpLoadType;

import java.util.Optional;
import java.util.stream.DoubleStream;

/**
 * @author nkuehnel / MOIA
 */
public class StopWaypointImpl implements StopWaypoint {

    private final DrtStopTask task;
    private final double latestArrivalTime;// relating to max passenger drive time (for dropoff requests)
    private final double earliestArrivalTime;
    private final double latestDepartureTime;// relating to passenger max wait time (for pickup requests)
    private final DvrpLoad outgoingOccupancy;
    private final DvrpLoad emptyLoad;
    private final boolean scheduleWaitBeforeDrive;

    @Nullable
    private final DvrpLoad changedCapacity;

    public StopWaypointImpl(DrtStopTask task, DvrpLoad outgoingOccupancy, DvrpLoadType loadType, boolean scheduleWaitBeforeDrive) {
        this.task = task;
        this.outgoingOccupancy = outgoingOccupancy;
        this.emptyLoad = loadType.getEmptyLoad();
        this.scheduleWaitBeforeDrive = scheduleWaitBeforeDrive;
        this.changedCapacity = null;

        // essentially the min of the latest possible arrival times at this stop
        latestArrivalTime = calcLatestArrivalTime();

        // essentially the min of the earliest arrival times at this stop
        earliestArrivalTime = calcEarliestArrivalTime();

        // essentially the min of the latest possible pickup times at this stop
        latestDepartureTime = calcLatestDepartureTime();

    }

    public StopWaypointImpl(DrtStopTask task, double latestArrivalTime, double latestDepartureTime, DvrpLoad outgoingOccupancy,
                            DvrpLoadType loadType) {
        this.task = task;
        this.latestArrivalTime = latestArrivalTime;
        this.latestDepartureTime = latestDepartureTime;
        this.outgoingOccupancy = outgoingOccupancy;
        this.emptyLoad = loadType.getEmptyLoad();
        this.scheduleWaitBeforeDrive = false;
        this.changedCapacity = null;
        this.earliestArrivalTime = getArrivalTime();
    }

    public StopWaypointImpl(DrtCapacityChangeTask task, DvrpLoadType loadType) {
        this.task = task;
        this.latestArrivalTime = task.getBeginTime();
        this.latestDepartureTime = task.getEndTime();
        this.outgoingOccupancy = loadType.getEmptyLoad();
        this.emptyLoad = loadType.getEmptyLoad();
        this.changedCapacity = task.getChangedCapacity();
        this.scheduleWaitBeforeDrive = false;
        this.earliestArrivalTime = getArrivalTime();
    }

    @Override
    public Link getLink() {
        return task.getLink();
    }

    @Override
    public double getArrivalTime() {
        return task.getBeginTime();
    }

    @Override
    public double getDepartureTime() {
        return task.getEndTime();
    }

    @Override
    public DvrpLoad getOutgoingOccupancy() {
        return outgoingOccupancy;
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
        return task;
    }

    @Override
    public DvrpLoad getOccupancyChange() {
        DvrpLoad pickedUp = task.getPickupRequests().values().stream().map(AcceptedDrtRequest::getLoad).reduce(DvrpLoad::add).orElse(emptyLoad);
        DvrpLoad droppedOff = task.getDropoffRequests().values().stream().map(AcceptedDrtRequest::getLoad).reduce(DvrpLoad::add).orElse(emptyLoad);
        return pickedUp.subtract(droppedOff);
    }

    @Override
    public Optional<DvrpLoad> getChangedCapacity() {
        return Optional.ofNullable(changedCapacity);
    }

    @Override
    public boolean scheduleWaitBeforeDrive() {
        return scheduleWaitBeforeDrive;
    }

    private double calcLatestArrivalTime() {
        return getMaxTimeConstraint(
                task.getDropoffRequests().values().stream().mapToDouble(request -> request.getLatestArrivalTime() - request.getDropoffDuration()),
                task.getBeginTime());
    }

    private double calcEarliestArrivalTime() {

        if(getChangedCapacity().isPresent()) {
            return task.getBeginTime();
        }

        return task.getPickupRequests().values()
                .stream()
                .mapToDouble(AcceptedDrtRequest::getEarliestStartTime)
                .min()
                .orElse(0);
    }

    private double calcLatestDepartureTime() {
        return getMaxTimeConstraint(
                task.getPickupRequests().values().stream().mapToDouble(AcceptedDrtRequest::getLatestStartTime),
                task.getEndTime());
    }

    private double getMaxTimeConstraint(DoubleStream latestAllowedTimes, double scheduledTime) {
        //XXX if task is already delayed beyond one or more of latestTimes, use scheduledTime as maxTime constraint
        //thus we can still add a new request to the already scheduled stops (as no further delays are incurred)
        //but we cannot add a new stop before the delayed task
        return Math.max(latestAllowedTimes.min().orElse(Double.MAX_VALUE), scheduledTime);
    }

    @Override
    public String toString() {
        return "VehicleData.Stop for: " + task.toString();
    }
}
