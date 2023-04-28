package org.matsim.contrib.freight.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.events.FreightTourEndEvent;
import org.matsim.contrib.freight.events.FreightTourStartEvent;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class FreightTimeAndDistanceAnalysisEventsHandler implements BasicEventHandler {

	private final static Logger log = LogManager.getLogger(FreightTimeAndDistanceAnalysisEventsHandler.class);

	private final Scenario scenario;



	private final Map<Id<Vehicle>, Double> vehicleId2TourDuration = new LinkedHashMap<>();
	private final Map<Id<Vehicle>, Double> vehicleId2TourLength = new LinkedHashMap<>();

	private final Map<Id<Vehicle>, Id<Carrier>> vehicleId2CarrierId = new LinkedHashMap<>();
	private final Map<Id<Vehicle>, Id<Tour>> vehicleId2TourId = new LinkedHashMap<>();

	private final Map<Id<VehicleType>, Double> vehicleTypeId2SumOfDuration = new LinkedHashMap<>();
	private final Map<Id<VehicleType>, Double> vehicleTypeId2Mileage = new LinkedHashMap<>();

	private final Map<Id<Vehicle>, Id<VehicleType>> vehicleId2VehicleTypeId = new LinkedHashMap<>();

	private final Map<String, Double> tourStartTime = new LinkedHashMap<>();


	public FreightTimeAndDistanceAnalysisEventsHandler(Scenario scenario) {
		this.scenario = scenario;
	}

	private void handleEvent(FreightTourStartEvent event) {
		// Save time of freight tour start
		final String key = event.getCarrierId().toString() + "_" + event.getTourId().toString();
		tourStartTime.put(key, event.getTime());
	}

	//Fix costs for vehicle usage
	private void handleEvent(FreightTourEndEvent event) {
		final String key = event.getCarrierId().toString() + "_" + event.getTourId().toString();
		double tourDuration = event.getTime() - tourStartTime.get(key);
		vehicleId2TourDuration.put(event.getVehicleId(), tourDuration);
		Id<VehicleType> vehTypeId = VehicleUtils.findVehicle(event.getVehicleId(), scenario).getType().getId();
		vehicleTypeId2SumOfDuration.merge(vehTypeId, tourDuration, Double::sum);

		//Some general information for this vehicle
		vehicleId2CarrierId.putIfAbsent(event.getVehicleId(), event.getCarrierId());
		vehicleId2TourId.putIfAbsent(event.getVehicleId(), event.getTourId());

		vehicleId2VehicleTypeId.putIfAbsent(event.getVehicleId(), vehTypeId);
	}

	private void handleEvent(LinkEnterEvent event) {
		final double distance = scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
		vehicleId2TourLength.merge(event.getVehicleId(), distance, Double::sum);

		final Id<VehicleType> vehTypeId = VehicleUtils.findVehicle(event.getVehicleId(), scenario).getType().getId();
		vehicleTypeId2Mileage.merge(vehTypeId, distance, Double::sum);
	}


	@Override public void handleEvent(Event event) {
//		log.info(event + " ; " +event.getEventType());
		if (event instanceof FreightTourStartEvent freightTourStartEvent) {
			handleEvent(freightTourStartEvent);
		} else if (event instanceof FreightTourEndEvent freightTourEndEvent) {
			handleEvent(freightTourEndEvent);
		} else if (event instanceof LinkEnterEvent linkEnterEvent) {
			handleEvent(linkEnterEvent);
		}
	}


	void writeTravelTimeAndDistance(String analysisOutputDirectory, Scenario scenario) throws IOException {
		log.info("Writing out Time & Distance & Costs ... perVehicle");
		//Travel time and distance per vehicle
		String fileName = analysisOutputDirectory + "TimeDistance_perVehicle.tsv";

		BufferedWriter bw1 = new BufferedWriter(new FileWriter(fileName));

		//Write headline:
		bw1.write("vehicleId \t carrierId \t vehicleTypeId \t tourId \t tourDuration[s] \t travelDistance[m] \t " +
				"costPerSecond[EUR/s] \t costPerMeter[EUR/m] \t fixedCosts[EUR] \t varCostsTime[EUR] \t varCostsDist[EUR] \t totalCosts[EUR]");
		bw1.newLine();

		for (Id<Vehicle> vehicleId : vehicleId2VehicleTypeId.keySet()) {

			final Double durationInSeconds = vehicleId2TourDuration.get(vehicleId);
			final Double distanceInMeters = vehicleId2TourLength.get(vehicleId);

			final VehicleType vehicleType = VehicleUtils.findVehicle(vehicleId, scenario).getType();
			final Double costsPerSecond = vehicleType.getCostInformation().getCostsPerSecond();
			final Double costsPerMeter = vehicleType.getCostInformation().getCostsPerMeter();
			final Double fixedCost = vehicleType.getCostInformation().getFixedCosts();

			final double varCostsTime = durationInSeconds * costsPerSecond;
			final double varCostsDist = distanceInMeters * costsPerMeter;
			final double totalVehCosts = fixedCost + varCostsTime + varCostsDist;

			bw1.write(vehicleId.toString());
			bw1.write("\t" + vehicleId2CarrierId.get(vehicleId));
			bw1.write("\t" + vehicleType.getId().toString());
			bw1.write("\t" + vehicleId2TourId.get(vehicleId));

			bw1.write("\t" + durationInSeconds);
			bw1.write("\t" + distanceInMeters);
			bw1.write("\t" + costsPerSecond);
			bw1.write("\t" + costsPerMeter);
			bw1.write("\t" + fixedCost);
			bw1.write("\t" + varCostsTime);
			bw1.write("\t" + varCostsDist);
			bw1.write("\t" + totalVehCosts);

			bw1.newLine();
		}

		bw1.close();
		log.info("Output written to " + fileName);
	}


	void writeTravelTimeAndDistancePerVehicleType(String analysisOutputDirectory, Scenario scenario) throws IOException {
		log.info("Writing out Time & Distance & Costs ... perVehicleType");

		//----- All VehicleTypes in vehicles container. Used so that all vehicleTypes will appear in the output
		TreeMap<Id<VehicleType>, VehicleType> vehicleTypesMap = new TreeMap<>(scenario.getVehicles().getVehicleTypes());


		String fileName = analysisOutputDirectory + "TimeDistance_perVehicleType.tsv";

		BufferedWriter bw1 = new BufferedWriter(new FileWriter(fileName));

		//Write headline:
		bw1.write("vehicleTypeId \t nuOfVehicles \t SumOfTourDuration[s] \t SumOfTravelDistances[m] \t " +
				"costPerSecond[EUR/s] \t costPerMeter[EUR/m] \t fixedCosts[EUR/veh] \t" +
				"varCostsTime[EUR] \t varCostsDist[EUR] \t fixedCosts[EUR] \t totalCosts[EUR]");
		bw1.newLine();

		for (VehicleType vehicleType : vehicleTypesMap.values()) {

			long nuOfVehicles = vehicleId2VehicleTypeId.values().stream().filter(v -> v == vehicleType.getId()).count();

			final Double costRatePerSecond = vehicleType.getCostInformation().getCostsPerSecond();
			final Double costRatePerMeter = vehicleType.getCostInformation().getCostsPerMeter();
			final Double fixedCostPerVeh = vehicleType.getCostInformation().getFixedCosts();

			final Double sumOfDurationInSeconds = vehicleTypeId2SumOfDuration.getOrDefault(vehicleType.getId(), 0.);
			final Double sumOfDistanceInMeters = vehicleTypeId2Mileage.getOrDefault(vehicleType.getId(), 0.);

			final double sumOfVarCostsTime = sumOfDurationInSeconds * costRatePerSecond;
			final double sumOfVarCostsDistance = sumOfDistanceInMeters * costRatePerMeter;
			final double sumOfFixCosts = nuOfVehicles * fixedCostPerVeh;

			bw1.write(vehicleType.getId().toString());

			bw1.write("\t" + nuOfVehicles);
			bw1.write("\t" + sumOfDurationInSeconds);
			bw1.write("\t" + sumOfDistanceInMeters);
			bw1.write("\t" + costRatePerSecond);
			bw1.write("\t" + costRatePerMeter);
			bw1.write("\t" + fixedCostPerVeh);
			bw1.write("\t" + sumOfVarCostsTime);
			bw1.write("\t" + sumOfVarCostsDistance);
			bw1.write("\t" + sumOfFixCosts);
			bw1.write("\t" + (sumOfFixCosts + sumOfVarCostsTime + sumOfVarCostsDistance));

			bw1.newLine();
		}

		bw1.close();
		log.info("Output written to " + fileName);
	}
}
