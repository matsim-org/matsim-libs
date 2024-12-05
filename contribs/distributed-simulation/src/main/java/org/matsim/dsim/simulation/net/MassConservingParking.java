package org.matsim.dsim.simulation.net;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles parked vehicles according to config:qsim.vehicleBehavio=exception. This Parking implementation stores vehicles parked at links. When
 * a vehicle is requested to be unparked, it will throw an exception if the vehicle has not been parked on that link.
 */
class MassConservingParking implements ParkedVehicles {

	private final Map<Id<Link>, Map<Id<Vehicle>, DistributedMobsimVehicle>> parkedVehicles = new HashMap<>();

	@Override
	public DistributedMobsimVehicle unpark(Id<Vehicle> vehicleId, Id<Link> linkId) {
		var vehiclesOnLink = parkedVehicles.get(linkId);

		if (vehiclesOnLink == null) {
			throw new RuntimeException("Vehicle: " + vehicleId + " was not parked on link: " + linkId);
		}

		var vehicle = vehiclesOnLink.remove(vehicleId);

		if (vehiclesOnLink.isEmpty()) {
			parkedVehicles.remove(linkId);
		}

		if (vehicle == null) {
			throw new RuntimeException("Vehicle: " + vehicleId + " was not parked on link: " + linkId);
		}

		return vehicle;
	}

	@Override
	public void park(DistributedMobsimVehicle vehicle, SimLink link) {
		parkedVehicles.computeIfAbsent(link.getId(), _ -> new HashMap<>())
			.put(vehicle.getId(), vehicle);
	}
}
