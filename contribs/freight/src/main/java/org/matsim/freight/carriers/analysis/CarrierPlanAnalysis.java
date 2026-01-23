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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Locale;
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
 * - number of Services _planned
 * - number of Services _handled
 * - number of shipments _planned
 * - number of shipments _handled
 * - number of not handled jobs
 * - number of planned demand size
 * - number of handled demand size
 * to a tsv-file.
 *
 * @author Kai Martins-Turner (kturner), Ricardo Ewert
 */
/*package-private*/ class CarrierPlanAnalysis {

	private static final Logger log = LogManager.getLogger(CarrierPlanAnalysis.class);
	private final String delimiter;
	private enum JobsType {
		shipments, services, none
	}
	final Carriers carriers;

	/*package-private*/ CarrierPlanAnalysis(String delimiter, Carriers carriers) {
		this.delimiter = delimiter;
		this.carriers = carriers;
	}

	/*package-private*/ void runAnalysisAndWriteStats(String analysisOutputDirectory, CarriersAnalysis.CarrierAnalysisType analysisType) {
		log.info("Writing out carrier analysis ...");

		Path path = Path.of(analysisOutputDirectory);
		String fileName = switch (analysisType) {
			case carriersPlans_unPlanned -> path.resolve("Carriers_stats_unPlanned.tsv").toString();
			case carriersPlans, carriersAndEvents -> path.resolve("Carriers_stats.tsv").toString();
		};
		switch (analysisType) {
			case carriersPlans, carriersAndEvents -> createKPIOutput(path);
		}
		try (BufferedWriter bw1 = new BufferedWriter(new FileWriter(fileName))) {
			String headerGeneral = String.join(delimiter,
				"carrierId",
				"nuOfJspritIterations",
				"fleetSize",
				"nuOfPossibleVehicleTypes",
				"nuOfPossibleVehicles",
				"nuOfServiceLocations_planned",
				"nuOfPickupLocations_planned",
				"nuOfDeliveryLocations_planned");
			String header = switch (analysisType) {
				case carriersPlans_unPlanned -> String.join(delimiter,
					headerGeneral,
					"jobType",
					"nuOfJobs_planned",
					"demandSize_planned"
				);
				case carriersPlans, carriersAndEvents -> String.join(delimiter,
					headerGeneral,
					"MATSimScoreSelectedPlan",
					"jspritScoreSelectedPlan",
					"nuOfTours",
					"jobType",
					"nuOfJobs_planned",
					"nuOfJobs_handled",
					"noOfJobs_notHandled",
					"demandSize_planned",
					"demandSize_handled",
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
				int numberOfPlannedJobs = carrier.getShipments().size() + carrier.getServices().size();
				JobsType jobsType;
				int numberJspritIterations = CarriersUtils.getJspritIterations(carrier);

				int numberOfDifferentServiceLocations_demand = (int) carrier.getServices().values().stream().map(
					CarrierService::getServiceLinkId).distinct().count();
				int numberOfDifferentPickupLocations_demand = (int) carrier.getShipments().values().stream().map(
					CarrierShipment::getPickupLinkId).distinct().count();
				int numberOfDifferentDeliveryLocations_demand = (int) carrier.getShipments().values().stream().map(
					CarrierShipment::getDeliveryLinkId).distinct().count();

				int numberOfPlannedDemandSize;
				if (!carrier.getShipments().isEmpty()) {
					numberOfPlannedDemandSize = carrier.getShipments().values().stream().mapToInt(CarrierShipment::getCapacityDemand).sum();
					jobsType = JobsType.shipments;
				} else if (!carrier.getServices().isEmpty()) {
					numberOfPlannedDemandSize = carrier.getServices().values().stream().mapToInt(CarrierService::getCapacityDemand).sum();
					jobsType = JobsType.services;
				}
				else {
					numberOfPlannedDemandSize = 0;
					jobsType = JobsType.none;
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
						bw1.write(delimiter + jobsType);
						bw1.write(delimiter + numberOfPlannedJobs);
						bw1.write(delimiter + numberOfPlannedDemandSize);
					}
					case carriersPlans, carriersAndEvents -> {
						int numberOfHandledPickups = 0, nuOfServiceHandled = 0, numberOfHandledDemandSize = 0;
						int notHandledJobs = numberOfPlannedJobs;
						if (carrier.getSelectedPlan() != null) {
							numberOfHandledPickups = (int) carrier.getSelectedPlan().getScheduledTours().stream().mapToDouble(
								t -> t.getTour().getTourElements().stream().filter(te -> te instanceof Tour.Pickup).count()).sum();
							nuOfServiceHandled = (int) carrier.getSelectedPlan().getScheduledTours().stream().mapToDouble(
								t -> t.getTour().getTourElements().stream().filter(te -> te instanceof Tour.ServiceActivity).count()).sum();
							if (jobsType == JobsType.shipments) {
								numberOfHandledDemandSize = carrier.getSelectedPlan().getScheduledTours().stream().mapToInt(
									t -> t.getTour().getTourElements().stream().filter(te -> te instanceof Tour.Pickup).mapToInt(
										te -> ((Tour.Pickup) te).getShipment().getCapacityDemand()).sum()).sum();
								notHandledJobs -= numberOfHandledPickups;
							} else {
								numberOfHandledDemandSize = carrier.getSelectedPlan().getScheduledTours().stream().mapToInt(
									t -> t.getTour().getTourElements().stream().filter(te -> te instanceof Tour.ServiceActivity).mapToInt(
										te -> ((Tour.ServiceActivity) te).getService().getCapacityDemand()).sum()).sum();
								notHandledJobs -= nuOfServiceHandled;
							}
						}
						int numberOfHandledJobs = numberOfHandledPickups + nuOfServiceHandled;

						CarriersUtils.allJobsHandledBySelectedPlan(carrier);

						CarrierPlan plan = carrier.getSelectedPlan();

						Double score = null;
						Double jspritScore = null;
						int tours = 0;

						if (plan instanceof CarrierPlan p) {
							score = (p.getScore() instanceof Double s) ? s : null;
							jspritScore = (p.getJspritScore() instanceof Double js) ? js : null;
							tours = (p.getScheduledTours() != null) ? p.getScheduledTours().size() : 0;
						}

						bw1.write(delimiter + score);
						bw1.write(delimiter + jspritScore);
						bw1.write(delimiter + tours);
						bw1.write(delimiter + jobsType);
						bw1.write(delimiter + numberOfPlannedJobs);
						bw1.write(delimiter + numberOfHandledJobs);
						bw1.write(delimiter + notHandledJobs);
						bw1.write(delimiter + numberOfPlannedDemandSize);
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

	private void createKPIOutput(Path path) {

		try (BufferedWriter bw1 = new BufferedWriter(new FileWriter(path.resolve("Carriers_KPIs.tsv").toString()))) {


			bw1.write("Number of Carrier" + delimiter + carriers.getCarriers().size());
			bw1.newLine();
			int numberOfVehicles = carriers.getCarriers().values().stream()
				.mapToInt(c -> c.getSelectedPlan().getScheduledTours().size()).sum();
			bw1.write("Number of Vehicles in Solution" + delimiter + numberOfVehicles);
			bw1.newLine();
			int jspritComputationTime = 0;
			bw1.write("Jsprit Computation Time" + delimiter + jspritComputationTime);
			bw1.newLine();
			double jspritScore = carriers.getCarriers().values().stream()
				.mapToDouble(c -> {
					CarrierPlan plan = c.getSelectedPlan();
					if (plan != null && plan.getJspritScore() != null) {
						return plan.getJspritScore();
					} else {
						return 0.0;
					}
				}).sum();
			NumberFormat nf = NumberFormat.getIntegerInstance(Locale.US);
			nf.setGroupingUsed(true);
			bw1.write("Total Jsprit Score of all Carriers" + delimiter + nf.format(new BigDecimal(jspritScore).setScale(0, RoundingMode.HALF_UP)));
			bw1.newLine();

			int numberNotHandledJobs = carriers.getCarriers().values().stream()
				.mapToInt(c -> {
					int plannedJobs = c.getShipments().size() + c.getServices().size();
					int handledJobs = 0;
					CarrierPlan plan = c.getSelectedPlan();
					if (plan != null) {
						handledJobs = (int) plan.getScheduledTours().stream().mapToDouble(
							t -> t.getTour().getTourElements().stream().filter(te -> te instanceof Tour.Pickup || te instanceof Tour.ServiceActivity).count()).sum();
					}
					return plannedJobs - handledJobs;
				}).sum();
			bw1.write("Total Number of not handled Jobs" + delimiter + numberNotHandledJobs);
			bw1.newLine();
			bw1.close();
			log.info("KPI Output written to {}", path.resolve("Carriers_KPIs.tsv").toString());


		} catch (IOException e) {
			log.error("Could not write carrier stats to file", e);
		}
	}
}
