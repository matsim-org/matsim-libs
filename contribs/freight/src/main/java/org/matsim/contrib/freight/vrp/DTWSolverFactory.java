package org.matsim.contrib.freight.vrp;

/**
 * Configures solver for solving the SINGLE DEPOT DISTRIBUTION/DELIVERY vrp problem.
 */

import java.util.Collection;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateStandardAlgorithmFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgentFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgentFactoryFinder;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.TourCost;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemType;
import org.matsim.core.gbl.MatsimRandom;

public class DTWSolverFactory implements VRPSolverFactory{

	private Random random = MatsimRandom.getRandom();

	public void setRandom(Random random) {
		this.random = random;
	}

	@Override
	public VRPSolver createSolver(Collection<CarrierShipment> shipments, Collection<CarrierVehicle> carrierVehicles, Network network, TourCost tourCost, VehicleRoutingCosts costs) {
		verifyDistributionProblem(shipments,carrierVehicles);
		ServiceProviderAgentFactory spFactory = new ServiceProviderAgentFactoryFinder(tourCost,costs).getFactory(VehicleRoutingProblemType.CVRPTW);
		MatsimVrpSolver rrSolver = new MatsimVrpSolver(shipments, carrierVehicles, costs);
		RuinAndRecreateStandardAlgorithmFactory ruinAndRecreateFactory = new RuinAndRecreateStandardAlgorithmFactory(spFactory);
		rrSolver.setRuinAndRecreateFactory(ruinAndRecreateFactory);
		return rrSolver;
	}

	private void verifyDistributionProblem(Collection<CarrierShipment> shipments, Collection<CarrierVehicle> carrierVehicles) {
		Id location = null;
		for(CarrierVehicle v : carrierVehicles){
			if(location == null){
				location = v.getLocation();
			}
			else if(!location.toString().equals(v.getLocation().toString())){
				throw new IllegalStateException("if you use this solver " + this.getClass().toString() + "), all vehicles must have the same depot-location. vehicle " + v.getVehicleId() + " has not.");
			}
		}
		for(CarrierShipment s : shipments){
			if(location == null){
				return;
			}
			if(!s.getFrom().toString().equals(location.toString())){
				throw new IllegalStateException("if you use this solver, all shipments must have the same from-location. errorShipment " + s);
			}
		}
		
	}

}
