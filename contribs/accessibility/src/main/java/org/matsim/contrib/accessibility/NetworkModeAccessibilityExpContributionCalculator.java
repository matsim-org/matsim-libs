package org.matsim.contrib.accessibility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.accessibility.utils.*;
import org.matsim.contrib.roadpricing.RoadPricingScheme;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.*;
import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * @author thibautd, dziemke
 */
final class NetworkModeAccessibilityExpContributionCalculator implements AccessibilityContributionCalculator {
	private static final Logger LOG = LogManager.getLogger( NetworkModeAccessibilityExpContributionCalculator.class );

	private final String mode;
	private final TravelDisutilityFactory travelDisutilityFactory;
	private final TravelTime travelTime;
	private final Scenario scenario;

	private final TravelDisutility travelDisutility;
	private final ScoringConfigGroup scoringConfigGroup;
	private final NetworkConfigGroup networkConfigGroup;
	private final double betaCarTT_h;
	private final double betaCarDist_m;
	private final double betaCarDistMonetary_m;
	private final double marginalUtilityOfMoney;
	private final Double globalAverageIncome;

	private final AccessibilityConfigGroup acg;


	private Network subNetwork;

	private double betaWalkTT;
	private double walkSpeed_m_s;

	TripRouter tripRouter ;

	private Node fromNode = null;
	private LeastCostPathTreeExtended lcpt;
	//private final DijkstraTree dijkstraTree;
	//private final MultiNodePathCalculator multiNodePathCalculator;
	//private ImaginaryNode aggregatedToNodes;

	private Map<Id<? extends BasicLocation>, ArrayList<ActivityFacility>> aggregatedMeasurePoints;
	private Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities;
	private static PrintWriter csvWriter;
	private static final Object CSV_LOCK = new Object();



	public NetworkModeAccessibilityExpContributionCalculator(String mode, final TravelTime travelTime, final TravelDisutilityFactory travelDisutilityFactory, TripRouter tripRouter, Scenario scenario) {
		this.mode = mode;
		this.travelTime = travelTime;
		this.travelDisutilityFactory = travelDisutilityFactory;
		this.scenario = scenario;

		this.tripRouter = tripRouter;
		Gbl.assertNotNull(travelDisutilityFactory);
		this.travelDisutility = travelDisutilityFactory.createTravelDisutility(travelTime);

		scoringConfigGroup = scenario.getConfig().scoring();
		networkConfigGroup = scenario.getConfig().network();

		RoadPricingScheme scheme = (RoadPricingScheme) scenario.getScenarioElement( RoadPricingScheme.ELEMENT_NAME );
		this.lcpt = new LeastCostPathTreeExtended(travelTime, travelDisutility, scheme);
//		this.lcpt = new LeastCostPathTree(travelTime, travelDisutility);
		//this.dijkstraTree = new DijkstraTree(network, travelDisutility, travelTime);
		//FastMultiNodeDijkstraFactory fastMultiNodeDijkstraFactory = new FastMultiNodeDijkstraFactory(true);
		//this.multiNodePathCalculator = (MultiNodePathCalculator) fastMultiNodeDijkstraFactory.createPathCalculator(network, travelDisutility, travelTime);



		betaWalkTT = scoringConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling() - scoringConfigGroup.getPerforming_utils_hr();

		this.walkSpeed_m_s = scenario.getConfig().routing().getTeleportedModeSpeeds().get(TransportMode.walk);

		// drt params
		this.betaCarTT_h = scoringConfigGroup.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() - scoringConfigGroup.getPerforming_utils_hr();
		this.betaCarDist_m = scoringConfigGroup.getModes().get(TransportMode.car).getMarginalUtilityOfDistance();

		// euros per meter
		this.betaCarDistMonetary_m = scoringConfigGroup.getModes().get(TransportMode.car).getMonetaryDistanceRate();

		this.marginalUtilityOfMoney = scoringConfigGroup.getMarginalUtilityOfMoney();


		acg = (AccessibilityConfigGroup) this.scenario.getConfig().getModules().get(AccessibilityConfigGroup.GROUP_NAME);

		//todo toggle for single opportunity deepdive
		synchronized (CSV_LOCK) {
			if (csvWriter == null) {
				try {
					csvWriter = new PrintWriter(new File("person_deepdive.csv"));
					csvWriter.println("person,timeCar,distCar,expSum");
					csvWriter.flush();
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		}

		if (acg.isPersonBased()) {
			this.globalAverageIncome = scenario.getPopulation().getPersons().values().stream()
				.filter(person -> PersonUtils.getIncome(person) != null) //consider only agents that have a specific income provided
				.mapToDouble(PersonUtils::getIncome)
				.filter(dd -> dd > 0)
				.average().getAsDouble();
		} else {
			this.globalAverageIncome = null;
		}


	}


	@Override
	public void initialize(ActivityFacilities measuringPoints, ActivityFacilities opportunities) {
		LOG.warn("Initializing calculator for mode " + mode + "...");
		LOG.warn("Full network has " + scenario.getNetwork().getNodes().size() + " nodes.");
        subNetwork = NetworkUtils.createNetwork(networkConfigGroup);
        Set<String> modeSet = new HashSet<>();
        TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
        if (mode.equals(Modes4Accessibility.freespeed.name())) {
        	modeSet.add(TransportMode.car);
		} else {
        	modeSet.add(mode);
		}
        filter.filter(subNetwork, modeSet);
        if (subNetwork.getNodes().size() == 0) {
        	throw new RuntimeException("Network has 0 nodes for mode " + mode + ". Something is wrong.");
        }
		LOG.warn("sub-network for mode " + modeSet + " now has " + subNetwork.getNodes().size() + " nodes.");

        this.aggregatedMeasurePoints = AccessibilityUtils.aggregateMeasurePointsWithSameNearestNode(measuringPoints, subNetwork);
		this.aggregatedOpportunities = AccessibilityUtils.aggregateOpportunitiesWithSameNearestNode(opportunities, subNetwork, scenario.getConfig());
	}


	@Override
	public void notifyNewOriginNode(Id<? extends BasicLocation> fromNodeId, Double departureTime) {
		this.fromNode = subNetwork.getNodes().get(fromNodeId);
		this.lcpt.calculateExtended(subNetwork, fromNode, departureTime);
		//this.dijkstraTree.calcLeastCostPathTree(fromNode, departureTime);
		//multiNodePathCalculator.calcLeastCostPath(fromNode, aggregatedToNodes, departureTime, null, null);
	}


	@Override
	public double computeContributionOfOpportunity(ActivityFacility origin,
			Map<Id<? extends BasicLocation>, AggregationObject> aggregatedOpportunities, Double departureTime) {
		double expSum = 0.;

		// Find nearest link to measuring point
		Link nearestLink = NetworkUtils.getNearestLinkExactly(subNetwork, origin.getCoord());

		// Distances includes :
		// (1) distanceCoord2Intersection: from measuring point to closest point on link (WALKED)
		// (2) distanceIntersection2Node: distance along Link from intersection to end of link (DRIVEN)
		Distances distance = NetworkUtil.getDistances2NodeViaGivenLink(origin.getCoord(), nearestLink, fromNode);

		// first we deal with (1): (a) tt --> (b) (dis)utility
		double walkTravelTimeMeasuringPoint2Road_h = distance.getDistancePoint2Intersection() / (this.walkSpeed_m_s * 3600);
		double walkUtilityMeasuringPoint2Road = (walkTravelTimeMeasuringPoint2Road_h * betaWalkTT);

		// now we deal with (2): Travel on section of first link to first node
		double distanceFraction = distance.getDistanceIntersection2Node() / nearestLink.getLength();
		double congestedCarUtilityRoad2Node = -travelDisutility.getLinkTravelDisutility(nearestLink, departureTime, null, null) * distanceFraction;

		// now we take the ASC
		double modeSpecificConstant = AccessibilityUtils.getModeSpecificConstantForAccessibilities(mode, scoringConfigGroup);

		double lastTimeCar = 0.;
		double lastDistCar = 0.;

		for (final AggregationObject destination : aggregatedOpportunities.values()) {

			// Remaining travel on network

			double congestedCarUtility;
			if(!acg.isPersonBased()) {
				congestedCarUtility = -lcpt.getTree().get(((Node) destination.getNearestBasicLocation()).getId()).getCost();
				//double congestedCarUtility = - dijkstraTree.getLeastCostPath(destination.getNearestNode()).travelCost;
				//double congestedCarUtility = - multiNodePathCalculator.constructPath(fromNode, destination.getNearestNode(), departureTime).travelCost;
			} else {
				// using trip router
//			List<? extends PlanElement> planElements = tripRouter.calcRoute(TransportMode.car, homeFacility, opportunity, departureTime, person, null);
//			Leg mainLeg = extractLeg(planElements, TransportMode.car);
//			double timeCar = mainLeg.getTravelTime().seconds();
//			double distCar = mainLeg.getRoute().getDistance();

				// using lcpt
				double timeCar = lcpt.getTree().get(((Node) destination.getNearestBasicLocation()).getId()).getTime() - departureTime;
				double distCar = lcpt.getTreeExtended().get(((Node) destination.getNearestBasicLocation()).getId()).getDistance();

				// utility lost by time
				double utilityTimeCar = timeCar / 3600 * betaCarTT_h;//travel time * marginal utility of time spent traveling

				// utility lost by distance
				double utilityDistCar = distCar * betaCarDist_m;//marginal utility of distance

				double incomeFactor = this.globalAverageIncome / PersonUtils.getIncome((Person) origin.getAttributes().getAttribute("person"));

				double utilityDistCarMonetary = distCar * betaCarDistMonetary_m * marginalUtilityOfMoney * incomeFactor;//distance * monetary distance rate * marginal utility of money

				congestedCarUtility = utilityTimeCar + utilityDistCar + utilityDistCarMonetary;
				//			CharyparNagelScoringFunctionFactory fac = new CharyparNagelScoringFunctionFactory(scenario);
//			ScoringFunction newScoringFunction = fac.createNewScoringFunction(person);
//			newScoringFunction.
				lastTimeCar = timeCar;
				lastDistCar = distCar;
			}


			// Pre-computed effect of all opportunities reachable from destination network node
			// Combine all utility components (using the identity: exp(a+b) = exp(a) * exp(b))
			double sumExpVjkWalk = destination.getSum();

				expSum += Math.exp(this.scoringConfigGroup.getBrainExpBeta() * (walkUtilityMeasuringPoint2Road + modeSpecificConstant
					+ congestedCarUtilityRoad2Node + congestedCarUtility)) * sumExpVjkWalk;
		}
		if (acg.isPersonBased()) {
			Person person = (Person) origin.getAttributes().getAttribute("person");

			synchronized (CSV_LOCK) {
				csvWriter.printf(
					"%s,%.6f,%.6f,%.15g%n",
					person.getId(),
					lastTimeCar,
					lastDistCar,
					Math.log(expSum)
				);
				csvWriter.flush();
			}
		}
		return expSum;
	}


	// Needed if MultiNodePathCalculator is used as router -- experimental
//	public void setToNodes(ImaginaryNode aggregatedToNodes) {
//		log.warn("Setting toNodes.");
//		this.aggregatedToNodes = aggregatedToNodes;
//	}


	@Override
	public NetworkModeAccessibilityExpContributionCalculator duplicate() {
		LOG.info("Creating another NetworkModeAccessibilityExpContributionCalculator object.");
		NetworkModeAccessibilityExpContributionCalculator networkModeAccessibilityExpContributionCalculator =
				new NetworkModeAccessibilityExpContributionCalculator(this.mode, this.travelTime, this.travelDisutilityFactory, this.tripRouter,this.scenario);
		networkModeAccessibilityExpContributionCalculator.subNetwork = this.subNetwork;
		networkModeAccessibilityExpContributionCalculator.aggregatedMeasurePoints = this.aggregatedMeasurePoints;
		networkModeAccessibilityExpContributionCalculator.aggregatedOpportunities = this.aggregatedOpportunities;
		return networkModeAccessibilityExpContributionCalculator;
	}


	@Override
    public Map<Id<? extends BasicLocation>, ArrayList<ActivityFacility>> getAggregatedMeasurePoints() {
        return aggregatedMeasurePoints;
    }


	@Override
	public Map<Id<? extends BasicLocation>, AggregationObject> getAgregatedOpportunities() {
		return aggregatedOpportunities;
	}

	public void closeCsvWriter() {
		if (csvWriter != null) {
			csvWriter.close();
		}
	}
}


