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

package org.matsim.freightDemandGeneration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.*;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.controller.CarrierModule;
import org.matsim.freight.carriers.controller.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.usecases.chessboard.CarrierScoringFunctionFactoryImpl;
import picocli.CommandLine;

import javax.management.InvalidAttributeValueException;
import java.io.IOException;
import java.nio.file.Path;
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

	private final DemandGenerationSpecification demandGenerationSpecification;

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

	protected enum TotalDemandGenerationsOption {
		demandPerPerson, demandForShape, NoSelection
	}

	protected enum DemandDistributionOption {
		toRandomLinks, toRandomPersons, toPersonsByAge, NoSelection
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
	private String populationFilePath;

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

	public FreightDemandGeneration() {
		this.demandGenerationSpecification = new DefaultDemandGenerationSpecification();
		log.info("Using default {} for job duration calculation", demandGenerationSpecification.getClass().getSimpleName());
	}

	public FreightDemandGeneration(DemandGenerationSpecification demandGenerationSpecification) {
		this.demandGenerationSpecification = demandGenerationSpecification;
		log.info("Using {} for job duration calculation", demandGenerationSpecification.getClass().getSimpleName());
	}

	public static void main(String[] args) {
		System.exit(new CommandLine(new FreightDemandGeneration()).execute(args));
	}

	@Override
	public Integer call() throws IOException, InvalidAttributeValueException, ExecutionException, InterruptedException {

		String vehicleTypesFileLocation = carrierVehicleFilePath.toString();
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
		log.info("Start creating carriers. Selected option: {}", selectedCarrierInputOption);
		prepareVehicles(config, vehicleTypesFileLocation);

		// load or create carrier
		Scenario scenario = ScenarioUtils.loadScenario(config);

		ShpOptions.Index indexShape = null;
		shp = new ShpOptions(shapeFilePath, shapeCRS, null);
		if (shp.isDefined()) {
			log.warn("Use of shpFile. Locations for the carriers and the demand only in shp: {}", shp.getShapeFile());
			indexShape = shp.createIndex(shapeCategory);
			crsTransformationFromNetworkToShape = shp.createTransformation(networkCRS);
		}
		log.info("Start creating carriers. Selected option: {}", selectedCarrierInputOption);
		createCarrier(scenario, selectedCarrierInputOption, csvCarrierPath, indexShape,
			defaultJspritIterations, crsTransformationFromNetworkToShape);

		// create the demand
		log.info("Start creating the demand. Selected option: {}", selectedCarrierInputOption);
		createDemand(selectedDemandGenerationOption, scenario, csvDemandPath, indexShape, populationFilePath,
			selectedPopulationSamplingOption, selectedPopulationOption, Boolean.parseBoolean(combineSimilarJobs),
			crsTransformationFromNetworkToShape);

		// prepare the VRP and get a solution
		Controller controller = prepareController(scenario);
		demandGenerationSpecification.writeAdditionalOutputFiles(controller);

		solveSelectedSolution(selectedSolution, config, controller);

		log.info("Finished");
		return 0;
	}

	/**
	 * Deletes the existing output file and sets the number of the last iteration
	 *
	 * @param lastMATSimIteration 	the last iteration of MATSim
	 * @param coordinateSystem   	global coordinate system
	 * @return 						Config
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
		if (carrierFilePath != null)
			freightCarriersConfigGroup.setCarriersFile(carrierFilePath.toString());
		OutputDirectoryLogging.initLogging(new OutputDirectoryHierarchy(config));
		return config;
	}

	/**
	 * Sets the network and the networkChangeEvents if they are available.
	 *
	 * @param config 							Config
	 * @param networkPathOfOtherNetwork 		path to the network
	 * @param networkChangeEventsFileLocation 	path to the networkChangeEvents
	 * @throws RuntimeException 				if the networkPathOfOtherNetwork is empty
	 */
	private static void setNetworkAndNetworkChangeEvents(Config config, String networkPathOfOtherNetwork,
														 String networkChangeEventsFileLocation) throws RuntimeException {

		if (networkPathOfOtherNetwork.isEmpty())
			throw new RuntimeException("no correct network path network");
		else {
			config.network().setInputFile(networkPathOfOtherNetwork);
			log.info("The following input network is selected: imported network from {}", networkPathOfOtherNetwork);
			if (networkChangeEventsFileLocation.isEmpty())
				log.info("No networkChangeEvents selected");
			else {
				log.info("Setting networkChangeEventsInput file: {}", networkChangeEventsFileLocation);
				config.network().setTimeVariantNetwork(true);
				config.network().setChangeEventsInputFile(networkChangeEventsFileLocation);
			}
		}
	}

	/**
	 * Reads the carrier vehicle file.
	 *
	 * @param config 					Config
	 * @param vehicleTypesFileLocation 	path to the vehicleTypes
	 */
	private static void prepareVehicles(Config config, String vehicleTypesFileLocation) {

		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		if (Objects.equals(vehicleTypesFileLocation, ""))
			throw new RuntimeException("No path to the vehicleTypes selected");
		else {
			freightCarriersConfigGroup.setCarriersVehicleTypesFile(vehicleTypesFileLocation);
			log.info("Get vehicleTypes from: {}", vehicleTypesFileLocation);
		}
	}

	/**
	 * Differs between the different options of creating the carrier.
	 *
	 * @param scenario 							Scenario
	 * @param selectedCarrierInputOption 		selected carrier input option
	 * @param csvLocationCarrier 				path to the carrier csv
	 * @param indexShape 						shape index of the shape file
	 * @param defaultJspritIterations 			default number of jsprit iterations
	 * @param crsTransformationNetworkAndShape 	transformation of the network and shape
	 * @throws IOException 						if the carrier file is not found
	 */
	private void createCarrier(Scenario scenario, CarrierInputOptions selectedCarrierInputOption,
							   Path csvLocationCarrier, ShpOptions.Index indexShape,
							   int defaultJspritIterations, CoordinateTransformation crsTransformationNetworkAndShape) throws IOException {

		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(),
			FreightCarriersConfigGroup.class);
		switch (selectedCarrierInputOption) {
			case addCSVDataToExistingCarrierFileData -> {
				// reads an existing carrier file and adds the information based on the read csv
				// carrier file
				if (freightCarriersConfigGroup.getCarriersFile() == null)
					throw new RuntimeException("No path to the carrier file selected");
				else {
					CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
					log.info("Load carriers from: {}", freightCarriersConfigGroup.getCarriersFile());
					CarrierReaderFromCSV.readAndCreateCarrierFromCSV(scenario, freightCarriersConfigGroup, csvLocationCarrier,
						indexShape, defaultJspritIterations, crsTransformationNetworkAndShape, shapeCategory);
				}
			}
			case readCarrierFile -> {
				// reads only a carrier file as the carrier import.
				if (freightCarriersConfigGroup.getCarriersFile() == null)
					throw new RuntimeException("No path to the carrier file selected");
				else {
					CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
					log.info("Load carriers from: {}", freightCarriersConfigGroup.getCarriersFile());
				}
			}
			case createCarriersFromCSV ->
				// creates all carriers based on the given information in the read carrier csv
				CarrierReaderFromCSV.readAndCreateCarrierFromCSV(scenario, freightCarriersConfigGroup, csvLocationCarrier,
					indexShape, defaultJspritIterations, crsTransformationNetworkAndShape, shapeCategory);
			default -> throw new RuntimeException("no method to create or read carrier selected.");
		}
	}

	/**
	 * Differs between the different options of creating the demand.
	 *
	 * @param selectedDemandGenerationOption 	selected demand generation option
	 * @param scenario 							Scenario
	 * @param csvLocationDemand 				path to the demand csv
	 * @param indexShape 						shape index of the shape file
	 * @param populationFilePath 				path to the population file
	 * @param selectedSamplingOption 			selected population sampling option
	 * @param selectedPopulationOption 			selected population option
	 * @param combineSimilarJobs 				boolean if the jobs of the same carrier with same location and time will be combined
	 * @param crsTransformationNetworkAndShape 	transformation of the network and shape
	 * @throws IOException 						if the demand file is not found
	 */
	private void createDemand(DemandGenerationOptions selectedDemandGenerationOption, Scenario scenario,
							  Path csvLocationDemand, ShpOptions.Index indexShape, String populationFilePath,
							  PopulationSamplingOption selectedSamplingOption, PopulationOptions selectedPopulationOption,
							  boolean combineSimilarJobs, CoordinateTransformation crsTransformationNetworkAndShape) throws IOException {

		switch (selectedDemandGenerationOption) {
			case createDemandFromCSV ->
				// creates the demand by using the information given in the read csv file
					DemandReaderFromCSV.readAndCreateDemand(scenario, csvLocationDemand, indexShape, combineSimilarJobs,
							crsTransformationNetworkAndShape, null, shapeCategory, demandGenerationSpecification);
			case createDemandFromCSVAndUsePopulation -> {
				/*
				 * Option creates the demand by using the information given in the read csv file
				 * and uses a population for finding demand locations
				 */
				Population population = PopulationUtils.readPopulation(populationFilePath);
				switch (selectedSamplingOption) {
					/*
					 * This option is important if the sample of the population and the sample of
					 * the resulting demand is different. For example, you can create with a 10pct
					 * sample a 100pct demand modal for the waste collection.
					 */
					case createMoreLocations ->
						/*
						 * If the demand sample is higher than the population sample, more demand
						 * locations are created related to the given share of persons in the population
						 * with this demand.
						 */
						FreightDemandGenerationUtils.preparePopulation(population, sampleSizeInputPopulation,
							upSamplePopulationTo, "changeNumberOfLocationsWithDemand");
					case increaseDemandOnLocation -> {
						/*
						 * If the demand sample is higher than the population sample, the demand per
						 * person will be increased.
						 */
						log.warn("You have selected the option to increase the demand on the location. "
							+ "Because the simulation always uses the given demand the results are similar to the option with the same sample size.");
						FreightDemandGenerationUtils.preparePopulation(population, sampleSizeInputPopulation,
							upSamplePopulationTo, "changeDemandOnLocation");
					}
					case noPopulationSampling ->
						/*
						 * If the demand sample is equal to the population sample, the demand is created
						 * based on the given population and the set input population sampleSize
						 */
						FreightDemandGenerationUtils.preparePopulation(population, sampleSizeInputPopulation,
							sampleSizeInputPopulation, "noPopulationSampling");
					default -> throw new RuntimeException("No valid sampling option selected!");
				}
				switch (selectedPopulationOption) {
					case useNoPopulation:
						break;
					case useHolePopulation:
						// uses the whole population as possible demand locations
						//ERROR2
						if(population != null && shapeCategory != null )
							log.warn("Population isn't reduced to shapefile even though shapefile is defined. This might lead to errors when no areas or locations for pickup or delivery are defined.");
						DemandReaderFromCSV.readAndCreateDemand(scenario, csvLocationDemand, indexShape,
								combineSimilarJobs, crsTransformationNetworkAndShape, population, shapeCategory, demandGenerationSpecification);
						break;
					case usePopulationInShape:
						// uses only the population with home location in the given shape file
						FreightDemandGenerationUtils.reducePopulationToShapeArea(population,
							shp.createIndex(populationCRS, "_"));
						DemandReaderFromCSV.readAndCreateDemand(scenario, csvLocationDemand, indexShape,
								combineSimilarJobs, crsTransformationNetworkAndShape, population, shapeCategory, demandGenerationSpecification);
						break;
					default:
						throw new RuntimeException("No valid population option selected!");
				}
			}
			case useDemandFromCarrierFile -> {
				// use only the given demand of the read carrier file
				boolean oneCarrierHasJobs = false;
				for (Carrier carrier : CarriersUtils.getCarriers(scenario).getCarriers().values())
					if (!CarriersUtils.hasJobs(carrier))
						log.warn("{} has no jobs which can be used", carrier.getId().toString());
					else {
						oneCarrierHasJobs = true;
						log.info("Used the demand of the carrier {} from the carrierFile!", carrier.getId().toString());
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
	 * @param scenario 	Scenario
	 * @return 			Controller
	 */
	private static Controller prepareController(Scenario scenario) {
		Controller controller = ControllerUtils.createController(scenario);
		controller.addOverridingModule(new CarrierModule());
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(CarrierScoringFunctionFactory.class).to(CarrierScoringFunctionFactoryImpl.class);
			}});

		return controller;
	}

	/**
	 * Differs between the different options for solving the VRP problem.
	 *
	 * @param selectedSolution 			selected solution option
	 * @param config 					Config
	 * @param controller 				Controller
	 * @throws ExecutionException 		if the execution of the jsprit fails
	 * @throws InterruptedException 	if the execution of the jsprit is interrupted
	 */
	private static void solveSelectedSolution(OptionsOfVRPSolutions selectedSolution, Config config,
			Controller controller) throws ExecutionException, InterruptedException {
		CarriersUtils.writeCarriers(controller.getScenario(), "output_carriersNoPlans.xml");

		if (Objects.requireNonNull(selectedSolution) == OptionsOfVRPSolutions.createNoSolutionAndOnlyWriteCarrierFile) {
			log.warn(
				"##Finished without solution of the VRP. If you also want to run jsprit and/or MATSim, please change case of optionsOfVRPSolutions");
			System.exit(0);
		}
		boolean runMatSim = false;
		switch (selectedSolution) {
			case runJspritAndMATSim, runJspritAndMATSimWithDistanceConstraint -> runMatSim = true;
		}
		boolean useDistanceConstraint = false;
		switch (selectedSolution) {
			case runJspritWithDistanceConstraint, runJspritAndMATSimWithDistanceConstraint -> useDistanceConstraint = true;
		}
		runJsprit(controller, useDistanceConstraint);
		if (runMatSim)
			controller.run();
		else
			log.warn(
					"##Finished with the jsprit solution. If you also want to run MATSim, please change  case of optionsOfVRPSolutions");
		CarriersUtils.writeCarriers(controller.getScenario(), "output_carriersWithPlans.xml");
	}

	/**
	 * Runs jsprit.
	 *
	 * @param controller 				Controller
	 * @param usingRangeRestriction 	boolean if the range restriction is used
	 * @throws ExecutionException 		if the execution of the jsprit fails
	 * @throws InterruptedException 	if the execution of the jsprit is interrupted
	 */
	private static void runJsprit(Controller controller, boolean usingRangeRestriction)
			throws ExecutionException, InterruptedException {
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(controller.getConfig(),
				FreightCarriersConfigGroup.class);
		if (usingRangeRestriction)
			freightCarriersConfigGroup.setUseDistanceConstraintForTourPlanning(
					FreightCarriersConfigGroup.UseDistanceConstraintForTourPlanning.basedOnEnergyConsumption);
		CarriersUtils.runJsprit(controller.getScenario());
	}
}
