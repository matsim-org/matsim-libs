package org.matsim.contrib.drt.optimizer;

import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.load.DvrpLoad;

/**
 * @author nkuehnel / MOIA
 */
public interface StopWaypoint extends Waypoint {

    double getLatestArrivalTime();

    double getLatestDepartureTime();

    double getEarliestArrivalTime();

    DrtStopTask getTask();

    DvrpLoad getOccupancyChange();
    DvrpLoad getChangedCapacity();

    boolean scheduleWaitBeforeDrive();
}
