package org.matsim.freight.logistics.resourceImplementations;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierPlan;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.jsprit.MatsimJspritFactory;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts;
import org.matsim.freight.carriers.jsprit.NetworkRouter;
import org.matsim.freight.carriers.jsprit.VehicleTypeDependentRoadPricingCalculator;

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
   *
   * @param carrier               Carrier for which the problem should be solved
   * @param network               the underlying network to create the network based transport costs
   * @return Carrier  with the solution of the VehicleRoutingProblem and the routed plan.
   * @Todo: include toll in the NetbasedCosts (if set), so it is also pat of the VRP
   * @Todo: Find a way to reuse the netbasedCosts over the iterations(?) to avoid re-setting this up???
   * <li> Pro: saves computation times,
   * <li> Con: There is now update of the costs if the network (load) changes.
   * <li> --> do it at least per Carrier or generally or stay as it is? --> Discuss with KN
   * @Todo: Make the number of jsprit-Iterations configurable
   */
  public static Carrier solveVrpWithJsprit(Carrier carrier, Network network) {
    NetworkBasedTransportCosts netbasedTransportCosts =
            NetworkBasedTransportCosts.Builder.newInstance(
                            network, ResourceImplementationUtils.getVehicleTypeCollection(carrier))
                    .build();

    VehicleRoutingProblem vrp =
            MatsimJspritFactory.createRoutingProblemBuilder(carrier, network)
                    .setRoutingCost(netbasedTransportCosts)
                    .build();

    //Setting jspritIterations to use central infrastructure -> should go more up in the code
    CarriersUtils.setJspritIterations(carrier, 1);

    VehicleRoutingAlgorithm algorithm = Jsprit.createAlgorithm(vrp);
    algorithm.setMaxIterations(CarriersUtils.getJspritIterations(carrier));

    VehicleRoutingProblemSolution solution = Solutions.bestOf(algorithm.searchSolutions());

    CarrierPlan plan = MatsimJspritFactory.createPlan(carrier, solution);
    NetworkRouter.routePlan(plan, netbasedTransportCosts);
    carrier.setSelectedPlan(plan);
    return carrier;
  }

  /**
   * First try with tolls.
   * Rest is the same as {@link #solveVrpWithJsprit(Carrier, Network)}.
   * @param carrier  Carrier for which the problem should be solved
   * @param network  the underlying network to create the network based transport costs
   * @param roadPricingCalculator the road pricing calculator to calculate the tolls
   * @return Carrier  with the solution of the VehicleRoutingProblem and the routed plan.
   */
  public static Carrier solveVrpWithJspritWithToll(Carrier carrier, Network network, VehicleTypeDependentRoadPricingCalculator roadPricingCalculator) {
    if (roadPricingCalculator != null) {
      NetworkBasedTransportCosts netbasedTransportCosts =
          NetworkBasedTransportCosts.Builder.newInstance(
                  network, ResourceImplementationUtils.getVehicleTypeCollection(carrier))
              .setRoadPricingCalculator(roadPricingCalculator)
              .build();

      VehicleRoutingProblem vrp =
          MatsimJspritFactory.createRoutingProblemBuilder(carrier, network)
                  .setRoutingCost(netbasedTransportCosts)
                  .build();

      //Setting jspritIterations to use central infrastructure -> should go more up in the code
      CarriersUtils.setJspritIterations(carrier, 1);

      VehicleRoutingAlgorithm vra = Jsprit.createAlgorithm(vrp);
      vra.setMaxIterations(CarriersUtils.getJspritIterations(carrier));
      VehicleRoutingProblemSolution solution = Solutions.bestOf(vra.searchSolutions());

      CarrierPlan plan = MatsimJspritFactory.createPlan(carrier, solution);
      NetworkRouter.routePlan(plan, netbasedTransportCosts);
      carrier.setSelectedPlan(plan);
      return carrier;

    } else { //no Toll -> goto previous implementation without toll
        return solveVrpWithJsprit(carrier, network);
    }

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
