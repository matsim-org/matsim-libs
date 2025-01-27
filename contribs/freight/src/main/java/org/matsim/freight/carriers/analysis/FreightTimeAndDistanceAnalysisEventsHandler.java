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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.carriers.events.CarrierTourEndEvent;
import org.matsim.freight.carriers.events.CarrierTourStartEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierTourEndEventHandler;
import org.matsim.freight.carriers.events.eventhandler.CarrierTourStartEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class FreightTimeAndDistanceAnalysisEventsHandler implements CarrierTourStartEventHandler, CarrierTourEndEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	private final static Logger log = LogManager.getLogger(FreightTimeAndDistanceAnalysisEventsHandler.class);
	private final String delimiter;

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


	public FreightTimeAndDistanceAnalysisEventsHandler(String delimiter, Scenario scenario) {
		this.delimiter = delimiter;
		this.scenario = scenario;
	}

	@Override
	public void handleEvent(CarrierTourStartEvent event) {
		// Save time of freight tour start
		final String key = event.getCarrierId().toString() + "_" + event.getTourId().toString();
		tourStartTime.put(key, event.getTime());

		//Some general information for this vehicle
		VehicleType vehType = VehicleUtils.findVehicle(event.getVehicleId(), scenario).getType();
		vehicleId2CarrierId.putIfAbsent(event.getVehicleId(), event.getCarrierId());
		vehicleId2TourId.putIfAbsent(event.getVehicleId(), event.getTourId());
		vehicleId2VehicleType.putIfAbsent(event.getVehicleId(), vehType);
		vehicleId2TourLength.putIfAbsent(event.getVehicleId(), 0.);
		vehicleId2TourDuration.putIfAbsent(event.getVehicleId(), 0.);
		vehicleId2TravelTime.putIfAbsent(event.getVehicleId(), 0.);
	}

	@Override
	public void handleEvent(CarrierTourEndEvent event) {
		final String key = event.getCarrierId().toString() + "_" + event.getTourId().toString();
		double tourDuration = event.getTime() - tourStartTime.get(key);
		vehicleId2TourDuration.put(event.getVehicleId(), tourDuration); //TODO, check if this may overwrite old data and if this is intended to do so
		VehicleType vehType = VehicleUtils.findVehicle(event.getVehicleId(), scenario).getType();
		vehicleTypeId2SumOfTourDuration.merge(vehType.getId(), tourDuration, Double::sum);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		vehicleEnteredLinkTime.put(event.getVehicleId(), event.getTime());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// necessary if non-carrier vehicles are in the simulation
		if (!vehicleId2CarrierId.containsKey(event.getVehicleId()))
			return;
		final double distance = scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
		vehicleId2TourLength.merge(event.getVehicleId(), distance, Double::sum);
		vehicleEnteredLinkTime.put(event.getVehicleId(), event.getTime()); //Safe time when entering the link.

		final Id<VehicleType> vehTypeId = VehicleUtils.findVehicle(event.getVehicleId(), scenario).getType().getId();
		vehicleTypeId2Mileage.merge(vehTypeId, distance, Double::sum);
	}

	//If the vehicle leaves a link at the end, the travelTime is calculated and stored.
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// nessessary if non-carrier vehicles are in the simulation
		if (!vehicleId2CarrierId.containsKey(event.getVehicleId()))
			return;
		final Id<Vehicle> vehicleId = event.getVehicleId();
		if (vehicleEnteredLinkTime.containsKey(vehicleId)) {
			double tt = event.getTime() - vehicleEnteredLinkTime.get(vehicleId);
			vehicleId2TravelTime.merge(vehicleId, tt, Double::sum); //per vehicle

			final Id<VehicleType> vehTypeId = VehicleUtils.findVehicle(event.getVehicleId(), scenario).getType().getId();
			vehicleTypeId2TravelTime.merge(vehTypeId, tt, Double::sum); // per VehType

			vehicleEnteredLinkTime.remove(vehicleId); //remove from that list.
		}
	}

	//If the vehicle leaves a link because it reached its destination, the travelTime is calculated and stored.
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		// necessary if non-carrier vehicles are in the simulation
		if (!vehicleId2CarrierId.containsKey(event.getVehicleId()))
			return;
		final Id<Vehicle> vehicleId = event.getVehicleId();
		if (vehicleEnteredLinkTime.containsKey(vehicleId)) {
			double tt = event.getTime() - vehicleEnteredLinkTime.get(vehicleId);
			vehicleId2TravelTime.merge(vehicleId, tt, Double::sum);//per vehicle

			final Id<VehicleType> vehTypeId = VehicleUtils.findVehicle(event.getVehicleId(), scenario).getType().getId();
			vehicleTypeId2TravelTime.merge(vehTypeId, tt, Double::sum); // per VehType

			vehicleEnteredLinkTime.remove(vehicleId); //remove from that list.
		}
	}

	void writeTravelTimeAndDistancePerCarrier(String analysisOutputDirectory, Scenario scenario) {
		log.info("Writing out Time & Distance & Costs ... perCarrier");
		//Travel time and distance per vehicle
		String fileName = Path.of(analysisOutputDirectory).resolve("TimeDistance_perCarrier.tsv").toString();

		try (BufferedWriter bw1 = new BufferedWriter(new FileWriter(fileName))) {
			//Write headline:
			bw1.write(String.join(delimiter,
				"carrierId",
				"nuOfTours",
				"tourDurations[s]",
				"tourDurations[h]",
				"travelDistances[m]",
				"travelDistances[km]",
				"travelTimes[s]",
				"travelTimes[h]",
				"fixedCosts[EUR]",
				"varCostsTime[EUR]",
				"varCostsDist[EUR]",
				"totalCosts[EUR]"));
			bw1.newLine();
			for (Id<Carrier> carrierId : CarriersUtils.getCarriers(scenario).getCarriers().keySet()) {

				final int nuOfTours = vehicleId2TourId.entrySet().stream().filter(
					entry -> vehicleId2CarrierId.get(entry.getKey()).equals(carrierId)).mapToInt(entry -> 1).sum();
				final double durationInSeconds = vehicleId2TourDuration.entrySet().stream().filter(
					entry -> vehicleId2CarrierId.get(entry.getKey()).equals(carrierId)).mapToDouble(Map.Entry::getValue).sum();
				final double distanceInMeters = vehicleId2TourLength.entrySet().stream().filter(
					entry -> vehicleId2CarrierId.get(entry.getKey()).equals(carrierId)).mapToDouble(Map.Entry::getValue).sum();
				final double travelTimeInSeconds = vehicleId2TravelTime.entrySet().stream().filter(
					entry -> vehicleId2CarrierId.get(entry.getKey()).equals(carrierId)).mapToDouble(Map.Entry::getValue).sum();

				bw1.write(carrierId.toString());
				bw1.write(delimiter + nuOfTours);

				bw1.write(delimiter + durationInSeconds);
				bw1.write(delimiter + durationInSeconds / 3600);

				bw1.write(delimiter + distanceInMeters);
				bw1.write(delimiter + distanceInMeters / 1000);

				bw1.write(delimiter + travelTimeInSeconds);
				bw1.write(delimiter + travelTimeInSeconds / 3600);

				double varCostsTime = 0.;
				double varCostsDist = 0.;
				double fixedCosts = 0.;
				double totalVehCosts = 0.;
				for (Id<Vehicle> vehicleId : vehicleId2VehicleType.keySet()) {
					if (vehicleId2CarrierId.get(vehicleId).equals(carrierId)) {
						final VehicleType vehicleType = VehicleUtils.findVehicle(vehicleId, scenario).getType();
						final Double costsPerSecond = vehicleType.getCostInformation().getCostsPerSecond();
						final Double costsPerMeter = vehicleType.getCostInformation().getCostsPerMeter();
						fixedCosts = fixedCosts + vehicleType.getCostInformation().getFixedCosts();

						varCostsTime = varCostsTime + vehicleId2TourDuration.get(vehicleId) * costsPerSecond;
						varCostsDist = varCostsDist + vehicleId2TourLength.get(vehicleId) * costsPerMeter;
					}
				}
				totalVehCosts = fixedCosts + varCostsTime + varCostsDist;
				bw1.write(delimiter + fixedCosts);
				bw1.write(delimiter + varCostsTime);
				bw1.write(delimiter + varCostsDist);
				bw1.write(delimiter + totalVehCosts);
				bw1.newLine();
			}
			bw1.close();
			log.info("Carrier event analysis output written to {}", fileName);
		} catch (IOException e) {
			log.error("Error writing to file: {}", fileName, e);
		}
	}

	void writeTravelTimeAndDistancePerVehicle(String analysisOutputDirectory, Scenario scenario) {
		log.info("Writing out Time & Distance & Costs ... perVehicle");
		//Travel time and distance per vehicle
		String fileName = Path.of(analysisOutputDirectory).resolve("TimeDistance_perVehicle.tsv").toString();

		try (BufferedWriter bw1 = new BufferedWriter(new FileWriter(fileName))) {
			//Write headline:
			bw1.write(String.join(delimiter,
				"vehicleId",
				"carrierId",
				"vehicleTypeId",
				"tourId",
				"tourDuration[s]",
				"tourDuration[h]",
				"travelDistance[m]",
				"travelDistance[km]",
				"travelTime[s]",
				"travelTime[h]",
				"costPerSecond[EUR/s]",
				"costPerMeter[EUR/m]",
				"fixedCosts[EUR]",
				"varCostsTime[EUR]",
				"varCostsDist[EUR]",
				"totalCosts[EUR]"));
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
				bw1.write(delimiter + vehicleId2CarrierId.get(vehicleId));
				bw1.write(delimiter + vehicleType.getId().toString());
				bw1.write(delimiter + vehicleId2TourId.get(vehicleId));

				bw1.write(delimiter + durationInSeconds);
				bw1.write(delimiter + durationInSeconds / 3600);

				bw1.write(delimiter + distanceInMeters);
				bw1.write(delimiter + distanceInMeters / 1000);

				bw1.write(delimiter + travelTimeInSeconds);
				bw1.write(delimiter + travelTimeInSeconds / 3600);

				bw1.write(delimiter + costsPerSecond);
				bw1.write(delimiter + costsPerMeter);
				bw1.write(delimiter + fixedCost);
				bw1.write(delimiter + varCostsTime);
				bw1.write(delimiter + varCostsDist);
				bw1.write(delimiter + totalVehCosts);

				bw1.newLine();
			}

			bw1.close();
			log.info("Vehicle event analysis output written to {}", fileName);
		} catch (IOException e) {
			log.error("Error writing to file: {}", fileName, e);
		}
	}

	void writeTravelTimeAndDistancePerVehicleType(String analysisOutputDirectory, Scenario scenario) {
		log.info("Writing out Time & Distance & Costs ... perVehicleType");

		//----- All VehicleTypes in CarrierVehicleTypes container. Used so that even unused vehTypes appear in the output
		TreeMap<Id<VehicleType>, VehicleType> vehicleTypesMap = new TreeMap<>(CarriersUtils.getCarrierVehicleTypes(scenario).getVehicleTypes());
		//For the case that there are additional vehicle types found in the events.
		for (VehicleType vehicleType : vehicleId2VehicleType.values()) {
			vehicleTypesMap.putIfAbsent(vehicleType.getId(), vehicleType);
		}

		String fileName = Path.of(analysisOutputDirectory).resolve("TimeDistance_perVehicleType.tsv").toString();

		try (BufferedWriter bw1 = new BufferedWriter(new FileWriter(fileName))) {
			//Write headline:
			bw1.write(String.join(delimiter,
				"vehicleTypeId",
				"nuOfVehicles",
				"SumOfTourDuration[s]",
				"SumOfTourDuration[h]",
				"SumOfTravelDistances[m]",
				"SumOfTravelDistances[km]",
				"SumOfTravelTime[s]",
				"SumOfTravelTime[h]",
				"costPerSecond[EUR/s]",
				"costPerMeter[EUR/m]",
				"fixedCosts[EUR/veh]",
				"varCostsTime[EUR]",
				"varCostsDist[EUR]",
				"fixedCosts[EUR]",
				"totalCosts[EUR]"));
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

				bw1.write(delimiter + nuOfVehicles);
				bw1.write(delimiter + sumOfTourDurationInSeconds);
				bw1.write(delimiter + sumOfTourDurationInSeconds / 3600);
				bw1.write(delimiter + sumOfDistanceInMeters);
				bw1.write(delimiter + sumOfDistanceInMeters / 1000);
				bw1.write(delimiter + sumOfTravelTimeInSeconds);
				bw1.write(delimiter + sumOfTravelTimeInSeconds / 3600);
				bw1.write(delimiter + costRatePerSecond);
				bw1.write(delimiter + costRatePerMeter);
				bw1.write(delimiter + fixedCostPerVeh);
				bw1.write(delimiter + sumOfVarCostsTime);
				bw1.write(delimiter + sumOfVarCostsDistance);
				bw1.write(delimiter + sumOfFixCosts);
				bw1.write(delimiter + (sumOfFixCosts + sumOfVarCostsTime + sumOfVarCostsDistance));

				bw1.newLine();
			}

			bw1.close();
			log.info("VehicleType event analysis output written to {}", fileName);
		} catch (IOException e) {
			log.error("Error writing to file: {}", fileName, e);
		}
	}
}
