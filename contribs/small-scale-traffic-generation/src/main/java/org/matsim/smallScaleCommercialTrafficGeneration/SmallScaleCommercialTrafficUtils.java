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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.carriers.ScheduledTour;
import org.matsim.freight.carriers.Tour;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
			double distance = NetworkUtils.getEuclideanDistance(centroidPointOfBuildingPolygon,	possibleLink.getCoord());
			if (distance < minDistance) {
				newLink = possibleLink.getId();
				minDistance = distance;
			}
		}
		if (newLink == null && numberOfPossibleLinks > 0) {
			for (Link possibleLink : linksPerZone.get(zone).values()) {
				double distance = NetworkUtils.getEuclideanDistance(centroidPointOfBuildingPolygon,	possibleLink.getCoord());
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
	static void createPlansBasedOnCarrierPlans(Scenario scenario, GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType smallScaleCommercialTrafficType, Path output,
											   String modelName, String sampleName, String nameOutputPopulation, int numberOfPlanVariantsPerAgent) {

		Population population = scenario.getPopulation();
		PopulationFactory popFactory = population.getFactory();

		Map<String, AtomicLong> idCounter = new HashMap<>();


		for (Carrier carrier : CarriersUtils.addOrGetCarriers(scenario).getCarriers().values()) {
			if (carrier.getSelectedPlan() == null){
				log.warn("Carrier {} has no selected plan. Therefore, no population plans are created for this carrier.", carrier.getId());
				continue;
			}
			for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()) {

				Plan plan = popFactory.createPlan();

				String subpopulation = carrier.getAttributes().getAttribute("subpopulation").toString();
				String mode = tour.getVehicle().getType().getNetworkMode();

				Tour.Start start = tour.getTour().getStart();
				Activity startActivity = PopulationUtils.createActivityFromCoord("commercial_start",
					scenario.getNetwork().getLinks().get(start.getLocation()).getFromNode().getCoord());
				startActivity.setLinkId(start.getLocation());
				startActivity.setEndTime(tour.getDeparture());

				plan.addActivity(startActivity);
				List<Tour.TourElement> tourElements = tour.getTour().getTourElements();
				for (Tour.TourElement tourElement : tourElements) {
					if (tourElement instanceof Tour.Leg tourLeg) {
						createAndAddLegBasedOnCarrierLeg(plan, mode, tourLeg);
						continue;
					}
					if (tourElement instanceof Tour.TourActivity activity) {
						Activity newActivity = PopulationUtils.createActivityFromCoord(activity.getActivityType(),
							scenario.getNetwork().getLinks().get(activity.getLocation()).getFromNode().getCoord());
						newActivity.setMaximumDuration(activity.getDuration());
						newActivity.setLinkId(activity.getLocation());
						plan.addActivity(newActivity);
					}
				}
				Tour.End end = tour.getTour().getEnd();
				Activity endActivity = PopulationUtils.createActivityFromCoord("commercial_end",
					scenario.getNetwork().getLinks().get(end.getLocation()).getFromNode().getCoord());
				endActivity.setLinkId(end.getLocation());
				plan.addActivity(endActivity);
				String key = String.format("%s_%s_%s", subpopulation, carrier.getAttributes().getAttribute("tourStartArea"),
					carrier.getAttributes().getAttribute("purpose"));

				long id = idCounter.computeIfAbsent(key, (k) -> new AtomicLong()).getAndIncrement();

				Person newPerson = popFactory.createPerson(Id.createPersonId(key + "_" + id));

				newPerson.addPlan(plan);
				PopulationUtils.putSubpopulation(newPerson, subpopulation);
				newPerson.getAttributes().putAttribute("purpose",
					carrier.getAttributes().getAttribute("purpose"));
				if (carrier.getAttributes().getAsMap().containsKey("tourStartArea"))
					newPerson.getAttributes().putAttribute("tourStartArea",
						carrier.getAttributes().getAttribute("tourStartArea"));
				if (carrier.getAttributes().getAsMap().containsKey("startCategory"))
					newPerson.getAttributes().putAttribute("startCategory",
						carrier.getAttributes().getAttribute("startCategory").toString());
				newPerson.getAttributes().putAttribute("carrierId", carrier.getId().toString());
				newPerson.getAttributes().putAttribute("tourId", tour.getTour().getId().toString());

				VehicleUtils.insertVehicleTypesIntoPersonAttributes(newPerson, Map.of(mode, tour.getVehicle().getType().getId()));

				population.addPerson(newPerson);
			}
		}

		String outputPopulationFile;
		if (nameOutputPopulation == null)
			if (smallScaleCommercialTrafficType.equals(GenerateSmallScaleCommercialTrafficDemand.SmallScaleCommercialTrafficType.completeSmallScaleCommercialTraffic))
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
		ProjectionUtils.putCRS(population, scenario.getConfig().global().getCoordinateSystem());
		PopulationUtils.writePopulation(population, outputPopulationFile);
		log.info("Population with {} persons created including the plans in {}.", population.getPersons().size(), outputPopulationFile);
	}

	private static void createAndAddLegBasedOnCarrierLeg(Plan plan, String mode, Tour.Leg carrierLeg) {
		Leg leg = PopulationUtils.createAndAddLeg(plan, mode);
		leg.setDepartureTime(carrierLeg.getExpectedDepartureTime());
		leg.setTravelTime(carrierLeg.getExpectedTransportTime());

		Route route = carrierLeg.getRoute();
		if (route != null) {
			Route routeCopy = route.clone();
			routeCopy.setTravelTime(carrierLeg.getExpectedTransportTime());
			leg.setRoute(routeCopy);
		}
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
	 * Creates the stable folder suffix used for carrier part outputs.
	 *
	 * @param partIndex zero-based part index
	 * @param partCount total number of parts
	 * @return suffix in the form {@code part-001-of-015}
	 */
	static String getCarrierPartSuffix(int partIndex, int partCount) {
		return "part-" + String.format("%03d", partIndex + 1) + "-of-" + String.format("%03d", partCount);
	}

	/**
	 * Adds the controller run id prefix to output file names when a run id is configured.
	 *
	 * @param config config that may contain a controller run id
	 * @param fileName base file name without run id prefix
	 * @return {@code <runId>.<fileName>} or {@code fileName} if no run id is configured
	 */
	static String getRunIdPrefixedFileName(Config config, String fileName) {
		String runId = config.controller().getRunId();
		if (runId == null || runId.isBlank()) {
			return fileName;
		}
		return runId + "." + fileName;
	}

	/**
	 * Keeps only the deterministic subset of carriers assigned to one carrier part.
	 * <p>
	 * Carrier ids are sorted lexicographically and then distributed by {@code sortedIndex % partCount}. This keeps the
	 * split reproducible across runs and makes the merge lossless because every carrier id is assigned to exactly one
	 * part.
	 *
	 * @param scenario scenario whose carrier collection should be reduced
	 * @param partIndex zero-based part index to keep
	 * @param partCount total number of parts
	 */
	static void filterCarriersForPart(Scenario scenario, int partIndex, int partCount) {
		Carriers carriers = CarriersUtils.addOrGetCarriers(scenario);
		List<Id<Carrier>> sortedCarrierIds = carriers.getCarriers().keySet().stream()
			.sorted(Comparator.comparing(Id::toString))
			.toList();
		Set<Id<Carrier>> selectedCarrierIds = IntStream.range(0, sortedCarrierIds.size())
			.filter(carrierIndex -> carrierIndex % partCount == partIndex)
			.mapToObj(sortedCarrierIds::get)
			.collect(Collectors.toSet());
		carriers.getCarriers().keySet().removeIf(carrierId -> !selectedCarrierIds.contains(carrierId));
		log.info("Selected small scale commercial carrier part {}/{} with {} carriers.", partIndex + 1, partCount, carriers.getCarriers().size());
	}

	/**
	 * Loads a scenario with one specific carrier file and its matching carrier vehicle type file.
	 * <p>
	 * The freight config is updated with absolute paths before loading. This avoids path resolution surprises when
	 * carrier part folders or merge folders are outside the config file directory.
	 *
	 * @param baseConfig config that should be used as loading context
	 * @param carrierFile carrier file to load
	 * @param carrierVehicleTypesFileName base name of the carrier vehicle type file located next to the carrier file
	 * @return scenario with carriers loaded according to the freight config
	 */
	static Scenario loadScenarioWithCarrierFile(Config baseConfig, Path carrierFile, String carrierVehicleTypesFileName) {
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(baseConfig, FreightCarriersConfigGroup.class);
		freightCarriersConfigGroup.setCarriersFile(carrierFile.toAbsolutePath().toString());
		Path carrierVehicleTypesFile = resolveCarrierVehicleTypesFile(baseConfig, carrierFile, carrierVehicleTypesFileName);
		if (carrierVehicleTypesFile != null) {
			freightCarriersConfigGroup.setCarriersVehicleTypesFile(carrierVehicleTypesFile.toAbsolutePath().toString());
		} else if (baseConfig.vehicles() != null && freightCarriersConfigGroup.getCarriersVehicleTypesFile() == null) {
			freightCarriersConfigGroup.setCarriersVehicleTypesFile(baseConfig.vehicles().getVehiclesFile());
		}
		Scenario scenario = ScenarioUtils.loadScenario(baseConfig);
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
		return scenario;
	}

	/**
	 * Loads only carriers and carrier vehicle types into a minimal scenario.
	 * <p>
	 * This avoids loading the network when callers only need to read carrier files, for example when merging carrier
	 * part outputs.
	 *
	 * @param baseConfig config that should be used as path resolution context
	 * @param carrierFile carrier file to load
	 * @param carrierVehicleTypesFileName base name of the carrier vehicle type file located next to the carrier file
	 * @return scenario with carriers loaded according to the freight config, but without loading other scenario inputs
	 */
	static Scenario loadScenarioWithCarrierFileOnly(Config baseConfig, Path carrierFile, String carrierVehicleTypesFileName) {
		Config carrierConfig = ConfigUtils.createConfig();
		carrierConfig.setContext(baseConfig.getContext());
		FreightCarriersConfigGroup carrierFreightConfigGroup = ConfigUtils.addOrGetModule(carrierConfig, FreightCarriersConfigGroup.class);
		carrierFreightConfigGroup.setCarriersFile(carrierFile.toAbsolutePath().toString());

		Path carrierVehicleTypesFile = resolveCarrierVehicleTypesFile(baseConfig, carrierFile, carrierVehicleTypesFileName);
		if (carrierVehicleTypesFile != null) {
			carrierFreightConfigGroup.setCarriersVehicleTypesFile(carrierVehicleTypesFile.toAbsolutePath().toString());
		} else {
			FreightCarriersConfigGroup baseFreightConfigGroup = ConfigUtils.addOrGetModule(baseConfig, FreightCarriersConfigGroup.class);
			if (baseFreightConfigGroup.getCarriersVehicleTypesFile() != null) {
				carrierFreightConfigGroup.setCarriersVehicleTypesFile(baseFreightConfigGroup.getCarriersVehicleTypesFile());
			} else if (baseConfig.vehicles() != null && baseConfig.vehicles().getVehiclesFile() != null) {
				carrierFreightConfigGroup.setCarriersVehicleTypesFile(baseConfig.vehicles().getVehiclesFile());
			}
		}

		Scenario scenario = ScenarioUtils.createScenario(carrierConfig);
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
		return scenario;
	}

	/**
	 * Resolves the run-id-prefixed carrier vehicle type file located next to a carrier file.
	 *
	 * @param config config whose run id is used for the expected file name
	 * @param carrierFile carrier file whose parent folder should contain the vehicle type file
	 * @param carrierVehicleTypesFileName base name of the carrier vehicle type file
	 * @return sibling vehicle type file if it exists, otherwise {@code null}
	 */
	static Path resolveCarrierVehicleTypesFile(Config config, Path carrierFile, String carrierVehicleTypesFileName) {
		if (carrierFile == null || carrierFile.getParent() == null) {
			return null;
		}
		Path siblingCarrierVehicleTypesFile = carrierFile.getParent().resolve(getRunIdPrefixedFileName(config, carrierVehicleTypesFileName));
		if (Files.exists(siblingCarrierVehicleTypesFile)) {
			return siblingCarrierVehicleTypesFile;
		}
		return null;
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
	static Map<String, Object2DoubleMap<StructuralAttribute>> readDataDistribution(Path pathToDataDistributionToZones) throws IOException {
		if (!Files.exists(pathToDataDistributionToZones)) {
			log.error("Required data per zone file {} not found", pathToDataDistributionToZones);
		}

		Map<String, Object2DoubleMap<StructuralAttribute>> resultingDataPerZone = new HashMap<>();
		try (BufferedReader reader = IOUtils.getBufferedReader(pathToDataDistributionToZones.toString())) {
			CSVParser parse = CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter('\t').setHeader()
				.setSkipHeaderRecord(true).get().parse(reader);

			for (CSVRecord record : parse) {
				String zoneID = record.get("zoneID");
				resultingDataPerZone.put(zoneID, new Object2DoubleOpenHashMap<>());
				for (int n = 2; n < parse.getHeaderMap().size(); n++) {
					Optional<StructuralAttribute> category = StructuralAttribute.fromLabel(parse.getHeaderNames().get(n));
					if (category.isEmpty()) {
						log.warn("The category '{}' in the data distribution file is not known. Please check the input file and the defined categories.", parse.getHeaderNames().get(n));
						continue;
					}
					resultingDataPerZone.get(zoneID).mergeDouble(category.get(),
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

	public enum StructuralAttribute {
		INHABITANTS("Inhabitants"),
		EMPLOYEE("Employee"),
		EMPLOYEE_PRIMARY("Employee Primary Sector"),
		EMPLOYEE_CONSTRUCTION("Employee Construction"),
		EMPLOYEE_SECONDARY("Employee Secondary Sector Rest"),
		EMPLOYEE_RETAIL("Employee Retail"),
		EMPLOYEE_TRAFFIC("Employee Traffic/Parcels"),
		EMPLOYEE_TERTIARY("Employee Tertiary Sector Rest");

		private final String label;

		StructuralAttribute(String label) { this.label = label; }
		public String getLabel() { return label; }

		private static final Map<String, StructuralAttribute> BY_LABEL =
			Arrays.stream(values()).collect(Collectors.toMap(StructuralAttribute::getLabel, e -> e));

		public static Optional<StructuralAttribute> fromLabel(String label) {
			return Optional.ofNullable(BY_LABEL.get(label));
		}
	}
}
