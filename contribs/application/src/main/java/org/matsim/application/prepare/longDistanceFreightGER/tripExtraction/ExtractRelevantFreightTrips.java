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
import java.util.HashSet;
import java.util.List;
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

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to country wide freight traffic")
	private Path freightDataDirectory;

	@CommandLine.Option(names = "--network", description = "Path to network file", required = true)
	private Path networkPath;

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
		config.network().setInputFile(networkPath.toString());
		Scenario outputScenario = ScenarioUtils.loadScenario(config);
		config.plans().setInputFile(freightDataDirectory.toString());
		config.routing().setRoutingRandomness(0);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		Population originalPlans = scenario.getPopulation();
		Population outputPlans = outputScenario.getPopulation();
		PopulationFactory populationFactory = outputPlans.getFactory();

		// Create router
		FreeSpeedTravelTime travelTime = new FreeSpeedTravelTime();
		LeastCostPathCalculatorFactory fastAStarLandmarksFactory = new SpeedyALTFactory();
		RandomizingTimeDistanceTravelDisutilityFactory disutilityFactory = new RandomizingTimeDistanceTravelDisutilityFactory(
				"car", config);
		TravelDisutility travelDisutility = disutilityFactory.createTravelDisutility(travelTime);
		LeastCostPathCalculator router = fastAStarLandmarksFactory.createPathCalculator(network, travelDisutility,
				travelTime);

		// Reading Shape file
		Geometry relevantArea = shp.getGeometry();

		CoordinateTransformation sct = shp.createTransformation(crs.getInputCRS());

		log.info("Filtering the links within the relevant area...");
		// Identify links on the boundary
		Set<Id<Link>> linksOnTheBoundary = new HashSet<>();
		for (Link link : network.getLinks().values()) {
			Coord fromCoord = sct.transform(link.getFromNode().getCoord());
			Coord toCoord = sct.transform(link.getToNode().getCoord());
			if (relevantArea.contains(MGC.coord2Point(fromCoord)) ^ relevantArea.contains(MGC.coord2Point(toCoord))) {
				linksOnTheBoundary.add(link.getId());
			}
		}
		log.info("Finished filtering the links within the relevant area...");

		CoordinateTransformation ct = crs.getTransformation();

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
			Id<Link> startLink = startActivity.getLinkId();
			Id<Link> endLink = endActivity.getLinkId();
			Coord startCoord = startActivity.getCoord();
			Coord endCoord = endActivity.getCoord();
			double departureTime = startActivity.getEndTime().orElse(0);

			boolean originIsInside = relevantArea.contains(MGC.coord2Point(sct.transform(startCoord))) || linksOnTheBoundary.contains(startLink);
			boolean destinationIsInside = relevantArea.contains(MGC.coord2Point(sct.transform(endCoord))) || linksOnTheBoundary.contains(endLink);

			Activity act0 = populationFactory.createActivityFromCoord("freight_start", null);
			Leg leg = populationFactory.createLeg(legMode);
			Activity act1 = populationFactory.createActivityFromCoord("freight_end", null);
			String geographicalTripTypeAttribute;

			switch (geographicalTripType) {
				case ALL -> {
					geographicalTripTypeAttribute = createActivitiesForInternalTrips(originIsInside, destinationIsInside, act0, ct, startCoord, act1, endCoord);
					if (geographicalTripTypeAttribute == null) {
						geographicalTripTypeAttribute = createActivitiesForOutgoingTrip(originIsInside, destinationIsInside, act0, ct, startCoord, router, network,
							startLink, endLink, linksOnTheBoundary, act1, endCoord);
					}
					if (geographicalTripTypeAttribute == null) {
						geographicalTripTypeAttribute = createActivitiesForIncomingTrips(originIsInside, destinationIsInside, act0, ct, startCoord, router, network,
							startLink, endLink, linksOnTheBoundary, act1, endCoord);
					}
					if (geographicalTripTypeAttribute == null) {
						geographicalTripTypeAttribute = createActivitiesForTransitTrip(originIsInside, destinationIsInside, act0, ct, startCoord, router, network,
							startLink, endLink, linksOnTheBoundary, act1, endCoord);
					}
				}
				case INTERNAL ->
					geographicalTripTypeAttribute = createActivitiesForInternalTrips(originIsInside, destinationIsInside, act0, ct, startCoord, act1, endCoord);
				case OUTGOING ->
					geographicalTripTypeAttribute = createActivitiesForOutgoingTrip(originIsInside, destinationIsInside, act0, ct, startCoord, router, network,
						startLink, endLink,	linksOnTheBoundary, act1, endCoord);
				case INCOMING ->
					geographicalTripTypeAttribute = createActivitiesForIncomingTrips(originIsInside, destinationIsInside, act0, ct, startCoord, router, network,
						startLink, endLink,	linksOnTheBoundary, act1, endCoord);
				case TRANSIT ->
					geographicalTripTypeAttribute = createActivitiesForTransitTrip(originIsInside, destinationIsInside, act0, ct, startCoord, router, network,
						startLink, endLink,	linksOnTheBoundary, act1, endCoord);
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
	private String createActivitiesForTransitTrip(boolean originIsInside, boolean destinationIsInside, Activity act0, CoordinateTransformation ct, Coord startCoord,
												LeastCostPathCalculator router, Network network, Id<Link> startLink, Id<Link> endLink,
												Set<Id<Link>> linksOnTheBoundary, Activity act1, Coord endCoord) {
		if (!originIsInside && !destinationIsInside) {
			boolean vehicleIsInside = false;
			LeastCostPathCalculator.Path route = router.calcLeastCostPath(
					network.getLinks().get(startLink), network.getLinks().get(endLink), 0,
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
							act0.setCoord(ct.transform(link.getCoord()));
							addBoundaryAttributesToStartActivity(act0, distanceFromStartToBoundary, travelTimeFromStartToBoundary);
							vehicleIsInside = true;
						} else {
							act1.setCoord(ct.transform(link.getCoord()));
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
						act0.setCoord(ct.transform(startCoord));
						act1.setCoord(ct.transform(endCoord));
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
	private String createActivitiesForIncomingTrips(boolean originIsInside, boolean destinationIsInside, Activity act0, CoordinateTransformation ct, Coord startCoord,
												  LeastCostPathCalculator router, Network network, Id<Link> startLink, Id<Link> endLink,
												  Set<Id<Link>> linksOnTheBoundary, Activity act1, Coord endCoord) {
		if (!originIsInside && destinationIsInside) {
			if (cutOnBoundary) {
				boolean isCoordSet = false;
				LeastCostPathCalculator.Path route = router.calcLeastCostPath(
						network.getLinks().get(startLink), network.getLinks().get(endLink),
						0, null, null);
				if (route.links.isEmpty()) {
					return null;
				}
				double travelTimeFromStartToBoundary = 0;
				double distanceFromStartToBoundary = 0;
				for (Link link : route.links) {
					if (linksOnTheBoundary.contains(link.getId())) {
						act0.setCoord(ct.transform(link.getCoord()));
						addBoundaryAttributesToStartActivity(act0, distanceFromStartToBoundary, travelTimeFromStartToBoundary);
						isCoordSet = true;
						break;
					}
					travelTimeFromStartToBoundary += getLinkTravelTime(link);
					distanceFromStartToBoundary += link.getLength();
				}
				if (!isCoordSet) {
					Coord originalCoord = route.links.getFirst().getCoord();
					act0.setCoord(ct.transform(originalCoord));
				}
			} else {
				act0.setCoord(ct.transform(startCoord));
			}
			act1.setCoord(ct.transform(endCoord));
			return "incoming";
		}
		return null;
	}

	/**
	 * Create activities if the trip is an outgoing trip
	 */
	private String createActivitiesForOutgoingTrip(boolean originIsInside, boolean destinationIsInside, Activity act0, CoordinateTransformation ct, Coord startCoord,
												 LeastCostPathCalculator router, Network network, Id<Link> startLink, Id<Link> endLink,
												 Set<Id<Link>> linksOnTheBoundary, Activity act1, Coord endCoord) {
		if (originIsInside && !destinationIsInside) {
			act0.setCoord(ct.transform(startCoord));
			if (cutOnBoundary) {
				boolean isCoordSet = false;
				LeastCostPathCalculator.Path route = router.calcLeastCostPath(
					network.getLinks().get(startLink), network.getLinks().get(endLink),
					0, null, null);
				if (route.links.isEmpty()) {
					return null;
				}
				RouteMetrics routeMetrics = getRouteMetrics(route.links);
				double travelTimeFromStartToBoundary = 0;
				double distanceFromStartToBoundary = 0;
				for (Link link : route.links) {
					if (linksOnTheBoundary.contains(link.getId())) {
						act1.setCoord(ct.transform(link.getCoord()));
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
					Coord originalCoord = route.links.get(lastOne).getCoord();
					act1.setCoord(ct.transform(originalCoord));
				}
			} else {
				act1.setCoord(ct.transform(endCoord));
			}
			return "outgoing";
		}
		return null;
	}

	/**
	 * Create activities if the trip is an internal trip
	 */
	private static String createActivitiesForInternalTrips(boolean originIsInside, boolean destinationIsInside, Activity act0, CoordinateTransformation ct, Coord startCoord,
														 Activity act1, Coord endCoord) {
		if (originIsInside && destinationIsInside) {
			act0.setCoord(ct.transform(startCoord));
			act1.setCoord(ct.transform(endCoord));
			return "internal";
		}
		return null;
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
		return Math.floor(link.getLength() / link.getFreespeed()) + 1;
	}

	private record RouteMetrics(double distance, double travelTime) {
	}
}
