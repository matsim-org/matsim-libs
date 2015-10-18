package gunnar.ihop2.integration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.utils.objectattributes.ObjectAttributeUtils2;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import saleem.stockholmscenario.utils.StockholmTransformationFactory;
import cadyts.utilities.misc.StreamFlushHandler;
import floetteroed.utilities.SimpleLogFormatter;
import floetteroed.utilities.config.Config;
import floetteroed.utilities.config.ConfigReader;
import gunnar.ihop2.regent.costwriting.TravelTimeMatrices;
import gunnar.ihop2.regent.demandreading.PopulationCreator;
import gunnar.ihop2.regent.demandreading.ZonalSystem;

public class MATSimDummy {

	public static final String IHOP2_ELEMENT = "ihop2";

	public static final String MATSIMCONFIG_FILENAME_ELEMENT = "matsimconfig";

	public static final String ZONESHAPE_FILENAME_ELEMENT = "zoneshapefile";

	public static final String BUILDINGSHAPE_FILENAME_ELEMENT = "buildingshapefile";

	public static final String REGENTPOPULATIONSAMPLE_ELEMENT = "regentpopulationsample";

	public static final String MATSIMPOPULATIONSUBSAMPLE_ELEMENT = "matsimpopulationsubsample";

	public static final String POPULATION_ATTRIBUTE_FILENAME_ELEMENT = "population";

	public static final String LINKATTRIBUTE_FILENAME_ELEMENT = "linkattributefile";

	public static final String TRAVELTIME_MATRIX_FILENAME_ELEMENT = "traveltimes";

	// TODO CONTINUE HERE

	public static final String REGENT_FOLDER_ELEMENT = "regentfolder";

	public static final String REGENT_COMMAND_ELEMENT = "regentcommand";

	public static final String ITERATIONS_ELEMENT = "iterations";

	public static final String ZONE_ELEMENT = "zone";

	private static void fatal(final String msg) {
		Logger.getLogger(MATSimDummy.class.getName()).severe(
				"FATAL ERROR: " + msg);
		System.exit(-1);
	}

	private static void fatal(final Exception e) {
		Logger.getLogger(MATSimDummy.class.getName()).severe(
				"FATAL ERROR: " + e);
		e.printStackTrace();
		System.exit(-1);
	}

	private static void checkNonNull(final String fileName,
			final String definition) {
		if (fileName == null) {
			Logger.getLogger(MATSimDummy.class.getName()).severe(
					definition + " is null.");
		}
	}

	private static void checkFileExistence(final String fileName,
			final String definition) {
		if (!(new File(fileName)).exists()) {
			Logger.getLogger(MATSimDummy.class.getName()).severe(
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

		Logger.getLogger(MATSimDummy.class.getName()).info(
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

		final String zoneShapeFileName = config.get(IHOP2_ELEMENT,
				ZONESHAPE_FILENAME_ELEMENT);
		checkNonNull(zoneShapeFileName, "zone shapefile name");
		checkFileExistence(zoneShapeFileName, "zone shapefile");

		final String buildingShapeFileName = config.get(IHOP2_ELEMENT,
				BUILDINGSHAPE_FILENAME_ELEMENT);
		checkNonNull(buildingShapeFileName, "building shapefile name");
		checkFileExistence(buildingShapeFileName, "building shapefile");

		double regentPopulationSample;
		try {
			regentPopulationSample = Double.parseDouble(config.get(
					IHOP2_ELEMENT, REGENTPOPULATIONSAMPLE_ELEMENT));
		} catch (Exception e) {
			regentPopulationSample = Double.NaN;
			fatal("Could not read ihop2 configuration element "
					+ REGENTPOPULATIONSAMPLE_ELEMENT);
		}

		double matsimPopulationSubSample;
		try {
			matsimPopulationSubSample = Double.parseDouble(config.get(
					IHOP2_ELEMENT, MATSIMPOPULATIONSUBSAMPLE_ELEMENT));
		} catch (Exception e) {
			matsimPopulationSubSample = Double.NaN;
			fatal("Could not read ihop2 configuration element "
					+ MATSIMPOPULATIONSUBSAMPLE_ELEMENT);
		}

		final String populationFileName = config.get(IHOP2_ELEMENT,
				POPULATION_ATTRIBUTE_FILENAME_ELEMENT);
		checkNonNull(populationFileName, "population file name");
		checkFileExistence(populationFileName, "population file");

		final String linkAttributeFileName = config.get(IHOP2_ELEMENT,
				LINKATTRIBUTE_FILENAME_ELEMENT);
		checkNonNull(linkAttributeFileName, "linkattribute file name");
		checkFileExistence(linkAttributeFileName, "linkattribute file");

		// TODO CONTINUE HERE

		final String traveltimesFileName = config.get(IHOP2_ELEMENT,
				TRAVELTIME_MATRIX_FILENAME_ELEMENT);
		checkNonNull(traveltimesFileName, "traveltimes file name");

		final String regentFolder = config.get(IHOP2_ELEMENT,
				REGENT_FOLDER_ELEMENT);
		checkNonNull(regentFolder, "regent folder file name");
		checkFileExistence(regentFolder, "regent folder");

		final String regentCommand = config.get(IHOP2_ELEMENT,
				REGENT_COMMAND_ELEMENT);
		checkNonNull(regentCommand, "regent command");

		Integer maxIterations = null;
		try {
			maxIterations = Integer.parseInt(config.get(IHOP2_ELEMENT,
					ITERATIONS_ELEMENT));
		} catch (NumberFormatException e) {
			fatal("could not read the " + ITERATIONS_ELEMENT + " XML element.");
		}

		final List<String> zoneIDs = config
				.getList(IHOP2_ELEMENT, ZONE_ELEMENT);
		if (zoneIDs == null) {
			fatal("could not read the " + ZONE_ELEMENT + " XML element.");
			System.exit(-1);
		} else if (zoneIDs.size() == 0) {
			fatal("there are no zones defined in the xml file.");
		}
		Collections.sort(zoneIDs, new StringAsIntegerComparator());

		Logger.getLogger(MATSimDummy.class.getName()).info(
				"... program parameters appear OK so far.");

		/*
		 * -------------------- PERMANENT CONFIGURATIONS --------------------
		 */

		// TODO CONTINUE HERE

		// Logger.getLogger(MATSimDummy.class.getName()).info(
		// "Creating zonal system ...");
		//
		// final ZonalSystem zonalSystem;
		// {
		// final org.matsim.core.config.Config matsimConfig = ConfigUtils
		// .loadConfig(matsimConfigFileName);
		// final Scenario scenario = ScenarioUtils.loadScenario(matsimConfig);
		// zonalSystem = new ZonalSystem(zoneShapeFileName,
		// StockholmTransformationFactory.WGS84_EPSG3857);
		// zonalSystem.addNetwork(scenario.getNetwork(),
		// StockholmTransformationFactory.WGS84_SWEREF99);
		// zonalSystem.addBuildings(buildingShapeFileName);
		// }
		//
		// Logger.getLogger(MATSimDummy.class.getName()).info(
		// "... succeeded to create zonal system.");

		/*
		 * ==================== OUTER ITERATIONS ====================
		 */

		for (int iteration = 1; iteration <= maxIterations; iteration++) {

			Logger.getLogger(MATSimDummy.class.getName()).info(
					"---------- STARTING ITERATION " + iteration
							+ " ----------");

			/*
			 * -------------------- LOAD CONFIGURATION --------------------
			 */

			Logger.getLogger(MATSimDummy.class.getName()).info(
					"Loading matsim configuration file: "
							+ matsimConfigFileName + " ... ");
			final org.matsim.core.config.Config matsimConfig = ConfigUtils
					.loadConfig(matsimConfigFileName);
			final String matsimNetworkFileName = matsimConfig.getModule(
					"network").getValue("inputNetworkFile");
			final String initialPlansFileName = matsimConfig.getModule("plans")
					.getValue("inputPlansFile");

			/*
			 * -------------------- CREATE POPULATION --------------------
			 */
			Logger.getLogger(MATSimDummy.class.getName()).info(
					"Creating MATSim population ... ");

			final PopulationCreator populationCreator = new PopulationCreator(
					matsimNetworkFileName, zoneShapeFileName,
					StockholmTransformationFactory.WGS84_EPSG3857,
					populationFileName);
			populationCreator.setBuildingsFileName(buildingShapeFileName);
			// pc.setAgentHomeXYFile("./data/demand_output/agenthomeXY_v03.txt");
			// pc.setAgentWorkXYFile("./data/demand_output/agentWorkXY_v03.txt");
			// pc.setNetworkNodeXYFile("./data/demand_output/nodeXY_v03.txt");
			// pc.setZonesBoundaryShapeFileName("./data/shapes/limit_EPSG3857.shp");
			populationCreator
					.setPopulationSampleFactor(matsimPopulationSubSample);
			final ObjectAttributes linkAttributes = new ObjectAttributes();
			final ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(
					linkAttributes);
			reader.parse(linkAttributeFileName);
			populationCreator.setLinkAttributes(linkAttributes);
			try {
				populationCreator.run(initialPlansFileName);
			} catch (FileNotFoundException e1) {
				throw new RuntimeException(e1);
			}

			Logger.getLogger(MATSimDummy.class.getName()).info(
					"... succeeded to create population.");

			/*
			 * -------------------- RUN MATSIM --------------------
			 */

			Logger.getLogger(MATSimDummy.class.getName()).info(
					"Running MATSim ...");

			// TODO erase output file

			Controler controler = new Controler(matsimConfig);
			matsimConfig.getModule("qsim").addParam(
					"flowCapacityFactor",
					Double.toString(regentPopulationSample
							* matsimPopulationSubSample));
			matsimConfig.getModule("qsim").addParam(
					"storageCapacityFactor",
					Double.toString(regentPopulationSample
							* matsimPopulationSubSample));
			matsimConfig.planCalcScore().setWriteExperiencedPlans(true);
			controler.run();

			Logger.getLogger(MATSimDummy.class.getName()).info(
					"... MATSim run completed.");

			/*
			 * -------------------- WRITE TRAVELTIME MATRIX --------------------
			 */

			Logger.getLogger(MATSimDummy.class.getName()).info(
					"Creating travel time matrix data structures ...");

			// TODO take this out of the controler!?
			final Scenario scenario = ScenarioUtils.loadScenario(matsimConfig);
			final int timeBinSize = 15 * 60;
			final int endTime = 12 * 3600;
			final TravelTimeCalculator ttcalc = new TravelTimeCalculator(
					scenario.getNetwork(), timeBinSize, endTime, scenario
							.getConfig().travelTimeCalculator());
			final Set<String> relevantLinkIDs = new LinkedHashSet<String>(
					ObjectAttributeUtils2.allObjectKeys(linkAttributes));
			// final String zonesShapeFileName =
			// "./data/shapes/sverige_TZ_EPSG3857.shp";
			// TODO this was already created in the population generation
			final ZonalSystem zonalSystem = new ZonalSystem(zoneShapeFileName,
					StockholmTransformationFactory.WGS84_EPSG3857);
			zonalSystem.addNetwork(scenario.getNetwork(),
					StockholmTransformationFactory.WGS84_SWEREF99);
			final String lastIteration = matsimConfig.getModule("controler")
					.getValue("lastIteration");
			final String eventsFileName = matsimConfig.getModule("controler")
					.getValue("outputDirectory")
					+ "ITERS/it."
					+ lastIteration
					+ "/" + lastIteration + ".events.xml.gz";

			final int startTime_s = 6 * 3600 + 1800;
			final int binSize_s = 3600;
			final int binCnt = 1;
			final int sampleCnt = 1;

			new TravelTimeMatrices(scenario.getNetwork(), ttcalc,
					eventsFileName,
					// traveltimesFileName,
					relevantLinkIDs, zoneIDs, zonalSystem, new Random(),
					startTime_s, binSize_s, binCnt, sampleCnt);
			Logger.getLogger(MATSimDummy.class.getName()).warning(
					"TRAVEL TIME WRITING IS TURNED OFF. TALK TO GUNNAR");
			// TODO configure seed

			Logger.getLogger(MATSimDummy.class.getName()).info(
					"... succeeded to write traveltime matrices to file: "
							+ traveltimesFileName);

			/*
			 * -------------------- RUN REGENT --------------------
			 */

			Logger.getLogger(MATSimDummy.class.getName()).info(
					"Running Regent: " + regentCommand + " ...");

			final Process proc;
			final int exitVal;
			try {
				proc = Runtime.getRuntime().exec(regentCommand, null,
						new File(regentFolder));
				exitVal = proc.waitFor();
				if (exitVal != 0) {
					fatal("Regent terminated with exit code " + exitVal + ".");
				}
			} catch (Exception e) {
				fatal(e);
			}

			Logger.getLogger(MATSimDummy.class.getName()).info(
					"... succeeded to run Regent");

		}

		Logger.getLogger(MATSimDummy.class.getName()).info("DONE");
		System.out.println("... DONE");
	}
}
