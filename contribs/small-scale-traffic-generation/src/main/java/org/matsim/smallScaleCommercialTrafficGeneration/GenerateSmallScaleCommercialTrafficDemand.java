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

import com.google.inject.Inject;
import com.google.inject.Provider;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.geotools.api.feature.simple.SimpleFeature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions.Index;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.UnmaterializedConfigGroupChecker;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.facilities.ActivityFacility;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.carriers.controler.*;
import org.matsim.freight.carriers.usecases.chessboard.CarrierTravelDisutilities;
import org.matsim.smallScaleCommercialTrafficGeneration.data.DefaultTourSpecificationsByUsingKID2002;
import org.matsim.smallScaleCommercialTrafficGeneration.data.GetCommercialTourSpecifications;
import org.matsim.vehicles.CostInformation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles;
import static org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.readDataDistribution;

/**
 * Tool to generate small scale commercial traffic for a selected area. The needed input data are: employee information for the area and three shapes files (zones, buildings, landuse). These data should be available with OSM.
 *
 * @author Ricardo Ewert
 */
//TODO: use EnumeratedDistribution for distributions with probabilities
@CommandLine.Command(name = "generate-small-scale-commercial-traffic", description = "Generates plans for a small scale commercial traffic model", showDefaultValues = true)
public class GenerateSmallScaleCommercialTrafficDemand implements MATSimAppCommand {
	// freight traffic from extern:

	// Option 1: take "as is" from Chengqi code.

	// Option 2: differentiate FTL and LTL by Gütergruppe.  FTL as in option 1.  LTL per Gütergruppe _ein_ Ziel in Zone, = "Hub".  Verteilverkehr
	// von dort.  Startseite genauso.

	// Option 3: Leerkamp (nur in RVR Modell).

	private static final Logger log = LogManager.getLogger(GenerateSmallScaleCommercialTrafficDemand.class);
	private final IntegrateExistingTrafficToSmallScaleCommercial integrateExistingTrafficToSmallScaleCommercial;
	private final GetCommercialTourSpecifications getCommercialTourSpecifications;

	private enum CreationOption {
		useExistingCarrierFileWithSolution, createNewCarrierFile, useExistingCarrierFileWithoutSolution
	}

	public enum SmallScaleCommercialTrafficType {
		commercialPersonTraffic, goodsTraffic, completeSmallScaleCommercialTraffic
	}

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to the config for small scale commercial generation")
	private Path configPath;

	@CommandLine.Option(names = "--pathToDataDistributionToZones", description = "Path to the data distribution to zones")
	private Path pathToDataDistributionToZones;

	@CommandLine.Option(names = "--pathToCommercialFacilities", description = "Path to the commercial facilities.")
	private Path pathToCommercialFacilities;

	@CommandLine.Option(names = "--carrierFilePath", description = "Path to the carrier file.")
	private Path carrierFilePath;

	@CommandLine.Option(names = "--sample", description = "Scaling factor of the small scale commercial traffic (0, 1)", required = true)
	private double sample;

	@CommandLine.Option(names = "--jspritIterations", description = "Set number of jsprit iterations", required = true)
	private int jspritIterations;

	@CommandLine.Option(names = "--creationOption", description = "Set option of mode differentiation:  useExistingCarrierFileWithSolution, createNewCarrierFile, useExistingCarrierFileWithoutSolution")
	private CreationOption usedCreationOption;

	@CommandLine.Option(names = "--smallScaleCommercialTrafficType", description = "Select traffic type. Options: commercialPersonTraffic, goodsTraffic, completeSmallScaleCommercialTraffic (contains both types)")
	private SmallScaleCommercialTrafficType usedSmallScaleCommercialTrafficType;

	@CommandLine.Option(names = "--includeExistingModels", description = "If models for some segments exist they can be included.")
	private boolean includeExistingModels;

	@CommandLine.Option(names = "--zoneShapeFileName", description = "Path of the zone shape file.")
	private Path shapeFileZonePath;

	@CommandLine.Option(names = "--zoneShapeFileNameColumn", description = "Name of the unique column of the name/Id of each zone in the zones shape file.")
	private String shapeFileZoneNameColumn;

	@CommandLine.Option(names = "--shapeCRS", description = "CRS of the three input shape files (zones, landuse, buildings")
	private String shapeCRS;

	@CommandLine.Option(names = "--resistanceFactor", defaultValue = "0.005", description = "ResistanceFactor for the trip distribution")
	private double resistanceFactor;

	@CommandLine.Option(names = "--nameOutputPopulation", description = "Name of the output Population")
	private String nameOutputPopulation;

	@CommandLine.Option(names = "--numberOfPlanVariantsPerAgent", description = "If an agent should have variant plans, you should set this parameter.", defaultValue = "1")
	private int numberOfPlanVariantsPerAgent;

	@CommandLine.Option(names = "--network", description = "Overwrite network file in config")
	private String network;

	@CommandLine.Option(names = "--pathOutput", description = "Path for the output")
	private Path output;

	private Random rnd;
	private RandomGenerator rng;
	private final Map<String, Map<String, List<ActivityFacility>>> facilitiesPerZone = new HashMap<>();

	private Index indexZones;

	public GenerateSmallScaleCommercialTrafficDemand() {
		this.integrateExistingTrafficToSmallScaleCommercial = new DefaultIntegrateExistingTrafficToSmallScaleCommercialImpl();
		log.info("Using default {} if existing models are integrated!", DefaultIntegrateExistingTrafficToSmallScaleCommercialImpl.class.getSimpleName());
		this.getCommercialTourSpecifications = new DefaultTourSpecificationsByUsingKID2002();
		log.info("Using default {} for tour specifications!", DefaultTourSpecificationsByUsingKID2002.class.getSimpleName());
	}
	public GenerateSmallScaleCommercialTrafficDemand(IntegrateExistingTrafficToSmallScaleCommercial integrateExistingTrafficToSmallScaleCommercial, GetCommercialTourSpecifications getCommercialTourSpecifications) {
		if (integrateExistingTrafficToSmallScaleCommercial == null){
			this.integrateExistingTrafficToSmallScaleCommercial = new DefaultIntegrateExistingTrafficToSmallScaleCommercialImpl();
			log.info("Using default {} if existing models are integrated!", DefaultIntegrateExistingTrafficToSmallScaleCommercialImpl.class.getSimpleName());
		} else {
			this.integrateExistingTrafficToSmallScaleCommercial = integrateExistingTrafficToSmallScaleCommercial;
			log.info("Using {} if existing models are integrated!", integrateExistingTrafficToSmallScaleCommercial.getClass().getSimpleName());
		}
		if (getCommercialTourSpecifications == null){
			this.getCommercialTourSpecifications = new DefaultTourSpecificationsByUsingKID2002();
			log.info("Using default {} for tour specifications!", DefaultTourSpecificationsByUsingKID2002.class.getSimpleName());
		} else {
			this.getCommercialTourSpecifications = getCommercialTourSpecifications;
			log.info("Using {} for tour specifications!", getCommercialTourSpecifications.getClass().getSimpleName());
		}
	}

	public static void main(String[] args) {
		System.exit(new CommandLine(new GenerateSmallScaleCommercialTrafficDemand()).execute(args));
	}

	@Override
	public Integer call() throws Exception {
		Configurator.setLevel("org.matsim.core.utils.geometry.geotools.MGC", Level.ERROR);

		String modelName = configPath.getParent().getFileName().toString();

		String sampleName = SmallScaleCommercialTrafficUtils.getSampleNameOfOutputFolder(sample);

		Config config = readAndCheckConfig(configPath, modelName, sampleName, output);

		output = Path.of(config.controller().getOutputDirectory());

		Scenario scenario = ScenarioUtils.loadScenario(config);
		NetworkUtils.runNetworkCleaner(scenario.getNetwork()); // e.g. for vulkaneifel network

		FreightCarriersConfigGroup freightCarriersConfigGroup;
		switch (usedCreationOption) {
			case useExistingCarrierFileWithSolution, useExistingCarrierFileWithoutSolution -> {
				log.info("Existing carriers (including carrier vehicle types) should be set in the freight config group");
				if (includeExistingModels)
					throw new Exception(
						"You set that existing models should included to the new model. This is only possible for a creation of the new carrier file and not by using an existing.");
				freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
				if (freightCarriersConfigGroup.getCarriersFile() == null)
					freightCarriersConfigGroup.setCarriersFile(carrierFilePath.toString());
				if (config.vehicles() != null && freightCarriersConfigGroup.getCarriersVehicleTypesFile() == null)
					freightCarriersConfigGroup.setCarriersVehicleTypesFile(config.vehicles().getVehiclesFile());
				log.info("Load carriers from: {}", freightCarriersConfigGroup.getCarriersFile());
				CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

				// Remove vehicle types which are not used by the carriers
				Map<Id<VehicleType>, VehicleType> readVehicleTypes = CarriersUtils.getCarrierVehicleTypes(scenario).getVehicleTypes();
				List<Id<VehicleType>> usedCarrierVehicleTypes = CarriersUtils.getCarriers(scenario).getCarriers().values().stream()
					.flatMap(carrier -> carrier.getCarrierCapabilities().getCarrierVehicles().values().stream())
					.map(vehicle -> vehicle.getType().getId())
					.distinct()
					.toList();

				readVehicleTypes.keySet().removeIf(vehicleType -> !usedCarrierVehicleTypes.contains(vehicleType));

				if (Objects.requireNonNull(usedCreationOption) == CreationOption.useExistingCarrierFileWithoutSolution) {
					solveSeparatedVRPs(scenario, null);
				}
			}
			default -> {
				if (!Files.exists(shapeFileZonePath)) {
					throw new Exception("Required districts shape file {} not found" + shapeFileZonePath.toString());
				}
				indexZones = SmallScaleCommercialTrafficUtils.getIndexZones(shapeFileZonePath, shapeCRS, shapeFileZoneNameColumn);

				Map<String, Object2DoubleMap<String>> resultingDataPerZone = readDataDistribution(pathToDataDistributionToZones);
				filterFacilitiesForZones(scenario, facilitiesPerZone);
				Map<String, Map<Id<Link>, Link>> linksPerZone = filterLinksForZones(scenario, indexZones, facilitiesPerZone, shapeFileZoneNameColumn);

				switch (usedSmallScaleCommercialTrafficType) {
					case commercialPersonTraffic, goodsTraffic ->
						createCarriersAndDemand(output, scenario, resultingDataPerZone, linksPerZone,
							usedSmallScaleCommercialTrafficType.toString(),
							includeExistingModels);
					case completeSmallScaleCommercialTraffic -> {
						createCarriersAndDemand(output, scenario, resultingDataPerZone, linksPerZone, "commercialPersonTraffic",
							includeExistingModels);
						includeExistingModels = false; // because already included in the step before
						createCarriersAndDemand(output, scenario, resultingDataPerZone, linksPerZone, "goodsTraffic",
							includeExistingModels);
					}
					default -> throw new RuntimeException("No traffic type selected.");
				}
				if (config.controller().getRunId() == null)
					new CarrierPlanWriter(CarriersUtils.addOrGetCarriers(scenario))
						.write(scenario.getConfig().controller().getOutputDirectory() + "/output_CarrierDemand.xml");
				else
					new CarrierPlanWriter(CarriersUtils.addOrGetCarriers(scenario))
						.write(scenario.getConfig().controller().getOutputDirectory() + "/"
							+ scenario.getConfig().controller().getRunId() + ".output_CarrierDemand.xml");
				solveSeparatedVRPs(scenario, linksPerZone);
			}
		}
		if (config.controller().getRunId() == null)
			new CarrierPlanWriter(CarriersUtils.addOrGetCarriers(scenario)).write(
				scenario.getConfig().controller().getOutputDirectory() + "/output_CarrierDemandWithPlans.xml");
		else
			new CarrierPlanWriter(CarriersUtils.addOrGetCarriers(scenario))
				.write(
					scenario.getConfig().controller().getOutputDirectory() + "/" + scenario.getConfig().controller().getRunId() + ".output_CarrierDemandWithPlans.xml");

		Controler controler = prepareControler(scenario);

		// Creating inject always adds check for unmaterialized config groups.
		controler.getInjector();

		// Removes check after injector has been created
		controler.getConfig().removeConfigConsistencyChecker(UnmaterializedConfigGroupChecker.class);

		controler.run();

		SmallScaleCommercialTrafficUtils.createPlansBasedOnCarrierPlans(controler.getScenario(),
			usedSmallScaleCommercialTrafficType.toString(), output, modelName, sampleName, nameOutputPopulation, numberOfPlanVariantsPerAgent);

		return 0;
	}

	/** Creates a map with the different facility types per building.
	 * @param scenario 				complete Scenario
	 * @param facilitiesPerZone 	Map with facilities per zone
	 */
	private void filterFacilitiesForZones(Scenario scenario, Map<String, Map<String, List<ActivityFacility>>> facilitiesPerZone) {
		scenario.getActivityFacilities().getFacilities().values().forEach((activityFacility -> {
			activityFacility.getActivityOptions().values().forEach(activityOption -> {
				facilitiesPerZone.computeIfAbsent((String) activityFacility.getAttributes().getAttribute("zone"), k -> new HashMap<>())
					.computeIfAbsent(activityOption.getType(), k -> new ArrayList<>()).add(activityFacility);
			});
		}));
	}

	/**
	 * @param originalScenario complete Scenario
	 * @param linksPerZone   list with Links for each region
	 */
	private void solveSeparatedVRPs(Scenario originalScenario, Map<String, Map<Id<Link>, Link>> linksPerZone) throws Exception {

		boolean splitCarrier = true;
		boolean splitVRPs = false;
		int maxServicesPerCarrier = 100;
		Map<Id<Carrier>, Carrier> allCarriers = new HashMap<>(
			CarriersUtils.getCarriers(originalScenario).getCarriers());
		Map<Id<Carrier>, Carrier> solvedCarriers = new HashMap<>();
		List<Id<Carrier>> keyList = new ArrayList<>(allCarriers.keySet());
		CarriersUtils.getCarriers(originalScenario).getCarriers().values().forEach(carrier -> {
			if (CarriersUtils.getJspritIterations(carrier) == 0) {
				allCarriers.remove(carrier.getId());
				solvedCarriers.put(carrier.getId(), carrier);
			}
		});
		int carrierSteps = 30;
		for (int i = 0; i < allCarriers.size(); i++) {
			int fromIndex = i * carrierSteps;
			int toIndex = (i + 1) * carrierSteps;
			if (toIndex >= allCarriers.size())
				toIndex = allCarriers.size();

			Map<Id<Carrier>, Carrier> subCarriers = new HashMap<>(allCarriers);
			List<Id<Carrier>> subList;
			if (splitVRPs) {
				subList = keyList.subList(fromIndex, toIndex);
				subCarriers.keySet().retainAll(subList);
			} else {
				fromIndex = 0;
				toIndex = allCarriers.size();
			}

			if (splitCarrier) {
				Map<Id<Carrier>, Carrier> subCarriersToAdd = new HashMap<>();
				List<Id<Carrier>> keyListCarrierToRemove = new ArrayList<>();
				for (Carrier carrier : subCarriers.values()) {

					int countedServices = 0;
					int countedVehicles = 0;
					if (carrier.getServices().size() > maxServicesPerCarrier) {

						int numberOfNewCarrier = (int) Math
							.ceil((double) carrier.getServices().size() / (double) maxServicesPerCarrier);
						int numberOfServicesPerNewCarrier = Math
							.round((float) carrier.getServices().size() / numberOfNewCarrier);

						int j = 0;
						while (j < numberOfNewCarrier) {

							int numberOfServicesForNewCarrier = numberOfServicesPerNewCarrier;
							int numberOfVehiclesForNewCarrier = numberOfServicesPerNewCarrier;
							if (j + 1 == numberOfNewCarrier) {
								numberOfServicesForNewCarrier = carrier.getServices().size() - countedServices;
								numberOfVehiclesForNewCarrier = carrier.getCarrierCapabilities().getCarrierVehicles()
									.size() - countedVehicles;
							}
							Carrier newCarrier = CarriersUtils.createCarrier(
								Id.create(carrier.getId().toString() + "_part_" + (j + 1), Carrier.class));
							CarrierCapabilities newCarrierCapabilities = CarrierCapabilities.Builder.newInstance()
								.setFleetSize(carrier.getCarrierCapabilities().getFleetSize()).build();
							newCarrierCapabilities.getCarrierVehicles()
								.putAll(carrier.getCarrierCapabilities().getCarrierVehicles());
							newCarrier.setCarrierCapabilities(newCarrierCapabilities);
							newCarrier.getServices().putAll(carrier.getServices());
							CarriersUtils.setJspritIterations(newCarrier, CarriersUtils.getJspritIterations(carrier));
							carrier.getAttributes().getAsMap().keySet().forEach(attribute -> newCarrier.getAttributes()
								.putAttribute(attribute, carrier.getAttributes().getAttribute(attribute)));

							List<Id<Vehicle>> vehiclesForNewCarrier = new ArrayList<>(
								carrier.getCarrierCapabilities().getCarrierVehicles().keySet());
							List<Id<CarrierService>> servicesForNewCarrier = new ArrayList<>(
								carrier.getServices().keySet());

							List<Id<Vehicle>> subListVehicles = vehiclesForNewCarrier.subList(
								j * numberOfServicesPerNewCarrier,
								j * numberOfServicesPerNewCarrier + numberOfVehiclesForNewCarrier);
							List<Id<CarrierService>> subListServices = servicesForNewCarrier.subList(
								j * numberOfServicesPerNewCarrier,
								j * numberOfServicesPerNewCarrier + numberOfServicesForNewCarrier);

							newCarrier.getCarrierCapabilities().getCarrierVehicles().keySet()
								.retainAll(subListVehicles);
							newCarrier.getServices().keySet().retainAll(subListServices);

							countedVehicles += newCarrier.getCarrierCapabilities().getCarrierVehicles().size();
							countedServices += newCarrier.getServices().size();

							subCarriersToAdd.put(newCarrier.getId(), newCarrier);
							j++;
						}
						keyListCarrierToRemove.add(carrier.getId());
						if (countedVehicles != carrier.getCarrierCapabilities().getCarrierVehicles().size())
							throw new Exception("Split parts of the carrier " + carrier.getId().toString()
								+ " has a different number of vehicles than the original carrier");
						if (countedServices != carrier.getServices().size())
							throw new Exception("Split parts of the carrier " + carrier.getId().toString()
								+ " has a different number of services than the original carrier");

					}
				}
				subCarriers.putAll(subCarriersToAdd);
				for (Id<Carrier> id : keyListCarrierToRemove) {
					subCarriers.remove(id);
				}
			}
			CarriersUtils.getCarriers(originalScenario).getCarriers().clear();
			CarriersUtils.getCarriers(originalScenario).getCarriers().putAll(subCarriers);
			log.info("Solving carriers {}-{} of all {} carriers. This are {} VRP to solve.", fromIndex + 1, toIndex, allCarriers.size(),
				subCarriers.size());
			CarriersUtils.runJsprit(originalScenario);
			solvedCarriers.putAll(CarriersUtils.getCarriers(originalScenario).getCarriers());
			CarriersUtils.getCarriers(originalScenario).getCarriers().clear();
			if (!splitVRPs)
				break;
		}
		CarriersUtils.getCarriers(originalScenario).getCarriers().putAll(solvedCarriers);
		CarriersUtils.getCarriers(originalScenario).getCarriers().values().forEach(carrier -> {
			if (linksPerZone != null && !carrier.getAttributes().getAsMap().containsKey("tourStartArea")) {
				List<String> startAreas = new ArrayList<>();
				for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()) {
					String tourStartZone = SmallScaleCommercialTrafficUtils
						.findZoneOfLink(tour.getTour().getStartLinkId(), linksPerZone);
					if (!startAreas.contains(tourStartZone))
						startAreas.add(tourStartZone);
				}
				carrier.getAttributes().putAttribute("tourStartArea",
					String.join(";", startAreas));
			}
		});
	}

	private void createCarriersAndDemand(Path output, Scenario scenario,
										 Map<String, Object2DoubleMap<String>> resultingDataPerZone,
										 Map<String, Map<Id<Link>, Link>> linksPerZone, String smallScaleCommercialTrafficType,
										 boolean includeExistingModels) throws Exception {

		ArrayList<String> modesORvehTypes;
		if (smallScaleCommercialTrafficType.equals("goodsTraffic"))
			modesORvehTypes = new ArrayList<>(
				Arrays.asList("vehTyp1", "vehTyp2", "vehTyp3", "vehTyp4", "vehTyp5"));
		else if (smallScaleCommercialTrafficType.equals("commercialPersonTraffic"))
			modesORvehTypes = new ArrayList<>(List.of("total"));
		else
			throw new Exception("Invalid traffic type selected!");

		TrafficVolumeGeneration.setInputParameters(smallScaleCommercialTrafficType);

		Map<TrafficVolumeGeneration.TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_start = TrafficVolumeGeneration
			.createTrafficVolume_start(resultingDataPerZone, output, sample, modesORvehTypes, smallScaleCommercialTrafficType);
		Map<TrafficVolumeGeneration.TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_stop = TrafficVolumeGeneration
			.createTrafficVolume_stop(resultingDataPerZone, output, sample, modesORvehTypes, smallScaleCommercialTrafficType);

		if (includeExistingModels) {
			integrateExistingTrafficToSmallScaleCommercial.readExistingCarriersFromFolder(scenario, sample, linksPerZone);
			integrateExistingTrafficToSmallScaleCommercial.reduceDemandBasedOnExistingCarriers(scenario, linksPerZone, smallScaleCommercialTrafficType,
				trafficVolumePerTypeAndZone_start, trafficVolumePerTypeAndZone_stop);
		}
		final TripDistributionMatrix odMatrix = createTripDistribution(trafficVolumePerTypeAndZone_start,
			trafficVolumePerTypeAndZone_stop, smallScaleCommercialTrafficType, scenario, output, linksPerZone);
		createCarriers(scenario, odMatrix, resultingDataPerZone, smallScaleCommercialTrafficType, linksPerZone);
	}

	/**
	 * Reads and checks config if all necessary parameters are set.
	 */
	private Config readAndCheckConfig(Path configPath, String modelName, String sampleName, Path output) throws Exception {

		Config config = ConfigUtils.loadConfig(configPath.toString());
		if (output == null || output.toString().isEmpty())
			config.controller().setOutputDirectory(Path.of(config.controller().getOutputDirectory()).resolve(modelName)
				.resolve(usedSmallScaleCommercialTrafficType.toString() + "_" + sampleName + "pct" + "_"
					+ LocalDate.now() + "_" + LocalTime.now().toSecondOfDay() + "_" + resistanceFactor)
				.toString());
		else
			config.controller().setOutputDirectory(output.toString());

		// Reset some config values that are not needed
		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(0);
		config.plans().setInputFile(null);
		config.transit().setTransitScheduleFile(null);
		config.transit().setVehiclesFile(null);
		config.counts().setInputFile(null);
		config.facilities().setInputFile(pathToCommercialFacilities.toString());
		// Set flow and storage capacity to a high value
		config.qsim().setFlowCapFactor(sample * 4);
		config.qsim().setStorageCapFactor(sample * 4);
		config.qsim().setUsePersonIdForMissingVehicleId(true);

		// Overwrite network
		if (network != null)
			config.network().setInputFile(network);

		// Some files are written before the controller is created, deleting the directory is not an option
		config.controller().setOverwriteFileSetting(overwriteExistingFiles);

		new OutputDirectoryHierarchy(config.controller().getOutputDirectory(), config.controller().getRunId(),
			config.controller().getOverwriteFileSetting(), ControllerConfigGroup.CompressionType.gzip);
		new File(Path.of(config.controller().getOutputDirectory()).resolve("calculatedData").toString()).mkdir();
		MatsimRandom.getRandom().setSeed(config.global().getRandomSeed());

		rnd = MatsimRandom.getRandom();
		rng = new MersenneTwister(config.global().getRandomSeed());

		if (config.network().getInputFile() == null)
			throw new Exception("No network file in config");
		if (config.global().getCoordinateSystem() == null)
			throw new Exception("No global CRS is set in config");
		if (config.controller().getOutputDirectory() == null)
			throw new Exception("No output directory was set");

		return config;
	}

	/**
	 * Prepares the controller.
	 */
	private Controler prepareControler(Scenario scenario) {
		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new CarrierModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(CarrierStrategyManager.class).toProvider(
					new MyCarrierPlanStrategyManagerFactory(CarriersUtils.getCarrierVehicleTypes(scenario)));
				bind(CarrierScoringFunctionFactory.class).toInstance(new MyCarrierScoringFunctionFactory());
			}
		});

		controler.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

		return controler;
	}

	/**
	 * Creates the carriers and the related demand, based on the generated
	 * TripDistributionMatrix.
	 */
	private void createCarriers(Scenario scenario, TripDistributionMatrix odMatrix,
								Map<String, Object2DoubleMap<String>> resultingDataPerZone, String smallScaleCommercialTrafficType,
								Map<String, Map<Id<Link>, Link>> linksPerZone) {
		int maxNumberOfCarrier = odMatrix.getListOfPurposes().size() * odMatrix.getListOfZones().size()
			* odMatrix.getListOfModesOrVehTypes().size();
		int createdCarrier = 0;
		int fixedNumberOfVehiclePerTypeAndLocation = 1; //TODO possible improvement, perhaps check KiD

		EnumeratedDistribution<TourStartAndDuration> tourDistribution = getCommercialTourSpecifications.createTourDistribution(smallScaleCommercialTrafficType, rng);

		Map<StopDurationGoodTrafficKey, EnumeratedDistribution<DurationsBounds>> stopDurationTimeSelector = getCommercialTourSpecifications.createStopDurationDistributionPerCategory(smallScaleCommercialTrafficType, rng);

		CarrierVehicleTypes carrierVehicleTypes = CarriersUtils.getCarrierVehicleTypes(scenario);
		Map<Id<VehicleType>, VehicleType> additionalCarrierVehicleTypes = scenario.getVehicles().getVehicleTypes();

		// Only a vehicle with cost information will work properly
		additionalCarrierVehicleTypes.values().stream()
			.filter(vehicleType -> vehicleType.getCostInformation().getCostsPerSecond() != null)
			.forEach(vehicleType -> carrierVehicleTypes.getVehicleTypes().putIfAbsent(vehicleType.getId(), vehicleType));

		for (VehicleType vehicleType : carrierVehicleTypes.getVehicleTypes().values()) {
			CostInformation costInformation = vehicleType.getCostInformation();
			VehicleUtils.setCostsPerSecondInService(costInformation, costInformation.getCostsPerSecond());
			VehicleUtils.setCostsPerSecondWaiting(costInformation, costInformation.getCostsPerSecond());
		}

		for (Integer purpose : odMatrix.getListOfPurposes()) {
			for (String startZone : odMatrix.getListOfZones()) {
				for (String modeORvehType : odMatrix.getListOfModesOrVehTypes()) {
					boolean isStartingLocation = false;
					checkIfIsStartingPosition:
					{
						for (String possibleStopZone : odMatrix.getListOfZones()) {
							if (!modeORvehType.equals("pt") && !modeORvehType.equals("op"))
								if (odMatrix.getTripDistributionValue(startZone, possibleStopZone, modeORvehType,
									purpose, smallScaleCommercialTrafficType) != 0) {
									isStartingLocation = true;
									break checkIfIsStartingPosition;
								}
						}
					}
					//TODO make vehcile selection configurable
					if (isStartingLocation) {
						double occupancyRate = 0;
						String[] possibleVehicleTypes = null;
						ArrayList<String> startCategory = new ArrayList<>();
						ArrayList<String> stopCategory = new ArrayList<>();
						stopCategory.add("Employee Primary Sector");
						stopCategory.add("Employee Construction");
						stopCategory.add("Employee Secondary Sector Rest");
						stopCategory.add("Employee Retail");
						stopCategory.add("Employee Traffic/Parcels");
						stopCategory.add("Employee Tertiary Sector Rest");
						stopCategory.add("Inhabitants");
						if (purpose == 1) {
							if (smallScaleCommercialTrafficType.equals("commercialPersonTraffic")) {
								possibleVehicleTypes = new String[]{"vwCaddy", "e_SpaceTourer"};
								occupancyRate = 1.5;
							}
							startCategory.add("Employee Secondary Sector Rest");
							stopCategory.clear();
							stopCategory.add("Employee Secondary Sector Rest");
						} else if (purpose == 2) {
							if (smallScaleCommercialTrafficType.equals("commercialPersonTraffic")) {
								possibleVehicleTypes = new String[]{"vwCaddy", "e_SpaceTourer"};
								occupancyRate = 1.6;
							}
							startCategory.add("Employee Secondary Sector Rest");
						} else if (purpose == 3) {
							if (smallScaleCommercialTrafficType.equals("commercialPersonTraffic")) {
								possibleVehicleTypes = new String[]{"golf1.4", "c_zero"};
								occupancyRate = 1.2;
							}
							startCategory.add("Employee Retail");
							startCategory.add("Employee Tertiary Sector Rest");
						} else if (purpose == 4) {
							if (smallScaleCommercialTrafficType.equals("commercialPersonTraffic")) {
								possibleVehicleTypes = new String[]{"golf1.4", "c_zero"};
								occupancyRate = 1.2;
							}
							startCategory.add("Employee Traffic/Parcels");
						} else if (purpose == 5) {
							if (smallScaleCommercialTrafficType.equals("commercialPersonTraffic")) {
								possibleVehicleTypes = new String[]{"mercedes313", "e_SpaceTourer"};
								occupancyRate = 1.7;
							}
							startCategory.add("Employee Construction");
						} else if (purpose == 6) {
							startCategory.add("Inhabitants");
						}
						if (smallScaleCommercialTrafficType.equals("goodsTraffic")) {
							occupancyRate = 1.;
							switch (modeORvehType) {
								case "vehTyp1" ->
									possibleVehicleTypes = new String[]{"vwCaddy", "e_SpaceTourer"}; // possible to add more types, see source
								case "vehTyp2" ->
									possibleVehicleTypes = new String[]{"mercedes313", "e_SpaceTourer"};
								case "vehTyp3", "vehTyp4" ->
									possibleVehicleTypes = new String[]{"light8t", "light8t_electro"};
								case "vehTyp5" ->
									possibleVehicleTypes = new String[]{"medium18t", "medium18t_electro", "heavy40t", "heavy40t_electro"};
							}
						}

						// use only types of the possibleTypes which are in the given types file
						List<String> vehicleTypes = new ArrayList<>();
						assert possibleVehicleTypes != null;

						for (String possibleVehicleType : possibleVehicleTypes) {
							if (CarriersUtils.getCarrierVehicleTypes(scenario).getVehicleTypes().containsKey(
								Id.create(possibleVehicleType, VehicleType.class)))
								vehicleTypes.add(possibleVehicleType);
						}
						// find a start category with existing employees in this zone
						Collections.shuffle(startCategory, rnd);
						String selectedStartCategory = startCategory.getFirst();
						for (int count = 1; resultingDataPerZone.get(startZone).getDouble(selectedStartCategory) == 0; count++) {
							if (count <= startCategory.size())
								selectedStartCategory = startCategory.get(rnd.nextInt(startCategory.size()));
							else
								selectedStartCategory = stopCategory.get(rnd.nextInt(stopCategory.size()));
						}
						String carrierName = null;
						if (smallScaleCommercialTrafficType.equals("goodsTraffic")) {
							carrierName = "Carrier_Goods_" + startZone + "_purpose_" + purpose + "_" + modeORvehType;
						} else if (smallScaleCommercialTrafficType.equals("commercialPersonTraffic"))
							carrierName = "Carrier_Business_" + startZone + "_purpose_" + purpose;
						int numberOfDepots = odMatrix.getSumOfServicesForStartZone(startZone, modeORvehType, purpose,
							smallScaleCommercialTrafficType);
						FleetSize fleetSize = FleetSize.FINITE;
						ArrayList<String> vehicleDepots = new ArrayList<>();
						createdCarrier++;
						log.info("Create carrier number {} of a maximum Number of {} carriers.", createdCarrier, maxNumberOfCarrier);
						log.info("Carrier: {}; depots: {}; services: {}", carrierName, numberOfDepots,
							(int) Math.ceil(odMatrix.getSumOfServicesForStartZone(startZone, modeORvehType,
								purpose, smallScaleCommercialTrafficType) / occupancyRate));
						createNewCarrierAndAddVehicleTypes(scenario, purpose, startZone,
							selectedStartCategory, carrierName, vehicleTypes, numberOfDepots, fleetSize,
							fixedNumberOfVehiclePerTypeAndLocation, vehicleDepots, linksPerZone, smallScaleCommercialTrafficType,
							tourDistribution);
						log.info("Create services for carrier: {}", carrierName);
						for (String stopZone : odMatrix.getListOfZones()) {
							int trafficVolumeForOD = Math.round((float)odMatrix.getTripDistributionValue(startZone,
								stopZone, modeORvehType, purpose, smallScaleCommercialTrafficType));
							int numberOfJobs = (int) Math.ceil(trafficVolumeForOD / occupancyRate);
							if (numberOfJobs == 0)
								continue;
							// find a category for the tour stop with existing employees in this zone
							String selectedStopCategory = stopCategory.get(rnd.nextInt(stopCategory.size()));
							while (resultingDataPerZone.get(stopZone).getDouble(selectedStopCategory) == 0)
								selectedStopCategory = stopCategory.get(rnd.nextInt(stopCategory.size()));
							String[] serviceArea = new String[]{stopZone};
							int serviceTimePerStop;
							if (selectedStartCategory.equals("Inhabitants"))
								serviceTimePerStop = getServiceTimePerStop(stopDurationTimeSelector, startCategory.getFirst(), modeORvehType, smallScaleCommercialTrafficType);
							else
								serviceTimePerStop = getServiceTimePerStop(stopDurationTimeSelector, selectedStartCategory, modeORvehType, smallScaleCommercialTrafficType);

							TimeWindow serviceTimeWindow = TimeWindow.newInstance(0,
								24 * 3600); //TODO eventuell anpassen wegen veränderter Tourzeiten
							createServices(scenario, vehicleDepots, selectedStopCategory, carrierName,
								numberOfJobs, serviceArea, serviceTimePerStop, serviceTimeWindow, linksPerZone);
						}
					}
				}
			}
		}

//		System.out.println("Final results for the start time distribution");
//		tourStartTimeSelector.writeResults();

//		System.out.println("Final results for the tour duration distribution");
//		tourDurationTimeSelector.writeResults();

//		for (StopDurationGoodTrafficKey sector : stopDurationTimeSelector.keySet()) {
//			System.out.println("Final results for the stop duration distribution in sector " + sector);
//			stopDurationTimeSelector.get(sector);
//		}

		log.warn("The jspritIterations are now set to {} in this simulation!", jspritIterations);
		log.info("Finished creating {} carriers including related services.", createdCarrier);
	}

	/**
	 * Creates the services for one carrier.
	 */
	private void createServices(Scenario scenario, ArrayList<String> noPossibleLinks,
								String selectedStopCategory, String carrierName, int numberOfJobs, String[] serviceArea,
								Integer serviceTimePerStop, TimeWindow serviceTimeWindow,
								Map<String, Map<Id<Link>, Link>> linksPerZone) {

		String stopZone = serviceArea[0];

		for (int i = 0; i < numberOfJobs; i++) {

			Id<Link> linkId = findPossibleLink(stopZone, selectedStopCategory, noPossibleLinks, linksPerZone);
			Id<CarrierService> idNewService = Id.create(carrierName + "_" + linkId + "_" + rnd.nextInt(10000),
				CarrierService.class);

			CarrierService thisService = CarrierService.Builder.newInstance(idNewService, linkId)
				.setServiceDuration(serviceTimePerStop).setServiceStartTimeWindow(serviceTimeWindow).build();
			CarriersUtils.getCarriers(scenario).getCarriers().get(Id.create(carrierName, Carrier.class)).getServices()
				.put(thisService.getId(), thisService);
		}

	}

	/**
	 * Creates the carrier and the related vehicles.
	 */
	private void createNewCarrierAndAddVehicleTypes(Scenario scenario, Integer purpose, String startZone,
													String selectedStartCategory, String carrierName,
													List<String> vehicleTypes, int numberOfDepots, FleetSize fleetSize,
													int fixedNumberOfVehiclePerTypeAndLocation,
													List<String> vehicleDepots, Map<String, Map<Id<Link>, Link>> linksPerZone,
													String smallScaleCommercialTrafficType,
													EnumeratedDistribution<TourStartAndDuration> tourStartTimeSelector) {

		Carriers carriers = CarriersUtils.addOrGetCarriers(scenario);
		CarrierVehicleTypes carrierVehicleTypes = CarriersUtils.getCarrierVehicleTypes(scenario);

		CarrierCapabilities carrierCapabilities;

		Carrier thisCarrier = CarriersUtils.createCarrier(Id.create(carrierName, Carrier.class));
		if (smallScaleCommercialTrafficType.equals("commercialPersonTraffic") && purpose == 3)
			thisCarrier.getAttributes().putAttribute("subpopulation", smallScaleCommercialTrafficType + "_service");
		else
			thisCarrier.getAttributes().putAttribute("subpopulation", smallScaleCommercialTrafficType);

		thisCarrier.getAttributes().putAttribute("purpose", purpose);
		thisCarrier.getAttributes().putAttribute("tourStartArea", startZone);
		if (jspritIterations > 0)
			CarriersUtils.setJspritIterations(thisCarrier, jspritIterations);
		carrierCapabilities = CarrierCapabilities.Builder.newInstance().setFleetSize(fleetSize).build();
		carriers.addCarrier(thisCarrier);

		while (vehicleDepots.size() < numberOfDepots) {
			Id<Link> linkId = findPossibleLink(startZone, selectedStartCategory, null, linksPerZone);
			vehicleDepots.add(linkId.toString());
		}

		for (String singleDepot : vehicleDepots) {
			TourStartAndDuration t = tourStartTimeSelector.sample();

			int vehicleStartTime = getVehicleStartTime(t);
			int tourDuration = getVehicleTourDuration(t);
			int vehicleEndTime = vehicleStartTime + tourDuration;
			for (String thisVehicleType : vehicleTypes) { //TODO Flottenzusammensetzung anpassen. Momentan pro Depot alle Fahrzeugtypen 1x erzeugen
				VehicleType thisType = carrierVehicleTypes.getVehicleTypes()
					.get(Id.create(thisVehicleType, VehicleType.class));
				if (fixedNumberOfVehiclePerTypeAndLocation == 0)
					fixedNumberOfVehiclePerTypeAndLocation = 1;
				for (int i = 0; i < fixedNumberOfVehiclePerTypeAndLocation; i++) {
					CarrierVehicle newCarrierVehicle = CarrierVehicle.Builder
						.newInstance(
							Id.create(
								thisCarrier.getId().toString() + "_"
									+ (carrierCapabilities.getCarrierVehicles().size() + 1),
								Vehicle.class),
							Id.createLinkId(singleDepot), thisType)
						.setEarliestStart(vehicleStartTime).setLatestEnd(vehicleEndTime).build();
					carrierCapabilities.getCarrierVehicles().put(newCarrierVehicle.getId(), newCarrierVehicle);
					if (!carrierCapabilities.getVehicleTypes().contains(thisType))
						carrierCapabilities.getVehicleTypes().add(thisType);
				}
			}

			thisCarrier.setCarrierCapabilities(carrierCapabilities);
		}
	}

	/**
	 * Gives a duration for the created tour under the given probability.
	 *
	 */
	private int getVehicleTourDuration(TourStartAndDuration t) {
		return (int) rnd.nextDouble(t.minDuration * 60, t.maxDuration * 60);
	}

	/**
	 * Gives a tour start time for the created tour under the given probability.
	 */
	private int getVehicleStartTime(TourStartAndDuration t) {
		return rnd.nextInt(t.hourLower * 3600, t.hourUpper * 3600);
	}


	/**
	 * Give a service duration based on the purpose and the trafficType under a given probability
	 *
	 * @param serviceDurationTimeSelector 		the selector for the service duration
	 * @param employeeCategory 					the category of the employee
	 * @param modeORvehType 					the mode or vehicle type
	 * @param smallScaleCommercialTrafficType 	the traffic type
	 * @return 									the service duration
	 */
	private Integer getServiceTimePerStop(Map<StopDurationGoodTrafficKey, EnumeratedDistribution<DurationsBounds>> serviceDurationTimeSelector,
										  String employeeCategory,
										  String modeORvehType, String smallScaleCommercialTrafficType) {
		StopDurationGoodTrafficKey key = null;
		if (smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.commercialPersonTraffic.toString()))
			key = makeStopDurationGoodTrafficKey(employeeCategory, null);
		else if (smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.goodsTraffic.toString())) {
			key = makeStopDurationGoodTrafficKey(employeeCategory, modeORvehType);
		}
		DurationsBounds serviceDurationBounds = serviceDurationTimeSelector.get(key).sample();
		int serviceDurationLowerBound = serviceDurationBounds.minDuration();
		int serviceDurationUpperBound = serviceDurationBounds.maxDuration();
		return rnd.nextInt(serviceDurationLowerBound * 60, serviceDurationUpperBound * 60);
	}

	/**
	 * Finds a possible link for a service or the vehicle location.
	 */
	private Id<Link> findPossibleLink(String zone, String selectedCategory, List<String> noPossibleLinks,
									  Map<String, Map<Id<Link>, Link>> linksPerZone) {

		Id<Link> newLink = null;
		for (int a = 0; newLink == null && a < facilitiesPerZone.get(zone).get(selectedCategory).size() * 2; a++) {

			ActivityFacility possibleBuilding = facilitiesPerZone.get(zone).get(selectedCategory)
				.get(rnd.nextInt(facilitiesPerZone.get(zone).get(selectedCategory).size())); //TODO Wkt für die Auswahl anpassen
			Coord centroidPointOfBuildingPolygon = possibleBuilding.getCoord();

			int numberOfPossibleLinks = linksPerZone.get(zone).size();

			// searches and selects the nearest link of the possible links in this zone
			newLink = SmallScaleCommercialTrafficUtils.findNearestPossibleLink(zone, noPossibleLinks, linksPerZone, newLink,
				centroidPointOfBuildingPolygon, numberOfPossibleLinks);
		}
		if (newLink == null)
			throw new RuntimeException("No possible link for buildings with type '" + selectedCategory + "' in zone '"
				+ zone + "' found. buildings in category: " + facilitiesPerZone.get(zone).get(selectedCategory)
				+ "; possibleLinks in zone: " + linksPerZone.get(zone).size());
		return newLink;
	}

	/**
	 * Filters links by used mode "car" and creates Map with all links in each zone
	 */
	static Map<String, Map<Id<Link>, Link>> filterLinksForZones(Scenario scenario, Index indexZones,
																Map<String, Map<String, List<ActivityFacility>>> facilitiesPerZone,
																String shapeFileZoneNameColumn) {
		Map<String, Map<Id<Link>, Link>> linksPerZone = new HashMap<>();
		log.info("Filtering and assign links to zones. This take some time...");

		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
		Set<String> modes = new HashSet<>();
		modes.add("car");
		Network filteredNetwork = NetworkUtils.createNetwork(scenario.getConfig().network());
		filter.filter(filteredNetwork, modes);

		CoordinateTransformation ct = indexZones.getShp().createTransformation(ProjectionUtils.getCRS(scenario.getNetwork()));
		//TODO possible check if newCoord attribute is really needed (find better way)
		List<Link> links = new ArrayList<>(filteredNetwork.getLinks().values());
		links.forEach(l -> l.getAttributes().putAttribute("newCoord",
			CoordUtils.round(ct.transform(l.getCoord()))));
		links.forEach(l -> l.getAttributes().putAttribute("zone",
			indexZones.query((Coord) l.getAttributes().getAttribute("newCoord"))));
		links = links.stream().filter(l -> l.getAttributes().getAttribute("zone") != null).toList();
		links.forEach(l -> linksPerZone
			.computeIfAbsent((String) l.getAttributes().getAttribute("zone"), (k) -> new HashMap<>())
			.put(l.getId(), l));
		if (linksPerZone.size() != indexZones.size())
			findNearestLinkForZonesWithoutLinks(filteredNetwork, linksPerZone, indexZones, facilitiesPerZone, shapeFileZoneNameColumn);

		return linksPerZone;
	}

	/**
	 * Finds for areas without links the nearest Link if the area contains any building.
	 */
	private static void findNearestLinkForZonesWithoutLinks(Network networkToChange, Map<String, Map<Id<Link>, Link>> linksPerZone,
															Index shpZones,
															Map<String, Map<String, List<ActivityFacility>>> facilitiesPerZone,
															String shapeFileZoneNameColumn) {
		for (SimpleFeature singleArea : shpZones.getAllFeatures()) {
			String zoneID = (String) singleArea.getAttribute(shapeFileZoneNameColumn);
			if (!linksPerZone.containsKey(zoneID) && facilitiesPerZone.get(zoneID) != null) {
				for (List<ActivityFacility> buildingList : facilitiesPerZone.get(zoneID).values()) {
					for (ActivityFacility building : buildingList) {
						Link l = NetworkUtils.getNearestLinkExactly(networkToChange, building.getCoord());
                        assert l != null;
                        linksPerZone
							.computeIfAbsent(zoneID, (k) -> new HashMap<>())
							.put(l.getId(), l);
					}
				}
			}
		}
	}

	/**
	 * Creates the number of trips between the zones for each mode and purpose.
	 */
	private TripDistributionMatrix createTripDistribution(
		Map<TrafficVolumeGeneration.TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_start,
		Map<TrafficVolumeGeneration.TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_stop,
		String smallScaleCommercialTrafficType, Scenario scenario, Path output, Map<String, Map<Id<Link>, Link>> linksPerZone)
		throws Exception {

		ArrayList<String> listOfZones = new ArrayList<>();
		trafficVolume_start.forEach((k, v) -> {
			if (!listOfZones.contains(k.getZone()))
				listOfZones.add(k.getZone());
		});
		final TripDistributionMatrix odMatrix = TripDistributionMatrix.Builder
			.newInstance(indexZones, trafficVolume_start, trafficVolume_stop, smallScaleCommercialTrafficType, listOfZones).build();
		Network network = scenario.getNetwork();
		int count = 0;

		for (TrafficVolumeGeneration.TrafficVolumeKey trafficVolumeKey : trafficVolume_start.keySet()) {
			count++;
			if (count % 50 == 0 || count == 1)
				log.info("Create OD pair {} of {}", count, trafficVolume_start.size());

			String startZone = trafficVolumeKey.getZone();
			String modeORvehType = trafficVolumeKey.getModeORvehType();
			for (Integer purpose : trafficVolume_start.get(trafficVolumeKey).keySet()) {
				Collections.shuffle(listOfZones, rnd);
				for (String stopZone : listOfZones) {
					odMatrix.setTripDistributionValue(startZone, stopZone, modeORvehType, purpose, smallScaleCommercialTrafficType,
						network, linksPerZone, resistanceFactor, shapeFileZoneNameColumn);
				}
			}
		}
		odMatrix.clearRoundingError();
		odMatrix.writeODMatrices(output, smallScaleCommercialTrafficType);
		return odMatrix;
	}

	private static class MyCarrierScoringFunctionFactory implements CarrierScoringFunctionFactory {

		@Inject
		private Network network;

		@Override
		public ScoringFunction createScoringFunction(Carrier carrier) {
			SumScoringFunction sf = new SumScoringFunction();
			DriversLegScoring driverLegScoring = new DriversLegScoring(carrier, network);
			VehicleEmploymentScoring vehicleEmploymentScoring = new VehicleEmploymentScoring(carrier);
			DriversActivityScoring actScoring = new DriversActivityScoring();
			sf.addScoringFunction(driverLegScoring);
			sf.addScoringFunction(vehicleEmploymentScoring);
			sf.addScoringFunction(actScoring);
			return sf;
		}

	}

	private static class MyCarrierPlanStrategyManagerFactory implements Provider<CarrierStrategyManager> {

		@Inject
		private Network network;

		@Inject
		private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

		@Inject
		private Map<String, TravelTime> modeTravelTimes;

		private final CarrierVehicleTypes types;

		public MyCarrierPlanStrategyManagerFactory(CarrierVehicleTypes types) {
			this.types = types;
		}

		@Override
		public CarrierStrategyManager get() {
			TravelDisutility travelDisutility = CarrierTravelDisutilities.createBaseDisutility(types,
				modeTravelTimes.get(TransportMode.car));
			final LeastCostPathCalculator router = leastCostPathCalculatorFactory.createPathCalculator(network,
				travelDisutility, modeTravelTimes.get(TransportMode.car));

//			final GenericStrategyManager<CarrierPlan, Carrier> strategyManager = new GenericStrategyManager<>();
			final CarrierStrategyManager strategyManager = CarrierControlerUtils.createDefaultCarrierStrategyManager();
			strategyManager.setMaxPlansPerAgent(5);
			{
				GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>(
					new ExpBetaPlanChanger.Factory<CarrierPlan, Carrier>().setBetaValue(1.0).build());

				strategyManager.addStrategy(strategy, null, 1.0);

			}
			{
				GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>(
					new KeepSelected<>());
				strategy.addStrategyModule(new CarrierTimeAllocationMutator.Factory().build());
				strategy.addStrategyModule(new
					CarrierReRouteVehicles.Factory(router, network, modeTravelTimes.get(TransportMode.car)).build());
				strategyManager.addStrategy(strategy, null, 0.5);
			}
			return strategyManager;
		}
	}

	static class DriversActivityScoring implements SumScoringFunction.BasicScoring, SumScoringFunction.ActivityScoring {


		private double score;

		public DriversActivityScoring() {
			super();
		}

		@Override
		public void finish() {
		}

		@Override
		public double getScore() {
			return score;
		}

		@Override
		public void handleFirstActivity(Activity act) {
			handleActivity(act);
		}

		@Override
		public void handleActivity(Activity act) {
			if (act instanceof FreightActivity) {
				double actStartTime = act.getStartTime().seconds();

				// log.info(act + " start: " + Time.writeTime(actStartTime));
				TimeWindow tw = ((FreightActivity) act).getTimeWindow();
				if (actStartTime > tw.getEnd()) {
					double missedTimeWindowPenalty = 0.01;
					double penalty_score = (-1) * (actStartTime - tw.getEnd()) * missedTimeWindowPenalty;
					if (!(penalty_score <= 0.0))
						throw new AssertionError("penalty score must be negative");
					// log.info("penalty " + penalty_score);
					score += penalty_score;

				}
				double timeParameter = 0.008;
				double actTimeCosts = (act.getEndTime().seconds() - actStartTime) * timeParameter;
				// log.info("actCosts " + actTimeCosts);
				if (!(actTimeCosts >= 0.0))
					throw new AssertionError("actTimeCosts must be positive");
				score += actTimeCosts * (-1);
			}
		}

		@Override
		public void handleLastActivity(Activity act) {
			handleActivity(act);
		}

	}

	static class DriversLegScoring implements SumScoringFunction.BasicScoring, SumScoringFunction.LegScoring {

		// private static final Logger log = Logger.getLogger(DriversLegScoring.class);

		private double score = 0.0;
		private final Network network;
		private final Carrier carrier;
		private final Set<CarrierVehicle> employedVehicles;

		public DriversLegScoring(Carrier carrier, Network network) {
			super();
			this.network = network;
			this.carrier = carrier;
			employedVehicles = new HashSet<>();
		}

		@Override
		public void finish() {

		}

		@Override
		public double getScore() {
			return score;
		}

		private double getTimeParameter(CarrierVehicle vehicle) {
			return vehicle.getType().getCostInformation().getCostsPerSecond();
		}

		private double getDistanceParameter(CarrierVehicle vehicle) {
			return vehicle.getType().getCostInformation().getCostsPerMeter();
		}

		@Override
		public void handleLeg(Leg leg) {
			if (leg.getRoute() instanceof NetworkRoute nRoute) {
				Id<Vehicle> vehicleId = nRoute.getVehicleId();
				CarrierVehicle vehicle = CarriersUtils.getCarrierVehicle(carrier, vehicleId);
				Gbl.assertNotNull(vehicle);
				employedVehicles.add(vehicle);
				double distance = 0.0;
				if (leg.getRoute() instanceof NetworkRoute) {
					Link startLink = network.getLinks().get(leg.getRoute().getStartLinkId());
					distance += startLink.getLength();
					for (Id<Link> linkId : ((NetworkRoute) leg.getRoute()).getLinkIds()) {
						distance += network.getLinks().get(linkId).getLength();
					}
					distance += network.getLinks().get(leg.getRoute().getEndLinkId()).getLength();
				}
				double distanceCosts = distance * getDistanceParameter(vehicle);
				if (!(distanceCosts >= 0.0))
					throw new AssertionError("distanceCosts must be positive");
				score += (-1) * distanceCosts;
				double timeCosts = leg.getTravelTime().seconds() * getTimeParameter(vehicle);
				if (!(timeCosts >= 0.0))
					throw new AssertionError("distanceCosts must be positive");
				score += (-1) * timeCosts;
			}
		}
	}

	static class VehicleEmploymentScoring implements SumScoringFunction.BasicScoring {

		private final Carrier carrier;

		public VehicleEmploymentScoring(Carrier carrier) {
			super();
			this.carrier = carrier;
		}

		@Override
		public void finish() {

		}

		@Override
		public double getScore() {
			double score = 0.;
			CarrierPlan selectedPlan = carrier.getSelectedPlan();
			if (selectedPlan == null)
				return 0.;
			for (ScheduledTour tour : selectedPlan.getScheduledTours()) {
				if (!tour.getTour().getTourElements().isEmpty()) {
					score += (-1) * tour.getVehicle().getType().getCostInformation().getFixedCosts();
				}
			}
			return score;
		}

	}

	public record StopDurationGoodTrafficKey(String employeeCategory, String vehicleType) {

		@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				StopDurationGoodTrafficKey other = (StopDurationGoodTrafficKey) obj;
				if (employeeCategory == null) {
					if (other.employeeCategory != null)
						return false;
				} else if (!employeeCategory.equals(other.employeeCategory))
					return false;
				if (vehicleType == null) {
					return other.vehicleType == null;
				} else return vehicleType.equals(other.vehicleType);
			}
		}
	public static StopDurationGoodTrafficKey makeStopDurationGoodTrafficKey(String employeeCategory, String vehicleType) {
		return new StopDurationGoodTrafficKey(employeeCategory, vehicleType);
	}

	public record TourStartAndDuration(int hourLower, int hourUpper, double minDuration, double maxDuration) {}

	public record DurationsBounds(int minDuration, int maxDuration) {}

}
