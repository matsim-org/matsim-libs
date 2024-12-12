package org.matsim.core.mobsim.dsim;

import org.matsim.api.core.v01.Message;

import java.util.List;

/**
 * Container holding a vehicle and its driver and passengers.
 */
public record VehicleContainer(Class<? extends DistributedMobsimVehicle> vehicleType, Message vehicle,
							   org.matsim.core.mobsim.dsim.VehicleContainer.Occupant driver, List<Occupant> passengers) implements Message {

	public record Occupant(Class<? extends DistributedMobsimAgent> type, Message occupant) {
		public Occupant(DistributedMobsimAgent agent) {
			this(agent.getClass(), agent.toMessage());
		}
	}
}
