/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

import com.google.common.base.Joiner;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.solution.SolutionCostCalculator;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.BreakActivity;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
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
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.carriers.Tour.Pickup;
import org.matsim.freight.carriers.Tour.ServiceActivity;
import org.matsim.freight.carriers.Tour.TourElement;
import org.matsim.freight.carriers.jsprit.MatsimJspritFactory;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utils for the SmallScaleFreightTraffic
 *
 * @author Ricardo Ewert
 */
public class SmallScaleCommercialTrafficUtils {

	private static final Logger log = LogManager.getLogger(SmallScaleCommercialTrafficUtils.class);
	private static final Joiner JOIN = Joiner.on("\t");

	/**
	 * Creates and return the Index of the zone shape.
	 *
	 * @param shapeFileZonePath       Path to the shape file of the zones
	 * @param shapeCRS                CRS of the shape file
	 * @param shapeFileZoneNameColumn Column name of the zone in the shape file
	 * @return indexZones
	 */
	static Index getIndexZones(Path shapeFileZonePath, String shapeCRS, String shapeFileZoneNameColumn) {

		ShpOptions shpZones = new ShpOptions(shapeFileZonePath, shapeCRS, StandardCharsets.UTF_8);
		if (shpZones.readFeatures().iterator().next().getAttribute(shapeFileZoneNameColumn) == null)
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
	 static Index getIndexLanduse(Path shapeFileLandusePath, String shapeCRS, String shapeFileLanduseTypeColumn) {
		ShpOptions shpLanduse = new ShpOptions(shapeFileLandusePath, shapeCRS, StandardCharsets.UTF_8);
		if (shpLanduse.readFeatures().iterator().next().getAttribute(shapeFileLanduseTypeColumn) == null)
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
	static Index getIndexBuildings(Path shapeFileBuildingsPath, String shapeCRS, String shapeFileBuildingTypeColumn) {
		ShpOptions shpBuildings = new ShpOptions(shapeFileBuildingsPath, shapeCRS, StandardCharsets.UTF_8);
		if (shpBuildings.readFeatures().iterator().next().getAttribute(shapeFileBuildingTypeColumn) == null)
			throw new NullPointerException("The column '" + shapeFileBuildingTypeColumn + "' does not exist in the building shape file. Please check the input.");

		return shpBuildings.createIndex(shapeCRS, shapeFileBuildingTypeColumn);
	}

	/**
	 * Creates and return the Index of the regions shape.
	 *
	 * @param shapeFileRegionsPath     Path to the shape file of the regions
	 * @param shapeCRS                 CRS of the shape file
	 * @param regionsShapeRegionColumn Column name of the region in the shape file
	 * @return indexRegions
	 */
	public static Index getIndexRegions(Path shapeFileRegionsPath, String shapeCRS, String regionsShapeRegionColumn) {
		ShpOptions shpRegions = new ShpOptions(shapeFileRegionsPath, shapeCRS, StandardCharsets.UTF_8);
		if (shpRegions.readFeatures().iterator().next().getAttribute(regionsShapeRegionColumn) == null)
			throw new NullPointerException("The column '" + regionsShapeRegionColumn + "' does not exist in the region shape file. Please check the input.");
		return shpRegions.createIndex(shapeCRS, regionsShapeRegionColumn);
	}

	/**
	 * Writes a csv file with the result of the distribution per zone of the input data.
	 */
	static void writeResultOfDataDistribution(Map<String, Object2DoubleMap<String>> resultingDataPerZone,
											  Path outputFileInOutputFolder, Map<String, String> zoneIdRegionConnection)
		throws IOException {

		writeCSVWithCategoryHeader(resultingDataPerZone, outputFileInOutputFolder, zoneIdRegionConnection);
		log.info("The data distribution is finished and written to: " + outputFileInOutputFolder);
	}

	/** Finds the nearest possible link for the building polygon.
	 * @param zone
	 * @param noPossibleLinks
	 * @param linksPerZone
	 * @param newLink
	 * @param centroidPointOfBuildingPolygon
	 * @param numberOfPossibleLinks
	 * @return
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
	 * Writer of data distribution data.
	 */
	private static void writeCSVWithCategoryHeader(Map<String, Object2DoubleMap<String>> resultingDataPerZone,
												   Path outputFileInInputFolder,
												   Map<String, String> zoneIdRegionConnection) throws MalformedURLException {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFileInInputFolder.toUri().toURL(),
			StandardCharsets.UTF_8, true);
		try {
			String[] header = new String[]{"zoneID", "region", "Inhabitants", "Employee", "Employee Primary Sector",
				"Employee Construction", "Employee Secondary Sector Rest", "Employee Retail",
				"Employee Traffic/Parcels", "Employee Tertiary Sector Rest"};
			JOIN.appendTo(writer, header);
			writer.write("\n");
			for (String zone : resultingDataPerZone.keySet()) {
				List<String> row = new ArrayList<>();
				row.add(zone);
				row.add(zoneIdRegionConnection.get(zone));
				for (String category : header) {
					if (!category.equals("zoneID") && !category.equals("region"))
						row.add(String.valueOf((int) Math.round(resultingDataPerZone.get(zone).getDouble(category))));
				}
				JOIN.appendTo(writer, row);
				writer.write("\n");
			}

			writer.close();

		} catch (IOException e) {
			log.error("Could not write the csv file with the data distribution data.", e);
		}
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
			final String mode;
			if (subpopulation.contains("commercialPersonTraffic"))
				mode = "car";
			else if (subpopulation.contains("goodsTraffic"))
				mode = "freight";
			else
				mode = relatedCarrier.getAttributes().getAttribute("networkMode").toString();
			List<PlanElement> tourElements = person.getSelectedPlan().getPlanElements();
			double tourStartTime = 0;
			for (PlanElement tourElement : tourElements) {

				if (tourElement instanceof Activity activity) {
					activity.setCoord(
						scenario.getNetwork().getLinks().get(activity.getLinkId()).getFromNode().getCoord());
					if (activity.getType().equals("start")) {
						tourStartTime = activity.getEndTime().seconds();
						activity.setType("commercial_start");
					} else
						activity.setEndTimeUndefined();
					if (activity.getType().equals("end")) {
						activity.setStartTime(tourStartTime + 8 * 3600);
						activity.setType("commercial_end");
					}
					plan.addActivity(activity);
				}
				if (tourElement instanceof Leg) {
					Leg legActivity = popFactory.createLeg(mode);
					plan.addLeg(legActivity);
				}
			}

			String key = String.format("%s_%s_%s", subpopulation, relatedCarrier.getAttributes().getAttribute("tourStartArea"),
				relatedCarrier.getAttributes().getAttribute("purpose")); //TODO

			long id = idCounter.computeIfAbsent(key, (k) -> new AtomicLong()).getAndIncrement();

			Person newPerson = popFactory.createPerson(Id.createPersonId(key + "_" + id));

			newPerson.addPlan(plan);
			PopulationUtils.putSubpopulation(newPerson, subpopulation);
			newPerson.getAttributes().putAttribute("purpose",
				relatedCarrier.getAttributes().getAttribute("purpose"));
			if (relatedCarrier.getAttributes().getAsMap().containsKey("tourStartArea"))
				newPerson.getAttributes().putAttribute("tourStartArea",
					relatedCarrier.getAttributes().getAttribute("tourStartArea"));

			Id<Vehicle> vehicleId = Id.createVehicleId(person.getId().toString());

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
				"You selected " + numberOfPlanVariantsPerAgent + " of different plan variants per agent. This is invalid. Please check the input parameter. The default is 1 and is now set for the output.");

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
	 * Reads existing scenarios and add them to the scenario. If the scenario is
	 * part of the goodsTraffic or commercialPersonTraffic the demand of the existing
	 * scenario reduces the demand of the small scale commercial traffic. The
	 * dispersedTraffic will be added additionally.
	 */
	static void readExistingModels(Scenario scenario, double sampleScenario,
								   Map<String, Map<Id<Link>, Link>> linksPerZone) throws Exception {

		Path existingModelsFolder = Path.of(scenario.getConfig().getContext().toURI()).getParent().resolve("existingModels");
		String locationOfExistingModels = existingModelsFolder.resolve("existingModels.csv").toString();
		CSVParser parse = CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter('\t').setHeader()
			.setSkipHeaderRecord(true).build().parse(IOUtils.getBufferedReader(locationOfExistingModels));
		for (CSVRecord record : parse) {
			String modelName = record.get("model");
			double sampleSizeExistingScenario = Double.parseDouble(record.get("sampleSize"));
			String modelTrafficType = record.get("smallScaleCommercialTrafficType");
			final Integer modelPurpose;
			if (!Objects.equals(record.get("purpose"), ""))
				modelPurpose = Integer.parseInt(record.get("purpose"));
			else
				modelPurpose = null;
			final String vehicleType;
			if (!Objects.equals(record.get("vehicleType"), ""))
				vehicleType = record.get("vehicleType");
			else
				vehicleType = null;
			final String modelMode = record.get("networkMode");

			Path scenarioLocation = existingModelsFolder.resolve(modelName);
			if (!Files.exists(scenarioLocation.resolve("output_carriers.xml.gz")))
				throw new Exception("For the existing model " + modelName
					+ " no carrierFile exists. The carrierFile should have the name 'output_carriers.xml.gz'");
			if (!Files.exists(scenarioLocation.resolve("vehicleTypes.xml.gz")))
				throw new Exception("For the existing model " + modelName
					+ " no vehicleTypesFile exists. The vehicleTypesFile should have the name 'vehicleTypes.xml.gz'");

			log.info("Integrating existing scenario: " + modelName);

			CarrierVehicleTypes readVehicleTypes = new CarrierVehicleTypes();
			CarrierVehicleTypes usedVehicleTypes = new CarrierVehicleTypes();
			new CarrierVehicleTypeReader(readVehicleTypes)
				.readFile(scenarioLocation.resolve("vehicleTypes.xml.gz").toString());

			Carriers carriers = new Carriers();
			new CarrierPlanXmlReader(carriers, readVehicleTypes)
				.readFile(scenarioLocation.resolve("output_carriers.xml.gz").toString());

			if (sampleSizeExistingScenario < sampleScenario)
				throw new Exception("The sample size of the existing scenario " + modelName
					+ "is smaller than the sample size of the scenario. No upscaling for existing scenarios implemented.");

			double sampleFactor = sampleScenario / sampleSizeExistingScenario;

			int numberOfToursExistingScenario = 0;
			for (Carrier carrier : carriers.getCarriers().values()) {
				if (!carrier.getPlans().isEmpty())
					numberOfToursExistingScenario = numberOfToursExistingScenario
						+ carrier.getSelectedPlan().getScheduledTours().size();
			}
			int sampledNumberOfToursExistingScenario = (int) Math.round(numberOfToursExistingScenario * sampleFactor);
			List<Carrier> carrierToRemove = new ArrayList<>();
			int remainedTours = 0;
			double roundingError = 0.;

			log.info("The existing scenario " + modelName + " is a " + (int) (sampleSizeExistingScenario * 100)
				+ "% scenario and has " + numberOfToursExistingScenario + " tours");
			log.info("The existing scenario " + modelName + " will be sampled down to the scenario sample size of "
				+ (int) (sampleScenario * 100) + "% which results in " + sampledNumberOfToursExistingScenario
				+ " tours.");

			int numberOfAnalyzedTours = 0;
			for (Carrier carrier : carriers.getCarriers().values()) {
				if (!carrier.getPlans().isEmpty()) {
					int numberOfOriginalTours = carrier.getSelectedPlan().getScheduledTours().size();
					numberOfAnalyzedTours += numberOfOriginalTours;
					int numberOfRemainingTours = (int) Math.round(numberOfOriginalTours * sampleFactor);
					roundingError = roundingError + numberOfRemainingTours - (numberOfOriginalTours * sampleFactor);
					int numberOfToursToRemove = numberOfOriginalTours - numberOfRemainingTours;
					List<ScheduledTour> toursToRemove = new ArrayList<>();

					if (roundingError <= -1 && numberOfToursToRemove > 0) {
						numberOfToursToRemove = numberOfToursToRemove - 1;
						numberOfRemainingTours = numberOfRemainingTours + 1;
						roundingError = roundingError + 1;
					}
					if (roundingError >= 1 && numberOfRemainingTours != numberOfToursToRemove) {
						numberOfToursToRemove = numberOfToursToRemove + 1;
						numberOfRemainingTours = numberOfRemainingTours - 1;
						roundingError = roundingError - 1;
					}
					remainedTours = remainedTours + numberOfRemainingTours;
					if (remainedTours > sampledNumberOfToursExistingScenario) {
						remainedTours = remainedTours - 1;
						numberOfRemainingTours = numberOfRemainingTours - 1;
						numberOfToursToRemove = numberOfToursToRemove + 1;
					}
					// last carrier with scheduled tours
					if (numberOfAnalyzedTours == numberOfToursExistingScenario
						&& remainedTours != sampledNumberOfToursExistingScenario) {
						numberOfRemainingTours = sampledNumberOfToursExistingScenario - remainedTours;
						numberOfToursToRemove = numberOfOriginalTours - numberOfRemainingTours;
						remainedTours = remainedTours + numberOfRemainingTours;
					}
					// remove carrier because no tours remaining
					if (numberOfOriginalTours == numberOfToursToRemove) {
						carrierToRemove.add(carrier);
						continue;
					}

					while (toursToRemove.size() < numberOfToursToRemove) {
						Object[] tours = carrier.getSelectedPlan().getScheduledTours().toArray();
						ScheduledTour tour = (ScheduledTour) tours[MatsimRandom.getRandom().nextInt(tours.length)];
						toursToRemove.add(tour);
						carrier.getSelectedPlan().getScheduledTours().remove(tour);
					}

					// remove services/shipments from removed tours
					if (!carrier.getServices().isEmpty()) {
						for (ScheduledTour removedTour : toursToRemove) {
							for (TourElement tourElement : removedTour.getTour().getTourElements()) {
								if (tourElement instanceof ServiceActivity service) {
									carrier.getServices().remove(service.getService().getId());
								}
							}
						}
					} else if (!carrier.getShipments().isEmpty()) {
						for (ScheduledTour removedTour : toursToRemove) {
							for (TourElement tourElement : removedTour.getTour().getTourElements()) {
								if (tourElement instanceof Pickup pickup) {
									carrier.getShipments().remove(pickup.getShipment().getId());
								}
							}
						}
					}
					// remove vehicles of removed tours and check if all vehicleTypes are still
					// needed
					if (carrier.getCarrierCapabilities().getFleetSize().equals(FleetSize.FINITE)) {
						for (ScheduledTour removedTour : toursToRemove) {
							carrier.getCarrierCapabilities().getCarrierVehicles()
								.remove(removedTour.getVehicle().getId());
						}
					} else if (carrier.getCarrierCapabilities().getFleetSize().equals(FleetSize.INFINITE)) {
						carrier.getCarrierCapabilities().getCarrierVehicles().clear();
						for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()) {
							carrier.getCarrierCapabilities().getCarrierVehicles().put(tour.getVehicle().getId(),
								tour.getVehicle());
						}
					}
					List<VehicleType> vehicleTypesToRemove = new ArrayList<>();
					for (VehicleType existingVehicleType : carrier.getCarrierCapabilities().getVehicleTypes()) {
						boolean vehicleTypeNeeded = false;
						for (CarrierVehicle vehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
							if (vehicle.getType().equals(existingVehicleType)) {
								vehicleTypeNeeded = true;
								usedVehicleTypes.getVehicleTypes().put(existingVehicleType.getId(),
									existingVehicleType);
							}
						}
						if (!vehicleTypeNeeded)
							vehicleTypesToRemove.add(existingVehicleType);
					}
					carrier.getCarrierCapabilities().getVehicleTypes().removeAll(vehicleTypesToRemove);
				}
				// carriers without solutions
				else {
					if (!carrier.getServices().isEmpty()) {
						int numberOfServicesToRemove = carrier.getServices().size()
							- (int) Math.round(carrier.getServices().size() * sampleFactor);
						for (int i = 0; i < numberOfServicesToRemove; i++) {
							Object[] services = carrier.getServices().keySet().toArray();
							carrier.getServices().remove(services[MatsimRandom.getRandom().nextInt(services.length)]);
						}
					}
					if (!carrier.getShipments().isEmpty()) {
						int numberOfShipmentsToRemove = carrier.getShipments().size()
							- (int) Math.round(carrier.getShipments().size() * sampleFactor);
						for (int i = 0; i < numberOfShipmentsToRemove; i++) {
							Object[] shipments = carrier.getShipments().keySet().toArray();
							carrier.getShipments().remove(shipments[MatsimRandom.getRandom().nextInt(shipments.length)]);
						}
					}
				}
			}
			carrierToRemove.forEach(carrier -> carriers.getCarriers().remove(carrier.getId()));
			CarriersUtils.getCarrierVehicleTypes(scenario).getVehicleTypes().putAll(usedVehicleTypes.getVehicleTypes());

			carriers.getCarriers().values().forEach(carrier -> {
				Carrier newCarrier = CarriersUtils
					.createCarrier(Id.create(modelName + "_" + carrier.getId().toString(), Carrier.class));
				newCarrier.getAttributes().putAttribute("subpopulation", modelTrafficType);
				if (modelPurpose != null)
					newCarrier.getAttributes().putAttribute("purpose", modelPurpose);
				newCarrier.getAttributes().putAttribute("existingModel", modelName);
				newCarrier.getAttributes().putAttribute("networkMode", modelMode);
				if (vehicleType != null)
					newCarrier.getAttributes().putAttribute("vehicleType", vehicleType);
				newCarrier.setCarrierCapabilities(carrier.getCarrierCapabilities());

				if (!carrier.getServices().isEmpty())
					newCarrier.getServices().putAll(carrier.getServices());
				else if (!carrier.getShipments().isEmpty())
					newCarrier.getShipments().putAll(carrier.getShipments());
				if (carrier.getSelectedPlan() != null) {
					newCarrier.setSelectedPlan(carrier.getSelectedPlan());

					List<String> startAreas = new ArrayList<>();
					for (ScheduledTour tour : newCarrier.getSelectedPlan().getScheduledTours()) {
						String tourStartZone = findZoneOfLink(tour.getTour().getStartLinkId(), linksPerZone);
						if (!startAreas.contains(tourStartZone))
							startAreas.add(tourStartZone);
					}
					newCarrier.getAttributes().putAttribute("tourStartArea",
						String.join(";", startAreas));

					CarriersUtils.setJspritIterations(newCarrier, 0);
					// recalculate score for selectedPlan
					VehicleRoutingProblem vrp = MatsimJspritFactory
						.createRoutingProblemBuilder(carrier, scenario.getNetwork()).build();
					VehicleRoutingProblemSolution solution = MatsimJspritFactory
						.createSolution(newCarrier.getSelectedPlan(), vrp);
					SolutionCostCalculator solutionCostsCalculator = getObjectiveFunction(vrp, Double.MAX_VALUE);
					double costs = solutionCostsCalculator.getCosts(solution) * (-1);
					carrier.getSelectedPlan().setScore(costs);
				} else {
					CarriersUtils.setJspritIterations(newCarrier, CarriersUtils.getJspritIterations(carrier));
					newCarrier.getCarrierCapabilities().setFleetSize(carrier.getCarrierCapabilities().getFleetSize());
				}
				CarriersUtils.addOrGetCarriers(scenario).getCarriers().put(newCarrier.getId(), newCarrier);
			});
		}
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

	/**
	 * Creates a cost calculator.
	 */
	private static SolutionCostCalculator getObjectiveFunction(final VehicleRoutingProblem vrp, final double maxCosts) {

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
