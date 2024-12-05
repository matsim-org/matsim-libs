package org.matsim.dsim.simulation.net;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleFactory;
import org.matsim.vehicles.Vehicle;

/**
 * Parged vehicles logic for config:qsim.vehicleBehavior=teleport. The logic for parked vehicles diverts from that implemented in the
 * Qsim.
 * <p>
 * TeleportedParking does not keep track of vehicles, but generates new vehicles on the spot. This means there is no guarantee that a certain
 * vehicle is only used once. Use config:qsim.vehicleBehavior=exception to ensure this behavior
 */
class TeleportedParking implements ParkedVehicles {

	private final QVehicleFactory vehicleFactory;
	private final Scenario scenario;

	@Inject
	public TeleportedParking(QVehicleFactory vehicleFactory, Scenario scenario) {
		this.vehicleFactory = vehicleFactory;
		this.scenario = scenario;
	}

	@Override
	public DistributedMobsimVehicle unpark(Id<Vehicle> vehicleId, Id<Link> linkId) {

		var vehicle = scenario.getVehicles().getVehicles().get(vehicleId);
		var simVehicle = vehicleFactory.createQVehicle(vehicle);

		assert simVehicle instanceof DistributedMobsimVehicle :
			"Vehicle Factory must return an instance of 'DistributedMobsimVehicle' if used with 'DSim'.";

		return (DistributedMobsimVehicle) simVehicle;
	}

	@Override
	public void park(DistributedMobsimVehicle vehicle, SimLink link) {
		// do nothing
	}
}
