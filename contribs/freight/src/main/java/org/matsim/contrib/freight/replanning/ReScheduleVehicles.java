package org.matsim.contrib.freight.replanning;

import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierFactory;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.vrp.VRPSolver;
import org.matsim.contrib.freight.vrp.VRPSolverFactoryImpl;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateStandardAlgorithmFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgentFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgentFactoryFinder;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.TourCost;
import org.matsim.contrib.freight.vrp.basics.VRPSchema;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
import org.matsim.contrib.freight.vrp.basics.VrpType;

public class ReScheduleVehicles implements CarrierPlanStrategyModule{

	private VehicleRoutingCosts costs;
	
	private TourCost tourCost;

	private Network network;
	
	public ReScheduleVehicles(Network network, VehicleRoutingCosts costs, TourCost tourCost) {
		super();
		this.costs = costs;
		this.tourCost = tourCost;
		this.network = network;
	}

	@Override
	public void handleActor(Carrier carrier) {	
		ServiceProviderAgentFactory spFactory = new ServiceProviderAgentFactoryFinder(tourCost,costs).getFactory(VRPSchema.SINGLEDEPOT_DISTRIBUTION_TIMEWINDOWS);
		VRPSolver vrpSolver = new VRPSolverFactoryImpl(new RuinAndRecreateStandardAlgorithmFactory(spFactory), VrpType.SINGLE_DEPOT_DISTRIBUTION).createSolver(new CarrierFactory().getShipments(carrier.getContracts()), 
				new CarrierFactory().getVehicles(carrier.getCarrierCapabilities()), network, tourCost, costs);
		Collection<ScheduledTour> scheduledTours = vrpSolver.solve();
		carrier.setSelectedPlan(new CarrierPlan(scheduledTours));
	}

}
