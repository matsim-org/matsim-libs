/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.carriers.events.CarrierTourEndEvent;
import org.matsim.freight.carriers.events.CarrierTourStartEvent;
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

	private final Map<Id<Vehicle>, Double> vehicleId2TravelTime = new LinkedHashMap<>();

	private final Map<Id<Vehicle>, Id<Carrier>> vehicleId2CarrierId = new LinkedHashMap<>();
	private final Map<Id<Vehicle>, Id<Tour>> vehicleId2TourId = new LinkedHashMap<>();

	private final Map<Id<VehicleType>, Double> vehicleTypeId2SumOfTourDuration = new LinkedHashMap<>();
	private final Map<Id<VehicleType>, Double> vehicleTypeId2Mileage = new LinkedHashMap<>();
	private final Map<Id<VehicleType>, Double> vehicleTypeId2TravelTime = new LinkedHashMap<>();

	private final Map<Id<Vehicle>, VehicleType> vehicleId2VehicleType = new TreeMap<>();

	private final Map<String, Double> tourStartTime = new LinkedHashMap<>();

	private final Map<Id<Vehicle>, Double> vehicleEnteredLinkTime = new LinkedHashMap<>();


	public FreightTimeAndDistanceAnalysisEventsHandler(Scenario scenario) {
		this.scenario = scenario;
	}

	private void handleEvent(CarrierTourStartEvent event) {
		// Save time of freight tour start
		final String key = event.getCarrierId().toString() + "_" + event.getTourId().toString();
		tourStartTime.put(key, event.getTime());
	}

	//Fix costs for vehicle usage
	private void handleEvent(CarrierTourEndEvent event) {
		final String key = event.getCarrierId().toString() + "_" + event.getTourId().toString();
		double tourDuration = event.getTime() - tourStartTime.get(key);
		vehicleId2TourDuration.put(event.getVehicleId(), tourDuration);
		VehicleType vehType = VehicleUtils.findVehicle(event.getVehicleId(), scenario).getType();
		vehicleTypeId2SumOfTourDuration.merge(vehType.getId(), tourDuration, Double::sum);

		//Some general information for this vehicle
		vehicleId2CarrierId.putIfAbsent(event.getVehicleId(), event.getCarrierId());
		vehicleId2TourId.putIfAbsent(event.getVehicleId(), event.getTourId());

		vehicleId2VehicleType.putIfAbsent(event.getVehicleId(), vehType);
	}

	private void handleEvent(LinkEnterEvent event) {
		final double distance = scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
		vehicleId2TourLength.merge(event.getVehicleId(), distance, Double::sum);
		vehicleEnteredLinkTime.put(event.getVehicleId(), event.getTime()); //Safe time when entering the link.

		final Id<VehicleType> vehTypeId = VehicleUtils.findVehicle(event.getVehicleId(), scenario).getType().getId();
		vehicleTypeId2Mileage.merge(vehTypeId, distance, Double::sum);
	}

	//If the vehicle leaves a link at the end, the travelTime is calculated and stored.
	private void handleEvent(LinkLeaveEvent event){
		final Id<Vehicle> vehicleId = event.getVehicleId();
		if (vehicleEnteredLinkTime.containsKey(vehicleId)){
			double tt = event.getTime() - vehicleEnteredLinkTime.get(vehicleId);
			vehicleId2TravelTime.merge(vehicleId, tt, Double::sum); //per vehicle

			final Id<VehicleType> vehTypeId = VehicleUtils.findVehicle(event.getVehicleId(), scenario).getType().getId();
			vehicleTypeId2TravelTime.merge(vehTypeId, tt, Double::sum); // per VehType

			vehicleEnteredLinkTime.remove(vehicleId); //remove from that list.
		}
	}

	//If the vehicle leaves a link because it reached its destination, the travelTime is calculated and stored.
	private void handleEvent(VehicleLeavesTrafficEvent event){
		final Id<Vehicle> vehicleId = event.getVehicleId();
		if (vehicleEnteredLinkTime.containsKey(vehicleId)){
			double tt = event.getTime() - vehicleEnteredLinkTime.get(vehicleId);
			vehicleId2TravelTime.merge(vehicleId, tt, Double::sum);//per vehicle

			final Id<VehicleType> vehTypeId = VehicleUtils.findVehicle(event.getVehicleId(), scenario).getType().getId();
			vehicleTypeId2TravelTime.merge(vehTypeId, tt, Double::sum); // per VehType

			vehicleEnteredLinkTime.remove(vehicleId); //remove from that list.
		}
	}

	private void handleEvent(VehicleEntersTrafficEvent event){
		vehicleEnteredLinkTime.put(event.getVehicleId(), event.getTime());
	}

	@Override public void handleEvent(Event event) {
		if (event instanceof CarrierTourStartEvent carrierTourStartEvent) {
			handleEvent(carrierTourStartEvent);
		} else if (event instanceof CarrierTourEndEvent carrierTourEndEvent) {
			handleEvent(carrierTourEndEvent);
		} else if (event instanceof LinkEnterEvent linkEnterEvent) {
			handleEvent(linkEnterEvent);
		} else if (event instanceof LinkLeaveEvent linkLeaveEvent) {
			handleEvent(linkLeaveEvent);
		} else if (event instanceof VehicleLeavesTrafficEvent vehicleLeavesTrafficEvent) {
			handleEvent(vehicleLeavesTrafficEvent);
		} else if (event instanceof VehicleEntersTrafficEvent vehicleEntersTrafficEvent) {
			handleEvent(vehicleEntersTrafficEvent);
		}
	}

	void writeTravelTimeAndDistancePerVehicle(String analysisOutputDirectory, Scenario scenario) throws IOException {
		log.info("Writing out Time & Distance & Costs ... perVehicle");
		//Travel time and distance per vehicle
		String fileName = analysisOutputDirectory + "TimeDistance_perVehicle.tsv";

		BufferedWriter bw1 = new BufferedWriter(new FileWriter(fileName));

		//Write headline:
		bw1.write("vehicleId \t carrierId \t vehicleTypeId \t tourId \t "
			+ "tourDuration[s] \t tourDuration[h] \t"
			+ "travelDistance[m] \t travelDistance[km] \t " +
				"travelTime[s] \t travelTime[h] \t" +
			"costPerSecond[EUR/s] \t costPerMeter[EUR/m] \t fixedCosts[EUR] \t varCostsTime[EUR] \t varCostsDist[EUR] \t totalCosts[EUR]");
		bw1.newLine();

		for (Id<Vehicle> vehicleId : vehicleId2VehicleType.keySet()) {

			final Double durationInSeconds = vehicleId2TourDuration.get(vehicleId);
			final Double distanceInMeters = vehicleId2TourLength.get(vehicleId);
			final Double travelTimeInSeconds = vehicleId2TravelTime.get(vehicleId);


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
			bw1.write("\t" + durationInSeconds /3600);

			bw1.write("\t" + distanceInMeters);
			bw1.write("\t" + distanceInMeters/1000);

			bw1.write("\t" + travelTimeInSeconds);
			bw1.write("\t" + travelTimeInSeconds /3600);

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

		//----- All VehicleTypes in CarriervehicleTypes container. Used so that even unused vehTypes appear in the output
		TreeMap<Id<VehicleType>, VehicleType> vehicleTypesMap = new TreeMap<>(CarriersUtils.getCarrierVehicleTypes(scenario).getVehicleTypes());
		//For the case that there are additional vehicle types found in the events.
		for (VehicleType vehicleType : vehicleId2VehicleType.values()) {
			vehicleTypesMap.putIfAbsent(vehicleType.getId(), vehicleType);
		}

		String fileName = analysisOutputDirectory + "TimeDistance_perVehicleType.tsv";

		BufferedWriter bw1 = new BufferedWriter(new FileWriter(fileName));
		//Write headline:
		bw1.write("vehicleTypeId \t nuOfVehicles \t " +
			 "SumOfTourDuration[s] \t SumOfTourDuration[h] \t" +
			 "SumOfTravelDistances[m] \t SumOfTravelDistances[km] \t " +
			"SumOfTravelTime[s] \t SumOfTravelTime[h] \t" +
				"costPerSecond[EUR/s] \t costPerMeter[EUR/m] \t fixedCosts[EUR/veh] \t" +
				"varCostsTime[EUR] \t varCostsDist[EUR] \t fixedCosts[EUR] \t totalCosts[EUR]");
		bw1.newLine();

		for (VehicleType vehicleType : vehicleTypesMap.values()) {
			long nuOfVehicles = vehicleId2VehicleType.values().stream().filter(vehType -> vehType.getId() == vehicleType.getId()).count();

			final Double costRatePerSecond = vehicleType.getCostInformation().getCostsPerSecond();
			final Double costRatePerMeter = vehicleType.getCostInformation().getCostsPerMeter();
			final Double fixedCostPerVeh = vehicleType.getCostInformation().getFixedCosts();

			final Double sumOfTourDurationInSeconds = vehicleTypeId2SumOfTourDuration.getOrDefault(vehicleType.getId(), 0.);
			final Double sumOfDistanceInMeters = vehicleTypeId2Mileage.getOrDefault(vehicleType.getId(), 0.);
			final Double sumOfTravelTimeInSeconds = vehicleTypeId2TravelTime.getOrDefault(vehicleType.getId(), 0.);

			final double sumOfVarCostsTime = sumOfTourDurationInSeconds * costRatePerSecond;
			final double sumOfVarCostsDistance = sumOfDistanceInMeters * costRatePerMeter;
			final double sumOfFixCosts = nuOfVehicles * fixedCostPerVeh;

			bw1.write(vehicleType.getId().toString());

			bw1.write("\t" + nuOfVehicles);
			bw1.write("\t" + sumOfTourDurationInSeconds);
			bw1.write("\t" + sumOfTourDurationInSeconds / 3600);
			bw1.write("\t" + sumOfDistanceInMeters);
			bw1.write("\t" + sumOfDistanceInMeters / 1000);
			bw1.write("\t" + sumOfTravelTimeInSeconds);
			bw1.write("\t" + sumOfTravelTimeInSeconds / 3600);
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
