package org.matsim.core.mobsim.dsim;

@FunctionalInterface
public interface NotifyVehicleEntersPartition {

	void onVehicleEntersPartition(DistributedMobsimVehicle vehicle);
}
