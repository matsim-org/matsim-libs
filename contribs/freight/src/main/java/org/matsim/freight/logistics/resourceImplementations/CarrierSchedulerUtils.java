package org.matsim.freight.logistics.resourceImplementations;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.roadpricing.RoadPricingScheme;
import org.matsim.contrib.roadpricing.RoadPricingUtils;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierPlan;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.jsprit.MatsimJspritFactory;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts;
import org.matsim.freight.carriers.jsprit.NetworkRouter;
import org.matsim.freight.logistics.LSPUtils;

/**
 * This class contains some code fragments, that are used in the different *CarrierScheduler
 * classes. To avoid code duplication these methods are extracted and located here more centralized.
 *
 * @author Kai Martins-Turner (kturner)
 */
public class CarrierSchedulerUtils {
  private static final Logger log = LogManager.getLogger(CarrierSchedulerUtils.class);
  private static final String LOGIC_OF_VRP = "logicOfVrp";

  /**
   * Creates a VehicleRoutingProblem from a carrier and a network and solves it with Jsprit.
   * If a roadPricingScheme is given, the tolls are considered in the routing costs.
   * <p>
   * This looks for me (KMT) similar to what is done in {@link org.matsim.freight.carriers.CarriersUtils#runJsprit(Scenario)}.
   * So, maybe this can be more simplify.
   *
   * @param carrier  Carrier for which the problem should be solved
   * @param scenario the scenario
   * @return Carrier  with the solution of the VehicleRoutingProblem and the routed plan.
   */
  public static Carrier solveVrpWithJsprit(Carrier carrier, Scenario scenario) {
    // Maybe it make sense to store this object instead of rebuilding it for each carrier (in each iteration) ???
    // pro: save computation time
    // con: interdependencies, if something changes in the network (load), the object is not up-to-date & it is not clear, if the object is thread safe
    // Decision for the time being: rebuild it for each carrier to have a clear state KMT/KN Aug'24
    NetworkBasedTransportCosts netbasedTransportCosts;
    Network network = scenario.getNetwork();
    RoadPricingScheme roadPricingScheme = null;
    try {
      roadPricingScheme = RoadPricingUtils.getRoadPricingScheme(scenario);
    } catch (Exception e) {
      log.info("Was not able getting RoadPricingScheme. Tolls cannot be considered.", e);
    }
    if (roadPricingScheme != null) {
      netbasedTransportCosts = NetworkBasedTransportCosts.Builder.newInstance(network, ResourceImplementationUtils.getVehicleTypeCollection(carrier))
              .setRoadPricingScheme(roadPricingScheme)
              .build();
    } else {
      log.debug("RoadPricingScheme is null. Tolls cannot be considered.");
      netbasedTransportCosts = NetworkBasedTransportCosts.Builder.newInstance(network, ResourceImplementationUtils.getVehicleTypeCollection(carrier))
              .build();
    }

    VehicleRoutingProblem vrp =
            MatsimJspritFactory.createRoutingProblemBuilder(carrier, network)
                    .setRoutingCost(netbasedTransportCosts)
                    .build();

    //If jspritIterations are not set (get.... returns a negativ value), set it to 1
    int jspritIterations;
    if (CarriersUtils.getJspritIterations(carrier) >= 1) {
      jspritIterations = CarriersUtils.getJspritIterations(carrier);
    } else  {
      log.info("Jsprit iterations are not set (properly) for carrier {}. Set to 1.", carrier.getId());
      jspritIterations = 1;
    }

    VehicleRoutingAlgorithm vra = Jsprit.createAlgorithm(vrp);
    vra.setMaxIterations(jspritIterations);
    VehicleRoutingProblemSolution solution = Solutions.bestOf(vra.searchSolutions());

    CarrierPlan plan = MatsimJspritFactory.createPlan(carrier, solution);
    NetworkRouter.routePlan(plan, netbasedTransportCosts);
    carrier.addPlan(plan);
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

  /**
   * Sum up the jsprit score of the given list of CarrierPlans.
   * As a consequence this is not from the one and only jsprit run, but from all jsprit runs af the different auxiliary carriers.
   * @param scheduledPlans the scheduled plans with the jsprit results
   * @return the summ of the scores coming from jsprit
   */
  public static Double sumUpJspritScore(List<CarrierPlan> scheduledPlans) {
    double jspritScore = 0;
    for (CarrierPlan scheduledPlan : scheduledPlans) {
      if (scheduledPlan.getJspritScore() != null) {
        jspritScore = jspritScore + scheduledPlan.getJspritScore();      }
    }
    return jspritScore;
  }

  /**
   * Setter for the internal solving logic of a VRP.
   * This decides later, whether the VRP is build base on {@link org.matsim.freight.carriers.CarrierService}s or {@link org.matsim.freight.carriers.CarrierShipment}s.
   *
   * @param carrier The carrier for which the setting should be set.
   * @param logicOfVrp the logic of the VRP
   */
  public static void setVrpLogic(Carrier carrier, LSPUtils.LogicOfVrp logicOfVrp){
    carrier.getAttributes().putAttribute(LOGIC_OF_VRP, logicOfVrp);
  }

    /**
     * Getter for the internal solving logic of a VRP.
     * This decides later, whether the VRP is build base on {@link org.matsim.freight.carriers.CarrierService}s or {@link org.matsim.freight.carriers.CarrierShipment}s.
     *
     * @param carrier The carrier for which the setting should be got.
     * @return the logic of the VRP, returns {@link LSPUtils.LogicOfVrp#serviceBased} if not set.
     */
  public static LSPUtils.LogicOfVrp getVrpLogic(Carrier carrier){
    LSPUtils.LogicOfVrp result = (LSPUtils.LogicOfVrp) carrier.getAttributes().getAttribute(LOGIC_OF_VRP);
    if (result == null){
      log.error("VRPLogic not found for carrier {}. Will return {}", carrier.getId(), LSPUtils.LogicOfVrp.serviceBased);
      return LSPUtils.LogicOfVrp.serviceBased;
    } else {
      return result ;
    }
  }

}
