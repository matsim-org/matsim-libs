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

import com.google.common.collect.Sets;
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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions.Index;
import org.matsim.contrib.roadpricing.*;
import org.matsim.contrib.common.conventions.vsp.SubpopulationDefaultNames;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.UnmaterializedConfigGroupChecker;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkTransform;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.analysis.CarriersAnalysis;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.simwrapper.dashboard.*;
import org.matsim.smallScaleCommercialTrafficGeneration.data.CommercialTourSpecifications;
import org.matsim.smallScaleCommercialTrafficGeneration.data.DefaultTourSpecificationsByUsingKID2002;
import org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.StructuralAttribute;

import org.matsim.vehicles.CostInformation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import picocli.CommandLine;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.readDataDistribution;

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

	private static final Logger log = LogManager.getLogger(GenerateSmallScaleCommercialTrafficDemand.class);
	private static final String UNSOLVED_CARRIER_FILE = "output_carriers_unsolvedVRP.xml.gz";
	private static final String SOLVED_CARRIER_FILE = "output_carriers_solvedVRP.xml.gz";
	private static final String CARRIER_VEHICLE_TYPES_FILE = "output_carriersVehicleTypes.xml.gz";
	private static final String CARRIER_PARTS_FOLDER = "carrierParts";
	private final IntegrateExistingTrafficToSmallScaleCommercial integrateExistingTrafficToSmallScaleCommercial;
	private final CommercialTourSpecifications commercialTourSpecifications;
	protected final OdMatrixEntryInformationProvider odMatrixEntryInformationProvider;
	private final UnhandledServicesSolution unhandledServicesSolution;

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

	@CommandLine.Option(names = {"--additionalTravelBufferPerIterationInMinutes", "--additionalTravelBufferPerTourAndIterationInMinutes"}, description = "Additional travel buffer in minutes per scheduled tour and carrier-replanning iteration. Used while resolving carriers with unhandled services; if set too low, carriers may not serve all services.", defaultValue = "30")
	private int additionalTravelBufferPerTourAndIterationInMinutes;

	@CommandLine.Option(names = "--maxNumberOfLoopsForVRPSolving", description = "Limit of carrier replanning iterations, where carriers with unhandled services get new plans. If your carrier-plans are still not fully served, increase this limit.", defaultValue = "100")
	private int maxNumberOfLoopsForVRPSolving;

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

	@CommandLine.Option(names = "--resistanceFactor_commercialPersonTraffic", description = "ResistanceFactor for the trip distribution")
	private double resistanceFactor_commercialPersonTraffic;

	@CommandLine.Option(names = "--resistanceFactor_goodsTraffic", description = "ResistanceFactor for the trip distribution")
	private double resistanceFactor_goodsTraffic;

	@CommandLine.Option(names = "--nameOutputPopulation", description = "Name of the output Population")
	private String nameOutputPopulation;

	@CommandLine.Option(names = "--numberOfPlanVariantsPerAgent", description = "If an agent should have variant plans, you should set this parameter.", defaultValue = "1")
	private int numberOfPlanVariantsPerAgent;

	@CommandLine.Option(names = "--network", description = "Overwrite network file in config")
	private String network;

	@CommandLine.Option(names = "--pathOutput", description = "Path for the output")
	private Path output;

	@CommandLine.Option(names = "--MATSimIterationsAfterDemandGeneration", description = "If selected, the MATSim simulation will be run for the selected number of iterations after demand generation. if not selected, only demand generation is performed.")
	private Integer MATSimIterationsAfterDemandGeneration;

	@CommandLine.Option(names = "--factorForTravelBufferCalculation", description = "Factor applied to the total service duration when estimating the required total tour duration for initial vehicle creation. Values above 1.0 reserve additional time for travel between services", defaultValue = "1.2")
	private double factorForTravelBufferCalculation;

	@CommandLine.Option(names = "--useRangeConstraintForTourPlanning", description = "Option to use range constraint for planning the tours. If this is selected, the range is restricted based on consumption information in the vehicle types file.")
	private boolean useRangeConstraintForTourPlanning;

	@CommandLine.Option(names = "--distanceConstraintUsableRange", description = "Usable range in percent of the energy capacity applied to the vehicle range during tour planning. For example, a value of 80 limits the usable range to 80 percent. This option is only applied if --useRangeConstraintForTourPlanning is selected.", defaultValue = "100")
	private double distanceConstraintUsableRange;

	@CommandLine.Option(names = "--createSmallScaleCommercialCarrierFileOnly", description = "Create the unsolved small scale commercial carrier file and stop before tour planning.")
	private boolean createSmallScaleCommercialCarrierFileOnly;

	@CommandLine.Option(names = "--smallScaleCommercialCarrierPartCount", defaultValue = "1", description = "Number of independent carrier parts for small scale commercial tour planning. Use with --smallScaleCommercialCarrierPartIndex.")
	private int smallScaleCommercialCarrierPartCount;

	@CommandLine.Option(names = "--smallScaleCommercialCarrierPartIndex", defaultValue = "0", description = "Zero-based index of the independent carrier part to solve.")
	private int smallScaleCommercialCarrierPartIndex;

	@CommandLine.Option(names = "--mergeSmallScaleCommercialCarrierParts", description = "Merge independently solved small scale commercial carrier parts and create the population from the merged solution.")
	private boolean mergeSmallScaleCommercialCarrierParts;

	@CommandLine.Option(names = "--smallScaleCommercialCarrierPartsFolder", description = "Folder containing the solved small scale commercial carrier part folders. Defaults to <pathOutput>/carrierParts.")
	private Path smallScaleCommercialCarrierPartsFolder;

	private Random rnd;
	private RandomGenerator rng;
	private final Map<String, Map<StructuralAttribute, EnumeratedDistribution<ActivityFacility>>> facilitiesPerZoneWithProbabilities = new HashMap<>();
	private final Map<Id<Carrier>, CarrierAttributes> carrierId2carrierAttributes = new HashMap<>();
	private final Map<SmallScaleCommercialTrafficType, Double> resistanceFactorsPerModelType = new HashMap<>();

	private Map<SmallScaleCommercialTrafficType, EnumeratedDistribution<TourStartAndDuration>> tourDistribution = null;
	private Map<ServiceDurationPerCategoryKey, EnumeratedDistribution<DurationsBounds>> serviceDurationTimeSelector = null;

	private TripDistributionMatrix odMatrix;
	private Map<String, Object2DoubleMap<StructuralAttribute>> resultingDataPerZone;
	private Map<String, Map<Id<Link>, Link>> linksPerZone;

	private Index indexZones;

	private final String[] configArgs;

	public GenerateSmallScaleCommercialTrafficDemand() {
		this(null, null, null, null, null, null);
	}

	public GenerateSmallScaleCommercialTrafficDemand(String[] configArgs) {
		this(configArgs, null, null, null, null, null);
	}

	public GenerateSmallScaleCommercialTrafficDemand(IntegrateExistingTrafficToSmallScaleCommercial integrateExistingTrafficToSmallScaleCommercial,
	                                                 CommercialTourSpecifications commercialTourSpecifications, OdMatrixEntryInformationProvider odMatrixEntryInformationProvider,
	                                                 VehicleTypeSelection vehicleTypeSelection, UnhandledServicesSolution unhandledServicesSolution) {
		this(null, integrateExistingTrafficToSmallScaleCommercial, commercialTourSpecifications, odMatrixEntryInformationProvider, vehicleTypeSelection, unhandledServicesSolution);
	}
	public GenerateSmallScaleCommercialTrafficDemand(String[] configArgs,
	                                                 IntegrateExistingTrafficToSmallScaleCommercial integrateExistingTrafficToSmallScaleCommercial,
	                                                 CommercialTourSpecifications commercialTourSpecifications, OdMatrixEntryInformationProvider odMatrixEntryInformationProvider,
	                                                 VehicleTypeSelection vehicleTypeSelection, UnhandledServicesSolution unhandledServicesSolution) {

		this.configArgs = (configArgs == null) ? new String[0] : configArgs;

		if (integrateExistingTrafficToSmallScaleCommercial == null) {
			this.integrateExistingTrafficToSmallScaleCommercial = new DefaultIntegrateExistingTrafficToSmallScaleCommercialImpl();
			log.info("Using default {} if existing models are integrated!", DefaultIntegrateExistingTrafficToSmallScaleCommercialImpl.class.getSimpleName());
		} else {
			this.integrateExistingTrafficToSmallScaleCommercial = integrateExistingTrafficToSmallScaleCommercial;
			log.info("Using {} if existing models are integrated!", integrateExistingTrafficToSmallScaleCommercial.getClass().getSimpleName());
		}
		if (commercialTourSpecifications == null) {
			this.commercialTourSpecifications = new DefaultTourSpecificationsByUsingKID2002();
			log.info("Using default {} for tour specifications!", DefaultTourSpecificationsByUsingKID2002.class.getSimpleName());
		} else {
			this.commercialTourSpecifications = commercialTourSpecifications;
			log.info("Using {} for tour specifications!", commercialTourSpecifications.getClass().getSimpleName());
		}
		if (odMatrixEntryInformationProvider == null) {
			if (vehicleTypeSelection != null) {
				this.odMatrixEntryInformationProvider = new DefaultOdMatrixEntryInformationProvider(vehicleTypeSelection);
				log.info("Using default {} with provided {} for OD matrix entry information!", DefaultOdMatrixEntryInformationProvider.class.getSimpleName(), vehicleTypeSelection.getClass().getSimpleName());
			}
			else {
				this.odMatrixEntryInformationProvider = new DefaultOdMatrixEntryInformationProvider();
				log.info("Using default {} for OD matrix entry information!", DefaultOdMatrixEntryInformationProvider.class.getSimpleName());
			}
		} else {
			this.odMatrixEntryInformationProvider = odMatrixEntryInformationProvider;
			log.info("Using {} for OD matrix entry information!", odMatrixEntryInformationProvider.getClass().getSimpleName());
		}
		if (unhandledServicesSolution == null) {
			this.unhandledServicesSolution = new DefaultUnhandledServicesSolution(this);
			log.info("Using default {} for unhandled-services-solution!", DefaultUnhandledServicesSolution.class.getSimpleName());
		} else {
			this.unhandledServicesSolution = unhandledServicesSolution;
			log.info("Using {} for unhandled-services-solution!", unhandledServicesSolution.getClass().getSimpleName());
		}
	}

	public static void main(String[] args) {
		new GenerateSmallScaleCommercialTrafficDemand().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		Configurator.setLevel("org.matsim.core.utils.geometry.geotools.MGC", Level.ERROR);
		validateCarrierPartOptions();

		String modelName = configPath.getParent().getFileName().toString();

		String sampleName = SmallScaleCommercialTrafficUtils.getSampleNameOfOutputFolder(sample);

		/*
		 * A carrier part run needs two different output concepts:
		 * - finalOutput points to the shared traffic output folder created by the init step. The shared unsolved carrier
		 *   file is read from there, and the merge step later writes the complete result there.
		 * - configOutput points to the isolated part folder. It must be set before readAndCheckConfig initializes the
		 *   OutputDirectoryHierarchy, otherwise MATSim would touch or even delete the shared traffic output folder when
		 *   a single part job starts on the cluster.
		 */
		Path requestedOutput = output;
		Path configOutput = isSolvingOnlyCarrierPart() && requestedOutput != null
			? getCarrierPartOutputPath(requestedOutput)
			: requestedOutput;
		Config config = readAndCheckConfig(configArgs, configPath, modelName, sampleName, configOutput);

		output = Path.of(config.controller().getOutputDirectory());
		Path finalOutput = isSolvingOnlyCarrierPart() && requestedOutput != null ? requestedOutput : output;

		Scenario scenario;
		if (mergeSmallScaleCommercialCarrierParts) {
			mergeSmallScaleCommercialCarrierParts(config, finalOutput);
			scenario = SmallScaleCommercialTrafficUtils.loadScenarioWithCarrierFile(config,
				finalOutput.resolve(SmallScaleCommercialTrafficUtils.getRunIdPrefixedFileName(config, SOLVED_CARRIER_FILE)), CARRIER_VEHICLE_TYPES_FILE);
		} else {
			if (isSolvingOnlyCarrierPart()) {
				if (usedCreationOption == CreationOption.createNewCarrierFile) {
					Path sharedCarrierFile = finalOutput.resolve(SmallScaleCommercialTrafficUtils.getRunIdPrefixedFileName(config, UNSOLVED_CARRIER_FILE));
					if (!Files.exists(sharedCarrierFile)) {
						throw new IllegalStateException("Missing shared small scale commercial carrier file without solution: " + sharedCarrierFile
							+ ". Run with --createSmallScaleCommercialCarrierFileOnly before starting carrier part jobs.");
					}
					carrierFilePath = sharedCarrierFile;
					usedCreationOption = CreationOption.useExistingCarrierFileWithoutSolution;
				}
			}

			scenario = ScenarioUtils.loadScenario(config);

			resultingDataPerZone = readDataDistribution(pathToDataDistributionToZones);
			serviceDurationTimeSelector = commercialTourSpecifications.createStopDurationDistributionPerCategory(rng);
			tourDistribution = commercialTourSpecifications.createTourDistribution(rng);

			if ((usedSmallScaleCommercialTrafficType == SmallScaleCommercialTrafficType.commercialPersonTraffic || usedSmallScaleCommercialTrafficType == SmallScaleCommercialTrafficType.completeSmallScaleCommercialTraffic) && resistanceFactor_commercialPersonTraffic == 0.)
				throw new Exception("You selected commercialPersonTraffic but did not set a resistanceFactor_commercialPersonTraffic. Please set it.");
			if ((usedSmallScaleCommercialTrafficType == SmallScaleCommercialTrafficType.goodsTraffic || usedSmallScaleCommercialTrafficType == SmallScaleCommercialTrafficType.completeSmallScaleCommercialTraffic) && resistanceFactor_goodsTraffic == 0.)
				throw new Exception("You selected goodsTraffic but did not set a resistanceFactor_goodsTraffic > 0. Please set it.");

			resistanceFactorsPerModelType.put(SmallScaleCommercialTrafficType.commercialPersonTraffic, resistanceFactor_commercialPersonTraffic);
			resistanceFactorsPerModelType.put(SmallScaleCommercialTrafficType.goodsTraffic, resistanceFactor_goodsTraffic);
			log.info("Set resistance factor for commercialPersonTraffic to {} and for goodsTraffic to {}.", resistanceFactor_commercialPersonTraffic, resistanceFactor_goodsTraffic);

			switch (usedCreationOption) {
				case useExistingCarrierFileWithSolution, useExistingCarrierFileWithoutSolution -> {
					log.info("Existing carriers (including carrier vehicle types) should be set in the freight config group");
					if (includeExistingModels)
						throw new Exception(
							"You set that existing models should included to the new model. This is only possible for a creation of the new carrier file and not by using an existing.");
					FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);

					if (freightCarriersConfigGroup.getCarriersFile() == null)
						freightCarriersConfigGroup.setCarriersFile(carrierFilePath.toAbsolutePath().toString());
					Path carrierVehicleTypesFile = SmallScaleCommercialTrafficUtils.resolveCarrierVehicleTypesFile(config, carrierFilePath, CARRIER_VEHICLE_TYPES_FILE);
					if (carrierVehicleTypesFile != null)
						freightCarriersConfigGroup.setCarriersVehicleTypesFile(carrierVehicleTypesFile.toAbsolutePath().toString());
					else if (config.vehicles() != null && freightCarriersConfigGroup.getCarriersVehicleTypesFile() == null)
						freightCarriersConfigGroup.setCarriersVehicleTypesFile(config.vehicles().getVehiclesFile());
					log.info("Load carriers from: {}", freightCarriersConfigGroup.getCarriersFile());
					CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

				// Remove vehicle types which are not used by the carriers
				Map<Id<VehicleType>, VehicleType> readVehicleTypes = CarriersUtils.getOrAddCarrierVehicleTypes(scenario).getVehicleTypes();
				List<Id<VehicleType>> usedCarrierVehicleTypes = CarriersUtils.getCarriers(scenario).getCarriers().values().stream()
					.flatMap(carrier -> carrier.getCarrierCapabilities().getCarrierVehicles().values().stream())
					.map(vehicle -> vehicle.getType().getId())
					.distinct()
					.toList();

				readVehicleTypes.keySet().removeIf(vehicleType -> !usedCarrierVehicleTypes.contains(vehicleType));

				// if we do solving of unhandled jobs, these steps are necessary to prepare the solving.
				if (maxNumberOfLoopsForVRPSolving > 0) {
					if (!Files.exists(shapeFileZonePath)) {
						throw new Exception("Required districts shape file {} not found" + shapeFileZonePath.toString());
					}
					indexZones = SmallScaleCommercialTrafficUtils.getIndexZones(shapeFileZonePath, shapeCRS, shapeFileZoneNameColumn);
					filterFacilitiesForZones(scenario);
					linksPerZone = filterLinksForZones(scenario, this.indexZones, facilitiesPerZoneWithProbabilities, shapeFileZoneNameColumn);
				}
				if (Objects.requireNonNull(usedCreationOption) == CreationOption.useExistingCarrierFileWithoutSolution) {
					CarriersUtils.getCarriers(scenario).getCarriers().values().forEach(CarriersUtils::clearCarrierPlans);
				} else {
					// if we use an existing carrier with a solution, we delete only the plans of carriers which have unhandled jobs.
					List<Carrier> nonCompleteSolvedCarriers = CarriersUtils.createListOfCarrierWithUnhandledJobs(
						CarriersUtils.getCarriers(scenario));
					if (!nonCompleteSolvedCarriers.isEmpty()) {
						log.info(
							"By using the option {} {} carriers of all {} carriers are found with unhandled jobs. These carriers will be solved and the plans of the fully planed carriers will remain. ",
							CreationOption.useExistingCarrierFileWithSolution, nonCompleteSolvedCarriers.size(), CarriersUtils.getCarriers(scenario).getCarriers().size());
						nonCompleteSolvedCarriers.forEach((CarriersUtils::clearCarrierPlans));
					}
				}
				filterCarriersForSelectedPart(scenario);
				ensureJspritIterationsForCarriersToSolve(scenario);
				// for the case @useExistingCarrierFileWithSolution the method solveSeparatedVRPs skips carriers with existing plans. But if a carrier without plans exists, it will be solved.
				CarriersUtils.writeCarriers(scenario, UNSOLVED_CARRIER_FILE);
				solveVRP(scenario);
				}
				default -> {
					if (!Files.exists(shapeFileZonePath)) {
						throw new Exception("Required districts shape file {} not found" + shapeFileZonePath.toString());
					}
					indexZones = SmallScaleCommercialTrafficUtils.getIndexZones(shapeFileZonePath, shapeCRS, shapeFileZoneNameColumn);

					filterFacilitiesForZones(scenario);
					prepareConfigForResultingModes(scenario);

					switch (usedSmallScaleCommercialTrafficType) {
						case commercialPersonTraffic, goodsTraffic -> createCarriersAndDemand(output, scenario,
							usedSmallScaleCommercialTrafficType,
							includeExistingModels, indexZones);
						case completeSmallScaleCommercialTraffic -> {
							createCarriersAndDemand(output, scenario, SmallScaleCommercialTrafficType.commercialPersonTraffic,
								includeExistingModels, indexZones);
							createCarriersAndDemand(output, scenario, SmallScaleCommercialTrafficType.goodsTraffic,
								false, indexZones);
						}
						default -> throw new RuntimeException("No traffic type selected.");
					}
					CarriersUtils.writeCarriers(scenario, UNSOLVED_CARRIER_FILE);
					if (createSmallScaleCommercialCarrierFileOnly) {
						CarriersUtils.writeCarrierVehicleTypes(scenario, CARRIER_VEHICLE_TYPES_FILE);
						log.info("Created small scale commercial carrier file without solution. Skipping jsprit and population generation.");
						return 0;
					}
					filterCarriersForSelectedPart(scenario);
					if (isSolvingOnlyCarrierPart()) {
						CarriersUtils.writeCarriers(scenario, UNSOLVED_CARRIER_FILE);
					}
					ensureJspritIterationsForCarriersToSolve(scenario);
					solveVRP(scenario);
				}
			}
		}
		CarriersUtils.writeCarrierVehicleTypes(scenario, CARRIER_VEHICLE_TYPES_FILE);
		CarriersUtils.writeCarriers(scenario, SOLVED_CARRIER_FILE);

		CarriersAnalysis carriersAnalysis = new CarriersAnalysis(scenario, output.resolve("analysis").resolve("freight").toString());
		carriersAnalysis.runCarrierAnalysis(CarriersAnalysis.CarrierAnalysisType.carriersStatsAndDetailedTourAnalysisBasedOnCarrierPlans);

		if (isSolvingOnlyCarrierPart()) {
			log.info("Solved small scale commercial carrier part {}/{}. Population and carrier analysis will be created by the merge step.",
				smallScaleCommercialCarrierPartIndex + 1, smallScaleCommercialCarrierPartCount);
			return 0;
		}
		SmallScaleCommercialTrafficUtils.createPlansBasedOnCarrierPlans(scenario,
			usedSmallScaleCommercialTrafficType, output, modelName, sampleName, nameOutputPopulation, numberOfPlanVariantsPerAgent);

		if (MATSimIterationsAfterDemandGeneration != null && MATSimIterationsAfterDemandGeneration >= 0) {
			log.info("Running MATSim for {} iterations after demand generation.", MATSimIterationsAfterDemandGeneration);
			Carriers carriers = CarriersUtils.addOrGetCarriers(scenario);
			carriers.getCarriers().clear();

			//this is necessary because integrated existing models can have additional vehicleTypes
			CarriersUtils.getOrAddCarrierVehicleTypes(scenario).getVehicleTypes().values().forEach(vehicleType -> {
				log.info("Adding vehicle type {} to scenario vehicles.", vehicleType.getId());
				if (!scenario.getVehicles().getVehicleTypes().containsKey(vehicleType.getId()))
					scenario.getVehicles().addVehicleType(vehicleType);
			});
			Set<String> activityTypes = new HashSet<>(
				scenario.getPopulation().getPersons().values().stream()
					.flatMap(person -> PopulationUtils.getActivities(person.getSelectedPlan(),
						TripStructureUtils.StageActivityHandling.ExcludeStageActivities).stream())
					.map(Activity::getType)
					.toList()
			);
			for (String activityType : activityTypes) {
				config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams(activityType).setTypicalDuration(30 * 60));
			}
			List<String> subpopulations = scenario.getPopulation().getPersons().values().stream()
				.map(PopulationUtils::getSubpopulation)
				.filter(Objects::nonNull)
				.toList();

			subpopulations.forEach(subpopulation -> {
				config.replanning().addStrategySettings(
					new ReplanningConfigGroup.StrategySettings().setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta).setWeight(
						0.85).setSubpopulation(subpopulation));

				config.replanning().addStrategySettings(
					new ReplanningConfigGroup.StrategySettings().setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute).setWeight(
						0.1).setSubpopulation(subpopulation));
			});

			Set<String> modes = scenario.getVehicles().getVehicleTypes().values().stream()
				.map(vehicleType -> vehicleType.getId().toString())
				.distinct().collect(Collectors.toSet());

			modes.forEach(mode -> {
				ScoringConfigGroup.ModeParams thisModeParams = new ScoringConfigGroup.ModeParams(mode);
				config.scoring().addModeParams(thisModeParams);
			});

			Set<String> qsimModes = new HashSet<>(config.qsim().getMainModes());
			config.qsim().setMainModes(Sets.union(qsimModes, modes));

			Set<String> networkModes = new HashSet<>(config.routing().getNetworkModes());
			config.routing().setNetworkModes(Sets.union(networkModes, modes));

			SimWrapper sw = SimWrapper.create();
			sw.getConfigGroup().defaultParams().setShp(null);
			sw.getConfigGroup().setDefaultDashboards(SimWrapperConfigGroup.DefaultDashboardsMode.disabled);
			sw.getConfigGroup().setSampleSize(sample);
			sw.addDashboard(new OverviewDashboard(modes));
			sw.addDashboard(new CarrierDashboard("(*.)?output_carriers_withPlans.xml.gz"));
			sw.addDashboard(new TripDashboard().setGroupsOfSubpopulationsForCommercialAnalysis("commercialPersonTraffic=commercialPersonTraffic,commercialPersonTraffic_service;smallScaleGoodsTraffic=goodsTraffic").setAnalysisArgs("--shp-filter", "none"));
			sw.addDashboard(new CommercialTrafficDashboard(config.global().getCoordinateSystem()).setGroupsOfSubpopulationsForCommercialAnalysis("commercialPersonTraffic=commercialPersonTraffic,commercialPersonTraffic_service;smallScaleGoodsTraffic=goodsTraffic"));
			sw.addDashboard(new TrafficDashboard(modes));
			Controller controller = prepareController(scenario);

			if (!RoadPricingUtils.addOrGetRoadPricingScheme(scenario).getTolledLinkIds().isEmpty()) {
				controller.addOverridingModule(new RoadPricingModule(RoadPricingUtils.addOrGetRoadPricingScheme(scenario)));
			}
			controller.addOverridingModule(new SimWrapperModule(sw));

			// Creating inject always adds check for unmaterialized config groups.
			controller.getInjector();

			// Removes check after injector has been created
			controller.getConfig().removeConfigConsistencyChecker(UnmaterializedConfigGroupChecker.class);

			controller.run();
		}
		return 0;
	}

	/**
	 * Creates a map with the different facility types per building.
	 *
	 * @param scenario complete Scenario
	 */
	private void filterFacilitiesForZones(Scenario scenario) {
		Map<String, Map<StructuralAttribute, List<Pair<ActivityFacility, Double>>>> pairsPerZone = new HashMap<>();

		scenario.getActivityFacilities().getFacilities().values().forEach(facility -> {
			Object zoneObj = facility.getAttributes().getAttribute("zone");
			if (!(zoneObj instanceof String zone) || zone.isBlank())
				return;

			Object wObj = facility.getAttributes().getAttribute("areaPerBuildingCategory");
			if (!(wObj instanceof Number wNum))
				throw new RuntimeException("The attribute 'areaPerBuildingCategory' is expected to be set for each facility and to be a number. Facility: " + facility.getId());

			double weight = wNum.doubleValue();
			if (!(weight > 0.0) || Double.isInfinite(weight))
				return;

			facility.getActivityOptions().values().forEach(opt -> {
				String activityType = opt.getType();
				// if the structural attribute is an employee, add the facility to employee and the detailed element
				if (activityType.contains(StructuralAttribute.EMPLOYEE.getLabel()))
					pairsPerZone.computeIfAbsent(zone, z -> new HashMap<>()).computeIfAbsent(StructuralAttribute.EMPLOYEE, t -> new ArrayList<>()).add(
						Pair.create(facility, weight));
				pairsPerZone.computeIfAbsent(zone, z -> new HashMap<>()).computeIfAbsent(StructuralAttribute.fromLabel(activityType).get(), t -> new ArrayList<>()).add(
					Pair.create(facility, weight));
			});
		});
		pairsPerZone.forEach((zone, byType) -> {
			Map<StructuralAttribute, EnumeratedDistribution<ActivityFacility>> distByType = facilitiesPerZoneWithProbabilities.computeIfAbsent(zone, z -> new HashMap<>());

			byType.forEach((activityType, pairs) -> {
				if (!pairs.isEmpty()) {
					distByType.put(activityType, new EnumeratedDistribution<>(rng, pairs));
				}
			});
		});
	}

	/**
	 * Validates the command line options that control the split-carrier workflow.
	 * <p>
	 * The init, part solving, and merge modes are mutually constrained: part indices must point to an existing
	 * zero-based part, merging only makes sense for more than one part, and creating the shared unsolved carrier
	 * file cannot be combined with merging already solved parts.
	 */
	private void validateCarrierPartOptions() {
		if (smallScaleCommercialCarrierPartCount < 1) {
			throw new IllegalArgumentException("--smallScaleCommercialCarrierPartCount must be at least 1.");
		}
		if (smallScaleCommercialCarrierPartIndex < 0 || smallScaleCommercialCarrierPartIndex >= smallScaleCommercialCarrierPartCount) {
			throw new IllegalArgumentException("--smallScaleCommercialCarrierPartIndex must be between 0 and --smallScaleCommercialCarrierPartCount - 1.");
		}
		if (mergeSmallScaleCommercialCarrierParts && smallScaleCommercialCarrierPartCount == 1) {
			throw new IllegalArgumentException("--smallScaleCommercialCarrierPartCount must be greater than 1 when merging small scale commercial carrier parts.");
		}
		if (createSmallScaleCommercialCarrierFileOnly && mergeSmallScaleCommercialCarrierParts) {
			throw new IllegalArgumentException("--createSmallScaleCommercialCarrierFileOnly and --mergeSmallScaleCommercialCarrierParts cannot be used together.");
		}
	}

	/**
	 * Returns whether this invocation solves exactly one carrier part.
	 * <p>
	 * In this mode the command reads the shared unsolved carrier file from the final output folder, keeps only the
	 * deterministic carrier subset for {@link #smallScaleCommercialCarrierPartIndex}, solves that subset, writes it
	 * below {@code carrierParts/part-xxx-of-yyy}, and stops before population creation.
	 */
	private boolean isSolvingOnlyCarrierPart() {
		return smallScaleCommercialCarrierPartCount > 1 && !createSmallScaleCommercialCarrierFileOnly && !mergeSmallScaleCommercialCarrierParts;
	}

	/**
	 * Resolves the output folder for the currently selected carrier part.
	 *
	 * @param finalOutput output folder of the complete small-scale commercial run
	 * @return folder where the current part writes its unsolved and solved carrier files
	 */
	private Path getCarrierPartOutputPath(Path finalOutput) {
		return finalOutput.resolve(CARRIER_PARTS_FOLDER)
			.resolve(SmallScaleCommercialTrafficUtils.getCarrierPartSuffix(smallScaleCommercialCarrierPartIndex, smallScaleCommercialCarrierPartCount));
	}

	/**
	 * Keeps only the deterministic subset of carriers assigned to the current part.
	 * <p>
	 * Carrier ids are sorted lexicographically and then distributed by {@code sortedIndex % partCount}. This keeps the
	 * split reproducible across runs and makes the merge lossless because every carrier id is assigned to exactly one
	 * part.
	 *
	 * @param scenario scenario whose carrier collection should be reduced to the selected part
	 */
	private void filterCarriersForSelectedPart(Scenario scenario) {
		if (!isSolvingOnlyCarrierPart()) {
			return;
		}
		SmallScaleCommercialTrafficUtils.filterCarriersForPart(scenario, smallScaleCommercialCarrierPartIndex, smallScaleCommercialCarrierPartCount);
	}

	/**
	 * Applies the requested jsprit iteration count to all carriers that are part of the current solve.
	 * <p>
	 * This is mainly needed when a part run starts from an unsolved carrier file that was written in the init step:
	 * the file may contain carriers without the command line iteration override, so the selected carriers are
	 * normalized before jsprit is started.
	 *
	 * @param scenario scenario containing the carriers that will be passed to jsprit
	 */
	private void ensureJspritIterationsForCarriersToSolve(Scenario scenario) {
		if (jspritIterations <= 0) {
			return;
		}
		CarriersUtils.getCarriers(scenario).getCarriers().values()
			.forEach(carrier -> CarriersUtils.setJspritIterations(carrier, jspritIterations));
	}

	/**
	 * Merges all independently solved carrier parts into the final small-scale commercial output folder.
	 * <p>
	 * The shared unsolved carrier file is an init artifact and is deliberately not touched here. The merge step only
	 * combines the solved VRP carrier files from all part runs into the final solved carrier file. This solved carrier
	 * file is then used by the main command flow to create the population and, if requested, to run MATSim iterations
	 * after demand generation.
	 *
	 * @param baseConfig config used to derive run-id-prefixed file names and carrier vehicle type locations
	 * @param finalOutput final output folder of the complete traffic type run
	 */
	private void mergeSmallScaleCommercialCarrierParts(Config baseConfig, Path finalOutput) throws MalformedURLException {
		Path carrierPartsFolder = smallScaleCommercialCarrierPartsFolder == null
			? finalOutput.resolve(CARRIER_PARTS_FOLDER)
			: smallScaleCommercialCarrierPartsFolder;
		mergeSmallScaleCommercialCarrierPartFiles(baseConfig, carrierPartsFolder, finalOutput, SOLVED_CARRIER_FILE);
	}

	/**
	 * Merges one carrier file type from every carrier part into a single carrier file.
	 * <p>
	 * The method is used for the solved VRP result. It also collects the carrier vehicle types from the part folders
	 * and writes one merged vehicle type file next to the merged carriers.
	 *
	 * @param baseConfig config used for run-id-prefixed file names and for loading the part carrier files
	 * @param carrierPartsFolder folder containing all {@code part-xxx-of-yyy} subfolders
	 * @param finalOutput output folder for the merged carrier and vehicle type files
	 * @param carrierFileName base name of the carrier file to merge
	 */
	private void mergeSmallScaleCommercialCarrierPartFiles(Config baseConfig, Path carrierPartsFolder, Path finalOutput, String carrierFileName) throws MalformedURLException {
		Path outputCarrierFile = finalOutput.resolve(SmallScaleCommercialTrafficUtils.getRunIdPrefixedFileName(baseConfig, carrierFileName));
		if (Files.exists(outputCarrierFile)) {
			throw new IllegalStateException("Merged small scale commercial carrier file already exists: " + outputCarrierFile
				+ ". Delete or move this file before running the merge again.");
		}

		Scenario mergedScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Carriers mergedCarriers = CarriersUtils.addOrGetCarriers(mergedScenario);
		for (int partIndex = 0; partIndex < smallScaleCommercialCarrierPartCount; partIndex++) {
			Path partCarrierFile = carrierPartsFolder.resolve(SmallScaleCommercialTrafficUtils.getCarrierPartSuffix(partIndex, smallScaleCommercialCarrierPartCount))
				.resolve(SmallScaleCommercialTrafficUtils.getRunIdPrefixedFileName(baseConfig, carrierFileName));
			if (!Files.exists(partCarrierFile)) {
				throw new IllegalArgumentException("Missing small scale commercial carrier part file: " + partCarrierFile);
			}
			Scenario partScenario = SmallScaleCommercialTrafficUtils.loadScenarioWithCarrierFileOnly(baseConfig, partCarrierFile, CARRIER_VEHICLE_TYPES_FILE);
			CarriersUtils.getOrAddCarrierVehicleTypes(partScenario).getVehicleTypes().forEach((vehicleTypeId, vehicleType) ->
				CarriersUtils.getOrAddCarrierVehicleTypes(mergedScenario).getVehicleTypes().putIfAbsent(vehicleTypeId, vehicleType));
			for (Carrier carrier : CarriersUtils.getCarriers(partScenario).getCarriers().values()) {
				if (mergedCarriers.getCarriers().containsKey(carrier.getId())) {
					throw new IllegalArgumentException("Duplicate carrier id while merging small scale commercial carrier parts: " + carrier.getId());
				}
				mergedCarriers.addCarrier(carrier);
			}
		}
		CarriersUtils.writeCarrierVehicleTypes(CarriersUtils.getOrAddCarrierVehicleTypes(mergedScenario),
			finalOutput.resolve(SmallScaleCommercialTrafficUtils.getRunIdPrefixedFileName(baseConfig, CARRIER_VEHICLE_TYPES_FILE)).toAbsolutePath().toString());
		CarriersUtils.writeCarriers(mergedCarriers, outputCarrierFile.toString());
		log.info("Merged {} small scale commercial carrier parts into {}.", smallScaleCommercialCarrierPartCount, outputCarrierFile);
	}

	/**
	 * Solves the generated carrier-plans and puts them into the Scenario.
	 * If a carrier has unhandled services, a carrier-replanning loop deletes the old plans and generates new plans.
	 * The new plans will then be solved and checked again.
	 * This is repeated until the carrier-plans are solved or the {@code maxNumberOfLoopsForVRPSolving} are reached.
	 * @param originalScenario complete Scenario
	 */
	private void solveVRP(Scenario originalScenario) throws Exception {

		CarriersUtils.addRoadPricingForVRPToEnsureSolutionsBasedOnNetworkModes(originalScenario);


		// comparison of results for hannover: increasing to maxServicesPerCarrier = 300 results in:
		// computationTime *4; numberOfVehicles -2%; Jsprit score: -0,4%, drivenDistanc: -4,4%
		// RE: result is to set this to maxServicesPerCarrier = 100;
		boolean splitCarrier = true;
		int maxServicesPerCarrier = 100;
		Carriers carriers = CarriersUtils.addOrGetCarriers(originalScenario);
		Map<Id<Carrier>, List<Id<Carrier>>> carrierId2subCarrierIds = new HashMap<>();

		int splitCarriers = 0;

		if (splitCarrier) {
			Map<Id<Carrier>, Carrier> subCarriersToAdd = new HashMap<>();
			List<Id<Carrier>> keyListCarrierToRemove = new ArrayList<>();
			for (Carrier carrier : carriers.getCarriers().values()) {
				if (CarriersUtils.getJspritIterations(carrier) == 0 || CarriersUtils.allJobsHandledBySelectedPlan(carrier))
					continue;
				int countedServices = 0;
				int countedVehicles = 0;
				if (carrier.getServices().size() > maxServicesPerCarrier) {
					splitCarriers++;
					int numberOfNewCarrier = (int) Math
						.ceil((double) carrier.getServices().size() / (double) maxServicesPerCarrier);
					int numberOfServicesPerNewCarrier = (int) Math
						.floor((double) carrier.getServices().size() / numberOfNewCarrier);

					int totalVehicles = carrier.getCarrierCapabilities().getCarrierVehicles().size();
					int numberOfVehiclesPerNewCarrier = totalVehicles / numberOfNewCarrier;
					int numberOfVehiclesRemainder = totalVehicles % numberOfNewCarrier;

					List<Id<Vehicle>> vehiclesForNewCarrier = new ArrayList<>(
						carrier.getCarrierCapabilities().getCarrierVehicles().keySet());
					vehiclesForNewCarrier.sort(Comparator.comparing(Id::toString));
					List<Id<CarrierService>> servicesForNewCarrier = new ArrayList<>(
						carrier.getServices().keySet());
					servicesForNewCarrier.sort(Comparator.comparing(Id::toString));

					for (int j = 0; j < numberOfNewCarrier; j++) {

						int numberOfServicesForNewCarrier = numberOfServicesPerNewCarrier;
						if (j + 1 == numberOfNewCarrier)
							numberOfServicesForNewCarrier = carrier.getServices().size() - countedServices;

						int numberOfVehiclesForNewCarrier = numberOfVehiclesPerNewCarrier;
						if (j < numberOfVehiclesRemainder)
							numberOfVehiclesForNewCarrier++;

						Carrier newCarrier = CarriersUtils.createCarrier(
							Id.create(carrier.getId().toString() + "_part_" + (j + 1), Carrier.class));
						CarrierCapabilities newCarrierCapabilities = CarrierCapabilities.Builder.newInstance()
							.setFleetSize(carrier.getCarrierCapabilities().getFleetSize()).build();
						newCarrierCapabilities.getVehicleTypes().addAll(carrier.getCarrierCapabilities().getVehicleTypes());
						newCarrierCapabilities.getCarrierVehicles().putAll(carrier.getCarrierCapabilities().getCarrierVehicles());
						newCarrier.setCarrierCapabilities(newCarrierCapabilities);
						newCarrier.getServices().putAll(carrier.getServices());
						CarriersUtils.setJspritIterations(newCarrier, CarriersUtils.getJspritIterations(carrier));
						carrier.getAttributes().getAsMap().keySet().forEach(attribute -> newCarrier.getAttributes()
							.putAttribute(attribute, carrier.getAttributes().getAttribute(attribute)));

						carrierId2subCarrierIds.putIfAbsent(carrier.getId(), new LinkedList<>());
						carrierId2subCarrierIds.get(carrier.getId()).add(newCarrier.getId());

						int fromIndexVehicles = Math.min(countedVehicles, vehiclesForNewCarrier.size());
						int fromIndexServices = Math.min(countedServices, servicesForNewCarrier.size());

						int toIndexVehicles = fromIndexVehicles + numberOfVehiclesForNewCarrier;
						int toIndexServices = fromIndexServices + numberOfServicesForNewCarrier;

						// just to be sure that the index is not out of bounds
						toIndexVehicles = Math.min(toIndexVehicles, vehiclesForNewCarrier.size());
						toIndexServices = Math.min(toIndexServices, servicesForNewCarrier.size());

						if (fromIndexVehicles == toIndexVehicles && fromIndexServices == toIndexServices) {
							throw new IllegalStateException("No remaining vehicles/services but still splitting: " + carrier.getId());
						}

						List<Id<Vehicle>> subListVehicles = vehiclesForNewCarrier.subList(fromIndexVehicles, toIndexVehicles);
						List<Id<CarrierService>> subListServices = servicesForNewCarrier.subList(fromIndexServices, toIndexServices);

						newCarrier.getCarrierCapabilities().getCarrierVehicles().keySet()
							.retainAll(subListVehicles);
						newCarrier.getServices().keySet().retainAll(subListServices);

						countedVehicles += newCarrier.getCarrierCapabilities().getCarrierVehicles().size();
						countedServices += newCarrier.getServices().size();

						subCarriersToAdd.put(newCarrier.getId(), newCarrier);
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
			log.info("Splitting carriers: {}/{}, because the maximum number of services per carriers is set to {}.", splitCarriers, carriers.getCarriers().size(),
				maxServicesPerCarrier);
			// add created parts of new carriers and delete the old ones
			carriers.getCarriers().putAll(subCarriersToAdd);
			for (Id<Carrier> id : keyListCarrierToRemove) {
				carriers.getCarriers().remove(id);
			}
			log.info("New number of carriers to solve: {}.", carriers.getCarriers().size());

		}

		// Map the values to the new subcarriers
		for (Id<Carrier> oldCarrierId : carrierId2subCarrierIds.keySet()) {
			for (Id<Carrier> newCarrierId : carrierId2subCarrierIds.get(oldCarrierId)) {
				if (carrierId2carrierAttributes.putIfAbsent(newCarrierId, carrierId2carrierAttributes.get(oldCarrierId)) != null)
					throw new Exception("CarrierAttributes already exist for the carrier " + newCarrierId.toString());
			}
		}

		log.info("Start solving {} carriers. Carriers with 0 Jsprit iterations will be skipped and carriers with existing plans will be skipped.",
			carriers.getCarriers().size());
		CarriersUtils.runJsprit(originalScenario, CarriersUtils.CarrierSelectionForSolution.solveOnlyForCarrierWithoutPlans);
		List<Carrier> nonCompleteSolvedCarriers = CarriersUtils.createListOfCarrierWithUnhandledJobs(CarriersUtils.getCarriers(originalScenario));
		if (!nonCompleteSolvedCarriers.isEmpty() && maxNumberOfLoopsForVRPSolving > 0) {
			CarriersUtils.writeCarriers(CarriersUtils.getCarriers(originalScenario),
				originalScenario.getConfig().controller().getOutputDirectory() + "/" + originalScenario.getConfig().controller().getRunId() + ".output_carriers_notCompletelySolved.xml.gz");
			unhandledServicesSolution.tryToSolveAllCarriersCompletely(originalScenario, nonCompleteSolvedCarriers);
		}

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
										 SmallScaleCommercialTrafficType smallScaleCommercialTrafficType,
										 boolean includeExistingModels, Index indexZones) throws Exception {
		ArrayList<String> modesORvehTypes;
		if (smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.goodsTraffic))
			modesORvehTypes = new ArrayList<>(
				Arrays.asList("vehTyp1", "vehTyp2", "vehTyp3", "vehTyp4", "vehTyp5"));
		else if (smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.commercialPersonTraffic))
			modesORvehTypes = new ArrayList<>(List.of("total"));
		else
			throw new Exception("Invalid traffic type selected!");

		TrafficVolumeGeneration.setInputParameters(smallScaleCommercialTrafficType);

		Map<TrafficVolumeGeneration.TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_start = TrafficVolumeGeneration
			.createTrafficVolume_start(resultingDataPerZone, output, sample, modesORvehTypes, smallScaleCommercialTrafficType);
		Map<TrafficVolumeGeneration.TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolumePerTypeAndZone_stop = TrafficVolumeGeneration
			.createTrafficVolume_stop(resultingDataPerZone, output, sample, modesORvehTypes, smallScaleCommercialTrafficType);

		if (includeExistingModels) {
			integrateExistingTrafficToSmallScaleCommercial.readExistingCarriersFromFolder(scenario, sample, indexZones);
			integrateExistingTrafficToSmallScaleCommercial.reduceDemandBasedOnExistingCarriers(scenario, indexZones, smallScaleCommercialTrafficType,
				trafficVolumePerTypeAndZone_start, trafficVolumePerTypeAndZone_stop);
		}

		NetworkUtils.cleanNetwork(scenario.getNetwork(), scenario.getConfig().qsim().getMainModes());
		if (linksPerZone == null)
			linksPerZone = filterLinksForZones(scenario, this.indexZones, facilitiesPerZoneWithProbabilities, shapeFileZoneNameColumn);

		odMatrix = createTripDistribution(trafficVolumePerTypeAndZone_start,
			trafficVolumePerTypeAndZone_stop, smallScaleCommercialTrafficType, scenario, output);
		createCarriers(scenario, smallScaleCommercialTrafficType);
	}

	/**
	 * This method prepares the config to include all resulting modes from the vehicle types.
	 * This done so late, because only after adding existing carriers the used vehicle types and modes are known.
	 *
	 * @param scenario scenario
	 */
	private static void prepareConfigForResultingModes(Scenario scenario) {
		Set<String> modes = scenario.getVehicles().getVehicleTypes().values().stream()
			.map(VehicleType::getNetworkMode).collect(Collectors.toSet());

		modes.forEach(mode -> {
			ScoringConfigGroup.ModeParams thisModeParams = new ScoringConfigGroup.ModeParams(mode);
			scenario.getConfig().scoring().addModeParams(thisModeParams);
		});

		Set<String> qsimModes = new HashSet<>(scenario.getConfig().qsim().getMainModes());
		scenario.getConfig().qsim().setMainModes(Sets.union(qsimModes, modes));

		Set<String> networkModes = new HashSet<>(scenario.getConfig().routing().getNetworkModes());
		scenario.getConfig().routing().setNetworkModes(Sets.union(networkModes, modes));
	}

	/**
	 * Reads and checks config if all necessary parameters are set.
	 */
	private Config readAndCheckConfig(String[] configArgs, Path configPath, String modelName, String sampleName, Path output) throws Exception {

		Config config = ConfigUtils.loadConfig(IOUtils.getFileUrl(configPath.toString()), configArgs);
		if (output == null || output.toString().isEmpty())
			config.controller().setOutputDirectory(Path.of(config.controller().getOutputDirectory()).resolve(modelName)
				.resolve(usedSmallScaleCommercialTrafficType.toString() + "_" + sampleName + "pct" + "_"
					+ LocalDate.now() + "_" + LocalTime.now().toSecondOfDay() + "_" + resistanceFactorsPerModelType)
				.toString());
		else
			config.controller().setOutputDirectory(output.toString());

		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		if (useRangeConstraintForTourPlanning) {
			freightCarriersConfigGroup.setUseDistanceConstraintForTourPlanning(FreightCarriersConfigGroup.UseDistanceConstraintForTourPlanning.basedOnEnergyConsumption);
			log.info("Using range constraint for tour planning based on energy consumption information in the vehicle types file.");
			freightCarriersConfigGroup.setDistanceConstraintUsableRange(distanceConstraintUsableRange);
			log.info("Distance constraint safety margin is set to {} for vehicles with energy capacity information.", distanceConstraintUsableRange);
		}
		if (freightCarriersConfigGroup.getCarriersVehicleTypesFile() != null)
			config.vehicles().setVehiclesFile(freightCarriersConfigGroup.getCarriersVehicleTypesFile());

		// Reset some config values that are not needed
		config.controller().setFirstIteration(0);
		if (MATSimIterationsAfterDemandGeneration != null)
			config.controller().setLastIteration(MATSimIterationsAfterDemandGeneration);
		config.controller().setCompressionType(ControllerConfigGroup.CompressionType.gzip);
		config.plans().setInputFile(null);
		config.transit().setTransitScheduleFile(null);
		config.transit().setVehiclesFile(null);
		config.counts().setInputFile(null);
		config.facilities().setInputFile(pathToCommercialFacilities.toString());
		config.qsim().setFlowCapFactor(sample);
		config.qsim().setStorageCapFactor(sample);
		config.qsim().setUsePersonIdForMissingVehicleId(true);
		config.timeAllocationMutator().setMutateAroundInitialEndTimeOnly(false);
		// Overwrite network
		if (network != null)
			config.network().setInputFile(network);

		// Split-carrier steps share one traffic output folder. They must not delete it when a single init, part, or merge job starts.
		if (createSmallScaleCommercialCarrierFileOnly || isSolvingOnlyCarrierPart() || mergeSmallScaleCommercialCarrierParts) {
			config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		}
		OutputDirectoryLogging.initLogging(new OutputDirectoryHierarchy(config));

		new File(Path.of(config.controller().getOutputDirectory()).resolve("calculatedData").toString()).mkdir();

		this.rnd = new Random(config.global().getRandomSeed());
		this.rng = new MersenneTwister(config.global().getRandomSeed());

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
	private Controller prepareController(Scenario scenario) {
		Controller controller = ControllerUtils.createController(scenario);
		// use overwriteExistingFiles because before setting up the OutputDirectoryHierarchy, the OverwriteFileSetting was failIfDirectoryExists
		// in mean time some files were already written (e.g. carriers analysis), so we need to allow overwriting here
		controller.getConfig().controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		controller.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort);
		return controller;
	}

	/**
	 * Creates the carriers and the related demand, based on the generated
	 * TripDistributionMatrix.
	 * @param scenario Scenario (loaded from your config), where the carriers will be put into
	 * @param smallScaleCommercialTrafficType Selected traffic types. Options: commercialPersonTraffic, goodsTraffic
	 */
	public void createCarriers(Scenario scenario,
							   SmallScaleCommercialTrafficType smallScaleCommercialTrafficType) {

		int maxNumberOfCarrier = odMatrix.getListOfPurposes().size() * odMatrix.getListOfZones().size()
			* odMatrix.getListOfModesOrVehTypes().size();
		int createdCarrier = 0;
		int fixedNumberOfVehiclePerTypeAndLocation = 1; //TODO possible improvement, perhaps check KiD

		CarrierVehicleTypes carrierVehicleTypes = CarriersUtils.getOrAddCarrierVehicleTypes(scenario);
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
		Carriers carriers = CarriersUtils.addOrGetCarriers(scenario);

		for (Integer purpose : odMatrix.getListOfPurposes()) {
			for (String startZone : odMatrix.getListOfZones()) {
				for (String modeORvehType : odMatrix.getListOfModesOrVehTypes()) {

					// Check if this purpose, startZone, modeORvehType combination is a possiblr starting location (by looking if it has a trip-distribution-entry)
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
						// Get the vehicle-types and start/stop-categories
						OdMatrixEntryInformationProvider.OdMatrixEntryInformation odMatrixEntry = odMatrixEntryInformationProvider.getOdMatrixEntryInformation(purpose, modeORvehType, smallScaleCommercialTrafficType);

						// use only types of the possibleTypes which are in the given types file
						List<String> vehicleTypes = new ArrayList<>();
						assert odMatrixEntry.possibleVehicleTypes != null: "possibleVehicleTypes is null for odMatrixEntry:" + odMatrixEntry;

						for (String possibleVehicleType : odMatrixEntry.possibleVehicleTypes) {
							if (CarriersUtils.getOrAddCarrierVehicleTypes(scenario).getVehicleTypes().containsKey(
								Id.create(possibleVehicleType, VehicleType.class)))
								vehicleTypes.add(possibleVehicleType);
						}
						if (vehicleTypes.isEmpty())
							throw new RuntimeException("The possible vehicle types found for purpose " + purpose + ", modeORvehType "
								+ modeORvehType + ", smallScaleCommercialTrafficType " + smallScaleCommercialTrafficType +" do not exist in the given vehicle types file. PLease check your input file.");
						StructuralAttribute selectedStartCategory = getSelectedStartCategory(startZone, odMatrixEntry);

						// Generate carrierName
						String carrierName = null;
						if (smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.goodsTraffic)) {
							carrierName = "Carrier_Goods_" + startZone + "_purpose_" + purpose + "_" + modeORvehType;
						} else if (smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.commercialPersonTraffic))
							carrierName = "Carrier_Business_" + startZone + "_purpose_" + purpose;

						// Create the Carrier
						CarrierCapabilities.FleetSize fleetSize = CarrierCapabilities.FleetSize.FINITE;
						ArrayList<String> vehicleDepots = new ArrayList<>();
						createdCarrier++;
						log.info("Create carrier number {} of a maximum Number of {} carriers.", createdCarrier, maxNumberOfCarrier);

						CarrierAttributes carrierAttributes = new CarrierAttributes(purpose, startZone, selectedStartCategory, modeORvehType,
							smallScaleCommercialTrafficType, vehicleDepots, odMatrixEntry);
						if(carrierId2carrierAttributes.putIfAbsent(Id.create(carrierName, Carrier.class), carrierAttributes) != null)
							throw new RuntimeException("CarrierAttributes already exist for the carrier " + carrierName);

						Carrier newCarrier = CarriersUtils.createCarrier(Id.create(carrierName, Carrier.class));
						// Now Create services for this carrier
						createServices(newCarrier, carrierAttributes);
						log.info("Carrier: {}; created services: {}", carrierName, newCarrier.getServices().size());

						createNewCarrierAndAddVehicleTypes(carrierVehicleTypes, newCarrier, carrierAttributes, vehicleTypes, fleetSize,
							fixedNumberOfVehiclePerTypeAndLocation);
						log.info("New: Carrier: {}; vehicles: {}; services: {}", carrierName, newCarrier.getCarrierCapabilities().getCarrierVehicles().size(), newCarrier.getServices().size());

						carriers.addCarrier(newCarrier);
					}
				}
			}
		}
		log.warn("The jspritIterations are now set to {} in this simulation!", jspritIterations);
		log.info("Finished creating {} carriers including related services.", createdCarrier);
	}

	/**
	 * Selects a start category for the given start zone, based on the possible start categories for this zone.
	 *
	 * @param startZone     zone where the carrier starts
	 * @param odMatrixEntry odMatrixEntry
	 * @return the selected start category
	 */
	protected StructuralAttribute getSelectedStartCategory(String startZone, OdMatrixEntryInformationProvider.OdMatrixEntryInformation odMatrixEntry) {
		// Find a start category with existing employees in this zone
		StructuralAttribute selectedStartCategory = odMatrixEntry.startCategoryDistribution.sample();
		// we start with count = 1 because the first category is already selected, and if this category has employees, we can use it.
		// Otherwise, we have to find another category.
		for (int count = 1; resultingDataPerZone.get(startZone).getDouble(selectedStartCategory) == 0; count++) {
			if (count < 4)
				selectedStartCategory = odMatrixEntry.startCategoryDistribution.sample();
			else {
				// if no possible start category with employees is found, take a random category of the stop categories,
				// the reason that no start category with employees is found is that traffic volume for employees in general is created,
				// so that it is possible that we have traffic, although we have no employees in the given start category.
				// That's why we exclude Inhabitants as a possible start category.
				selectedStartCategory = odMatrixEntry.stopCategoryDistribution.sample();
				if (selectedStartCategory.equals(StructuralAttribute.INHABITANTS))
					selectedStartCategory = odMatrixEntry.stopCategoryDistribution.sample();
				if (resultingDataPerZone.get(startZone).getDouble(selectedStartCategory) > 0)
					log.warn(
						"No possible start category with employees found for zone {}. Take a random category of the stop categories: {}. The possible start categories are: {}",
						startZone, selectedStartCategory, odMatrixEntry.startCategoryDistribution.getPmf().toString());
			}
		}
		return selectedStartCategory;
	}

	/**
	 * Generates and adds the services for the given carrier.
	 */
	private void createServices(Carrier newCarrier, CarrierAttributes carrierAttributes) {
		log.info("Create services for carrier: {}", newCarrier.getId());
		int countedServices = 0;
		for (String stopZone : odMatrix.getListOfZones()) {
			int trafficVolumeForOD = Math.round((float)odMatrix.getTripDistributionValue(carrierAttributes.startZone,
				stopZone, carrierAttributes.modeORvehType, carrierAttributes.purpose, carrierAttributes.smallScaleCommercialTrafficType));
			int numberOfJobs = (int) Math.ceil(trafficVolumeForOD / carrierAttributes.odMatrixEntry.occupancyRate);
			if (numberOfJobs == 0)
				continue;
			for (int i = 0; i < numberOfJobs; i++) {
				// find a category for the tour stop with existing employees in this zone
				StructuralAttribute selectedStopCategory = carrierAttributes.odMatrixEntry.stopCategoryDistribution.sample();
				while (resultingDataPerZone.get(stopZone).getDouble(selectedStopCategory) == 0)
					selectedStopCategory = carrierAttributes.odMatrixEntry.stopCategoryDistribution.sample();
				// additionalTravelBufferPerTourAndIterationInMinutes is only used while resolving carriers with unhandled services.
				int serviceTimePerStop = getServiceTimePerStop(carrierAttributes);
				TimeWindow serviceTimeWindow = TimeWindow.newInstance(0, 36 * 3600); // extended time window so that late tours can handle it
				createService(newCarrier, carrierAttributes.vehicleDepots, selectedStopCategory, stopZone, serviceTimePerStop, serviceTimeWindow, countedServices);
				countedServices++;
			}
		}
	}

	/**
	 * Give a service duration based on the purpose and the trafficType under a given probability
	 *
	 * @param carrierAttributes The attributes of the carrier
	 * @return The service time in seconds
	 */
	Integer getServiceTimePerStop(CarrierAttributes carrierAttributes) {
		ServiceDurationPerCategoryKey key;
		// we use the start category for the service time selection because the start category represents the employees
		if (carrierAttributes.smallScaleCommercialTrafficType().equals(SmallScaleCommercialTrafficType.commercialPersonTraffic)) {
			if (carrierAttributes.odMatrixEntry().startCategoryDistribution.getPmf().stream()
				.noneMatch(p -> p.getKey().equals(carrierAttributes.selectedStartCategory())))
				key = GenerateSmallScaleCommercialTrafficDemand.makeServiceDurationPerCategoryKey(carrierAttributes.odMatrixEntry.startCategoryDistribution.sample(), null, carrierAttributes.smallScaleCommercialTrafficType());
			else
				key = GenerateSmallScaleCommercialTrafficDemand.makeServiceDurationPerCategoryKey(carrierAttributes.selectedStartCategory, null,
					carrierAttributes.smallScaleCommercialTrafficType());
		}
		else if (carrierAttributes.smallScaleCommercialTrafficType().equals(SmallScaleCommercialTrafficType.goodsTraffic)) {
			key = GenerateSmallScaleCommercialTrafficDemand.makeServiceDurationPerCategoryKey(carrierAttributes.selectedStartCategory(),
				carrierAttributes.modeORvehType(), carrierAttributes.smallScaleCommercialTrafficType());
		} else {
			throw new RuntimeException("Unknown traffic type: " + carrierAttributes.smallScaleCommercialTrafficType());
		}
			DurationsBounds serviceDurationBounds = serviceDurationTimeSelector.get(key).sample();
			int serviceDurationLowerBound = serviceDurationBounds.minDuration();
			int serviceDurationUpperBound = serviceDurationBounds.maxDuration();
			return rnd.nextInt(serviceDurationLowerBound * 60, serviceDurationUpperBound * 60);
	}

	/**
	 * Adds a service with the given attributes to the carrier.
	 */
	private void createService(Carrier newCarrier, ArrayList<String> noPossibleLinks, StructuralAttribute selectedStopCategory, String stopZone,
							   Integer serviceTimePerStop, TimeWindow serviceTimeWindow, int i) {
		Id<Link> linkId = findPossibleLink(stopZone, selectedStopCategory, noPossibleLinks);
		Id<CarrierService> idNewService = Id.create(newCarrier.getId().toString() + "_" + linkId + "_" + (i + 1),
			CarrierService.class);

		CarrierService thisService = CarrierService.Builder.newInstance(idNewService, linkId, 0)
			.setServiceDuration(serviceTimePerStop)
			.setServiceStartingTimeWindow(serviceTimeWindow)
			.build();
		CarriersUtils.addService(newCarrier, thisService);
	}

	/**
	 * Creates the carrier and the related vehicles.
	 */
	private void createNewCarrierAndAddVehicleTypes(CarrierVehicleTypes carrierVehicleTypes, Carrier thisCarrier, CarrierAttributes carrierAttributes,
													List<String> vehicleTypes, CarrierCapabilities.FleetSize fleetSize,
													int fixedNumberOfVehiclePerTypeAndLocation) {

		if (carrierAttributes.smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.commercialPersonTraffic) && carrierAttributes.purpose == 3)
			thisCarrier.getAttributes().putAttribute("subpopulation", SubpopulationDefaultNames.SUBPOP_COM_PERSON_SERVICE);
		else if (carrierAttributes.smallScaleCommercialTrafficType.equals(SmallScaleCommercialTrafficType.goodsTraffic) )
			thisCarrier.getAttributes().putAttribute("subpopulation", SubpopulationDefaultNames.SUBPOP_GOODS);
		else
			thisCarrier.getAttributes().putAttribute("subpopulation", SubpopulationDefaultNames.SUBPOP_COM_PERSON);

		thisCarrier.getAttributes().putAttribute("purpose", carrierAttributes.purpose);
		thisCarrier.getAttributes().putAttribute("tourStartArea", carrierAttributes.startZone);
		thisCarrier.getAttributes().putAttribute("startCategory", carrierAttributes.selectedStartCategory);
		if (jspritIterations > 0)
			CarriersUtils.setJspritIterations(thisCarrier, jspritIterations);
		CarrierCapabilities carrierCapabilities = CarrierCapabilities.Builder.newInstance().setFleetSize(fleetSize).build();

		double sumServiceDurationsJobs = thisCarrier.getServices().values().stream().mapToDouble(CarrierService::getServiceDuration).sum() * factorForTravelBufferCalculation;

		double sumMaxTourDurationsOfVehicles = 0;

		while (sumMaxTourDurationsOfVehicles <= sumServiceDurationsJobs) {
			TourStartAndDuration t = tourDistribution.get(carrierAttributes.smallScaleCommercialTrafficType).sample();
			tourDistribution.get(carrierAttributes.smallScaleCommercialTrafficType).sample(2);
			int vehicleStartTime = t.getVehicleStartTime(this.rnd);
			int tourDuration = t.getVehicleTourDuration(this.rnd);
			int vehicleEndTime = vehicleStartTime + tourDuration;
			Id<Link> linkId = findPossibleLink(carrierAttributes.startZone, carrierAttributes.selectedStartCategory, null);
			for (String thisVehicleType : vehicleTypes) { //TODO Flottenzusammensetzung anpassen. Momentan pro Depot alle Fahrzeugtypen 1x erzeugen
				VehicleType thisType = carrierVehicleTypes.getVehicleTypes()
					.get(Id.create(thisVehicleType, VehicleType.class));
				if (fixedNumberOfVehiclePerTypeAndLocation == 0)
					fixedNumberOfVehiclePerTypeAndLocation = 1;
				for (int i = 0; i < fixedNumberOfVehiclePerTypeAndLocation; i++) {
					sumMaxTourDurationsOfVehicles += tourDuration;
					CarrierVehicle newCarrierVehicle = CarrierVehicle.Builder.newInstance(
						Id.create(thisCarrier.getId().toString() + "_" + (carrierCapabilities.getCarrierVehicles().size() + 1), Vehicle.class),
						linkId, thisType).setEarliestStart(vehicleStartTime).setLatestEnd(vehicleEndTime).build();
					carrierCapabilities.getCarrierVehicles().put(newCarrierVehicle.getId(), newCarrierVehicle);
					if (!carrierCapabilities.getVehicleTypes().contains(thisType)) carrierCapabilities.getVehicleTypes().add(thisType);
				}
			}
			thisCarrier.setCarrierCapabilities(carrierCapabilities);
		}
	}

	/**
	 * Finds a possible link for a service or the vehicle location.
	 */
	Id<Link> findPossibleLink(String zone, StructuralAttribute selectedCategory, List<String> noPossibleLinks) {
		Id<Link> newLink = null;
		for (int a = 0; newLink == null && a < facilitiesPerZoneWithProbabilities.get(zone).get(selectedCategory).getPmf().size() * 2; a++) {

			ActivityFacility possibleBuilding = facilitiesPerZoneWithProbabilities.get(zone).get(selectedCategory).sample();
			Coord centroidPointOfBuildingPolygon = possibleBuilding.getCoord();

			int numberOfPossibleLinks = linksPerZone.get(zone).size();

			// searches and selects the nearest link of the possible links in this zone
			newLink = SmallScaleCommercialTrafficUtils.findNearestPossibleLink(zone, noPossibleLinks, linksPerZone, null,
				centroidPointOfBuildingPolygon, numberOfPossibleLinks);
		}
		if (newLink == null)
			throw new RuntimeException("No possible link for buildings with type '" + selectedCategory.getLabel() + "' in zone '"
				+ zone + "' found. buildings in category: " + facilitiesPerZoneWithProbabilities.get(zone).get(selectedCategory)
				+ "; possibleLinks in zone: " + linksPerZone.get(zone).size());
		return newLink;
	}

	/**
	 * Filters links by used modes and creates Map with all links in each zone
	 */
	static Map<String, Map<Id<Link>, Link>> filterLinksForZones(Scenario scenario, Index indexZones,
																Map<String, Map<StructuralAttribute, EnumeratedDistribution<ActivityFacility>>> facilitiesPerZoneWithProbabilities,
																String shapeFileZoneNameColumn) {
		Map<String, Map<Id<Link>, Link>> linksPerZone = new HashMap<>();
		log.info("Filtering and assign links to zones. This take some time...");

		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
		Set<String> modes = new HashSet<>(scenario.getConfig().routing().getNetworkModes());
		Network filteredNetwork = NetworkUtils.createNetwork(scenario.getConfig().network());
		filter.filter(filteredNetwork, modes);

		CoordinateTransformation ct = indexZones.getShp().createTransformation(ProjectionUtils.getCRS(scenario.getNetwork()));
		NetworkTransform nT = new NetworkTransform(ct);
		nT.run(filteredNetwork);
		List<Link> links = new ArrayList<>(filteredNetwork.getLinks().values());
		links.forEach(l -> l.getAttributes().putAttribute("zone", indexZones.query(l.getCoord())));
		links = links.stream().filter(l -> l.getAttributes().getAttribute("zone") != null).toList();
		links.forEach(l -> linksPerZone
			.computeIfAbsent((String) l.getAttributes().getAttribute("zone"), (k) -> new HashMap<>())
			.put(l.getId(), l));
		if (linksPerZone.size() != indexZones.size())
			findNearestLinkForZonesWithoutLinks(filteredNetwork, linksPerZone, indexZones, facilitiesPerZoneWithProbabilities, shapeFileZoneNameColumn);

		return linksPerZone;
	}

	/**
	 * Finds for areas without links the nearest Link if the area contains any building.
	 */
	private static void findNearestLinkForZonesWithoutLinks(Network networkToChange, Map<String, Map<Id<Link>, Link>> linksPerZone,
															Index shpZones,
															Map<String, Map<StructuralAttribute, EnumeratedDistribution<ActivityFacility>>> facilitiesPerZoneWithProbabilities,
															String shapeFileZoneNameColumn) {
		for (SimpleFeature singleArea : shpZones.getAllFeatures()) {
			String zoneID = (String) singleArea.getAttribute(shapeFileZoneNameColumn);
			if (!linksPerZone.containsKey(zoneID) && facilitiesPerZoneWithProbabilities.get(zoneID) != null) {
				for (EnumeratedDistribution<ActivityFacility> buildingPairsList : facilitiesPerZoneWithProbabilities.get(zoneID).values()) {
					for (Pair<ActivityFacility, Double> buildingPair : buildingPairsList.getPmf()) {
						Link l = NetworkUtils.getNearestLinkExactly(networkToChange, buildingPair.getKey().getCoord());
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
	 * Reads the scenario network once more as a static network for the OD resistance precomputation.
	 * <p>
	 * The main scenario network may be time-dependent. The OD resistance values are static, so this method loads only
	 * the base network file with the default non-time-variant link factory and deliberately does not read network change
	 * events.
	 */
	private static Network readStaticNetworkForResistancePrecompute(Scenario scenario) {
		Network staticNetwork = NetworkUtils.createNetwork();
		new MatsimNetworkReader(ProjectionUtils.getCRS(scenario.getNetwork()), scenario.getConfig().global().getCoordinateSystem(), staticNetwork)
			.readURL(scenario.getConfig().network().getInputFileURL(scenario.getConfig().getContext()));
		return staticNetwork;
	}

	/**
	 * Creates the number of trips between the zones for each mode and purpose.
	 */
	private TripDistributionMatrix createTripDistribution(
		Map<TrafficVolumeGeneration.TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_start,
		Map<TrafficVolumeGeneration.TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_stop,
		SmallScaleCommercialTrafficType smallScaleCommercialTrafficType, Scenario scenario, Path output)
		throws Exception {

		ArrayList<String> listOfZones = new ArrayList<>();
		trafficVolume_start.forEach((k, v) -> {
			if (!listOfZones.contains(k.zone()))
				listOfZones.add(k.zone());
		});
		final TripDistributionMatrix odMatrix = TripDistributionMatrix.Builder
			.newInstance(indexZones, shapeFileZoneNameColumn, trafficVolume_start, trafficVolume_stop, smallScaleCommercialTrafficType, listOfZones).build();
		Network staticNetworkForODGeneration = readStaticNetworkForResistancePrecompute(scenario);
		log.info("Create trip distribution for traffic type {} with resistance factor {}.", smallScaleCommercialTrafficType, resistanceFactorsPerModelType.get(smallScaleCommercialTrafficType).toString());
		// Route all zone-pair resistance values on a static network before the OD loop so later matrix work only reads cached values.
		odMatrix.precomputeResistanceFunctionValues(staticNetworkForODGeneration, linksPerZone, resistanceFactorsPerModelType.get(smallScaleCommercialTrafficType));
		List<TrafficVolumeGeneration.TrafficVolumeKey> trafficVolumeKeys = new ArrayList<>(trafficVolume_start.keySet());
		List<String> usedModesORvehTypes = trafficVolumeKeys.stream()
			.map(TrafficVolumeGeneration.TrafficVolumeKey::modeORvehType).distinct().sorted().toList();
		List<Integer> usedPurposes = trafficVolume_start.values().stream().flatMap(
			purposeVolumes -> purposeVolumes.keySet().stream()).distinct().sorted().toList();

		Counter ODcounter = new Counter("OD destination slice # ",
			" of " + usedModesORvehTypes.size() * usedPurposes.size() * listOfZones.size() + " processed.");
		for (String modeORvehType : usedModesORvehTypes) {
			List<TrafficVolumeGeneration.TrafficVolumeKey> startKeysForMode = trafficVolumeKeys.stream()
				.filter(trafficVolumeKey -> trafficVolumeKey.modeORvehType().equals(modeORvehType))
				.toList();
			for (Integer purpose : usedPurposes) {
				List<String> shuffledStopZones = new ArrayList<>(listOfZones);
				Collections.shuffle(shuffledStopZones, rnd);
				for (String stopZone : shuffledStopZones) {
					List<TrafficVolumeGeneration.TrafficVolumeKey> shuffledStartKeys = new ArrayList<>(startKeysForMode);
					Collections.shuffle(shuffledStartKeys, rnd);
					for (TrafficVolumeGeneration.TrafficVolumeKey trafficVolumeKey : shuffledStartKeys) {
						odMatrix.setTripDistributionValue(trafficVolumeKey.zone(), stopZone, modeORvehType, purpose,
							smallScaleCommercialTrafficType, staticNetworkForODGeneration, linksPerZone,
							resistanceFactorsPerModelType.get(smallScaleCommercialTrafficType));
					}
					ODcounter.incCounter();
				}
			}
		}
		odMatrix.clearRoundingError(rnd);
		odMatrix.writeODMatrices(output, smallScaleCommercialTrafficType);
		return odMatrix;
	}

	public Map<SmallScaleCommercialTrafficType, EnumeratedDistribution<TourStartAndDuration>> getTourDistribution() {
		return tourDistribution;
	}

	public Map<ServiceDurationPerCategoryKey, EnumeratedDistribution<DurationsBounds>> getServiceDurationTimeSelector() {
		return serviceDurationTimeSelector;
	}

	public Map<Id<Carrier>, CarrierAttributes> getCarrierId2carrierAttributes() {
		return carrierId2carrierAttributes;
	}

	public int getMaxNumberOfLoopsForVRPSolving(){
		return maxNumberOfLoopsForVRPSolving;
	}

	public int getAdditionalTravelBufferPerTourAndIterationInMinutes(){
		return additionalTravelBufferPerTourAndIterationInMinutes;
	}
	public double getFactorForTravelBufferCalculation(){
		return factorForTravelBufferCalculation;
	}
	public record ServiceDurationPerCategoryKey(StructuralAttribute employeeCategory, String vehicleType, SmallScaleCommercialTrafficType smallScaleCommercialTrafficType) {}

	public static ServiceDurationPerCategoryKey makeServiceDurationPerCategoryKey(StructuralAttribute employeeCategory, String vehicleType, SmallScaleCommercialTrafficType smallScaleCommercialTrafficType) {
		return new ServiceDurationPerCategoryKey(employeeCategory, vehicleType, smallScaleCommercialTrafficType);
	}

	public record TourStartAndDuration(int hourLower, int hourUpper, double minDuration, double maxDuration) {
		/**
		 * Gives a duration for the created tour under the given probability.
		 */
		public int getVehicleTourDuration(Random rnd) {
			if (minDuration == 0.)
				return (int) maxDuration() * 60;
			else
				return (int) rnd.nextDouble(minDuration * 60, maxDuration * 60);
		}

		/**
		 * Gives a tour start time for the created tour under the given probability.
		 */
		public int getVehicleStartTime(Random rnd) {
			return rnd.nextInt(hourLower * 3600, hourUpper * 3600);
		}
	}

	public record DurationsBounds(int minDuration, int maxDuration) {}

	/**
	 * The attributes of a carrier, used during the generation
	 * @param purpose purpose of this carrier denoted as an index. Can be used in {@link OdMatrixEntryInformationProvider} to get more information about this carrier.
	 * @param startZone start zone of this carrier, entry from {@link TripDistributionMatrix#getListOfZones()}
	 * @param selectedStartCategory start category of this carrier, selected randomly from
	 * @param modeORvehType entry from {@link TripDistributionMatrix#getListOfModesOrVehTypes()}
	 * @param smallScaleCommercialTrafficType Entry from {@link SmallScaleCommercialTrafficType} for this carrier
	 *                                        <i>(NOTE: This value only differs between carriers if {@link SmallScaleCommercialTrafficType#completeSmallScaleCommercialTraffic is selected)</i>
	 * @param vehicleDepots Containing the depots of this carrier with linkIds as strings
	 */
	public record CarrierAttributes(int purpose, String startZone, StructuralAttribute selectedStartCategory, String modeORvehType,
									SmallScaleCommercialTrafficType smallScaleCommercialTrafficType, ArrayList<String> vehicleDepots,
									OdMatrixEntryInformationProvider.OdMatrixEntryInformation odMatrixEntry) {}
}
