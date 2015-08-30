package gunnar.ihop2.integration;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.Matrix;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import floetteroed.utilities.config.Config;
import floetteroed.utilities.config.ConfigReader;

public class MATSimDummy {

	public static final String IHOP2_ELEMENT = "ihop2";

	public static final String POPULATION_ATTRIBUTE_FILENAME_ELEMENT = "population";

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

		final String populationFileName = config.get(IHOP2_ELEMENT,
				POPULATION_ATTRIBUTE_FILENAME_ELEMENT);
		checkNonNull(populationFileName, "population file name");
		checkFileExistence(populationFileName, "population file");

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
		 * -------------------- LOAD POPULATION --------------------
		 */

		for (int iteration = 1; iteration <= maxIterations; iteration++) {

			System.out.println();
			System.out.println("Starting iteration " + iteration
					+ " ----------");
			System.out.println();

			System.out.println("Loading population file: " + populationFileName
					+ " ... ");

			ObjectAttributes attrs = null;
			try {
				attrs = new ObjectAttributes();
				final ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(
						attrs);
				reader.parse(populationFileName);
			} catch (Exception e) {
				fatal(e);
			}			
			
			System.out.println("... population data appears OK so far.");
			System.out.println();

			/*
			 * -------------------- RUN MATSIM --------------------
			 */

			System.out.println("Pretending to run one MATSim simulation ...");
			System.out.println("... MATSim run completed.");
			System.out.println();

			/*
			 * -------------------- WRITE TRAVELTIME MATRIX --------------------
			 */

			System.out
					.println("Creating travel time matrix data structures ...");

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
