package org.matsim.core.mobsim.qsim.qnetsimengine.vehicle_handler;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

/**
 * This interface provides functionality to decide what happens if a vehicle
 * interacts with a link in the network simulation.
 * 
 * @author Sebastian Hörl <sebastian.hoerl@ivt.baug.ethz.ch>
 */
public interface VehicleHandler {
	/**
	 * Called when a vehicle departs in the network simulation.
	 */
	void handleVehicleDeparture(QVehicle vehicle, Link link);

	/**
	 * Called when a vehicle wants to arrive at a certain link the network
	 * simulation.
	 * 
	 * @return Return value defines whether vehicle is currently allowed to arrive
	 *         at the link. If not, the link will block until the next query or
	 *         until the agent changes its plan dynamically.
	 */
	boolean handleVehicleArrival(QVehicle vehicle, Link link);

	/**
	 * Called when a vehicle is initially placed on a certain link.
	 */
	void handleInitialVehicleArrival(QVehicle vehicle, Link link);
}
