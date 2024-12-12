package org.matsim.core.mobsim.dsim;


import org.matsim.api.core.v01.Message;

import java.util.ArrayList;
import java.util.List;

public record SimStepMessage(double simstep, List<CapacityUpdate> capUpdates, List<Teleportation> teleportations,
							 List<VehicleContainer> vehicles) implements Message {

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private double simstep;
		private List<CapacityUpdate> capUpdates = new ArrayList<>();
		private List<Teleportation> teleportations = new ArrayList<>();
		private List<VehicleContainer> vehicles = new ArrayList<>();

		public Builder setSimstep(double simstep) {
			this.simstep = simstep;
			return this;
		}

		public Builder addCapacityUpdate(CapacityUpdate capUpdate) {
			capUpdates.add(capUpdate);
			return this;
		}

		public Builder addTeleportation(Teleportation teleportation) {
			teleportations.add(teleportation);
			return this;
		}

		public Builder addVehicleContainer(VehicleContainer vehicle) {
			vehicles.add(vehicle);
			return this;
		}

		public SimStepMessage build() {
			return new SimStepMessage(simstep, capUpdates, teleportations, vehicles);
		}

		public void clear() {
			// Need to create new lists, because the current ones are passed as references to other partitions
			capUpdates = new ArrayList<>();
			teleportations = new ArrayList<>();
			vehicles = new ArrayList<>();
			simstep = 0;
		}
	}

}
