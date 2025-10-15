package org.matsim.application.analysis.commercialTraffic;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

public class LinkVolumeCommercialEventHandler implements LinkLeaveEventHandler, ActivityStartEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler,ActivityEndEventHandler {

	private final Map<Id<Link>, Object2DoubleOpenHashMap<String>> linkVolumesPerMode = new HashMap<>();
	private final Object2DoubleOpenHashMap<String> travelDistancesPerMode = new Object2DoubleOpenHashMap<>();
	private final Object2DoubleOpenHashMap<String> travelDistancesPerType = new Object2DoubleOpenHashMap<>();
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

	private final HashMap<Id<Vehicle>, String> vehicleSubpopulation = new HashMap<>();
	private final HashMap<Id<Vehicle>, Id<Person>> vehicleIdToPersonId = new HashMap<>();

	private final Map<Integer, Object2DoubleMap<String>> relations = new HashMap<>();
	private final Scenario scenario;
	private final Geometry geometryInvestigationArea;
	private final double sampleSize;
	Map<String, Map<String, String>> personMap = new HashMap<>();
	private final Vehicles vehicles;

	public LinkVolumeCommercialEventHandler(Scenario scenario, String personFile, double sampleSize, ShpOptions shpInvestigationArea) {
		if (shpInvestigationArea != null)
			this.geometryInvestigationArea = shpInvestigationArea.getGeometry();
		else
			this.geometryInvestigationArea = null;
		this.scenario = scenario;
		this.sampleSize = sampleSize;

		vehicles = VehicleUtils.createVehiclesContainer();
		new  MatsimVehicleReader(vehicles).readFile(personFile.replace("_persons.csv", "_allVehicles.xml"));

		try (BufferedReader br = IOUtils.getBufferedReader(personFile)) {
			String line = br.readLine();  // Read the header line
			if (line != null) {
				// Split the header line by the delimiter to get column names
				String[] headers = line.split(";");

				// Read the rest of the lines
				while ((line = br.readLine()) != null) {
					// Split each line into values
					String[] values = line.split(";");

					// Assuming the first column is the "person"
					String person = values[0];

					// Create a map to store column values for the current person
					Map<String, String> personDetails = new HashMap<>();
					for (int i = 1; i < values.length; i++) {
						personDetails.put(headers[i], values[i]);  // Map column name to value
					}

					// Add the person and their details to the main map
					personMap.put(person, personDetails);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void reset(int iteration) {
		this.linkVolumesPerMode.clear();
		this.travelDistancesPerMode.clear();
		this.travelDistancesPerType.clear();
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
		// checks if an investigation area is defined. If yes
		if (geometryInvestigationArea != null && geometryInvestigationArea.contains(MGC.coord2Point(event.getCoord())))
			currentTrips_Started_inInvestigationArea.add(event.getPersonId());
		else
			currentTrips_Started_OutsideOrNoSelectedArea.add(event.getPersonId());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if ((currentTrips_Started_inInvestigationArea.contains(event.getPersonId()) || currentTrips_Started_OutsideOrNoSelectedArea.contains(event.getPersonId())) && currentTrip_Distance_perPerson.getDouble(event.getPersonId()) > 0) {
			boolean isInInvestigationArea = false;
			if (geometryInvestigationArea != null)
				isInInvestigationArea = geometryInvestigationArea.contains(MGC.coord2Point(event.getCoord()));
			distancesPerTrip_perPerson_all.computeIfAbsent(event.getPersonId(), k -> new ArrayList<>()).add(
				currentTrip_Distance_perPerson.getDouble(event.getPersonId()));
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
				} else {
					// transit trips or if no investigation area is defined all trips
					distancesPerTrip_perPerson_transit.computeIfAbsent(event.getPersonId(), k -> new ArrayList<>()).add(
						currentTrip_Distance_perPerson.removeDouble(event.getPersonId()));
					distancesPerTrip_perPerson_transit_inInvestigationArea.computeIfAbsent(event.getPersonId(), k -> new ArrayList<>()).add(
						currentTrip_Distance_perPerson_inInvestigationArea.removeDouble(event.getPersonId()));
				}
				currentTrips_Started_OutsideOrNoSelectedArea.remove(event.getPersonId());
			}
		}

		String personID = event.getPersonId().toString();
		Map<String, String> personAttributes = personMap.get(personID);
		String subpopulation = personAttributes.get("subpopulation").replace("_trip", "").replace("_service", "");

		if (subpopulation.equals("goodsTraffic") || subpopulation.contains("commercialPersonTraffic")) {
			if (event.getActType().contains("end")) {
				return;
			}
			if (event.getActType().equals("service")) {
				relations.computeIfAbsent(relations.size(), (k) -> new Object2DoubleOpenHashMap<>()).putIfAbsent(subpopulation + "_service_X",
					event.getCoord().getX());
				relations.get(relations.size() - 1).put(subpopulation + "_service_Y", event.getCoord().getY());
				relations.get(relations.size() - 1).put(subpopulation + "_start_X", Double.parseDouble(personAttributes.get("first_act_x")));
				relations.get(relations.size() - 1).put(subpopulation + "_start_Y", Double.parseDouble(personAttributes.get("first_act_y")));
			}
		}
		else if (subpopulation.equals("longDistanceFreight") || subpopulation.equals("FTL_kv") || subpopulation.equals("FTL")) {
			if (event.getActType().equals("freight_end")) {
				relations.computeIfAbsent(relations.size(), (k) -> new Object2DoubleOpenHashMap<>()).putIfAbsent(subpopulation + "_end_X",
					event.getCoord().getX());
				relations.get(relations.size() - 1).put(subpopulation + "_end_Y", event.getCoord().getY());
				relations.get(relations.size() - 1).put(subpopulation + "_start_X", Double.parseDouble(personAttributes.get("first_act_x")));
				relations.get(relations.size() - 1).put(subpopulation + "_start_Y", Double.parseDouble(personAttributes.get("first_act_y")));
			}
		}
		else if (subpopulation.equals("LTL")) {
			if (personID.contains("WasteCollection")) {
				subpopulation = subpopulation.replace("LTL", "WasteCollection");
				if (event.getActType().equals("pickup")) {
					relations.computeIfAbsent(relations.size(), (k) -> new Object2DoubleOpenHashMap<>()).putIfAbsent(subpopulation + "_pickup_X",
						event.getCoord().getX());
					relations.get(relations.size() - 1).put(subpopulation + "_pickup_Y", event.getCoord().getY());
					relations.get(relations.size() - 1).put(subpopulation + "_delivery_X", Double.parseDouble(personAttributes.get("first_act_x")));
					relations.get(relations.size() - 1).put(subpopulation + "_delivery_Y", Double.parseDouble(personAttributes.get("first_act_y")));
				}
				return;
			}
			if (event.getActType().equals("delivery")) {
				if (personID.contains("ParcelDelivery"))
					subpopulation = subpopulation.replace("LTL", "ParcelDelivery");
				relations.computeIfAbsent(relations.size(), (k) -> new Object2DoubleOpenHashMap<>()).putIfAbsent(subpopulation + "_delivery_X",
					event.getCoord().getX());
				relations.get(relations.size() - 1).put(subpopulation + "_delivery_Y", event.getCoord().getY());
				relations.get(relations.size() - 1).put(subpopulation + "_pickup_X", Double.parseDouble(personAttributes.get("first_act_x")));
				relations.get(relations.size() - 1).put(subpopulation + "_pickup_Y", Double.parseDouble(personAttributes.get("first_act_y")));
			}
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (event.getLinkId().toString().contains("pt_") || !vehicleSubpopulation.containsKey(event.getVehicleId()))
			return;
		String mode = scenario.getVehicles().getVehicles().get(event.getVehicleId()).getType().getNetworkMode();
		Link link = scenario.getNetwork().getLinks().get(event.getLinkId());

		linkVolumesPerMode.computeIfAbsent(event.getLinkId(), (k) -> new Object2DoubleOpenHashMap<>());
		int factorForSampleOfInput = (int) (1/sampleSize);

		boolean isInInvestigationArea = false;
		if (geometryInvestigationArea != null)
			isInInvestigationArea = geometryInvestigationArea.contains(MGC.coord2Point(link.getCoord()));
		String modelType = null;
		if (event.getVehicleId().toString().contains("goodsTraffic_") || event.getVehicleId().toString().contains("commercialPersonTraffic")) {
			modelType = "Small-Scale-Commercial-Traffic";
		}
		else if (event.getVehicleId().toString().contains("longDistanceFreight")) {
			modelType = "Long-Distance-Freight-Traffic";
		}
		else if (event.getVehicleId().toString().contains("FTL_kv")) {
			modelType = "FTL_kv-Traffic";
		}
		else if (event.getVehicleId().toString().contains("FTL")) {
			modelType = "FTL-Traffic";
		}
		else if (event.getVehicleId().toString().contains("ParcelDelivery_")) {
			modelType = "KEP";

		}
		else if (event.getVehicleId().toString().contains("WasteCollection_")) {
			modelType = "WasteCollection";
		}
		else if (event.getVehicleId().toString().contains("GoodsType_")) {
			modelType = "LTL-Traffic";
		}
		else if (event.getVehicleId().toString().contains("freight_") && !event.getVehicleId().toString().contains("FTL")) {
			modelType = "Transit-Freight-Traffic";
		}
		else if (vehicleSubpopulation.get(event.getVehicleId()).equals("person"))
			modelType = "Person";

		// Add the link volume for the specific modes or modelTypes
		linkVolumesPerMode.get(event.getLinkId()).mergeDouble(modelType, factorForSampleOfInput, Double::sum);
		linkVolumesPerMode.get(event.getLinkId()).mergeDouble("allCommercialVehicles", factorForSampleOfInput, Double::sum);
		linkVolumesPerMode.get(event.getLinkId()).mergeDouble(mode, factorForSampleOfInput, Double::sum);

		String vehicleType = vehicles.getVehicles().get(event.getVehicleId()).getType().getId().toString();
		travelDistancesPerVehicle.computeIfAbsent(vehicleType, (k) -> new Object2DoubleOpenHashMap<>()).mergeDouble(event.getVehicleId().toString(), link.getLength(), Double::sum);

		currentTrip_Distance_perPerson.merge(vehicleIdToPersonId.get(event.getVehicleId()), link.getLength(), Double::sum);
		if (isInInvestigationArea) {
			currentTrip_Distance_perPerson_inInvestigationArea.mergeDouble(vehicleIdToPersonId.get(event.getVehicleId()), link.getLength(), Double::sum);
			travelDistancesPerType.mergeDouble(modelType, link.getLength(), Double::sum);
			travelDistancesPerSubpopulation.mergeDouble(vehicleSubpopulation.get(event.getVehicleId()), link.getLength(), Double::sum);
			travelDistancesPerMode.mergeDouble(mode, link.getLength(), Double::sum);
			travelDistancesPerVehicle_inInvestigationArea.computeIfAbsent(vehicleType, (k) -> new Object2DoubleOpenHashMap<>()).mergeDouble(event.getVehicleId().toString(), link.getLength(), Double::sum);
		}
	}
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (event.getLinkId().toString().contains("pt_") || event.getNetworkMode().equals("bike"))
			return;

		String subpopulation = personMap.get(event.getPersonId().toString()).get("subpopulation");
		tourStartPerPerson.computeIfAbsent(event.getVehicleId(), (k) -> event.getTime());
		vehicleSubpopulation.computeIfAbsent(event.getVehicleId(), (k) -> subpopulation);
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
	public Object2DoubleOpenHashMap<String> getTravelDistancesPerType() {
		return travelDistancesPerType;
	}
	public HashMap<String, Object2DoubleOpenHashMap<String>> getTravelDistancesPerVehicle() {
		return travelDistancesPerVehicle;
	}
	public HashMap<Id<Vehicle>, String> getVehicleSubpopulation() {
		return vehicleSubpopulation;
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

	public HashMap<Id<Vehicle>, Double> getTourDurationPerPerson() {
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
}

