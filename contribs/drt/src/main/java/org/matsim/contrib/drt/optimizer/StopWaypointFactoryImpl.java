package org.matsim.contrib.drt.optimizer;

import org.matsim.contrib.drt.schedule.DrtCapacityChangeTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.DvrpLoadType;

/**
 * @author nkuehnel / MOIA
 */
public class StopWaypointFactoryImpl implements StopWaypointFactory {

    private final DvrpLoadType loadType;

    public StopWaypointFactoryImpl(DvrpLoadType loadType) {
        this.loadType = loadType;
    }

    @Override
    public StopWaypoint createStopWaypoint(DrtStopTask task, DvrpLoad outgoingOccupancy) {
        if(task instanceof DrtCapacityChangeTask capacityChangeTask) {
            assert outgoingOccupancy.isEmpty() : "occupancy SHOULD be empty at this point.";
            return new StopWaypointImpl(capacityChangeTask, loadType);
        } else {
            return new StopWaypointImpl(task, outgoingOccupancy, loadType);
        }
    }
}
