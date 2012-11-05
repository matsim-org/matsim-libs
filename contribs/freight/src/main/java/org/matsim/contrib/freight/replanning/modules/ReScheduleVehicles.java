package org.matsim.contrib.freight.replanning.modules;

import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.replanning.CarrierReplanningStrategyModule;
import org.matsim.contrib.freight.vrp.MatsimVrpSolver;
import org.matsim.contrib.freight.vrp.MatsimVrpSolverFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.TourCost;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;

public class ReScheduleVehicles implements CarrierReplanningStrategyModule {

	private Network network;

	private MatsimVrpSolverFactory vrpSolverFactory;

	private VehicleRoutingCosts costs;

	private TourCost tourCost;

	public ReScheduleVehicles(Network network, TourCost tourCost, VehicleRoutingCosts costs, MatsimVrpSolverFactory vrpSolverFactory) {
		super();
		this.network = network;
		this.costs = costs;
		this.vrpSolverFactory = vrpSolverFactory;
		this.tourCost = tourCost;
	}

	@Override
	public void handlePlan(CarrierPlan carrierPlan) {
		assert carrierPlan != null : "cannot handle plan. plan is null";
		Carrier carrier = carrierPlan.getCarrier();
		MatsimVrpSolver solver = vrpSolverFactory.createSolver(carrier, network, tourCost, costs);
		Collection<ScheduledTour> tours = solver.solve();
		carrierPlan.getScheduledTours().clear();
		carrierPlan.getScheduledTours().addAll(tours);
		carrierPlan.setScore(null);
	}

}
