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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.ScheduledTour;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.carriers.events.CarrierTourEndEvent;
import org.matsim.freight.carriers.events.CarrierTourStartEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierTourEndEventHandler;
import org.matsim.freight.carriers.events.eventhandler.CarrierTourStartEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author Kai Martins-Turner (kturner), Ricardo Ewert
 */
/*package-private*/ class CarrierTimeAndDistanceAnalysis implements CarrierTourStartEventHandler, CarrierTourEndEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	private final static Logger log = LogManager.getLogger(CarrierTimeAndDistanceAnalysis.class);
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


	/*package-private*/ CarrierTimeAndDistanceAnalysis(String delimiter, Scenario scenario) {
		this.delimiter = delimiter;
		this.scenario = scenario;
	}

	/*package-private*/ void collectFromSelectedCarrierPlans() {
		for (Carrier carrier : CarriersUtils.getCarriers(scenario).getCarriers().values()) {
			if (carrier.getSelectedPlan() == null) {
				continue;
			}
			for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
				collectTourStats(carrier, scheduledTour);
			}
		}
	}

	private void collectTourStats(Carrier carrier, ScheduledTour scheduledTour) {
		Id<Vehicle> vehicleId = scheduledTour.getVehicle().getId();
		Id<Tour> tourId = scheduledTour.getTour().getId();
		VehicleType vehType = scheduledTour.getVehicle().getType();

		double tourDuration = 0.;
		double travelTime = 0.;
		double distance = 0.;

		for (Tour.TourElement tourElement : scheduledTour.getTour().getTourElements()) {
			log.info("tourElement: {}", tourElement);
			if (tourElement instanceof Tour.Leg leg) {
				double legTravelTime = getLegTravelTime(leg, vehType);
				travelTime += legTravelTime;
				tourDuration += legTravelTime;
				distance += getLegDistance(leg);
			} else if (tourElement instanceof Tour.TourActivity activity) {
				tourDuration += getActivityDuration(activity);
			}
		}

		vehicleId2CarrierId.put(vehicleId, carrier.getId());
		vehicleId2TourId.put(vehicleId, tourId);
		vehicleId2VehicleType.put(vehicleId, vehType);
		vehicleId2TourLength.put(vehicleId, distance);
		vehicleId2TourDuration.put(vehicleId, tourDuration);
		vehicleId2TravelTime.put(vehicleId, travelTime);

		vehicleTypeId2SumOfTourDuration.merge(vehType.getId(), tourDuration, Double::sum);
		vehicleTypeId2Mileage.merge(vehType.getId(), distance, Double::sum);
		vehicleTypeId2TravelTime.merge(vehType.getId(), travelTime, Double::sum);
	}

	private double getLegDistance(Tour.Leg leg) {
		if (leg.getRoute() != null && Double.isFinite(leg.getRoute().getDistance()) && leg.getRoute().getDistance() > 0) {
			return leg.getRoute().getDistance();
		}

		if (!(leg.getRoute() instanceof NetworkRoute networkRoute)) {
			return 0.;
		}

		if (isStationaryLeg(networkRoute)) {
			return 0.;
		}

		double distance = 0.;
		for (Id<Link> linkId : networkRoute.getLinkIds()) {
			distance += getLinkLength(linkId);
		}
		distance += getLinkLength(networkRoute.getEndLinkId());
		return distance;
	}

	private double getLegTravelTime(Tour.Leg leg, VehicleType vehicleType) {
		if (!(leg.getRoute() instanceof NetworkRoute networkRoute)) {
			return leg.getExpectedTransportTime();
		}

		if (isStationaryLeg(networkRoute)) {
			return 0.;
		}

		// Reconstruct travel time from the routed links instead of trusting expectedTransportTime from the
		// carriers file. The XML stores expected_transp_time only with second precision, so reloading the
		// carrier plan truncates fractional seconds and drifts away from the event-based analysis.
		double travelTime = 0.;
		for (Id<Link> linkId : networkRoute.getLinkIds()) {
			Double linkTravelTime = getDiscretizedLinkTravelTime(linkId, vehicleType);
			if (linkTravelTime == null) {
				return leg.getExpectedTransportTime();
			}
			travelTime += linkTravelTime;
		}

		Double endLinkTravelTime = getDiscretizedLinkTravelTime(networkRoute.getEndLinkId(), vehicleType);
		if (endLinkTravelTime == null) {
			return leg.getExpectedTransportTime();
		}
		return travelTime + endLinkTravelTime;
	}

	private boolean isStationaryLeg(NetworkRoute networkRoute) {
		return networkRoute.getStartLinkId() == networkRoute.getEndLinkId()
			&& (networkRoute.getLinkIds() == null || networkRoute.getLinkIds().isEmpty());
	}

	private Double getDiscretizedLinkTravelTime(Id<Link> linkId, VehicleType vehicleType) {
		Link link = scenario.getNetwork().getLinks().get(linkId);
		if (link == null) {
			return null;
		}

		double maximumVelocity = vehicleType.getMaximumVelocity();
		if (!Double.isFinite(maximumVelocity) || maximumVelocity <= 0.) {
			maximumVelocity = link.getFreespeed();
		}
		double velocity = Math.min(maximumVelocity, link.getFreespeed());
		if (!Double.isFinite(velocity) || velocity <= 0.) {
			return null;
		}

		double rawTravelTime = link.getLength() / velocity;
		if (rawTravelTime <= 0.) {
			return 0.;
		}

		// QSim operates on discrete time steps. A link with a fractional free-speed travel time therefore
		// effectively consumes the next full simulation step in the event stream. Applying the same
		// discretization here keeps carriersStatsAndDetailedTourAnalysisBasedOnCarrierPlans aligned with the event-based outputs.
		double timeStepSize = scenario.getConfig().qsim().getTimeStepSize();
		if (!Double.isFinite(timeStepSize) || timeStepSize <= 0.) {
			timeStepSize = 1.;
		}
		return timeStepSize * Math.ceil(rawTravelTime / timeStepSize);
	}

	private double getActivityDuration(Tour.TourActivity activity) {
		double duration = activity.getDuration();
		if (duration <= 0.) {
			return getTimeStepSize();
		}
		// Activities also advance in discrete QSim steps, so the realized tour duration is one time step
		// longer than the nominal activity duration seen in the carrier plan.
		return duration + getTimeStepSize();
	}

	private double getTimeStepSize() {
		double timeStepSize = scenario.getConfig().qsim().getTimeStepSize();
		if (!Double.isFinite(timeStepSize) || timeStepSize <= 0.) {
			return 1.;
		}
		return timeStepSize;
	}

	private double getLinkLength(Id<Link> linkId) {
		Link link = scenario.getNetwork().getLinks().get(linkId);
		return link == null ? 0. : link.getLength();
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
		// necessary if non-carrier vehicles are in the simulation
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

	/*package-private*/ void writeTravelTimeAndDistancePerCarrier(String analysisOutputDirectory, Scenario scenario) {
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
				for (Id<Vehicle> vehicleId : vehicleId2VehicleType.keySet()) {
					if (vehicleId2CarrierId.get(vehicleId).equals(carrierId)) {
						final VehicleType vehicleType = vehicleId2VehicleType.get(vehicleId);
						final Double costsPerSecond = vehicleType.getCostInformation().getCostsPerSecond();
						final Double costsPerMeter = vehicleType.getCostInformation().getCostsPerMeter();
						fixedCosts = fixedCosts + vehicleType.getCostInformation().getFixedCosts();

						varCostsTime = varCostsTime + vehicleId2TourDuration.get(vehicleId) * costsPerSecond;
						varCostsDist = varCostsDist + vehicleId2TourLength.get(vehicleId) * costsPerMeter;
					}
				}

				double totalVehCosts = fixedCosts + varCostsTime + varCostsDist;
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

	/*package-private*/ void writeTravelTimeAndDistancePerVehicle(String analysisOutputDirectory, Scenario scenario) {
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


				final VehicleType vehicleType = vehicleId2VehicleType.get(vehicleId);
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

	/*package-private*/ void writeTravelTimeAndDistancePerVehicleType(String analysisOutputDirectory, Scenario scenario) {
		log.info("Writing out Time & Distance & Costs ... perVehicleType");

		//----- All VehicleTypes in CarrierVehicleTypes container. Used so that even unused vehTypes appear in the output
		TreeMap<Id<VehicleType>, VehicleType> vehicleTypesMap = new TreeMap<>(CarriersUtils.getOrAddCarrierVehicleTypes(scenario).getVehicleTypes());
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
