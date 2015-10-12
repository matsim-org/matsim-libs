package gunnar.ihop2.integration;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.Matrix;
import org.matsim.utils.objectattributes.ObjectAttributeUtils2;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import saleem.stockholmscenario.utils.StockholmTransformationFactory;
import floetteroed.utilities.config.Config;
import floetteroed.utilities.config.ConfigReader;
import gunnar.ihop2.regent.demandreading.PopulationCreator;

public class MATSimDummy {

	public static final String IHOP2_ELEMENT = "ihop2";

	public static final String MATSIMCONFIG_FILENAME_ELEMENT = "matsimconfig";

	public static final String ZONESHAPE_FILENAME_ELEMENT = "zoneshapefile";

	public static final String BUILDINGSHAPE_FILENAME_ELEMENT = "buildingshapefile";

	public static final String REGENTPOPULATIONSAMPLE_ELEMENT = "regentpopulationsample";

	public static final String MATSIMPOPULATIONSUBSAMPLE_ELEMENT = "matsimpopulationsubsample";

	public static final String POPULATION_ATTRIBUTE_FILENAME_ELEMENT = "population";

	public static final String LINKATTRIBUTE_FILENAME_ELEMENT = "linkattributefile";

	// TODO CONTINUE HERE

	public static final String TRAVELTIME_MATRIX_FILENAME_ELEMENT = "traveltimes";

	public static final String REGENT_FOLDER_ELEMENT = "regentfolder";

	public static final String REGENT_COMMAND_ELEMENT = "regentcommand";

	public static final String ITERATIONS_ELEMENT = "iterations";

	public static final String ZONE_ELEMENT = "zone";

	private static void fatal(final String msg) {
		System.out.println("FATAL ERROR: " + msg);
		System.exit(-1);
	}

	private static void fatal(final Exception e) {
		System.out.println("FATAL ERROR. Stack trace:");
		e.printStackTrace();
		System.exit(-1);
	}

	private static void checkNonNull(final String fileName,
			final String definition) {
		if (fileName == null) {
			fatal(definition + " is null.");
		}
	}

	private static void checkFileExistence(final String fileName,
			final String definition) {
		if (!(new File(fileName)).exists()) {
			fatal(definition + " \"" + fileName + "\" could not be found.");
		}
	}

	public static void main(String[] args) {

		System.out.println("STARTED");
		System.out.println();

		/*
		 * -------------------- CONFIGURE --------------------
		 */

		System.out.println("Checking program parameters ... ");

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

		final List<String> zones = config.getList(IHOP2_ELEMENT, ZONE_ELEMENT);
		if (zones == null) {
			fatal("could not read the " + ZONE_ELEMENT + " XML element.");
			System.exit(-1);
		} else if (zones.size() == 0) {
			fatal("there are no zones defined in the xml file.");
		}
		Collections.sort(zones, new StringAsIntegerComparator());

		System.out.println("... program parameters appear OK so far.");
		System.out.println();

		/*
		 * ==================== OUTER ITERATIONS ====================
		 */

		for (int iteration = 1; iteration <= maxIterations; iteration++) {

			System.out.println();
			System.out.println("Starting iteration " + iteration
					+ " ----------");
			System.out.println();

			/*
			 * -------------------- LOAD CONFIGURATION --------------------
			 */

			System.out.println("Loading matsim configuration file: "
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

			System.out.println("... population data appears OK so far.");
			System.out.println();

			/*
			 * -------------------- RUN MATSIM --------------------
			 */

			System.out.println("Running MATSim ...");
			System.out.println();

			// TODO erase output file

			Controler controler = new Controler(matsimConfig);
			controler.run();

			System.out.println("... MATSim run completed.");
			System.out.println();

			/*
			 * -------------------- WRITE TRAVELTIME MATRIX --------------------
			 */

			System.out
					.println("Creating travel time matrix data structures ...");

			// TODO take this out of the controler!?
			final Scenario scenario = ScenarioUtils.loadScenario(matsimConfig);
			final int timeBinSize = 15 * 60;
			final int endTime = 12 * 3600;
			final TravelTimeCalculator ttcalc = new TravelTimeCalculator(
					scenario.getNetwork(), timeBinSize, endTime, scenario
							.getConfig().travelTimeCalculator());
			final Set<String> relevantLinkIDs = new LinkedHashSet<String>(
					ObjectAttributeUtils2.allObjectKeys(linkAttributes));

			// TODO CONTINUE HERE

			// final String zonesShapeFileName =
			// "./data/shapes/sverige_TZ_EPSG3857.shp";
			// final ZonalSystem zonalSystem = new
			// ZonalSystem(zonesShapeFileName,
			// StockholmTransformationFactory.WGS84_EPSG3857);
			// zonalSystem.addNetwork(scenario.getNetwork(),
			// StockholmTransformationFactory.WGS84_SWEREF99);
			//
			// final TravelTimesWriter ttWriter = new TravelTimesWriter(
			// scenario.getNetwork(), ttcalc);
			//
			// final String eventsFileName =
			// "./data/run/output/ITERS/it.0/0.events.xml.gz";
			// final String regentMatrixFileName = "./data/run/regent-tts.xml";
			// ttWriter.run(eventsFileName, regentMatrixFileName,
			// relevantLinkIDs,
			// zonalSystem);

			System.out.println("STOP");
			System.exit(0);

			final Matrices matrices = new Matrices();
			final Matrix work = matrices.createMatrix("WORK",
					"random work tour travel times");
			final Matrix other = matrices.createMatrix("OTHER",
					"random other tour travel times");

			try {
				for (String fromZone : zones) {
					for (String toZone : zones) {
						work.createEntry(fromZone, toZone, Math.random());
						other.createEntry(fromZone, toZone, Math.random());
					}
				}
			} catch (Exception e) {
				fatal(e);
			}

			System.out
					.println("... succeeded to create travel time matrix data structures.");
			System.out.println();

			System.out.println("Writing traveltime matrices: "
					+ traveltimesFileName + " ... ");

			try {
				final MatricesWriter writer = new MatricesWriter(matrices);
				writer.setIndentationString("  ");
				writer.setPrettyPrint(true);
				writer.write(traveltimesFileName);
			} catch (Exception e) {
				fatal(e);
			}

			System.out.println("... succeded to write travel time matrices.");
			System.out.println();

			/*
			 * -------------------- RUN REGENT --------------------
			 */

			System.out.println("Running Regent: " + regentCommand + " ...");

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

			System.out.println("... succeeded to run Regent");
			System.out.println();

		}

		System.out.println("DONE");
	}
}
