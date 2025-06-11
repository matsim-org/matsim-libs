/* *********************************************************************** *
 * project: org.matsim.*
 * Controller.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.smallScaleCommercialTrafficGeneration;

import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.solution.SolutionCostCalculator;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.BreakActivity;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.options.ShpOptions;
import org.matsim.application.options.ShpOptions.Index;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utils for the SmallScaleFreightTraffic
 *
 * @author Ricardo Ewert
 */
public class SmallScaleCommercialTrafficUtils {

	private static final Logger log = LogManager.getLogger(SmallScaleCommercialTrafficUtils.class);

	/**
	 * Creates and return the Index of the zone shape.
	 *
	 * @param shapeFileZonePath       Path to the shape file of the zones
	 * @param shapeCRS                CRS of the shape file
	 * @param shapeFileZoneNameColumn Column name of the zone in the shape file
	 * @return indexZones
	 */
	public static Index getIndexZones(Path shapeFileZonePath, String shapeCRS, String shapeFileZoneNameColumn) {

		ShpOptions shpZones = new ShpOptions(shapeFileZonePath, shapeCRS, StandardCharsets.UTF_8);
		if (shpZones.readFeatures().getFirst().getAttribute(shapeFileZoneNameColumn) == null)
			throw new NullPointerException("The column '" + shapeFileZoneNameColumn + "' does not exist in the zones shape file. Please check the input.");
		return shpZones.createIndex(shapeCRS, shapeFileZoneNameColumn);
	}

	/**
	 * Creates and return the Index of the landuse shape.
	 *
	 * @param shapeFileLandusePath       	Path to the shape file of the landuse
     * @param shapeCRS 				 		CRS of the shape file
     * @param shapeFileLanduseTypeColumn 	Column name of the landuse in the shape file
     * @return indexLanduse
	 */
	 public static Index getIndexLanduse(Path shapeFileLandusePath, String shapeCRS, String shapeFileLanduseTypeColumn) {
		ShpOptions shpLanduse = new ShpOptions(shapeFileLandusePath, shapeCRS, StandardCharsets.UTF_8);
		if (shpLanduse.readFeatures().getFirst().getAttribute(shapeFileLanduseTypeColumn) == null)
			throw new NullPointerException("The column '" + shapeFileLanduseTypeColumn + "' does not exist in the landuse shape file. Please check the input.");
		return shpLanduse.createIndex(shapeCRS, shapeFileLanduseTypeColumn);
	}

	/**
	 * Creates and return the Index of the building shape.
	 *
	 * @param shapeFileBuildingsPath      	Path to the shape file of the buildings
     * @param shapeCRS 				 		CRS of the shape file
     * @param shapeFileBuildingTypeColumn 	Column name of the building in the shape file
     * @return indexBuildings
	 */
	public static Index getIndexBuildings(Path shapeFileBuildingsPath, String shapeCRS, String shapeFileBuildingTypeColumn) {
		ShpOptions shpBuildings = new ShpOptions(shapeFileBuildingsPath, shapeCRS, StandardCharsets.UTF_8);
		if (shpBuildings.readFeatures().getFirst().getAttribute(shapeFileBuildingTypeColumn) == null)
			throw new NullPointerException("The column '" + shapeFileBuildingTypeColumn + "' does not exist in the building shape file. Please check the input.");

		return shpBuildings.createIndex(shapeCRS, shapeFileBuildingTypeColumn);
	}

	/**
	 * Creates and return the Index of the regions shapes.
	 *
	 * @param shapeFileRegionsPath     Path to the shape file of the regions
	 * @param shapeCRS                 CRS of the shape file
	 * @param regionsShapeRegionColumn Column name of the region in the shape file
	 * @return indexRegions
	 */
	public static Index getIndexRegions(Path shapeFileRegionsPath, String shapeCRS, String regionsShapeRegionColumn) {
		ShpOptions shpRegions = new ShpOptions(shapeFileRegionsPath, shapeCRS, StandardCharsets.UTF_8);
		if (shpRegions.readFeatures().getFirst().getAttribute(regionsShapeRegionColumn) == null)
			throw new NullPointerException("The column '" + regionsShapeRegionColumn + "' does not exist in the region shape file. Please check the input.");
		return shpRegions.createIndex(shapeCRS, regionsShapeRegionColumn);
	}

	/** Finds the nearest possible link for the building polygon.
	 * @param zone  							zone of the building
	 * @param noPossibleLinks 					list of links that are not possible
	 * @param linksPerZone 						map of links per zone
	 * @param newLink 							new link
	 * @param centroidPointOfBuildingPolygon 	centroid point of the building polygon
	 * @param numberOfPossibleLinks 			number of possible links
	 * @return 									new possible Link
	 */
	static Id<Link> findNearestPossibleLink(String zone, List<String> noPossibleLinks, Map<String, Map<Id<Link>, Link>> linksPerZone,
											Id<Link> newLink, Coord centroidPointOfBuildingPolygon, int numberOfPossibleLinks) {
		double minDistance = Double.MAX_VALUE;
		searchLink:
		for (Link possibleLink : linksPerZone.get(zone).values()) {
			if (possibleLink.getToNode().getOutLinks() == null)
				continue;
			if (noPossibleLinks != null && numberOfPossibleLinks > noPossibleLinks.size())
				for (String depotLink : noPossibleLinks) {
					if (depotLink.equals(possibleLink.getId().toString())
						|| (NetworkUtils.findLinkInOppositeDirection(possibleLink) != null && depotLink.equals(
						NetworkUtils.findLinkInOppositeDirection(possibleLink).getId().toString())))
						continue searchLink;
				}
			double distance = NetworkUtils.getEuclideanDistance(centroidPointOfBuildingPolygon,
				(Coord) possibleLink.getAttributes().getAttribute("newCoord"));
			if (distance < minDistance) {
				newLink = possibleLink.getId();
				minDistance = distance;
			}
		}
		if (newLink == null && numberOfPossibleLinks > 0) {
			for (Link possibleLink : linksPerZone.get(zone).values()) {
				double distance = NetworkUtils.getEuclideanDistance(centroidPointOfBuildingPolygon,
					(Coord) possibleLink.getAttributes().getAttribute("newCoord"));
				if (distance < minDistance) {
					newLink = possibleLink.getId();
					minDistance = distance;
				}
			}
		}

		return newLink;
	}

	/**
	 * Creates a population including the plans in preparation for the MATSim run. If a different name of the population is set, different plan variants per person are created
	 */
	static void createPlansBasedOnCarrierPlans(Scenario scenario, String smallScaleCommercialTrafficType, Path output,
											   String modelName, String sampleName, String nameOutputPopulation, int numberOfPlanVariantsPerAgent) {

		Population population = scenario.getPopulation();
		PopulationFactory popFactory = population.getFactory();

		Map<String, AtomicLong> idCounter = new HashMap<>();

		Population populationFromCarrier = (Population) scenario.getScenarioElement("allpersons");
		Vehicles allVehicles = VehicleUtils.getOrCreateAllvehicles(scenario);

		for (Person person : populationFromCarrier.getPersons().values()) {

			Plan plan = popFactory.createPlan();
			String carrierName = person.getId().toString().split("freight_")[1].split("_veh_")[0];
			Carrier relatedCarrier = CarriersUtils.addOrGetCarriers(scenario).getCarriers()
				.get(Id.create(carrierName, Carrier.class));
			String subpopulation = relatedCarrier.getAttributes().getAttribute("subpopulation").toString();
			Id<Vehicle> vehicleId = Id.createVehicleId(person.getId().toString());
			String mode = allVehicles.getVehicles().get(vehicleId).getType().getNetworkMode();

			List<PlanElement> tourElements = person.getSelectedPlan().getPlanElements();
			for (PlanElement tourElement : tourElements) {

				if (tourElement instanceof Activity activity) {
					Activity newActivity = PopulationUtils.createActivityFromCoord(activity.getType(),
						scenario.getNetwork().getLinks().get(activity.getLinkId()).getFromNode().getCoord());
					if (activity.getMaximumDuration() != OptionalTime.undefined())
						newActivity.setMaximumDuration(activity.getMaximumDuration().seconds());
					if (activity.getType().equals("start")) {
						newActivity.setEndTime(activity.getEndTime().seconds());
						newActivity.setType("commercial_start");
					}
					if (activity.getType().equals("end"))
						newActivity.setType("commercial_end");
					plan.addActivity(newActivity);
				}
				if (tourElement instanceof Leg) {
					PopulationUtils.createAndAddLeg(plan, mode);
				}
			}

			String key = String.format("%s_%s_%s", subpopulation, relatedCarrier.getAttributes().getAttribute("tourStartArea"),
				relatedCarrier.getAttributes().getAttribute("purpose"));

			long id = idCounter.computeIfAbsent(key, (k) -> new AtomicLong()).getAndIncrement();

			Person newPerson = popFactory.createPerson(Id.createPersonId(key + "_" + id));

			newPerson.addPlan(plan);
			PopulationUtils.putSubpopulation(newPerson, subpopulation);
			newPerson.getAttributes().putAttribute("purpose",
				relatedCarrier.getAttributes().getAttribute("purpose"));
			if (relatedCarrier.getAttributes().getAsMap().containsKey("tourStartArea"))
				newPerson.getAttributes().putAttribute("tourStartArea",
					relatedCarrier.getAttributes().getAttribute("tourStartArea"));

			VehicleUtils.insertVehicleIdsIntoPersonAttributes(newPerson, Map.of(mode, vehicleId));
			VehicleUtils.insertVehicleTypesIntoPersonAttributes(newPerson, Map.of(mode, allVehicles.getVehicles().get(vehicleId).getType().getId()));

			population.addPerson(newPerson);
		}

		String outputPopulationFile;
		if (nameOutputPopulation == null)
			if (smallScaleCommercialTrafficType.equals("completeSmallScaleCommercialTraffic"))
				outputPopulationFile = output.toString() + "/" + modelName + "_" + "smallScaleCommercialTraffic" + "_" + sampleName + "pct_plans.xml.gz";
			else
				outputPopulationFile = output.toString() + "/" + modelName + "_" + smallScaleCommercialTrafficType + "_" + sampleName + "pct_plans.xml.gz";
		else
			outputPopulationFile = output.toString() + "/" + nameOutputPopulation;
		if (numberOfPlanVariantsPerAgent > 1)
//				CreateDifferentPlansForFreightPopulation.createMorePlansWithDifferentStartTimes(population, numberOfPlanVariantsPerAgent, 6*3600, 14*3600, 8*3600);
			CreateDifferentPlansForFreightPopulation.createMorePlansWithDifferentActivityOrder(population, numberOfPlanVariantsPerAgent);
		else if (numberOfPlanVariantsPerAgent < 1)
			log.warn(
				"You selected {} of different plan variants per agent. This is invalid. Please check the input parameter. The default is 1 and is now set for the output.",
				numberOfPlanVariantsPerAgent);

		PopulationUtils.writePopulation(population, outputPopulationFile);
		scenario.getPopulation().getPersons().clear();
	}

	static String getSampleNameOfOutputFolder(double sample) {
		String sampleName;
		if ((sample * 100) % 1 == 0)
			sampleName = String.valueOf((int) (sample * 100));
		else
			sampleName = String.valueOf((sample * 100));
		return sampleName;
	}

	/**
	 * Find the zone where the link is located
	 */
	static String findZoneOfLink(Id<Link> linkId, Map<String, Map<Id<Link>, Link>> linksPerZone) {
		for (String area : linksPerZone.keySet()) {
			if (linksPerZone.get(area).containsKey(linkId))
				return area;
		}
		return null;
	}


	/** Reads the data distribution of the zones.
	 * @param pathToDataDistributionToZones Path to the data distribution of the zones
	 * @return 								resultingDataPerZone
	 * @throws IOException 					if the file is not found
	 */
	static Map<String, Object2DoubleMap<String>> readDataDistribution(Path pathToDataDistributionToZones) throws IOException {
		if (!Files.exists(pathToDataDistributionToZones)) {
			log.error("Required data per zone file {} not found", pathToDataDistributionToZones);
		}

		Map<String, Object2DoubleMap<String>> resultingDataPerZone = new HashMap<>();
		try (BufferedReader reader = IOUtils.getBufferedReader(pathToDataDistributionToZones.toString())) {
			CSVParser parse = CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter('\t').setHeader()
				.setSkipHeaderRecord(true).build().parse(reader);

			for (CSVRecord record : parse) {
				String zoneID = record.get("zoneID");
				resultingDataPerZone.put(zoneID, new Object2DoubleOpenHashMap<>());
				for (int n = 2; n < parse.getHeaderMap().size(); n++) {
					resultingDataPerZone.get(zoneID).mergeDouble(parse.getHeaderNames().get(n),
						Double.parseDouble(record.get(n)), Double::sum);
				}
			}
		}
		log.info("Data distribution for {} zones was read from {}", resultingDataPerZone.size(), pathToDataDistributionToZones);
		return resultingDataPerZone;

	}

	/**
	 * Creates a cost calculator.
	 */
	static SolutionCostCalculator getObjectiveFunction(final VehicleRoutingProblem vrp, final double maxCosts) {

		return new SolutionCostCalculator() {
			@Override
			public double getCosts(VehicleRoutingProblemSolution solution) {
				double costs = 0.;

				for (VehicleRoute route : solution.getRoutes()) {
					costs += route.getVehicle().getType().getVehicleCostParams().fix;
					boolean hasBreak = false;
					TourActivity prevAct = route.getStart();
					for (TourActivity act : route.getActivities()) {
						if (act instanceof BreakActivity)
							hasBreak = true;
						costs += vrp.getTransportCosts().getTransportCost(prevAct.getLocation(), act.getLocation(),
							prevAct.getEndTime(), route.getDriver(), route.getVehicle());
						costs += vrp.getActivityCosts().getActivityCost(act, act.getArrTime(), route.getDriver(),
							route.getVehicle());
						prevAct = act;
					}
					costs += vrp.getTransportCosts().getTransportCost(prevAct.getLocation(),
						route.getEnd().getLocation(), prevAct.getEndTime(), route.getDriver(), route.getVehicle());
					if (route.getVehicle().getBreak() != null) {
						if (!hasBreak) {
							// break defined and required but not assigned penalty
							if (route.getEnd().getArrTime() > route.getVehicle().getBreak().getTimeWindow().getEnd()) {
								costs += 4 * (maxCosts * 2 + route.getVehicle().getBreak().getServiceDuration()
									* route.getVehicle().getType().getVehicleCostParams().perServiceTimeUnit);
							}
						}
					}
				}
				for (Job j : solution.getUnassignedJobs()) {
					costs += maxCosts * 2 * (11 - j.getPriority());
				}
				return costs;
			}
		};
	}
}
