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
import org.matsim.application.options.ShpOptions;
import org.matsim.application.options.ShpOptions.Index;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.controler.*;
import org.matsim.contrib.freight.usecases.chessboard.CarrierTravelDisutilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.vehicles.CostInformation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/** Tool to generate small scale commercial traffic for a selected area. The needed input data are: employee information for the area and three shapes files (zones, buildings, landuse). These data should be available with OSM.
 *
 * @author Ricardo Ewert
 *
 */
@CommandLine.Command(name = "generate-small-scale-commercial-traffic", description = "Generates plans for a small scale commercial traffic model", showDefaultValues = true)
public class CreateSmallScaleCommercialTrafficDemand implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CreateSmallScaleCommercialTrafficDemand.class);
	private static final HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone = new HashMap<>();
	private static Path shapeFileLandusePath = null;
	private static Path shapeFileZonePath = null;
	private static Path shapeFileBuildingsPath = null;
	private static final HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection = new HashMap<>();

	private enum CreationOption {
		useExistingCarrierFileWithSolution, createNewCarrierFile, useExistingCarrierFileWithoutSolution
	}

	private enum LanduseConfiguration {
		useOnlyOSMLanduse, useOSMBuildingsAndLanduse, useExistingDataDistribution
	}

	private enum TrafficType {
		businessTraffic, freightTraffic, commercialTraffic
	}

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to the freight data directory")
	private Path inputDataDirectory;

	@CommandLine.Option(names = "--sample", description = "Scaling factor of the freight traffic (0, 1)", required = true)
	private double sample;

	@CommandLine.Option(names = "--jspritIterations", description = "Set number of jsprit iterations", required = true)
	private int jspritIterations;

	@CommandLine.Option(names = "--creationOption", description = "Set option of mode differentiation:  useExistingCarrierFileWithSolution, createNewCarrierFile, useExistingCarrierFileWithoutSolution")
	private CreationOption usedCreationOption;

	@CommandLine.Option(names = "--landuseConfiguration", description = "Set option of used OSM data. Options: useOnlyOSMLanduse, useOSMBuildingsAndLanduse, useExistingDataDistribution")
	private LanduseConfiguration usedLanduseConfiguration;

	@CommandLine.Option(names = "--trafficType", description = "Select traffic type. Options: businessTraffic, freightTraffic, commercialTraffic (contains both types)")
	private TrafficType usedTrafficType;

	@CommandLine.Option(names = "--includeExistingModels", description = "If models for some segments exist they can be included.")
	private boolean includeExistingModels;

	@CommandLine.Option(names = "--zoneShapeFileName", description = "Path of the zone shape file.")
	private Path zoneShapeFilePath;

	@CommandLine.Option(names = "--buildingsShapeFileName", description = "Path of the buildings shape file")
	private Path buildingsShapeFilePath;

	@CommandLine.Option(names = "--landuseShapeFileName", description = "Path of the landuse shape file")
	private Path landuseShapeFilePath;

	@CommandLine.Option(names = "--shapeCRS", description = "CRS of the three input shape files (zones, landuse, buildings")
	private String shapeCRS;

	@CommandLine.Option(names = "--resistanceFactor", defaultValue = "0.005", description = "ResistanceFactor for the trip distribution")
	private double resistanceFactor;

	@CommandLine.Option(names = "--nameOutputPopulation", description = "Name of the output Population")
	private String nameOutputPopulation;

	@CommandLine.Option(names = "--PathOutput", description = "Path for the output")
	private Path output;

	private SplittableRandom rnd;

	public static void main(String[] args) {
		System.exit(new CommandLine(new CreateSmallScaleCommercialTrafficDemand()).execute(args));
	}

	@Override
	public Integer call() throws Exception {
		Configurator.setLevel("org.matsim.core.utils.geometry.geotools.MGC", Level.ERROR);

		String modelName = inputDataDirectory.getFileName().toString();

		String sampleName = SmallScaleCommercialTrafficUtils.getSampleNameOfOutputFolder(sample);

		Config config = readAndCheckConfig(inputDataDirectory, modelName, sampleName, output);

		output = Path.of(config.controler().getOutputDirectory());

		Scenario scenario = ScenarioUtils.loadScenario(config);
		String carriersFileLocation;
		FreightConfigGroup freightConfigGroup;
		switch (usedCreationOption) {
			case useExistingCarrierFileWithSolution -> {
				log.info("Existing carriers (including carrier vehicle types ) should be set in the freight config group");
				if (includeExistingModels)
					throw new Exception(
							"You set that existing models should included to the new model. This is only possible for a creation of the new carrier file and not by using an existing.");
				freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
				log.info("Load carriers from: " + freightConfigGroup.getCarriersFile());
				FreightUtils.loadCarriersAccordingToFreightConfig(scenario);
			}
			case useExistingCarrierFileWithoutSolution -> {
				log.info("Existing carriers (including carrier vehicle types ) should be set in the freight config group");
				if (includeExistingModels)
					throw new Exception(
							"You set that existing models should included to the new model. This is only possible for a creation of the new carrier file and not by using an existing.");
				freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
				log.info("Load carriers from: " + freightConfigGroup.getCarriersFile());
				FreightUtils.loadCarriersAccordingToFreightConfig(scenario);
				solveSeparatedVRPs(scenario, null);
			}
			default -> {
				shapeFileZonePath = inputDataDirectory.resolve(zoneShapeFilePath);
				shapeFileLandusePath = inputDataDirectory.resolve(landuseShapeFilePath);
				shapeFileBuildingsPath = inputDataDirectory.resolve(buildingsShapeFilePath);
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
				HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = LanduseBuildingAnalysis
						.createInputDataDistribution(output, landuseCategoriesAndDataConnection, inputDataDirectory,
								usedLanduseConfiguration.toString(), shapeFileLandusePath, shapeFileZonePath,
								shapeFileBuildingsPath, shapeCRS, buildingsPerZone);
				ShpOptions shpZones = new ShpOptions(shapeFileZonePath, shapeCRS, StandardCharsets.UTF_8);
				Map<String, HashMap<Id<Link>, Link>> regionLinksMap = filterLinksForZones(scenario, shpZones,
						SmallScaleCommercialTrafficUtils.getIndexZones(shapeFileZonePath, shapeCRS));
				switch (usedTrafficType) {
					case businessTraffic, freightTraffic ->
							createCarriersAndDemand(config, output, scenario, shpZones, resultingDataPerZone, regionLinksMap, usedTrafficType.toString(),
									inputDataDirectory, includeExistingModels);
					case commercialTraffic -> {
						createCarriersAndDemand(config, output, scenario, shpZones, resultingDataPerZone, regionLinksMap, "businessTraffic",
								inputDataDirectory, includeExistingModels);
						includeExistingModels = false; // because already included in the step before
						createCarriersAndDemand(config, output, scenario, shpZones, resultingDataPerZone, regionLinksMap, "freightTraffic",
								inputDataDirectory, includeExistingModels);
					}
					default -> throw new RuntimeException("No traffic type selected.");
				}
				if (config.controler().getRunId() == null)
					new CarrierPlanWriter(FreightUtils.addOrGetCarriers(scenario))
							.write(scenario.getConfig().controler().getOutputDirectory() + "/output_CarrierDemand.xml");
				else
					new CarrierPlanWriter(FreightUtils.addOrGetCarriers(scenario))
							.write(scenario.getConfig().controler().getOutputDirectory() + "/"
									+ scenario.getConfig().controler().getRunId() + ".output_CarrierDemand.xml");
				solveSeparatedVRPs(scenario, regionLinksMap);
			}
		}
		if (config.controler().getRunId() == null)
			new CarrierPlanWriter(FreightUtils.addOrGetCarriers(scenario)).write(
					scenario.getConfig().controler().getOutputDirectory() + "/output_CarrierDemandWithPlans.xml");
		else
			new CarrierPlanWriter(FreightUtils.addOrGetCarriers(scenario))
					.write(scenario.getConfig().controler().getOutputDirectory() + "/"+scenario.getConfig().controler().getRunId() + ".output_CarrierDemandWithPlans.xml");
		Controler controler = prepareControler(scenario);
		controler.run();
		SmallScaleCommercialTrafficUtils.createPlansBasedOnCarrierPlans(controler.getScenario(),
				usedTrafficType.toString(), output, modelName, sampleName, nameOutputPopulation);
		return 0;
	}

	/**
	 * @param originalScenario complete Scenario
	 * @param regionLinksMap list with Links for each region
	 */
	private void solveSeparatedVRPs(Scenario originalScenario, Map<String, HashMap<Id<Link>, Link>> regionLinksMap) throws Exception {

		boolean splitCarrier = true;
		boolean splitVRPs = false;
		int maxServicesPerCarrier = 100;
		Map<Id<Carrier>, Carrier> allCarriers = new HashMap<>(
				FreightUtils.getCarriers(originalScenario).getCarriers());
		Map<Id<Carrier>, Carrier> solvedCarriers = new HashMap<>();
		List<Id<Carrier>> keyList = new ArrayList<>(allCarriers.keySet());
		FreightUtils.getCarriers(originalScenario).getCarriers().values().forEach(carrier -> {
			if (CarrierUtils.getJspritIterations(carrier) == 0) {
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
								.round(carrier.getServices().size() / numberOfNewCarrier);

						int j = 0;
						while (j < numberOfNewCarrier) {

							int numberOfServicesForNewCarrier = numberOfServicesPerNewCarrier;
							int numberOfVehiclesForNewCarrier = numberOfServicesPerNewCarrier;
							if (j + 1 == numberOfNewCarrier) {
								numberOfServicesForNewCarrier = carrier.getServices().size() - countedServices;
								numberOfVehiclesForNewCarrier = carrier.getCarrierCapabilities().getCarrierVehicles()
										.size() - countedVehicles;
							}
							Carrier newCarrier = CarrierUtils.createCarrier(
									Id.create(carrier.getId().toString() + "_part_" + (j + 1), Carrier.class));
							CarrierCapabilities newCarrierCapabilities = CarrierCapabilities.Builder.newInstance()
									.setFleetSize(carrier.getCarrierCapabilities().getFleetSize()).build();
							newCarrierCapabilities.getCarrierVehicles()
									.putAll(carrier.getCarrierCapabilities().getCarrierVehicles());
							newCarrier.setCarrierCapabilities(newCarrierCapabilities);
							newCarrier.getServices().putAll(carrier.getServices());
							CarrierUtils.setJspritIterations(newCarrier, CarrierUtils.getJspritIterations(carrier));
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
			FreightUtils.getCarriers(originalScenario).getCarriers().clear();
			FreightUtils.getCarriers(originalScenario).getCarriers().putAll(subCarriers);
			log.info("Solving carriers " + (fromIndex + 1) + "-" + (toIndex) + " of all " + allCarriers.size()
					+ " carriers. This are " + subCarriers.size() + " VRP to solve.");
			FreightUtils.runJsprit(originalScenario);
			solvedCarriers.putAll(FreightUtils.getCarriers(originalScenario).getCarriers());
			FreightUtils.getCarriers(originalScenario).getCarriers().clear();
			if (!splitVRPs)
				break;
		}
		FreightUtils.getCarriers(originalScenario).getCarriers().putAll(solvedCarriers);
		FreightUtils.getCarriers(originalScenario).getCarriers().values().forEach(carrier -> {
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

	private void createCarriersAndDemand(Config config, Path output, Scenario scenario, ShpOptions shpZones,
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone,
			Map<String, HashMap<Id<Link>, Link>> regionLinksMap, String usedTrafficType, Path inputDataDirectory,
			boolean includeExistingModels) throws Exception {

		ArrayList<String> modesORvehTypes;
		if (usedTrafficType.equals("freightTraffic"))
			modesORvehTypes = new ArrayList<>(
					Arrays.asList("vehTyp1", "vehTyp2", "vehTyp3", "vehTyp4", "vehTyp5"));
		else if (usedTrafficType.equals("businessTraffic"))
			modesORvehTypes = new ArrayList<>(List.of("total"));
		else
			throw new Exception("Invalid traffic type selected!");

		TrafficVolumeGeneration.setInputParameters(usedTrafficType);

		HashMap<TrafficVolumeGeneration.TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_start = TrafficVolumeGeneration
				.createTrafficVolume_start(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);
		HashMap<TrafficVolumeGeneration.TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_stop = TrafficVolumeGeneration
				.createTrafficVolume_stop(resultingDataPerZone, output, sample, modesORvehTypes, usedTrafficType);

		if (includeExistingModels) {
			SmallScaleCommercialTrafficUtils.readExistingModels(scenario, sample, inputDataDirectory, regionLinksMap);
			TrafficVolumeGeneration.reduceDemandBasedOnExistingCarriers(scenario, regionLinksMap, usedTrafficType,
					trafficVolumePerTypeAndZone_start, trafficVolumePerTypeAndZone_stop);
		}
		final TripDistributionMatrix odMatrix = createTripDistribution(trafficVolumePerTypeAndZone_start,
				trafficVolumePerTypeAndZone_stop, shpZones, usedTrafficType, scenario, output, regionLinksMap);
		createCarriers(config, scenario, odMatrix, resultingDataPerZone, usedTrafficType, regionLinksMap);
	}

	/** Reads and checks config if all necessary parameter are set.
	 */
	private Config readAndCheckConfig(Path inputDataDirectory, String modelName, String sampleName, Path output) throws Exception {

		Config config = ConfigUtils.loadConfig(inputDataDirectory.resolve("config_demand.xml").toString());
		if (output == null)
			config.controler().setOutputDirectory(Path.of(config.controler().getOutputDirectory()).resolve(modelName)
				.resolve(usedTrafficType.toString() + "_" + sampleName + "pct" + "_"
						+ java.time.LocalDate.now() + "_" + java.time.LocalTime.now().toSecondOfDay() + "_" + resistanceFactor)
				.toString());
		else
			config.controler().setOutputDirectory(output.toString());
		new OutputDirectoryHierarchy(config.controler().getOutputDirectory(), config.controler().getRunId(),
				config.controler().getOverwriteFileSetting(), ControlerConfigGroup.CompressionType.gzip);
		new File(Path.of(config.controler().getOutputDirectory()).resolve("calculatedData").toString()).mkdir();
		rnd  = new SplittableRandom(config.global().getRandomSeed());
		if (config.network().getInputFile() == null)
			throw new Exception("No network file in config");
		if (config.network().getInputCRS() == null)
			throw new Exception("No network CRS is set in config");
		if (config.global().getCoordinateSystem() == null)
			throw new Exception("No global CRS is set in config");
		if (config.controler().getOutputDirectory() == null)
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
						new MyCarrierPlanStrategyManagerFactory(FreightUtils.getCarrierVehicleTypes(scenario)));
				bind(CarrierScoringFunctionFactory.class).toInstance(new MyCarrierScoringFunctionFactory());
			}
		});
		controler.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort);
		return controler;
	}

	/**
	 * Creates the carriers and the related demand, based on the generated
	 * TripDistributionMatrix.
	 */
	private void createCarriers(Config config, Scenario scenario, TripDistributionMatrix odMatrix,
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone, String trafficType,
			Map<String, HashMap<Id<Link>, Link>> regionLinksMap) {
		int maxNumberOfCarrier = odMatrix.getListOfPurposes().size() * odMatrix.getListOfZones().size()
				* odMatrix.getListOfModesOrVehTypes().size();
		int createdCarrier = 0;

		CarrierVehicleTypes carrierVehicleTypes = FreightUtils.getCarrierVehicleTypes(scenario);
		Map<Id<VehicleType>, VehicleType> additionalCarrierVehicleTypes = scenario.getVehicles().getVehicleTypes();
		additionalCarrierVehicleTypes.values().forEach(
				vehicleType -> carrierVehicleTypes.getVehicleTypes().putIfAbsent(vehicleType.getId(), vehicleType));

		for (VehicleType vehicleType : carrierVehicleTypes.getVehicleTypes().values()) {
			CostInformation costInformation = vehicleType.getCostInformation();
			VehicleUtils.setCostsPerSecondInService(costInformation, costInformation.getCostsPerSecond());
			VehicleUtils.setCostsPerSecondWaiting(costInformation, costInformation.getCostsPerSecond());
		}

		for (Integer purpose : odMatrix.getListOfPurposes()) {
			for (String startZone : odMatrix.getListOfZones()) {
				for (String modeORvehType : odMatrix.getListOfModesOrVehTypes()) {
					boolean isStartingLocation = false;
					checkIfIsStartingPosition: {
						for (String possibleStopZone : odMatrix.getListOfZones()) {
							if (!modeORvehType.equals("pt") && !modeORvehType.equals("op"))
								if (odMatrix.getTripDistributionValue(startZone, possibleStopZone, modeORvehType,
										purpose, trafficType) != 0) {
									isStartingLocation = true;
									break checkIfIsStartingPosition;
								}
						}
					}
					if (isStartingLocation) {
						double occupancyRate = 0;
						String[] possibleVehicleTypes = null;
						Integer serviceTimePerStop = null;
						ArrayList<String> startCategory = new ArrayList<>();
						ArrayList<String> stopCategory = new ArrayList<>();
						if (purpose == 1) {
							if (trafficType.equals("freightTraffic")) {
								occupancyRate = 1.;
							} else if (trafficType.equals("businessTraffic")) {
								possibleVehicleTypes = new String[] { "vwCaddy", "e_SpaceTourer"};
								serviceTimePerStop = (int) Math.round(71.7 * 60);
								occupancyRate = 1.5;
							}
							startCategory.add("Employee Secondary Sector Rest");
							stopCategory.add("Employee Secondary Sector Rest");
						} else if (purpose == 2) {
							if (trafficType.equals("freightTraffic")) {
								occupancyRate = 1.;
							} else if (trafficType.equals("businessTraffic")) {
								possibleVehicleTypes = new String[] { "vwCaddy", "e_SpaceTourer"};
								serviceTimePerStop = (int) Math.round(70.4 * 60); // Durschnitt aus Handel,Transp.,Einw.
								occupancyRate = 1.6;
							}
							startCategory.add("Employee Secondary Sector Rest");
							stopCategory.add("Employee Primary Sector");
							stopCategory.add("Employee Construction");
							stopCategory.add("Employee Secondary Sector Rest");
							stopCategory.add("Employee Retail");
							stopCategory.add("Employee Traffic/Parcels");
							stopCategory.add("Employee Tertiary Sector Rest");
							stopCategory.add("Inhabitants");
						} else if (purpose == 3) {
							if (trafficType.equals("freightTraffic")) {
								occupancyRate = 1.;
							} else if (trafficType.equals("businessTraffic")) {
								possibleVehicleTypes = new String[] { "golf1.4", "c_zero" };
								serviceTimePerStop = (int) Math.round(70.4 * 60);
								occupancyRate = 1.2;
							}
							startCategory.add("Employee Retail");
							startCategory.add("Employee Tertiary Sector Rest");
							stopCategory.add("Employee Primary Sector");
							stopCategory.add("Employee Construction");
							stopCategory.add("Employee Secondary Sector Rest");
							stopCategory.add("Employee Retail");
							stopCategory.add("Employee Traffic/Parcels");
							stopCategory.add("Employee Tertiary Sector Rest");
							stopCategory.add("Inhabitants");
						} else if (purpose == 4) {
							if (trafficType.equals("freightTraffic")) {
								occupancyRate = 1.;
							} else if (trafficType.equals("businessTraffic")) {
								possibleVehicleTypes = new String[] { "golf1.4", "c_zero" };
								serviceTimePerStop = (int) Math.round(100.6 * 60);
								occupancyRate = 1.2;
							}
							startCategory.add("Employee Traffic/Parcels");
							stopCategory.add("Employee Primary Sector");
							stopCategory.add("Employee Construction");
							stopCategory.add("Employee Secondary Sector Rest");
							stopCategory.add("Employee Retail");
							stopCategory.add("Employee Traffic/Parcels");
							stopCategory.add("Employee Tertiary Sector Rest");
							stopCategory.add("Inhabitants");
						} else if (purpose == 5) {
							if (trafficType.equals("freightTraffic")) {
								occupancyRate = 1.;
							} else if (trafficType.equals("businessTraffic")) {
								possibleVehicleTypes = new String[] { "mercedes313", "e_SpaceTourer" };
								serviceTimePerStop = (int) Math.round(214.7 * 60);
								occupancyRate = 1.7;
							}
							startCategory.add("Employee Construction");
							stopCategory.add("Employee Primary Sector");
							stopCategory.add("Employee Construction");
							stopCategory.add("Employee Secondary Sector Rest");
							stopCategory.add("Employee Retail");
							stopCategory.add("Employee Traffic/Parcels");
							stopCategory.add("Employee Tertiary Sector Rest");
							stopCategory.add("Inhabitants");
						} else if (purpose == 6) {
							occupancyRate = 1.;
							startCategory.add("Inhabitants");
							stopCategory.add("Employee Primary Sector");
							stopCategory.add("Employee Construction");
							stopCategory.add("Employee Secondary Sector Rest");
							stopCategory.add("Employee Retail");
							stopCategory.add("Employee Traffic/Parcels");
							stopCategory.add("Employee Tertiary Sector Rest");
							stopCategory.add("Inhabitants");
						}
						if (trafficType.equals("freightTraffic")) {
							switch (modeORvehType) {
								case "vehTyp1" -> {
									possibleVehicleTypes = new String[]{"vwCaddy", "e_SpaceTourer"}; // possible to add more types, see source
									serviceTimePerStop = Math.round(120 * 60);
								}
								case "vehTyp2" -> {
									possibleVehicleTypes = new String[]{"mercedes313", "e_SpaceTourer"};
									serviceTimePerStop = Math.round(150 * 60);
								}
								case "vehTyp3" -> {
									possibleVehicleTypes = new String[]{"light8t", "light8t_electro"};
									serviceTimePerStop = Math.round(120 * 60);
								}
								case "vehTyp4" -> {
									possibleVehicleTypes = new String[]{"light8t", "light8t_electro"};
									serviceTimePerStop = Math.round(75 * 60);
								}
								case "vehTyp5" -> {
									possibleVehicleTypes = new String[]{"medium18t", "medium18t_electro", "heavy40t", "heavy40t_electro"};
									serviceTimePerStop = Math.round(65 * 60);
								}
							}
						}

						// use only types of the possibleTypes which are in the given types file
						List<String> vehicleTypes = new ArrayList<>();

						assert possibleVehicleTypes != null;
						for (String possibleVehicleType : possibleVehicleTypes) {
							if (FreightUtils.getCarrierVehicleTypes(scenario).getVehicleTypes().containsKey(Id.create(possibleVehicleType, VehicleType.class)))
									vehicleTypes.add(possibleVehicleType);
						}
						String selectedStartCategory = startCategory.get(rnd.nextInt(startCategory.size()));
						while (resultingDataPerZone.get(startZone).getDouble(selectedStartCategory) == 0)
							selectedStartCategory = stopCategory.get(rnd.nextInt(stopCategory.size()));

						String carrierName = null;
						if (trafficType.equals("freightTraffic")) {
							carrierName = "Carrier_Freight_" + startZone + "_purpose_" + purpose + "_" + modeORvehType;
						} else if (trafficType.equals("businessTraffic"))
							carrierName = "Carrier_Business_" + startZone + "_purpose_" + purpose;
						int numberOfDepots = odMatrix.getSumOfServicesForStartZone(startZone, modeORvehType, purpose,
								trafficType);
						FleetSize fleetSize = FleetSize.FINITE;
						int fixedNumberOfVehiclePerTypeAndLocation = 1; //TODO possible improvement
						ArrayList<String> vehicleDepots = new ArrayList<>();
						createdCarrier++;
						log.info("Create carrier number " + createdCarrier + " of a maximum Number of "
								+ maxNumberOfCarrier + " carriers.");
						log.info("Carrier: " + carrierName + "; depots: " + numberOfDepots + "; services: "
								+ (int) Math.ceil(odMatrix.getSumOfServicesForStartZone(startZone, modeORvehType,
										purpose, trafficType) / occupancyRate));
						createNewCarrierAndAddVehicleTypes(scenario, purpose, startZone,
								selectedStartCategory, carrierName, vehicleTypes, numberOfDepots, fleetSize,
								fixedNumberOfVehiclePerTypeAndLocation, vehicleDepots, regionLinksMap, trafficType);
						log.info("Create services for carrier: " + carrierName);
						for (String stopZone : odMatrix.getListOfZones()) {
							int trafficVolumeForOD = Math.round(odMatrix.getTripDistributionValue(startZone,
									stopZone, modeORvehType, purpose, trafficType));
							int numberOfJobs = (int) Math.ceil(trafficVolumeForOD / occupancyRate);
							if (numberOfJobs == 0)
								continue;
							String selectedStopCategory = stopCategory.get(rnd.nextInt(stopCategory.size()));
							while (resultingDataPerZone.get(stopZone).getDouble(selectedStopCategory) == 0)
								selectedStopCategory = stopCategory.get(rnd.nextInt(stopCategory.size()));
							String[] serviceArea = new String[] { stopZone };
							TimeWindow serviceTimeWindow = TimeWindow.newInstance(6 * 3600, 20 * 3600);
							createServices(scenario, vehicleDepots, selectedStopCategory, carrierName,
									numberOfJobs, serviceArea, serviceTimePerStop, serviceTimeWindow, regionLinksMap);
						}
					}
				}
			}
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
								Map<String, HashMap<Id<Link>, Link>> regionLinksMap) {

		String stopZone = serviceArea[0];

		for (int i = 0; i < numberOfJobs; i++) {

			Id<Link> linkId = findPossibleLink(stopZone, selectedStopCategory, noPossibleLinks, regionLinksMap, shapeCRS);
			Id<CarrierService> idNewService = Id.create(carrierName + "_" + linkId + "_" + rnd.nextInt(10000),
					CarrierService.class);

			CarrierService thisService = CarrierService.Builder.newInstance(idNewService, linkId)
					.setServiceDuration(serviceTimePerStop).setServiceStartTimeWindow(serviceTimeWindow).build();
			FreightUtils.getCarriers(scenario).getCarriers().get(Id.create(carrierName, Carrier.class)).getServices()
					.put(thisService.getId(), thisService);
		}

	}

	/**
	 * Creates the carrier and the related vehicles.
	 */
	private void createNewCarrierAndAddVehicleTypes(Scenario scenario, Integer purpose, String startZone,
													String selectedStartCategory, String carrierName,
													List<String> vehicleTypes, int numberOfDepots, FleetSize fleetSize, int fixedNumberOfVehiclePerTypeAndLocation,
													ArrayList<String> vehicleDepots, Map<String, HashMap<Id<Link>, Link>> regionLinksMap, String trafficType) {

		Carriers carriers = FreightUtils.addOrGetCarriers(scenario);
		CarrierVehicleTypes carrierVehicleTypes = FreightUtils.getCarrierVehicleTypes(scenario);

		CarrierCapabilities carrierCapabilities;

		Carrier thisCarrier = CarrierUtils.createCarrier(Id.create(carrierName, Carrier.class));
		if (trafficType.equals("businessTraffic") && purpose == 3)
			thisCarrier.getAttributes().putAttribute("subpopulation", trafficType+"_service");
		else
			thisCarrier.getAttributes().putAttribute("subpopulation", trafficType);

		thisCarrier.getAttributes().putAttribute("purpose", purpose);
		thisCarrier.getAttributes().putAttribute("tourStartArea", startZone);
		if (jspritIterations > 0)
			CarrierUtils.setJspritIterations(thisCarrier, jspritIterations);
		carrierCapabilities = CarrierCapabilities.Builder.newInstance().setFleetSize(fleetSize).build();
		carriers.addCarrier(thisCarrier);

		while (vehicleDepots.size() < numberOfDepots) {
			Id<Link> link = findPossibleLink(startZone, selectedStartCategory, null, regionLinksMap, shapeCRS);
			vehicleDepots.add(link.toString());
		}
		for (String singleDepot : vehicleDepots) {
			int vehicleStartTime = rnd.nextInt(6 * 3600, 14 * 3600); // TODO Verteilung über den Tag prüfen
			int vehicleEndTime = vehicleStartTime + 8 * 3600;
			for (String thisVehicleType : vehicleTypes) {
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
	 * Finds a possible link for a service or the vehicle location.
	 */
	private Id<Link> findPossibleLink(String zone, String selectedCategory, ArrayList<String> noPossibleLinks,
			Map<String, HashMap<Id<Link>, Link>> regionLinksMap, String shapeCRS) {

		Index indexZones = SmallScaleCommercialTrafficUtils.getIndexZones(shapeFileZonePath, shapeCRS);

		if (buildingsPerZone.isEmpty()) {
			ShpOptions shpBuildings = new ShpOptions(shapeFileBuildingsPath, "EPSG:4326", StandardCharsets.UTF_8);
			List<SimpleFeature> buildingsFeatures = shpBuildings.readFeatures();
			LanduseBuildingAnalysis.analyzeBuildingType(buildingsFeatures, buildingsPerZone,
					landuseCategoriesAndDataConnection, shapeFileLandusePath, indexZones, shapeCRS);
		}
		Id<Link> newLink = null;
		for (int a = 0; newLink == null && a < buildingsPerZone.get(zone).get(selectedCategory).size() * 2; a++) {

			SimpleFeature possibleBuilding = buildingsPerZone.get(zone).get(selectedCategory)
					.get(rnd.nextInt(buildingsPerZone.get(zone).get(selectedCategory).size()));
			Coord centroidPointOfBuildingPolygon = MGC
					.point2Coord(((Geometry) possibleBuilding.getDefaultGeometry()).getCentroid());
			double minDistance = Double.MAX_VALUE;
			int numberOfPossibleLinks = regionLinksMap.get(zone).size();

			// searches and selects the nearest link of the possible links in this zone
			searchLink: for (Link possibleLink : regionLinksMap.get(zone).values()) {
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
		}
		if (newLink == null)
			throw new RuntimeException("No possible link for buildings with type '" + selectedCategory + "' in zone '"
					+ zone + "' found. buildings in category: " + buildingsPerZone.get(zone).get(selectedCategory)
					+ "; possibleLinks in zone: " + regionLinksMap.get(zone).size());
		return newLink;
	}

	/** Filters links by used mode "car" and creates Map with all links in each zone
	 */
	static Map<String, HashMap<Id<Link>, Link>> filterLinksForZones(Scenario scenario, ShpOptions shpZones, Index indexZones) throws URISyntaxException {
		Map<String, HashMap<Id<Link>, Link>> regionLinksMap = new HashMap<>();
		List<Link> links;
		log.info("Filtering and assign links to zones. This take some time...");

		String networkPath;
		if (scenario.getConfig().network().getInputFile().startsWith( "https:" ))
			networkPath = scenario.getConfig().network().getInputFile();
		else
			networkPath = scenario.getConfig().getContext().toURI().resolve(scenario.getConfig().network().getInputFile()).getPath();

		Network networkToChange = NetworkUtils.readNetwork(networkPath);
		links = networkToChange.getLinks().values().stream().filter(l -> l.getAllowedModes().contains("car"))
				.collect(Collectors.toList());
		links.forEach(l -> l.getAttributes().putAttribute("newCoord",
				shpZones.createTransformation(scenario.getConfig().network().getInputCRS()).transform(l.getCoord())));
		links.forEach(l -> l.getAttributes().putAttribute("zone",
				indexZones.query((Coord) l.getAttributes().getAttribute("newCoord"))));
		links = links.stream().filter(l -> l.getAttributes().getAttribute("zone") != null).collect(Collectors.toList());
		links.forEach(l -> regionLinksMap
				.computeIfAbsent((String) l.getAttributes().getAttribute("zone"), (k) -> new HashMap<>())
				.put(l.getId(), l));
		return regionLinksMap;
	}

	/**
	 * Creates the number of trips between the zones for each mode and purpose.
	 */
	private TripDistributionMatrix createTripDistribution(
			HashMap<TrafficVolumeGeneration.TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_start,
			HashMap<TrafficVolumeGeneration.TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_stop, ShpOptions shpZones,
			String usedTrafficType, Scenario scenario, Path output, Map<String, HashMap<Id<Link>, Link>> regionLinksMap)
			throws Exception {

		final TripDistributionMatrix odMatrix = TripDistributionMatrix.Builder
				.newInstance(shpZones, trafficVolume_start, trafficVolume_stop, usedTrafficType).build();
		ArrayList<String> listOfZones = new ArrayList<>();
		trafficVolume_start.forEach((k, v) -> {
			if (!listOfZones.contains(k.getZone()))
				listOfZones.add(k.getZone());
		});
		Network network = scenario.getNetwork();
		int count = 0;

		for (TrafficVolumeGeneration.TrafficVolumeKey trafficVolumeKey : trafficVolume_start.keySet()) {
			count++;
			if (count % 50 == 0 || count == 1)
				log.info("Create OD pair " + count + " of " + trafficVolume_start.size());

			String startZone = trafficVolumeKey.getZone();
			String modeORvehType = trafficVolumeKey.getModeORvehType();
			for (Integer purpose : trafficVolume_start.get(trafficVolumeKey).keySet()) {
				Collections.shuffle(listOfZones);
				for (String stopZone : listOfZones) {
					odMatrix.setTripDistributionValue(startZone, stopZone, modeORvehType, purpose, usedTrafficType,
							network, regionLinksMap, resistanceFactor);
				}
			}
		}
		odMatrix.clearRoundingError();
		odMatrix.writeODMatrices(output, usedTrafficType);
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
			final CarrierStrategyManager strategyManager = FreightUtils.createDefaultCarrierStrategyManager();
			strategyManager.setMaxPlansPerAgent(5);
			{
				GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>(
						new  ExpBetaPlanChanger.Factory<CarrierPlan,Carrier>().setBetaValue(1.0).build());

				strategyManager.addStrategy(strategy, null, 1.0);

			}
			{
				GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>(
						new KeepSelected<>());
				strategy.addStrategyModule(new CarrierTimeAllocationMutator.Factory().build());
				strategy.addStrategyModule( new
						CarrierReRouteVehicles.Factory(router, network, modeTravelTimes.get(TransportMode.car)).build());
				strategyManager.addStrategy(strategy, null, 0.5);
			}
			return strategyManager;
		}
	}

	static class DriversActivityScoring implements SumScoringFunction.BasicScoring, SumScoringFunction.ActivityScoring {

		// private static final Logger log =
		// Logger.getLogger(DriversActivityScoring.class);

		private double score;
		private final double timeParameter = 0.008;
		private final double missedTimeWindowPenalty = 0.01;

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
					double penalty_score = (-1) * (actStartTime - tw.getEnd()) * missedTimeWindowPenalty;
					if (!(penalty_score <= 0.0))
						throw new AssertionError("penalty score must be negative");
					// log.info("penalty " + penalty_score);
					score += penalty_score;

				}
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
				CarrierVehicle vehicle = CarrierUtils.getCarrierVehicle(carrier, vehicleId);
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
}
