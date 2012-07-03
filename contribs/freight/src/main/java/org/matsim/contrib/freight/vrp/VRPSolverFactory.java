package org.matsim.contrib.freight.vrp;

import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.TourCost;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;

public interface VRPSolverFactory {
	
	public abstract VRPSolver createSolver(Collection<CarrierShipment> shipments, Collection<CarrierVehicle> carrierVehicles, Network network, TourCost tourCost, VehicleRoutingCosts costs);

}
