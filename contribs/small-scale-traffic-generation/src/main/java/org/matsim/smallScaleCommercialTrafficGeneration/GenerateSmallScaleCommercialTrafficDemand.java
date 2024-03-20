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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.locationtech.jts.geom.Geometry;
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
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.carriers.controler.*;
import org.matsim.freight.carriers.usecases.chessboard.CarrierTravelDisutilities;
import org.matsim.vehicles.CostInformation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles;

/**
 * Tool to generate small scale commercial traffic for a selected area. The needed input data are: employee information for the area and three shapes files (zones, buildings, landuse). These data should be available with OSM.
 *
 * @author Ricardo Ewert
 */
@CommandLine.Command(name = "generate-small-scale-commercial-traffic", description = "Generates plans for a small scale commercial traffic model", showDefaultValues = true)
public class GenerateSmallScaleCommercialTrafficDemand implements MATSimAppCommand {
	// freight traffic from extern:

	// Option 1: take "as is" from Chengqi code.

	// Option 2: differentiate FTL and LTL by Gütergruppe.  FTL as in option 1.  LTL per Gütergruppe _ein_ Ziel in Zone, = "Hub".  Verteilverkehr
	// von dort.  Startseite genauso.

	// Option 3: Leerkamp (nur in RVR Modell).

	// Option: Add prepare class with OSM Analyse and create facility file with results
	private static final Logger log = LogManager.getLogger(GenerateSmallScaleCommercialTrafficDemand.class);

	private enum CreationOption {
		useExistingCarrierFileWithSolution, createNewCarrierFile, useExistingCarrierFileWithoutSolution
	}

	private enum LanduseConfiguration {
		useOnlyOSMLanduse, useOSMBuildingsAndLanduse, useExistingDataDistribution
	}

	private enum SmallScaleCommercialTrafficType {
		commercialPersonTraffic, goodsTraffic, completeSmallScaleCommercialTraffic
	}

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to the config for small scale commercial generation")
	private Path configPath;

	@CommandLine.Option(names = "--pathToInvestigationAreaData", description = "Path to the investigation area data")
	private Path pathToInvestigationAreaData;

	@CommandLine.Option(names = "--pathToExistingDataDistributionToZones", description = "Path to the existing data distribution to zones. This is only needed if the option useExistingDataDistribution is selected.")
	private Path pathToExistingDataDistributionToZones;

	@CommandLine.Option(names = "--sample", description = "Scaling factor of the small scale commercial traffic (0, 1)", required = true)
	private double sample;

	@CommandLine.Option(names = "--jspritIterations", description = "Set number of jsprit iterations", required = true)
	private int jspritIterations;

	@CommandLine.Option(names = "--creationOption", description = "Set option of mode differentiation:  useExistingCarrierFileWithSolution, createNewCarrierFile, useExistingCarrierFileWithoutSolution")
	private CreationOption usedCreationOption;

	@CommandLine.Option(names = "--landuseConfiguration", description = "Set option of used OSM data. Options: useOnlyOSMLanduse, useOSMBuildingsAndLanduse, useExistingDataDistribution")
	private LanduseConfiguration usedLanduseConfiguration;

	@CommandLine.Option(names = "--smallScaleCommercialTrafficType", description = "Select traffic type. Options: commercialPersonTraffic, goodsTraffic, completeSmallScaleCommercialTraffic (contains both types)")
	private SmallScaleCommercialTrafficType usedSmallScaleCommercialTrafficType;

	@CommandLine.Option(names = "--includeExistingModels", description = "If models for some segments exist they can be included.")
	private boolean includeExistingModels;

	@CommandLine.Option(names = "--regionsShapeFileName", description = "Path of the region shape file.")
	private Path shapeFileRegionsPath;

		@CommandLine.Option(names = "--regionsShapeRegionColumn", description = "Name of the region column in the region shape file.")
	private String regionsShapeRegionColumn;

	@CommandLine.Option(names = "--zoneShapeFileName", description = "Path of the zone shape file.")
	private Path shapeFileZonePath;

	@CommandLine.Option(names = "--zoneShapeFileNameColumn", description = "Name of the unique column of the name/Id of each zone in the zones shape file.")
	private String shapeFileZoneNameColumn;

	@CommandLine.Option(names = "--buildingsShapeFileName", description = "Path of the buildings shape file")
	private Path shapeFileBuildingsPath;

	@CommandLine.Option(names = "--shapeFileBuildingTypeColumn", description = "Name of the unique column of the building type in the buildings shape file.")
	private String shapeFileBuildingTypeColumn;

	@CommandLine.Option(names = "--landuseShapeFileName", description = "Path of the landuse shape file")
	private Path shapeFileLandusePath;

	@CommandLine.Option(names = "--shapeFileLanduseTypeColumn", description = "Name of the unique column of the landuse type in the landuse shape file.")
	private String shapeFileLanduseTypeColumn;

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
	private final Map<String, Map<String, List<SimpleFeature>>> buildingsPerZone = new HashMap<>();
	private final Map<String, List<String>> landuseCategoriesAndDataConnection = new HashMap<>();

	private Index indexZones;
	private Index indexBuildings;
	private Index indexLanduse;
	private Index indexInvestigationAreaRegions;

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
			case useExistingCarrierFileWithSolution -> {
				log.info("Existing carriers (including carrier vehicle types ) should be set in the freight config group");
				if (includeExistingModels)
					throw new Exception(
						"You set that existing models should included to the new model. This is only possible for a creation of the new carrier file and not by using an existing.");
				freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
				if (config.vehicles() != null && freightCarriersConfigGroup.getCarriersVehicleTypesFile() == null)
					freightCarriersConfigGroup.setCarriersVehicleTypesFile(config.vehicles().getVehiclesFile());
				log.info("Load carriers from: " + freightCarriersConfigGroup.getCarriersFile());
				CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
			}
			case useExistingCarrierFileWithoutSolution -> {
				log.info("Existing carriers (including carrier vehicle types ) should be set in the freight config group");
				if (includeExistingModels)
					throw new Exception(
						"You set that existing models should included to the new model. This is only possible for a creation of the new carrier file and not by using an existing.");
				freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
				if (config.vehicles() != null && freightCarriersConfigGroup.getCarriersVehicleTypesFile() == null)
					freightCarriersConfigGroup.setCarriersVehicleTypesFile(config.vehicles().getVehiclesFile());
				log.info("Load carriers from: " + freightCarriersConfigGroup.getCarriersFile());
				CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
				solveSeparatedVRPs(scenario, null);
			}
			default -> {
				if (!Files.exists(shapeFileLandusePath)) {
					throw new Exception("Required landuse shape file not found:" + shapeFileLandusePath.toString());
				}
				if (!Files.exists(shapeFileBuildingsPath)) {
					throw new Exception(
						"Required OSM buildings shape file {} not found" + shapeFileBuildingsPath.toString());
				}
				if (!Files.exists(shapeFileZonePath)) {
					throw new Exception("Required districts shape file {} not found" + shapeFileZonePath.toString());
				}
				if (!Files.exists(shapeFileRegionsPath)) {
					throw new Exception("Required regions shape file {} not found" + shapeFileRegionsPath.toString());
				}

				indexZones = SmallScaleCommercialTrafficUtils.getIndexZones(shapeFileZonePath, shapeCRS, shapeFileZoneNameColumn);
				indexBuildings = SmallScaleCommercialTrafficUtils.getIndexBuildings(shapeFileBuildingsPath, shapeCRS, shapeFileBuildingTypeColumn);
				indexLanduse = SmallScaleCommercialTrafficUtils.getIndexLanduse(shapeFileLandusePath, shapeCRS, shapeFileLanduseTypeColumn);
				indexInvestigationAreaRegions = SmallScaleCommercialTrafficUtils.getIndexRegions(shapeFileRegionsPath, shapeCRS, regionsShapeRegionColumn);

				Map<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
					.createInputDataDistribution(output, landuseCategoriesAndDataConnection,
						usedLanduseConfiguration.toString(), indexLanduse, indexZones,
						indexBuildings, indexInvestigationAreaRegions, shapeFileZoneNameColumn, buildingsPerZone, pathToInvestigationAreaData,
						pathToExistingDataDistributionToZones);
				Map<String, Map<Id<Link>, Link>> linksPerZone = filterLinksForZones(scenario, indexZones, buildingsPerZone);

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

	/**
	 * @param originalScenario complete Scenario
	 * @param regionLinksMap   list with Links for each region
	 */
	private void solveSeparatedVRPs(Scenario originalScenario, Map<String, Map<Id<Link>, Link>> regionLinksMap) throws Exception {

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
			log.info("Solving carriers " + (fromIndex + 1) + "-" + (toIndex) + " of all " + allCarriers.size()
				+ " carriers. This are " + subCarriers.size() + " VRP to solve.");
			CarriersUtils.runJsprit(originalScenario);
			solvedCarriers.putAll(CarriersUtils.getCarriers(originalScenario).getCarriers());
			CarriersUtils.getCarriers(originalScenario).getCarriers().clear();
			if (!splitVRPs)
				break;
		}
		CarriersUtils.getCarriers(originalScenario).getCarriers().putAll(solvedCarriers);
		CarriersUtils.getCarriers(originalScenario).getCarriers().values().forEach(carrier -> {
			if (regionLinksMap != null && !carrier.getAttributes().getAsMap().containsKey("tourStartArea")) {
				List<String> startAreas = new ArrayList<>();
				for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()) {
					String tourStartZone = SmallScaleCommercialTrafficUtils
						.findZoneOfLink(tour.getTour().getStartLinkId(), regionLinksMap);
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
			SmallScaleCommercialTrafficUtils.readExistingModels(scenario, sample, linksPerZone);
			TrafficVolumeGeneration.reduceDemandBasedOnExistingCarriers(scenario, linksPerZone, smallScaleCommercialTrafficType,
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
					+ java.time.LocalDate.now() + "_" + java.time.LocalTime.now().toSecondOfDay() + "_" + resistanceFactor)
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
								Map<String, Map<Id<Link>, Link>> regionLinksMap) {
		int maxNumberOfCarrier = odMatrix.getListOfPurposes().size() * odMatrix.getListOfZones().size()
			* odMatrix.getListOfModesOrVehTypes().size();
		int createdCarrier = 0;
		int fixedNumberOfVehiclePerTypeAndLocation = 1; //TODO possible improvement, perhaps check KiD
		ValueSelectorUnderGivenProbability tourStartTimeSelector = createTourStartTimeDistribution(smallScaleCommercialTrafficType);
		ValueSelectorUnderGivenProbability tourDurationTimeSelector = createTourDurationTimeDistribution(smallScaleCommercialTrafficType);
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
						log.info("Create carrier number " + createdCarrier + " of a maximum Number of "
							+ maxNumberOfCarrier + " carriers.");
						log.info("Carrier: " + carrierName + "; depots: " + numberOfDepots + "; services: "
							+ (int) Math.ceil(odMatrix.getSumOfServicesForStartZone(startZone, modeORvehType,
							purpose, smallScaleCommercialTrafficType) / occupancyRate));
						createNewCarrierAndAddVehicleTypes(scenario, purpose, startZone,
							selectedStartCategory, carrierName, vehicleTypes, numberOfDepots, fleetSize,
							fixedNumberOfVehiclePerTypeAndLocation, vehicleDepots, regionLinksMap, smallScaleCommercialTrafficType,
							tourStartTimeSelector, tourDurationTimeSelector);
						log.info("Create services for carrier: " + carrierName);
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
								numberOfJobs, serviceArea, serviceTimePerStop, serviceTimeWindow, regionLinksMap);
						}
					}
				}
			}
		}
		System.out.println("Final results for the start time distribution");
		tourStartTimeSelector.writeResults();

		System.out.println("Final results for the tour duration distribution");
		tourDurationTimeSelector.writeResults();

		for (StopDurationGoodTrafficKey sector : stopDurationTimeSelector.keySet()) {
			System.out.println("Final results for the stop duration distribution in sector " + sector);
			stopDurationTimeSelector.get(sector).writeResults();
		}

		log.warn("The jspritIterations are now set to " + jspritIterations + " in this simulation!");
		log.info("Finished creating " + createdCarrier + " carriers including related services.");
	}

	/**
	 * Creates the services for one carrier.
	 */
	private void createServices(Scenario scenario, ArrayList<String> noPossibleLinks,
								String selectedStopCategory, String carrierName, int numberOfJobs, String[] serviceArea,
								Integer serviceTimePerStop, TimeWindow serviceTimeWindow,
								Map<String, Map<Id<Link>, Link>> regionLinksMap) {

		String stopZone = serviceArea[0];

		for (int i = 0; i < numberOfJobs; i++) {

			Id<Link> linkId = findPossibleLink(stopZone, selectedStopCategory, noPossibleLinks, regionLinksMap);
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
													List<String> vehicleDepots, Map<String, Map<Id<Link>, Link>> regionLinksMap,
													String smallScaleCommercialTrafficType,
													ValueSelectorUnderGivenProbability tourStartTimeSelector,
													ValueSelectorUnderGivenProbability tourDurationTimeSelector) {

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
			Id<Link> link = findPossibleLink(startZone, selectedStartCategory, null, regionLinksMap);
			vehicleDepots.add(link.toString());
		}

		for (String singleDepot : vehicleDepots) {
			int vehicleStartTime = getVehicleStartTime(tourStartTimeSelector);
			int tourDuration = getVehicleTourDuration(tourDurationTimeSelector);
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
	 * @param tourDurationTimeSelector
	 * @return
	 */
	private int getVehicleTourDuration(ValueSelectorUnderGivenProbability tourDurationTimeSelector) {
		ValueSelectorUnderGivenProbability.ProbabilityForValue selectedValue = tourDurationTimeSelector.getNextValueUnderGivenProbability();
		int tourDurationLowerBound = Integer.parseInt(selectedValue.getValue());
		int tourDurationUpperBound = Integer.parseInt(selectedValue.getUpperBound());
		return rnd.nextInt(tourDurationLowerBound * 3600, tourDurationUpperBound * 3600);
	}

	/**
	 * Gives a tour start time for the created tour under the given probability.
	 *
	 * @param tourStartTimeSelector
	 * @return
	 */
	private int getVehicleStartTime(ValueSelectorUnderGivenProbability tourStartTimeSelector) {
		ValueSelectorUnderGivenProbability.ProbabilityForValue selectedValue = tourStartTimeSelector.getNextValueUnderGivenProbability();
		int tourStartTimeLowerBound = Integer.parseInt(selectedValue.getValue());
		int tourStartTimeUpperBound = Integer.parseInt(selectedValue.getUpperBound());
		return rnd.nextInt(tourStartTimeLowerBound * 3600, tourStartTimeUpperBound * 3600);

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
									  Map<String, Map<Id<Link>, Link>> regionLinksMap) {

		if (buildingsPerZone.isEmpty()) {
			List<SimpleFeature> buildingsFeatures = indexBuildings.getAllFeatures();
			LanduseBuildingAnalysis.analyzeBuildingType(buildingsFeatures, buildingsPerZone,
				landuseCategoriesAndDataConnection, indexLanduse, indexZones);
		}
		Id<Link> newLink = null;
		for (int a = 0; newLink == null && a < buildingsPerZone.get(zone).get(selectedCategory).size() * 2; a++) {

			SimpleFeature possibleBuilding = buildingsPerZone.get(zone).get(selectedCategory)
				.get(rnd.nextInt(buildingsPerZone.get(zone).get(selectedCategory).size()));
			Coord centroidPointOfBuildingPolygon = MGC
				.point2Coord(((Geometry) possibleBuilding.getDefaultGeometry()).getCentroid());

			int numberOfPossibleLinks = regionLinksMap.get(zone).size();

			// searches and selects the nearest link of the possible links in this zone
			newLink = SmallScaleCommercialTrafficUtils.findNearestPossibleLink(zone, noPossibleLinks, regionLinksMap, newLink,
				centroidPointOfBuildingPolygon, numberOfPossibleLinks);
		}
		if (newLink == null)
			throw new RuntimeException("No possible link for buildings with type '" + selectedCategory + "' in zone '"
				+ zone + "' found. buildings in category: " + buildingsPerZone.get(zone).get(selectedCategory)
				+ "; possibleLinks in zone: " + regionLinksMap.get(zone).size());
		return newLink;
	}

	/**
	 * Filters links by used mode "car" and creates Map with all links in each zone
	 */
	static Map<String, Map<Id<Link>, Link>> filterLinksForZones(Scenario scenario, Index indexZones,
																	Map<String, Map<String, List<SimpleFeature>>> buildingsPerZone) throws URISyntaxException {
		Map<String, Map<Id<Link>, Link>> regionLinksMap = new HashMap<>();
		List<Link> links;
		log.info("Filtering and assign links to zones. This take some time...");

		String networkPath;
		if (scenario.getConfig().network().getInputFile().startsWith("https:"))
			networkPath = scenario.getConfig().network().getInputFile();
		else
			networkPath = scenario.getConfig().getContext().toURI().resolve(scenario.getConfig().network().getInputFile()).getPath();

		Network networkToChange = NetworkUtils.readNetwork(networkPath);
		NetworkUtils.runNetworkCleaner(networkToChange);

		CoordinateTransformation ct = indexZones.getShp().createTransformation(ProjectionUtils.getCRS(scenario.getNetwork()));

		links = networkToChange.getLinks().values().stream().filter(l -> l.getAllowedModes().contains("car"))
			.collect(Collectors.toList());
		links.forEach(l -> l.getAttributes().putAttribute("newCoord",
			CoordUtils.round(ct.transform(l.getCoord()))));
		links.forEach(l -> l.getAttributes().putAttribute("zone",
			indexZones.query((Coord) l.getAttributes().getAttribute("newCoord"))));
		links = links.stream().filter(l -> l.getAttributes().getAttribute("zone") != null).collect(Collectors.toList());
		links.forEach(l -> regionLinksMap
			.computeIfAbsent((String) l.getAttributes().getAttribute("zone"), (k) -> new HashMap<>())
			.put(l.getId(), l));
		if (regionLinksMap.size() != indexZones.size())
			findNearestLinkForZonesWithoutLinks(networkToChange, regionLinksMap, indexZones, buildingsPerZone);

		return regionLinksMap;
	}

	/**
	 * Finds for areas without links the nearest Link if the area contains any building.
	 */
	private static void findNearestLinkForZonesWithoutLinks(Network networkToChange, Map<String, Map<Id<Link>, Link>> regionLinksMap,
															Index shpZones,
															Map<String, Map<String, List<SimpleFeature>>> buildingsPerZone) {
		for (SimpleFeature singleArea : shpZones.getAllFeatures()) {
			String zoneID = (String) singleArea.getAttribute("areaID");
			if (!regionLinksMap.containsKey(zoneID) && buildingsPerZone.get(zoneID) != null) {
				for (List<SimpleFeature> buildingList : buildingsPerZone.get(zoneID).values()) {
					for (SimpleFeature building : buildingList) {
						Link l = NetworkUtils.getNearestLink(networkToChange,
							MGC.point2Coord(((Geometry) building.getDefaultGeometry()).getCentroid()));
                        assert l != null;
                        regionLinksMap
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
		String smallScaleCommercialTrafficType, Scenario scenario, Path output, Map<String, Map<Id<Link>, Link>> regionLinksMap)
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
				log.info("Create OD pair " + count + " of " + trafficVolume_start.size());

			String startZone = trafficVolumeKey.getZone();
			String modeORvehType = trafficVolumeKey.getModeORvehType();
			for (Integer purpose : trafficVolume_start.get(trafficVolumeKey).keySet()) {
				Collections.shuffle(listOfZones, rnd);
				for (String stopZone : listOfZones) {
					odMatrix.setTripDistributionValue(startZone, stopZone, modeORvehType, purpose, smallScaleCommercialTrafficType,
						network, regionLinksMap, resistanceFactor, shapeFileZoneNameColumn);
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
}
