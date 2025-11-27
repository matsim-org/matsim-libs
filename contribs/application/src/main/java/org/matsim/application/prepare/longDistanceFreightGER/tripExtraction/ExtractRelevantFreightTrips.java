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
import org.matsim.utils.objectattributes.attributable.Attributes;
import picocli.CommandLine;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

@CommandLine.Command(name = "extract-freight-trips", description = "Extract subset of freight trips from whole population", showDefaultValues = true)
public class ExtractRelevantFreightTrips implements MATSimAppCommand {
	// This script will extract the relevant freight trips within the given shape
	// file from the German wide freight traffic.
	private static final Logger log = LogManager.getLogger(ExtractRelevantFreightTrips.class);
	public static final String GEOGRAPHICAL_TRIP_TYPE = "geographical_Trip_Type";

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

	@CommandLine.Option(names = "--legMode", description = "Set leg mode for long distance freight legs.", defaultValue = "freight")
	private String legMode;

	@CommandLine.Option(names = "--subpopulation", description = "Set subpopulation for the extracted freight trips", defaultValue = "freight")
	private String subpopulation;

	private final SplittableRandom rnd = new SplittableRandom(4711);

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
		List<Id<Link>> linksOnTheBoundary = new ArrayList<>();
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
            Attributes attributes = person.getAttributes();
			// By default, the plan of each freight person consist of only 3 elements:
			// startAct, leg, endAct
			Activity startActivity = (Activity) plan.getPlanElements().get(0);
			Activity endActivity = (Activity) plan.getPlanElements().get(2);
			Id<Link> startLink = startActivity.getLinkId();
			Id<Link> endLink = endActivity.getLinkId();
			Coord startCoord = startActivity.getCoord();
			Coord endCoord = endActivity.getCoord();
			double departureTime = startActivity.getEndTime().orElse(0);

			boolean originIsInside = relevantArea.contains(MGC.coord2Point(sct.transform(startCoord)));
			boolean destinationIsInside = relevantArea.contains(MGC.coord2Point(sct.transform(endCoord)));

			Activity act0 = populationFactory.createActivityFromCoord("freight_start", null);
			Leg leg = populationFactory.createLeg(legMode);
			Activity act1 = populationFactory.createActivityFromCoord("freight_end", null);

			switch (geographicalTripType) {
				case ALL -> {
					createActivitiesForInternalTrips(originIsInside, destinationIsInside, act0, ct, startCoord, departureTime, act1, endCoord, attributes);
					createActivitiesForOutgoingTrip(originIsInside, destinationIsInside, act0, ct, startCoord, departureTime, router, network,
						startLink, endLink,
						linksOnTheBoundary, act1, endCoord, attributes);
					createActivitiesForIncomingTrips(originIsInside, destinationIsInside, act0, ct, startCoord, departureTime, router, network,
						startLink, endLink, linksOnTheBoundary, act1, endCoord, attributes);
					createActivitiesForTransitTrip(originIsInside, destinationIsInside, act0, ct, startCoord, departureTime, router, network,
						startLink, endLink, linksOnTheBoundary, act1, endCoord, attributes);
				}
				case INTERNAL ->
					createActivitiesForInternalTrips(originIsInside, destinationIsInside, act0, ct, startCoord, departureTime, act1, endCoord,
						attributes);
				case OUTGOING ->
					createActivitiesForOutgoingTrip(originIsInside, destinationIsInside, act0, ct, startCoord, departureTime, router, network,
						startLink, endLink,	linksOnTheBoundary, act1, endCoord, attributes);
				case INCOMING ->
					createActivitiesForIncomingTrips(originIsInside, destinationIsInside, act0, ct, startCoord, departureTime, router, network,
						startLink, endLink,	linksOnTheBoundary, act1, endCoord, attributes);
				case TRANSIT ->
					createActivitiesForTransitTrip(originIsInside, destinationIsInside, act0, ct, startCoord, departureTime, router, network,
						startLink, endLink,	linksOnTheBoundary, act1, endCoord, attributes);
				default -> throw new IllegalStateException("Unexpected value: " + geographicalTripType);
			}

			// Add new freight person to the output plans if trips is relevant
			if (act0.getEndTime().orElse(86400) < 86400) {
				Person freightPerson = populationFactory.createPerson(Id.create("freight_" + generated, Person.class));
				attributes.getAsMap().forEach(freightPerson.getAttributes()::putAttribute);
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
		if (!Files.exists(outputPath.getParent())) {
			Files.createDirectory(outputPath.getParent());
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
	private void createActivitiesForTransitTrip(boolean originIsInside, boolean destinationIsInside, Activity act0, CoordinateTransformation ct, Coord startCoord,
												double departureTime, LeastCostPathCalculator router, Network network, Id<Link> startLink, Id<Link> endLink,
												List<Id<Link>> linksOnTheBoundary, Activity act1, Coord endCoord, Attributes attributes) {
		if (!originIsInside && !destinationIsInside) {
			double timeSpent = 0;
			boolean vehicleIsInside = false;
			LeastCostPathCalculator.Path route = router.calcLeastCostPath(
					network.getLinks().get(startLink), network.getLinks().get(endLink), 0,
					null, null);
			if (route.links.isEmpty()) {
				return;
			}
			if (cutOnBoundary) {
				for (Link link : route.links) {
					if (linksOnTheBoundary.contains(link.getId())) {
						if (!vehicleIsInside) {
							act0.setCoord(ct.transform(link.getCoord()));
							double newEndTime = departureTime + timeSpent;
							if (newEndTime >= 24 * 3600)
								newEndTime = rnd.nextInt(86400);
							act0.setEndTime(newEndTime);
							vehicleIsInside = true;
						} else {
							act1.setCoord(ct.transform(link.getCoord()));
							break;
						}
					}
					timeSpent += Math.floor(link.getLength() / link.getFreespeed()) + 1;
				}
			} else {
				for (Link link : route.links) {
					if (linksOnTheBoundary.contains(link.getId())) {
						act0.setCoord(ct.transform(startCoord));
						act0.setEndTime(departureTime);
						act1.setCoord(ct.transform(endCoord));
						break;
					}
				}
			}
			attributes.putAttribute(GEOGRAPHICAL_TRIP_TYPE, "transit");
		}
	}

	/**
	 * Create activities if the trip is an incoming trip
	 */
	private void createActivitiesForIncomingTrips(boolean originIsInside, boolean destinationIsInside, Activity act0, CoordinateTransformation ct, Coord startCoord,
												  double departureTime, LeastCostPathCalculator router, Network network, Id<Link> startLink, Id<Link> endLink,
												  List<Id<Link>> linksOnTheBoundary, Activity act1, Coord endCoord, Attributes attributes) {
		if (!originIsInside && destinationIsInside) {
			if (cutOnBoundary) {
				boolean isCoordSet = false;
				LeastCostPathCalculator.Path route = router.calcLeastCostPath(
						network.getLinks().get(startLink), network.getLinks().get(endLink),
						0, null, null);
				if (route.links.isEmpty()) {
					return;
				}
				double timeSpent = 0;
				for (Link link : route.links) {
					if (linksOnTheBoundary.contains(link.getId())) {
						act0.setCoord(ct.transform(link.getCoord()));
						double newEndTime = departureTime + timeSpent;
						if (newEndTime >= 86400)
							newEndTime = rnd.nextInt(86400);
						act0.setEndTime(newEndTime);
						isCoordSet = true;
						break;
					}
					timeSpent += Math.floor(link.getLength() / link.getFreespeed()) + 1;
				}
				if (!isCoordSet) {
					Coord originalCoord = route.links.getFirst().getCoord();
					act0.setCoord(ct.transform(originalCoord));
					act0.setEndTime(departureTime);
				}
			} else {
				act0.setCoord(ct.transform(startCoord));
				act0.setEndTime(departureTime);
			}
			act1.setCoord(ct.transform(endCoord));
			attributes.putAttribute(GEOGRAPHICAL_TRIP_TYPE, "incoming");
		}
	}

	/**
	 * Create activities if the trip is an outgoing trip
	 */
	private void createActivitiesForOutgoingTrip(boolean originIsInside, boolean destinationIsInside, Activity act0, CoordinateTransformation ct, Coord startCoord,
												 double departureTime, LeastCostPathCalculator router, Network network, Id<Link> startLink, Id<Link> endLink,
												 List<Id<Link>> linksOnTheBoundary, Activity act1, Coord endCoord, Attributes attributes) {
		if (originIsInside && !destinationIsInside) {
			act0.setCoord(ct.transform(startCoord));
			act0.setEndTime(departureTime);
			if (cutOnBoundary) {
				boolean isCoordSet = false;
				LeastCostPathCalculator.Path route = router.calcLeastCostPath(
					network.getLinks().get(startLink), network.getLinks().get(endLink),
					0, null, null);
				if (route.links.isEmpty()) {
					return;
				}
				for (Link link : route.links) {
					if (linksOnTheBoundary.contains(link.getId())) {
						act1.setCoord(ct.transform(link.getCoord()));
						isCoordSet = true;
						break;
					}
				}
				if (!isCoordSet) {
					int lastOne = route.links.size() - 1;
					Coord originalCoord = route.links.get(lastOne).getCoord();
					act1.setCoord(ct.transform(originalCoord));
				}
			} else {
				act1.setCoord(ct.transform(endCoord));
			}
			attributes.putAttribute(GEOGRAPHICAL_TRIP_TYPE, "outgoing");
		}
	}

	/**
	 * Create activities if the trip is an internal trip
	 */
	private static void createActivitiesForInternalTrips(boolean originIsInside, boolean destinationIsInside, Activity act0, CoordinateTransformation ct, Coord startCoord,
														 double departureTime, Activity act1, Coord endCoord, Attributes attributes) {
		if (originIsInside && destinationIsInside) {
			act0.setCoord(ct.transform(startCoord));
			act0.setEndTime(departureTime);
			act1.setCoord(ct.transform(endCoord));
			attributes.putAttribute(GEOGRAPHICAL_TRIP_TYPE, "internal");
		}
	}
}
