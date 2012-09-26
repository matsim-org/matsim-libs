package freight;

import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierFactory;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.vrp.MatsimVrpSolver;
import org.matsim.contrib.freight.vrp.MatsimVrpSolverFactoryImpl;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateStandardAlgorithmFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgentFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgentFactoryFinder;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.TourCost;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemType;

public class ReScheduleVehicles implements CarrierPlanStrategyModule {

	private VehicleRoutingCosts costs;

	private TourCost tourCost;

	private Network network;

	public ReScheduleVehicles(Network network, VehicleRoutingCosts costs,
			TourCost tourCost) {
		super();
		this.costs = costs;
		this.tourCost = tourCost;
		this.network = network;
	}

	@Override
	public void handleCarrier(Carrier carrier) {
		ServiceProviderAgentFactory spFactory = new ServiceProviderAgentFactoryFinder(tourCost, costs).getFactory(VehicleRoutingProblemType.CVRPTW);
		MatsimVrpSolver vrpSolver = new MatsimVrpSolverFactoryImpl(
				new RuinAndRecreateStandardAlgorithmFactory(spFactory),
				VehicleRoutingProblemType.CVRPTW).createSolver(carrier
				.getShipments(), new CarrierFactory().getVehicles(carrier
				.getCarrierCapabilities()), network, tourCost, costs);
		Collection<ScheduledTour> scheduledTours = vrpSolver.solve();
		carrier.setSelectedPlan(new CarrierPlan(scheduledTours));
	}

}
