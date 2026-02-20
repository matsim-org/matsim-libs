package org.matsim.application.analysis.commercialTraffic;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.analysis.population.TripAnalysis;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.stream.Collectors;


public class CommercialTrafficAnalysisEventHandler implements LinkLeaveEventHandler, ActivityStartEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, ActivityEndEventHandler {

	private static final Logger log = LogManager.getLogger(CommercialTrafficAnalysisEventHandler.class);

	private final Map<Id<Link>, Object2DoubleOpenHashMap<String>> linkVolumesPerMode = new HashMap<>();
	private final Object2DoubleOpenHashMap<String> travelDistancesPerMode = new Object2DoubleOpenHashMap<>();
	private final Object2DoubleOpenHashMap<String> travelDistancesPerGroup = new Object2DoubleOpenHashMap<>();
	private final Object2DoubleOpenHashMap<String> travelDistancesPerSubpopulation = new Object2DoubleOpenHashMap<>();
	private final HashMap<String, Object2DoubleOpenHashMap<String>> travelDistancesPerVehicle = new HashMap<>();
	private final HashMap<String, Object2DoubleOpenHashMap<String>> travelDistancesPerVehicle_inInvestigationArea = new HashMap<>();

	private final HashMap<Id<Vehicle>, Double> tourStartPerPerson = new HashMap<>();
	private final HashMap<Id<Vehicle>, Double> tourEndPerPerson = new HashMap<>();

	private final List<Id<Person>> currentTrips_Started_inInvestigationArea = new ArrayList<>();
	private final List<Id<Person>> currentTrips_Started_OutsideOrNoSelectedArea = new ArrayList<>();

	private final HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_internal = new HashMap<>();
	private final HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_internal_inInvestigationArea = new HashMap<>();
	private final HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_incoming = new HashMap<>();
	private final HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_incoming_inInvestigationArea = new HashMap<>();
	private final HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_outgoing = new HashMap<>();
	private final HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_outgoing_inInvestigationArea = new HashMap<>();
	private final HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_transit = new HashMap<>();
	private final HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_transit_inInvestigationArea = new HashMap<>();
	private final HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_all = new HashMap<>();
	private final HashMap<Id<Person>, List<Double>> distancesPerTrip_perPerson_all_inInvestigationArea = new HashMap<>();

	private final Object2DoubleOpenHashMap<Id<Person>> currentTrip_Distance_perPerson = new Object2DoubleOpenHashMap<>();
	private final Object2DoubleOpenHashMap<Id<Person>> currentTrip_Distance_perPerson_inInvestigationArea = new Object2DoubleOpenHashMap<>();
	private final Object2IntOpenHashMap<Id<Person>> numberOfJobsPerPerson = new Object2IntOpenHashMap<>();


	private final HashMap<Id<Vehicle>, String> groupOfRelevantVehicles = new HashMap<>();
	private final HashMap<Id<Vehicle>, Id<Person>> vehicleIdToPersonId = new HashMap<>();

	private final Map<Integer, Object2DoubleMap<String>> relations = new HashMap<>();
	private final Scenario scenario;
	private final Geometry geometryInvestigationArea;
	private final double sampleSize;
	private final Map<String, List<String>> groupsOfSubpopulationsForCommercialAnalysis;

	public CommercialTrafficAnalysisEventHandler(Scenario scenario, double sampleSize, ShpOptions shpInvestigationArea,
												 Map<String, List<String>> groupsOfSubpopulationsForCommercialAnalysis) {
		if (shpInvestigationArea.getShapeFile() != null)
			this.geometryInvestigationArea = shpInvestigationArea.getGeometry();
		else
			this.geometryInvestigationArea = null;
		this.scenario = scenario;
		this.sampleSize = sampleSize;
		this.groupsOfSubpopulationsForCommercialAnalysis = groupsOfSubpopulationsForCommercialAnalysis;
	}

	@Override
	public void reset(int iteration) {
		this.linkVolumesPerMode.clear();
		this.travelDistancesPerMode.clear();
		this.travelDistancesPerGroup.clear();
		this.travelDistancesPerSubpopulation.clear();
		this.travelDistancesPerVehicle.clear();
		this.travelDistancesPerVehicle_inInvestigationArea.clear();
		this.currentTrip_Distance_perPerson.clear();
		this.currentTrip_Distance_perPerson_inInvestigationArea.clear();
		this.distancesPerTrip_perPerson_internal.clear();
		this.distancesPerTrip_perPerson_internal_inInvestigationArea.clear();
		this.distancesPerTrip_perPerson_incoming.clear();
		this.distancesPerTrip_perPerson_incoming_inInvestigationArea.clear();
		this.distancesPerTrip_perPerson_outgoing.clear();
		this.distancesPerTrip_perPerson_outgoing_inInvestigationArea.clear();
		this.distancesPerTrip_perPerson_transit.clear();
		this.distancesPerTrip_perPerson_transit_inInvestigationArea.clear();
		this.distancesPerTrip_perPerson_all.clear();
		this.distancesPerTrip_perPerson_all_inInvestigationArea.clear();
		this.currentTrips_Started_inInvestigationArea.clear();
		this.currentTrips_Started_OutsideOrNoSelectedArea.clear();
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (TripStructureUtils.isStageActivityType(event.getActType()))
			return;
		// checks if an investigation area is defined. If yes
		if (geometryInvestigationArea != null && geometryInvestigationArea.contains(MGC.coord2Point(event.getCoord())))
			currentTrips_Started_inInvestigationArea.add(event.getPersonId());
		else
			currentTrips_Started_OutsideOrNoSelectedArea.add(event.getPersonId());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (TripStructureUtils.isStageActivityType(event.getActType()))
			return;
		if ((currentTrips_Started_inInvestigationArea.contains(event.getPersonId()) || currentTrips_Started_OutsideOrNoSelectedArea.contains(event.getPersonId())) && currentTrip_Distance_perPerson.getDouble(event.getPersonId()) > 0) {
			boolean isInInvestigationArea = false;
			boolean useShpFile = false;
			if (geometryInvestigationArea != null) {
				isInInvestigationArea = geometryInvestigationArea.contains(MGC.coord2Point(event.getCoord()));
				useShpFile = true;
			}
			distancesPerTrip_perPerson_all.computeIfAbsent(event.getPersonId(), k -> new ArrayList<>()).add(
				currentTrip_Distance_perPerson.getDouble(event.getPersonId()));
			if (useShpFile)
				distancesPerTrip_perPerson_all_inInvestigationArea.computeIfAbsent(event.getPersonId(), k -> new ArrayList<>()).add(
				currentTrip_Distance_perPerson_inInvestigationArea.getDouble(event.getPersonId()));
			if (currentTrips_Started_inInvestigationArea.contains(event.getPersonId())) {
				if (isInInvestigationArea) {
					//internal trips
					distancesPerTrip_perPerson_internal.computeIfAbsent(event.getPersonId(), k -> new ArrayList<>()).add(
						currentTrip_Distance_perPerson.removeDouble(event.getPersonId()));
					distancesPerTrip_perPerson_internal_inInvestigationArea.computeIfAbsent(event.getPersonId(), k -> new ArrayList<>()).add(
						currentTrip_Distance_perPerson_inInvestigationArea.removeDouble(event.getPersonId()));
				} else {
					// outgoing trips
					distancesPerTrip_perPerson_outgoing.computeIfAbsent(event.getPersonId(), k -> new ArrayList<>()).add(
						currentTrip_Distance_perPerson.removeDouble(event.getPersonId()));
					distancesPerTrip_perPerson_outgoing_inInvestigationArea.computeIfAbsent(event.getPersonId(), k -> new ArrayList<>()).add(
						currentTrip_Distance_perPerson_inInvestigationArea.removeDouble(event.getPersonId()));
				}
				currentTrips_Started_inInvestigationArea.remove(event.getPersonId());
			} else if (currentTrips_Started_OutsideOrNoSelectedArea.contains(event.getPersonId())) {
				if (isInInvestigationArea) {
					// incoming trips
					distancesPerTrip_perPerson_incoming.computeIfAbsent(event.getPersonId(), k -> new ArrayList<>()).add(
						currentTrip_Distance_perPerson.removeDouble(event.getPersonId()));
					distancesPerTrip_perPerson_incoming_inInvestigationArea.computeIfAbsent(event.getPersonId(), k -> new ArrayList<>()).add(
						currentTrip_Distance_perPerson_inInvestigationArea.removeDouble(event.getPersonId()));
				} else if (useShpFile) {
					// transit trips or if no investigation area is defined all trips
					distancesPerTrip_perPerson_transit.computeIfAbsent(event.getPersonId(), k -> new ArrayList<>()).add(
						currentTrip_Distance_perPerson.removeDouble(event.getPersonId()));
					distancesPerTrip_perPerson_transit_inInvestigationArea.computeIfAbsent(event.getPersonId(), k -> new ArrayList<>()).add(
						currentTrip_Distance_perPerson_inInvestigationArea.removeDouble(event.getPersonId()));
				}
				else
					currentTrip_Distance_perPerson.removeDouble(event.getPersonId()); // because it was not removed before, because no shapefile was used
				currentTrips_Started_OutsideOrNoSelectedArea.remove(event.getPersonId());
			}
		}

		Person person = scenario.getPopulation().getPersons().get(event.getPersonId());
		String group = getGroupOfSubpopulation(PopulationUtils.getSubpopulation(person));
		List<Activity> activities = PopulationUtils.getActivities(person.getSelectedPlan(),
			TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
		Coord startCoord = activities.getFirst().getCoord();
		Set<String> activitiesTypes = activities.subList(1,activities.size()-1).stream()
			.map(Activity::getType)
			.collect(Collectors.toSet());
		// only consider relations of the jobs and not the first and last activity of a commercial tour, when the list of activities is empty, this is an A to B trip, and we consider it as well
		if (group.equals(TripAnalysis.ModelType.UNASSIGNED.toString()) ||(!activitiesTypes.isEmpty() && !activitiesTypes.contains(event.getActType()))) {
			return;
		}
		numberOfJobsPerPerson.mergeInt(event.getPersonId(), 1, Integer::sum);
		relations.computeIfAbsent(relations.size(), (k) -> new Object2DoubleOpenHashMap<>()).putIfAbsent(group + "_act_X",
			event.getCoord().getX());
		relations.get(relations.size() - 1).put(group + "_act_Y", event.getCoord().getY());
		relations.get(relations.size() - 1).put(group + "_start_X", startCoord.getX());
		relations.get(relations.size() - 1).put(group + "_start_Y", startCoord.getY());
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (!groupOfRelevantVehicles.containsKey(event.getVehicleId()))
			return;
		String mode = scenario.getVehicles().getVehicles().get(event.getVehicleId()).getType().getNetworkMode();
		Link link = scenario.getNetwork().getLinks().get(event.getLinkId());

		linkVolumesPerMode.computeIfAbsent(event.getLinkId(), (k) -> new Object2DoubleOpenHashMap<>());
		int factorForSampleOfInput = (int) (1/sampleSize);

		boolean isInInvestigationArea = false;
		if (geometryInvestigationArea != null)
			isInInvestigationArea = geometryInvestigationArea.contains(MGC.coord2Point(link.getCoord()));
		String group = groupOfRelevantVehicles.get(event.getVehicleId());

		// Add the link volume for the specific modes or modelTypes
		linkVolumesPerMode.get(event.getLinkId()).mergeDouble(group, factorForSampleOfInput, Double::sum);
		linkVolumesPerMode.get(event.getLinkId()).mergeDouble("allCommercialVehicles", factorForSampleOfInput, Double::sum);
		linkVolumesPerMode.get(event.getLinkId()).mergeDouble(mode, factorForSampleOfInput, Double::sum);

		String vehicleType = scenario.getVehicles().getVehicles().get(event.getVehicleId()).getType().getId().toString();
		travelDistancesPerVehicle.computeIfAbsent(vehicleType, (k) -> new Object2DoubleOpenHashMap<>()).mergeDouble(event.getVehicleId().toString(), link.getLength(), Double::sum);

		currentTrip_Distance_perPerson.merge(vehicleIdToPersonId.get(event.getVehicleId()), link.getLength(), Double::sum);
		if (isInInvestigationArea || geometryInvestigationArea == null) {
			currentTrip_Distance_perPerson_inInvestigationArea.mergeDouble(vehicleIdToPersonId.get(event.getVehicleId()), link.getLength(), Double::sum);
			travelDistancesPerGroup.mergeDouble(group, link.getLength(), Double::sum);
			travelDistancesPerSubpopulation.mergeDouble(PopulationUtils.getSubpopulation(scenario.getPopulation().getPersons().get(vehicleIdToPersonId.get(event.getVehicleId()))), link.getLength(), Double::sum);
			travelDistancesPerMode.mergeDouble(mode, link.getLength(), Double::sum);
			travelDistancesPerVehicle_inInvestigationArea.computeIfAbsent(vehicleType, (k) -> new Object2DoubleOpenHashMap<>()).mergeDouble(event.getVehicleId().toString(), link.getLength(), Double::sum);
		}
	}
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {

		Person person = scenario.getPopulation().getPersons().get(event.getPersonId());
		String group = getGroupOfSubpopulation(PopulationUtils.getSubpopulation(person));

		if (group.equals(TripAnalysis.ModelType.UNASSIGNED.toString()))
			return;
		String vehicleType = scenario.getVehicles().getVehicles().get(event.getVehicleId()).getType().getId().toString();
		travelDistancesPerVehicle.computeIfAbsent(vehicleType, (_) -> new Object2DoubleOpenHashMap<>()).mergeDouble(event.getVehicleId().toString(), 0, Double::sum);

		tourStartPerPerson.computeIfAbsent(event.getVehicleId(), (_) -> event.getTime());
		groupOfRelevantVehicles.computeIfAbsent(event.getVehicleId(), (_) -> group);
		vehicleIdToPersonId.put(event.getVehicleId(), event.getPersonId());
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		tourEndPerPerson.put(event.getVehicleId(), event.getTime());
	}
	public Map<Id<Link>, Object2DoubleOpenHashMap<String>> getLinkVolumesPerMode() {
		return linkVolumesPerMode;
	}

	public Object2DoubleOpenHashMap<String> getTravelDistancesPerMode() {
		return travelDistancesPerMode;
	}

	public Object2DoubleOpenHashMap<String> getTravelDistancesPerSubpopulation () {
		return travelDistancesPerSubpopulation;
	}
	public Object2DoubleOpenHashMap<String> getTravelDistancesPerGroup() {
		return travelDistancesPerGroup;
	}
	public HashMap<String, Object2DoubleOpenHashMap<String>> getTravelDistancesPerVehicle() {
		return travelDistancesPerVehicle;
	}
	public HashMap<Id<Vehicle>, String> getGroupOfRelevantVehicles() {
		return groupOfRelevantVehicles;
	}

	public Map<Integer, Object2DoubleMap<String>> getRelations() {
		return relations;
	}
	public HashMap<Id<Person>, List<Double>> getDistancesPerTrip_perPerson_internal() {
		return distancesPerTrip_perPerson_internal;
	}
	public HashMap<Id<Person>, List<Double>> getDistancesPerTrip_perPerson_internal_inInvestigationArea() {
		return distancesPerTrip_perPerson_internal_inInvestigationArea;
	}
	public HashMap<Id<Person>, List<Double>> getDistancesPerTrip_perPerson_incoming() {
		return distancesPerTrip_perPerson_incoming;
	}
	public HashMap<Id<Person>, List<Double>> getDistancesPerTrip_perPerson_incoming_inInvestigationArea() {
		return distancesPerTrip_perPerson_incoming_inInvestigationArea;
	}
	public HashMap<Id<Person>, List<Double>> getDistancesPerTrip_perPerson_outgoing() {
		return distancesPerTrip_perPerson_outgoing;
	}
	public HashMap<Id<Person>, List<Double>> getDistancesPerTrip_perPerson_outgoing_inInvestigationArea() {
		return distancesPerTrip_perPerson_outgoing_inInvestigationArea;
	}
	public HashMap<Id<Person>, List<Double>> getDistancesPerTrip_perPerson_transit() {
		return distancesPerTrip_perPerson_transit;
	}
	public HashMap<Id<Person>, List<Double>> getDistancesPerTrip_perPerson_transit_inInvestigationArea() {
		return distancesPerTrip_perPerson_transit_inInvestigationArea;
	}
	public HashMap<Id<Person>, List<Double>> getDistancesPerTrip_perPerson_all() {
		return distancesPerTrip_perPerson_all;
	}
	public HashMap<Id<Person>, List<Double>> getDistancesPerTrip_perPerson_all_inInvestigationArea() {
		return distancesPerTrip_perPerson_all_inInvestigationArea;
	}

	public HashMap<Id<Vehicle>, Double> getTourDurationPerVehicle() {
		HashMap<Id<Vehicle>, Double> tourDurationPerPerson = new HashMap<>();
		for (Id<Vehicle> vehicleId : tourStartPerPerson.keySet()) {
			if (tourEndPerPerson.containsKey(vehicleId)) {
				tourDurationPerPerson.put(vehicleId, tourEndPerPerson.get(vehicleId) - tourStartPerPerson.get(vehicleId));
			}
		}
		return tourDurationPerPerson;
	}

	public HashMap<Id<Vehicle>, Id<Person>> getVehicleIdToPersonId() {
		return vehicleIdToPersonId;
	}

	public Object2IntOpenHashMap<Id<Person>> getNumberOfJobsPerPerson() {
		return numberOfJobsPerPerson;
	}

	/**
	 * Gets the group name of a subpopulation. If the subpopulation is not assigned to any group, NO_GROUP_ASSIGNED is returned.
	 *
	 * @return the group name of the subpopulation or NO_GROUP_ASSIGNED if the subpopulation is not assigned to any group.
	 */
	private String getGroupOfSubpopulation(String subpopulation) {
		String group = null;
		for (Map.Entry<String, List<String>> entry : groupsOfSubpopulationsForCommercialAnalysis.entrySet()) {
			if (entry.getValue().contains(subpopulation)) {
				if (group != null)
					log.warn("Subpopulation {} is assigned to multiple groups. Returning the first group {}. Other group is {}", subpopulation, group,
						entry.getKey());
				else
					group = entry.getKey();
			}
		}
		if (group != null)
			return group;
		else
			return TripAnalysis.ModelType.UNASSIGNED.toString();
	}
}

