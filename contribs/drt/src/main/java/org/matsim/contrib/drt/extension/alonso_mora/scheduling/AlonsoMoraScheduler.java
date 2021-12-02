package org.matsim.contrib.drt.extension.alonso_mora.scheduling;

import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraVehicle;

/**
 * This interface covers the implementation of a stop sequence into a DVRP
 * schedule.
 * 
 * @author sebhoerl
 */
public interface AlonsoMoraScheduler {
	/**
	 * Takes the stop sequence (route) of the vehicle and assigns an updated DVRP
	 * schedule. Furthermore, the assignment of pickup and dropoff tasks to the
	 * requests should be performed.
	 */
	void schedule(AlonsoMoraVehicle vehicle, double now);
}
