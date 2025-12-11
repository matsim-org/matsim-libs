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
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Time;
import org.matsim.freight.carriers.*;

/**
 * Some basic analysis / data collection for {@link Carriers}(files)
 * <p></p>
 * For all carriers it writes out the:
 * - score of the selected plan
 * - number of tours (= vehicles) of the selected plan
 * - number of Services _input
 * - number of Services _handled
 * - number of shipments _input
 * - number of shipments _handled
 * - number of not handled jobs
 * - number of planned demand size
 * - number of handled demand size
 * to a tsv-file.
 *
 * @author Kai Martins-Turner (kturner), Ricardo Ewert
 */
public class CarrierPlanAnalysis {

	private static final Logger log = LogManager.getLogger(CarrierPlanAnalysis.class);
	public final String delimiter;

	final Carriers carriers;

	public CarrierPlanAnalysis(String delimiter, Carriers carriers) {
		this.delimiter = delimiter;
		this.carriers = carriers;
	}

	public void runAnalysisAndWriteStats(String analysisOutputDirectory, CarriersAnalysis.CarrierAnalysisType analysisType) {
		log.info("Writing out carrier analysis ...");

		Path path = Path.of(analysisOutputDirectory);
		String fileName = switch (analysisType) {
			case carriersPlans_unPlanned -> path.resolve("Carriers_stats_unPlanned.tsv").toString();
			case carriersPlans, carriersAndEvents -> path.resolve("Carriers_stats.tsv").toString();
		};
		try (BufferedWriter bw1 = new BufferedWriter(new FileWriter(fileName))) {
			String headerGeneral = String.join(delimiter,
				"carrierId",
				"nuOfJspritIterations",
				"fleetSize",
				"nuOfPossibleVehicleTypes",
				"nuOfPossibleVehicles",
				"nuOfServiceLocations_input",
				"nuOfPickupLocations_input",
				"nuOfDeliveryLocations_input");
			String header = switch (analysisType) {
				case carriersPlans_unPlanned -> String.join(delimiter,
					headerGeneral,
					"nuOfShipments_input",
					"nuOfServices_input",
					"nuOfPlanedDemandSize"
				);
				case carriersPlans, carriersAndEvents -> String.join(delimiter,
					headerGeneral,
					"MATSimScoreSelectedPlan",
					"jSpritScoreSelectedPlan",
					"nuOfTours",
					"nuOfShipments_input",
					"nuOfShipments_handled",
					"nuOfServices_input",
					"nuOfServices_handled",
					"noOfNotHandledJobs",
					"nuOfPlanedDemandSize",
					"nuOfHandledDemandSize",
					"jspritComputationTime"
				);
			};

			//Write headline:
			bw1.write(header);
			bw1.newLine();

			final TreeMap<Id<Carrier>, Carrier> sortedCarrierMap = new TreeMap<>(carriers.getCarriers());

			for (Carrier carrier : sortedCarrierMap.values()) {

				int numberOfPossibleVehicles = carrier.getCarrierCapabilities().getCarrierVehicles().size();
				int numberOfPossibleVehicleTypes = carrier.getCarrierCapabilities().getCarrierVehicles().values().stream().map(
					CarrierVehicle::getVehicleTypeId).distinct().mapToInt(vt -> 1).sum();
				String fleetSize = carrier.getCarrierCapabilities().getFleetSize().toString();
				int numberOfPlanedShipments = carrier.getShipments().size();
				int numberOfPlanedServices = carrier.getServices().size();
				int numberJspritIterations = CarriersUtils.getJspritIterations(carrier);

				int numberOfDifferentServiceLocations_demand = (int) carrier.getServices().values().stream().map(
					CarrierService::getServiceLinkId).distinct().count();
				int numberOfDifferentPickupLocations_demand = (int) carrier.getShipments().values().stream().map(
					CarrierShipment::getPickupLinkId).distinct().count();
				int numberOfDifferentDeliveryLocations_demand = (int) carrier.getShipments().values().stream().map(
					CarrierShipment::getDeliveryLinkId).distinct().count();

				int numberOfPlanedDemandSize;
				if (numberOfPlanedShipments > 0) {
					numberOfPlanedDemandSize = carrier.getShipments().values().stream().mapToInt(CarrierShipment::getCapacityDemand).sum();
				} else {
					numberOfPlanedDemandSize = carrier.getServices().values().stream().mapToInt(CarrierService::getCapacityDemand).sum();
				}

				bw1.write(carrier.getId().toString());
				bw1.write(delimiter + numberJspritIterations);
				bw1.write(delimiter + fleetSize);
				bw1.write(delimiter + numberOfPossibleVehicleTypes);
				bw1.write(delimiter + numberOfPossibleVehicles);
				bw1.write(delimiter + numberOfDifferentServiceLocations_demand);
				bw1.write(delimiter + numberOfDifferentPickupLocations_demand);
				bw1.write(delimiter + numberOfDifferentDeliveryLocations_demand);
				switch (analysisType) {
					case carriersPlans_unPlanned -> {
						bw1.write(delimiter + numberOfPlanedShipments);
						bw1.write(delimiter + numberOfPlanedServices);
						bw1.write(delimiter + numberOfPlanedDemandSize);
					}
					case carriersAndEvents -> {
						int numberOfHandledPickups = (int) carrier.getSelectedPlan().getScheduledTours().stream().mapToDouble(
							t -> t.getTour().getTourElements().stream().filter(te -> te instanceof Tour.Pickup).count()).sum();
						int numberOfHandledDeliveries = (int) carrier.getSelectedPlan().getScheduledTours().stream().mapToDouble(
							t -> t.getTour().getTourElements().stream().filter(te -> te instanceof Tour.Delivery).count()).sum();
						int nuOfServiceHandled = (int) carrier.getSelectedPlan().getScheduledTours().stream().mapToDouble(
							t -> t.getTour().getTourElements().stream().filter(te -> te instanceof Tour.ServiceActivity).count()).sum();
						int notHandledJobs;
						int numberOfHandledDemandSize;

						if (numberOfPlanedShipments > 0) {
							numberOfPlanedDemandSize = carrier.getShipments().values().stream().mapToInt(CarrierShipment::getCapacityDemand).sum();
							numberOfHandledDemandSize = carrier.getSelectedPlan().getScheduledTours().stream().mapToInt(
								t -> t.getTour().getTourElements().stream().filter(te -> te instanceof Tour.Pickup).mapToInt(
									te -> ((Tour.Pickup) te).getShipment().getCapacityDemand()).sum()).sum();
							notHandledJobs = numberOfPlanedShipments - numberOfHandledPickups;
						} else {
							numberOfPlanedDemandSize = carrier.getServices().values().stream().mapToInt(CarrierService::getCapacityDemand).sum();
							numberOfHandledDemandSize = carrier.getSelectedPlan().getScheduledTours().stream().mapToInt(
								t -> t.getTour().getTourElements().stream().filter(te -> te instanceof Tour.ServiceActivity).mapToInt(
									te -> ((Tour.ServiceActivity) te).getService().getCapacityDemand()).sum()).sum();
							notHandledJobs = numberOfPlanedServices - nuOfServiceHandled;
						}
						CarriersUtils.allJobsHandledBySelectedPlan(carrier);

						if (numberOfHandledDeliveries != numberOfHandledPickups) {
							log.warn(
								"Number of handled pickups and deliveries are not equal for carrier {}. Pickups: {}, Deliveries: {}. This should not happen!!",
								carrier.getId(), numberOfHandledPickups, numberOfHandledDeliveries);
						}
						bw1.write(delimiter + carrier.getSelectedPlan().getScore());
						bw1.write(delimiter + carrier.getSelectedPlan().getJspritScore());
						bw1.write(delimiter + carrier.getSelectedPlan().getScheduledTours().size());
						bw1.write(delimiter + numberOfPlanedShipments);
						bw1.write(delimiter + numberOfHandledPickups);
						bw1.write(delimiter + numberOfPlanedServices);
						bw1.write(delimiter + nuOfServiceHandled);
						bw1.write(delimiter + notHandledJobs);
						bw1.write(delimiter + numberOfPlanedDemandSize);
						bw1.write(delimiter + numberOfHandledDemandSize);
						if (CarriersUtils.getJspritComputationTime(carrier) != Integer.MIN_VALUE)
							bw1.write(delimiter + Time.writeTime(CarriersUtils.getJspritComputationTime(carrier), Time.TIMEFORMAT_HHMMSS));
						else
							bw1.write(delimiter + "null");
					}
				}
				bw1.newLine();
			}

			bw1.close();
			log.info("Output written to {}", fileName);
		} catch (IOException e) {
			log.error("Could not write carrier stats to file", e);
		}
	}
}
