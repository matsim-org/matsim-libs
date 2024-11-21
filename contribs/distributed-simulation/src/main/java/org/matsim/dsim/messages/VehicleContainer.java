package org.matsim.dsim.messages;

import lombok.Builder;
import lombok.Data;
import org.matsim.api.core.v01.Message;
import org.matsim.core.mobsim.framework.DistributedMobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.DistributedMobsimVehicle;

import java.util.List;

/**
 * Container holding a vehicle and its driver and passengers.
 */
@Builder(setterPrefix = "set")
@Data
public class VehicleContainer implements Message {

	private final Class<? extends DistributedMobsimVehicle> vehicleType;
	private final Message vehicle;

	private final Occupant driver;
	private final List<Occupant> passengers;

	public record Occupant(Class<? extends DistributedMobsimAgent> type, Message occupant ) {
		public Occupant(DistributedMobsimAgent agent) {
			this(agent.getClass(), agent.toMessage());
		}
	}

}
