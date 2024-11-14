package org.matsim.dsim.messages;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.matsim.api.core.v01.Message;

import java.util.List;

@Builder(setterPrefix = "set", toBuilder = true)
@AllArgsConstructor
@Data
public class SimStepMessage implements Message {

	private final double simstep;

	@Singular
	private final List<CapacityUpdate> capacityUpdates;

	@Singular
	private final List<Teleportation> teleportationMsgs;

	@Singular
	private final List<VehicleContainer> vehicles;

	public int getCapacityUpdateMsgsCount() {
		return this.capacityUpdates.size();
	}

	public int getTeleportationMsgsCount() {
		return this.teleportationMsgs.size();
	}

	public int getVehicleMsgsCount() {
		return this.vehicles.size();
	}
}
