package org.matsim.contrib.accessibility;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.accessibility.utils.*;
import org.matsim.contrib.drt.routing.DrtStopFacility;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.*;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.matrices.Entry;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author thibautd, dziemke
 */
final class EstimatedDrtAccessibilityContributionCalculator implements AccessibilityContributionCalculator {
	private static final Logger LOG = LogManager.getLogger( NetworkModeAccessibilityExpContributionCalculator.class );

	private final String mode;
	private final TravelDisutilityFactory travelDisutilityFactory;
	private final TravelTime travelTime;
	private final Scenario scenario;

	private final TravelDisutility travelDisutility;
	private final ScoringConfigGroup scoringConfigGroup;
	private final NetworkConfigGroup networkConfigGroup;

	private Network subNetwork;

	private final double betaWalkTT_h;

	private final double betaDrtTT_h;
	private final double betaDrtDist_m;
	private final double walkSpeed_m_s;

	private Node fromNode = null;

	private Map<Id<? extends BasicLocation>, ArrayList<ActivityFacility>> aggregatedMeasurePoints;
	private Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities;

	private final LeastCostPathCalculator router;
	TripRouter tripRouter ;// TODO: this Getter is a temporary hack. Talk to TS about other ways of accessing the stopFinder
	private DvrpRoutingModule.AccessEgressFacilityFinder stopFinder;

	public EstimatedDrtAccessibilityContributionCalculator(String mode, final TravelTime travelTime, final TravelDisutilityFactory travelDisutilityFactory, Scenario scenario, TripRouter tripRouter) {
		this.mode = mode;
		this.travelTime = travelTime;
		this.travelDisutilityFactory = travelDisutilityFactory;
		this.scenario = scenario;
		this.scoringConfigGroup = scenario.getConfig().scoring();
		this.networkConfigGroup = scenario.getConfig().network();
		this.tripRouter = tripRouter;

		Gbl.assertNotNull(travelDisutilityFactory);
		this.travelDisutility = travelDisutilityFactory.createTravelDisutility(travelTime);
		this.router = new SpeedyALTFactory().createPathCalculator(scenario.getNetwork(), travelDisutility, travelTime);

		betaWalkTT_h = scoringConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() - scoringConfigGroup.getPerforming_utils_hr();
		betaDrtTT_h = scoringConfigGroup.getModes().get(TransportMode.drt).getMarginalUtilityOfTraveling() - scoringConfigGroup.getPerforming_utils_hr();
		betaDrtDist_m = scoringConfigGroup.getModes().get(TransportMode.drt).getMarginalUtilityOfDistance();
		this.walkSpeed_m_s = scenario.getConfig().routing().getTeleportedModeSpeeds().get(TransportMode.walk);

		stopFinder = ((DvrpRoutingModule) tripRouter.getRoutingModule(TransportMode.drt)).getStopFinder();// todo
//		this.drtEstimator = new EuclideanDistanceBasedDrtEstimator(scenario.getNetwork(), 1.2, 0.0842928, 337.1288522,  5 * 60, 0, 0, 0);
//		this.drtEstimator = DetourBasedDrtEstimator.normalDistributed(337.1288522, 0.0842928, 0., 0. * 60, 0);



	}


	@Override
	public void initialize(ActivityFacilities measuringPoints, ActivityFacilities opportunities) {



		LOG.warn("Initializing calculator for mode " + mode + "...");
		LOG.warn("Full network has " + scenario.getNetwork().getNodes().size() + " nodes.");
		subNetwork = NetworkUtils.createNetwork(networkConfigGroup);
		Set<String> modeSet = new HashSet<>();
		modeSet.add(TransportMode.car);
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
		filter.filter(subNetwork, modeSet);
		if (subNetwork.getNodes().size() == 0) {
			throw new RuntimeException("Network has 0 nodes for mode " + mode + ". Something is wrong.");
		}
		LOG.warn("sub-network for mode " + modeSet + " now has " + subNetwork.getNodes().size() + " nodes.");

		this.aggregatedMeasurePoints = AccessibilityUtils.aggregateMeasurePointsWithSameNearestNode(measuringPoints, subNetwork);
//		this.aggregatedOpportunities = AccessibilityUtils.aggregateOpportunitiesWithSameNearestNode(opportunities, subNetwork, scenario.getConfig());

//		this.aggregatedMeasurePoints = measuringPoints;
		this.aggregatedOpportunities = aggregateOpportunitiesWithSameNearestDrtStop(opportunities, subNetwork, scenario.getConfig());


	}


	@Override
	public void notifyNewOriginNode(Id<? extends BasicLocation> fromNodeId, Double departureTime) {
		this.fromNode = subNetwork.getNodes().get(fromNodeId);
//		this.lcpt.calculate(subNetwork, fromNode, departureTime);

	}


	@Override
	public double computeContributionOfOpportunity(ActivityFacility origin,
												   Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities, Double departureTime) {
		double expSum = 0.;

		Optional<Pair<Facility, Facility>> facilities = stopFinder.findFacilities(origin, origin, null); //todo: cleanup
		Facility nearestStopAccess = facilities.get().getKey();

		List<? extends PlanElement> planElementsAccess = tripRouter.calcRoute(TransportMode.walk, origin, nearestStopAccess, departureTime, null, null);

		double accessTime_h = ((Leg) planElementsAccess.get(0)).getTravelTime().seconds() / 3600;

		double utility_access = accessTime_h * betaWalkTT_h;


		double utilityDrtConstant = AccessibilityUtils.getModeSpecificConstantForAccessibilities(TransportMode.drt, scoringConfigGroup);


		for (AggregationObject destination : aggregatedOpportunities.values()) {

			// Calculate main drt leg:
			Facility nearestStopEgress = (Facility) destination.getNearestBasicLocation();

			// Doesn't work because we need a person... //TODO: replace actual person with a fake person, or find workaround.
			List<? extends PlanElement> planElementsMain = tripRouter.calcRoute(TransportMode.car, nearestStopAccess, nearestStopEgress, 10 * 3600, scenario.getPopulation().getPersons().get(Id.createPersonId("1213")), null);

			double directRideDistance_m = ((Leg) planElementsMain.get(2)).getRoute().getDistance();

//			getAlphaBeta(null, null, null);

			double waitTime_s = 103.34; //TODO
			double rideTime_s = 47.84 + 0.1087 * directRideDistance_m;
			double totalTime_h = (waitTime_s + rideTime_s) / 3600;
			double utilityDrtTime = betaDrtTT_h * totalTime_h;
			double utilityDrtDistance = betaDrtDist_m * directRideDistance_m;



			// Pre-computed effect of all opportunities reachable from destination network node
			double sumExpVjkWalk = destination.getSum();


			System.out.println("___________________________________");
			System.out.println("Measuring Point: " + origin.getId().toString());
			System.out.println("utility access: " + utility_access + " ----- =" + betaWalkTT_h + " * " + accessTime_h);
			System.out.println("utility drt (time): " + utilityDrtTime + " ----- =" + betaDrtTT_h + " * " + totalTime_h);
			System.out.println("utility drt (distance): " + utilityDrtDistance + " ----- =" + betaDrtDist_m + " * " + directRideDistance_m);
			System.out.println("utility drt (constant): " + utilityDrtConstant);
			System.out.println("utility egress: " + Math.log(sumExpVjkWalk));
//			System.out.println("utility egress: (sumExpVjkWalk) exponential of utility, sum over all opportunities near stop: : " + sumExpVjkWalk);

			expSum += Math.exp(this.scoringConfigGroup.getBrainExpBeta() *
				(utility_access + utilityDrtTime + utilityDrtDistance + utilityDrtConstant ))
				* sumExpVjkWalk;
		}
		return expSum;
	}


	@Override
	public EstimatedDrtAccessibilityContributionCalculator duplicate() {
		LOG.info("Creating another EstimatedDrtAccessibilityContributionCalculator object.");
		EstimatedDrtAccessibilityContributionCalculator estimatedDrtAccessibilityContributionCalculator =
			new EstimatedDrtAccessibilityContributionCalculator(this.mode, this.travelTime, this.travelDisutilityFactory, this.scenario, tripRouter);
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


	private final Map<Id<? extends BasicLocation>, AggregationObject> aggregateOpportunitiesWithSameNearestDrtStop(
		final ActivityFacilities opportunities, Network network, Config config ) {
		// yyyy this method ignores the "capacities" of the facilities. kai, mar'14
		// for now, we decided not to add "capacities" as it is not needed for current projects. dz, feb'16

//		double walkSpeed_m_h = config.routing().getTeleportedModeSpeeds().get(TransportMode.walk) * 3600.;
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class);

		LOG.info("Aggregating " + opportunities.getFacilities().size() + " opportunities with same nearest node...");
		Map<Id<? extends BasicLocation>, AggregationObject> opportunityClusterMap = new ConcurrentHashMap<>();

		for (ActivityFacility opportunity : opportunities.getFacilities().values()) {

			Optional<Pair<Facility, Facility>> facilities = stopFinder.findFacilities(opportunity, opportunity, null);
			DrtStopFacility nearestStop = (DrtStopFacility) facilities.get().getKey();

			List<? extends PlanElement> planElements = tripRouter.calcRoute(TransportMode.walk, nearestStop, opportunity, 10 * 3600., null, null);// departure time should matter for walk
			double egressTime_s = ((Leg) planElements.get(0)).getTravelTime().seconds();

			double VjkWalkTravelTime = egressTime_s / 3600 * betaWalkTT_h; // a.k.a utility_egress

			double expVjk = Math.exp(config.scoring().getBrainExpBeta() * VjkWalkTravelTime);


			// add Vjk to sum
			AggregationObject jco = opportunityClusterMap.get(nearestStop.getId()); // Why "jco"?
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
		LOG.info("Aggregated " + opportunities.getFacilities().size() + " opportunities to " + opportunityClusterMap.size() + " nodes.");
		return opportunityClusterMap;
	}
}
