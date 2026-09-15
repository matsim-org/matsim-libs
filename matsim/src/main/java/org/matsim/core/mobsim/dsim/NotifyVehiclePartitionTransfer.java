package org.matsim.core.mobsim.dsim;

public interface NotifyVehiclePartitionTransfer {

	void onVehicleLeavesPartition(DistributedMobsimVehicle vehicle, int toPartition);

	void onVehicleEntersPartition(DistributedMobsimVehicle vehicle);
}
