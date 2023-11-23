package org.matsim.application.prepare.freight.tripExtraction;

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
import picocli.CommandLine;

import java.io.FileWriter;
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

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to country wide freight traffic")
	private Path freightDataDirectory;

	@CommandLine.Option(names = "--network", description = "Path to network file", required = true)
	private Path networkPath;

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	@CommandLine.Mixin
	private CrsOptions crs = new CrsOptions();

	@CommandLine.Option(names = "--output", description = "Output path", required = true)
	private Path outputPath;

	@CommandLine.Option(names = "--cut-on-boundary", description = "Cut trips on shape-file boundary", defaultValue = "false")
	private boolean cutOnBoundary;

	private final SplittableRandom rnd = new SplittableRandom(4711);

	private final List<Coord> fromCoords = new ArrayList<>();
	private final List<Coord> toCoords = new ArrayList<>();

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

		// Identify links on the boundary
		List<Id<Link>> linksOnTheBoundary = new ArrayList<>();
		for (Link link : network.getLinks().values()) {
			Coord fromCoord = sct.transform(link.getFromNode().getCoord());
			Coord toCoord = sct.transform(link.getToNode().getCoord());
			if (relevantArea.contains(MGC.coord2Point(fromCoord)) ^ relevantArea.contains(MGC.coord2Point(toCoord))) {
				linksOnTheBoundary.add(link.getId());
			}
		}

		CoordinateTransformation ct = crs.getTransformation();

		// Create modified population
		int generated = 0;
		int processed = 0;
		log.info("Start creating the modified plans: there are in total {} persons to be processed",
				originalPlans.getPersons().keySet().size());

		for (Person person : originalPlans.getPersons().values()) {

			processed += 1;
			if (processed % 10000 == 0) {
				log.info("Processing: {} persons have been processed", processed);
			}

			Plan plan = person.getSelectedPlan();
//            String goodType = (String) person.getAttributes().getAttribute("type_of_good"); // TODO keep all the attribute from original population
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
			Leg leg = populationFactory.createLeg("freight");
			Activity act1 = populationFactory.createActivityFromCoord("freight_end", null);

			// Case 1: both origin and destination are within the relevant region
			if (originIsInside && destinationIsInside) {
				act0.setCoord(ct.transform(startCoord));
				act0.setEndTime(departureTime);
				act1.setCoord(ct.transform(endCoord));
			}

			// Case 2: outgoing trips
			if (originIsInside && !destinationIsInside) {
				act0.setCoord(ct.transform(startCoord));
				act0.setEndTime(departureTime);
				if (cutOnBoundary) {
					boolean isCoordSet = false;
					LeastCostPathCalculator.Path route = router.calcLeastCostPath(
							network.getLinks().get(startLink).getToNode(), network.getLinks().get(endLink).getToNode(),
							0, null, null);
					if (route.links.size() == 0) {
						continue;
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

			}
			// Case 3: incoming trips
			if (!originIsInside && destinationIsInside) {
				if (cutOnBoundary) {
					boolean isCoordSet = false;
					LeastCostPathCalculator.Path route = router.calcLeastCostPath(
							network.getLinks().get(startLink).getToNode(), network.getLinks().get(endLink).getToNode(),
							0, null, null);
					if (route.links.size() == 0) {
						continue;
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
						Coord originalCoord = route.links.get(0).getCoord();
						act0.setCoord(ct.transform(originalCoord));
						act0.setEndTime(departureTime);
					}
				} else {
					act0.setCoord(ct.transform(startCoord));
					act0.setEndTime(departureTime);
				}
				act1.setCoord(ct.transform(endCoord));
			}

			// case 4: through trips
			if (!originIsInside && !destinationIsInside) {
				boolean tripIsRelevant = false;
				double timeSpent = 0;
				boolean vehicleIsInside = false;
				LeastCostPathCalculator.Path route = router.calcLeastCostPath(
						network.getLinks().get(startLink).getToNode(), network.getLinks().get(endLink).getToNode(), 0,
						null, null);
				if (route.links.size() == 0) {
					continue;
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
								tripIsRelevant = true;
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
							tripIsRelevant = true;
							break;
						}
					}
				}
				if (!tripIsRelevant) {
					continue;
				}
			}

			// Add new freight person to the output plans
			if (act0.getEndTime().orElse(86400) < 86400) {
				Person freightPerson = populationFactory.createPerson(Id.create("freight_" + generated, Person.class));
				freightPerson.getAttributes().putAttribute("subpopulation", "freight");
				Plan freightPersonPlan = populationFactory.createPlan();
				freightPersonPlan.addActivity(act0);
				freightPersonPlan.addLeg(leg);
				freightPersonPlan.addActivity(act1);
				freightPerson.addPlan(freightPersonPlan);
				outputPlans.addPerson(freightPerson);
				generated += 1;

				fromCoords.add(act0.getCoord());
				toCoords.add(act1.getCoord());
			}
		}

		if (crs.getTargetCRS() != null)
			ProjectionUtils.putCRS(originalPlans, crs.getTargetCRS());

		// Write population
		log.info("Writing population file...");
		if (!Files.exists(outputPath.getParent())) {
			Files.createDirectory(outputPath.getParent());
		}

		PopulationWriter pw = new PopulationWriter(outputPlans);
		pw.write(outputPath.toString());

		String resultSummaryPath = outputPath.toString().replace(".gz", "").replace(".xml", "")
				+ "-locations-summary.tsv";
		CSVPrinter tsvWriter = new CSVPrinter(new FileWriter(resultSummaryPath), CSVFormat.TDF);
		tsvWriter.printRecord("trip_id", "from_x", "from_y", "to_x", "to_y");
		for (int i = 0; i < fromCoords.size(); i++) {
			Coord fromCoord = fromCoords.get(i);
			Coord toCoord = toCoords.get(i);
			List<String> outputRow = new ArrayList<>();
			outputRow.add(Integer.toString(i + 1));
			outputRow.add(Double.toString(fromCoord.getX()));
			outputRow.add(Double.toString(fromCoord.getY()));
			outputRow.add(Double.toString(toCoord.getX()));
			outputRow.add(Double.toString(toCoord.getY()));
			tsvWriter.printRecord(outputRow);
		}
		tsvWriter.close();

		return 0;
	}
}
