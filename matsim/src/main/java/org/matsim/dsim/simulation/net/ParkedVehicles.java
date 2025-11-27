package org.matsim.dsim.simulation.net;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.vehicles.Vehicle;

/**
 * Functionality to park vehicles in the Simulation. Currently, we support varying modes of how parked vehicles are handled. This is done via
 * config:dsim.vehicleBehavior. Depending on the config param different behaviors can be implemented to be bound into the {@link NetworkTrafficEngine}
 */
public interface ParkedVehicles {

	/**
	 * Get a parked vehicle to start driving on the specified linkId. The method
	 * only gets the vehicle. The call site is responsible for adding drivers to the
	 * vehicle and generating the corresponding events.
	 */
	DistributedMobsimVehicle unpark(Id<Vehicle> vehicleId, Id<Link> linkId);

	/**
	 * Park a vehicle which is not used anymore on the specified simulation link. The
	 * call site is responsible for the logic of agents leaving the vehicle and for
	 * generating the corresponding events.
	 */
	void park(DistributedMobsimVehicle vehicle, SimLink link);
}
