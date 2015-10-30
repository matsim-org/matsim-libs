package gunnar.ihop2.integration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import cadyts.utilities.misc.StreamFlushHandler;
import floetteroed.utilities.SimpleLogFormatter;
import floetteroed.utilities.Time;
import floetteroed.utilities.config.Config;
import floetteroed.utilities.config.ConfigReader;
import gunnar.ihop2.utils.StringAsIntegerComparator;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RunScaper {

	public static final String IHOP2_ELEMENT = "ihop2";

	public static final String MATSIMCONFIG_FILENAME_ELEMENT = "matsimconfig";

	public static final String ZONESHAPE_FILENAME_ELEMENT = "zoneshapefile";

	public static final String BUILDINGSHAPE_FILENAME_ELEMENT = "buildingshapefile";

	public static final String REGENTPOPULATIONSAMPLE_ELEMENT = "regentpopulationsample";

	public static final String MATSIMPOPULATIONSUBSAMPLE_ELEMENT = "matsimpopulationsubsample";

	public static final String POPULATION_ATTRIBUTE_FILENAME_ELEMENT = "population";

	public static final String LINKATTRIBUTE_FILENAME_ELEMENT = "linkattributefile";

	public static final String DEMANDMODEL_ELEMENT = "demandmodel";

	public enum DEMANDMODEL {
		regent, scaper
	};

	public static final String TRAVELTIME_MATRIX_FILENAME_ELEMENT = "traveltimes";

	public static final String REGENT_FOLDER_ELEMENT = "regentfolder";

	public static final String REGENT_COMMAND_ELEMENT = "regentcommand";

	public static final String ITERATIONS_ELEMENT = "iterations";

	public static final String ZONE_ELEMENT = "zone";

	public static final String RANDOMSEED_ELEMENT = "randomseed";

	public static final String ANALYSIS_STARTTIME_ELEMENT = "analysisstarttime";

	public static final String ANALYSIS_BINSIZE_ELEMENT = "analysisbinsize";

	public static final String ANALYSIS_BINCOUNT_ELEMENT = "analysisbincount";

	public static final String NODESAMPLE_SIZE_ELEMENT = "nodesamplesize";

	public static void fatal(final String msg) {
		Logger.getLogger(RunScaper.class.getName()).severe(
				"FATAL ERROR: " + msg);
		System.exit(-1);
	}

	public static void fatal(final Exception e) {
		Logger.getLogger(RunScaper.class.getName()).severe("FATAL ERROR: " + e);
		e.printStackTrace();
		System.exit(-1);
	}

	private static void checkNonNull(final String fileName,
			final String definition) {
		if (fileName == null) {
			Logger.getLogger(RunScaper.class.getName()).severe(
					definition + " is null.");
		}
	}

	private static void checkFileExistence(final String fileName,
			final String definition) {
		if (!(new File(fileName)).exists()) {
			Logger.getLogger(RunScaper.class.getName()).severe(
					definition + " \"" + fileName + "\" could not be found.");
		}
	}

	public static void main(String[] args) {

		System.out.println("STARTED");
		System.out.println();

		/*
		 * -------------------- INITIALIZE LOGGING --------------------
		 */

		final Logger logger = Logger.getLogger("");
		logger.setUseParentHandlers(false);
		for (Handler h : logger.getHandlers()) {
			h.flush();
			if (h instanceof FileHandler) { // don't close the console stream
				h.close();
			}
			logger.removeHandler(h);
		}

		final StreamFlushHandler stdOutHandler = new StreamFlushHandler(
				System.out, new SimpleLogFormatter("IHOP2 "));
		logger.addHandler(stdOutHandler);

		try {
			final FileHandler fileHandler = new FileHandler("./log.txt", false);
			fileHandler.setFormatter(new SimpleLogFormatter(null));
			logger.addHandler(fileHandler);
		} catch (IOException e) {
			logger.warning("unable to create integration log file");
		}

		logger.setLevel(Level.INFO);
		for (Handler h : logger.getHandlers()) {
			h.setLevel(Level.INFO);
		}

		/*
		 * -------------------- CONFIGURE --------------------
		 */

		Logger.getLogger(RunScaper.class.getName()).info(
				"Checking program parameters ... ");

		final String configFileName = args[0];
		checkNonNull(configFileName, "config");
		checkFileExistence(configFileName, "config file");

		final ConfigReader configReader = new ConfigReader();
		final Config config = configReader.read(configFileName);

		final String matsimConfigFileName = config.get(IHOP2_ELEMENT,
				MATSIMCONFIG_FILENAME_ELEMENT);
		checkNonNull(matsimConfigFileName, "matsimconfig file name");
		checkFileExistence(matsimConfigFileName, "matsimconfig file");
		Logger.getLogger(RunScaper.class.getName()).info(
				MATSIMCONFIG_FILENAME_ELEMENT + " = " + matsimConfigFileName);

		final String zoneShapeFileName = config.get(IHOP2_ELEMENT,
				ZONESHAPE_FILENAME_ELEMENT);
		checkNonNull(zoneShapeFileName, "zone shapefile name");
		checkFileExistence(zoneShapeFileName, "zone shapefile");
		Logger.getLogger(RunScaper.class.getName()).info(
				ZONESHAPE_FILENAME_ELEMENT + " = " + zoneShapeFileName);

		final String buildingShapeFileName = config.get(IHOP2_ELEMENT,
				BUILDINGSHAPE_FILENAME_ELEMENT);
		checkNonNull(buildingShapeFileName, "building shapefile name");
		checkFileExistence(buildingShapeFileName, "building shapefile");
		Logger.getLogger(RunScaper.class.getName()).info(
				BUILDINGSHAPE_FILENAME_ELEMENT + " = " + buildingShapeFileName);

		double regentPopulationSample;
		try {
			regentPopulationSample = Double.parseDouble(config.get(
					IHOP2_ELEMENT, REGENTPOPULATIONSAMPLE_ELEMENT));
		} catch (Exception e) {
			regentPopulationSample = Double.NaN;
			fatal("Could not read ihop2 configuration element "
					+ REGENTPOPULATIONSAMPLE_ELEMENT);
		}
		Logger.getLogger(RunScaper.class.getName())
				.info(REGENTPOPULATIONSAMPLE_ELEMENT + " = "
						+ regentPopulationSample);

		double matsimPopulationSubSample;
		try {
			matsimPopulationSubSample = Double.parseDouble(config.get(
					IHOP2_ELEMENT, MATSIMPOPULATIONSUBSAMPLE_ELEMENT));
		} catch (Exception e) {
			matsimPopulationSubSample = Double.NaN;
			fatal("Could not read ihop2 configuration element "
					+ MATSIMPOPULATIONSUBSAMPLE_ELEMENT);
		}
		Logger.getLogger(RunScaper.class.getName()).info(
				MATSIMPOPULATIONSUBSAMPLE_ELEMENT + " = "
						+ matsimPopulationSubSample);

		final String populationFileName = config.get(IHOP2_ELEMENT,
				POPULATION_ATTRIBUTE_FILENAME_ELEMENT);
		checkNonNull(populationFileName, "population file name");
		checkFileExistence(populationFileName, "population file");
		Logger.getLogger(RunScaper.class.getName()).info(
				POPULATION_ATTRIBUTE_FILENAME_ELEMENT + " = "
						+ populationFileName);

		final String linkAttributeFileName = config.get(IHOP2_ELEMENT,
				LINKATTRIBUTE_FILENAME_ELEMENT);
		checkNonNull(linkAttributeFileName, "linkattribute file name");
		checkFileExistence(linkAttributeFileName, "linkattribute file");
		Logger.getLogger(RunScaper.class.getName()).info(
				LINKATTRIBUTE_FILENAME_ELEMENT + " = " + linkAttributeFileName);

		final String demandModelName = config.get(IHOP2_ELEMENT,
				DEMANDMODEL_ELEMENT).toLowerCase();
		DEMANDMODEL demandModel = null;
		try {
			demandModel = DEMANDMODEL.valueOf(demandModelName);
		} catch (IllegalArgumentException e) {
			fatal("Demand model \"" + demandModelName + "\" is unknown.");
		}
		Logger.getLogger(RunScaper.class.getName()).info(
				DEMANDMODEL_ELEMENT + " = " + demandModel);

		final String traveltimesFileName = config.get(IHOP2_ELEMENT,
				TRAVELTIME_MATRIX_FILENAME_ELEMENT);
		checkNonNull(traveltimesFileName, "traveltimes file name");
		Logger.getLogger(RunScaper.class.getName()).info(
				TRAVELTIME_MATRIX_FILENAME_ELEMENT + " = "
						+ traveltimesFileName);

		final String regentFolder = config.get(IHOP2_ELEMENT,
				REGENT_FOLDER_ELEMENT);
		checkNonNull(regentFolder, "regent folder file name");
		checkFileExistence(regentFolder, "regent folder");
		Logger.getLogger(RunScaper.class.getName()).info(
				REGENT_FOLDER_ELEMENT + " = " + regentFolder);

		final String regentCommand = config.get(IHOP2_ELEMENT,
				REGENT_COMMAND_ELEMENT);
		checkNonNull(regentCommand, "regent command");
		Logger.getLogger(RunScaper.class.getName()).info(
				REGENT_COMMAND_ELEMENT + " = " + regentCommand);

		Integer maxIterations = null;
		try {
			maxIterations = Integer.parseInt(config.get(IHOP2_ELEMENT,
					ITERATIONS_ELEMENT));
		} catch (NumberFormatException e) {
			fatal("could not read the " + ITERATIONS_ELEMENT + " XML element.");
		}
		Logger.getLogger(RunScaper.class.getName()).info(
				ITERATIONS_ELEMENT + " = " + maxIterations);

		final List<String> zoneIDs = config
				.getList(IHOP2_ELEMENT, ZONE_ELEMENT);
		if (zoneIDs == null) {
			fatal("could not read the " + ZONE_ELEMENT + " XML element.");
			System.exit(-1);
		} else if (zoneIDs.size() == 0) {
			fatal("there are no zones defined in the xml file.");
		}
		Collections.sort(zoneIDs, new StringAsIntegerComparator());
		Logger.getLogger(RunScaper.class.getName()).info(
				"Number of relevant zones: " + zoneIDs.size());

		final String randomSeedStr = config.get(IHOP2_ELEMENT,
				RANDOMSEED_ELEMENT);
		Random rnd;
		if (randomSeedStr != null) {
			try {
				final Long randomSeed = Long.parseLong(randomSeedStr);
				rnd = new Random(randomSeed);
			} catch (NumberFormatException e) {
				rnd = null;
				fatal("could not read the " + ITERATIONS_ELEMENT
						+ " XML element.");
			}
		} else {
			Logger.getLogger(RunScaper.class.getName()).info(
					"no random seed specified");
			rnd = new Random();
		}
		Logger.getLogger(RunScaper.class.getName()).info(
				RANDOMSEED_ELEMENT + " = " + randomSeedStr);

		Integer analysisStartTime_s = null;
		try {
			analysisStartTime_s = Time.secFromStr(config.get(IHOP2_ELEMENT,
					ANALYSIS_STARTTIME_ELEMENT));
		} catch (Exception e) {
			fatal("could not read the " + ANALYSIS_STARTTIME_ELEMENT
					+ " XML element.");
		}
		Logger.getLogger(RunScaper.class.getName()).info(
				ANALYSIS_STARTTIME_ELEMENT + " = " + analysisStartTime_s);

		Integer analysisBinSize_s = null;
		try {
			analysisBinSize_s = Time.secFromStr(config.get(IHOP2_ELEMENT,
					ANALYSIS_BINSIZE_ELEMENT));
		} catch (Exception e) {
			fatal("could not read the " + ANALYSIS_BINSIZE_ELEMENT
					+ " XML element.");
		}
		Logger.getLogger(RunScaper.class.getName()).info(
				ANALYSIS_BINSIZE_ELEMENT + " = " + analysisBinSize_s);

		Integer analysisBinCnt = null;
		try {
			analysisBinCnt = Integer.parseInt(config.get(IHOP2_ELEMENT,
					ANALYSIS_BINCOUNT_ELEMENT));
		} catch (NumberFormatException e) {
			fatal("could not read the " + ANALYSIS_BINCOUNT_ELEMENT
					+ " XML element.");
		}
		Logger.getLogger(RunScaper.class.getName()).info(
				ANALYSIS_BINCOUNT_ELEMENT + " = " + analysisBinCnt);

		Integer nodeSampleSize = null;
		try {
			nodeSampleSize = Integer.parseInt(config.get(IHOP2_ELEMENT,
					NODESAMPLE_SIZE_ELEMENT));
		} catch (NumberFormatException e) {
			fatal("could not read the " + NODESAMPLE_SIZE_ELEMENT
					+ " XML element.");
		}
		Logger.getLogger(RunScaper.class.getName()).info(
				NODESAMPLE_SIZE_ELEMENT + " = " + nodeSampleSize);

		Logger.getLogger(RunScaper.class.getName()).info(
				"... program parameters appear OK so far.");

		/*
		 * -------------------- PERMANENT CONFIGURATIONS --------------------
		 */

		/*
		 * ==================== OUTER ITERATIONS ====================
		 */

		// for (int iteration = 1; iteration <= maxIterations; iteration++) {
		//
		// Logger.getLogger(RunScaper.class.getName()).info(
		// "---------- STARTING ITERATION " + iteration + " OF "
		// + maxIterations + " ----------");
		//
		// /*
		// * -------------------- LOAD CONFIGURATION --------------------
		// */
		//
		// Logger.getLogger(RunScaper.class.getName()).info(
		// "Loading matsim configuration file: "
		// + matsimConfigFileName + " ... ");
		// final org.matsim.core.config.Config matsimConfig = ConfigUtils
		// .loadConfig(matsimConfigFileName);
		// final String matsimNetworkFileName = matsimConfig.getModule(
		// "network").getValue("inputNetworkFile");
		// final String initialPlansFileName = matsimConfig.getModule("plans")
		// .getValue("inputPlansFile");
		//
		// /*
		// * -------------------- CREATE POPULATION --------------------
		// */
		// Logger.getLogger(RunScaper.class.getName()).info(
		// "Creating MATSim population ... ");
		//
		// if (DEMANDMODEL.regent.equals(demandModel)) {
		// final PopulationCreator populationCreator = new PopulationCreator(
		// matsimNetworkFileName, zoneShapeFileName,
		// StockholmTransformationFactory.WGS84_EPSG3857,
		// populationFileName);
		// populationCreator.setBuildingsFileName(buildingShapeFileName);
		// populationCreator
		// .setPopulationSampleFactor(matsimPopulationSubSample);
		// final ObjectAttributes linkAttributes = new ObjectAttributes();
		// final ObjectAttributesXmlReader reader = new
		// ObjectAttributesXmlReader(
		// linkAttributes);
		// reader.parse(linkAttributeFileName);
		// Logger.getLogger(RunScaper.class.getName()).warning(
		// "Removing all expanded links. This *should* have no "
		// + "effect if a non-expanded network is used.");
		// populationCreator.removeExpandedLinks(linkAttributes);
		// try {
		// populationCreator.run(initialPlansFileName);
		// } catch (FileNotFoundException e1) {
		// throw new RuntimeException(e1);
		// }
		// } else if (DEMANDMODEL.scaper.equals(demandModel)) {
		//
		// final ScaperPopulationCreator reader = new ScaperPopulationCreator(
		// matsimNetworkFileName, zoneShapeFileName,
		// StockholmTransformationFactory.WGS84_EPSG3857,
		// populationFileName);
		// PopulationWriter popwriter = new PopulationWriter(
		// reader.scenario.getPopulation(), null);
		// popwriter.write(initialPlansFileName);
		//
		// }
		//
		// Logger.getLogger(RunScaper.class.getName()).info(
		// "... succeeded to create population.");
		//
		// /*
		// * -------------------- RUN MATSIM --------------------
		// */
		//
		// Logger.getLogger(RunScaper.class.getName()).info(
		// "Running MATSim ...");
		//
		// Controler controler = new Controler(matsimConfig);
		//
		// matsimConfig.getModule("qsim").addParam(
		// "flowCapacityFactor",
		// Double.toString(regentPopulationSample
		// * matsimPopulationSubSample));
		// matsimConfig.getModule("qsim").addParam(
		// "storageCapacityFactor",
		// Double.toString(regentPopulationSample
		// * matsimPopulationSubSample));
		// matsimConfig.planCalcScore().setWriteExperiencedPlans(true);
		// matsimConfig.getModule("controler").addParam("overwriteFiles",
		// "deleteDirectoryIfExists");
		//
		// controler.run();
		//
		// Logger.getLogger(RunScaper.class.getName()).info(
		// "... MATSim run completed.");
		//
		// /*
		// * -------------------- WRITE TRAVELTIME MATRIX --------------------
		// */
		//
		// Logger.getLogger(RunScaper.class.getName()).info(
		// "Computing time-dependent travel times ...");
		//
		// // TODO this was already created in the population generation
		// final ZonalSystem zonalSystem = new ZonalSystem(zoneShapeFileName,
		// StockholmTransformationFactory.WGS84_EPSG3857);
		// zonalSystem.addNetwork(controler.getScenario().getNetwork(),
		// StockholmTransformationFactory.WGS84_SWEREF99);
		//
		// final TravelTimeMatrices travelTimeMatrices = new TravelTimeMatrices(
		// controler.getScenario().getNetwork(),
		// controler.getLinkTravelTimes(),
		// new LinkedHashSet<>(zoneIDs), zonalSystem, rnd,
		// analysisStartTime_s, analysisBinSize_s, analysisBinCnt,
		// nodeSampleSize);
		//
		// if (DEMANDMODEL.regent.equals(demandModel)) {
		//
		// Logger.getLogger(RunScaper.class.getName()).info(
		// "Computing tour travel times ...");
		//
		// // TODO make sure the population uses the experienced travel
		// // times.
		// final TourTravelTimes tourTravelTimes = new TourTravelTimes(
		// controler.getScenario(), travelTimeMatrices);
		// tourTravelTimes.writeTourTravelTimesToFile(traveltimesFileName);
		// tourTravelTimes
		// .writeHistogramsToFile("./departure-time-histograms.txt");
		//
		// } else if (DEMANDMODEL.scaper.equals(demandModel)) {
		//
		// travelTimeMatrices.writeToScaperFiles(traveltimesFileName);
		//
		// }
		//
		// Logger.getLogger(RunScaper.class.getName()).info(
		// "... succeeded to write traveltime matrices to file: "
		// + traveltimesFileName);
		//
		// /*
		// * -------------------- RUN REGENT --------------------
		// */

		Logger.getLogger(RunScaper.class.getName()).info(
				"Running demand model: " + regentCommand + " ...");

		final Process proc;
		final int exitVal;
		try {
			proc = Runtime.getRuntime().exec(regentCommand, null,
					new File(regentFolder));
			exitVal = proc.waitFor();
			if (exitVal != 0) {
				fatal("Scaper terminated with exit code " + exitVal + ".");
			}
		} catch (Exception e) {
			fatal(e);
		}

		Logger.getLogger(RunScaper.class.getName()).info(
				"... succeeded to run Scaper");
		// }

		Logger.getLogger(RunScaper.class.getName()).info("DONE");
		System.out.println("... DONE");
	}
}
