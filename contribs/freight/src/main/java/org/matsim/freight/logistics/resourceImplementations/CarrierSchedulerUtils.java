package org.matsim.freight.logistics.resourceImplementations;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import java.util.List;
import java.util.function.ToDoubleFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.roadpricing.RoadPricingScheme;
import org.matsim.contrib.roadpricing.RoadPricingUtils;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierPlan;
import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.TimeWindow;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.carriers.jsprit.MatsimJspritFactory;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts;
import org.matsim.freight.carriers.jsprit.NetworkRouter;
import org.matsim.freight.logistics.LSP;
import org.matsim.freight.logistics.LSPCarrierResource;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LSPUtils;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentPlanElement;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;

/**
 * Central place for helper methods that are used by the different {@link CarrierScheduler}
 * implementations, in order to avoid code duplication between them.
 *
 * @author Kai Martins-Turner (kturner)
 */
public class CarrierSchedulerUtils {
	private static final Logger log = LogManager.getLogger(CarrierSchedulerUtils.class);
	private static final String LOGIC_OF_VRP = "logicOfVrp";

	/**
	 * Creates a {@link VehicleRoutingProblem} from a carrier and a network and solves it with Jsprit.
	 * If a {@link RoadPricingScheme} is present in the scenario, the tolls are considered in the routing costs.
	 * <p>
	 * This looks for me (KMT) similar to what is done in {@link org.matsim.freight.carriers.CarriersUtils#runJsprit(Scenario)}.
	 * So, maybe this can be more simplified. (Most probably, the runJsprit method need this rpscheme - handling as well)
	 *
	 * @param carrier  Carrier for which the problem should be solved
	 * @param scenario the scenario
	 * @return Carrier  with the solution of the VehicleRoutingProblem and the routed plan.
	 */
	public static Carrier solveVrpWithJsprit(Carrier carrier, Scenario scenario) {
		// Rebuilt for each carrier (rather than cached) to keep a clear state, since the network load may change between iterations.
		// ---
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

		CarrierPlan plan = MatsimJspritFactory.createPlan(solution);
		NetworkRouter.routePlan(plan, netbasedTransportCosts);
		carrier.addPlan(plan);
		carrier.setSelectedPlan(plan);
		return carrier;
	}

	/**
	 * Sum up the score of the given list of {@link CarrierPlan}s.
	 *
	 * @param scheduledPlans the scheduled plans whose scores should be added up
	 * @return the sum of the scores coming from jsprit
	 */
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

	//*************************
	// Central methods for the different CarrierSchedulers
	//*************************

	/**
	 * Converts an {@link LspShipment} into a {@link CarrierService} with the same id. The id equality is needed to
	 * update the {@link org.matsim.freight.logistics.shipment.LspShipmentPlan} after the carrier has been scheduled.
	 *
	 * @param lspShipment                the shipment to convert
	 * @param linkId                     the link where the service takes place
	 * @param serviceStartingTimeWindow  the time window for the start of the service, or {@code null} to use the default
	 *                                   of {@link CarrierService.Builder}
	 * @param serviceDuration            the duration of the service: {@link LspShipment#getPickupServiceTime()} if the
	 *                                   shipment is loaded at the service, {@link LspShipment#getDeliveryServiceTime()} if
	 *                                   it is unloaded
	 */
	static CarrierService convertToCarrierService(LspShipment lspShipment, Id<Link> linkId, TimeWindow serviceStartingTimeWindow,
												  double serviceDuration) {
		Id<CarrierService> serviceId = Id.create(lspShipment.getId().toString(), CarrierService.class);
		CarrierService.Builder builder = CarrierService.Builder.newInstance(serviceId, linkId, lspShipment.getSize())
			.setServiceDuration(serviceDuration);
		if (serviceStartingTimeWindow != null) {
			builder.setServiceStartingTimeWindow(serviceStartingTimeWindow);
		}
		return builder.build();
	}

	/**
	 * Returns the sum of the pickup service times, i.e. the loading durations, of the {@link LspShipment}s that are served
	 * by the service activities of the given tour. The ids of the carrier services equal the ids of the LSP shipments.
	 */
	static double sumOfPickupServiceTimes(Tour tour, LSP lsp) {
		return sumOverShipmentsOfServiceActivities(tour, lsp, LspShipment::getPickupServiceTime);
	}

	/**
	 * Returns the sum of the delivery service times, i.e. the unloading durations, of the {@link LspShipment}s that are
	 * served by the service activities of the given tour. The ids of the carrier services equal the ids of the LSP
	 * shipments.
	 */
	static double sumOfDeliveryServiceTimes(Tour tour, LSP lsp) {
		return sumOverShipmentsOfServiceActivities(tour, lsp, LspShipment::getDeliveryServiceTime);
	}

	private static double sumOverShipmentsOfServiceActivities(Tour tour, LSP lsp, ToDoubleFunction<LspShipment> duration) {
		double sum = 0;
		for (Tour.TourElement element : tour.getTourElements()) {
			if (element instanceof Tour.ServiceActivity serviceActivity) {
				Id<LspShipment> lspShipmentId = Id.create(serviceActivity.getService().getId().toString(), LspShipment.class);
				LspShipment lspShipment = LSPUtils.findLspShipment(lsp, lspShipmentId);
				if (lspShipment == null) {
					throw new IllegalStateException("No LSP shipment with id " + lspShipmentId + " found in LSP " + lsp.getId() + ".");
				}
				sum += duration.applyAsDouble(lspShipment);
			}
		}
		return sum;
	}

	static void addShipmentLoadElement(LspShipment lspShipment, Tour tour, LSPCarrierResource resource, LSPPlan lspPlan) {
		LspShipmentUtils.ScheduledShipmentLoadBuilder builder = LspShipmentUtils.ScheduledShipmentLoadBuilder.newInstance();
		builder.setResourceId(resource.getId());

		for (LogisticChainElement element : resource.getClientElements()) {
			if (element.getIncomingShipments().getLspShipmentsWTime().contains(lspShipment)) {
				builder.setLogisticChainElement(element);
			}
		}

		// The start activity is not contained in the tour elements, so the first tour element is the leg after the start.
		Tour.Leg legAfterStart = (Tour.Leg) tour.getTourElements().getFirst();
		double startTimeOfTransport = legAfterStart.getExpectedDepartureTime();
		double cumulatedLoadingTime = sumOfPickupServiceTimes(tour, lspPlan.getLSP());
		builder.setStartTime(startTimeOfTransport - cumulatedLoadingTime);
		builder.setEndTime(startTimeOfTransport);

		LspShipmentPlanElement load = builder.build();
		Id<LspShipmentPlanElement> id = LspShipmentUtils.createPlanElementId(load);
		LspShipmentUtils.getOrCreateShipmentPlan(lspPlan, lspShipment.getId()).addPlanElement(id, load);
	}

}
