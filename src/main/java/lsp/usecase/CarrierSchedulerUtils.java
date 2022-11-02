package lsp.usecase;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;

import java.util.Collection;
import java.util.List;

/**
 * This class contains some code fragments, that are used in the different *CarrierScheduler classes.
 * To avoid code duplication these methods are extracted and located here more centralized.
 *
 * @author Kai Martins-Turner (kturner)
 */
/*package-private*/ class CarrierSchedulerUtils {
	static Carrier routeCarrier(Carrier carrier, Network network) {
		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);
		NetworkBasedTransportCosts.Builder tpcostsBuilder = NetworkBasedTransportCosts.Builder.newInstance(network, UsecaseUtils.getVehicleTypeCollection(carrier));
		NetworkBasedTransportCosts netbasedTransportcosts = tpcostsBuilder.build();
		vrpBuilder.setRoutingCost(netbasedTransportcosts);
		VehicleRoutingProblem vrp = vrpBuilder.build();

		VehicleRoutingAlgorithm algorithm = Jsprit.createAlgorithm(vrp);
		algorithm.setMaxIterations(1);
		Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();

		VehicleRoutingProblemSolution solution = Solutions.bestOf(solutions);

		CarrierPlan plan = MatsimJspritFactory.createPlan(carrier, solution);
		NetworkRouter.routePlan(plan, netbasedTransportcosts);
		carrier.setSelectedPlan(plan);
		return carrier;
	}

	static Double sumUpScore(List<CarrierPlan> scheduledPlans) {
		double score = 0;
		for (CarrierPlan scheduledPlan : scheduledPlans) {
			if (scheduledPlan.getScore() != null) {
				score = score + scheduledPlan.getScore();
			}
		}
		return score;
	}
}
