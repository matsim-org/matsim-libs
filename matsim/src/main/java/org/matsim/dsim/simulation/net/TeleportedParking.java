package org.matsim.dsim.simulation.net;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

/**
 * Parked vehicles logic for config:dsim.vehicleBehavior=teleport. The logic for parked vehicles diverts from that implemented in the
 * Qsim.
 * <p>
 * TeleportedParking does not keep track of vehicles, but hands out vehicle references. This means there is no guarantee that a certain
 * vehicle is only used once. Use config:dsim.vehicleBehavior=exception to ensure this behavior
 */
class TeleportedParking implements ParkedVehicles {

	private final Map<Id<Vehicle>, DistributedMobsimVehicle> vehicles = new HashMap<>();

	@Override
	public DistributedMobsimVehicle unpark(Id<Vehicle> vehicleId, Id<Link> linkId) {

		var vehicle = vehicles.get(vehicleId);
		assert vehicle != null : "Could not find vehicle " + vehicleId + " vehicles must be added to the simulation on prepareSim";
		return vehicle;
	}

	@Override
	public void park(DistributedMobsimVehicle vehicle, SimLink link) {
		vehicles.put(vehicle.getId(), vehicle);
	}
}
