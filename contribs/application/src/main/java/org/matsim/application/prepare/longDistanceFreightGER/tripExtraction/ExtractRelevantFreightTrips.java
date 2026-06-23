package org.matsim.application.prepare.longDistanceFreightGER.tripExtraction;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.common.conventions.vsp.SubpopulationDefaultNames;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import picocli.CommandLine;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CommandLine.Command(name = "extract-freight-trips", description = "Extract subset of freight trips from whole population", showDefaultValues = true)
public class ExtractRelevantFreightTrips implements MATSimAppCommand {
	// This script will extract the relevant freight trips within the given shape
	// file from the German wide freight traffic.
	private static final Logger log = LogManager.getLogger(ExtractRelevantFreightTrips.class);
	public static final String GEOGRAPHICAL_TRIP_TYPE = "geographical_Trip_Type";
	public static final String BOUNDARY = "boundary";
	public static final String ROUTED_DISTANCE_TO_BOUNDARY = "routedDistanceToBoundary";
	public static final String ROUTED_TRAVEL_TIME_TO_BOUNDARY = "routedTravelTimeToBoundary";
	public static final String ROUTED_DISTANCE_FROM_BOUNDARY = "routedDistanceFromBoundary";
	public static final String ROUTED_TRAVEL_TIME_FROM_BOUNDARY = "routedTravelTimeFromBoundary";
	public static final String ORIGINAL_X = "original_x";
	public static final String ORIGINAL_Y = "original_y";
	// Same-id links are only trusted if they are also close to the expected boundary coordinate.
	private static final double MAX_DISTANCE_FOR_SAME_ID_BOUNDARY_LINK = 50.0;

	/**
	 * Enum for the type of trips to be extracted. The following types are available:
	 * <ul> <li>ALL: Extract all trips driving in the shape are.</li>
	 * <li>INCOMING: Extract only trips with the destination in the shape and the origin outside the shape</li>
	 * <li>OUTGOING: Extract only trips with the origin in the shape and the destination outside the shape</li>
	 * <li>INTERNAL: Extract only trips with origin and destination in the shape area</li>
	 * <li>TRANSIT: Extract only trips driving through the shape area</li> </ul>
	 */
	private enum geographicalTripType {
		ALL, INCOMING, INTERNAL, OUTGOING, TRANSIT
	}

	private enum BoundaryCrossing {
		ENTERING, LEAVING
	}

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to country wide freight traffic")
	private Path freightDataDirectory;

	@CommandLine.Option(names = {"--long-distance-network", "--network"},
		description = "Path to the long-distance freight network used for routing and boundary detection", required = true)
	private Path longDistanceNetworkPath;

	@CommandLine.Option(names = {"--scenario-network"},
		description = "Scenario network can used to find new activity links based on the original coordinates of the start/end of the trips. If no scenario network is used, the only the original coords of the trips are used.")
	private Path scenarioNetworkPath;

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	@CommandLine.Mixin
	private CrsOptions crs = new CrsOptions();

	@CommandLine.Option(names = "--output", description = "Path of the output plans file", required = true)
	private Path outputPath;

	@CommandLine.Option(names = "--cut-on-boundary", description = "Cut trips on shape-file boundary", defaultValue = "false")
	private boolean cutOnBoundary;

	@CommandLine.Option(names = "--geographicalTripType", description = "Set the geographicalTripType: OUTGOING, INCOMING, TRANSIT, INTERNAL, ALL", defaultValue = "ALL")
	private geographicalTripType geographicalTripType;

	@CommandLine.Option(names = "--legMode", description = "Set leg mode for long distance freight legs.", defaultValue = "car")
	private String legMode;

	@CommandLine.Option(names = "--subpopulation", description = "Set subpopulation for the extracted freight trips", defaultValue = SubpopulationDefaultNames.SUBPOP_LONG_DISTANCE_FREIGHT)
	private String subpopulation;

	public static void main(String[] args) {
		System.exit(new CommandLine(new ExtractRelevantFreightTrips()).execute(args));
	}

	@Override
	public Integer call() throws Exception {

		if (shp.getShapeFile() == null) {
			log.error("Shape file needs to be defined");
			return 2;
		}

		// Loading Scenario
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem(crs.getInputCRS());
		config.network().setInputFile(longDistanceNetworkPath.toString());
		Scenario outputScenario = ScenarioUtils.loadScenario(config);
		config.plans().setInputFile(freightDataDirectory.toString());
		config.routing().setRoutingRandomness(0);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network longDistanceNetwork = scenario.getNetwork();
		Network scenarioNetwork = null;
		if (scenarioNetworkPath != null) {
			log.info("Loading scenario network from {}", scenarioNetworkPath);
			scenarioNetwork = NetworkUtils.readNetwork(scenarioNetworkPath.toString());
		}
		Population originalPlans = scenario.getPopulation();
		Population outputPlans = outputScenario.getPopulation();
		PopulationFactory populationFactory = outputPlans.getFactory();

		// Create router
		FreeSpeedTravelTime travelTime = new FreeSpeedTravelTime();
		LeastCostPathCalculatorFactory fastAStarLandmarksFactory = new SpeedyALTFactory();
		RandomizingTimeDistanceTravelDisutilityFactory disutilityFactory = new RandomizingTimeDistanceTravelDisutilityFactory(
				"car", config);
		TravelDisutility travelDisutility = disutilityFactory.createTravelDisutility(travelTime);
		LeastCostPathCalculator router = fastAStarLandmarksFactory.createPathCalculator(longDistanceNetwork, travelDisutility,
				travelTime);

		// Reading Shape file
		Geometry relevantArea = shp.getGeometry();

		CoordinateTransformation sct = shp.createTransformation(crs.getInputCRS());

		log.info("Filtering the links within the relevant area...");
		// Identify links on the boundary
		Set<Id<Link>> linksOnTheBoundary = new HashSet<>();
		for (Link link : longDistanceNetwork.getLinks().values()) {
			Coord fromCoord = sct.transform(link.getFromNode().getCoord());
			Coord toCoord = sct.transform(link.getToNode().getCoord());
			if (relevantArea.contains(MGC.coord2Point(fromCoord)) ^ relevantArea.contains(MGC.coord2Point(toCoord))) {
				linksOnTheBoundary.add(link.getId());
			}
		}
		log.info("Finished filtering the links within the relevant area...");

		CoordinateTransformation ct = crs.getTransformation();
		BoundaryLocationResolver boundaryLocationResolver = new BoundaryLocationResolver(scenarioNetwork, legMode);

		// Create modified population
		int generated = 0;
		int processed = 0;
		log.info("Start creating the modified plans: there are in total {} persons to be processed",
				originalPlans.getPersons().size());

		for (Person person : originalPlans.getPersons().values()) {

			processed += 1;
			if (processed % 10000 == 0) {
				log.info("Processing: {} persons of {} persons have been processed", processed, originalPlans.getPersons().size());
			}

			Plan plan = person.getSelectedPlan();
			// By default, the plan of each freight person consist of only 3 elements:
			// startAct, leg, endAct
			Activity startActivity = (Activity) plan.getPlanElements().get(0);
			Activity endActivity = (Activity) plan.getPlanElements().get(2);
			ResolvedActivityLocation startLocation = resolveActivityLocation(startActivity, longDistanceNetwork, scenarioNetwork, legMode);
			ResolvedActivityLocation endLocation = resolveActivityLocation(endActivity, longDistanceNetwork, scenarioNetwork, legMode);
			if (startLocation == null || endLocation == null) {
				continue;
			}
			Id<Link> startLink = startLocation.routingLink().getId();
			Id<Link> endLink = endLocation.routingLink().getId();
			double departureTime = startActivity.getEndTime().orElse(0);

			boolean originIsInside = relevantArea.contains(MGC.coord2Point(sct.transform(startLocation.originalCoord()))) || linksOnTheBoundary.contains(startLink);
			boolean destinationIsInside = relevantArea.contains(MGC.coord2Point(sct.transform(endLocation.originalCoord()))) || linksOnTheBoundary.contains(endLink);

			Activity act0 = populationFactory.createActivityFromCoord("freight_start", null);
			Leg leg = populationFactory.createLeg(legMode);
			Activity act1 = populationFactory.createActivityFromCoord("freight_end", null);
			String geographicalTripTypeAttribute;

			switch (geographicalTripType) {
				case ALL -> {
					geographicalTripTypeAttribute = createActivitiesForInternalTrips(originIsInside, destinationIsInside, act0, ct, startLocation, act1, endLocation);
					if (geographicalTripTypeAttribute == null) {
						geographicalTripTypeAttribute = createActivitiesForOutgoingTrip(originIsInside, destinationIsInside, act0, ct, startLocation, router,
							linksOnTheBoundary, act1, endLocation, boundaryLocationResolver);
					}
					if (geographicalTripTypeAttribute == null) {
						geographicalTripTypeAttribute = createActivitiesForIncomingTrips(originIsInside, destinationIsInside, act0, ct, startLocation, router,
							linksOnTheBoundary, act1, endLocation, boundaryLocationResolver);
					}
					if (geographicalTripTypeAttribute == null) {
						geographicalTripTypeAttribute = createActivitiesForTransitTrip(originIsInside, destinationIsInside, act0, ct, startLocation, router,
							linksOnTheBoundary, act1, endLocation, boundaryLocationResolver);
					}
				}
				case INTERNAL ->
					geographicalTripTypeAttribute = createActivitiesForInternalTrips(originIsInside, destinationIsInside, act0, ct, startLocation, act1, endLocation);
				case OUTGOING ->
					geographicalTripTypeAttribute = createActivitiesForOutgoingTrip(originIsInside, destinationIsInside, act0, ct, startLocation, router,
						linksOnTheBoundary, act1, endLocation, boundaryLocationResolver);
				case INCOMING ->
					geographicalTripTypeAttribute = createActivitiesForIncomingTrips(originIsInside, destinationIsInside, act0, ct, startLocation, router,
						linksOnTheBoundary, act1, endLocation, boundaryLocationResolver);
				case TRANSIT ->
					geographicalTripTypeAttribute = createActivitiesForTransitTrip(originIsInside, destinationIsInside, act0, ct, startLocation, router,
						linksOnTheBoundary, act1, endLocation, boundaryLocationResolver);
				default -> throw new IllegalStateException("Unexpected value: " + geographicalTripType);
			}

			// Add new freight person to the output plans if trips is relevant
			if (geographicalTripTypeAttribute != null && act0.getCoord() != null && act1.getCoord() != null && departureTime < 86400) {
				// The output start activity is newly created, so copy the original trip's start time.
				act0.setEndTime(departureTime);
				Person freightPerson = populationFactory.createPerson(Id.create("freight_" + generated, Person.class));
				person.getAttributes().getAsMap().forEach(freightPerson.getAttributes()::putAttribute);
				freightPerson.getAttributes().putAttribute(GEOGRAPHICAL_TRIP_TYPE, geographicalTripTypeAttribute);
				freightPerson.getAttributes().putAttribute("subpopulation", subpopulation);
				Plan freightPersonPlan = populationFactory.createPlan();
				freightPersonPlan.addActivity(act0);
				freightPersonPlan.addLeg(leg);
				freightPersonPlan.addActivity(act1);
				freightPerson.addPlan(freightPersonPlan);
				outputPlans.addPerson(freightPerson);
				generated += 1;
			}
		}

		if (crs.getTargetCRS() != null)
			ProjectionUtils.putCRS(outputPlans, crs.getTargetCRS());

		// Write population
		log.info("Writing population file...");
		Path outputDirectory = outputPath.getParent();
		if (outputDirectory != null) {
			Files.createDirectories(outputDirectory);
		}

		PopulationWriter pw = new PopulationWriter(outputPlans);
		pw.write(outputPath.toString());

		String resultSummaryPath = outputPath.toString().replace(".gz", "").replace(".xml", "")
				+ "-locations-summary.tsv";
		createOutput_tripOD_relations(resultSummaryPath, outputPlans);

		return 0;
	}

	private static void createOutput_tripOD_relations(String freightTripTsvPath, Population outputPopulation) throws IOException{
		// this was now copied from GenerateFreightPlans which was moved to matsim-germany.  Could be organized in a better way if desired; as
		// a quick fix e.g. use the static method from here inside matsim-germany.

		CSVPrinter tsvWriter = new CSVPrinter(new FileWriter(freightTripTsvPath), CSVFormat.TDF);
		tsvWriter.printRecord("trip_id", "from_x", "from_y", "to_x", "to_y");
		for (Person person : outputPopulation.getPersons().values()) {
			List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
			Activity act0 = (Activity) planElements.get(0);
			Activity act1 = (Activity) planElements.get(2);
			Coord fromCoord = act0.getCoord();
			Coord toCoord = act1.getCoord();
			tsvWriter.printRecord(person.getId().toString(), fromCoord.getX(), fromCoord.getY(), toCoord.getX(), toCoord.getY());
		}
		tsvWriter.close();
	}

	/**
	 * Create activities if the trip is a transit trips
	 */
	private String createActivitiesForTransitTrip(boolean originIsInside, boolean destinationIsInside, Activity act0, CoordinateTransformation ct,
												ResolvedActivityLocation startLocation, LeastCostPathCalculator router,
												Set<Id<Link>> linksOnTheBoundary, Activity act1, ResolvedActivityLocation endLocation,
												BoundaryLocationResolver boundaryLocationResolver) {
		if (!originIsInside && !destinationIsInside) {
			boolean vehicleIsInside = false;
			LeastCostPathCalculator.Path route = router.calcLeastCostPath(
					startLocation.routingLink(), endLocation.routingLink(), 0,
					null, null);
			if (route.links.isEmpty()) {
				return null;
			}
			if (cutOnBoundary) {
				double travelTimeFromStartToBoundary = 0;
				double distanceFromStartToBoundary = 0;
				RouteMetrics routeMetrics = getRouteMetrics(route.links);
				for (Link link : route.links) {
					if (linksOnTheBoundary.contains(link.getId())) {
						if (!vehicleIsInside) {
							setBoundaryActivityCoord(act0, link, ct, BoundaryCrossing.ENTERING, boundaryLocationResolver);
							addBoundaryAttributesToStartActivity(act0, distanceFromStartToBoundary, travelTimeFromStartToBoundary);
							vehicleIsInside = true;
						} else {
							setBoundaryActivityCoord(act1, link, ct, BoundaryCrossing.LEAVING, boundaryLocationResolver);
							double distanceFromBoundaryToEnd = routeMetrics.distance() - distanceFromStartToBoundary;
							double travelTimeFromBoundaryToEnd = routeMetrics.travelTime() - travelTimeFromStartToBoundary;
							addBoundaryAttributesToEndActivity(act1, distanceFromBoundaryToEnd, travelTimeFromBoundaryToEnd);
							break;
						}
					}
					travelTimeFromStartToBoundary += getLinkTravelTime(link);
					distanceFromStartToBoundary += link.getLength();
				}
			} else {
				for (Link link : route.links) {
					if (linksOnTheBoundary.contains(link.getId())) {
						setRealActivityCoord(act0, startLocation, ct);
						setRealActivityCoord(act1, endLocation, ct);
						break;
					}
				}
			}
			return "transit";
		}
		return null;
	}

	/**
	 * Create activities if the trip is an incoming trip
	 */
	private String createActivitiesForIncomingTrips(boolean originIsInside, boolean destinationIsInside, Activity act0, CoordinateTransformation ct,
												  ResolvedActivityLocation startLocation, LeastCostPathCalculator router,
												  Set<Id<Link>> linksOnTheBoundary, Activity act1, ResolvedActivityLocation endLocation,
												  BoundaryLocationResolver boundaryLocationResolver) {
		if (!originIsInside && destinationIsInside) {
			if (cutOnBoundary) {
				boolean isCoordSet = false;
				LeastCostPathCalculator.Path route = router.calcLeastCostPath(
						startLocation.routingLink(), endLocation.routingLink(),
						0, null, null);
				if (route.links.isEmpty()) {
					return null;
				}
				double travelTimeFromStartToBoundary = 0;
				double distanceFromStartToBoundary = 0;
				for (Link link : route.links) {
					if (linksOnTheBoundary.contains(link.getId())) {
						setBoundaryActivityCoord(act0, link, ct, BoundaryCrossing.ENTERING, boundaryLocationResolver);
						addBoundaryAttributesToStartActivity(act0, distanceFromStartToBoundary, travelTimeFromStartToBoundary);
						isCoordSet = true;
						break;
					}
					travelTimeFromStartToBoundary += getLinkTravelTime(link);
					distanceFromStartToBoundary += link.getLength();
				}
				if (!isCoordSet) {
					setBoundaryActivityCoord(act0, route.links.getFirst(), ct, BoundaryCrossing.ENTERING, boundaryLocationResolver);
				}
			} else {
				setRealActivityCoord(act0, startLocation, ct);
			}
			setRealActivityCoord(act1, endLocation, ct);
			return "incoming";
		}
		return null;
	}

	/**
	 * Create activities if the trip is an outgoing trip
	 */
	private String createActivitiesForOutgoingTrip(boolean originIsInside, boolean destinationIsInside, Activity act0, CoordinateTransformation ct,
												 ResolvedActivityLocation startLocation, LeastCostPathCalculator router,
												 Set<Id<Link>> linksOnTheBoundary, Activity act1, ResolvedActivityLocation endLocation,
												 BoundaryLocationResolver boundaryLocationResolver) {
		if (originIsInside && !destinationIsInside) {
			setRealActivityCoord(act0, startLocation, ct);
			if (cutOnBoundary) {
				boolean isCoordSet = false;
				LeastCostPathCalculator.Path route = router.calcLeastCostPath(
					startLocation.routingLink(), endLocation.routingLink(),
					0, null, null);
				if (route.links.isEmpty()) {
					return null;
				}
				RouteMetrics routeMetrics = getRouteMetrics(route.links);
				double travelTimeFromStartToBoundary = 0;
				double distanceFromStartToBoundary = 0;
				for (Link link : route.links) {
					if (linksOnTheBoundary.contains(link.getId())) {
						setBoundaryActivityCoord(act1, link, ct, BoundaryCrossing.LEAVING, boundaryLocationResolver);
						double distanceFromBoundaryToEnd = routeMetrics.distance() - distanceFromStartToBoundary;
						double travelTimeFromBoundaryToEnd = routeMetrics.travelTime() - travelTimeFromStartToBoundary;
						addBoundaryAttributesToEndActivity(act1, distanceFromBoundaryToEnd, travelTimeFromBoundaryToEnd);
						isCoordSet = true;
						break;
					}
					travelTimeFromStartToBoundary += getLinkTravelTime(link);
					distanceFromStartToBoundary += link.getLength();
				}
				if (!isCoordSet) {
					int lastOne = route.links.size() - 1;
					setBoundaryActivityCoord(act1, route.links.get(lastOne), ct, BoundaryCrossing.LEAVING, boundaryLocationResolver);
				}
			} else {
				setRealActivityCoord(act1, endLocation, ct);
			}
			return "outgoing";
		}
		return null;
	}

	/**
	 * Create activities if the trip is an internal trip
	 */
	private static String createActivitiesForInternalTrips(boolean originIsInside, boolean destinationIsInside, Activity act0, CoordinateTransformation ct,
														 ResolvedActivityLocation startLocation, Activity act1, ResolvedActivityLocation endLocation) {
		if (originIsInside && destinationIsInside) {
			setRealActivityCoord(act0, startLocation, ct);
			setRealActivityCoord(act1, endLocation, ct);
			return "internal";
		}
		return null;
	}

	/**
	 * Resolves the original coordinate, the routing link from the long-distance freight network, and optionally the
	 * activity coordinate anchor from the scenario network. This keeps routing stable while allowing real activities to
	 * be written near the scenario network instead of the coarse long-distance network.
	 */
	private static ResolvedActivityLocation resolveActivityLocation(Activity activity, Network longDistanceNetwork, Network scenarioNetwork,
																   String linkMode) {
		Coord originalCoord = getOriginalCoord(activity);
		Link routingLink = null;
		if (activity.getLinkId() != null) {
			routingLink = longDistanceNetwork.getLinks().get(activity.getLinkId());
		}
		if (routingLink == null && originalCoord != null) {
			routingLink = getNearestModeLink(longDistanceNetwork, originalCoord, linkMode);
		}
		if (originalCoord == null && routingLink != null) {
			originalCoord = routingLink.getCoord();
		}
		if (originalCoord == null || routingLink == null) {
			return null;
		}

		Link scenarioLink = null;
		if (scenarioNetwork != null) {
			scenarioLink = getNearestModeLink(scenarioNetwork, originalCoord, linkMode);
		}
		return new ResolvedActivityLocation(activity, originalCoord, routingLink, scenarioLink);
	}

	/**
	 * Reads the fachliche/original activity coordinate. If a previous extraction step already stored the original
	 * coordinate as attributes, this value is used; otherwise the current activity coordinate is treated as original.
	 */
	private static Coord getOriginalCoord(Activity activity) {
		Object x = activity.getAttributes().getAttribute(ORIGINAL_X);
		Object y = activity.getAttributes().getAttribute(ORIGINAL_Y);
		if (x != null && y != null) {
			return new Coord(Double.parseDouble(x.toString()), Double.parseDouble(y.toString()));
		}
		return activity.getCoord();
	}

	/**
	 * Writes a real origin/destination activity. If a scenario network was provided, its nearest link coordinate and
	 * link id are used; otherwise the original coordinate is kept and no link id is written. The original coordinate is
	 * always preserved as attributes for later analysis or re-mapping.
	 */
	private static void setRealActivityCoord(Activity outputActivity, ResolvedActivityLocation sourceLocation, CoordinateTransformation ct) {
		sourceLocation.activity().getAttributes().getAsMap().forEach(outputActivity.getAttributes()::putAttribute);
		Coord originalCoord = ct.transform(sourceLocation.originalCoord());
		Link scenarioLink = sourceLocation.scenarioLink();
		Coord activityCoord = scenarioLink != null ? scenarioLink.getCoord() : sourceLocation.originalCoord();
		outputActivity.setCoord(ct.transform(activityCoord));
		if (scenarioLink != null) {
			outputActivity.setLinkId(scenarioLink.getId());
		}
		outputActivity.getAttributes().putAttribute(ORIGINAL_X, originalCoord.getX());
		outputActivity.getAttributes().putAttribute(ORIGINAL_Y, originalCoord.getY());
	}

	/**
	 * Finds the nearest link for the requested mode. If no mode-compatible link is found, the nearest link of any mode
	 * is returned as fallback so that coordinate-only plans can still be processed.
	 */
	private static Link getNearestModeLink(Network network, Coord coord, String linkMode) {
		Link nearestLink = NetworkUtils.getNearestLinkExactly(network, coord);
		if (nearestLink == null || isModeCompatible(nearestLink, linkMode)) {
			return nearestLink;
		}

		Link nearestModeLink = null;
		double shortestDistance = Double.MAX_VALUE;
		for (Link link : network.getLinks().values()) {
			if (!isModeCompatible(link, linkMode)) {
				continue;
			}
			double distance = getDistanceToLink(coord, link);
			if (distance < shortestDistance) {
				shortestDistance = distance;
				nearestModeLink = link;
			}
		}
		return nearestModeLink != null ? nearestModeLink : nearestLink;
	}

	/**
	 * Writes artificial boundary activities from the route direction. If a scenario network is available, the coordinate
	 * and link id are taken from the best matching scenario link so downstream runs do not remap the activity to a nearby
	 * lower-class road. Without scenario network, this falls back to the long-distance boundary link coordinate only.
	 */
	private static void setBoundaryActivityCoord(Activity activity, Link boundaryLink, CoordinateTransformation ct, BoundaryCrossing crossing,
												BoundaryLocationResolver boundaryLocationResolver) {
		Coord longDistanceBoundaryCoord = getBoundaryNodeCoord(boundaryLink, crossing);
		Link scenarioLink = boundaryLocationResolver.resolveScenarioLink(boundaryLink, crossing, longDistanceBoundaryCoord);
		if (scenarioLink == null) {
			activity.setCoord(ct.transform(longDistanceBoundaryCoord));
		} else {
			activity.setCoord(ct.transform(NetworkUtils.getCloserNodeOnLink(longDistanceBoundaryCoord, scenarioLink).getCoord()));
			activity.setLinkId(scenarioLink.getId());
		}
	}

	/**
	 * Selects the boundary-side node of a long-distance link based on travel direction. Entering trips use the to-node;
	 * leaving trips use the from-node so the artificial activity remains on the simulated side of the cut.
	 */
	private static Coord getBoundaryNodeCoord(Link boundaryLink, BoundaryCrossing crossing) {
		return switch (crossing) {
			case ENTERING -> boundaryLink.getToNode().getCoord();
			case LEAVING -> boundaryLink.getFromNode().getCoord();
		};
	}

	/**
	 * Resolves boundary activities against the scenario network once per boundary link and crossing direction. The route
	 * is still cut on the long-distance network, but output activities are anchored to scenario-network links when
	 * possible.
	 */
	private static final class BoundaryLocationResolver {
		private final Network scenarioNetwork;
		private final String linkMode;
		private final Map<BoundaryCacheKey, Link> cache = new HashMap<>();

		private BoundaryLocationResolver(Network scenarioNetwork, String linkMode) {
			this.scenarioNetwork = scenarioNetwork;
			this.linkMode = linkMode;
		}

		/**
		 * Returns the scenario-network anchor for one long-distance boundary link. A scenario-network link is preferred if it has
		 * the same id, then if it has the same road type, then if it has the same road-type family; otherwise the nearest
		 * mode-compatible scenario link is used as fallback. If no scenario network exists, null is returned.
		 */
		private Link resolveScenarioLink(Link longDistanceBoundaryLink, BoundaryCrossing crossing, Coord longDistanceBoundaryCoord) {
			if (scenarioNetwork == null) {
				return null;
			}

			BoundaryCacheKey key = new BoundaryCacheKey(longDistanceBoundaryLink.getId(), crossing);
			Link cachedLink = cache.get(key);
			if (cachedLink != null) {
				return cachedLink;
			}

			Link scenarioLink = findMatchingScenarioBoundaryLink(longDistanceBoundaryLink, longDistanceBoundaryCoord);
			if (scenarioLink != null) {
				cache.put(key, scenarioLink);
			}
			return scenarioLink;
		}

		/**
		 * Searches the scenario network for a boundary link that is compatible with the long-distance boundary link. The
		 * type-based passes prevent motorway/trunk boundary activities from being snapped to closer local roads at the
		 * edge of a cut scenario network.
		 */
		private Link findMatchingScenarioBoundaryLink(Link longDistanceBoundaryLink, Coord longDistanceBoundaryCoord) {
			Link sameIdLink = scenarioNetwork.getLinks().get(longDistanceBoundaryLink.getId());
			if (sameIdLink != null && isModeCompatible(sameIdLink, linkMode) && isCloseToReferenceCoord(sameIdLink, longDistanceBoundaryCoord)) {
				return sameIdLink;
			}

			String roadType = getComparableRoadType(longDistanceBoundaryLink);
			if (roadType != null) {
				Link sameTypeLink = findNearestScenarioLink(longDistanceBoundaryLink, longDistanceBoundaryCoord, roadType, false);
				if (sameTypeLink == null) {
					sameTypeLink = findNearestScenarioLink(longDistanceBoundaryLink, longDistanceBoundaryCoord, getRoadTypeFamily(roadType), true);
				}
				if (sameTypeLink != null) {
					return sameTypeLink;
				}
			}

			return getNearestModeLink(scenarioNetwork, longDistanceBoundaryCoord, linkMode);
		}

		/**
		 * Finds the nearest mode-compatible scenario link matching either an exact road type or a road-type family.
		 * Same-direction candidates are preferred to avoid choosing the opposite carriageway when both directions are near.
		 */
		private Link findNearestScenarioLink(Link referenceLink, Coord referenceCoord, String requiredRoadType, boolean matchRoadTypeFamily) {
			Link nearestLink = null;
			double nearestDistance = Double.MAX_VALUE;
			Link nearestSameDirectionLink = null;
			double nearestSameDirectionDistance = Double.MAX_VALUE;

			for (Link link : scenarioNetwork.getLinks().values()) {
				if (!isModeCompatible(link, linkMode) || !matchesRoadType(link, requiredRoadType, matchRoadTypeFamily)) {
					continue;
				}

				double distance = getDistanceToLink(referenceCoord, link);
				if (distance < nearestDistance) {
					nearestDistance = distance;
					nearestLink = link;
				}
				if (pointsInSameDirection(referenceLink, link) && distance < nearestSameDirectionDistance) {
					nearestSameDirectionDistance = distance;
					nearestSameDirectionLink = link;
				}
			}

			return nearestSameDirectionLink != null ? nearestSameDirectionLink : nearestLink;
		}
	}

	/**
	 * Checks if a candidate link has the requested road type. Depending on the caller, the comparison is either exact or
	 * based on the road-type family, e.g. treating motorway and motorway_link as related.
	 */
	private static boolean matchesRoadType(Link link, String requiredRoadType, boolean matchRoadTypeFamily) {
		String linkRoadType = getComparableRoadType(link);
		if (linkRoadType == null) {
			return false;
		}
		if (matchRoadTypeFamily) {
			return getRoadTypeFamily(linkRoadType).equals(requiredRoadType);
		}
		return linkRoadType.equals(requiredRoadType);
	}

	/**
	 * Guards the same-id shortcut against accidental id collisions between different networks. A scenario link with the
	 * same id is only accepted if its straight link segment lies close to the long-distance boundary coordinate.
	 */
	private static boolean isCloseToReferenceCoord(Link link, Coord referenceCoord) {
		return getDistanceToLink(referenceCoord, link) <= MAX_DISTANCE_FOR_SAME_ID_BOUNDARY_LINK;
	}

	private static double getDistanceToLink(Coord coord, Link link) {
		return NetworkUtils.getEuclideanDistance(coord, NetworkUtils.findNearestPointOnLink(coord, link));
	}

	private static boolean isModeCompatible(Link link, String linkMode) {
		return linkMode == null || link.getAllowedModes().contains(linkMode);
	}

	/**
	 * Normalizes MATSim road type values for comparison, e.g. stripping the optional "highway." prefix. If the network
	 * has no road type, null is returned so type matching is skipped instead of matching every unclassified link.
	 */
	private static String getComparableRoadType(Link link) {
		String type = NetworkUtils.getType(link);
		if (type == null || type.isBlank()) {
			return null;
		}
		return type.replaceFirst("^highway\\.", "");
	}

	/**
	 * Reduces ramp/link variants to their parent road class. This allows the boundary search to still prefer motorway
	 * infrastructure if one network stores the cut as motorway and the other as motorway_link.
	 */
	private static String getRoadTypeFamily(String roadType) {
		if (roadType.endsWith("_link")) {
			return roadType.substring(0, roadType.length() - "_link".length());
		}
		return roadType;
	}

	/**
	 * Compares the direction vectors of two links. The search uses this to prefer the carriageway with the same travel
	 * direction when both directions are close to the boundary coordinate.
	 */
	private static boolean pointsInSameDirection(Link referenceLink, Link candidateLink) {
		double referenceX = referenceLink.getToNode().getCoord().getX() - referenceLink.getFromNode().getCoord().getX();
		double referenceY = referenceLink.getToNode().getCoord().getY() - referenceLink.getFromNode().getCoord().getY();
		double candidateX = candidateLink.getToNode().getCoord().getX() - candidateLink.getFromNode().getCoord().getX();
		double candidateY = candidateLink.getToNode().getCoord().getY() - candidateLink.getFromNode().getCoord().getY();
		return referenceX * candidateX + referenceY * candidateY >= 0;
	}

	private static void addBoundaryAttributesToStartActivity(Activity activity, double routedDistanceToBoundary, double routedTravelTimeToBoundary) {
		activity.getAttributes().putAttribute(BOUNDARY, true);
		activity.getAttributes().putAttribute(ROUTED_DISTANCE_TO_BOUNDARY, routedDistanceToBoundary);
		activity.getAttributes().putAttribute(ROUTED_TRAVEL_TIME_TO_BOUNDARY, routedTravelTimeToBoundary);
	}

	private static void addBoundaryAttributesToEndActivity(Activity activity, double routedDistanceFromBoundary, double routedTravelTimeFromBoundary) {
		activity.getAttributes().putAttribute(BOUNDARY, true);
		activity.getAttributes().putAttribute(ROUTED_DISTANCE_FROM_BOUNDARY, routedDistanceFromBoundary);
		activity.getAttributes().putAttribute(ROUTED_TRAVEL_TIME_FROM_BOUNDARY, routedTravelTimeFromBoundary);
	}

	private static RouteMetrics getRouteMetrics(List<Link> links) {
		double distance = 0;
		double travelTime = 0;
		for (Link link : links) {
			distance += link.getLength();
			travelTime += getLinkTravelTime(link);
		}
		return new RouteMetrics(distance, travelTime);
	}

	private static double getLinkTravelTime(Link link) {
		return Math.floor(NetworkUtils.getFreespeedTravelTime(link)) + 1;
	}

	private record RouteMetrics(double distance, double travelTime) {}

	private record ResolvedActivityLocation(Activity activity, Coord originalCoord, Link routingLink, Link scenarioLink) {}

	private record BoundaryCacheKey(Id<Link> linkId, BoundaryCrossing crossing) {}
}
