package org.matsim.contrib.drt.optimizer;

import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.core.mobsim.dsim.NodeSingleton;

/**
 * @author nkuehnel / MOIA
 */
@NodeSingleton
public interface StopWaypointFactory {

    StopWaypoint createStopWaypoint(DrtStopTask task, DvrpLoad outgoingOccupancy);
}
