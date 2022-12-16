package org.matsim.contrib.drt.optimizer;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

public interface SlackTimeCalculator {

	double[] computeSlackTimes(DvrpVehicle vehicle, double now, Waypoint.Stop[] stops);
}
