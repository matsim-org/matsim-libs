package org.matsim.freight.logistics.resourceImplementations;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierPlan;
import org.matsim.freight.carriers.jsprit.MatsimJspritFactory;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts;
import org.matsim.freight.carriers.jsprit.NetworkRouter;

/**
 * This class contains some code fragments, that are used in the different *CarrierScheduler
 * classes. To avoid code duplication these methods are extracted and located here more centralized.
 *
 * @author Kai Martins-Turner (kturner)
 */
public class CarrierSchedulerUtils {

  /**
   * Creates a VehicleRoutingProblem from a carrier and a network and solves it with Jsprit.
   * <p>
   * This looks for me (KMT) similar to what is done in {@link org.matsim.freight.carriers.CarriersUtils#runJsprit(Scenario)}.
   * So, maybe this can be more simplify.
   * <p>
   *  @Todo: include toll in the NetbasedCosts (if set), so it is also pat of the VRP
   *  @Todo: Find a way to reuse the netbasedCosts over the iterations(?) to avoid re-setting this up???
   *    <li> Pro: saves computation times,
   *    <li> Con: There is now update of the costs if the network (load) changes.
   *    <li> --> do it at least per Carrier or generally or stay as it is? --> Discuss with KN
   *  @Todo: Make the number of jsprit-Iterations configurable
   *
   * @param carrier  Carrier for which the problem should be solved
   * @param network  the underlying network to create the network based transport costs
   * @return Carrier  with the solution of the VehicleRoutingProblem and the routed plan.
   */
  public static Carrier solveVrpWithJsprit(Carrier carrier, Network network) {
    VehicleRoutingProblem.Builder vrpBuilder =
            MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);
    NetworkBasedTransportCosts.Builder tpcostsBuilder =
            NetworkBasedTransportCosts.Builder.newInstance(
                    network, ResourceImplementationUtils.getVehicleTypeCollection(carrier));
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
