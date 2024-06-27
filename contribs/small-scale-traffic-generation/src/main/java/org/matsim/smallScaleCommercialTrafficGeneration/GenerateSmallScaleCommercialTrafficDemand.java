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
import org.apache.commons.math3.util.Pair;
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
import org.matsim.vehicles.CostInformation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import picocli.CommandLine;

import java.io.File;
import java.net.URISyntaxException;
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
	private static IntegrateExistingTrafficToSmallScaleCommercial integrateExistingTrafficToSmallScaleCommercial;

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
		integrateExistingTrafficToSmallScaleCommercial = new DefaultIntegrateExistingTrafficToSmallScaleCommercialImpl();
		log.info("Using default {} if existing models are integrated!", DefaultIntegrateExistingTrafficToSmallScaleCommercialImpl.class.getSimpleName());
	}
	public GenerateSmallScaleCommercialTrafficDemand(IntegrateExistingTrafficToSmallScaleCommercial integrateExistingTrafficToSmallScaleCommercial) {
		GenerateSmallScaleCommercialTrafficDemand.integrateExistingTrafficToSmallScaleCommercial = integrateExistingTrafficToSmallScaleCommercial;
		log.info("Using {} if existing models are integrated!", integrateExistingTrafficToSmallScaleCommercial.getClass().getSimpleName());
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
	 * @param scenario
	 * @param facilitiesPerZone
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

		EnumeratedDistribution<TourStartAndDuration> tourDistribution = createTourDistribution(smallScaleCommercialTrafficType);

		Map<StopDurationGoodTrafficKey, ValueSelectorUnderGivenProbability> stopDurationTimeSelector = createStopDurationTimeDistributionPerCategory(
			smallScaleCommercialTrafficType);

		CarrierVehicleTypes carrierVehicleTypes = CarriersUtils.getCarrierVehicleTypes(scenario);
		Map<Id<VehicleType>, VehicleType> additionalCarrierVehicleTypes = scenario.getVehicles().getVehicleTypes();

		// Only vehicle with cost information will work properly
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
						String selectedStartCategory = startCategory.get(0);
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
								serviceTimePerStop = getServiceTimePerStop(stopDurationTimeSelector, startCategory.get(0), modeORvehType, smallScaleCommercialTrafficType);
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

		for (StopDurationGoodTrafficKey sector : stopDurationTimeSelector.keySet()) {
			System.out.println("Final results for the stop duration distribution in sector " + sector);
			stopDurationTimeSelector.get(sector).writeResults();
		}

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
	 * @param serviceDurationTimeSelector
	 * @param employeeCategory
	 * @param modeORvehType
	 * @param smallScaleCommercialTrafficType
	 * @return
	 */
	private Integer getServiceTimePerStop(Map<StopDurationGoodTrafficKey, ValueSelectorUnderGivenProbability> serviceDurationTimeSelector,
										  String employeeCategory,
										  String modeORvehType, String smallScaleCommercialTrafficType) {
		StopDurationGoodTrafficKey key = null;
		if (smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.commercialPersonTraffic.toString()))
			key = makeStopDurationGoodTrafficKey(employeeCategory, null);
		else if (smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.goodsTraffic.toString())) {
			key = makeStopDurationGoodTrafficKey(employeeCategory, modeORvehType);
		}
		ValueSelectorUnderGivenProbability.ProbabilityForValue selectedValue = serviceDurationTimeSelector.get(
			key).getNextValueUnderGivenProbability();
		int serviceDurationLowerBound = Integer.parseInt(selectedValue.getValue());
		int serviceDurationUpperBound = Integer.parseInt(selectedValue.getUpperBound());
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
																String shapeFileZoneNameColumn) throws URISyntaxException {
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

	/**
	 * Creates the probability distribution for the tour start times for the day.
	 * The values are given in [h] and have an upperBound.
	 * Data source: KiD 2002
	 *
	 * @return
	 */

	private ValueSelectorUnderGivenProbability createTourStartTimeDistribution(String smallScaleCommercialTrafficType) {

		List<ValueSelectorUnderGivenProbability.ProbabilityForValue> tourStartProbabilityDistribution = new ArrayList<>();
		if (smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.commercialPersonTraffic.toString())) {
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "1", 0.002));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("1", "2", 0.001));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("2", "3", 0.001));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("3", "4", 0.002));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("4", "5", 0.008));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("5", "6", 0.031));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("6", "7", 0.144));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("7", "8", 0.335));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("8", "9", 0.182));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("9", "10", 0.108));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "11", 0.057));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("11", "12", 0.032));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("12", "13", 0.021));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("13", "14", 0.021));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("14", "15", 0.019));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("15", "16", 0.012));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("16", "17", 0.009));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("17", "18", 0.006));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("18", "19", 0.004));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("19", "20", 0.003));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "21", 0.001));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("22", "23", 0.001));
		} else if (smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.goodsTraffic.toString())) {
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "1", 0.008));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("1", "2", 0.003));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("2", "3", 0.008));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("3", "4", 0.012));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("4", "5", 0.028));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("5", "6", 0.052));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("6", "7", 0.115));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("7", "8", 0.222));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("8", "9", 0.197));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("9", "10", 0.14));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "11", 0.076));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("11", "12", 0.035));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("12", "13", 0.022));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("13", "14", 0.022));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("14", "15", 0.021));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("15", "16", 0.014));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("16", "17", 0.008));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("17", "18", 0.005));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("18", "19", 0.004));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("19", "20", 0.002));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "21", 0.001));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("21", "22", 0.001));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("22", "23", 0.002));
			tourStartProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("23", "24", 0.001));

		}
		return new ValueSelectorUnderGivenProbability(tourStartProbabilityDistribution, rnd);
	}

	/**
	 * Creates the probability distribution for the tour duration for the day.
	 * The values are given in [h] and have an upperBound.
	 * Data source: KiD 2002
	 *
	 * @return
	 */
	private ValueSelectorUnderGivenProbability createTourDurationTimeDistribution(String smallScaleCommercialTrafficType) {
		List<ValueSelectorUnderGivenProbability.ProbabilityForValue> tourDurationProbabilityDistribution = new ArrayList<>();
		if (smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.commercialPersonTraffic.toString())) {
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "1", 0.14));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("1", "2", 0.066));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("2", "3", 0.056));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("3", "4", 0.052));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("4", "5", 0.061));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("5", "6", 0.063));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("6", "7", 0.07));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("7", "8", 0.086));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("8", "9", 0.14));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("9", "10", 0.122));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "11", 0.068));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("11", "12", 0.031));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("12", "13", 0.018));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("13", "14", 0.01));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("14", "15", 0.006));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("15", "16", 0.003));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("16", "17", 0.002));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("17", "18", 0.001));
		} else if (smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.goodsTraffic.toString())) {
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "1", 0.096));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("1", "2", 0.074));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("2", "3", 0.065));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("3", "4", 0.071));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("4", "5", 0.086));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("5", "6", 0.084));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("6", "7", 0.084));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("7", "8", 0.101));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("8", "9", 0.118));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("9", "10", 0.092));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "11", 0.048));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("11", "12", 0.027));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("12", "13", 0.015));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("13", "14", 0.011));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("14", "15", 0.006));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("15", "16", 0.004));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("16", "17", 0.002));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("17", "18", 0.001));
			tourDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("18", "19", 0.001));
		}

		return new ValueSelectorUnderGivenProbability(tourDurationProbabilityDistribution, rnd);
	}

	private EnumeratedDistribution<TourStartAndDuration> createTourDistribution(String smallScaleCommercialTrafficType) {
		List<Pair<TourStartAndDuration, Double>> tourDurationProbabilityDistribution = new ArrayList<>();

		if (smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.commercialPersonTraffic.toString())) {

			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 0.0, 30.0), 0.0005917893035900173));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 30.0, 60.0), 0.00021859484237437887));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 90.0, 120.0), 0.00037490287407786324));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 120.0, 180.0), 0.0004337321926125666));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 180.0, 240.0), 0.0005834182239827621));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 240.0, 300.0), 0.0005116938323661723));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 300.0, 360.0), 0.0005027065159573272));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 360.0, 420.0), 0.0006719740164147071));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 420.0, 480.0), 0.00022375027665644004));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 480.0, 540.0), 0.00022103749529549306));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 540.0, 600.0), 0.00022119440831885122));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 600.0, 660.0), 0.0002732185104003396));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 660.0, 720.0), 7.287567629774946e-05));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 720.0, 780.0), 0.0005090670761685264));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 780.0, 840.0), 0.0002169454122557984));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 840.0, 1080.0), 0.0016947794402011696));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 0.0, 30.0), 0.00033050926084770643));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 30.0, 60.0), 0.0004963985976117265));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 60.0, 90.0), 0.0009458837608304906));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 90.0, 120.0), 0.0006507941771038976));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 120.0, 180.0), 0.0002949035696660126));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 180.0, 240.0), 0.0005812406149568905));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 240.0, 300.0), 0.00072666224822023));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 300.0, 360.0), 0.0006017750128936798));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 360.0, 420.0), 0.0007696491628020603));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 420.0, 480.0), 0.0006951014583380694));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 480.0, 540.0), 0.0006675367479652174));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 540.0, 600.0), 0.0009951412624367468));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 600.0, 660.0), 0.0006193958232902363));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 660.0, 720.0), 0.0005496335422364244));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 720.0, 780.0), 0.000963763774344583));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 780.0, 840.0), 0.001585152586657775));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 840.0, 1080.0), 0.0022779973751500433));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 0.0, 30.0), 0.003678291745870938));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 30.0, 60.0), 0.0037749680865755936));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 60.0, 90.0), 0.0021464058981758467));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 90.0, 120.0), 0.0010105726369455444));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 120.0, 180.0), 0.0017166729332290624));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 180.0, 240.0), 0.001218657902054598));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 240.0, 300.0), 0.0019212859349972463));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 300.0, 360.0), 0.0018498349748915703));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 360.0, 420.0), 0.0020820722844894844));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 420.0, 480.0), 0.0033255032578691536));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 480.0, 540.0), 0.004499580798913233));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 540.0, 600.0), 0.004508722079694882));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 600.0, 660.0), 0.009460453046374911));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 660.0, 720.0), 0.008632039128635343));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 720.0, 780.0), 0.005173130409039029));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 780.0, 840.0), 0.0021287189901771954));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 840.0, 1080.0), 0.002735246591728173));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 0.0, 30.0), 0.015534599731489868));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 30.0, 60.0), 0.009424737666749776));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 60.0, 90.0), 0.003979757502241877));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 90.0, 120.0), 0.0026219034509082214));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 120.0, 180.0), 0.004373894821911171));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 180.0, 240.0), 0.005349695968407728));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 240.0, 300.0), 0.008398668008895199));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 300.0, 360.0), 0.013017576110359298));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 360.0, 420.0), 0.013178466937493282));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 420.0, 480.0), 0.015799261066253244));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 480.0, 540.0), 0.031932993774084484));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 540.0, 600.0), 0.056976770375347194));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 600.0, 660.0), 0.03411514635058722));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 660.0, 720.0), 0.010952547256934878));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 720.0, 780.0), 0.005071677294689363));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 780.0, 840.0), 0.002758017802376135));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 840.0, 1080.0), 0.003182481371327368));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 0.0, 30.0), 0.018010507239762663));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 30.0, 60.0), 0.009246211080247332));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 60.0, 90.0), 0.006297103845359016));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 90.0, 120.0), 0.003415561088528113));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 120.0, 180.0), 0.010918022744746231));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 180.0, 240.0), 0.011371721163141522));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 240.0, 300.0), 0.01861910064916215));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 300.0, 360.0), 0.015443374909900384));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 360.0, 420.0), 0.020470726990450452));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 420.0, 480.0), 0.030727618880727087));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 480.0, 540.0), 0.07364088624635841));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 540.0, 600.0), 0.04082061588575034));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 600.0, 660.0), 0.012935881167590665));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 660.0, 720.0), 0.005469250367916343));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 720.0, 780.0), 0.0030030673084490513));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 780.0, 840.0), 0.0011042643367551329));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 840.0, 1080.0), 0.0011327583672022575));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 0.0, 30.0), 0.015589932735904798));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 30.0, 60.0), 0.007157798082590814));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 60.0, 90.0), 0.006563655710107534));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 90.0, 120.0), 0.004888423230467872));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 120.0, 180.0), 0.01261126944262904));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 180.0, 240.0), 0.013275311108363174));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 240.0, 300.0), 0.011059737216827653));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 300.0, 360.0), 0.00980644443311104));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 360.0, 420.0), 0.013476523854959467));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 420.0, 480.0), 0.01766932338862498));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 480.0, 540.0), 0.013855266610087914));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 540.0, 600.0), 0.006090238569895901));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 600.0, 660.0), 0.00326688741194661));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 660.0, 720.0), 0.0009742217966822537));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 720.0, 780.0), 0.0008462163162537791));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 780.0, 840.0), 0.0009357453082055104));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 840.0, 1080.0), 0.0006867783494497427));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 0.0, 30.0), 0.011836581569331607));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 30.0, 60.0), 0.0060475163532472224));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 60.0, 90.0), 0.006091033719221284));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 90.0, 120.0), 0.004870323217391879));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 120.0, 180.0), 0.009852214102720915));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 180.0, 240.0), 0.006649077724867284));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 240.0, 300.0), 0.006549809619698136));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 300.0, 360.0), 0.00743649188225418));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 360.0, 420.0), 0.008370330719772223));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 420.0, 480.0), 0.006055410372169952));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 480.0, 540.0), 0.003221026290023441));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 540.0, 600.0), 0.00270804359225063));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 600.0, 660.0), 0.0011328763880567346));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 660.0, 720.0), 0.0005295062815147344));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 720.0, 780.0), 0.0005244739409173669));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 780.0, 840.0), 0.00022261373811852168));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 840.0, 1080.0), 0.0002976820307410009));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 0.0, 30.0), 0.0072347359578799255));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 30.0, 60.0), 0.005528762818372258));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 60.0, 90.0), 0.004301874597910846));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 90.0, 120.0), 0.002706271535768685));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 120.0, 180.0), 0.004461225555303183));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 180.0, 240.0), 0.003289266637558867));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 240.0, 300.0), 0.004773112389257731));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 300.0, 360.0), 0.004153307715767419));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 360.0, 420.0), 0.0023002274828502435));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 420.0, 480.0), 0.002295722460734858));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 480.0, 540.0), 0.0008008191218782178));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 540.0, 600.0), 0.0005302938593833011));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 600.0, 660.0), 0.00012017333498779025));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 660.0, 720.0), 0.00029497120761336085));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 720.0, 780.0), 7.442207741095891e-05));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 780.0, 840.0), 7.491510042413546e-05));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 0.0, 30.0), 0.005979044848708125));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 30.0, 60.0), 0.0030727725862362003));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 60.0, 90.0), 0.0018328582061095421));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 90.0, 120.0), 0.0015730248216810105));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 120.0, 180.0), 0.0025909176745678485));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 180.0, 240.0), 0.0023584284876344117));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 240.0, 300.0), 0.002888683132930499));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 300.0, 360.0), 0.0026723295114103734));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 360.0, 420.0), 0.001368034507711622));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 420.0, 480.0), 0.001322142609646873));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 480.0, 540.0), 0.00014896322977011863));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 540.0, 600.0), 0.00036793050573151096));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 600.0, 660.0), 0.0003024749417379503));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 660.0, 720.0), 7.263766179594998e-05));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 720.0, 780.0), 7.737798495114381e-05));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 840.0, 1080.0), 7.360037219024495e-05));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 0.0, 30.0), 0.005442934607459622));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 30.0, 60.0), 0.0023099603288455053));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 60.0, 90.0), 0.0015476125810207045));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 90.0, 120.0), 0.0015690710859882222));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 120.0, 180.0), 0.003155552178314994));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 180.0, 240.0), 0.0024715148201473933));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 240.0, 300.0), 0.00214638868043489));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 300.0, 360.0), 0.0017134793037846727));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 360.0, 420.0), 0.0009684921868733149));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 420.0, 480.0), 0.0005519992558366529));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 480.0, 540.0), 0.0004441672064981391));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 540.0, 600.0), 0.00022332686365997108));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 600.0, 660.0), 0.00023780343565208111));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 660.0, 720.0), 0.00014898555439278127));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 0.0, 30.0), 0.0065652971880044205));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 30.0, 60.0), 0.0033645458423904226));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 60.0, 90.0), 0.002247264924524252));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 90.0, 120.0), 0.0021755851670695867));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 120.0, 180.0), 0.00292250684836152));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 180.0, 240.0), 0.0029939610328467135));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 240.0, 300.0), 0.0013771262994841458));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 300.0, 360.0), 0.0005929387919824101));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 360.0, 420.0), 0.0007299574379337656));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 420.0, 480.0), 0.00015161310680499916));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 480.0, 540.0), 0.00022326623210165028));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 540.0, 600.0), 0.00021908720500178134));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 0.0, 30.0), 0.004700575755513116));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 30.0, 60.0), 0.002876930233578738));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 60.0, 90.0), 0.0012326059557891803));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 90.0, 120.0), 0.001688513011030605));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 120.0, 180.0), 0.0024148215923521744));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 180.0, 240.0), 0.0009664823712470381));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 240.0, 300.0), 0.0008158516384741175));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 300.0, 360.0), 0.0005326476409500361));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 360.0, 420.0), 0.00037447250704764534));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 420.0, 480.0), 7.278074100962308e-05));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 480.0, 540.0), 0.00015460621875651884));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 540.0, 600.0), 0.00022625636961834557));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 840.0, 1080.0), 7.369704340227916e-05));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(15, 16, 0.0, 30.0), 0.005421542133242069));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(15, 16, 30.0, 60.0), 0.0028543297205245563));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(15, 16, 60.0, 90.0), 0.001320449445343739));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(15, 16, 90.0, 120.0), 0.0011372744623221703));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(15, 16, 120.0, 180.0), 0.0011175546229352943));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(15, 16, 180.0, 240.0), 0.0005212091408906178));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(15, 16, 240.0, 300.0), 0.00025063117439263165));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(15, 16, 300.0, 360.0), 0.0002906557976189996));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(15, 16, 360.0, 420.0), 6.934683987097806e-05));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(15, 16, 420.0, 480.0), 7.198332684426051e-05));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(16, 17, 0.0, 30.0), 0.005997678933359281));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(16, 17, 30.0, 60.0), 0.0014450238860978966));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(16, 17, 60.0, 90.0), 0.0008909835110546583));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(16, 17, 90.0, 120.0), 0.0008692603958852261));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(16, 17, 120.0, 180.0), 0.0004645626068627116));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(16, 17, 180.0, 240.0), 0.0005161866418057845));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(16, 17, 240.0, 300.0), 0.00047492492382272117));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(16, 17, 300.0, 360.0), 7.348989097075777e-05));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(16, 17, 360.0, 420.0), 0.0003000342936128893));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(17, 18, 0.0, 30.0), 0.004621906661329853));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(17, 18, 30.0, 60.0), 0.0015152391398060199));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(17, 18, 60.0, 90.0), 0.0006769045119123614));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(17, 18, 90.0, 120.0), 0.00044820275277284946));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(17, 18, 120.0, 180.0), 0.0007140653752077821));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(17, 18, 180.0, 240.0), 0.0001502672132808765));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(17, 18, 240.0, 300.0), 0.0003842231300012746));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(17, 18, 300.0, 360.0), 0.00021634404805889257));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(18, 19, 0.0, 30.0), 0.0034023082743939916));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(18, 19, 30.0, 60.0), 0.0006251774232962365));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(18, 19, 60.0, 90.0), 0.00022163965781205308));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(18, 19, 90.0, 120.0), 7.360037219024495e-05));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(18, 19, 120.0, 180.0), 0.00045934601255169126));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(18, 19, 180.0, 240.0), 7.511874968194916e-05));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(18, 19, 240.0, 300.0), 0.0001486019187134722));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(18, 19, 300.0, 360.0), 7.505084488366769e-05));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(18, 19, 420.0, 480.0), 7.594714627228585e-05));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(19, 24, 0.0, 30.0), 0.005137034953520923));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(19, 24, 30.0, 60.0), 0.0010774703023578233));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(19, 24, 60.0, 90.0), 0.00048539418673270443));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(19, 24, 90.0, 120.0), 0.0002988049182984063));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(19, 24, 120.0, 180.0), 0.00032644209078127245));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(19, 24, 180.0, 240.0), 0.0005357497395368892));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(19, 24, 240.0, 300.0), 0.0002944914928100358));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(19, 24, 300.0, 360.0), 0.00022851651374757815));
		}
		else if (smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.goodsTraffic.toString())) {

			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 0.0, 30.0), 0.0002666800577200411));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 30.0, 60.0), 0.0006395055678719748));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 60.0, 90.0), 0.0007110769046958423));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 90.0, 120.0), 0.0006665961628449491));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 120.0, 180.0), 0.0023195866923785575));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 180.0, 240.0), 0.00261751319938476));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 240.0, 300.0), 0.0021430032453503087));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 300.0, 360.0), 0.0029303876579925905));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 360.0, 420.0), 0.00283576618143643));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 420.0, 480.0), 0.0027188265347502893));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 480.0, 540.0), 0.002597768116531099));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 540.0, 600.0), 0.002659151494701916));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 600.0, 660.0), 0.0021738406044924437));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 660.0, 720.0), 0.0021949848461843176));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 720.0, 780.0), 0.0021801193011023083));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 780.0, 840.0), 0.001746033717539671));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(0, 4, 840.0, 1080.0), 0.00350888397405923));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 0.0, 30.0), 0.0006845643884312735));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 30.0, 60.0), 0.0004003126952082357));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 60.0, 90.0), 0.0008155012585632697));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 90.0, 120.0), 0.0010930534970200114));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 120.0, 180.0), 0.0011760353713952051));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 180.0, 240.0), 0.0019364061980548415));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 240.0, 300.0), 0.002953452881036028));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 300.0, 360.0), 0.002589370165068672));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 360.0, 420.0), 0.0025604405819583055));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 420.0, 480.0), 0.0034319041631081476));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 480.0, 540.0), 0.0033480025727905907));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 540.0, 600.0), 0.002175717502193024));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 600.0, 660.0), 0.0028036478238686957));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 660.0, 720.0), 0.0028759635193342887));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 720.0, 780.0), 0.0017584406503249872));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 780.0, 840.0), 0.0016742001219093045));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(4, 5, 840.0, 1080.0), 0.0020658205220468245));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 0.0, 30.0), 0.0017247403950228777));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 30.0, 60.0), 0.003090998236080484));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 60.0, 90.0), 0.0015209554995803177));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 90.0, 120.0), 0.0016533392810110293));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 120.0, 180.0), 0.003732306124403562));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 180.0, 240.0), 0.004106247357091271));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 240.0, 300.0), 0.003188442431357427));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 300.0, 360.0), 0.005929370570550301));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 360.0, 420.0), 0.005992695595693005));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 420.0, 480.0), 0.006390572360276255));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 480.0, 540.0), 0.00993732232424166));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 540.0, 600.0), 0.007917613781985494));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 600.0, 660.0), 0.00753055040114282));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 660.0, 720.0), 0.004839531706746983));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 720.0, 780.0), 0.003571294178536547));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 780.0, 840.0), 0.0022261075091276465));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(5, 6, 840.0, 1080.0), 0.0020123396391017526));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 0.0, 30.0), 0.00553085745500388));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 30.0, 60.0), 0.005164301035284355));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 60.0, 90.0), 0.0034287284279468384));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 90.0, 120.0), 0.003359657704287739));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 120.0, 180.0), 0.005963896679549981));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 180.0, 240.0), 0.006376396116305889));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 240.0, 300.0), 0.011553162434249647));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 300.0, 360.0), 0.01216390369869719));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 360.0, 420.0), 0.015303642980241483));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 420.0, 480.0), 0.01894502604909179));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 480.0, 540.0), 0.026995818384739457));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 540.0, 600.0), 0.03735238580259259));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 600.0, 660.0), 0.02007351137947408));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 660.0, 720.0), 0.007579189226621267));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 720.0, 780.0), 0.003806896198418994));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 780.0, 840.0), 0.0020371212990837376));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(6, 7, 840.0, 1080.0), 0.00246729057836831));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 0.0, 30.0), 0.007834929725170775));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 30.0, 60.0), 0.007875284751511802));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 60.0, 90.0), 0.0056369706407995695));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 90.0, 120.0), 0.007252792818630801));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 120.0, 180.0), 0.011595289158181222));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 180.0, 240.0), 0.01584695155572567));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 240.0, 300.0), 0.019385993489144607));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 300.0, 360.0), 0.01804569113072999));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 360.0, 420.0), 0.020338168968415053));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 420.0, 480.0), 0.03244941203821404));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 480.0, 540.0), 0.046986423884473));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 540.0, 600.0), 0.026127574804977814));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 600.0, 660.0), 0.006859707180170414));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 660.0, 720.0), 0.004053368732850601));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 720.0, 780.0), 0.0017728320836715625));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 780.0, 840.0), 0.0008117046283836942));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(7, 8, 840.0, 1080.0), 0.0014889766393137468));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 0.0, 30.0), 0.008702611915372131));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 30.0, 60.0), 0.009703391735884857));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 60.0, 90.0), 0.00833249802530372));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 90.0, 120.0), 0.008160824294542027));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 120.0, 180.0), 0.014522058792957903));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 180.0, 240.0), 0.019189639247661674));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 240.0, 300.0), 0.022628081955363144));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 300.0, 360.0), 0.018168175275565253));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 360.0, 420.0), 0.01830766579908246));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 420.0, 480.0), 0.022414786327228577));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 480.0, 540.0), 0.015454698179801149));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 540.0, 600.0), 0.00743339793333549));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 600.0, 660.0), 0.0028959167218627997));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 660.0, 720.0), 0.0011608823477359163));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 720.0, 780.0), 0.0006126324367099846));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 780.0, 840.0), 0.0007090395380022889));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(8, 9, 840.0, 1080.0), 0.0009650931773638335));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 0.0, 30.0), 0.010532384705529854));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 30.0, 60.0), 0.010106787618396446));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 60.0, 90.0), 0.007305519187631069));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 90.0, 120.0), 0.0065298278976416635));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 120.0, 180.0), 0.012991661099288086));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 180.0, 240.0), 0.011082392048301831));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 240.0, 300.0), 0.013735041027849332));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 300.0, 360.0), 0.012921165569106639));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 360.0, 420.0), 0.010187951930469277));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 420.0, 480.0), 0.0070071162811467125));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 480.0, 540.0), 0.003478434072337058));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 540.0, 600.0), 0.002487434148850001));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 600.0, 660.0), 0.0007617139935295275));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 660.0, 720.0), 0.0004794259473854554));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 720.0, 780.0), 0.00011828408353297643));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(9, 10, 780.0, 840.0), 0.0009221448817170415));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 0.0, 30.0), 0.0053803765038808364));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 30.0, 60.0), 0.00748440387556175));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 60.0, 90.0), 0.003817044622559703));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 90.0, 120.0), 0.0042559767658946045));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 120.0, 180.0), 0.004633517730561146));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 180.0, 240.0), 0.0040156278424527785));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 240.0, 300.0), 0.004097425621422603));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 300.0, 360.0), 0.00534407493573042));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 360.0, 420.0), 0.002849425985304954));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 420.0, 480.0), 0.0024443772372422234));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 480.0, 540.0), 0.0011258612568464076));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 540.0, 600.0), 0.0005966047093584399));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 600.0, 660.0), 0.0005779388889435179));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 660.0, 720.0), 0.0004527621290439082));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 720.0, 780.0), 0.00011727646428602624));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(10, 11, 780.0, 840.0), 0.00011130198744577025));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 0.0, 30.0), 0.0025301846046864363));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 30.0, 60.0), 0.002932856090944951));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 60.0, 90.0), 0.0015297442159744696));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 90.0, 120.0), 0.0016816440829740813));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 120.0, 180.0), 0.0023140070407952395));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 180.0, 240.0), 0.0013768767086426792));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 240.0, 300.0), 0.0019019317686819275));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 300.0, 360.0), 0.0015577691125463963));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 360.0, 420.0), 0.001499121306916632));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 420.0, 480.0), 0.0007361366421130972));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 480.0, 540.0), 0.0007423049940853575));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 540.0, 600.0), 0.00011130198744577025));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 660.0, 720.0), 0.00024243947114654707));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(11, 12, 720.0, 780.0), 0.000261579996858755));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 0.0, 30.0), 0.0021669594044717543));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 30.0, 60.0), 0.0033993161916113994));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 60.0, 90.0), 0.001870484877697732));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 90.0, 120.0), 0.0008448185262884799));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 120.0, 180.0), 0.002024573233571085));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 180.0, 240.0), 0.0021888099857994042));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 240.0, 300.0), 0.0021657834323017752));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 300.0, 360.0), 0.0010623089332746248));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 360.0, 420.0), 0.0006268095760401356));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 420.0, 480.0), 0.0005094532977538987));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 480.0, 540.0), 0.0004744090926784203));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 540.0, 600.0), 0.00016487328572417658));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(12, 13, 660.0, 720.0), 0.0001162996982120756));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 0.0, 30.0), 0.0033401411497772818));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 30.0, 60.0), 0.002492685695459365));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 60.0, 90.0), 0.0027064477589805068));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 90.0, 120.0), 0.0018052297053924354));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 120.0, 180.0), 0.0027984509294891498));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 180.0, 240.0), 0.0022758505657711914));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 240.0, 300.0), 0.0003535503655144059));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 300.0, 360.0), 0.0005890430396050117));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 360.0, 420.0), 0.0002319134363595028));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 420.0, 480.0), 0.00011617748025141993));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 480.0, 540.0), 0.0003690064941818713));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 540.0, 600.0), 0.0001650495071007077));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 600.0, 660.0), 0.00023113252306835525));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(13, 14, 840.0, 1080.0), 0.00017239206443126303));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 0.0, 30.0), 0.003543871129770451));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 30.0, 60.0), 0.0018407982276338393));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 60.0, 90.0), 0.0010649270862293423));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 90.0, 120.0), 0.0009538696044712171));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 120.0, 180.0), 0.0021318639289119572));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 180.0, 240.0), 0.0019740243143620277));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 240.0, 300.0), 0.0006157677659961421));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 300.0, 360.0), 0.0004035374922773149));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 360.0, 420.0), 0.00011607019237524387));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 420.0, 480.0), 0.0003938282727195195));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 480.0, 540.0), 0.00011130198744577025));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(14, 15, 600.0, 660.0), 0.00011942109323430472));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(15, 16, 0.0, 30.0), 0.00254340964132742));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(15, 16, 30.0, 60.0), 0.0017847751078888892));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(15, 16, 60.0, 90.0), 0.000841891386995212));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(15, 16, 90.0, 120.0), 0.0003543852337006742));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(15, 16, 120.0, 180.0), 0.0013974221085794884));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(15, 16, 180.0, 240.0), 0.0006229273683665316));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(15, 16, 240.0, 300.0), 0.00020579571489011056));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(15, 16, 300.0, 360.0), 0.0004809214516599411));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(15, 16, 360.0, 420.0), 0.00022514291890117063));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(15, 16, 420.0, 480.0), 0.00014748146383900364));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(15, 16, 720.0, 780.0), 0.00011605559293173729));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(16, 17, 0.0, 30.0), 0.0019634787835054656));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(16, 17, 30.0, 60.0), 0.000860670737476427));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(16, 17, 60.0, 90.0), 0.0003550148096943092));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(16, 17, 90.0, 120.0), 0.000855728546868917));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(16, 17, 120.0, 180.0), 0.0009283998993093458));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(16, 17, 180.0, 240.0), 0.00022795178106384156));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(16, 17, 240.0, 300.0), 0.00024119874825349313));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(16, 17, 420.0, 480.0), 0.00023429279224671318));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(16, 17, 480.0, 540.0), 0.00011727269965059726));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(16, 17, 660.0, 720.0), 0.00011130198744577025));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(17, 18, 0.0, 30.0), 0.0017099830161073832));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(17, 18, 30.0, 60.0), 0.0006015092064895483));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(17, 18, 60.0, 90.0), 0.00011819436012345105));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(17, 18, 90.0, 120.0), 0.0002279569151752547));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(17, 18, 120.0, 180.0), 0.0006440525787748041));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(17, 18, 180.0, 240.0), 0.0003142746964600832));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(17, 18, 300.0, 360.0), 0.00022788575876606104));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(17, 18, 360.0, 420.0), 0.0004761806298753505));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(17, 18, 480.0, 540.0), 0.00011727269965059726));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(18, 19, 0.0, 30.0), 0.0020011795184968267));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(18, 19, 30.0, 60.0), 0.00023620950461199452));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(18, 19, 60.0, 90.0), 0.00011935825257957617));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(18, 19, 90.0, 120.0), 0.00011130198744577025));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(18, 19, 120.0, 180.0), 0.00012222981614916706));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(18, 19, 180.0, 240.0), 0.0002377005397786721));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(18, 19, 240.0, 300.0), 0.00026373526728965034));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(18, 19, 300.0, 360.0), 0.000256086036315955));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(18, 19, 360.0, 420.0), 0.00011394287938236544));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(19, 24, 0.0, 30.0), 0.0021116872169622083));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(19, 24, 30.0, 60.0), 0.0003681765715703113));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(19, 24, 60.0, 90.0), 0.0004137833254678062));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(19, 24, 90.0, 120.0), 0.00025108497234833097));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(19, 24, 120.0, 180.0), 0.0007576827338029722));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(19, 24, 180.0, 240.0), 0.0005180490039062906));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(19, 24, 240.0, 300.0), 0.0004944106124208977));
			tourDurationProbabilityDistribution.add(Pair.create(new TourStartAndDuration(19, 24, 300.0, 360.0), 0.0002278857587658224));
		} else
			throw new IllegalArgumentException("Unknown small scale commercial traffic type: " + smallScaleCommercialTrafficType);

		return new EnumeratedDistribution<>(rng, tourDurationProbabilityDistribution);
	}

	/**
	 * Creates the probability distribution for the duration of the services.
	 * The values are given in [min] and have an upperBound.
	 * Data source: KiD 2002
	 *
	 * @param smallScaleCommercialTrafficType
	 * @return
	 */
	private Map<StopDurationGoodTrafficKey, ValueSelectorUnderGivenProbability> createStopDurationTimeDistributionPerCategory(String smallScaleCommercialTrafficType) {

		Map<StopDurationGoodTrafficKey, ValueSelectorUnderGivenProbability> stopDurationProbabilityDistribution = new HashMap<>();
		if (smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.commercialPersonTraffic.toString())) {
			List<ValueSelectorUnderGivenProbability.ProbabilityForValue> singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "30", 0.098));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "60", 0.17));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "90", 0.127));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.11));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "180", 0.17));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.076));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.057));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "360", 0.01));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("360", "420", 0.026));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "480", 0.045));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("480", "540", 0.064));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "600", 0.034));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("600", "720", 0.012));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("720", "840", 0.002));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Primary Sector", null),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "30", 0.054));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "60", 0.164));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "90", 0.153));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.087));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "180", 0.12));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.055));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.044));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "360", 0.02));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("360", "420", 0.025));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "480", 0.069));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("480", "540", 0.132));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "600", 0.058));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("600", "720", 0.016));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("720", "840", 0.002));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Construction", null),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "30", 0.13));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "60", 0.324));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "90", 0.178));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.108));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "180", 0.097));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.034));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.02));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "360", 0.018));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("360", "420", 0.02));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "480", 0.027));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("480", "540", 0.029));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "600", 0.008));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("600", "720", 0.006));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("720", "840", 0.001));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Secondary Sector Rest",null),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "30", 0.178));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "60", 0.301));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "90", 0.192));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.104));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "180", 0.092));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.043));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.013));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "360", 0.017));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("360", "420", 0.011));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "480", 0.016));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("480", "540", 0.016));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "600", 0.007));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("600", "720", 0.007));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("720", "840", 0.001));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Retail",null),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "30", 0.144));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "60", 0.372));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "90", 0.203));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.069));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "180", 0.112));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.038));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.011));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "360", 0.011));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("360", "420", 0.011));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "480", 0.012));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("480", "540", 0.007));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "600", 0.005));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("600", "720", 0.005));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Traffic/Parcels",null),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "30", 0.196));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "60", 0.292));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "90", 0.19));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.101));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "180", 0.105));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.034));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.017));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "360", 0.009));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("360", "420", 0.013));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "480", 0.019));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("480", "540", 0.014));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "600", 0.006));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("600", "720", 0.004));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("720", "840", 0.001));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Tertiary Sector Rest",null),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

		} else if (smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.goodsTraffic.toString())) {
			List<ValueSelectorUnderGivenProbability.ProbabilityForValue> singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.038));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.049));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.052));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.094));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.125));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.094));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.167));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.094));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.113));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.056));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.04));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.024));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.009));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.016));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.026));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "660", 0.002));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Primary Sector", "vehTyp1"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.025));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.025));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.05));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.043));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.112));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.168));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.149));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.081));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.168));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.068));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.068));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.025));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "660", 0.019));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Primary Sector", "vehTyp2"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.036));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.098));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.036));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.016));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.042));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.124));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.085));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.144));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.105));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.052));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.072));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.052));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.023));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.033));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.062));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "660", 0.016));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("660", "780", 0.003));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Primary Sector", "vehTyp3"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.071));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.143));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.429));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.179));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.107));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.071));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Primary Sector", "vehTyp4"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.026));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.395));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.158));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.132));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.026));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.105));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.079));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.026));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "660", 0.053));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Primary Sector", "vehTyp5"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.014));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.033));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.064));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.109));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.088));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.095));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.112));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.105));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.114));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.053));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.088));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.038));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.012));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.01));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.051));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "660", 0.015));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Construction", "vehTyp1"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.02));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.027));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.061));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.045));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.068));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.083));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.112));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.114));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.146));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.058));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.114));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.036));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.022));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.007));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.065));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "660", 0.023));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Construction", "vehTyp2"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.009));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.04));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.074));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.09));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.086));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.069));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.113));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.135));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.071));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.008));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.044));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.041));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.03));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.021));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.075));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "660", 0.022));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Construction", "vehTyp3"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.036));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.055));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.018));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.236));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.073));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.018));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.164));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.091));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.109));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.055));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.018));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.055));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.055));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "660", 0.018));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Construction", "vehTyp4"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.014));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.163));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.21));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.165));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.125));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.095));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.101));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.04));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.03));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.024));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.006));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.008));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.002));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.004));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.008));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "660", 0.004));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Construction", "vehTyp5"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.072));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.093));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.123));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.113));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.137));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.081));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.102));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.087));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.079));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.032));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.021));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.018));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.016));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.009));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.014));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "780", 0.002));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Secondary Sector Rest", "vehTyp1"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.062));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.14));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.093));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.115));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.133));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.102));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.098));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.071));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.067));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.038));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.027));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.011));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.009));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.024));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.011));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Secondary Sector Rest", "vehTyp2"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.051));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.214));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.146));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.129));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.10));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.072));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.083));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.063));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.054));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.02));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.016));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.022));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.008));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.007));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.011));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "900", 0.003));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Secondary Sector Rest", "vehTyp3"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.163));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.224));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.153));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.061));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.173));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.082));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.122));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.01));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.01));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Secondary Sector Rest", "vehTyp4"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.003));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.195));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.225));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.16));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.143));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.089));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.075));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.031));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.048));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.01));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.003));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.007));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "660", 0.009));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Secondary Sector Rest", "vehTyp5"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.057));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.108));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.093));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.133));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.133));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.11));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.102));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.064));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.104));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.049));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.015));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.015));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.003));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.005));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.006));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "660", 0.003));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Retail", "vehTyp1"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.084));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.119));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.183));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.076));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.085));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.101));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.124));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.069));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.057));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.041));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.002));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.025));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.011));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.007));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.007));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "660", 0.004));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("780", "900", 0.002));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Retail", "vehTyp2"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.103));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.23));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.193));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.08));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.065));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.071));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.072));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.044));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.054));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.035));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.009));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.013));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.014));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.01));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.003));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "660", 0.003));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Retail", "vehTyp3"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.094));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.179));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.094));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.245));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.123));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.075));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.094));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.038));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.019));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.009));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.009));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.019));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Retail", "vehTyp4"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.066));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.063));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.142));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.165));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.135));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.102));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.122));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.033));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.086));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.043));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.023));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.017));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.003));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Retail", "vehTyp5"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.159));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.173));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.173));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.088));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.115));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.071));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.024));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.051));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.041));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.02));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.031));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.024));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.017));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.007));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.007));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Traffic/Parcels", "vehTyp1"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.292));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.135));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.062));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.197));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.051));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.079));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.022));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.045));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.056));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.034));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.006));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.022));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Traffic/Parcels", "vehTyp2"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.092));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.111));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.224));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.173));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.09));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.103));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.045));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.028));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.056));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.017));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.019));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.025));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.006));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.007));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.006));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Traffic/Parcels", "vehTyp3"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.146));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.098));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.146));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.195));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.268));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.012));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.024));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.024));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.024));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.037));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.012));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.012));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Traffic/Parcels", "vehTyp4"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.026));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.042));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.062));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.121));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.133));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.144));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.144));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.104));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.121));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.046));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.011));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.026));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.007));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.005));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "900", 0.008));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Traffic/Parcels", "vehTyp5"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.061));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.093));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.101));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.125));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.125));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.101));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.124));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.08));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.093));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.046));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.013));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.017));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.011));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.004));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.005));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Tertiary Sector Rest", "vehTyp1"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.081));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.101));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.101));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.109));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.124));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.065));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.109));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.124));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.097));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.032));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.022));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.017));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.003));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.007));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.008));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Tertiary Sector Rest", "vehTyp2"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.052));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.114));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.155));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.111));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.151));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.112));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.125));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.043));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.051));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.026));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.014));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.016));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.011));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.007));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.009));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("660", "780", 0.003));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Tertiary Sector Rest", "vehTyp3"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.02));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.082));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.102));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.449));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.061));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.163));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.102));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.02));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Tertiary Sector Rest", "vehTyp4"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.02));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.02));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.151));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.296));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.156));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.065));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.121));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.05));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.075));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.01));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.015));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.01));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.005));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.005));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Employee Tertiary Sector Rest", "vehTyp5"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));



			// because no data für private persons; use average numbers of all employee categories
			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.056));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.084));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.095));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.118));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.12));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.096));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.112));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.083));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.095));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.045));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.033));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.022));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.011));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.007));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.018));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "660", 0.004));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Inhabitants", "vehTyp1"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.077));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.093));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.103));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.092));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.098));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.091));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.108));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.092));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.095));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.043));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.035));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.024));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.01));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.011));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.021));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "660", 0.007));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Inhabitants", "vehTyp2"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.06));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.141));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.152));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.107));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.094));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.087));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.089));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.067));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.06));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.037));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.023));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.025));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.015));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.012));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.024));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "660", 0.006));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("660", "780", 0.001));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Inhabitants", "vehTyp3"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.062));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.11));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.12));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.144));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.151));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.129));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.062));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.079));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.041));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.031));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.019));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.014));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.007));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.014));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "540", 0.014));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("540", "660", 0.002));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Inhabitants", "vehTyp4"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));

			singleStopDurationProbabilityDistribution = new ArrayList<>();
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("0", "10", 0.024));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("10", "20", 0.099));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("20", "30", 0.147));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("30", "40", 0.17));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("40", "50", 0.133));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("50", "60", 0.108));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("60", "75", 0.116));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("75", "90", 0.058));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("90", "120", 0.075));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("120", "150", 0.03));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("150", "180", 0.01));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("180", "240", 0.014));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("240", "300", 0.005));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("300", "420", 0.004));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("420", "660", 0.007));
			singleStopDurationProbabilityDistribution.add(new ValueSelectorUnderGivenProbability.ProbabilityForValue("660", "900", 0.002));
			stopDurationProbabilityDistribution.put(makeStopDurationGoodTrafficKey("Inhabitants", "vehTyp5"),
				new ValueSelectorUnderGivenProbability(singleStopDurationProbabilityDistribution, rnd));
		}
		return stopDurationProbabilityDistribution;
	}

	private record StopDurationGoodTrafficKey(String employeeCategory, String vehicleType) {

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
	private StopDurationGoodTrafficKey makeStopDurationGoodTrafficKey(String employeeCategory, String vehicleType) {
		return new StopDurationGoodTrafficKey(employeeCategory, vehicleType);
	}

	private record TourStartAndDuration(int hourLower, int hourUpper, double minDuration, double maxDuration) {}

}
