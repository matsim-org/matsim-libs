package org.matsim.freight.logistics.resourceImplementations;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import org.matsim.api.core.v01.network.Network;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierPlan;
import org.matsim.freight.carriers.jsprit.MatsimJspritFactory;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts;
import org.matsim.freight.carriers.jsprit.NetworkRouter;

import java.util.Collection;
import java.util.List;

/**
 * This class contains some code fragments, that are used in the different *CarrierScheduler classes.
 * To avoid code duplication these methods are extracted and located here more centralized.
 *
 * @author Kai Martins-Turner (kturner)
 */
public class CarrierSchedulerUtils {
	public static Carrier routeCarrier(Carrier carrier, Network network) {
		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);
		NetworkBasedTransportCosts.Builder tpcostsBuilder = NetworkBasedTransportCosts.Builder.newInstance(network, ResourceImplementationUtils.getVehicleTypeCollection(carrier));
		NetworkBasedTransportCosts netbasedTransportCosts = tpcostsBuilder.build();
		vrpBuilder.setRoutingCost(netbasedTransportCosts);
		VehicleRoutingProblem vrp = vrpBuilder.build();

		VehicleRoutingAlgorithm algorithm = Jsprit.createAlgorithm(vrp);
		algorithm.setMaxIterations(1);
		Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();

		VehicleRoutingProblemSolution solution = Solutions.bestOf(solutions);

		CarrierPlan plan = MatsimJspritFactory.createPlan(carrier, solution);
		NetworkRouter.routePlan(plan, netbasedTransportCosts);
		carrier.setSelectedPlan(plan);
		return carrier;
	}

	public static Double sumUpScore(List<CarrierPlan> scheduledPlans) {
		double score = 0;
		for (CarrierPlan scheduledPlan : scheduledPlans) {
			if (scheduledPlan.getScore() != null) {
				score = score + scheduledPlan.getScore();
			}
		}
		return score;
	}
}
