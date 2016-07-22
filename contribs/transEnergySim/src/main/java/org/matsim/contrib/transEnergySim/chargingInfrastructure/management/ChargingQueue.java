package org.matsim.contrib.transEnergySim.chargingInfrastructure.management;

import org.matsim.contrib.transEnergySim.agents.VehicleAgent;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary.ChargingPlugType;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;

/*
 * A ChargingQueue manages a line of vehicles constrained by physical access to plugs which are tracked by type.
 * 
 * E.g. if a single charging point has 3 plus, 2 of level 2 and 1 of level 1, this class would be used to manage
 * the total number of vehicles with access to the charging point, but as vehicle are added to the queue, they
 * are also mapped to a particular plug type and are therefore tracked internally as separate queues, whose combined
 * size cannot exceed the maximum queue size.
 */
public interface ChargingQueue {
	
	// Returns true if vehicle successfully added to queue and false if queue is full
	boolean isPhysicalSiteFull();
	VehicleAgent dequeueVehicleFromChargingQueue(ChargingPlugType plugType);
	VehicleAgent peekAtChargingQueue(ChargingPlugType chargingPlugType);
	int getNumAtPhysicalSite();
	int maxSizeOfPhysicalSite();
	boolean removeVehicleFromChargingQueue(VehicleAgent vehicle);
	void removeVehicleFromPhysicalSite(VehicleAgent vehicle);
	boolean addVehicleToPhysicalSite();
	boolean addVehicleToChargingQueue(ChargingPlugType plugType, VehicleAgent vehicle);
	int getNumInChargingQueue(ChargingPlugType chargingPlugType);

}
