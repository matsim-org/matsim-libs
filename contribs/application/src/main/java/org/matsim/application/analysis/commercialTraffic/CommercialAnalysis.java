package org.matsim.application.analysis.commercialTraffic;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.matsim.application.ApplicationUtils.globFile;

@CommandLine.Command(
	name = "commercial-traffic-analysis", description = "General commercial traffic analysis.",
	mixinStandardHelpOptions = true, showDefaultValues = true
)
@CommandSpec(requireRunDirectory = true,
	produces = {
		"commercialTraffic_generalTravelData.csv",
		"commercialTraffic_link_volume.csv",
		"commercialTraffic_travelDistancesShares.csv",
		"commercialTraffic_travelDistancesShares_perMode.csv",
		"commercialTraffic_travelDistancesShares_perSubpopulation.csv",
		"commercialTraffic_travelDistancesShares_perType.csv",
		"commercialTraffic_travelDistances_perVehicle.csv",
		"commercialTraffic_relations.csv",
		"commercialTraffic_tour_durations.csv"
	}
)
public class CommercialAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CommercialAnalysis.class);

	private Map<String, List<String>> groupsOfSubpopulationsForCommercialAnalysis = new HashMap<>();
	//TODO make plans configurable
	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(CommercialAnalysis.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(CommercialAnalysis.class);
	@CommandLine.Mixin
	private ShpOptions shp;

	@CommandLine.Option(names = "--sampleSize", description = "The sample size of the simulation.")
	private static double sampleSize;
	@CommandLine.Option(names = "--groups-of-subpopulations-commercialAnalysis", description = "Define the subpopulations for the analysis of the commercial traffic and defines if the have different groups. If a group is defined by several subpopulations," +
		"split them by ','. and different groups are seperated by ';'. The analysis output will be for the given groups.", split = ";")
	private final Map<String, String> groupsOfSubpopulationsForCommercialAnalysisRaw = new HashMap<>();

	public static void main(String[] args) {
		new CommercialAnalysis().execute(args);
	}

	public Integer call() throws Exception {
		log.info("++++++++++++++++++ Start Analysis for RVR Freight simulations ++++++++++++++++++++++++++++");

		final String eventsFile = globFile(input.getRunDirectory(), "*output_events*").toString();

		if (!groupsOfSubpopulationsForCommercialAnalysisRaw.isEmpty())
			groupsOfSubpopulationsForCommercialAnalysis = getGroupsOfSubpopulations(groupsOfSubpopulationsForCommercialAnalysisRaw);
		// for SimWrapper
		final Path linkDemandOutputFile = output.getPath("commercialTraffic_link_volume.csv");
		log.info("Writing volume per link to: {}", linkDemandOutputFile);

		final Path travelDistancesPerModeOutputFile = output.getPath("commercialTraffic_travelDistancesShares.csv");
		log.info("Writing travel distances per mode to: {}", travelDistancesPerModeOutputFile);

		final Path travelDistancesPerVehicleOutputFile = output.getPath("commercialTraffic_travelDistances_perVehicle.csv");
		log.info("Writing travel distances per vehicle to: {}", travelDistancesPerVehicleOutputFile);

		final Path relationsOutputFile = output.getPath("commercialTraffic_relations.csv");
		log.info("Writing relations to: {}", relationsOutputFile);

		final Path tourDurationsOutputFile = output.getPath("commercialTraffic_tour_durations.csv");
		log.info("Writing tour durations to: {}", tourDurationsOutputFile);

		final Path generalTravelDataOutputFile = output.getPath("commercialTraffic_generalTravelData.csv");
		log.info("Writing general travel data to: {}", generalTravelDataOutputFile);

		Config config = ConfigUtils.createConfig();
		config.vehicles().setVehiclesFile(globFile(input.getRunDirectory(), "*output_vehicles*").toString());
		config.network().setInputFile(globFile(input.getRunDirectory(), "*output_network*").toString());

		config.global().setCoordinateSystem(null);
		config.plans().setInputFile(globFile(input.getRunDirectory(), "*output_plans*").toString());
		config.eventsManager().setNumberOfThreads(null);
		config.eventsManager().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(4);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		EventsManager eventsManager = EventsUtils.createEventsManager();

		// link events handler
		LinkVolumeCommercialEventHandler linkDemandEventHandler = new LinkVolumeCommercialEventHandler(scenario, sampleSize, shp, groupsOfSubpopulationsForCommercialAnalysis);
		eventsManager.addHandler(linkDemandEventHandler);

		eventsManager.initProcessing();

		log.info("-------------------------------------------------");
		log.info("Done reading the events file");
		log.info("Finish processing...");
		eventsManager.finishProcessing();
		new MatsimEventsReader(eventsManager).readFile(eventsFile);
		log.info("Closing events file...");

		createGeneralTravelDataAnalysis(generalTravelDataOutputFile, linkDemandEventHandler, scenario);
		createTravelDistancesShares(travelDistancesPerModeOutputFile, linkDemandEventHandler);
		createLinkVolumeAnalysis(scenario, linkDemandOutputFile, linkDemandEventHandler);
		createRelationsAnalysis(relationsOutputFile, linkDemandEventHandler);
		createAnalysisPerVehicle(travelDistancesPerVehicleOutputFile, linkDemandEventHandler);
		createTourDurationPerVehicle(tourDurationsOutputFile, linkDemandEventHandler, scenario);

		log.info("Done");
		log.info("All outputs of commercial analysis written to {}", output.getPath());
		log.info("-------------------------------------------------");
		return 0;
	}

	private void createTourDurationPerVehicle(Path tourDurationsOutputFile, LinkVolumeCommercialEventHandler linkDemandEventHandler,
											  Scenario scenario) {

		HashMap<Id<Vehicle>, Double> tourDurations = linkDemandEventHandler.getTourDurationPerPerson();
		HashMap<Id<Vehicle>, Id<Person>> vehicleToPersonId = linkDemandEventHandler.getVehicleIdToPersonId();

		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(tourDurationsOutputFile), CSVFormat.DEFAULT)) {

			printer.print("personId");
			printer.print("vehicleId");
			printer.print("subpopulation");
			printer.print("tourDurationInSeconds");
			printer.println();

			for (Id<Vehicle> vehicleId : tourDurations.keySet()) {
				Id<Person> personId = vehicleToPersonId.get(vehicleId);
				printer.print(personId);
				printer.print(vehicleId);
				printer.print(scenario.getPopulation().getPersons().get(personId).getAttributes().getAttribute("subpopulation"));
				printer.print(tourDurations.get(vehicleId));
				printer.println();
			}
		} catch (IOException e) {
			log.error("Could not create output file", e);
		}
	}

	private void createGeneralTravelDataAnalysis(Path generalTravelDataOutputFile, LinkVolumeCommercialEventHandler linkDemandEventHandler, Scenario scenario) {

		HashMap<Id<Vehicle>, String> groupOfVehicles = linkDemandEventHandler.getGroupOfRelevantVehicles();
		Map<String, List<Id<Vehicle>>> vehiclesPerGroup = groupOfVehicles.entrySet().stream()
			.collect(Collectors.groupingBy(
				Map.Entry::getValue,
				Collectors.mapping(Map.Entry::getKey, Collectors.toList())
			));
		HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_internal = linkDemandEventHandler.getDistancesPerTrip_perPerson_internal();
		HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_incoming = linkDemandEventHandler.getDistancesPerTrip_perPerson_incoming();
		HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_outgoing = linkDemandEventHandler.getDistancesPerTrip_perPerson_outgoing();
		HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_transit = linkDemandEventHandler.getDistancesPerTrip_perPerson_transit();
		HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_all = linkDemandEventHandler.getDistancesPerTrip_perPerson_all();

		HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_internal_inInvestigationArea = linkDemandEventHandler.getDistancesPerTrip_perPerson_internal_inInvestigationArea();
		HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_incoming_inInvestigationArea = linkDemandEventHandler.getDistancesPerTrip_perPerson_incoming_inInvestigationArea();
		HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_outgoing_inInvestigationArea = linkDemandEventHandler.getDistancesPerTrip_perPerson_outgoing_inInvestigationArea();
		HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_transit_inInvestigationArea = linkDemandEventHandler.getDistancesPerTrip_perPerson_transit_inInvestigationArea();
		HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_all_inInvestigationArea = linkDemandEventHandler.getDistancesPerTrip_perPerson_all_inInvestigationArea();

		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(generalTravelDataOutputFile), CSVFormat.DEFAULT)) {
			// _Intern: internal trips (start and end inside the area)
			// _Incoming: incoming trips (start outside the area and end inside the area)
			// _Outgoing: incoming trips (start inside the area and end outside the area)
			// _Transit: transit trips (start and end inside the area)
			// _all: all trips
			printer.print("group");
			printer.print("numberOfAgents");

			if (shp.isDefined()) {
				printer.print("numberOfTrips_Intern");
				printer.print("numberOfTrips_Incoming");
				printer.print("numberOfTrips_Outgoing");
				printer.print("numberOfTrips_Transit");
			}
			printer.print("numberOfTrips_all");

			if (shp.isDefined()) {
				printer.print("traveledDistance_Intern");
				printer.print("traveledDistance_Incoming");
				printer.print("traveledDistance_Outgoing");
				printer.print("traveledDistance_Transit");
			}
			printer.print("traveledDistance_all");
			if (shp.isDefined()) {
				printer.print("traveledDistanceInInvestigationArea_Intern");
				printer.print("traveledDistanceInInvestigationArea_Incoming");
				printer.print("traveledDistanceInInvestigationArea_Outgoing");
				printer.print("traveledDistanceInInvestigationArea_Transit");
				printer.print("traveledDistanceInInvestigationArea_all");
			}
//			printer.print("averageTripsPerAgent_Intern;"); //TODO why this commented out
//			printer.print("averageTripsPerAgent_Incoming;");
//			printer.print("averageTripsPerAgent_Outgoing;");
//			printer.print("averageTripsPerAgent_Transit;");
			printer.print("averageTripsPerAgent_all");
			if (shp.isDefined()) {
				printer.print("averageDistancePerTrip_Intern");
				printer.print("averageDistancePerTrip_Incoming");
				printer.print("averageDistancePerTrip_Outgoing");
				printer.print("averageDistancePerTrip_Transit");
			}
			printer.print("averageDistancePerTrip_all");
			printer.println();

			for (String group : vehiclesPerGroup.keySet()){
				printer.print(group);
				int numberOfAgentsInSubpopulation = vehiclesPerGroup.get(group).size();
				printer.print(numberOfAgentsInSubpopulation);

				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_internal_perSubpopulation = filterByPopulationGroup(distancesPerTrip_perPerson_internal, group, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_incoming_perSubpopulation = filterByPopulationGroup(distancesPerTrip_perPerson_incoming, group, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_outgoing_perSubpopulation = filterByPopulationGroup(distancesPerTrip_perPerson_outgoing, group, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_transit_perSubpopulation = filterByPopulationGroup(distancesPerTrip_perPerson_transit, group, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_all_perSubpopulation = filterByPopulationGroup(distancesPerTrip_perPerson_all, group, scenario);

				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_internal_inInvestigationArea_perSubpopulation = filterByPopulationGroup(distancesPerTrip_perPerson_internal_inInvestigationArea, group, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_incoming_inInvestigationArea_perSubpopulation = filterByPopulationGroup(distancesPerTrip_perPerson_incoming_inInvestigationArea, group, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_outgoing_inInvestigationArea_perSubpopulation = filterByPopulationGroup(distancesPerTrip_perPerson_outgoing_inInvestigationArea, group, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_transit_inInvestigationArea_perSubpopulation = filterByPopulationGroup(distancesPerTrip_perPerson_transit_inInvestigationArea, group, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_all_inInvestigationArea_perSubpopulation = filterByPopulationGroup(distancesPerTrip_perPerson_all_inInvestigationArea, group, scenario);

				int numberOfTrips_internal = distancesPerTrip_perPerson_internal_perSubpopulation.values().stream().mapToInt(List::size).sum();
				int numberOfTrips_incoming = distancesPerTrip_perPerson_incoming_perSubpopulation.values().stream().mapToInt(List::size).sum();
				int numberOfTrips_outgoing = distancesPerTrip_perPerson_outgoing_perSubpopulation.values().stream().mapToInt(List::size).sum();
				int numberOfTrips_transit = distancesPerTrip_perPerson_transit_perSubpopulation.values().stream().mapToInt(List::size).sum();
				int numberOfTrips_all = distancesPerTrip_perPerson_all_perSubpopulation.values().stream().mapToInt(List::size).sum();

				if (shp.isDefined()) {
					printer.print(numberOfTrips_internal);
					printer.print(numberOfTrips_incoming);
					printer.print(numberOfTrips_outgoing);
					printer.print(numberOfTrips_transit);
				}
				printer.print(numberOfTrips_all);

				double traveledDistance_internal = distancesPerTrip_perPerson_internal_perSubpopulation.values().stream().flatMapToDouble(list -> list.stream().mapToDouble(Double::doubleValue)).sum();
				double traveledDistance_incoming = distancesPerTrip_perPerson_incoming_perSubpopulation.values().stream().flatMapToDouble(list -> list.stream().mapToDouble(Double::doubleValue)).sum();
				double traveledDistance_outgoing = distancesPerTrip_perPerson_outgoing_perSubpopulation.values().stream().flatMapToDouble(list -> list.stream().mapToDouble(Double::doubleValue)).sum();
				double traveledDistance_transit = distancesPerTrip_perPerson_transit_perSubpopulation.values().stream().flatMapToDouble(list -> list.stream().mapToDouble(Double::doubleValue)).sum();
				double traveledDistance_all = distancesPerTrip_perPerson_all_perSubpopulation.values().stream().flatMapToDouble(list -> list.stream().mapToDouble(Double::doubleValue)).sum();

				if (shp.isDefined()) {
					printer.print(traveledDistance_internal);
					printer.print(traveledDistance_incoming);
					printer.print(traveledDistance_outgoing);
					printer.print(traveledDistance_transit);
				}
				printer.print(traveledDistance_all);
				if (shp.isDefined()) {
					printer.print(distancesPerTrip_perPerson_internal_inInvestigationArea_perSubpopulation.values().stream().flatMapToDouble(
						list -> list.stream().mapToDouble(Double::doubleValue)).sum());
					printer.print(distancesPerTrip_perPerson_incoming_inInvestigationArea_perSubpopulation.values().stream().flatMapToDouble(
						list -> list.stream().mapToDouble(Double::doubleValue)).sum());
					printer.print(distancesPerTrip_perPerson_outgoing_inInvestigationArea_perSubpopulation.values().stream().flatMapToDouble(
						list -> list.stream().mapToDouble(Double::doubleValue)).sum());
					printer.print(distancesPerTrip_perPerson_transit_inInvestigationArea_perSubpopulation.values().stream().flatMapToDouble(
						list -> list.stream().mapToDouble(Double::doubleValue)).sum());
					printer.print(distancesPerTrip_perPerson_all_inInvestigationArea_perSubpopulation.values().stream().flatMapToDouble(list -> list.stream().mapToDouble(Double::doubleValue)).sum());

				}
				printer.print(numberOfTrips_all == 0 ? "0" : (double) numberOfTrips_all / numberOfAgentsInSubpopulation);

				if (shp.isDefined()) {
					printer.print(numberOfTrips_internal == 0 ? "0;" : traveledDistance_internal / numberOfTrips_internal);
					printer.print(numberOfTrips_incoming == 0 ? "0;" : traveledDistance_incoming / numberOfTrips_incoming);
					printer.print(numberOfTrips_outgoing == 0 ? "0;" : traveledDistance_outgoing / numberOfTrips_outgoing);
					printer.print(numberOfTrips_transit == 0 ? "0;" : traveledDistance_transit / numberOfTrips_transit);
				}
				printer.print(numberOfTrips_all == 0 ? "0" : String.valueOf(traveledDistance_all / numberOfTrips_all));

				printer.println();
			}



		} catch (IOException e) {
			log.error("Could not create output file", e);
		}

	}

	private HashMap<Id<Person>, List<Double>> filterByPopulationGroup(HashMap<Id<Person>, List<Double>> distancesPerTripPerPerson, String populationGroup, Scenario scenario) {
		HashMap<Id<Person>, List<Double>> filteredList = new HashMap<>();
		distancesPerTripPerPerson.keySet().stream().filter(personId -> {
			Person person = scenario.getPopulation().getPersons().get(personId);
			if (PopulationUtils.getSubpopulation(person) == null)
				log.error("This agent has no populationGroup {}, This should not happen", personId);
			return groupsOfSubpopulationsForCommercialAnalysis.get(populationGroup).contains(PopulationUtils.getSubpopulation(person));
		}).forEach(personId -> {
			filteredList.put(personId, distancesPerTripPerPerson.get(personId));
		});
		return filteredList;
	}

	private void createAnalysisPerVehicle(Path travelDistancesPerVehicleOutputFile, LinkVolumeCommercialEventHandler linkDemandEventHandler) {
	//TODO make this analysis more general or move this as additional analysis to the rvr-freight-analysis module
		HashMap<String, Object2DoubleOpenHashMap<String>> travelDistancesPerVehicle = linkDemandEventHandler.getTravelDistancesPerVehicle();
		HashMap<Id<Vehicle>, String> vehicleSubpopulations = linkDemandEventHandler.getGroupOfRelevantVehicles();

		Map<String, Integer> maxDistanceWithDepotChargingInKilometers = createBatterieCapacitiesPerVehicleType();

		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(travelDistancesPerVehicleOutputFile), CSVFormat.DEFAULT)) {

			printer.print("vehicleId;");
			printer.print("vehicleType;");
			printer.print("subpopulation;");
			printer.print("distanceInKm;");
			printer.print("distanceInKmWithDepotCharging;");
			printer.print("shareOfTravelDistanceWithDepotCharging");
			printer.println();
			for (String vehicleType : travelDistancesPerVehicle.keySet()) {
				Object2DoubleOpenHashMap<String> travelDistancesForVehiclesWithThisType = travelDistancesPerVehicle.get(vehicleType);
				for (String vehicleId : travelDistancesForVehiclesWithThisType.keySet()) {
					printer.print(vehicleId);
					printer.print(vehicleType);
					printer.print(vehicleSubpopulations.get(Id.createVehicleId(vehicleId)));
					double traveledDistanceInKm = Math.round(travelDistancesForVehiclesWithThisType.getDouble(vehicleId)/10)/100.0;
					printer.print(traveledDistanceInKm);
					String maxDistanceWithoutRecharging;
					if (maxDistanceWithDepotChargingInKilometers.containsKey(vehicleType)) {
						maxDistanceWithoutRecharging = String.valueOf(maxDistanceWithDepotChargingInKilometers.get(vehicleType));
						printer.print(maxDistanceWithoutRecharging);
					} else {
						throw new IllegalArgumentException("Vehicle type " + vehicleType + " not found in maxDistanceWithDepotChargingInKilometers map.");
					}
					printer.print(String.valueOf(
						Math.round(traveledDistanceInKm / (maxDistanceWithDepotChargingInKilometers.get(vehicleType)) * 100) / 100.0));
					printer.println();
				}
			}

		} catch (IOException e) {
			log.error("Could not create output file", e);
		}
	}

	@NotNull
	private Map<String, Integer> createBatterieCapacitiesPerVehicleType() {
		Map<String, Integer> maxDistanceWithDepotChargingInKilometers = new HashMap<>();

		// Fahrzeugtyp und zugehörige maximale Reichweite (in Kilometern)
		maxDistanceWithDepotChargingInKilometers.put("golf1.4", 200);
		maxDistanceWithDepotChargingInKilometers.put("car", 200);
		maxDistanceWithDepotChargingInKilometers.put("vwCaddy", 120); // https://www.vw-nutzfahrzeuge.at/caddy/caddy/ehybrid
		maxDistanceWithDepotChargingInKilometers.put("mercedes313_parcel", 440); //https://www.adac.de/rund-ums-fahrzeug/autokatalog/marken-modelle/mercedes-benz/esprinter/
		maxDistanceWithDepotChargingInKilometers.put("mercedes313", 440);
		maxDistanceWithDepotChargingInKilometers.put("light8t", 174);
		maxDistanceWithDepotChargingInKilometers.put("medium18t", 395);
		maxDistanceWithDepotChargingInKilometers.put("medium18t_parcel", 395);
		maxDistanceWithDepotChargingInKilometers.put("waste_collection_diesel", 280);
		maxDistanceWithDepotChargingInKilometers.put("heavy40t", 416);
		maxDistanceWithDepotChargingInKilometers.put("heavy", 416);
		maxDistanceWithDepotChargingInKilometers.put("truck40t", 416);
		return maxDistanceWithDepotChargingInKilometers;
	}

	private void createRelationsAnalysis(Path relationsOutputFile, LinkVolumeCommercialEventHandler linkDemandEventHandler) {
		Map<Integer, Object2DoubleMap<String>> relations = linkDemandEventHandler.getRelations();
		ArrayList<String> header = findHeader(relations);
		ArrayList<Integer> relationNumbers = new ArrayList<>(relations.keySet());
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(relationsOutputFile), CSVFormat.DEFAULT)) {

			printer.print("relationNumber");
			for (String h : header) {
				printer.print(h);
			}

			printer.println();
			for (Integer relationNumber : relationNumbers) {
				printer.print(relationNumber);
				Object2DoubleMap<String> relation = relations.get(relationNumber);
				for (String value : header) {
					if (relation.containsKey(value))
						printer.print(String.valueOf(relation.getDouble(value)));
					else
						printer.print("0");
				}
				printer.println();
			}

		} catch (IOException e) {
			log.error("Could not create output file", e);
		}

	}

	private ArrayList<String> findHeader(Map<Integer, Object2DoubleMap<String>> relations) {
		ArrayList<String> header = new ArrayList<>();
		for (Object2DoubleMap<String> relation : relations.values()) {
			for (String value : relation.keySet()) {
				if (!header.contains(value)) header.add(value);
			}
		}
		header.sort(Comparator.naturalOrder());
		return header;
	}

	private void createTravelDistancesShares(Path travelDistancesPerModeOutputFile, LinkVolumeCommercialEventHandler linkDemandEventHandler) {
		String newFileName = travelDistancesPerModeOutputFile.getFileName().toString().replace(".csv", "_perMode.csv");
		Object2DoubleOpenHashMap<String> travelDistancesPerMode = linkDemandEventHandler.getTravelDistancesPerMode();
		writeDistanceFiles(travelDistancesPerMode, newFileName);
		newFileName = travelDistancesPerModeOutputFile.getFileName().toString().replace(".csv", "_perType.csv");
		Object2DoubleOpenHashMap<String> travelDistancesPerType = linkDemandEventHandler.getTravelDistancesPerType();
		writeDistanceFiles(travelDistancesPerType, newFileName);
		newFileName = travelDistancesPerModeOutputFile.getFileName().toString().replace(".csv", "_perSubpopulation.csv");
		Object2DoubleOpenHashMap<String> travelDistancesPerSubpopulation = linkDemandEventHandler.getTravelDistancesPerSubpopulation();
		writeDistanceFiles(travelDistancesPerSubpopulation, newFileName);
	}

	private void writeDistanceFiles(Object2DoubleOpenHashMap<String> travelDistancesPerMode, String fileName) {
		ArrayList<String> headerWithModes = new ArrayList<>(travelDistancesPerMode.keySet());
		headerWithModes.sort(Comparator.naturalOrder());
		double sumOfAllDistances = travelDistancesPerMode.values().doubleStream().sum();
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath(fileName)), CSVFormat.DEFAULT)) {

			for (String h : headerWithModes) {
				printer.print(h);
			}

			printer.println();

			// Write the data row
			for (String mode : headerWithModes) {
				printer.print(String.valueOf(travelDistancesPerMode.getDouble(mode) / sumOfAllDistances));
			}
			printer.println();

		} catch (IOException e) {
			log.error("Could not create output file", e);
		}
	}

	public void createLinkVolumeAnalysis(Scenario scenario, Path linkDemandOutputFile, LinkVolumeCommercialEventHandler linkDemandEventHandler) {

//		File file = new File(linkDemandOutputFile);
		Map<Id<Link>, Object2DoubleOpenHashMap<String>> linkVolumesPerMode = linkDemandEventHandler.getLinkVolumesPerMode();
		ArrayList<String> headerWithModes = new ArrayList<>(List.of("allCommercialVehicles"));
		headerWithModes.addAll(groupsOfSubpopulationsForCommercialAnalysis.keySet());
		scenario.getVehicles().getVehicleTypes().values().forEach(vehicleType -> {
			if (!headerWithModes.contains(vehicleType.getNetworkMode())) headerWithModes.add(vehicleType.getNetworkMode());
		});
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(linkDemandOutputFile), CSVFormat.Builder.create().setDelimiter(";").get())) {
			printer.print("linkId");
			for (String mode : headerWithModes) {
				printer.print(mode);
			}
			printer.println();

			for (Id<Link> linkId : linkVolumesPerMode.keySet()) {
				printer.print(linkId.toString());
				for (String mode : headerWithModes) {
					printer.print((int) (linkVolumesPerMode.get(linkId).getDouble(mode)));
				}
				printer.println();
			}
		} catch (IOException e) {
			log.error("Could not create output file", e);
		}
	}

	/**
	 * Converts the raw input map into a map of lists for easier processing.
	 */
	private Map<String, List<String>> getGroupsOfSubpopulations(Map<String, String> groupsOfSubpopulationsRaw) {
		Map<String, List<String>> groupsOfSubpopulations = new HashMap<>();
		for (Map.Entry<String, String> entry : groupsOfSubpopulationsRaw.entrySet()) {
			List<String> subpops = Arrays.asList(entry.getValue().split(","));
			groupsOfSubpopulations.put(entry.getKey(), subpops);
		}
		return groupsOfSubpopulations;
	}
}
