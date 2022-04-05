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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.management.InvalidAttributeValueException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.freight.Freight;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import picocli.CommandLine;

/**
 * @author: Ricardo Ewert. The class generates a freight demand based on the
 *          selected input options and the read input files. The format and
 *          column titles of the input csv should not be changed. The format of
 *          these files are given in the example project. See: TODO
 */
@CommandLine.Command(name = "generate-freight-demand", description = "The class generates a freight demand based on the\r\n"
		+ " *          selected input options and the read input files. The format and\r\n"
		+ " *          column titles of the input csv should not be changed. The format of\r\n"
		+ " *          these files are given in the example project. See: TODO", showDefaultValues = true)
public class FreightDemandGeneration implements Callable<Integer> {

	private enum CarrierInputOptions {
		readCarrierFile, createCarriersFromCSV, addCSVDataToExistingCarrierFileData
	}

	private enum DemandGenerationOptions {
		useDemandFromCarrierFile, createDemandFromCSV, createDemandFromCSVAndUsePopulation
	}

	private enum PopulationOptions {
		usePopulationHolePopulation, usePopulationInShape
	}

	private enum PopulationSamplingOption {
		createMoreLocations, increaseDemandOnLocation
	}

	private enum OptionsOfVRPSolutions {
		runJspritAndMATSim, runJspritAndMATSimWithDistanceConstraint, runJsprit, runJspritWithDistanceConstraint,
		createNoSolutionAndOnlyWriteCarrierFile
	}

	private static final Logger log = LogManager.getLogger(FreightDemandGeneration.class);

	@CommandLine.Option(names = "--carrierFileLocation", defaultValue = "../../../public-svn/matsim/scenarios/countries/de/freight-demand-generation/input_example/carrier_berlin_noDemand.xml", description = "Path to the carrierFile.")
	private static Path carrierFilePath;

	@CommandLine.Option(names = "--carrierVehicleFileLocation", defaultValue = "../../../public-svn/matsim/scenarios/countries/de/freight-demand-generation/input_example/vehicleTypes_default.xml", description = "Path to the carrierVehcileFile.")
	private static Path carrierVehicleFilePath;

	@CommandLine.Option(names = "--shapeFileLocation", defaultValue = "../../../public-svn/matsim/scenarios/countries/de/freight-demand-generation/input_example/shp/Berlin_Ortsteile.shp", description = "Path to the shape file.")
	private static Path shapeFilePath;

	@CommandLine.Option(names = "--shapeCRS", defaultValue = "EPSG:3857", description = "CRS of the shape file.")
	private static String shapeCRS;

	@CommandLine.Option(names = "--populationFileLocation", defaultValue = "../../../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-1pct/input/berlin-v5.5-1pct.plans.xml.gz", description = "Path to the population file.")
	private static Path populationFilePath;

	@CommandLine.Option(names = "--network", defaultValue = "../../../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz", description = "Path to desired network file")
	private static Path networkPath;

	@CommandLine.Option(names = "--networkCRS", defaultValue = "EPSG:31468", description = "CRS of the input network (e.g.\"EPSG:31468\"")
	private static String networkCRS;

	@CommandLine.Option(names = "--networkChangeEvents", defaultValue = "", description = "Set path to desired networkChangeEvents file if you want to use network change Events")
	private static Path networkChangeEventsPath;

	@CommandLine.Option(names = "--output", defaultValue = "output/demandGeneration/", description = "Path to output folder", required = true)
	private static Path outputLocation;

	@CommandLine.Option(names = "--carrierOption", defaultValue = "createCarriersFromCSV", description = "Set the choice of getting/creating carrier. Options: readCarrierFile, createCarriersFromCSV, addCSVDataToExistingCarrierFileData", required = true)
	private static CarrierInputOptions selectedCarrierInputOption;

	@CommandLine.Option(names = "--inputCarrierCSV", defaultValue = "../../../public-svn/matsim/scenarios/countries/de/freight-demand-generation/input_example/exampleCarrier.csv", description = "Path to input carrier CSV, if you want to read it.")
	private static Path csvCarrierPath;

	@CommandLine.Option(names = "--inputDemandCSV", defaultValue = "../../../public-svn/matsim/scenarios/countries/de/freight-demand-generation/input_example/exampleDemand.csv", description = "Path to input demand CSV, if you want to create a new demand based on the csv.")
	private static Path csvDemandPath;

	@CommandLine.Option(names = "--demandOption", defaultValue = "createDemandFromCSV", description = "Select the option of demand generation. Options: useDemandFromCarrierFile, createDemandFromCSV, createDemandFromCSVAndUsePopulation", required = true)
	private static DemandGenerationOptions selectedDemandGenerationOption;

	@CommandLine.Option(names = "--populationOption", defaultValue = "usePopulationInShape", description = "Select the option of using the population. Options: usePopulationHolePopulation, usePopulationInShape", required = true)
	private static PopulationOptions selectedPopulationOption;

	@CommandLine.Option(names = "--populationSamplingOption", defaultValue = "createMoreLocations", description = "Select the option of sampling if using a population. Options: createMoreLocations, increaseDemandOnLocation", required = true)
	private static PopulationSamplingOption selectedPopulationSamplingOption;

	@CommandLine.Option(names = "--populationSample", defaultValue = "0.01", description = "Sample of the selected population.")
	private static double sampleSizeInputPopulation;

	@CommandLine.Option(names = "--populationSamplingTo", defaultValue = "0.1", description = "Set the sample of the gerated demand.")
	private static double upSamplePopulationTo;

	@CommandLine.Option(names = "--populationCRS", defaultValue = "DHDN_GK4", description = "CRS of the population.")
	private static String populationCRS;

	@CommandLine.Option(names = "--combineSimilarJobs", defaultValue = "false", description = "Select the option if created jobs of the same carrier with same location and time will be combined. Options: true, false", required = true)
	private static boolean combineSimilarJobs;

	@CommandLine.Option(names = "--defaultJspriIterations", defaultValue = "3", description = "Set the default number of jsprit iterations.")
	private static int defaultJspritIterations;

	@CommandLine.Option(names = "--VRPSolutionsOption", defaultValue = "runJspritAndMATSim", description = "Select the option of solving the VRP. Options: runJspritAndMATSim, runJspritAndMATSimWithDistanceConstraint, runJsprit, runJspritWithDistanceConstraint, createNoSolutionAndOnlyWriteCarrierFile", required = true)
	private static OptionsOfVRPSolutions selectedSolution;

	public static void main(String[] args) {
		System.exit(new CommandLine(new FreightDemandGeneration()).execute(args));
	}

	@Override
	public Integer call() throws IOException, InvalidAttributeValueException, ExecutionException, InterruptedException {

		String vehicleTypesFileLocation = carrierVehicleFilePath.toString();
		String carriersFileLocation = carrierFilePath.toString();
		String shapeFileLocation = shapeFilePath.toString();
		String populationFile = populationFilePath.toString();
		CoordinateTransformation crsTransformationFromNetworkToShape = null;

		// create and prepare MATSim config
		outputLocation = outputLocation
				.resolve(java.time.LocalDate.now().toString() + "_" + java.time.LocalTime.now().toSecondOfDay());
		int lastMATSimIteration = 0;

		Config config = prepareConfig(lastMATSimIteration, networkCRS);

		log.info("Starting class to create a freight scenario");

		// select network configurations
		String networkPathOfOtherNetwork = networkPath.toString();
		String networkChangeEventsFilePath = networkChangeEventsPath.toString();
		setNetworkAndNetworkChangeEvents(config, networkPathOfOtherNetwork, networkChangeEventsFilePath);

		// load or create carrierVehicle
		log.info("Start creating carriers. Selected option: " + selectedCarrierInputOption);
		prepareVehicles(config, vehicleTypesFileLocation);

		// load or create carrier
		Scenario scenario = ScenarioUtils.loadScenario(config);

		String csvCarrierLocation = csvCarrierPath.toString();
		String csvLocationDemand = csvDemandPath.toString();

		Collection<SimpleFeature> polygonsInShape = null;
		if (!shapeFileLocation.equals("")) {
			log.info("Use shpFile to find possible locations for the carriers and the demand: " + shapeFileLocation);
			ShpOptions shpZones = new ShpOptions(shapeFilePath, shapeCRS, StandardCharsets.UTF_8);
			polygonsInShape = shpZones.readFeatures();
			crsTransformationFromNetworkToShape = shpZones.createTransformation(networkCRS);
		}
		log.info("Start creating carriers. Selected option: " + selectedCarrierInputOption);
		createCarrier(scenario, selectedCarrierInputOption, carriersFileLocation, csvCarrierLocation, polygonsInShape,
				defaultJspritIterations, crsTransformationFromNetworkToShape);

		// create the demand
		log.info("Start creating the demand. Selected option: " + selectedCarrierInputOption);
		createDemand(selectedDemandGenerationOption, scenario, csvLocationDemand, polygonsInShape, populationFile,
				selectedPopulationSamplingOption, selectedPopulationOption, combineSimilarJobs,
				crsTransformationFromNetworkToShape);

		// prepare the VRP and get a solution
		Controler controler = prepareControler(scenario);
		createDemandLocationsFile(controler);
		solveSelectedSolution(selectedSolution, config, controler);

		// TODO analyze results

		log.info("Finished");
		return 0;
	}

	/**
	 * Creates a tsv file with the locations of all created demand elements.
	 * 
	 * @param controler
	 */
	private static void createDemandLocationsFile(Controler controler) {

		Network network = controler.getScenario().getNetwork();
		FileWriter writer;
		File file;
		file = new File(controler.getConfig().controler().getOutputDirectory() + "/outputFacilitiesFile.tsv");
		try {
			writer = new FileWriter(file, true);
			writer.write("id	x	y	type	ServiceLocation	pickupLocation	deliveryLocation\n");

			for (Carrier thisCarrier : FreightUtils.getCarriers(controler.getScenario()).getCarriers().values()) {
				for (CarrierService thisService : thisCarrier.getServices().values()) {
					Coord coord = FreightDemandGenerationUtils.getCoordOfMiddlePointOfLink(network.getLinks().get(thisService.getLocationLinkId()));
					writer.write(thisCarrier.getId().toString() + thisService.getId().toString() + "	" + coord.getX()
							+ "	" + coord.getY() + "	" + "Service" + "	"
							+ thisService.getLocationLinkId().toString() + "		" + "\n");
				}
				for (CarrierShipment thisShipment : thisCarrier.getShipments().values()) {
					Coord coordFrom = FreightDemandGenerationUtils.getCoordOfMiddlePointOfLink(network.getLinks().get(thisShipment.getFrom()));
					Coord coordTo = FreightDemandGenerationUtils.getCoordOfMiddlePointOfLink(network.getLinks().get(thisShipment.getTo()));

					writer.write(thisCarrier.getId().toString() + thisShipment.getId().toString() + "	"
							+ coordFrom.getX() + "	" + coordFrom.getY() + "	" + "Pickup" + "		"
							+ thisShipment.getFrom().toString() + "	" + thisShipment.getTo().toString() + "\n");
					writer.write(thisCarrier.getId().toString() + thisShipment.getId().toString() + "	"
							+ coordTo.getX() + "	" + coordTo.getY() + "	" + "Delivery" + "		"
							+ thisShipment.getFrom().toString() + "	" + thisShipment.getTo().toString() + "\n");
				}
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Wrote job locations file under " + "/outputLocationFile.xml.gz");
	}

	/**
	 * Sets the network and the networkChangeEvents if the are available.
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

		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		if (vehicleTypesFileLocation == "")
			throw new RuntimeException("No path to the vehicleTypes selected");
		else {
			freightConfigGroup.setCarriersVehicleTypesFile(vehicleTypesFileLocation);
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
	private static void createCarrier(Scenario scenario, CarrierInputOptions selectedCarrierInputOption,
			String carriersFileLocation, String csvLocationCarrier, Collection<SimpleFeature> polygonsInShape,
			int defaultJspritIterations, CoordinateTransformation crsTransformationNetworkAndShape) throws IOException {

		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(),
				FreightConfigGroup.class);
		switch (selectedCarrierInputOption) {
		case addCSVDataToExistingCarrierFileData:
			// reads an existing carrier file and adds the information based on the read csv
			// carrier file
			if (carriersFileLocation == "")
				throw new RuntimeException("No path to the carrier file selected");
			else {
				freightConfigGroup.setCarriersFile(carriersFileLocation);
				FreightUtils.loadCarriersAccordingToFreightConfig(scenario);
				log.info("Load carriers from: " + carriersFileLocation);
				CarrierReaderFromCSV.readAndCreateCarrierFromCSV(scenario, freightConfigGroup, csvLocationCarrier,
						polygonsInShape, defaultJspritIterations, crsTransformationNetworkAndShape);
			}
			break;
		case readCarrierFile:
			// reads only a carrier file as the carrier import.
			if (carriersFileLocation == "")
				throw new RuntimeException("No path to the carrier file selected");
			else {
				freightConfigGroup.setCarriersFile(carriersFileLocation);
				FreightUtils.loadCarriersAccordingToFreightConfig(scenario);
				log.info("Load carriers from: " + carriersFileLocation);
			}
			break;
		case createCarriersFromCSV:
			// creates all carriers based on the given information in the read carrier csv
			CarrierReaderFromCSV.readAndCreateCarrierFromCSV(scenario, freightConfigGroup, csvLocationCarrier,
					polygonsInShape, defaultJspritIterations, crsTransformationNetworkAndShape);
			break;
		default:
			throw new RuntimeException("no methed to create or read carrier selected.");
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
	private static void createDemand(DemandGenerationOptions selectedDemandGenerationOption, Scenario scenario,
			String csvLocationDemand, Collection<SimpleFeature> polygonsInShape, String populationFile,
			PopulationSamplingOption selectedSamplingOption, PopulationOptions selectedPopulationOption,
			boolean combineSimilarJobs, CoordinateTransformation crsTransformationNetworkAndShape) throws IOException {

		Set<NewDemand> demandInformation = new HashSet<>();
		switch (selectedDemandGenerationOption) {
		case createDemandFromCSV:
			// creates the demand by using the information given in the read csv file
			demandInformation = readDemandInformation(csvLocationDemand, demandInformation, scenario, polygonsInShape);
			createDemandForCarriers(scenario, polygonsInShape, demandInformation, null, combineSimilarJobs,
					crsTransformationNetworkAndShape);
			break;
		case createDemandFromCSVAndUsePopulation:
			/*
			 * Option creates the demand by using the information given in the read csv file
			 * and uses a population for finding demand locations
			 */
			Population population = PopulationUtils.readPopulation(populationFile);
			demandInformation = readDemandInformation(csvLocationDemand, demandInformation, scenario, polygonsInShape);
			switch (selectedSamplingOption) {
			/*
			 * this option is important if the sample of the population and the sample of
			 * the resulting demand is different. For example you can create with a 10pct
			 * sample a 100pct demand modal for the waste collection.
			 */
			case createMoreLocations:
				/*
				 * If the demand sample is higher then the population sample, more demand
				 * location are created related to the given share of persons of the population
				 * with this demand.
				 */
				preparePopulation(population, sampleSizeInputPopulation, upSamplePopulationTo,
						"changeNumberOfLocationsWithDemand");
				break;
			case increaseDemandOnLocation:
				/*
				 * If the demand sample is higher then the population sample, the demand per
				 * person will be increased.
				 */
				preparePopulation(population, sampleSizeInputPopulation, upSamplePopulationTo,
						"changeDemandOnLocation");
				break;
			default:
				throw new RuntimeException("No valid sampling option selected!");
			}

			switch (selectedPopulationOption) {
			case usePopulationHolePopulation:
				// uses the hole population as possible demand locations
				createDemandForCarriers(scenario, polygonsInShape, demandInformation, population, combineSimilarJobs,
						crsTransformationNetworkAndShape);
				break;
			case usePopulationInShape:
				// uses only the population with home location in the given shape file
				CrsOptions populationCRSOptions = new CrsOptions(populationCRS, shapeCRS);
				reducePopulationToShapeArea(population, populationCRSOptions.getTransformation(), polygonsInShape);
				createDemandForCarriers(scenario, polygonsInShape, demandInformation, population, combineSimilarJobs,
						crsTransformationNetworkAndShape);
				break;
			default:
				throw new RuntimeException("No valid population option selected!");
			}
			break;
		case useDemandFromCarrierFile:
			// use only the given demand of the read carrier file
			boolean oneCarrierHasJobs = false;
			for (Carrier carrier : FreightUtils.getCarriers(scenario).getCarriers().values())
				if (carrier.getServices().isEmpty() && carrier.getShipments().isEmpty())
					log.warn(carrier.getId().toString() + " has no jobs which can be used");
				else {
					oneCarrierHasJobs = true;
					log.info("Used the demand of the carrier " + carrier.getId().toString() + " from the carrierFile!");
				}
			if (!oneCarrierHasJobs)
				throw new RuntimeException("Minimum one carrier has no jobs");
			break;
		default:
			throw new RuntimeException("No valid demand generation option selected!");
		}
	}

	/**
	 * Creates for every demand information the services/shipments for the carriers
	 * 
	 * @param scenario
	 * @param polygonsInShape
	 * @param demandInformation
	 * @param population
	 * @param combineSimilarJobs
	 * @param crsTransformationNetworkAndShape
	 */
	private static void createDemandForCarriers(Scenario scenario, Collection<SimpleFeature> polygonsInShape,
			Set<NewDemand> demandInformation, Population population, boolean combineSimilarJobs,
			CoordinateTransformation crsTransformationNetworkAndShape) {

		for (NewDemand newDemand : demandInformation) {
			if (newDemand.getTypeOfDemand().equals("service"))
				createServices(scenario, newDemand, polygonsInShape, population, combineSimilarJobs,
						crsTransformationNetworkAndShape);
			else if (newDemand.getTypeOfDemand().equals("shipment"))
				createShipments(scenario, newDemand, polygonsInShape, population, combineSimilarJobs,
						crsTransformationNetworkAndShape);
		}

	}

	/**
	 * Creates the shipments of a carrier.
	 * 
	 * @param scenario
	 * @param newDemand
	 * @param polygonsInShape
	 * @param population
	 * @param combineSimilarJobs
	 * @param crsTransformationNetworkAndShape
	 */
	private static void createShipments(Scenario scenario, NewDemand newDemand,
			Collection<SimpleFeature> polygonsInShape, Population population, boolean combineSimilarJobs,
			CoordinateTransformation crsTransformationNetworkAndShape) {

		int countOfLinks = 1;
		int distributedDemand = 0;
		double roundingError = 0;
		Double shareOfPopulationWithThisPickup = newDemand.getShareOfPopulationWithFirstJobElement();
		Double shareOfPopulationWithThisDelivery = newDemand.getShareOfPopulationWithSecondJobElement();
		Integer numberOfJobs = 0;
		Integer demandToDistribute = newDemand.getDemandToDistribute();
		Integer numberOfPickupLocations = newDemand.getNumberOfFirstJobElementLocations();
		Integer numberOfDeliveryLocations = newDemand.getNumberOfSecondJobElementLocations();
		String[] areasForPickupLocations = newDemand.getAreasFirstJobElement();
		String[] areasForDeliveryLocations = newDemand.getAreasSecondJobElement();
		String[] setLocationsOfPickup = newDemand.getLocationsOfFirstJobElement();
		String[] setLocationsOfDelivery = newDemand.getLocationsOfSecondJobElement();
		ArrayList<String> usedPickupLocations = new ArrayList<String>();
		ArrayList<String> usedDeliveryLocations = new ArrayList<String>();
		HashMap<Id<Person>, Person> possiblePersonsPickup = new HashMap<Id<Person>, Person>();
		HashMap<Id<Person>, Person> possiblePersonsDelivery = new HashMap<Id<Person>, Person>();
		HashMap<Id<Link>, Coord> middlePointsLinksPickup = new HashMap<Id<Link>, Coord>();
		HashMap<Id<Link>, Coord> middlePointsLinksDelivery = new HashMap<Id<Link>, Coord>();

		// set number of jobs
		if (shareOfPopulationWithThisPickup == null && shareOfPopulationWithThisDelivery == null)
			numberOfJobs = newDemand.getNumberOfJobs();
		else if (population == null)
			throw new RuntimeException(
					"No population found although input paramater <ShareOfPopulationWithThisDemand> is set");
		else {
			double sampleSizeInputPopulation = (double) population.getAttributes().getAttribute("sampleSize");
			double sampleTo = (double) population.getAttributes().getAttribute("samplingTo");
			String samplingOption = String.valueOf(population.getAttributes().getAttribute("samplingOption"));

			if (areasForPickupLocations != null)
				possiblePersonsPickup = findPossiblePersons(population, areasForPickupLocations, polygonsInShape,
						crsTransformationNetworkAndShape);
			else
				possiblePersonsPickup.putAll(population.getPersons());
			if (areasForDeliveryLocations != null)
				possiblePersonsDelivery = findPossiblePersons(population, areasForDeliveryLocations, polygonsInShape,
						crsTransformationNetworkAndShape);
			else
				possiblePersonsDelivery.putAll(population.getPersons());

			int numberPossibleJobsPickup = 0;
			int numberPossibleJobsDelivery = 0;
			if (shareOfPopulationWithThisPickup != null)
				numberPossibleJobsPickup = (int) Math
						.round(shareOfPopulationWithThisPickup * possiblePersonsPickup.size());
			if (shareOfPopulationWithThisDelivery != null)
				numberPossibleJobsDelivery = (int) Math
						.round(shareOfPopulationWithThisDelivery * possiblePersonsDelivery.size());

			if (numberPossibleJobsPickup > numberPossibleJobsDelivery) {
				if (sampleSizeInputPopulation == sampleTo) {
					numberOfJobs = (int) Math.round(shareOfPopulationWithThisPickup * numberPossibleJobsPickup);
					numberPossibleJobsPickup = numberOfJobs;
					if (shareOfPopulationWithThisDelivery != null)
						numberPossibleJobsDelivery = (int) Math
								.round(shareOfPopulationWithThisDelivery * numberPossibleJobsDelivery);
				} else if (samplingOption.equals("changeNumberOfLocationsWithDemand")) {
					numberOfJobs = (int) Math.round((sampleTo / sampleSizeInputPopulation)
							* (shareOfPopulationWithThisPickup * numberPossibleJobsPickup));
					numberPossibleJobsPickup = numberOfJobs;
					if (shareOfPopulationWithThisDelivery != null)
						numberPossibleJobsDelivery = (int) Math.round((sampleTo / sampleSizeInputPopulation)
								* (shareOfPopulationWithThisDelivery * numberPossibleJobsDelivery));
				} else if (samplingOption.equals("changeDemandOnLocation")) {
					demandToDistribute = (int) Math.round((sampleTo / sampleSizeInputPopulation) * demandToDistribute);
					numberOfJobs = (int) Math.round(shareOfPopulationWithThisPickup * numberPossibleJobsPickup);
					numberPossibleJobsPickup = numberOfJobs;
				} else
					throw new RuntimeException(
							"Error with the sampling of the demand based on the population. Please check sampling sizes and sampling options!!");
			} else {
				if (sampleSizeInputPopulation == sampleTo) {
					numberOfJobs = (int) Math.round(shareOfPopulationWithThisDelivery * numberPossibleJobsDelivery);
					numberPossibleJobsDelivery = numberOfJobs;
					numberPossibleJobsPickup = (int) Math
							.round(shareOfPopulationWithThisPickup * numberPossibleJobsPickup);
				} else if (samplingOption.equals("changeNumberOfLocationsWithDemand")) {
					numberOfJobs = (int) Math.round((sampleTo / sampleSizeInputPopulation)
							* (shareOfPopulationWithThisDelivery * numberPossibleJobsDelivery));
					numberPossibleJobsDelivery = numberOfJobs;
					if (shareOfPopulationWithThisDelivery != null)
						numberPossibleJobsPickup = (int) Math.round((sampleTo / sampleSizeInputPopulation)
								* (shareOfPopulationWithThisPickup * numberPossibleJobsPickup));
				} else if (samplingOption.equals("changeDemandOnLocation")) {
					demandToDistribute = (int) Math.round((sampleTo / sampleSizeInputPopulation) * demandToDistribute);
					numberOfJobs = (int) Math.round(shareOfPopulationWithThisDelivery * numberPossibleJobsDelivery);
					numberPossibleJobsDelivery = numberOfJobs;
				} else
					throw new RuntimeException(
							"Error with the sampling of the demand based on the population. Please check sampling sizes and sampling options!!");
			}
			if (numberPossibleJobsPickup != 0)
				numberOfPickupLocations = numberPossibleJobsPickup;
			if (numberPossibleJobsDelivery != 0)
				numberOfDeliveryLocations = numberPossibleJobsDelivery;
		}
		// find possible Links for delivery and pickup
		HashMap<Id<Link>, Link> possibleLinksPickup = findAllPossibleLinks(scenario, polygonsInShape,
				crsTransformationNetworkAndShape, numberOfPickupLocations, areasForPickupLocations,
				setLocationsOfPickup, possiblePersonsPickup, middlePointsLinksPickup);
		HashMap<Id<Link>, Link> possibleLinksDelivery = findAllPossibleLinks(scenario, polygonsInShape,
				crsTransformationNetworkAndShape, numberOfDeliveryLocations, areasForDeliveryLocations,
				setLocationsOfDelivery, possiblePersonsDelivery, middlePointsLinksDelivery);

		if (shareOfPopulationWithThisPickup != null)
			possibleLinksPickup.values().forEach(l -> middlePointsLinksPickup.put(l.getId(), FreightDemandGenerationUtils.getCoordOfMiddlePointOfLink(l)));
		if (shareOfPopulationWithThisDelivery != null)
			possibleLinksDelivery.values().forEach(l -> middlePointsLinksDelivery.put(l.getId(), FreightDemandGenerationUtils.getCoordOfMiddlePointOfLink(l)));
		if (possibleLinksPickup.isEmpty())
			throw new RuntimeException(
					"Not enough possible links to distribute the pickups. Select an different shapefile or check the CRS of the shapefile and network.");
		if (possibleLinksDelivery.isEmpty())
			throw new RuntimeException(
					"Not enough possible links to distribute the deliveries. Select an different shapefile or check the CRS of the shapefile and network.");

		if (setLocationsOfPickup != null)
			for (String selectedLinkIdPickups : setLocationsOfPickup)
				if (!possibleLinksPickup.containsKey(Id.createLinkId(selectedLinkIdPickups)))
					throw new RuntimeException("The selected link " + selectedLinkIdPickups
							+ " for pickup is not part of the possible links for pickup. Please check!");

		if (setLocationsOfDelivery != null)
			for (String selectedLinkIdDelivery : setLocationsOfDelivery)
				if (!possibleLinksDelivery.containsKey(Id.createLinkId(selectedLinkIdDelivery)))
					throw new RuntimeException("The selected link " + selectedLinkIdDelivery
							+ " for delivery is not part of the possible links for delivery. Please check!");

		// distribute the demand over the network because no number of jobs are selected
		if (numberOfJobs == null) {
			// creates shipments with a demand of 1
			if (possibleLinksPickup.size() > demandToDistribute || possibleLinksDelivery.size() > demandToDistribute) {
				for (int i = 0; i < demandToDistribute; i++) {
					Link linkPickup = null;
					Link linkDelivery = null;
					linkPickup = findNextUsedLink(scenario, polygonsInShape, possibleLinksPickup,
							numberOfPickupLocations, areasForPickupLocations, setLocationsOfPickup, usedPickupLocations,
							possiblePersonsPickup, middlePointsLinksPickup, crsTransformationNetworkAndShape, i);
					linkDelivery = findNextUsedLink(scenario, polygonsInShape, possibleLinksDelivery,
							numberOfDeliveryLocations, areasForDeliveryLocations, setLocationsOfDelivery,
							usedDeliveryLocations, possiblePersonsDelivery, middlePointsLinksDelivery,
							crsTransformationNetworkAndShape, i);

					double serviceTimePickup = newDemand.getFirstJobElementTimePerUnit();
					double serviceTimeDelivery = newDemand.getSecondJobElementTimePerUnit();
					TimeWindow timeWindowPickup = newDemand.getFirstJobElementTimeWindow();
					TimeWindow timeWindowDelivery = newDemand.getSecondJobElementTimeWindow();
					int demandForThisLink = 1;
					if (!usedPickupLocations.contains(linkPickup.getId().toString()))
						usedPickupLocations.add(linkPickup.getId().toString());
					if (!usedDeliveryLocations.contains(linkDelivery.getId().toString()))
						usedDeliveryLocations.add(linkDelivery.getId().toString());
					Id<CarrierShipment> idNewShipment = Id.create(
							createJobId(scenario, newDemand, linkPickup.getId(), linkDelivery.getId()),
							CarrierShipment.class);
					CarrierShipment thisShipment = CarrierShipment.Builder
							.newInstance(idNewShipment, linkPickup.getId(), linkDelivery.getId(), demandForThisLink)
							.setPickupServiceTime(serviceTimePickup).setPickupTimeWindow(timeWindowPickup)
							.setDeliveryServiceTime(serviceTimeDelivery).setDeliveryTimeWindow(timeWindowDelivery)
							.build();
					FreightUtils.getCarriers(scenario).getCarriers()
							.get(Id.create(newDemand.getCarrierID(), Carrier.class)).getShipments()
							.put(thisShipment.getId(), thisShipment);
				}
			} else
			// creates a demand on each link, demand depends on the length of the link
			{
				if (numberOfPickupLocations != null && numberOfDeliveryLocations != null)
					throw new RuntimeException(
							"Because the demand is higher than the number of links, the demand will be distrubted evenly over all links. You selected a certain number of pickup and delivery locations, which is not possible here!");
				HashMap<Id<Link>, Link> demandBasesLinks = null;
				double sumOfDemandBasedLinks = 0;
				boolean pickupIsDemandBase = true;
				Link linkPickup = null;
				Link linkDelivery = null;
				double sumOfPossibleLinkLenghtPickup = 0;
				double sumOfPossibleLinkLenghtDelivery = 0;
				possibleLinksPickup.values().forEach(l -> Double.sum(l.getLength(), sumOfPossibleLinkLenghtPickup));
				possibleLinksDelivery.values().forEach(l -> Double.sum(l.getLength(), sumOfPossibleLinkLenghtDelivery));
				if (numberOfPickupLocations == null && numberOfDeliveryLocations == null)
					if (possibleLinksPickup.size() > possibleLinksDelivery.size()) {
						demandBasesLinks = possibleLinksPickup;
						sumOfDemandBasedLinks = sumOfPossibleLinkLenghtPickup;
					} else {
						demandBasesLinks = possibleLinksDelivery;
						sumOfDemandBasedLinks = sumOfPossibleLinkLenghtDelivery;
						pickupIsDemandBase = false;
					}
				else if (numberOfPickupLocations != null) {
					demandBasesLinks = possibleLinksDelivery;
					sumOfDemandBasedLinks = sumOfPossibleLinkLenghtDelivery;
					pickupIsDemandBase = false;
				} else {
					demandBasesLinks = possibleLinksPickup;
					sumOfDemandBasedLinks = sumOfPossibleLinkLenghtPickup;
				}
				for (Link demandBasedLink : demandBasesLinks.values()) {
					int demandForThisLink;
					if (countOfLinks == demandBasesLinks.size()) {
						demandForThisLink = demandToDistribute - distributedDemand;
					} else {
						demandForThisLink = (int) Math.ceil(
								demandBasedLink.getLength() / sumOfDemandBasedLinks * (double) demandToDistribute);
						roundingError = roundingError + ((double) demandForThisLink
								- (demandBasedLink.getLength() / sumOfDemandBasedLinks * (double) demandToDistribute));
						if (roundingError > 1) {
							demandForThisLink = demandForThisLink - 1;
							roundingError = roundingError - 1;
						}
					}
					if (pickupIsDemandBase) {
						linkPickup = demandBasedLink;
						linkDelivery = findNextUsedLink(scenario, polygonsInShape, possibleLinksDelivery,
								numberOfDeliveryLocations, areasForDeliveryLocations, setLocationsOfDelivery,
								usedDeliveryLocations, possiblePersonsDelivery, middlePointsLinksDelivery,
								crsTransformationNetworkAndShape, countOfLinks - 1);
						while (usedDeliveryLocations.contains(linkDelivery.getId().toString())) {
							linkDelivery = findNextUsedLink(scenario, polygonsInShape, possibleLinksDelivery,
									numberOfDeliveryLocations, areasForDeliveryLocations, setLocationsOfDelivery,
									usedDeliveryLocations, possiblePersonsDelivery, middlePointsLinksDelivery,
									crsTransformationNetworkAndShape, countOfLinks - 1);
							if (usedDeliveryLocations.size() == possibleLinksDelivery.size()
									|| (numberOfDeliveryLocations != null
											&& usedDeliveryLocations.size() == numberOfDeliveryLocations))
								break;
						}
					} else {
						linkDelivery = demandBasedLink;
						linkPickup = findNextUsedLink(scenario, polygonsInShape, possibleLinksPickup,
								numberOfPickupLocations, areasForPickupLocations, setLocationsOfPickup,
								usedPickupLocations, possiblePersonsPickup, middlePointsLinksPickup,
								crsTransformationNetworkAndShape, countOfLinks - 1);
						while (usedPickupLocations.contains(linkPickup.getId().toString())) {
							linkPickup = findNextUsedLink(scenario, polygonsInShape, possibleLinksPickup,
									numberOfPickupLocations, areasForPickupLocations, setLocationsOfPickup,
									usedPickupLocations, possiblePersonsPickup, middlePointsLinksPickup,
									crsTransformationNetworkAndShape, countOfLinks - 1);
							if (usedPickupLocations.size() == possibleLinksPickup.size()
									|| (numberOfPickupLocations != null
											&& usedPickupLocations.size() == numberOfPickupLocations))
								break;
						}
					}
					countOfLinks++;
					if (!usedPickupLocations.contains(linkPickup.getId().toString()))
						usedPickupLocations.add(linkPickup.getId().toString());
					if (!usedDeliveryLocations.contains(linkDelivery.getId().toString()))
						usedDeliveryLocations.add(linkDelivery.getId().toString());
					double serviceTimePickup = newDemand.getFirstJobElementTimePerUnit() * demandForThisLink;
					double serviceTimeDelivery = newDemand.getSecondJobElementTimePerUnit() * demandForThisLink;
					TimeWindow timeWindowPickup = newDemand.getFirstJobElementTimeWindow();
					TimeWindow timeWindowDelivery = newDemand.getSecondJobElementTimeWindow();
					Id<CarrierShipment> idNewShipment = Id.create(
							createJobId(scenario, newDemand, linkPickup.getId(), linkDelivery.getId()),
							CarrierShipment.class);
					if (demandForThisLink > 0) {
						CarrierShipment thisShipment = CarrierShipment.Builder
								.newInstance(idNewShipment, linkPickup.getId(), linkDelivery.getId(), demandForThisLink)
								.setPickupServiceTime(serviceTimePickup).setPickupTimeWindow(timeWindowPickup)
								.setDeliveryServiceTime(serviceTimeDelivery).setDeliveryTimeWindow(timeWindowDelivery)
								.build();
						FreightUtils.getCarriers(scenario).getCarriers()
								.get(Id.create(newDemand.getCarrierID(), Carrier.class)).getShipments()
								.put(thisShipment.getId(), thisShipment);
					}
					distributedDemand = distributedDemand + demandForThisLink;
				}
			}
		} else

		// if a certain number of shipments is selected
		{
			for (int i = 0; i < numberOfJobs; i++) {

				if (demandToDistribute != 0 && demandToDistribute < numberOfJobs)
					throw new RuntimeException(
							"The resulting number of jobs is not feasible, because the demand is smaller then the number of jobs. Please check!");
				Link linkPickup = findNextUsedLink(scenario, polygonsInShape, possibleLinksPickup,
						numberOfPickupLocations, areasForPickupLocations, setLocationsOfPickup, usedPickupLocations,
						possiblePersonsPickup, middlePointsLinksPickup, crsTransformationNetworkAndShape, i);
				Link linkDelivery = findNextUsedLink(scenario, polygonsInShape, possibleLinksDelivery,
						numberOfDeliveryLocations, areasForDeliveryLocations, setLocationsOfDelivery,
						usedDeliveryLocations, possiblePersonsDelivery, middlePointsLinksDelivery,
						crsTransformationNetworkAndShape, i);
				int demandForThisLink = (int) Math.ceil((double) demandToDistribute / (double) numberOfJobs);
				if (numberOfJobs == (i + 1))
					demandForThisLink = demandToDistribute - distributedDemand;
				else {
					roundingError = roundingError
							+ ((double) demandForThisLink - ((double) demandToDistribute / (double) numberOfJobs));
					if (roundingError > 1) {
						demandForThisLink = demandForThisLink - 1;
						roundingError = roundingError - 1;
					}
				}
				if (!usedPickupLocations.contains(linkPickup.getId().toString()))
					usedPickupLocations.add(linkPickup.getId().toString());
				if (!usedDeliveryLocations.contains(linkDelivery.getId().toString()))
					usedDeliveryLocations.add(linkDelivery.getId().toString());
				double serviceTimePickup = 0;
				double serviceTimeDelivery = 0;
				if (demandForThisLink == 0) {
					serviceTimePickup = newDemand.getFirstJobElementTimePerUnit();
					serviceTimeDelivery = newDemand.getSecondJobElementTimePerUnit();
				} else {
					serviceTimePickup = newDemand.getFirstJobElementTimePerUnit() * demandForThisLink;
					serviceTimeDelivery = newDemand.getSecondJobElementTimePerUnit() * demandForThisLink;
				}
				TimeWindow timeWindowPickup = newDemand.getFirstJobElementTimeWindow();
				TimeWindow timeWindowDelivery = newDemand.getSecondJobElementTimeWindow();
				Id<CarrierShipment> idNewShipment = Id.create(
						createJobId(scenario, newDemand, linkPickup.getId(), linkDelivery.getId()),
						CarrierShipment.class);
				CarrierShipment thisShipment = CarrierShipment.Builder
						.newInstance(idNewShipment, linkPickup.getId(), linkDelivery.getId(), demandForThisLink)
						.setPickupServiceTime(serviceTimePickup).setPickupTimeWindow(timeWindowPickup)
						.setDeliveryServiceTime(serviceTimeDelivery).setDeliveryTimeWindow(timeWindowDelivery).build();
				FreightUtils.getCarriers(scenario).getCarriers().get(Id.create(newDemand.getCarrierID(), Carrier.class))
						.getShipments().put(thisShipment.getId(), thisShipment);
				distributedDemand = distributedDemand + demandForThisLink;
			}
		}
		if (combineSimilarJobs)
			reduceNumberOfJobsIfSameCharacteristics(scenario, newDemand);
	}

	/**
	 * Finds and returns all possible links for this job.
	 * 
	 * @param scenario
	 * @param polygonsInShape
	 * @param crsTransformationNetworkAndShape
	 * @param numberOfLocations
	 * @param areasForLocations
	 * @param setLocations
	 * @param possiblePersons
	 * @param middlePointsLinks
	 * @return
	 */
	private static HashMap<Id<Link>, Link> findAllPossibleLinks(Scenario scenario,
			Collection<SimpleFeature> polygonsInShape, CoordinateTransformation crsTransformationNetworkAndShape,
			Integer numberOfLocations, String[] areasForLocations, String[] setLocations,
			HashMap<Id<Person>, Person> possiblePersons, HashMap<Id<Link>, Coord> middlePointsLinks) {
		HashMap<Id<Link>, Link> possibleLinks = new HashMap<Id<Link>, Link>();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (numberOfLocations == null && !link.getId().toString().contains("pt") && FreightDemandGenerationUtils.checkPositionInShape(link, null,
					polygonsInShape, areasForLocations, crsTransformationNetworkAndShape)) {
				possibleLinks.put(link.getId(), link);
			} else if (numberOfLocations != null) {
				Link newPossibleLink = null;
				while (possibleLinks.size() < numberOfLocations) {
					newPossibleLink = findPossibleLinkForDemand(null, possiblePersons, middlePointsLinks,
							polygonsInShape, areasForLocations, numberOfLocations, scenario, setLocations,
							crsTransformationNetworkAndShape);
					if (!possibleLinks.containsKey(newPossibleLink.getId()))
						possibleLinks.put(newPossibleLink.getId(), newPossibleLink);
				}
			}
		}
		return possibleLinks;
	}

	/**
	 * If jobs of a carrier have the same characteristics (timewindow, location)
	 * they will be combined to one job,
	 * 
	 * @param scenario
	 * @param newDemand
	 */
	private static void reduceNumberOfJobsIfSameCharacteristics(Scenario scenario, NewDemand newDemand) {

		log.warn(
				"The number of Jobs will be reduzed if jobs have the same characteristics (e.g. time, location, carrier)");
		int connectedJobs = 0;
		if (newDemand.getTypeOfDemand().equals("shipment")) {
			HashMap<Id<CarrierShipment>, CarrierShipment> shipmentsToRemove = new HashMap<Id<CarrierShipment>, CarrierShipment>();
			ArrayList<CarrierShipment> shipmentsToAdd = new ArrayList<CarrierShipment>();
			Carrier thisCarrier = FreightUtils.getCarriers(scenario).getCarriers()
					.get(Id.create(newDemand.getCarrierID(), Carrier.class));
			for (Id<CarrierShipment> baseShipmentId : thisCarrier.getShipments().keySet()) {
				if (!shipmentsToRemove.containsKey(baseShipmentId)) {
					CarrierShipment baseShipment = thisCarrier.getShipments().get(baseShipmentId);
					HashMap<Id<CarrierShipment>, CarrierShipment> shipmentsToConnect = new HashMap<Id<CarrierShipment>, CarrierShipment>();
					shipmentsToConnect.put(baseShipmentId, baseShipment);
					for (Id<CarrierShipment> thisShipmentId : thisCarrier.getShipments().keySet()) {
						if (!shipmentsToRemove.containsKey(thisShipmentId)) {
							CarrierShipment thisShipment = thisCarrier.getShipments().get(thisShipmentId);
							if (baseShipment.getId() != thisShipment.getId()
									&& baseShipment.getFrom() == thisShipment.getFrom()
									&& baseShipment.getTo() == thisShipment.getTo()
									&& baseShipment.getPickupTimeWindow() == thisShipment.getPickupTimeWindow()
									&& baseShipment.getDeliveryTimeWindow() == thisShipment.getDeliveryTimeWindow())
								shipmentsToConnect.put(thisShipmentId, thisShipment);
						}
					}
					Id<CarrierShipment> idNewShipment = baseShipment.getId();
					int demandForThisLink = 0;
					double serviceTimePickup = 0;
					double serviceTimeDelivery = 0;
					for (CarrierShipment carrierShipment : shipmentsToConnect.values()) {
						demandForThisLink = demandForThisLink + carrierShipment.getSize();
						serviceTimePickup = serviceTimePickup + carrierShipment.getPickupServiceTime();
						serviceTimeDelivery = serviceTimeDelivery + carrierShipment.getDeliveryServiceTime();
						shipmentsToRemove.put(carrierShipment.getId(), carrierShipment);
						connectedJobs++;
					}
					CarrierShipment newShipment = CarrierShipment.Builder
							.newInstance(idNewShipment, baseShipment.getFrom(), baseShipment.getTo(), demandForThisLink)
							.setPickupServiceTime(serviceTimePickup)
							.setPickupTimeWindow(baseShipment.getPickupTimeWindow())
							.setDeliveryServiceTime(serviceTimeDelivery)
							.setDeliveryTimeWindow(baseShipment.getDeliveryTimeWindow()).build();

					shipmentsToAdd.add(newShipment);
					connectedJobs++;
				}
			}
			for (CarrierShipment id : shipmentsToRemove.values())
				thisCarrier.getShipments().remove(id.getId(), id);

			for (CarrierShipment carrierShipment : shipmentsToAdd) {
				thisCarrier.getShipments().put(carrierShipment.getId(), carrierShipment);
			}
			log.warn("Number of reduzed shipments: " + connectedJobs);
		}
		if (newDemand.getTypeOfDemand().equals("service")) {
			HashMap<Id<CarrierService>, CarrierService> servicesToRemove = new HashMap<Id<CarrierService>, CarrierService>();
			ArrayList<CarrierService> servicesToAdd = new ArrayList<CarrierService>();
			Carrier thisCarrier = FreightUtils.getCarriers(scenario).getCarriers()
					.get(Id.create(newDemand.getCarrierID(), Carrier.class));
			for (Id<CarrierService> baseServiceId : thisCarrier.getServices().keySet()) {
				if (!servicesToRemove.containsKey(baseServiceId)) {
					CarrierService baseService = thisCarrier.getServices().get(baseServiceId);
					HashMap<Id<CarrierService>, CarrierService> servicesToConnect = new HashMap<Id<CarrierService>, CarrierService>();
					servicesToConnect.put(baseServiceId, baseService);
					for (Id<CarrierService> thisServiceId : thisCarrier.getServices().keySet()) {
						if (!servicesToRemove.containsKey(thisServiceId)) {
							CarrierService thisService = thisCarrier.getServices().get(thisServiceId);
							if (baseService.getId() != thisService.getId()
									&& baseService.getLocationLinkId() == thisService.getLocationLinkId() && baseService
											.getServiceStartTimeWindow() == thisService.getServiceStartTimeWindow())
								servicesToConnect.put(thisServiceId, thisService);
						}
					}
					Id<CarrierService> idNewService = baseService.getId();
					int demandForThisLink = 0;
					double serviceTimeService = 0;
					for (CarrierService carrierService : servicesToConnect.values()) {
						demandForThisLink = demandForThisLink + carrierService.getCapacityDemand();
						serviceTimeService = serviceTimeService + carrierService.getServiceDuration();
						servicesToRemove.put(carrierService.getId(), carrierService);
						connectedJobs++;
					}
					CarrierService newService = CarrierService.Builder
							.newInstance(idNewService, baseService.getLocationLinkId())
							.setServiceDuration(serviceTimeService)
							.setServiceStartTimeWindow(baseService.getServiceStartTimeWindow())
							.setCapacityDemand(demandForThisLink).build();
					servicesToAdd.add(newService);
					connectedJobs++;
				}
			}
			for (CarrierService id : servicesToRemove.values())
				thisCarrier.getServices().remove(id.getId(), id);
			for (CarrierService carrierService : servicesToAdd) {
				thisCarrier.getServices().put(carrierService.getId(), carrierService);
			}
			log.warn("Number of reduzed shipments: " + connectedJobs);
		}
	}

	/**
	 * Creates the services.
	 * 
	 * @param scenario
	 * @param newDemand
	 * @param polygonsInShape
	 * @param population
	 * @param combineSimilarJobs
	 * @param crsTransformationNetworkAndShape
	 */
	private static void createServices(Scenario scenario, NewDemand newDemand,
			Collection<SimpleFeature> polygonsInShape, Population population, boolean combineSimilarJobs,
			CoordinateTransformation crsTransformationNetworkAndShape) {

		int countOfLinks = 1;
		int distributedDemand = 0;
		double roundingError = 0;
		Double shareOfPopulationWithThisService = newDemand.getShareOfPopulationWithFirstJobElement();
		Integer numberOfJobs = 0;
		Integer demandToDistribute = newDemand.getDemandToDistribute();
		String[] areasForServiceLocations = newDemand.getAreasFirstJobElement();
		String[] locationsOfServices = newDemand.getLocationsOfFirstJobElement();
		Integer numberOfServiceLocations = newDemand.getNumberOfFirstJobElementLocations();
		ArrayList<String> usedServiceLocations = new ArrayList<String>();
		int numberOfLinksInNetwork = scenario.getNetwork().getLinks().size();
		HashMap<Id<Person>, Person> possiblePersonsForService = new HashMap<Id<Person>, Person>();
		HashMap<Id<Link>, Coord> middlePointsLinksForService = new HashMap<Id<Link>, Coord>();

		// set number of jobs
		if (shareOfPopulationWithThisService == null)
			numberOfJobs = newDemand.getNumberOfJobs();
		else if (population == null)
			throw new RuntimeException(
					"No population found although input paramater <ShareOfPopulationWithThisDemand> is set");
		else {
			double sampleSizeInputPopulation = (double) population.getAttributes().getAttribute("sampleSize");
			double sampleTo = (double) population.getAttributes().getAttribute("samplingTo");
			String samplingOption = String.valueOf(population.getAttributes().getAttribute("samplingOption"));

			if (areasForServiceLocations != null)
				possiblePersonsForService = findPossiblePersons(population, areasForServiceLocations, polygonsInShape,
						crsTransformationNetworkAndShape);
			else
				possiblePersonsForService.putAll(population.getPersons());
			int numberPossibleServices = 0;
			if (shareOfPopulationWithThisService != null)
				numberPossibleServices = (int) Math
						.round(shareOfPopulationWithThisService * possiblePersonsForService.size());
			if (sampleSizeInputPopulation == sampleTo)
				numberOfJobs = (int) Math.round(shareOfPopulationWithThisService * possiblePersonsForService.size());
			else if (samplingOption.equals("changeNumberOfLocationsWithDemand"))
				numberOfJobs = (int) Math.round((sampleTo / sampleSizeInputPopulation)
						* (shareOfPopulationWithThisService * possiblePersonsForService.size()));
			else if (samplingOption.equals("changeDemandOnLocation")) {
				demandToDistribute = (int) Math.round((sampleTo / sampleSizeInputPopulation) * demandToDistribute);
				numberOfJobs = (int) Math.round(shareOfPopulationWithThisService * possiblePersonsForService.size());
			} else
				throw new RuntimeException(
						"Error with the sampling of the demand based on the population. Please check sampling sizes and sampling options!!");
			if (numberPossibleServices != 0)
				numberOfServiceLocations = numberPossibleServices;
		}
		// find possible links for the services
		HashMap<Id<Link>, Link> possibleLinksForService = findAllPossibleLinks(scenario, polygonsInShape,
				crsTransformationNetworkAndShape, numberOfServiceLocations, areasForServiceLocations,
				locationsOfServices, possiblePersonsForService, middlePointsLinksForService);

		if (shareOfPopulationWithThisService != null)
			possibleLinksForService.values()
					.forEach(l -> middlePointsLinksForService.put(l.getId(), FreightDemandGenerationUtils.getCoordOfMiddlePointOfLink(l)));
		if (locationsOfServices != null)
			for (String selectedLinkIdService : locationsOfServices)
				if (!possibleLinksForService.containsKey(Id.createLinkId(selectedLinkIdService)))
					throw new RuntimeException("The selected link " + selectedLinkIdService
							+ " for the service is not part of the possible links. Plaese check!");

		if (numberOfJobs == null) {
			// creates services with a demand of 1
			if (possibleLinksForService.size() > demandToDistribute) {
				for (int i = 0; i < demandToDistribute; i++) {

					Link link = findNextUsedLink(scenario, polygonsInShape, possibleLinksForService, numberOfJobs,
							areasForServiceLocations, locationsOfServices, usedServiceLocations,
							possiblePersonsForService, middlePointsLinksForService, crsTransformationNetworkAndShape,
							i);
					double serviceTime = newDemand.getFirstJobElementTimePerUnit();
					int demandForThisLink = 1;
					usedServiceLocations.add(link.getId().toString());
					Id<CarrierService> idNewService = Id.create(createJobId(scenario, newDemand, link.getId(), null),
							CarrierService.class);
					CarrierService thisService = CarrierService.Builder.newInstance(idNewService, link.getId())
							.setCapacityDemand(demandForThisLink).setServiceDuration(serviceTime)
							.setServiceStartTimeWindow(newDemand.getFirstJobElementTimeWindow()).build();
					FreightUtils.getCarriers(scenario).getCarriers()
							.get(Id.create(newDemand.getCarrierID(), Carrier.class)).getServices()
							.put(thisService.getId(), thisService);
				}
			} else
			// creates a demand on each link, demand depends on the length of the link
			{
				if (possibleLinksForService.isEmpty())
					throw new RuntimeException(
							"Not enough links in the shape file to distribute the demand. Select an different shapefile or check the CRS of the shapefile and network");
				if (numberOfServiceLocations != null)
					throw new RuntimeException(
							"Because the demand is higher than the number of links, the demand will be distrubted evenly over all links. You selected a certain number of service locations, which is not possible here!");
				double sumOfPossibleLinkLenght = 0;
				possibleLinksForService.values().forEach(l -> Double.sum(l.getLength(), sumOfPossibleLinkLenght));
				for (Link link : possibleLinksForService.values()) {
					int demandForThisLink;
					if (countOfLinks == scenario.getNetwork().getLinks().size()) {
						demandForThisLink = demandToDistribute - distributedDemand;
					} else {
						demandForThisLink = (int) Math
								.ceil(link.getLength() / sumOfPossibleLinkLenght * (double) demandToDistribute);
						roundingError = roundingError + ((double) demandForThisLink
								- (link.getLength() / sumOfPossibleLinkLenght * (double) demandToDistribute));
						if (roundingError > 1) {
							demandForThisLink = demandForThisLink - 1;
							roundingError = roundingError - 1;
						}
						countOfLinks++;
					}
					double serviceTime = newDemand.getFirstJobElementTimePerUnit() * demandForThisLink;
					Id<CarrierService> idNewService = Id.create(createJobId(scenario, newDemand, link.getId(), null),
							CarrierService.class);
					if (demandToDistribute > 0 && demandForThisLink > 0) {
						CarrierService thisService = CarrierService.Builder.newInstance(idNewService, link.getId())
								.setCapacityDemand(demandForThisLink).setServiceDuration(serviceTime)
								.setServiceStartTimeWindow(newDemand.getFirstJobElementTimeWindow()).build();
						FreightUtils.getCarriers(scenario).getCarriers().values().iterator().next().getServices()
								.put(thisService.getId(), thisService);
					}
					distributedDemand = distributedDemand + demandForThisLink;
				}
			}
		} else
		// if a certain number of services is selected
		{
			for (int i = 0; i < numberOfJobs; i++) {

				if (i * 2 > numberOfLinksInNetwork)
					throw new RuntimeException(
							"Not enough links in the shape file to distribute the demand. Select an different shapefile or check if shapefile and network has the same coordinateSystem.");
				Link link = null;
				if (numberOfServiceLocations == null || usedServiceLocations.size() < numberOfServiceLocations) {
					if (locationsOfServices != null && locationsOfServices.length > i) {
						link = scenario.getNetwork().getLinks().get(Id.createLinkId(locationsOfServices[i]));
					} else
						link = findNextUsedLink(scenario, polygonsInShape, possibleLinksForService,
								numberOfServiceLocations, areasForServiceLocations, locationsOfServices,
								usedServiceLocations, possiblePersonsForService, middlePointsLinksForService,
								crsTransformationNetworkAndShape, i);
				} else {
					Random rand = new Random();
					link = scenario.getNetwork().getLinks().get(Id.createLinkId(usedServiceLocations.stream()
							.skip(rand.nextInt(usedServiceLocations.size() - 1)).findFirst().get()));
				}
				int demandForThisLink = (int) Math.ceil((double) demandToDistribute / (double) numberOfJobs);
				if (numberOfJobs == (i + 1)) {
					demandForThisLink = demandToDistribute - distributedDemand;
				} else {
					roundingError = roundingError
							+ ((double) demandForThisLink - ((double) demandToDistribute / (double) numberOfJobs));
					if (roundingError > 1) {
						demandForThisLink = demandForThisLink - 1;
						roundingError = roundingError - 1;
					}
				}
				double serviceTime = 0;
				if (demandToDistribute == 0)
					serviceTime = newDemand.getFirstJobElementTimePerUnit();
				else
					serviceTime = newDemand.getFirstJobElementTimePerUnit() + demandForThisLink;
				usedServiceLocations.add(link.getId().toString());

				Id<CarrierService> idNewService = Id.create(createJobId(scenario, newDemand, link.getId(), null),
						CarrierService.class);
				if ((demandToDistribute > 0 && demandForThisLink > 0) || demandToDistribute == 0) {
					CarrierService thisService = CarrierService.Builder.newInstance(idNewService, link.getId())
							.setCapacityDemand(demandForThisLink).setServiceDuration(serviceTime)
							.setServiceStartTimeWindow(newDemand.getFirstJobElementTimeWindow()).build();
					FreightUtils.getCarriers(scenario).getCarriers()
							.get(Id.create(newDemand.getCarrierID(), Carrier.class)).getServices()
							.put(thisService.getId(), thisService);
				}
				distributedDemand = distributedDemand + demandForThisLink;
			}
		}
		if (combineSimilarJobs)
			reduceNumberOfJobsIfSameCharacteristics(scenario, newDemand);
	}

	/**
	 * Creates a job Id for a new job. If a certain Id is already used a number will
	 * be added at the end until no existing job was the same Id.
	 * 
	 * @param scenario
	 * @param newDemand
	 * @param linkPickup
	 * @param linkDelivery
	 * @return
	 */
	private static String createJobId(Scenario scenario, NewDemand newDemand, Id<Link> linkPickup,
			Id<Link> linkDelivery) {
		String newJobId = null;
		if (linkDelivery != null) {
			newJobId = "Shipment_" + linkPickup + "_" + linkDelivery;
			if (FreightUtils.getCarriers(scenario).getCarriers().get(Id.create(newDemand.getCarrierID(), Carrier.class))
					.getShipments().containsKey(Id.create(newJobId, CarrierShipment.class))) {
				for (int x = 1; FreightUtils.getCarriers(scenario).getCarriers()
						.get(Id.create(newDemand.getCarrierID(), Carrier.class)).getShipments()
						.containsKey(Id.create(newJobId, CarrierShipment.class)); x++) {
					newJobId = "Shipment_" + linkPickup + "_" + linkDelivery + "_" + x;
				}
			}
		} else {
			newJobId = "Service_" + linkPickup;
			if (FreightUtils.getCarriers(scenario).getCarriers().get(Id.create(newDemand.getCarrierID(), Carrier.class))
					.getServices().containsKey(Id.create(newJobId, CarrierShipment.class))) {
				for (int x = 1; FreightUtils.getCarriers(scenario).getCarriers()
						.get(Id.create(newDemand.getCarrierID(), Carrier.class)).getServices()
						.containsKey(Id.create(newJobId, CarrierShipment.class)); x++) {
					newJobId = "Service_" + linkPickup + "_" + x;
				}
			}
		}
		return newJobId.toString();
	}

	/**
	 * Finds the next link which can be used as a location.
	 * 
	 * @param scenario
	 * @param polygonsInShape
	 * @param possibleLinks
	 * @param selectedNumberOfLocations
	 * @param areasForLocations
	 * @param selectedLocations
	 * @param usedLocations
	 * @param possiblePersons
	 * @param middlePointsLinks
	 * @param crsTransformationNetworkAndShape
	 * @param i
	 * @return
	 */
	private static Link findNextUsedLink(Scenario scenario, Collection<SimpleFeature> polygonsInShape,
			HashMap<Id<Link>, Link> possibleLinks, Integer selectedNumberOfLocations, String[] areasForLocations,
			String[] selectedLocations, ArrayList<String> usedLocations, HashMap<Id<Person>, Person> possiblePersons,
			HashMap<Id<Link>, Coord> middlePointsLinks, CoordinateTransformation crsTransformationNetworkAndShape,
			int i) {
		Link link = null;

		if (selectedNumberOfLocations == null || usedLocations.size() < selectedNumberOfLocations) {
			if (selectedLocations != null && selectedLocations.length > i) {
				link = scenario.getNetwork().getLinks().get(Id.createLinkId(selectedLocations[i]));
			} else
				while (link == null
						|| (selectedNumberOfLocations != null && usedLocations.contains(link.getId().toString())))
					link = findPossibleLinkForDemand(possibleLinks, possiblePersons, middlePointsLinks, polygonsInShape,
							areasForLocations, selectedNumberOfLocations, scenario, selectedLocations,
							crsTransformationNetworkAndShape);
		} else {
			Random rand = new Random();
			link = scenario.getNetwork().getLinks()
					.get(Id.createLinkId(usedLocations.get(rand.nextInt(usedLocations.size()))));
		}
		return link;
	}

	/**
	 * Reads the demand information from the csv file and checks if the information
	 * are consistent
	 * 
	 * @param csvLocationDemand
	 * @param demandInformation
	 * @param scenario
	 * @param polygonsInShape
	 * @return
	 * @throws IOException
	 */
	private static Set<NewDemand> readDemandInformation(String csvLocationDemand, Set<NewDemand> demandInformation,
			Scenario scenario, Collection<SimpleFeature> polygonsInShape) throws IOException {

		CSVParser parse = CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader()
				.parse(IOUtils.getBufferedReader(csvLocationDemand));

		for (CSVRecord record : parse) {
			String carrierID = null;
			if (!record.get("carrierName").isBlank())
				carrierID = record.get("carrierName");
			Integer demandToDistribute = null;
			if (!record.get("demandToDistribute").isBlank())
				demandToDistribute = Integer.parseInt(record.get("demandToDistribute"));
			Integer numberOfJobs = null;
			if (!record.get("numberOfJobs").isBlank())
				numberOfJobs = Integer.parseInt(record.get("numberOfJobs"));
			Double shareOfPopulationWithFirstJobElement = null;
			if (!record.get("shareOfPopulationWithFirstJobElement").isBlank())
				shareOfPopulationWithFirstJobElement = Double
						.parseDouble(record.get("shareOfPopulationWithFirstJobElement"));
			String[] areasFirstJobElement = null;
			if (!record.get("areasFirstJobElement").isBlank())
				areasFirstJobElement = record.get("areasFirstJobElement").split(",");
			Integer numberOfFirstJobElementLocations = null;
			if (!record.get("numberOfFirstJobElementLocations").isBlank())
				numberOfFirstJobElementLocations = Integer.parseInt(record.get("numberOfFirstJobElementLocations"));
			String[] locationsOfFirstJobElement = null;
			if (!record.get("locationsOfFirstJobElement").isBlank())
				locationsOfFirstJobElement = record.get("locationsOfFirstJobElement").split(",");
			Integer firstJobElementTimePerUnit = null;
			if (!record.get("firstJobElementTimePerUnit").isBlank())
				firstJobElementTimePerUnit = Integer.parseInt(record.get("firstJobElementTimePerUnit"));
			TimeWindow firstJobElementTimeWindow = null;
			if (!record.get("firstJobElementStartTime").isBlank() || !record.get("firstJobElementEndTime").isBlank())
				firstJobElementTimeWindow = TimeWindow.newInstance(
						Integer.parseInt(record.get("firstJobElementStartTime")),
						Integer.parseInt(record.get("firstJobElementEndTime")));
			Double shareOfPopulationWithSecondJobElement = null;
			if (!record.get("shareOfPopulationWithSecondJobElement").isBlank())
				shareOfPopulationWithSecondJobElement = Double
						.parseDouble(record.get("shareOfPopulationWithSecondJobElement"));
			String[] areasSecondJobElement = null;
			if (!record.get("areasSecondJobElement").isBlank())
				areasSecondJobElement = record.get("areasSecondJobElement").split(",");
			Integer numberOfSecondJobElementLocations = null;
			if (!record.get("numberOfSecondJobElementLocations").isBlank())
				numberOfSecondJobElementLocations = Integer.parseInt(record.get("numberOfSecondJobElementLocations"));
			String[] locationsOfSecondJobElement = null;
			if (!record.get("locationsOfSecondJobElement").isBlank())
				locationsOfSecondJobElement = record.get("locationsOfSecondJobElement").split(",");
			Integer secondJobElementTimePerUnit = null;
			if (!record.get("secondJobElementTimePerUnit").isBlank())
				secondJobElementTimePerUnit = Integer.parseInt(record.get("secondJobElementTimePerUnit"));
			TimeWindow secondJobElementTimeWindow = null;
			if (!record.get("secondJobElementStartTime").isBlank() || !record.get("secondJobElementEndTime").isBlank())
				secondJobElementTimeWindow = TimeWindow.newInstance(
						Integer.parseInt(record.get("secondJobElementStartTime")),
						Integer.parseInt(record.get("secondJobElementEndTime")));

			if (areasSecondJobElement != null || numberOfSecondJobElementLocations != null
					|| locationsOfSecondJobElement != null || secondJobElementTimePerUnit != null
					|| secondJobElementTimeWindow != null) {
				NewDemand newShipmentDemand = new NewDemand(carrierID, demandToDistribute, numberOfJobs,
						shareOfPopulationWithFirstJobElement, areasFirstJobElement, numberOfFirstJobElementLocations,
						locationsOfFirstJobElement, firstJobElementTimePerUnit, firstJobElementTimeWindow,
						shareOfPopulationWithSecondJobElement, areasSecondJobElement, numberOfSecondJobElementLocations,
						locationsOfSecondJobElement, secondJobElementTimePerUnit, secondJobElementTimeWindow);
				demandInformation.add(newShipmentDemand);
			} else {
				NewDemand newServiceDemand = new NewDemand(carrierID, demandToDistribute, numberOfJobs,
						shareOfPopulationWithFirstJobElement, areasFirstJobElement, numberOfFirstJobElementLocations,
						locationsOfFirstJobElement, firstJobElementTimePerUnit, firstJobElementTimeWindow);
				demandInformation.add(newServiceDemand);
			}

		}
		checkNewDemand(scenario, demandInformation, polygonsInShape);
		return demandInformation;
	}

	/**
	 * Checks if the read demand information are useful to create the shipment or
	 * service demands
	 * 
	 * @param scenario
	 * @param demandInformation
	 * @param polygonsInShape
	 */
	private static void checkNewDemand(Scenario scenario, Set<NewDemand> demandInformation,
			Collection<SimpleFeature> polygonsInShape) {

		for (NewDemand newDemand : demandInformation) {
			if (newDemand.getCarrierID() == null || newDemand.getCarrierID().isBlank())
				throw new RuntimeException(
						"Minimum one demand is not related to a carrier. Every demand information has to be related to one carrier. Please check the input csv file!");
			Carriers carriers = (Carriers) scenario.getScenarioElement("carriers");
			if (!carriers.getCarriers().containsKey(Id.create(newDemand.getCarrierID(), Carrier.class)))
				throw new RuntimeException(
						"The created demand is not created for an existing carrier. Please create the carrier "
								+ newDemand.getCarrierID() + " first or relate the demand to another carrier");
			if (newDemand.getDemandToDistribute() == null)
				throw new RuntimeException("For the carrier " + newDemand.getCarrierID()
						+ ": No demand information found. You must add 0 as demand if you want no demand. Please check!");
			if (newDemand.getNumberOfJobs() != null && newDemand.getDemandToDistribute() != 0
					&& newDemand.getDemandToDistribute() < newDemand.getNumberOfJobs())
				throw new RuntimeException("For the carrier " + newDemand.getCarrierID()
						+ ": The number of jobs is higher than the demand. This is not possible because the minimum demand of one job is 1");
			if (newDemand.getNumberOfJobs() != null && newDemand.getNumberOfJobs() == 0)
				throw new RuntimeException("For the carrier " + newDemand.getCarrierID()
						+ ": The number of jobs can not be 0 !. Please check!");
			if (newDemand.getNumberOfJobs() == null
					&& (newDemand.getDemandToDistribute() == null || newDemand.getDemandToDistribute() == 0)
					&& newDemand.getShareOfPopulationWithFirstJobElement() == null
					&& newDemand.getShareOfPopulationWithSecondJobElement() == null)
				throw new RuntimeException(
						"You have to select a number of jobs, a population share or a demand. Please Check!!");
			if (newDemand.getShareOfPopulationWithFirstJobElement() != null)
				if (newDemand.getShareOfPopulationWithFirstJobElement() > 1
						|| newDemand.getShareOfPopulationWithFirstJobElement() <= 0)
					throw new RuntimeException("For the carrier " + newDemand.getCarrierID()
							+ ": The percentage of the population should be more than 0 and maximum 100pct. Please check!");
			if (newDemand.getShareOfPopulationWithFirstJobElement() != null
					&& newDemand.getNumberOfFirstJobElementLocations() != null)
				throw new RuntimeException("For the carrier " + newDemand.getCarrierID()
						+ ": Select either share of population or number of locations");
			if (newDemand.getAreasFirstJobElement() != null) {
				if (polygonsInShape == null)
					throw new RuntimeException("You selected a certain area for the carrier" + newDemand.getCarrierID()
							+ " although no shape file is loaded.");
				for (String demandArea : newDemand.getAreasFirstJobElement()) {
					boolean isInShape = false;
					for (SimpleFeature singlePolygon : polygonsInShape)
						if (singlePolygon.getAttribute("Ortsteil").equals(demandArea)
								|| singlePolygon.getAttribute("BEZNAME").equals(demandArea)) {
							isInShape = true;
							break;
						}
					if (!isInShape)
						throw new RuntimeException("The area " + demandArea + " for the demand generation of carrier"
								+ newDemand.getCarrierID() + " is not part of the given shapeFile");
				}
			}
			if (newDemand.getLocationsOfFirstJobElement() != null)
				for (String linkForDemand : newDemand.getLocationsOfFirstJobElement()) {
					if (!scenario.getNetwork().getLinks().containsKey(Id.createLinkId(linkForDemand)))
						throw new RuntimeException("The selected link " + linkForDemand + " for the demand of carrier "
								+ newDemand.getCarrierID() + " not part of the network. Please check!");
				}
			if (newDemand.getFirstJobElementTimePerUnit() == null)
				throw new RuntimeException("For the carrier " + newDemand.getCarrierID()
						+ ": No time per unit for one job element was selected");
			if (newDemand.getFirstJobElementTimeWindow() == null)
				throw new RuntimeException("For the carrier " + newDemand.getCarrierID()
						+ ": No timeWindow for one job element was selected");

			// for services
			if (newDemand.getTypeOfDemand().equals("service")) {
				if (newDemand.getNumberOfJobs() != null && newDemand.getShareOfPopulationWithFirstJobElement() != null)
					throw new RuntimeException("For the carrier " + newDemand.getCarrierID()
							+ ": Select either a numberOfJobs or a share of the population. Please check!");
				if (newDemand.getLocationsOfFirstJobElement() != null && newDemand.getNumberOfJobs() != null
						&& newDemand.getLocationsOfFirstJobElement().length > newDemand.getNumberOfJobs())
					throw new RuntimeException("For the carrier " + newDemand.getCarrierID()
							+ ": For creating services it is not possible to have a higher number of <locationsOfFirstJobElement> than <numberOfJobs>");
				if (newDemand.getLocationsOfFirstJobElement() != null
						&& newDemand.getNumberOfFirstJobElementLocations() != null
						&& newDemand.getLocationsOfFirstJobElement().length > newDemand
								.getNumberOfFirstJobElementLocations())
					throw new RuntimeException("For the carrier " + newDemand.getCarrierID()
							+ ": You selected more certain locations than the set number of locations");
			}
			// for shipments
			if (newDemand.getTypeOfDemand().equals("shipment")) {
				if (newDemand.getShareOfPopulationWithSecondJobElement() != null
						&& newDemand.getNumberOfSecondJobElementLocations() != null)
					throw new RuntimeException("For the carrier " + newDemand.getCarrierID()
							+ ": Select either share of population or number of locations");
				if (newDemand.getNumberOfJobs() != null && newDemand.getShareOfPopulationWithFirstJobElement() != null
						&& newDemand.getShareOfPopulationWithSecondJobElement() != null)
					throw new RuntimeException("For the carrier " + newDemand.getCarrierID()
							+ ": Select either a numberOfJobs or a share of the population. Please check!");
				if (newDemand.getShareOfPopulationWithSecondJobElement() != null)
					if (newDemand.getShareOfPopulationWithSecondJobElement() > 1
							|| newDemand.getShareOfPopulationWithSecondJobElement() <= 0)
						throw new RuntimeException("For the carrier " + newDemand.getCarrierID()
								+ ": The percentage of the population should be more than 0 and maximum 100pct. Please check!");
				if (newDemand.getAreasSecondJobElement() != null) {
					if (polygonsInShape == null)
						throw new RuntimeException("You selected a certain area for the carrier"
								+ newDemand.getCarrierID() + " although no shape file is loaded.");
					for (String demand : newDemand.getAreasSecondJobElement()) {
						boolean isInShape = false;
						for (SimpleFeature singlePolygon : polygonsInShape)
							if (singlePolygon.getAttribute("Ortsteil").equals(demand)
									|| singlePolygon.getAttribute("BEZNAME").equals(demand)) {
								isInShape = true;
								break;
							}
						if (!isInShape)
							throw new RuntimeException("The area " + demand + " for the demand generation of carrier"
									+ newDemand.getCarrierID() + " is not part of the given shapeFile");
					}
				}
				if (newDemand.getLocationsOfSecondJobElement() != null)
					for (String linkForDemand : newDemand.getLocationsOfSecondJobElement()) {
						if (!scenario.getNetwork().getLinks().containsKey(Id.createLinkId(linkForDemand)))
							throw new RuntimeException(
									"The selected link " + linkForDemand + " for the demand of carrier "
											+ newDemand.getCarrierID() + " not part of the network. Please check!");
					}
				if (newDemand.getSecondJobElementTimePerUnit() == null)
					throw new RuntimeException("For the carrier " + newDemand.getCarrierID()
							+ ": No time per unit for one job element was selected");
				if (newDemand.getSecondJobElementTimeWindow() == null)
					throw new RuntimeException("For the carrier " + newDemand.getCarrierID()
							+ ": No timeWindow for one job element was selected");
			}
		}
	}

	/**
	 * Reduces the population to all persons having their home in the shape
	 * 
	 * @param population
	 * @param crsTransformationPopulationAndShape
	 * @param polygonsInShape
	 */
	private static void reducePopulationToShapeArea(Population population,
			CoordinateTransformation crsTransformationPopulationAndShape, Collection<SimpleFeature> polygonsInShape) {

		List<Id<Person>> personsToRemove = new ArrayList<>();
		double x, y;
		for (Person person : population.getPersons().values()) {
			boolean isInShape = false;
			x = (double) person.getAttributes().getAttribute("homeX");
			y = (double) person.getAttributes().getAttribute("homeY");
			Point point = MGC
					.coord2Point(crsTransformationPopulationAndShape.transform(MGC.point2Coord(MGC.xy2Point(x, y))));
			for (SimpleFeature singlePolygon : polygonsInShape)
				if (((Geometry) singlePolygon.getDefaultGeometry()).contains(point)) {
					isInShape = true;
					break;
				}
			if (!isInShape)
				personsToRemove.add(person.getId());
		}
		for (Id<Person> id : personsToRemove)
			population.removePerson(id);
	}

	/**
	 * Differs between the different options for solving the VRP problem.
	 * 
	 * @param selectedSolution
	 * @param config
	 * @param controler
	 * @throws InvalidAttributeValueException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private static void solveSelectedSolution(OptionsOfVRPSolutions selectedSolution, Config config,
			Controler controler) throws InvalidAttributeValueException, ExecutionException, InterruptedException {
		switch (selectedSolution) {
		case runJspritAndMATSim:
			// solves the VRP with jsprit and runs MATSim afterwards
			new CarrierPlanXmlWriterV2((Carriers) controler.getScenario().getScenarioElement("carriers"))
					.write(config.controler().getOutputDirectory() + "/output_carriersNoPlans.xml");
			runJsprit(controler, false);
			controler.run();
			new CarrierPlanXmlWriterV2((Carriers) controler.getScenario().getScenarioElement("carriers"))
					.write(config.controler().getOutputDirectory() + "/output_carriersWithPlans.xml");
			break;
		case runJspritAndMATSimWithDistanceConstraint:
			// solves the VRP with jsprit by using the distance constraint and runs MATSim
			// afterwards
			new CarrierPlanXmlWriterV2((Carriers) controler.getScenario().getScenarioElement("carriers"))
					.write(config.controler().getOutputDirectory() + "/output_carriersNoPlans.xml");
			runJsprit(controler, true);
			controler.run();
			new CarrierPlanXmlWriterV2((Carriers) controler.getScenario().getScenarioElement("carriers"))
					.write(config.controler().getOutputDirectory() + "/output_carriersWithPlans.xml");
			break;
		case runJsprit:
			// solves only the VRP with jsprit
			new CarrierPlanXmlWriterV2((Carriers) controler.getScenario().getScenarioElement("carriers"))
					.write(config.controler().getOutputDirectory() + "/output_carriersNoPlans.xml");
			runJsprit(controler, false);
			new CarrierPlanXmlWriterV2((Carriers) controler.getScenario().getScenarioElement("carriers"))
					.write(config.controler().getOutputDirectory() + "/output_carriersWithPlans.xml");
			log.warn(
					"##Finished with the jsprit solution. If you also want to run MATSim, please change  case of optionsOfVRPSolutions");
			System.exit(0);
			break;
		case runJspritWithDistanceConstraint:
			// solves only the VRP with jsprit by using the distance constraint
			new CarrierPlanXmlWriterV2((Carriers) controler.getScenario().getScenarioElement("carriers"))
					.write(config.controler().getOutputDirectory() + "/output_carriersNoPlans.xml");
			runJsprit(controler, true);
			new CarrierPlanXmlWriterV2((Carriers) controler.getScenario().getScenarioElement("carriers"))
					.write(config.controler().getOutputDirectory() + "/output_carriersWithPlans.xml");
			log.warn(
					"##Finished with the jsprit solution. If you also want to run MATSim, please change  case of optionsOfVRPSolutions");
			System.exit(0);
			break;
		case createNoSolutionAndOnlyWriteCarrierFile:
			// creates no solution of the VRP and only writes the carrier file with the
			// generated carriers and demands
			new CarrierPlanXmlWriterV2((Carriers) controler.getScenario().getScenarioElement("carriers"))
					.write(config.controler().getOutputDirectory() + "/output_carriersNoPlans.xml");
			log.warn(
					"##Finished without solution of the VRP. If you also want to run jsprit and/or MATSim, please change case of optionsOfVRPSolutions");
			System.exit(0);
			break;
		default:
			break;
		}
	}

//	/**
//	 * Reads and create the carriers with reading the information from the csv file.
//	 * 
//	 * @param scenario
//	 * @param allNewCarrier
//	 * @param freightConfigGroup
//	 * @param csvLocationCarrier
//	 * @param polygonsInShape
//	 * @param defaultJspritIterations
//	 * @param crsTransformationNetworkAndShape
//	 * @throws IOException
//	 */
//	private static void readAndCreateCarrierFromCSV(Scenario scenario, 
//			FreightConfigGroup freightConfigGroup, String csvLocationCarrier, Collection<SimpleFeature> polygonsInShape,
//			int defaultJspritIterations, CoordinateTransformation crsTransformationNetworkAndShape) throws IOException {
//
//		log.info("Start reading carrier csv file: " + csvLocationCarrier);
//		Set<CarrierReaderFromCSV> allNewCarrier = new HashSet<>();
//		CSVParser parse = CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader()
//				.parse(IOUtils.getBufferedReader(csvLocationCarrier));
//		for (CSVRecord record : parse) {
//			String carrierID = null;
//			if (!record.get("carrierName").isBlank())
//				carrierID = record.get("carrierName");
//			String[] vehilceTypes = null;
//			if (!record.get("vehicleTypes").isBlank())
//				vehilceTypes = record.get("vehicleTypes").split(",");
//			int numberOfDepots = 0;
//			if (!record.get("numberOfDepots").isBlank())
//				numberOfDepots = Integer.parseInt(record.get("numberOfDepots"));
//			String[] vehicleDepots = null;
//			if (!record.get("selectedVehicleDepots").isBlank())
//				vehicleDepots = record.get("selectedVehicleDepots").split(",");
//			String[] areaOfAdditonalDepots = null;
//			if (!record.get("areaOfAdditonalDepots").isBlank())
//				areaOfAdditonalDepots = record.get("areaOfAdditonalDepots").split(",");
//			FleetSize fleetSize = null;
//			int fixedNumberOfVehilcePerTypeAndLocation = 0;
//			if (!record.get("fixedNumberOfVehilcePerTypeAndLocation").isBlank())
//				fixedNumberOfVehilcePerTypeAndLocation = Integer
//						.parseInt(record.get("fixedNumberOfVehilcePerTypeAndLocation"));
//			if (!record.get("fleetSize").isBlank() && record.get("fleetSize").contentEquals("infinite"))
//				fleetSize = FleetSize.INFINITE;
//			else if (!record.get("fleetSize").isBlank() && record.get("fleetSize").contentEquals("finite"))
//				fleetSize = FleetSize.FINITE;
//			else if (!record.get("fleetSize").isBlank())
//				throw new RuntimeException("Select a valid FleetSize for the carrier: " + carrierID
//						+ ". Possible is finite or infinite!!");
//			int vehicleStartTime = 0;
//			if (!record.get("vehicleStartTime").isBlank())
//				vehicleStartTime = Integer.parseInt(record.get("vehicleStartTime"));
//			int vehicleEndTime = 0;
//			if (!record.get("vehicleEndTime").isBlank())
//				vehicleEndTime = Integer.parseInt(record.get("vehicleEndTime"));
//			int jspritIterations = 0;
//			if (!record.get("jspritIterations").isBlank())
//				jspritIterations = Integer.parseInt(record.get("jspritIterations"));
//			CarrierReaderFromCSV newCarrier = new CarrierReaderFromCSV(carrierID, vehilceTypes, numberOfDepots, vehicleDepots,
//					areaOfAdditonalDepots, fleetSize, vehicleStartTime, vehicleEndTime, jspritIterations,
//					fixedNumberOfVehilcePerTypeAndLocation);
//			allNewCarrier.add(newCarrier);
//		}
//		checkNewCarrier(allNewCarrier, freightConfigGroup, scenario, polygonsInShape);
//		log.info("The read carrier information from the csv are checked without errors.");
//		createNewCarrierAndAddVehilceTypes(scenario, allNewCarrier, freightConfigGroup, polygonsInShape,
//				defaultJspritIterations, crsTransformationNetworkAndShape);
//	}

//	/**
//	 * Checks if the read carrier information are consistent.
//	 * 
//	 * @param allNewCarrier
//	 * @param freightConfigGroup
//	 * @param scenario
//	 * @param polygonsInShape
//	 */
//	private static void checkNewCarrier(Set<CarrierReaderFromCSV> allNewCarrier, FreightConfigGroup freightConfigGroup,
//			Scenario scenario, Collection<SimpleFeature> polygonsInShape) {
//
//		FreightUtils.addOrGetCarriers(scenario);
//		for (CarrierReaderFromCSV carrier : allNewCarrier) {
//			if (FreightUtils.getCarriers(scenario).getCarriers()
//					.containsKey(Id.create(carrier.getName(), Carrier.class)))
//				throw new RuntimeException("The Carrier " + carrier.getName()
//						+ " being loaded from the csv is already in the given Carrier file. It is not possible to add to an existing Carrier. Please check!");
//
//			if (carrier.getName() == null || carrier.getName().isBlank())
//				throw new RuntimeException(
//						"Minimum one carrier has no name. Every carrier information has to be related to one carrier. Please check the input csv file!");
//			CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
//			new CarrierVehicleTypeReader(carrierVehicleTypes)
//					.readFile(freightConfigGroup.getCarriersVehicleTypesFile());
//			if (carrier.getVehicleTypes() != null)
//				for (String type : carrier.getVehicleTypes()) {
//					if (!carrierVehicleTypes.getVehicleTypes().containsKey(Id.create(type, VehicleType.class)))
//						throw new RuntimeException("The selected vehicleType " + type + " of the carrier "
//								+ carrier.getName()
//								+ " in the input file is not part of imported vehicle types. Please change the type or add the type in the vehicleTypes input file!");
//				}
//			if (carrier.getVehicleDepots() != null) {
//				if (carrier.getNumberOfDepotsPerType() < carrier.getVehicleDepots().length)
//					throw new RuntimeException("For the carrier " + carrier.getName()
//							+ " more certain depots than the given number of depots are selected. (numberOfDepots < selectedVehicleDepots)");
//
//				for (String linkDepot : carrier.getVehicleDepots()) {
//					if (!scenario.getNetwork().getLinks().containsKey(Id.create(linkDepot, Link.class)))
//						throw new RuntimeException("The selected link " + linkDepot + " for a depot of the carrier "
//								+ carrier.getName() + " is not part of the network. Please check!");
//				}
//			}
//			if (carrier.getVehicleTypes() != null && carrier.getNumberOfDepotsPerType() == 0
//					&& carrier.getVehicleDepots() == null)
//				throw new RuntimeException(
//						"If a vehicle type is selected in the input file, numberOfDepots or selectedVehicleDepots should be set. Please check carrier "
//								+ carrier.getName());
//			if (carrier.getVehicleDepots() != null
//					&& (carrier.getNumberOfDepotsPerType() > carrier.getVehicleDepots().length)
//					&& carrier.getAreaOfAdditonalDepots() == null)
//				log.warn(
//						"No possible area for addional depot given. Random choice in the hole network of a possible position");
//			if (carrier.getVehicleDepots() == null && (carrier.getNumberOfDepotsPerType() > 0)
//					&& carrier.getAreaOfAdditonalDepots() == null)
//				log.warn(
//						"No possible area for addional depot given. Random choice in the hole network of a possible position");
//			if (carrier.getAreaOfAdditonalDepots() != null) {
//				if (polygonsInShape == null)
//					throw new RuntimeException("For carrier " + carrier.getName()
//							+ " a certain area for depots is selected, but no shape is read in. Please check.");
//				for (String depotArea : carrier.getAreaOfAdditonalDepots()) {
//					boolean isInShape = false;
//					for (SimpleFeature singlePolygon : polygonsInShape) {
//						if (singlePolygon.getAttribute("Ortsteil").equals(depotArea)
//								|| singlePolygon.getAttribute("BEZNAME").equals(depotArea)) {
//							isInShape = true;
//							break;
//						}
//					}
//					if (!isInShape)
//						throw new RuntimeException("The area " + depotArea + " of the possible depots of carrier"
//								+ carrier.getName() + " is not part of the given shapeFile");
//				}
//			}
//			if (carrier.getFixedNumberOfVehilcePerTypeAndLocation() != 0)
//				for (CarrierReaderFromCSV existingCarrier : allNewCarrier)
//					if ((existingCarrier.getName().equals(carrier.getName())
//							&& existingCarrier.getFleetSize() == FleetSize.INFINITE)
//							|| carrier.getFleetSize() == FleetSize.INFINITE)
//						throw new RuntimeException("For the carrier " + carrier.getName()
//								+ " a infinite fleetSize configuration was set, although you want to set a fixed number of vehicles. Please check!");
//			if (carrier.getFleetSize() != null)
//				for (CarrierReaderFromCSV existingCarrier : allNewCarrier)
//					if (existingCarrier.getName().equals(carrier.getName()) && existingCarrier.getFleetSize() != null
//							&& existingCarrier.getFleetSize() != carrier.getFleetSize())
//						throw new RuntimeException("For the carrier " + carrier.getName()
//								+ " different fleetSize configuration was set. Please check and select only one!");
//			if (carrier.getVehicleTypes() != null) {
//				if (carrier.getVehicleStartTime() == 0 || carrier.getVehicleEndTime() == 0)
//					throw new RuntimeException("For the vehicle types of the carrier " + carrier.getName()
//							+ " no start and/or end time for the vehicles was selected. Please set both times!!");
//				if (carrier.getVehicleStartTime() >= carrier.getVehicleEndTime())
//					throw new RuntimeException("For the vehicle types of the carrier " + carrier.getName()
//							+ " a startTime after the endTime for the vehicles was selected. Please check!");
//			}
//			if (carrier.getJspritIterations() != 0)
//				for (CarrierReaderFromCSV existingCarrier : allNewCarrier)
//					if (existingCarrier.getName().equals(carrier.getName())
//							&& existingCarrier.getJspritIterations() != 0
//							&& existingCarrier.getJspritIterations() != carrier.getJspritIterations())
//						throw new RuntimeException("For the carrier " + carrier.getName()
//								+ " different number of jsprit iterations are set. Please check!");
//		}
//	}

//	/**
//	 * Read and creates the carrier and the vehicle types.
//	 * 
//	 * @param scenario
//	 * @param allNewCarrier
//	 * @param freightConfigGroup
//	 * @param polygonsInShape
//	 * @param defaultJspritIterations
//	 * @param crsTransformationNetworkAndShape
//	 */
//	private static void createNewCarrierAndAddVehilceTypes(Scenario scenario, Set<CarrierReaderFromCSV> allNewCarrier,
//			FreightConfigGroup freightConfigGroup, Collection<SimpleFeature> polygonsInShape,
//			int defaultJspritIterations, CoordinateTransformation crsTransformationNetworkAndShape) {
//
//		Carriers carriers = FreightUtils.addOrGetCarriers(scenario);
//		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
//		CarrierVehicleTypes usedCarrierVehicleTypes = FreightUtils.getCarrierVehicleTypes(scenario);
//		new CarrierVehicleTypeReader(carrierVehicleTypes).readFile(freightConfigGroup.getCarriersVehicleTypesFile());
//
//		for (CarrierReaderFromCSV singleNewCarrier : allNewCarrier) {
//			if (singleNewCarrier.getVehicleTypes() == null) {
//				continue;
//			}
//			Carrier thisCarrier = null;
//			CarrierCapabilities carrierCapabilities = null;
//			if (carriers.getCarriers().containsKey(Id.create(singleNewCarrier.getName(), Carrier.class))) {
//				thisCarrier = carriers.getCarriers().get(Id.create(singleNewCarrier.getName(), Carrier.class));
//				carrierCapabilities = thisCarrier.getCarrierCapabilities();
//				if (carrierCapabilities.getFleetSize() == null && singleNewCarrier.getFleetSize() != null)
//					carrierCapabilities.setFleetSize(singleNewCarrier.getFleetSize());
//				if (singleNewCarrier.getJspritIterations() > 0)
//					CarrierUtils.setJspritIterations(thisCarrier, singleNewCarrier.getJspritIterations());
//			} else {
//				thisCarrier = CarrierUtils.createCarrier(Id.create(singleNewCarrier.getName(), Carrier.class));
//				if (singleNewCarrier.getJspritIterations() > 0)
//					CarrierUtils.setJspritIterations(thisCarrier, singleNewCarrier.getJspritIterations());
//				carrierCapabilities = CarrierCapabilities.Builder.newInstance()
//						.setFleetSize(singleNewCarrier.getFleetSize()).build();
//				carriers.addCarrier(thisCarrier);
//			}
//			if (singleNewCarrier.getVehicleDepots() == null)
//				singleNewCarrier.setVehicleDepots(new String[] {});
//			while (singleNewCarrier.getVehicleDepots().length < singleNewCarrier.getNumberOfDepotsPerType()) {
//				Random rand = new Random();
//				Link link = scenario.getNetwork().getLinks().values().stream()
//						.skip(rand.nextInt(scenario.getNetwork().getLinks().size())).findFirst().get();
//				if (!link.getId().toString().contains("pt") && checkPositionInShape(link, null, polygonsInShape,
//						singleNewCarrier.getAreaOfAdditonalDepots(), crsTransformationNetworkAndShape)) {
//					singleNewCarrier.addVehicleDepots(singleNewCarrier.getVehicleDepots(), link.getId().toString());
//				}
//			}
//			for (String singleDepot : singleNewCarrier.getVehicleDepots()) {
//				for (String thisVehicleType : singleNewCarrier.getVehicleTypes()) {
//					VehicleType thisType = carrierVehicleTypes.getVehicleTypes()
//							.get(Id.create(thisVehicleType, VehicleType.class));
//					usedCarrierVehicleTypes.getVehicleTypes().putIfAbsent(Id.create(thisVehicleType, VehicleType.class),
//							thisType);
//					if (singleNewCarrier.getFixedNumberOfVehilcePerTypeAndLocation() == 0)
//						singleNewCarrier.setFixedNumberOfVehilcePerTypeAndLocation(1);
//					for (int i = 0; i < singleNewCarrier.getFixedNumberOfVehilcePerTypeAndLocation(); i++) {
//						CarrierVehicle newCarrierVehicle = CarrierVehicle.Builder.newInstance(Id.create(
//								thisType.getId().toString() + "_" + thisCarrier.getId().toString() + "_" + singleDepot
//										+ "_start" + singleNewCarrier.getVehicleStartTime() + "_" + (i + 1),
//								Vehicle.class), Id.createLinkId(singleDepot), thisType)
//								.setEarliestStart(singleNewCarrier.getVehicleStartTime())
//								.setLatestEnd(singleNewCarrier.getVehicleEndTime()).build();
//						carrierCapabilities.getCarrierVehicles().put(newCarrierVehicle.getId(), newCarrierVehicle);
//						if (!carrierCapabilities.getVehicleTypes().contains(thisType))
//							carrierCapabilities.getVehicleTypes().add(thisType);
//					}
//				}
//			}
//			thisCarrier.setCarrierCapabilities(carrierCapabilities);
//		}
//		for (Carrier carrier : carriers.getCarriers().values()) {
//			if (CarrierUtils.getJspritIterations(carrier) == Integer.MIN_VALUE) {
//				CarrierUtils.setJspritIterations(carrier, defaultJspritIterations);
//				log.warn("The jspritIterations are now set to the default value of " + defaultJspritIterations
//						+ " in this simulation!");
//			}
//		}
//	}

	/**
	 * Finds all persons which are possible for the demand.
	 * 
	 * @param population
	 * @param areasForServiceLocations
	 * @param polygonsInShape
	 * @param crsTransformationNetworkAndShape
	 * @return
	 */
	private static HashMap<Id<Person>, Person> findPossiblePersons(Population population,
			String[] areasForServiceLocations, Collection<SimpleFeature> polygonsInShape,
			CoordinateTransformation crsTransformationNetworkAndShape) {

		HashMap<Id<Person>, Person> possiblePersons = new HashMap<Id<Person>, Person>();

		for (Person person : population.getPersons().values()) {
			Point p = MGC.xy2Point((double) person.getAttributes().getAttribute("homeX"),
					(double) person.getAttributes().getAttribute("homeY"));
			Coord coord = crsTransformationNetworkAndShape.transform(MGC.point2Coord(p));

			if (FreightDemandGenerationUtils.checkPositionInShape(null, MGC.coord2Point(coord), polygonsInShape, areasForServiceLocations,
					crsTransformationNetworkAndShape))
				possiblePersons.put(person.getId(), person);
		}
		return possiblePersons;
	}

	/**
	 * Searches a possible link for the demand.
	 * 
	 * @param possibleLinks
	 * @param possiblePersons
	 * @param middlePointsLinks
	 * @param polygonsInShape
	 * @param areasForTheDemand
	 * @param selectedNumberOfLocations
	 * @param scenario
	 * @param selectedLocations
	 * @param crsTransformationNetworkAndShape
	 * @return
	 */
	private static Link findPossibleLinkForDemand(HashMap<Id<Link>, Link> possibleLinks,
			HashMap<Id<Person>, Person> possiblePersons, HashMap<Id<Link>, Coord> middlePointsLinks,
			Collection<SimpleFeature> polygonsInShape, String[] areasForTheDemand, Integer selectedNumberOfLocations,
			Scenario scenario, String[] selectedLocations, CoordinateTransformation crsTransformationNetworkAndShape) {
		Random rand = new Random();
		Link selectedlink = null;
		Link newLink = null;
		if (selectedNumberOfLocations == null)
			selectedNumberOfLocations = 0;
		while (selectedlink == null) {
			if (possibleLinks == null || possibleLinks.size() < selectedNumberOfLocations) {
				if (selectedLocations != null && selectedLocations.length > possibleLinks.size()) {
					newLink = scenario.getNetwork().getLinks()
							.get(Id.createLinkId(selectedLocations[possibleLinks.size()]));
				} else {
					Random randLink = new Random();
					newLink = scenario.getNetwork().getLinks().values().stream()
							.skip(randLink.nextInt(scenario.getNetwork().getLinks().size())).findFirst().get();
				}
			} else {
				if (middlePointsLinks.isEmpty()) {
					newLink = possibleLinks.values().stream().skip(rand.nextInt(possibleLinks.size())).findFirst()
							.get();
				} else {
					Person person = possiblePersons.values().stream().skip(rand.nextInt(possiblePersons.size()))
							.findFirst().get();
					Point homePoint = MGC.xy2Point((double) person.getAttributes().getAttribute("homeX"),
							(double) person.getAttributes().getAttribute("homeY"));
					newLink = possibleLinks.get(findNearestLink(MGC.point2Coord(homePoint), middlePointsLinks));
				}
			}
			if (!newLink.getId().toString().contains("pt") && (polygonsInShape == null || FreightDemandGenerationUtils.checkPositionInShape(newLink,
					null, polygonsInShape, areasForTheDemand, crsTransformationNetworkAndShape)))
				selectedlink = newLink;
		}
		return selectedlink;
	}

//	/**
//	 * Creates the middle coord of a link.
//	 * 
//	 * @param link
//	 * @return Middle coord of the Link
//	 */
//	private static Coord middlePointOfLink(Link link) {
//
//		double x, y, xCoordFrom, xCoordTo, yCoordFrom, yCoordTo;
//		xCoordFrom = link.getFromNode().getCoord().getX();
//		xCoordTo = link.getToNode().getCoord().getX();
//		yCoordFrom = link.getFromNode().getCoord().getY();
//		yCoordTo = link.getToNode().getCoord().getY();
//		if (xCoordFrom > xCoordTo)
//			x = xCoordFrom - ((xCoordFrom - xCoordTo) / 2);
//		else
//			x = xCoordTo - ((xCoordTo - xCoordFrom) / 2);
//		if (yCoordFrom > yCoordTo)
//			y = yCoordFrom - ((yCoordFrom - yCoordTo) / 2);
//		else
//			y = yCoordTo - ((yCoordTo - yCoordFrom) / 2);
//
//		return MGC.point2Coord(MGC.xy2Point(x, y));
//	}

	/**
	 * Finds the nearest possible link of one certain location.
	 * 
	 * @param pointActivity
	 * @param middlePointsLinks
	 * @return
	 */
	private static Id<Link> findNearestLink(Coord pointActivity, HashMap<Id<Link>, Coord> middlePointsLinks) {

		Id<Link> nearestLink = null;
		double distance;
		double minDistance = -1;

		for (Id<Link> link : middlePointsLinks.keySet()) {
			Coord middlePointLink = middlePointsLinks.get(link);
			distance = NetworkUtils.getEuclideanDistance(pointActivity, middlePointLink);

			if (minDistance == -1 || distance < minDistance) {
				minDistance = distance;
				nearestLink = link;
			}
		}
		return nearestLink;
	}

//	/**
//	 * Checks if a link is one of the possible areas.
//	 * 
//	 * @param link
//	 * @param point
//	 * @param polygonsInShape
//	 * @param possibleAreas
//	 * @param crsTransformationNetworkAndShape
//	 * @return
//	 */
//	private static boolean checkPositionInShape(Link link, Point point, Collection<SimpleFeature> polygonsInShape,
//			String[] possibleAreas, CoordinateTransformation crsTransformationNetworkAndShape) {
//
//		if (polygonsInShape == null)
//			return true;
//		boolean isInShape = false;
//		Point p = null;
//		if (link != null && point == null) {
//			p = MGC.coord2Point(crsTransformationNetworkAndShape.transform(middlePointOfLink(link)));
//		} else if (link == null && point != null)
//			p = point;
//		for (SimpleFeature singlePolygon : polygonsInShape) {
//			if (possibleAreas != null) {
//				for (String area : possibleAreas) {
//					if (area.equals(singlePolygon.getAttribute("Ortsteil"))
//							|| area.equals(singlePolygon.getAttribute("BEZNAME")))
//						if (((Geometry) singlePolygon.getDefaultGeometry()).contains(p)) {
//							isInShape = true;
//							return isInShape;
//						}
//				}
//			} else {
//				if (((Geometry) singlePolygon.getDefaultGeometry()).contains(p)) {
//					isInShape = true;
//					return isInShape;
//				}
//			}
//		}
//		return isInShape;
//	}

	/**
	 * Prepares the controller.
	 * 
	 * @param scenario
	 * @return
	 */
	private static Controler prepareControler(Scenario scenario) {
		Controler controler = new Controler(scenario);

		Freight.configure(controler);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new CarrierModule());
			}
		});
		return controler;
	}

	/**
	 * Deletes the existing output file and sets the number of the last iteration
	 * 
	 * @param lastMATSimIteration
	 * @param coordinateSystem
	 * @return
	 */
	private static Config prepareConfig(int lastMATSimIteration, String coordinateSystem) {
		Config config = ConfigUtils.createConfig();
		ScenarioUtils.loadScenario(config);
		config.controler().setOutputDirectory(outputLocation.toString());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		new OutputDirectoryHierarchy(config.controler().getOutputDirectory(), config.controler().getRunId(),
				config.controler().getOverwriteFileSetting(), ControlerConfigGroup.CompressionType.gzip);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setLastIteration(lastMATSimIteration);
		config.global().setRandomSeed(4177);
		config.global().setCoordinateSystem(coordinateSystem);
		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		freightConfigGroup.setTravelTimeSliceWidth(1800);
		freightConfigGroup.setTimeWindowHandling(FreightConfigGroup.TimeWindowHandling.enforceBeginnings);

		return config;
	}

	/**
	 * Adds the home coordinates to attributes and removes plans
	 * 
	 * @param population
	 * @param sampleSizeInputPopulation
	 * @param sampleTo
	 * @param samlingOption
	 */
	private static void preparePopulation(Population population, double sampleSizeInputPopulation, double sampleTo,
			String samlingOption) {
		List<Id<Person>> personsToRemove = new ArrayList<>();
		population.getAttributes().putAttribute("sampleSize", sampleSizeInputPopulation);
		population.getAttributes().putAttribute("samplingTo", sampleTo);
		population.getAttributes().putAttribute("samplingOption", samlingOption);

		for (Person person : population.getPersons().values()) {
			if (!person.getAttributes().getAttribute("subpopulation").toString().equals("person")) {
				personsToRemove.add(person.getId());
				continue;
			}
			for (Plan plan : person.getPlans())
				for (PlanElement element : plan.getPlanElements())
					if (element instanceof Activity)
						if (((Activity) element).getType().contains("home")) {
							double x = ((Activity) element).getCoord().getX();
							double y = ((Activity) element).getCoord().getY();
							person.getAttributes().putAttribute("homeX", x);
							person.getAttributes().putAttribute("homeY", y);
							break;
						}
			person.removePlan(person.getSelectedPlan());
		}
		for (Id<Person> id : personsToRemove)
			population.removePerson(id);
	}

	/**
	 * Runs jsprit
	 * 
	 * @param controler
	 * @param usingRangeRestriction
	 * @throws InvalidAttributeValueException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private static void runJsprit(Controler controler, boolean usingRangeRestriction)
			throws InvalidAttributeValueException, ExecutionException, InterruptedException {
		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(controler.getConfig(),
				FreightConfigGroup.class);
		if (usingRangeRestriction)
			freightConfigGroup.setUseDistanceConstraintForTourPlanning(
					FreightConfigGroup.UseDistanceConstraintForTourPlanning.basedOnEnergyConsumption);
		FreightUtils.runJsprit(controler.getScenario());
	}
}
