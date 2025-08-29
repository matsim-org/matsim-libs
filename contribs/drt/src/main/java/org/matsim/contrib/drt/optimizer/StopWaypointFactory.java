package org.matsim.contrib.drt.optimizer;

import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.load.DvrpLoad;

/**
 * @author nkuehnel / MOIA
 */
public interface StopWaypointFactory {

    StopWaypoint createStopWaypoint(DrtStopTask task, DvrpLoad outgoingOccupancy);
}
