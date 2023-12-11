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

package org.matsim.freightDemandGeneration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierPlanWriter;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.controler.CarrierModule;
import org.matsim.freight.carriers.controler.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.usecases.chessboard.CarrierScoringFunctionFactoryImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;

import javax.management.InvalidAttributeValueException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * The class generates a freight demand based on the selected input options and
 * the read input files. The format and column titles of both input csv should
 * not be changed. The format of the files can be found in the test input of the
 * package.
 *
 * @author: Ricardo Ewert
 */
@CommandLine.Command(name = "generate-freight-demand", description = "The class generates a freight demand based on the "
		+ " *          selected input options and the read input files. The format and "
		+ " *          column titles of the input csv should not be changed. The format of"
		+ " *          these files are given in the example project. See: TODO", showDefaultValues = true)
public class FreightDemandGeneration implements MATSimAppCommand {

	private enum CarrierInputOptions {
		readCarrierFile, createCarriersFromCSV, addCSVDataToExistingCarrierFileData
	}

	private enum DemandGenerationOptions {
		useDemandFromCarrierFile, createDemandFromCSV, createDemandFromCSVAndUsePopulation
	}

	private enum PopulationOptions {
		useHolePopulation, usePopulationInShape, useNoPopulation
	}

	private enum PopulationSamplingOption {
		createMoreLocations, increaseDemandOnLocation, noPopulationSampling
	}

	private enum OptionsOfVRPSolutions {
		runJspritAndMATSim, runJspritAndMATSimWithDistanceConstraint, runJsprit, runJspritWithDistanceConstraint,
		createNoSolutionAndOnlyWriteCarrierFile
	}

	private static final Logger log = LogManager.getLogger(FreightDemandGeneration.class);

	@CommandLine.Option(names = "--output", description = "Path to output folder", required = true)
	private Path outputLocation;

	@CommandLine.Option(names = "--carrierOption", description = "Set the choice of getting/creating carrier. Options: readCarrierFile, createCarriersFromCSV, addCSVDataToExistingCarrierFileData", required = true)
	private CarrierInputOptions selectedCarrierInputOption;

	@CommandLine.Option(names = "--demandOption", description = "Select the option of demand generation. Options: useDemandFromCarrierFile, createDemandFromCSV, createDemandFromCSVAndUsePopulation", required = true)
	private DemandGenerationOptions selectedDemandGenerationOption;

	@CommandLine.Option(names = "--populationOption", description = "Select the option of using the population. Options: useHolePopulation, usePopulationInShape, useNoPopulation", required = true)
	private PopulationOptions selectedPopulationOption;

	@CommandLine.Option(names = "--populationSamplingOption", description = "Select the option of sampling if using a population. Options: createMoreLocations, increaseDemandOnLocation, noPopulationSampling", required = true)
	private PopulationSamplingOption selectedPopulationSamplingOption;

	@CommandLine.Option(names = "--VRPSolutionsOption", description = "Select the option of solving the VRP. Options: runJspritAndMATSim, runJspritAndMATSimWithDistanceConstraint, runJsprit, runJspritWithDistanceConstraint, createNoSolutionAndOnlyWriteCarrierFile", required = true)
	private OptionsOfVRPSolutions selectedSolution;

	@CommandLine.Option(names = "--combineSimilarJobs", defaultValue = "false", description = "Select the option if created jobs of the same carrier with same location and time will be combined. Options: true, false", required = true)
	private String combineSimilarJobs;

	@CommandLine.Option(names = "--carrierFileLocation", description = "Path to the carrierFile.", defaultValue = "")
	private Path carrierFilePath;

	@CommandLine.Option(names = "--carrierVehicleFileLocation", description = "Path to the carrierVehicleFile.")
	private Path carrierVehicleFilePath;

	@CommandLine.Option(names = "--shapeFileLocation", description = "Path to the shape file.")
	private static Path shapeFilePath;

	@CommandLine.Option(names = "--shapeCRS", description = "CRS of the shape file.")
	private static String shapeCRS;

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions(shapeFilePath, shapeCRS, null);

	@CommandLine.Option(names = "--populationFileLocation", description = "Path to the population file.", defaultValue = "")
	private Path populationFilePath;

	@CommandLine.Option(names = "--populationCRS", description = "CRS of the input network (e.g.\"EPSG:31468\")")
	private String populationCRS;

	@CommandLine.Option(names = "--network", description = "Path to desired network file", defaultValue = "")
	private String networkPath;

	@CommandLine.Option(names = "--networkCRS", description = "CRS of the input network (e.g.\"EPSG:31468\")")
	private String networkCRS;

	@CommandLine.Option(names = "--networkChangeEvents", description = "Set path to desired networkChangeEvents file if you want to use network change Events", defaultValue = "")
	private Path networkChangeEventsPath;

	@CommandLine.Option(names = "--shapeCategory", description = "Column name in the shape file for the data connection in the csv files")
	private String shapeCategory;

	@CommandLine.Option(names = "--inputCarrierCSV", description = "Path to input carrier CSV, if you want to read it.")
	private Path csvCarrierPath;

	@CommandLine.Option(names = "--inputDemandCSV", description = "Path to input demand CSV, if you want to create a new demand based on the csv.")
	private Path csvDemandPath;

	@CommandLine.Option(names = "--populationSample", description = "Sample of the selected population.")
	private double sampleSizeInputPopulation;

	@CommandLine.Option(names = "--populationSamplingTo", description = "Set the sample of the generated demand.")
	private double upSamplePopulationTo;

	@CommandLine.Option(names = "--defaultJspritIterations", description = "Set the default number of jsprit iterations.")
	private int defaultJspritIterations;

	public static void main(String[] args) {
		System.exit(new CommandLine(new FreightDemandGeneration()).execute(args));
	}

	@Override
	public Integer call() throws IOException, InvalidAttributeValueException, ExecutionException, InterruptedException {

		String vehicleTypesFileLocation = carrierVehicleFilePath.toString();
		String carriersFileLocation = carrierFilePath.toString();
		String populationFile = populationFilePath.toString();
		CoordinateTransformation crsTransformationFromNetworkToShape = null;

		// create and prepare MATSim config
		outputLocation = outputLocation
				.resolve(java.time.LocalDate.now() + "_" + java.time.LocalTime.now().toSecondOfDay());
		int lastMATSimIteration = 0;

		Config config = prepareConfig(lastMATSimIteration, networkCRS);

		log.info("Starting class to create a freight scenario");

		// select network configurations
		String networkPathOfOtherNetwork = networkPath;
		String networkChangeEventsFilePath = networkChangeEventsPath.toString();
		setNetworkAndNetworkChangeEvents(config, networkPathOfOtherNetwork, networkChangeEventsFilePath);

		// load or create carrierVehicle
		log.info("Start creating carriers. Selected option: " + selectedCarrierInputOption);
		prepareVehicles(config, vehicleTypesFileLocation);

		// load or create carrier
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Collection<SimpleFeature> polygonsInShape = null;
		shp = new ShpOptions(shapeFilePath, shapeCRS, null);
		if (shp.isDefined()) {
			log.warn("Use of shpFile. Locations for the carriers and the demand only in shp: " + shp.getShapeFile());
			polygonsInShape = shp.readFeatures();
			crsTransformationFromNetworkToShape = shp.createTransformation(networkCRS);
		}
		log.info("Start creating carriers. Selected option: " + selectedCarrierInputOption);
		createCarrier(scenario, selectedCarrierInputOption, carriersFileLocation, csvCarrierPath, polygonsInShape,
				defaultJspritIterations, crsTransformationFromNetworkToShape);

		// create the demand
		log.info("Start creating the demand. Selected option: " + selectedCarrierInputOption);
		createDemand(selectedDemandGenerationOption, scenario, csvDemandPath, polygonsInShape, populationFile,
				selectedPopulationSamplingOption, selectedPopulationOption, Boolean.getBoolean(combineSimilarJobs),
				crsTransformationFromNetworkToShape);

		// prepare the VRP and get a solution
		Controler controler = prepareControler(scenario);
		FreightDemandGenerationUtils.createDemandLocationsFile(controler);
		solveSelectedSolution(selectedSolution, config, controler);

		// TODO analyze results

		log.info("Finished");
		return 0;
	}

	/**
	 * Deletes the existing output file and sets the number of the last iteration
	 *
	 * @param lastMATSimIteration
	 * @param coordinateSystem
	 * @return
	 */
	private Config prepareConfig(int lastMATSimIteration, String coordinateSystem) {
		Config config = ConfigUtils.createConfig();
//		ScenarioUtils.loadScenario(config);
		config.controller().setOutputDirectory(outputLocation.toString());
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		new OutputDirectoryHierarchy(config.controller().getOutputDirectory(), config.controller().getRunId(),
				config.controller().getOverwriteFileSetting(), ControllerConfigGroup.CompressionType.gzip);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.controller().setLastIteration(lastMATSimIteration);
		config.global().setRandomSeed(4177);
		config.global().setCoordinateSystem(coordinateSystem);
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightCarriersConfigGroup.setTravelTimeSliceWidth(1800);
		freightCarriersConfigGroup.setTimeWindowHandling(FreightCarriersConfigGroup.TimeWindowHandling.enforceBeginnings);

		return config;
	}

	/**
	 * Sets the network and the networkChangeEvents if they are available.
	 *
	 * @param config
	 * @param networkPathOfOtherNetwork
	 * @param networkChangeEventsFileLocation
	 * @throws RuntimeException
	 */
	private static void setNetworkAndNetworkChangeEvents(Config config, String networkPathOfOtherNetwork,
			String networkChangeEventsFileLocation) throws RuntimeException {

		if (networkPathOfOtherNetwork.equals(""))
			throw new RuntimeException("no correct network path network");
		else {
			config.network().setInputFile(networkPathOfOtherNetwork);
			log.info("The following input network is selected: imported network from " + networkPathOfOtherNetwork);
			if (networkChangeEventsFileLocation.equals(""))
				log.info("No networkChangeEvents selected");
			else {
				log.info("Setting networkChangeEventsInput file: " + networkChangeEventsFileLocation);
				config.network().setTimeVariantNetwork(true);
				config.network().setChangeEventsInputFile(networkChangeEventsFileLocation);
			}
		}
	}

	/**
	 * Reads the carrier vehicle file.
	 *
	 * @param config
	 * @param vehicleTypesFileLocation
	 */
	private static void prepareVehicles(Config config, String vehicleTypesFileLocation) {

		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		if (Objects.equals(vehicleTypesFileLocation, ""))
			throw new RuntimeException("No path to the vehicleTypes selected");
		else {
			freightCarriersConfigGroup.setCarriersVehicleTypesFile(vehicleTypesFileLocation);
			log.info("Get vehicleTypes from: " + vehicleTypesFileLocation);
		}
	}

	/**
	 * Differs between the different options of creating the carrier.
	 *
	 * @param scenario
	 * @param selectedCarrierInputOption
	 * @param carriersFileLocation
	 * @param csvLocationCarrier
	 * @param polygonsInShape
	 * @param defaultJspritIterations
	 * @param crsTransformationNetworkAndShape
	 * @throws IOException
	 */
	private void createCarrier(Scenario scenario, CarrierInputOptions selectedCarrierInputOption,
			String carriersFileLocation, Path csvLocationCarrier, Collection<SimpleFeature> polygonsInShape,
			int defaultJspritIterations, CoordinateTransformation crsTransformationNetworkAndShape) throws IOException {

		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(),
				FreightCarriersConfigGroup.class);
		switch (selectedCarrierInputOption) {
			case addCSVDataToExistingCarrierFileData -> {
				// reads an existing carrier file and adds the information based on the read csv
				// carrier file
				if (Objects.equals(carriersFileLocation, ""))
					throw new RuntimeException("No path to the carrier file selected");
				else {
					freightCarriersConfigGroup.setCarriersFile(carriersFileLocation);
					CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
					log.info("Load carriers from: " + carriersFileLocation);
					CarrierReaderFromCSV.readAndCreateCarrierFromCSV(scenario, freightCarriersConfigGroup, csvLocationCarrier,
							polygonsInShape, defaultJspritIterations, crsTransformationNetworkAndShape, shapeCategory);
				}
			}
			case readCarrierFile -> {
				// reads only a carrier file as the carrier import.
				if (Objects.equals(carriersFileLocation, ""))
					throw new RuntimeException("No path to the carrier file selected");
				else {
					freightCarriersConfigGroup.setCarriersFile(carriersFileLocation);
					CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
					log.info("Load carriers from: " + carriersFileLocation);
				}
			}
			case createCarriersFromCSV ->
				// creates all carriers based on the given information in the read carrier csv
					CarrierReaderFromCSV.readAndCreateCarrierFromCSV(scenario, freightCarriersConfigGroup, csvLocationCarrier,
							polygonsInShape, defaultJspritIterations, crsTransformationNetworkAndShape, shapeCategory);
			default -> throw new RuntimeException("no method to create or read carrier selected.");
		}
	}

	/**
	 * Differs between the different options of creating the demand..
	 *
	 * @param selectedDemandGenerationOption
	 * @param scenario
	 * @param csvLocationDemand
	 * @param polygonsInShape
	 * @param populationFile
	 * @param selectedSamplingOption
	 * @param selectedPopulationOption
	 * @param combineSimilarJobs
	 * @param crsTransformationNetworkAndShape
	 * @throws IOException
	 */
	private void createDemand(DemandGenerationOptions selectedDemandGenerationOption, Scenario scenario,
							  Path csvLocationDemand, Collection<SimpleFeature> polygonsInShape, String populationFile,
			PopulationSamplingOption selectedSamplingOption, PopulationOptions selectedPopulationOption,
			boolean combineSimilarJobs, CoordinateTransformation crsTransformationNetworkAndShape) throws IOException {

		switch (selectedDemandGenerationOption) {
			case createDemandFromCSV ->
				// creates the demand by using the information given in the read csv file
					DemandReaderFromCSV.readAndCreateDemand(scenario, csvLocationDemand, polygonsInShape, combineSimilarJobs,
							crsTransformationNetworkAndShape, null, shapeCategory);
			case createDemandFromCSVAndUsePopulation -> {
				/*
				 * Option creates the demand by using the information given in the read csv file
				 * and uses a population for finding demand locations
				 */
				Population population = PopulationUtils.readPopulation(populationFile);
				switch (selectedSamplingOption) {
					/*
					 * this option is important if the sample of the population and the sample of
					 * the resulting demand is different. For example, you can create with a 10pct
					 * sample a 100pct demand modal for the waste collection.
					 */
					case createMoreLocations ->
						/*
						 * If the demand sample is higher than the population sample, more demand
						 * location are created related to the given share of persons of the population
						 * with this demand.
						 */
							FreightDemandGenerationUtils.preparePopulation(population, sampleSizeInputPopulation,
									upSamplePopulationTo, "changeNumberOfLocationsWithDemand");
					case increaseDemandOnLocation ->
						/*
						 * If the demand sample is higher than the population sample, the demand per
						 * person will be increased.
						 */
							FreightDemandGenerationUtils.preparePopulation(population, sampleSizeInputPopulation,
									upSamplePopulationTo, "changeDemandOnLocation");
					default -> throw new RuntimeException("No valid sampling option selected!");
				}
				switch (selectedPopulationOption) {
					case useNoPopulation:
						break;
					case useHolePopulation:
						// uses the hole population as possible demand locations
						DemandReaderFromCSV.readAndCreateDemand(scenario, csvLocationDemand, polygonsInShape,
								combineSimilarJobs, crsTransformationNetworkAndShape, population, shapeCategory);
						break;
					case usePopulationInShape:
						// uses only the population with home location in the given shape file
						FreightDemandGenerationUtils.reducePopulationToShapeArea(population,
								shp.createIndex(populationCRS, "_"));
						DemandReaderFromCSV.readAndCreateDemand(scenario, csvLocationDemand, polygonsInShape,
								combineSimilarJobs, crsTransformationNetworkAndShape, population, shapeCategory);
						break;
					default:
						throw new RuntimeException("No valid population option selected!");
				}
			}
			case useDemandFromCarrierFile -> {
				// use only the given demand of the read carrier file
				boolean oneCarrierHasJobs = false;
				for (Carrier carrier : CarriersUtils.getCarriers(scenario).getCarriers().values())
					if (carrier.getServices().isEmpty() && carrier.getShipments().isEmpty())
						log.warn(carrier.getId().toString() + " has no jobs which can be used");
					else {
						oneCarrierHasJobs = true;
						log.info("Used the demand of the carrier " + carrier.getId().toString() + " from the carrierFile!");
					}
				if (!oneCarrierHasJobs)
					throw new RuntimeException("Minimum one carrier has no jobs");
			}
			default -> throw new RuntimeException("No valid demand generation option selected!");
		}
	}

	/**
	 * Prepares the controller.
	 *
	 * @param scenario
	 * @return
	 */
	private static Controler prepareControler(Scenario scenario) {
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new CarrierModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(CarrierScoringFunctionFactory.class).to(CarrierScoringFunctionFactoryImpl.class);
		}});

		return controler;
	}

	/**
	 * Differs between the different options for solving the VRP problem.
	 *
	 * @param selectedSolution
	 * @param config
	 * @param controler
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private static void solveSelectedSolution(OptionsOfVRPSolutions selectedSolution, Config config,
			Controler controler) throws ExecutionException, InterruptedException {
		switch (selectedSolution) {
			case runJspritAndMATSim -> {
				// solves the VRP with jsprit and runs MATSim afterwards
				new CarrierPlanWriter((Carriers) controler.getScenario().getScenarioElement("carriers"))
						.write(config.controller().getOutputDirectory() + "/output_carriersNoPlans.xml");
				runJsprit(controler, false);
				controler.run();
				new CarrierPlanWriter((Carriers) controler.getScenario().getScenarioElement("carriers"))
						.write(config.controller().getOutputDirectory() + "/output_carriersWithPlans.xml");
			}
			case runJspritAndMATSimWithDistanceConstraint -> {
				// solves the VRP with jsprit by using the distance constraint and runs MATSim
				// afterwards
				new CarrierPlanWriter((Carriers) controler.getScenario().getScenarioElement("carriers"))
						.write(config.controller().getOutputDirectory() + "/output_carriersNoPlans.xml");
				runJsprit(controler, true);
				controler.run();
				new CarrierPlanWriter((Carriers) controler.getScenario().getScenarioElement("carriers"))
						.write(config.controller().getOutputDirectory() + "/output_carriersWithPlans.xml");
			}
			case runJsprit -> {
				// solves only the VRP with jsprit
				new CarrierPlanWriter((Carriers) controler.getScenario().getScenarioElement("carriers"))
						.write(config.controller().getOutputDirectory() + "/output_carriersNoPlans.xml");
				runJsprit(controler, false);
				new CarrierPlanWriter((Carriers) controler.getScenario().getScenarioElement("carriers"))
						.write(config.controller().getOutputDirectory() + "/output_carriersWithPlans.xml");
				log.warn(
						"##Finished with the jsprit solution. If you also want to run MATSim, please change  case of optionsOfVRPSolutions");
				System.exit(0);
			}
			case runJspritWithDistanceConstraint -> {
				// solves only the VRP with jsprit by using the distance constraint
				new CarrierPlanWriter((Carriers) controler.getScenario().getScenarioElement("carriers"))
						.write(config.controller().getOutputDirectory() + "/output_carriersNoPlans.xml");
				runJsprit(controler, true);
				new CarrierPlanWriter((Carriers) controler.getScenario().getScenarioElement("carriers"))
						.write(config.controller().getOutputDirectory() + "/output_carriersWithPlans.xml");
				log.warn(
						"##Finished with the jsprit solution. If you also want to run MATSim, please change  case of optionsOfVRPSolutions");
				System.exit(0);
			}
			case createNoSolutionAndOnlyWriteCarrierFile -> {
				// creates no solution of the VRP and only writes the carrier file with the
				// generated carriers and demands
				new CarrierPlanWriter((Carriers) controler.getScenario().getScenarioElement("carriers"))
						.write(config.controller().getOutputDirectory() + "/output_carriersNoPlans.xml");
				log.warn(
						"##Finished without solution of the VRP. If you also want to run jsprit and/or MATSim, please change case of optionsOfVRPSolutions");
				System.exit(0);
			}
			default -> {
			}
		}
	}

	/**
	 * Runs jsprit.
	 *
	 * @param controler
	 * @param usingRangeRestriction
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private static void runJsprit(Controler controler, boolean usingRangeRestriction)
			throws ExecutionException, InterruptedException {
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(controler.getConfig(),
				FreightCarriersConfigGroup.class);
		if (usingRangeRestriction)
			freightCarriersConfigGroup.setUseDistanceConstraintForTourPlanning(
					FreightCarriersConfigGroup.UseDistanceConstraintForTourPlanning.basedOnEnergyConsumption);
		CarriersUtils.runJsprit(controler.getScenario());
	}
}
