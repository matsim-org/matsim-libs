package org.matsim.contrib.accessibility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.util.Assert;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.accessibility.utils.*;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtStopFacility;
import org.matsim.contrib.dvrp.router.ClosestAccessEgressFacilityFinder;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.*;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.matsim.contrib.accessibility.AccessibilityUtils.extractLeg;

/**
 * @author jakobrehmann
 */
final class EstimatedDrtAccessibilityContributionCalculator implements AccessibilityContributionCalculator {
	private static final Logger LOG = LogManager.getLogger(EstimatedDrtAccessibilityContributionCalculator.class);
	private final String mode;
	private final Scenario scenario;
	private final ScoringConfigGroup scoringConfigGroup;
	private Network subNetwork;
	private final double betaDrtTT_h;
	private final double betaDrtDist_m;
	private final double betaWalkTT_h;
	private final double betaWalkDist_m;

	private Map<Id<? extends BasicLocation>, ArrayList<ActivityFacility>> aggregatedMeasurePoints;
	private Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities;

	private final TripRouter tripRouter ;

	private final DvrpRoutingModule.AccessEgressFacilityFinder stopFinder;

	private final DrtEstimator drtEstimator;
	private final Person dummyPerson;


	EstimatedDrtAccessibilityContributionCalculator(String mode, Scenario scenario, DvrpRoutingModule.AccessEgressFacilityFinder stopFinder, TripRouter tripRouter, DrtEstimator drtEstimator) {

		this.mode = mode;

		this.scenario = scenario;
		this.scoringConfigGroup = scenario.getConfig().scoring();
		this.tripRouter = tripRouter;
		this.stopFinder = stopFinder;
		this.drtEstimator = drtEstimator;

		// We need a dummy person, with a dummy vehicle, for the trip routers
		this.dummyPerson = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("dummy"));

		// Add default veh type, if doesn't yet exist
		VehicleType vehicleType = VehicleUtils.createDefaultVehicleType();
		if(!scenario.getVehicles().getVehicleTypes().containsKey(vehicleType.getId()))
			scenario.getVehicles().addVehicleType(vehicleType);

		// add dummy vehicle of default type
		Id<Vehicle> dummyVehicleId = Id.createVehicleId("dummy-veh");
		VehicleUtils.insertVehicleIdsIntoPersonAttributes(dummyPerson, Map.of(TransportMode.car, dummyVehicleId));

		if (!scenario.getVehicles().getVehicles().containsKey(dummyVehicleId)) {
			Vehicle vehicle = VehicleUtils.createVehicle(dummyVehicleId,vehicleType);
			scenario.getVehicles().addVehicle(vehicle);
		}

		// drt params
		this.betaDrtTT_h = scoringConfigGroup.getModes().get(TransportMode.drt).getMarginalUtilityOfTraveling() - scoringConfigGroup.getPerforming_utils_hr();
		this.betaDrtDist_m = scoringConfigGroup.getModes().get(TransportMode.drt).getMarginalUtilityOfDistance();

		// walk params
		this.betaWalkTT_h = scoringConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() - scoringConfigGroup.getPerforming_utils_hr();
		this.betaWalkDist_m = scoringConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfDistance();

	}


	@Override
	public void initialize(ActivityFacilities measuringPoints, ActivityFacilities opportunities) {

		LOG.warn("Initializing calculator for mode " + mode + "...");

		// Prepare measure points
		this.aggregatedMeasurePoints = new ConcurrentHashMap<>();
		Gbl.assertNotNull(measuringPoints);
		Gbl.assertNotNull(measuringPoints.getFacilities());

		// We decided to keep the measuring points on a disaggregate level (not aggregated by nearest DRT Stop) to allow for the calculation of the direct
		// walk from measuring point to opportunity
		for (ActivityFacility measuringPoint : measuringPoints.getFacilities().values()) {
			Id<ActivityFacility> facilityId = measuringPoint.getId();
			if(!aggregatedMeasurePoints.containsKey(facilityId)) {
				aggregatedMeasurePoints.put(facilityId, new ArrayList<>());
			}
			aggregatedMeasurePoints.get(facilityId).add(measuringPoint);
		}



		// We decided to keep opportunities disaggregated for same reason as stated above.
//		this.aggregatedOpportunities = aggregateOpportunitiesWithSameNearestDrtStop(opportunities, scenario.getConfig());
		this.aggregatedOpportunities = keepOpportunitiesDisaggregated(opportunities, scenario.getConfig());


	}


	@Override
	public void notifyNewOriginNode(Id<? extends BasicLocation> fromNodeId, Double departureTime) {
	//		nothing to do here...
	}


	@Override
	public double computeContributionOfOpportunity(ActivityFacility origin,
												   Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities, Double departureTime) {
		// quite possibly, the person should be part of the computeContributionOfOpportunity signature
		// quite possibly, the routingAttributes should be part of the computeContributionOfOpportunity signature

		// initialize sum of utilities
		double expSum = 0.;

		// find the closest stop to measuring point.
		Assert.isTrue(stopFinder instanceof ClosestAccessEgressFacilityFinder, "So far, findClosestStop() is only implemented in ClosestAccessEgressFacilityFinder");
		Facility nearestStopAccess = ((ClosestAccessEgressFacilityFinder) stopFinder).findClosestStop(origin);


		// UTILITY OF ACCESS WALK LEG
		// use walk router to calculate access walk time. Since it is a walk trip, there should only be a single leg.
		List<? extends PlanElement> planElementsAccess = tripRouter.calcRoute(TransportMode.walk, origin, nearestStopAccess, departureTime, null, null);
		Leg accessLeg = extractLeg(planElementsAccess, TransportMode.walk);
		double accessTime_h = accessLeg.getTravelTime().seconds() / 3600;
		double accessDist_m = accessLeg.getRoute().getDistance();
		double utilityAccess = accessTime_h * betaWalkTT_h + accessDist_m * betaWalkDist_m;

		Attributes routingAttributes = new AttributesImpl();

		// now we iterate through opportunities, to calculate the combined econometric accessibility from a single measuring point to all opportunities
		for (AggregationObject destination : aggregatedOpportunities.values()) {


			// find closest DRT stop to opportunity
			Facility opportunity = (Facility) destination.getNearestBasicLocation();
			Facility nearestStopEgress = ((ClosestAccessEgressFacilityFinder) stopFinder).findClosestStop(opportunity);

			// UTILITY OF DRT LEG
			// first, we route a car trip from pickup DRT stop to dropoff DRT stop, and extract the travel distance and travel time.
			List<? extends PlanElement> planElements = tripRouter.calcRoute(TransportMode.car, nearestStopAccess, nearestStopEgress, departureTime, dummyPerson, routingAttributes);
			Leg mainLeg = extractLeg(planElements, TransportMode.car);
			double directRideDistance_m = mainLeg.getRoute().getDistance();
			double directRideTime_sec = mainLeg.getRoute().getTravelTime().seconds();


			// DRT Estimator needs a "drt route"
			DrtRoute drtRoute = new DrtRoute(Id.createLinkId("dummyFrom"), Id.createLinkId("dummyTo"));
			drtRoute.setDistance(directRideDistance_m); // todo: since this is based on the distance and not the time of the direct car trips, congestion effects are not yet included.
			drtRoute.setDirectRideTime(directRideTime_sec);

			// Use DRT Estimator to gather estimate ride time including detours, as well as wait time.
			DrtEstimator.Estimate estimate = drtEstimator.estimate(drtRoute, OptionalTime.defined(departureTime));
			double waitTime_s = estimate.waitingTime();
			double rideTime_s = estimate.rideTime();

			// calculate time-based utility of DRT leg, based on the estimated wait and ride time
			double totalTime_h = (waitTime_s + rideTime_s) / 3600;
			double utilityDrtTime = betaDrtTT_h * totalTime_h;

			// calculate distance-based utility of DRT leg. NOTE: this distance does not include detours.
			// As the Estimator adds the detour to the time, we didn't know how to include distance detours in clean and comparable manner.
			double utilityDrtDistance = betaDrtDist_m * directRideDistance_m;


			// UTILITY OF EGRESS
			List<? extends PlanElement> planElementsEgress = tripRouter.calcRoute(TransportMode.walk, nearestStopEgress, opportunity, departureTime, null, null);
			Leg egressLeg = extractLeg(planElementsEgress, TransportMode.walk);
			double egressTime_h = egressLeg.getTravelTime().seconds() / 3600;
			double egressDist_m = egressLeg.getRoute().getDistance();
			double utilityEgress = egressTime_h * betaWalkTT_h + egressDist_m * betaWalkDist_m;

			// ASC
			double utilityDrtConstant = AccessibilityUtils.getModeSpecificConstantForAccessibilities(TransportMode.drt, scoringConfigGroup);

			// TOTAL UTILITY OF DRT TRIP (including access/egress/etc)
			double drtUtility =
					utilityAccess +
					utilityDrtTime +
					utilityDrtDistance +
					utilityEgress +
					utilityDrtConstant;





			// DIRECT WALK UTILITY
			// We need to also calculate direct walk utility to see whether it has a higher utility than DRT.
			List<? extends PlanElement> planElementsDirectWalk = tripRouter.calcRoute(TransportMode.walk, origin, opportunity, departureTime, null, null);
			Leg directWalkLeg = extractLeg(planElementsDirectWalk, TransportMode.walk);
			double directWalkTime_h = directWalkLeg.getTravelTime().seconds() / 3600;
			double directWalkDist_m = directWalkLeg.getRoute().getDistance();
			double directWalkUtility = directWalkTime_h * betaWalkTT_h +
				directWalkDist_m * betaWalkDist_m +
				AccessibilityUtils.getModeSpecificConstantForAccessibilities(TransportMode.walk, scoringConfigGroup);


			// Now we have to decide, whether DRT or direct is used.
			// We choose walk if it's utility if:
			// 		(a) walk utility is higher (less negative) than that of DRT
			// 		(b) the access drt stop is same as the egress drt stop (which means they don't actually take drt)

			double travelUtility;

			if (nearestStopAccess.equals(nearestStopEgress) || directWalkUtility > drtUtility) {
				travelUtility = directWalkUtility;
			} else {
				travelUtility = drtUtility;
			}

			// utility from measuring point to specific opportunity is added into the total utility from that measuring point.
			expSum += Math.exp(this.scoringConfigGroup.getBrainExpBeta() * travelUtility);

		}
		return expSum;
	}



	@Override
	public EstimatedDrtAccessibilityContributionCalculator duplicate() {
		LOG.info("Creating another EstimatedDrtAccessibilityContributionCalculator object.");
		EstimatedDrtAccessibilityContributionCalculator estimatedDrtAccessibilityContributionCalculator =
			new EstimatedDrtAccessibilityContributionCalculator(this.mode, this.scenario, this.stopFinder, this.tripRouter, this.drtEstimator);
		estimatedDrtAccessibilityContributionCalculator.subNetwork = this.subNetwork;
//		estimatedDrtAccessibilityContributionCalculator.aggregatedMeasurePoints = this.aggregatedMeasurePoints;
		estimatedDrtAccessibilityContributionCalculator.aggregatedOpportunities = this.aggregatedOpportunities;
		return estimatedDrtAccessibilityContributionCalculator;
	}


	@Override
	public Map<Id<? extends BasicLocation>, ArrayList<ActivityFacility>> getAggregatedMeasurePoints() {
		return aggregatedMeasurePoints;
	}


	@Override
	public Map<Id<? extends BasicLocation>, AggregationObject> getAgregatedOpportunities() {
		return aggregatedOpportunities;
	}


	private Map<Id<? extends BasicLocation>, AggregationObject> keepOpportunitiesDisaggregated(final ActivityFacilities opportunities, Config config ) {
		Map<Id<? extends BasicLocation>, AggregationObject> opportunityMap = new ConcurrentHashMap<>();

		for (ActivityFacility opportunity : opportunities.getFacilities().values()) {
			AggregationObject opportunityAsAggObj = new AggregationObject(opportunity.getId(), null, null, opportunity, 0.);
			opportunityMap.put(opportunity.getId(), opportunityAsAggObj);
		}
		return opportunityMap;
	}
	private Map<Id<? extends BasicLocation>, AggregationObject> aggregateOpportunitiesWithSameNearestDrtStop(
		final ActivityFacilities opportunities, Config config ) {

		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class);

		LOG.info("Aggregating " + opportunities.getFacilities().size() + " opportunities with same nearest drt stop...");
		Map<Id<? extends BasicLocation>, AggregationObject> opportunityClusterMap = new ConcurrentHashMap<>();

		for (ActivityFacility opportunity : opportunities.getFacilities().values()) {

			Assert.isTrue(stopFinder instanceof ClosestAccessEgressFacilityFinder, "So far, findClosestStop() is only implemented in ClosestAccessEgressFacilityFinder");
			DrtStopFacility nearestStop = (DrtStopFacility) ((ClosestAccessEgressFacilityFinder) stopFinder).findClosestStop(opportunity);

			List<? extends PlanElement> planElements = tripRouter.calcRoute(TransportMode.walk, nearestStop, opportunity, 10 * 3600., null, null);// departure time shouldn't matter for walk
			Leg leg = extractLeg(planElements, TransportMode.walk);
			double egressTime_s = leg.getTravelTime().seconds();

			double VjkWalkTravelTime = egressTime_s / 3600 * betaWalkTT_h; // a.k.a utility_egress

			double expVjk = Math.exp(config.scoring().getBrainExpBeta() * VjkWalkTravelTime);


			// add Vjk to sum
			AggregationObject jco = opportunityClusterMap.get(nearestStop.getId());
			if (jco == null) {
				jco = new AggregationObject(opportunity.getId(), null, null, nearestStop, 0.);
				opportunityClusterMap.put(nearestStop.getId(), jco);
			}
			if (acg.isUseOpportunityWeights()) {
				if (opportunity.getAttributes().getAttribute( Labels.WEIGHT ) == null) {
					throw new RuntimeException("If option \"useOpportunityWeights\" is used, the facilities must have an attribute with key " + Labels.WEIGHT + ".");
				} else {
					double weight = Double.parseDouble(opportunity.getAttributes().getAttribute( Labels.WEIGHT ).toString() );
					jco.addObject(opportunity.getId(), expVjk * Math.pow(weight, acg.getWeightExponent()));
				}
			} else {
				jco.addObject(opportunity.getId(), expVjk);
			}
		}
		LOG.info("Aggregated " + opportunities.getFacilities().size() + " opportunities to " + opportunityClusterMap.size() + " drt stops.");
		return opportunityClusterMap;
	}
}
