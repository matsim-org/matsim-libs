package org.matsim.application.analysis.commercialTraffic;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import jakarta.validation.constraints.NotNull;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
	//TODO make plans configurable
	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(CommercialAnalysis.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(CommercialAnalysis.class);
	@CommandLine.Mixin
	private ShpOptions shp;
	@CommandLine.Option(names = "--analysisOutputDirectory", description = "The directory where the analysis output will be stored.", defaultValue = "analysis/commercialTraffic/")
	private static String analysisOutputDirectory;

	@CommandLine.Option(names = "--sampleSize", description = "The sample size of the simulation.")
	private static double sampleSize;

	public static void main(String[] args) {
		new CommercialAnalysis().execute(args);
	}

	public Integer call() throws Exception {
		log.info("++++++++++++++++++ Start Analysis for RVR Freight simulations ++++++++++++++++++++++++++++");

		final String eventsFile = globFile(input.getRunDirectory(), "*output_events*").toString();
		final String personFile = globFile(input.getRunDirectory(), "*output_persons*").toString();

		analysisOutputDirectory = input.getRunDirectory().resolve(analysisOutputDirectory).toString();
		analysisOutputDirectory = analysisOutputDirectory + "/";
		File dir = new File(analysisOutputDirectory);
		if (!dir.exists()) {
			dir.mkdirs();
		}

		// for SimWrapper
		final String linkDemandOutputFile = analysisOutputDirectory + "commercialTraffic_link_volume.csv";
		log.info("Writing volume per link to: {}", linkDemandOutputFile);

		final String travelDistancesPerModeOutputFile = analysisOutputDirectory + "commercialTraffic_travelDistancesShares.csv";
		log.info("Writing travel distances per mode to: {}", travelDistancesPerModeOutputFile);

		final String travelDistancesPerVehicleOutputFile = analysisOutputDirectory + "commercialTraffic_travelDistances_perVehicle.csv";
		log.info("Writing travel distances per vehicle to: {}", travelDistancesPerVehicleOutputFile);

		final String relationsOutputFile = analysisOutputDirectory + "commercialTraffic_relations.csv";
		log.info("Writing relations to: {}", relationsOutputFile);

		final String tourDurationsOutputFile = analysisOutputDirectory + "commercialTraffic_tour_durations.csv";
		log.info("Writing tour durations to: {}", tourDurationsOutputFile);

		final String generalTravelDataOutputFile = analysisOutputDirectory + "commercialTraffic_generalTravelData.csv";
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
		LinkVolumeCommercialEventHandler linkDemandEventHandler = new LinkVolumeCommercialEventHandler(scenario, personFile, sampleSize, shp);
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
		log.info("All output written to {}", analysisOutputDirectory);
		log.info("-------------------------------------------------");
		return 0;
	}

	private void createTourDurationPerVehicle(String tourDurationsOutputFile, LinkVolumeCommercialEventHandler linkDemandEventHandler,
											  Scenario scenario) {

		HashMap<Id<Vehicle>, Double> tourDurations = linkDemandEventHandler.getTourDurationPerPerson();
		HashMap<Id<Vehicle>, Id<Person>> vehicleToPersonId = linkDemandEventHandler.getVehicleIdToPersonId();

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(tourDurationsOutputFile));
			bw.write("personId;");
			bw.write("vehicleId;");
			bw.write("subpopulation;");
			bw.write("tourDurationInSeconds;");
			bw.newLine();

			for (Id<Vehicle> vehicleId : tourDurations.keySet()) {
				Id<Person> personId = vehicleToPersonId.get(vehicleId);
				bw.write(personId + ";");
				bw.write(vehicleId + ";");
				bw.write(scenario.getPopulation().getPersons().get(personId).getAttributes().getAttribute("subpopulation") + ";");
				bw.write(tourDurations.get(vehicleId) + ";");
				bw.newLine();
			}

			bw.close();
		} catch (IOException e) {
			log.error("Could not create output file", e);
		}
	}

	private void createGeneralTravelDataAnalysis(String generalTravelDataOutputFile, LinkVolumeCommercialEventHandler linkDemandEventHandler, Scenario scenario) {

		HashMap<Id<Vehicle>, String> subpopulationPerVehicle = linkDemandEventHandler.getVehicleSubpopulation();
		Map<String, List<Id<Vehicle>>> vehiclesPerSubpopulation = subpopulationPerVehicle.entrySet().stream()
			.collect(Collectors.groupingBy(
				Map.Entry::getValue,
				Collectors.mapping(Map.Entry::getKey, Collectors.toList())
			));
		HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_internal = linkDemandEventHandler.getDistancesPerTrip_perPerson_internal();
		HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_incoming = linkDemandEventHandler.getDistancesPerTrip_perPerson_incoming();
		HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_outgoing = linkDemandEventHandler.getDistancesPerTrip_perPerson_outgoing();
		HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_transit = linkDemandEventHandler.getDistancesPerTrip_perPerson_transit();
		HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_all = linkDemandEventHandler.getDistancesPerTrip_perPerson_all();

		HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_internal_inRuhrArea = linkDemandEventHandler.getDistancesPerTrip_perPerson_internal_inInvestigationArea();
		HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_incoming_inRuhrArea = linkDemandEventHandler.getDistancesPerTrip_perPerson_incoming_inInvestigationArea();
		HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_outgoing_inRuhrArea = linkDemandEventHandler.getDistancesPerTrip_perPerson_outgoing_inInvestigationArea();
		HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_transit_inRuhrArea = linkDemandEventHandler.getDistancesPerTrip_perPerson_transit_inInvestigationArea();
		HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_all_inRuhrArea = linkDemandEventHandler.getDistancesPerTrip_perPerson_all_inInvestigationArea();

		try {
			// _Intern: internal trips (start and end inside the area)
			// _Incoming: incoming trips (start outside the area and end inside the area)
			// _Outgoing: incoming trips (start inside the area and end outside the area)
			// _Transit: transit trips (start and end inside the area)
			// _all: all trips
			BufferedWriter bw = new BufferedWriter(new FileWriter(generalTravelDataOutputFile));
			bw.write("subpopulation;");
			bw.write("numberOfAgents;");

			bw.write("numberOfTrips_Intern;");
			bw.write("numberOfTrips_Incoming;");
			bw.write("numberOfTrips_Outgoing;");
			bw.write("numberOfTrips_Transit;");
			bw.write("numberOfTrips_all;");

			bw.write("traveledDistance_Intern;");
			bw.write("traveledDistance_Incoming;");
			bw.write("traveledDistance_Outgoing;");
			bw.write("traveledDistance_Transit;");
			bw.write("traveledDistance_all;");

			bw.write("traveledDistanceInRVR_area_Intern;");
			bw.write("traveledDistanceInRVR_area_Incoming;");
			bw.write("traveledDistanceInRVR_area_Outgoing;");
			bw.write("traveledDistanceInRVR_area_Transit;");
			bw.write("traveledDistanceInRVR_area_all;");

//			bw.write("averageTripsPerAgent_Intern;"); //TODO why this commented out
//			bw.write("averageTripsPerAgent_Incoming;");
//			bw.write("averageTripsPerAgent_Outgoing;");
//			bw.write("averageTripsPerAgent_Transit;");
			bw.write("averageTripsPerAgent_all;");

			bw.write("averageDistancePerTrip_Intern;");
			bw.write("averageDistancePerTrip_Incoming;");
			bw.write("averageDistancePerTrip_Outgoing;");
			bw.write("averageDistancePerTrip_Transit;");
			bw.write("averageDistancePerTrip_all;");
			bw.newLine();

			for (String subpopulation : vehiclesPerSubpopulation.keySet()){
				bw.write(subpopulation + ";");
				int numberOfAgentsInSubpopulation = vehiclesPerSubpopulation.get(subpopulation).size();
				bw.write(numberOfAgentsInSubpopulation + ";");

				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_internal_perSubpopulation = filterBySubpopulation(distancesPerTrip_perPerson_internal, subpopulation, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_incoming_perSubpopulation = filterBySubpopulation(distancesPerTrip_perPerson_incoming, subpopulation, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_outgoing_perSubpopulation = filterBySubpopulation(distancesPerTrip_perPerson_outgoing, subpopulation, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_transit_perSubpopulation = filterBySubpopulation(distancesPerTrip_perPerson_transit, subpopulation, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_all_perSubpopulation = filterBySubpopulation(distancesPerTrip_perPerson_all, subpopulation, scenario);

				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_internal_inRuhrArea_perSubpopulation = filterBySubpopulation(distancesPerTrip_perPerson_internal_inRuhrArea, subpopulation, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_incoming_inRuhrArea_perSubpopulation = filterBySubpopulation(distancesPerTrip_perPerson_incoming_inRuhrArea, subpopulation, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_outgoing_inRuhrArea_perSubpopulation = filterBySubpopulation(distancesPerTrip_perPerson_outgoing_inRuhrArea, subpopulation, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_transit_inRuhrArea_perSubpopulation = filterBySubpopulation(distancesPerTrip_perPerson_transit_inRuhrArea, subpopulation, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_all_inRuhrArea_perSubpopulation = filterBySubpopulation(distancesPerTrip_perPerson_all_inRuhrArea, subpopulation, scenario);

				int numberOfTrips_internal = distancesPerTrip_perPerson_internal_perSubpopulation.values().stream().mapToInt(List::size).sum();
				int numberOfTrips_incoming = distancesPerTrip_perPerson_incoming_perSubpopulation.values().stream().mapToInt(List::size).sum();
				int numberOfTrips_outgoing = distancesPerTrip_perPerson_outgoing_perSubpopulation.values().stream().mapToInt(List::size).sum();
				int numberOfTrips_transit = distancesPerTrip_perPerson_transit_perSubpopulation.values().stream().mapToInt(List::size).sum();
				int numberOfTrips_all = distancesPerTrip_perPerson_all_perSubpopulation.values().stream().mapToInt(List::size).sum();

				bw.write(numberOfTrips_internal + ";");
				bw.write(numberOfTrips_incoming + ";");
				bw.write(numberOfTrips_outgoing + ";");
				bw.write(numberOfTrips_transit + ";");
				bw.write(numberOfTrips_all + ";");

				double traveledDistance_internal = distancesPerTrip_perPerson_internal_perSubpopulation.values().stream().flatMapToDouble(list -> list.stream().mapToDouble(Double::doubleValue)).sum();
				double traveledDistance_incoming = distancesPerTrip_perPerson_incoming_perSubpopulation.values().stream().flatMapToDouble(list -> list.stream().mapToDouble(Double::doubleValue)).sum();
				double traveledDistance_outgoing = distancesPerTrip_perPerson_outgoing_perSubpopulation.values().stream().flatMapToDouble(list -> list.stream().mapToDouble(Double::doubleValue)).sum();
				double traveledDistance_transit = distancesPerTrip_perPerson_transit_perSubpopulation.values().stream().flatMapToDouble(list -> list.stream().mapToDouble(Double::doubleValue)).sum();
				double traveledDistance_all = distancesPerTrip_perPerson_all_perSubpopulation.values().stream().flatMapToDouble(list -> list.stream().mapToDouble(Double::doubleValue)).sum();

				bw.write(traveledDistance_internal + ";");
				bw.write(traveledDistance_incoming + ";");
				bw.write(traveledDistance_outgoing + ";");
				bw.write(traveledDistance_transit + ";");
				bw.write(traveledDistance_all + ";");

				bw.write(distancesPerTrip_perPerson_internal_inRuhrArea_perSubpopulation.values().stream().flatMapToDouble(list -> list.stream().mapToDouble(Double::doubleValue)).sum() + ";");
				bw.write(distancesPerTrip_perPerson_incoming_inRuhrArea_perSubpopulation.values().stream().flatMapToDouble(list -> list.stream().mapToDouble(Double::doubleValue)).sum() + ";");
				bw.write(distancesPerTrip_perPerson_outgoing_inRuhrArea_perSubpopulation.values().stream().flatMapToDouble(list -> list.stream().mapToDouble(Double::doubleValue)).sum() + ";");
				bw.write(distancesPerTrip_perPerson_transit_inRuhrArea_perSubpopulation.values().stream().flatMapToDouble(list -> list.stream().mapToDouble(Double::doubleValue)).sum() + ";");
				bw.write(distancesPerTrip_perPerson_all_inRuhrArea_perSubpopulation.values().stream().flatMapToDouble(list -> list.stream().mapToDouble(Double::doubleValue)).sum() + ";");

				bw.write(numberOfTrips_all == 0 ? "0" : (double) numberOfTrips_all / numberOfAgentsInSubpopulation + ";");

				bw.write(numberOfTrips_internal == 0 ? "0;" : traveledDistance_internal / numberOfTrips_internal + ";");
				bw.write(numberOfTrips_incoming == 0 ? "0;" : traveledDistance_incoming / numberOfTrips_incoming + ";");
				bw.write(numberOfTrips_outgoing == 0 ? "0;" : traveledDistance_outgoing / numberOfTrips_outgoing + ";");
				bw.write(numberOfTrips_transit == 0 ? "0;" : traveledDistance_transit / numberOfTrips_transit + ";");
				bw.write(numberOfTrips_all == 0 ? "0" : String.valueOf(traveledDistance_all / numberOfTrips_all));

				bw.newLine();
			}


			bw.close();
		} catch (IOException e) {
			log.error("Could not create output file", e);
		}

	}

	private HashMap<Id<Person>, List<Double>> filterBySubpopulation(HashMap<Id<Person>, List<Double>> distancesPerTripPerPerson, String subpopulation, Scenario scenario) {
		HashMap<Id<Person>, List<Double>> filteredList = new HashMap<>();
		distancesPerTripPerPerson.keySet().stream().filter(personId -> {
			Person person = scenario.getPopulation().getPersons().get(personId);
			if (PopulationUtils.getSubpopulation(person) == null)
				log.error("This agent has no subpopulation {}, This should not happen", personId);
			return PopulationUtils.getSubpopulation(person).equals(subpopulation);
		}).forEach(personId -> {
			filteredList.put(personId, distancesPerTripPerPerson.get(personId));
		});
		return filteredList;
	}

	private void createAnalysisPerVehicle(String travelDistancesPerVehicleOutputFile, LinkVolumeCommercialEventHandler linkDemandEventHandler) {
	//TODO make this analysis more general or move this as additional analysis to the rvr-freight-analysis module
		HashMap<String, Object2DoubleOpenHashMap<String>> travelDistancesPerVehicle = linkDemandEventHandler.getTravelDistancesPerVehicle();
		HashMap<Id<Vehicle>, String> vehicleSubpopulations = linkDemandEventHandler.getVehicleSubpopulation();

		Map<String, Integer> maxDistanceWithDepotChargingInKilometers = createBatterieCapacitiesPerVehicleType();

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(travelDistancesPerVehicleOutputFile));
			bw.write("vehicleId;");
			bw.write("vehicleType;");
			bw.write("subpopulation;");
			bw.write("distanceInKm;");
			bw.write("distanceInKmWithDepotCharging;");
			bw.write("shareOfTravelDistanceWithDepotCharging");
			bw.newLine();
			for (String vehicleType : travelDistancesPerVehicle.keySet()) {
				Object2DoubleOpenHashMap<String> travelDistancesForVehiclesWithThisType = travelDistancesPerVehicle.get(vehicleType);
				for (String vehicleId : travelDistancesForVehiclesWithThisType.keySet()) {
					bw.write(vehicleId + ";");
					bw.write(vehicleType + ";");
					bw.write(vehicleSubpopulations.get(Id.createVehicleId(vehicleId)) + ";");
					double traveledDistanceInKm = Math.round(travelDistancesForVehiclesWithThisType.getDouble(vehicleId)/10)/100.0;
					bw.write(traveledDistanceInKm + ";");
					String maxDistanceWithoutRecharging;
					if (maxDistanceWithDepotChargingInKilometers.containsKey(vehicleType)) {
						maxDistanceWithoutRecharging = String.valueOf(maxDistanceWithDepotChargingInKilometers.get(vehicleType));
						bw.write(maxDistanceWithoutRecharging + ";");
					} else {
						throw new IllegalArgumentException("Vehicle type " + vehicleType + " not found in maxDistanceWithDepotChargingInKilometers map.");
					}
					bw.write(String.valueOf(
						Math.round(traveledDistanceInKm / (maxDistanceWithDepotChargingInKilometers.get(vehicleType)) * 100) / 100.0));
					bw.newLine();
				}
			}
			bw.close();
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

	private void createRelationsAnalysis(String relationsOutputFile, LinkVolumeCommercialEventHandler linkDemandEventHandler) {
		File fileRelations = new File(relationsOutputFile);
		Map<Integer, Object2DoubleMap<String>> relations = linkDemandEventHandler.getRelations();
		ArrayList<String> header = findHeader(relations);
		ArrayList<Integer> relationNumbers = new ArrayList<>(relations.keySet());
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(fileRelations));
			bw.write("relationNumber;");
			bw.write(String.join(";", header));

			bw.newLine();
			for (Integer relationNumber : relationNumbers) {
				bw.write(relationNumber + ";");
				Object2DoubleMap<String> relation = relations.get(relationNumber);
				for (String value : header) {
					if (relation.containsKey(value))
						bw.write(String.valueOf(relation.getDouble(value)));
					else
						bw.write("0");
					if (header.indexOf(value) < header.size() - 1) {
						bw.write(";");
					}
				}
				bw.newLine();
			}
			bw.close();
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

	private void createTravelDistancesShares(String travelDistancesPerModeOutputFile, LinkVolumeCommercialEventHandler linkDemandEventHandler) {
		File filePerMode = new File(travelDistancesPerModeOutputFile.replace(".csv", "_perMode.csv"));
		Object2DoubleOpenHashMap<String> travelDistancesPerMode = linkDemandEventHandler.getTravelDistancesPerMode();
		writeDistanceFiles(travelDistancesPerMode, filePerMode);
		File filePerType = new File(travelDistancesPerModeOutputFile.replace(".csv", "_perType.csv"));
		Object2DoubleOpenHashMap<String> travelDistancesPerType = linkDemandEventHandler.getTravelDistancesPerType();
		writeDistanceFiles(travelDistancesPerType, filePerType);
		File filePerSubpopulation = new File(travelDistancesPerModeOutputFile.replace(".csv", "_perSubpopulation.csv"));
		Object2DoubleOpenHashMap<String> travelDistancesPerSubpopulation = linkDemandEventHandler.getTravelDistancesPerSubpopulation();
		writeDistanceFiles(travelDistancesPerSubpopulation, filePerSubpopulation);
	}

	private static void writeDistanceFiles(Object2DoubleOpenHashMap<String> travelDistancesPerMode, File file) {
		ArrayList<String> headerWithModes = new ArrayList<>(travelDistancesPerMode.keySet());
		headerWithModes.sort(Comparator.naturalOrder());
		double sumOfAllDistances = travelDistancesPerMode.values().doubleStream().sum();
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			bw.write(String.join(";", headerWithModes));

			bw.newLine();

			// Write the data row
			for (int i = 0; i < headerWithModes.size(); i++) {
				String mode = headerWithModes.get(i);
				bw.write(String.valueOf(travelDistancesPerMode.getDouble(mode) / sumOfAllDistances));

				// Add delimiter if it's not the last element
				if (i < headerWithModes.size() - 1) {
					bw.write(";");
				}
			}
			bw.newLine();
			bw.close();
		} catch (IOException e) {
			log.error("Could not create output file", e);
		}
	}

	public void createLinkVolumeAnalysis(Scenario scenario, String linkDemandOutputFile, LinkVolumeCommercialEventHandler linkDemandEventHandler) {

		File file = new File(linkDemandOutputFile);
		Map<Id<Link>, Object2DoubleOpenHashMap<String>> linkVolumesPerMode = linkDemandEventHandler.getLinkVolumesPerMode();
		ArrayList<String> headerWithModes = new ArrayList<>(List.of("allCommercialVehicles", "Small-Scale-Commercial-Traffic", "Transit-Freight-Traffic", "FTL-Traffic", "LTL-Traffic", "KEP", "FTL_kv-Traffic", "WasteCollection"));
		scenario.getVehicles().getVehicleTypes().values().forEach(vehicleType -> {
			if (!headerWithModes.contains(vehicleType.getNetworkMode())) headerWithModes.add(vehicleType.getNetworkMode());
		});
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("linkId");
			for (String mode : headerWithModes) {
				bw.write(";" + mode);
			}
			bw.newLine();

			for (Id<Link> linkId : linkVolumesPerMode.keySet()) {
				bw.write(linkId.toString());
				for (String mode : headerWithModes) {
					bw.write(";" + (int) (linkVolumesPerMode.get(linkId).getDouble(mode)));
				}
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			log.error("Could not create output file", e);
		}
	}
}
