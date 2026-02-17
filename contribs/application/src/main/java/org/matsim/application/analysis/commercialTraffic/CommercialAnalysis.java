package org.matsim.application.analysis.commercialTraffic;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.AnalysisUtils;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import picocli.CommandLine;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
		"generalTravelData.csv",
		"commercialTraffic_link_volume.csv",
		"travelDistancesShares_%s.csv",
		"tourAnalysis_%s.csv",
		"relations.csv",
		"activities.csv"
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
	@CommandLine.Option(names = "--dist-groups-in-KM", split = ",", description = "List of distances for binning", defaultValue = "0,10,20,30,40,50,60,75,90,105,120,150,180,240,300,420,540,660,780,900")
	private List<Long> distGroups;
	@CommandLine.Option(names = "--tour-Duration-groups-in-H", split = ",", description = "List of tour durations for binning", defaultValue = "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14")
	private List<Long> tourDurationGroups;
	@CommandLine.Option(names = "--number-of-jobs-groups", split = ",", description = "List of number of jobs groups for binning", defaultValue = "0,1,2,5,10,20")
	private List<Long> numberOfJobsGroups;
	@CommandLine.Option(names = "--activity-duration-groups", split = ",", description = "List of activity duration groups for binning", defaultValue = "0,30,60,90,120,180,240,300,360,420,480,540,600,720,840")
	private List<Long> activityDurationGroups;

	static void main(String[] args) {
		new CommercialAnalysis().execute(args);
	}

	public Integer call() throws Exception {
		log.info("++++++++++++++++++ Start Analysis for Commercial Agents ++++++++++++++++++++++++++++");

		final String eventsFile = globFile(input.getRunDirectory(), "*output_events*").toString();

		if (!groupsOfSubpopulationsForCommercialAnalysisRaw.isEmpty())
			groupsOfSubpopulationsForCommercialAnalysis = getGroupsOfSubpopulations(groupsOfSubpopulationsForCommercialAnalysisRaw);

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
		CommercialTrafficAnalysisEventHandler linkDemandEventHandler = new CommercialTrafficAnalysisEventHandler(scenario, sampleSize, shp,
			groupsOfSubpopulationsForCommercialAnalysis);
		eventsManager.addHandler(linkDemandEventHandler);

		eventsManager.initProcessing();

		log.info("-------------------------------------------------");
		log.info("Done reading the events file");
		log.info("Finish processing...");
		eventsManager.finishProcessing();
		new MatsimEventsReader(eventsManager).readFile(eventsFile);
		log.info("Closing events file...");

		createGeneralTravelDataAnalysis(linkDemandEventHandler, scenario);
		createTravelDistancesShares(linkDemandEventHandler);
		createLinkVolumeAnalysis(scenario, linkDemandEventHandler);
		createRelationsAnalysis(linkDemandEventHandler);
		createAnalysisPerVehicle(linkDemandEventHandler);
		createActivityAnalysis(scenario);

		log.info("Done");
		log.info("All outputs of commercial analysis written to {}", output.getPath());
		log.info("-------------------------------------------------");
		return 0;
	}

	private void createActivityAnalysis(Scenario scenario) {
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("activities.csv")), CSVFormat.DEFAULT)) {
			HashMap<String, List<ActivityInformation>> activityToPersonMap = new HashMap<>();
			List<String> activityDurationLabels = AnalysisUtils.createGroupLabels(activityDurationGroups);

			printer.print("personId");
			printer.print("groupOfSubpopulation");
			printer.print("startCategory");
			printer.print("activityType");
			printer.print("activityCountPerPerson");
			printer.print("activityDuration_group");
			printer.print("activityDurationInSeconds");
			printer.print("activityDurationInMinutes");
			for (String group : groupsOfSubpopulationsForCommercialAnalysis.keySet()) {
				printer.print("activityDurationInMinutes_" + group);
			}
			printer.println();
			Map<String, String> subpopToGroup = new HashMap<>();
			for (var e : groupsOfSubpopulationsForCommercialAnalysis.entrySet()) {
				String group = e.getKey();
				for (String subpop : e.getValue()) {
					subpopToGroup.put(subpop, group);
				}
			}

			for (Person person : scenario.getPopulation().getPersons().values()) {
				String subpop = PopulationUtils.getSubpopulation(person);
				String groupOfPerson = subpopToGroup.get(subpop);
				String startCategory = "undefined";
				if (person.getAttributes().getAttribute("startCategory") != null)
					startCategory = person.getAttributes().getAttribute("startCategory").toString();
				if (groupOfPerson != null) {
					List<Activity> activities = TripStructureUtils.getActivities(person.getSelectedPlan(),
						TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
					// skip first and last activity
					for (int i = 1; i <= activities.size() - 2; i++) {
						Activity activity = activities.get(i);
						double duration = activity.getMaximumDuration().seconds();
						BigDecimal durationInMinutes = new BigDecimal(duration / 60).setScale(0, RoundingMode.HALF_UP);
						String labelForValue = AnalysisUtils.getLabelForValue(durationInMinutes.longValue(), activityDurationGroups,
							activityDurationLabels);
						activityToPersonMap.computeIfAbsent(labelForValue, k -> new ArrayList<>()).add(
							new ActivityInformation(activity, i, person.getId(), groupOfPerson, startCategory));
					}
				}
			}
			for (String label : activityDurationLabels) {
				if (!activityToPersonMap.containsKey(label)) {
					continue;
				}
				for (ActivityInformation activityInformation : activityToPersonMap.get(label)) {
					Activity activity = activityInformation.activity;
					double duration = activity.getMaximumDuration().seconds();

					printer.print(activityInformation.personId);
					printer.print(activityInformation.groupOfSubpopulation);
					printer.print(activityInformation.startCategory);
					printer.print(activity.getType());
					printer.print(activityInformation.countForPerson);
					printer.print(label);
					printer.print(duration);
					BigDecimal durationInMinutes = new BigDecimal(duration / 60).setScale(0, RoundingMode.HALF_UP);
					printer.print(durationInMinutes);
					for (String group : groupsOfSubpopulationsForCommercialAnalysis.keySet()) {
						if (activityInformation.groupOfSubpopulation.equals(group)) {
							printer.print(durationInMinutes);
						} else {
							printer.print(Double.NaN);
						}
					}
					printer.println();
				}
			}
		} catch (IOException e) {
			log.error("Could not create output file", e);
		}
	}

	private void createGeneralTravelDataAnalysis(CommercialTrafficAnalysisEventHandler linkDemandEventHandler, Scenario scenario) {

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

		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("generalTravelData.csv")),
			CSVFormat.DEFAULT)) {
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
			printer.print("averageTripsPerAgent_all");
			if (shp.isDefined()) {
				printer.print("averageDistancePerTrip_Intern");
				printer.print("averageDistancePerTrip_Incoming");
				printer.print("averageDistancePerTrip_Outgoing");
				printer.print("averageDistancePerTrip_Transit");
			}
			printer.print("averageDistancePerTrip_all");
			printer.println();

			for (String group : vehiclesPerGroup.keySet()) {
				printer.print(group);
				int numberOfAgentsInSubpopulation = vehiclesPerGroup.get(group).size();
				printer.print(numberOfAgentsInSubpopulation);

				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_internal_perSubpopulation = filterByPopulationGroup(
					distancesPerTrip_perPerson_internal, group, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_incoming_perSubpopulation = filterByPopulationGroup(
					distancesPerTrip_perPerson_incoming, group, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_outgoing_perSubpopulation = filterByPopulationGroup(
					distancesPerTrip_perPerson_outgoing, group, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_transit_perSubpopulation = filterByPopulationGroup(
					distancesPerTrip_perPerson_transit, group, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_all_perSubpopulation = filterByPopulationGroup(
					distancesPerTrip_perPerson_all, group, scenario);

				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_internal_inInvestigationArea_perSubpopulation = filterByPopulationGroup(
					distancesPerTrip_perPerson_internal_inInvestigationArea, group, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_incoming_inInvestigationArea_perSubpopulation = filterByPopulationGroup(
					distancesPerTrip_perPerson_incoming_inInvestigationArea, group, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_outgoing_inInvestigationArea_perSubpopulation = filterByPopulationGroup(
					distancesPerTrip_perPerson_outgoing_inInvestigationArea, group, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_transit_inInvestigationArea_perSubpopulation = filterByPopulationGroup(
					distancesPerTrip_perPerson_transit_inInvestigationArea, group, scenario);
				HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_all_inInvestigationArea_perSubpopulation = filterByPopulationGroup(
					distancesPerTrip_perPerson_all_inInvestigationArea, group, scenario);

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

				double traveledDistance_internal = distancesPerTrip_perPerson_internal_perSubpopulation.values().stream().flatMapToDouble(
					list -> list.stream().mapToDouble(Double::doubleValue)).sum();
				double traveledDistance_incoming = distancesPerTrip_perPerson_incoming_perSubpopulation.values().stream().flatMapToDouble(
					list -> list.stream().mapToDouble(Double::doubleValue)).sum();
				double traveledDistance_outgoing = distancesPerTrip_perPerson_outgoing_perSubpopulation.values().stream().flatMapToDouble(
					list -> list.stream().mapToDouble(Double::doubleValue)).sum();
				double traveledDistance_transit = distancesPerTrip_perPerson_transit_perSubpopulation.values().stream().flatMapToDouble(
					list -> list.stream().mapToDouble(Double::doubleValue)).sum();
				double traveledDistance_all = distancesPerTrip_perPerson_all_perSubpopulation.values().stream().flatMapToDouble(
					list -> list.stream().mapToDouble(Double::doubleValue)).sum();

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
					printer.print(distancesPerTrip_perPerson_all_inInvestigationArea_perSubpopulation.values().stream().flatMapToDouble(
						list -> list.stream().mapToDouble(Double::doubleValue)).sum());

				}
				printer.print(numberOfTrips_all == 0 ? "0" : (double) numberOfTrips_all / numberOfAgentsInSubpopulation);

				if (shp.isDefined()) {
					printer.print(numberOfTrips_internal == 0 ? "0" : traveledDistance_internal / numberOfTrips_internal);
					printer.print(numberOfTrips_incoming == 0 ? "0" : traveledDistance_incoming / numberOfTrips_incoming);
					printer.print(numberOfTrips_outgoing == 0 ? "0" : traveledDistance_outgoing / numberOfTrips_outgoing);
					printer.print(numberOfTrips_transit == 0 ? "0" : traveledDistance_transit / numberOfTrips_transit);
				}
				printer.print(numberOfTrips_all == 0 ? "0" : String.valueOf(traveledDistance_all / numberOfTrips_all));

				printer.println();
			}


		} catch (IOException e) {
			log.error("Could not create output file", e);
		}

	}

	private HashMap<Id<Person>, List<Double>> filterByPopulationGroup(HashMap<Id<Person>, List<Double>> distancesPerTripPerPerson,
																	  String populationGroup, Scenario scenario) {
		HashMap<Id<Person>, List<Double>> filteredList = new HashMap<>();
		distancesPerTripPerPerson.keySet().stream().filter(personId -> {
			Person person = scenario.getPopulation().getPersons().get(personId);
			if (PopulationUtils.getSubpopulation(person) == null)
				log.error("This agent has no populationGroup {}, This should not happen", personId);
			return groupsOfSubpopulationsForCommercialAnalysis.get(populationGroup).contains(PopulationUtils.getSubpopulation(person));
		}).forEach(personId -> filteredList.put(personId, distancesPerTripPerPerson.get(personId)));
		return filteredList;
	}

	private void createAnalysisPerVehicle(CommercialTrafficAnalysisEventHandler linkDemandEventHandler) {
		HashMap<String, Object2DoubleOpenHashMap<String>> travelDistancesPerVehicle = linkDemandEventHandler.getTravelDistancesPerVehicle();
		HashMap<Id<Vehicle>, String> vehicleGroupOfSubpopulation = linkDemandEventHandler.getGroupOfRelevantVehicles();
		HashMap<Id<Vehicle>, Double> tourDurations = linkDemandEventHandler.getTourDurationPerVehicle();
		HashMap<Id<Vehicle>, Id<Person>> vehicleToPersonId = linkDemandEventHandler.getVehicleIdToPersonId();
		Object2IntOpenHashMap<Id<Person>> jobsPerPerson = linkDemandEventHandler.getNumberOfJobsPerPerson();
		Map<String, Integer> maxDistanceWithDepotChargingInKilometers = createBatterieCapacitiesPerVehicleType();
		List<String> distanceLabels = AnalysisUtils.createGroupLabels(distGroups);
		List<String> tourDurationLabels = AnalysisUtils.createGroupLabels(tourDurationGroups);
		List<String> numberOfJobsLabels = AnalysisUtils.createGroupLabels(numberOfJobsGroups);

		writeAnalysisPerVehicle_distances(distanceLabels, travelDistancesPerVehicle, vehicleToPersonId, vehicleGroupOfSubpopulation,
			maxDistanceWithDepotChargingInKilometers);
		writeAnalysisPerVehicle_durations(tourDurationLabels, travelDistancesPerVehicle, tourDurations, vehicleToPersonId,
			vehicleGroupOfSubpopulation);
		writeAnalysisPerVehicle_numberOfJobs(numberOfJobsLabels, travelDistancesPerVehicle, vehicleToPersonId, vehicleGroupOfSubpopulation,
			jobsPerPerson);
	}

	private void writeAnalysisPerVehicle_distances(List<String> distanceLabels,
												   HashMap<String, Object2DoubleOpenHashMap<String>> travelDistancesPerVehicle,
												   HashMap<Id<Vehicle>, Id<Person>> vehicleToPersonId,
												   HashMap<Id<Vehicle>, String> vehicleGroupOfSubpopulation,
												   Map<String, Integer> maxDistanceWithDepotChargingInKilometers) {
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("tourAnalysis_%s.csv", "distances")),
			CSVFormat.DEFAULT)) {

			printer.print("personId");
			printer.print("vehicleId");
			printer.print("vehicleType");
			printer.print("groupOfSubpopulation");
			printer.print("dist_group");
			printer.print("distanceInKm");
			for (String group : groupsOfSubpopulationsForCommercialAnalysis.keySet())
				printer.print("distanceInKm_" + group);
			printer.print("distanceInKmWithDepotCharging");
			printer.print("shareOfTravelDistanceWithDepotCharging");
			printer.println();
			for (String label : distanceLabels) {
				for (String vehicleType : travelDistancesPerVehicle.keySet()) {
					Object2DoubleOpenHashMap<String> travelDistancesForVehiclesWithThisType = travelDistancesPerVehicle.get(vehicleType);
					for (String vehicleId : travelDistancesForVehiclesWithThisType.keySet()) {
						double traveledDistanceInMeters = travelDistancesForVehiclesWithThisType.getDouble(vehicleId);
						double traveledDistanceInKm = Math.round(traveledDistanceInMeters / 10) / 100.0;

						String labelForValue = AnalysisUtils.getLabelForValue((long) traveledDistanceInKm, distGroups, distanceLabels);
						// this needed to have the correct order of distance groups in the output, so that it is vizualized correctly later on
						if (!label.equals(labelForValue))
							continue;
						printer.print(vehicleToPersonId.get(Id.createVehicleId(vehicleId)));
						printer.print(vehicleId);
						printer.print(vehicleType);
						String groupOfSubpopulation = vehicleGroupOfSubpopulation.get(Id.createVehicleId(vehicleId));
						printer.print(groupOfSubpopulation);
						printer.print(labelForValue);
						printer.print(traveledDistanceInKm);
						for (String group : groupsOfSubpopulationsForCommercialAnalysis.keySet()) {
							if (groupOfSubpopulation.equals(group))
								printer.print(traveledDistanceInKm);
							else
								printer.print(Double.NaN);
						}
						String maxDistanceWithoutRecharging;
						if (maxDistanceWithDepotChargingInKilometers.containsKey(vehicleType)) {
							maxDistanceWithoutRecharging = String.valueOf(maxDistanceWithDepotChargingInKilometers.get(vehicleType));
							printer.print(maxDistanceWithoutRecharging);
							printer.print(String.valueOf(
								Math.round(traveledDistanceInKm / (maxDistanceWithDepotChargingInKilometers.get(vehicleType)) * 100) / 100.0));
						} else {
							log.warn("Vehicle type {} not found in maxDistanceWithDepotChargingInKilometers map. Set to NaN", vehicleType);
							printer.print(Double.NaN);
							printer.print(Double.NaN);
						}
						printer.println();
					}
				}
			}
		} catch (IOException e) {
			log.error("Could not create output file", e);
		}
	}

	private void writeAnalysisPerVehicle_durations(List<String> tourDurationLabels,
												   HashMap<String, Object2DoubleOpenHashMap<String>> travelDistancesPerVehicle,
												   HashMap<Id<Vehicle>, Double> tourDurations,
												   HashMap<Id<Vehicle>, Id<Person>> vehicleToPersonId,
												   HashMap<Id<Vehicle>, String> vehicleGroupOfSubpopulation) {
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("tourAnalysis_%s.csv", "durations")),
			CSVFormat.DEFAULT)) {

			printer.print("personId");
			printer.print("vehicleId");
			printer.print("vehicleType");
			printer.print("groupOfSubpopulation");
			printer.print("duration_group");
			printer.print("tourDurationInSeconds");
			printer.print("tourDurationsInHours");
			for (String group : groupsOfSubpopulationsForCommercialAnalysis.keySet()) {
				printer.print("tourDurationsInHours_" + group);
			}
			printer.println();
			for (String label : tourDurationLabels) {
				for (Id<Vehicle> vehicleId : tourDurations.keySet()) {
					String vehicleType = null;
					for (String type : travelDistancesPerVehicle.keySet()) {
						if (travelDistancesPerVehicle.get(type).containsKey(vehicleId.toString())) {
							vehicleType = type;
							break;
						}
					}
					Double tourDurationsInS = tourDurations.get(Id.createVehicleId(vehicleId));
					BigDecimal tourDurationInH = new BigDecimal(tourDurationsInS / 3600.).setScale(2, RoundingMode.HALF_UP);

					String labelForValue = AnalysisUtils.getLabelForValue(tourDurationInH.longValue(), tourDurationGroups, tourDurationLabels);
					// this needed to have the correct order of distance groups in the output, so that it is vizualized correctly later on
					if (!label.equals(labelForValue))
						continue;
					printer.print(vehicleToPersonId.get(vehicleId));
					printer.print(vehicleId.toString());
					printer.print(vehicleType);
					String groupOfSubpopulation = vehicleGroupOfSubpopulation.get(Id.createVehicleId(vehicleId));
					printer.print(groupOfSubpopulation);

					printer.print(labelForValue);
					printer.print(tourDurationsInS);
					printer.print(tourDurationInH);
					for (String group : groupsOfSubpopulationsForCommercialAnalysis.keySet()) {
						if (groupOfSubpopulation.equals(group)) {
							printer.print(
								tourDurationInH);
						} else {
							printer.print(Double.NaN);
						}
					}
					printer.println();
				}
			}
		} catch (IOException e) {
			log.error("Could not create output file", e);
		}
	}

	private void writeAnalysisPerVehicle_numberOfJobs(List<String> numberOfJobsLabels,
													  HashMap<String, Object2DoubleOpenHashMap<String>> travelDistancesPerVehicle,
													  HashMap<Id<Vehicle>, Id<Person>> vehicleToPersonId,
													  HashMap<Id<Vehicle>, String> vehicleGroupOfSubpopulation,
													  Object2IntOpenHashMap<Id<Person>> jobsPerPerson) {
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("tourAnalysis_%s.csv", "jobsPerTour")),
			CSVFormat.DEFAULT)) {

			printer.print("personId");
			printer.print("vehicleId");
			printer.print("vehicleType");
			printer.print("groupOfSubpopulation");
			printer.print("numberOfJobs_group");
			printer.print("jobsPerTour");
			for (String group : groupsOfSubpopulationsForCommercialAnalysis.keySet()) {
				printer.print("jobsPerTour_" + group);
			}
			printer.println();

			for (String label : numberOfJobsLabels) {
				for (Id<Person> personId : jobsPerPerson.keySet()) {
					int jobsPerTour = jobsPerPerson.getInt(personId);

					String labelForValue = AnalysisUtils.getLabelForValue(jobsPerTour, numberOfJobsGroups, numberOfJobsLabels);
					// this needed to have the correct order of distance groups in the output, so that it is vizualized correctly later on
					if (!label.equals(labelForValue))
						continue;

					String vehicleId = null;
					for (Id<Vehicle> vehicleId1 : vehicleToPersonId.keySet()) {
						if (vehicleToPersonId.get(vehicleId1).equals(personId)) {
							vehicleId = vehicleId1.toString();
							break;
						}
					}
					String vehicleType = null;
					for (String vehicleType1 : travelDistancesPerVehicle.keySet()) {
						if (travelDistancesPerVehicle.get(vehicleType1).containsKey(vehicleId)) {
							vehicleType = vehicleType1;
							break;
						}
					}
					if (vehicleId == null || vehicleType == null) {
						continue;
					}
					printer.print(personId);
					printer.print(vehicleId);
					printer.print(vehicleType);
					String groupOfSubpopulation = vehicleGroupOfSubpopulation.get(Id.createVehicleId(vehicleId));
					printer.print(groupOfSubpopulation);
					printer.print(labelForValue);
					printer.print(jobsPerTour);
					for (String group : groupsOfSubpopulationsForCommercialAnalysis.keySet()) {
						if (groupOfSubpopulation.equals(group)) {
							printer.print(jobsPerTour);
						} else {
							printer.print(Double.NaN);
						}
					}
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
		maxDistanceWithDepotChargingInKilometers.put("mercedes313_parcel",
			440); //https://www.adac.de/rund-ums-fahrzeug/autokatalog/marken-modelle/mercedes-benz/esprinter/
		maxDistanceWithDepotChargingInKilometers.put("mercedes313", 440);
		maxDistanceWithDepotChargingInKilometers.put("light8t", 174);
		maxDistanceWithDepotChargingInKilometers.put("truck8t", 174);
		maxDistanceWithDepotChargingInKilometers.put("medium18t", 395);
		maxDistanceWithDepotChargingInKilometers.put("medium18t_parcel", 395);
		maxDistanceWithDepotChargingInKilometers.put("truck18t", 395);
		maxDistanceWithDepotChargingInKilometers.put("waste_collection_diesel", 280);
		maxDistanceWithDepotChargingInKilometers.put("heavy40t", 416);
		maxDistanceWithDepotChargingInKilometers.put("heavy", 416);
		maxDistanceWithDepotChargingInKilometers.put("truck40t", 416);
		return maxDistanceWithDepotChargingInKilometers;
	}

	private void createRelationsAnalysis(CommercialTrafficAnalysisEventHandler linkDemandEventHandler) {
		Map<Integer, Object2DoubleMap<String>> relations = linkDemandEventHandler.getRelations();
		ArrayList<String> header = getHeaderForRelations(relations);
		ArrayList<Integer> relationNumbers = new ArrayList<>(relations.keySet());
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("relations.csv")), CSVFormat.DEFAULT)) {

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

	/**
	 * Extracts and sorts unique header values for relations output.
	 */
	private ArrayList<String> getHeaderForRelations(Map<Integer, Object2DoubleMap<String>> relations) {
		ArrayList<String> header = new ArrayList<>();
		for (Object2DoubleMap<String> relation : relations.values()) {
			for (String value : relation.keySet()) {
				if (!header.contains(value)) header.add(value);
			}
		}
		header.sort(Comparator.naturalOrder());
		return header;
	}

	private void createTravelDistancesShares(CommercialTrafficAnalysisEventHandler linkDemandEventHandler) {
		String filename = "travelDistancesShares.csv";
		Path path = output.getPath("travelDistancesShares_%s.csv", "perMode");
		Object2DoubleOpenHashMap<String> travelDistancesPerMode = linkDemandEventHandler.getTravelDistancesPerMode();
		writeDistanceFiles(travelDistancesPerMode, path);
		path = output.getPath("travelDistancesShares_%s.csv", "perGroup");
		Object2DoubleOpenHashMap<String> travelDistancesPerGroup = linkDemandEventHandler.getTravelDistancesPerGroup();
		writeDistanceFiles(travelDistancesPerGroup, path);
		path = output.getPath("travelDistancesShares_%s.csv", "perSubpopulation");
		Object2DoubleOpenHashMap<String> travelDistancesPerSubpopulation = linkDemandEventHandler.getTravelDistancesPerSubpopulation();
		writeDistanceFiles(travelDistancesPerSubpopulation, path);
	}

	private void writeDistanceFiles(Object2DoubleOpenHashMap<String> travelDistancesPerMode, Path path) {
		ArrayList<String> headerWithModes = new ArrayList<>(travelDistancesPerMode.keySet());
		headerWithModes.sort(Comparator.naturalOrder());
		double sumOfAllDistances = travelDistancesPerMode.values().doubleStream().sum();
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(path), CSVFormat.DEFAULT)) {

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

	public void createLinkVolumeAnalysis(Scenario scenario, CommercialTrafficAnalysisEventHandler linkDemandEventHandler) {

//		File file = new File(linkDemandOutputFile);
		Map<Id<Link>, Object2DoubleOpenHashMap<String>> linkVolumesPerMode = linkDemandEventHandler.getLinkVolumesPerMode();
		ArrayList<String> headerWithModes = new ArrayList<>(List.of("allCommercialVehicles"));
		headerWithModes.addAll(groupsOfSubpopulationsForCommercialAnalysis.keySet());
		scenario.getVehicles().getVehicleTypes().values().forEach(vehicleType -> {
			if (!headerWithModes.contains(vehicleType.getNetworkMode())) headerWithModes.add(vehicleType.getNetworkMode());
		});
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output.getPath("commercialTraffic_link_volume.csv")),
			CSVFormat.Builder.create().setDelimiter(";").get())) {
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

	record ActivityInformation(Activity activity, int countForPerson, Id<Person> personId, String groupOfSubpopulation, String startCategory) {
	}
}
