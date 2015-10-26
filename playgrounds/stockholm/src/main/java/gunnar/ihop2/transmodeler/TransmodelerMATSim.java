package gunnar.ihop2.transmodeler;

import floetteroed.utilities.SimpleLogFormatter;
import floetteroed.utilities.config.Config;
import floetteroed.utilities.config.ConfigReader;
import gunnar.ihop2.integration.MATSimDummy;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import cadyts.utilities.misc.StreamFlushHandler;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TransmodelerMATSim {

	public static final String IHOP2_ELEMENT = "ihop2";

	public static final String PATHS_ELEMENT = "paths";

	public static final String TRIPS_ELEMENT = "trips";

	public static final String EVENTS_ELEMENT = "events";

	public static final String TRANSMODELERFOLDER_ELEMENT = "transmodelerfolder";

	public static final String TRANSMODELERCOMMAND_ELEMENT = "transmodelercommand";

	private static void fatal(final String msg) {
		Logger.getLogger(TransmodelerMATSim.class.getName()).severe(
				"FATAL ERROR: " + msg);
		System.exit(-1);
	}

	private static void fatal(final Exception e) {
		Logger.getLogger(TransmodelerMATSim.class.getName()).severe(
				"FATAL ERROR: " + e);
		e.printStackTrace();
		System.exit(-1);
	}

	private static void checkNonNull(final String fileName,
			final String definition) {
		if (fileName == null) {
			Logger.getLogger(TransmodelerMATSim.class.getName()).severe(
					definition + " is null.");
		}
	}

	private static void checkFileExistence(final String fileName,
			final String definition) {
		if (!(new File(fileName)).exists()) {
			Logger.getLogger(TransmodelerMATSim.class.getName()).severe(
					definition + " \"" + fileName + "\" could not be found.");
		}
	}

	public static void main(String[] args) {

		Logger.getLogger(MATSimDummy.class.getName()).info("STARTED");

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

		Logger.getLogger(TransmodelerMATSim.class.getName()).info(
				"Checking program parameters ... ");

		final String configFileName = args[0];
		checkNonNull(configFileName, "config");
		checkFileExistence(configFileName, "config file");

		final ConfigReader configReader = new ConfigReader();
		final Config config = configReader.read(configFileName);

		final String pathsFileName = config.get(IHOP2_ELEMENT, PATHS_ELEMENT);
		checkNonNull(pathsFileName, "paths file file name");
		checkFileExistence(pathsFileName, "paths file");
		Logger.getLogger(TransmodelerMATSim.class.getName()).info(
				PATHS_ELEMENT + " = " + pathsFileName);

		final String tripsFileName = config.get(IHOP2_ELEMENT, TRIPS_ELEMENT);
		checkNonNull(tripsFileName, "trips file file name");
		checkFileExistence(tripsFileName, "trips file");
		Logger.getLogger(TransmodelerMATSim.class.getName()).info(
				TRIPS_ELEMENT + " = " + tripsFileName);

		final String eventsFileName = config.get(IHOP2_ELEMENT, EVENTS_ELEMENT);
		checkNonNull(eventsFileName, "events file file name");
		Logger.getLogger(TransmodelerMATSim.class.getName()).info(
				EVENTS_ELEMENT + " = " + eventsFileName);

		final String transmodelerFolderName = config.get(IHOP2_ELEMENT,
				TRANSMODELERFOLDER_ELEMENT);
		checkNonNull(transmodelerFolderName, "transmodeler folder name");
		checkFileExistence(transmodelerFolderName, "transmodeler folder");
		Logger.getLogger(TransmodelerMATSim.class.getName()).info(
				TRANSMODELERFOLDER_ELEMENT + " = " + transmodelerFolderName);

		final String transmodelerCommand = config.get(IHOP2_ELEMENT,
				TRANSMODELERCOMMAND_ELEMENT);
		checkNonNull(transmodelerCommand, "transmodeler command");
		Logger.getLogger(TransmodelerMATSim.class.getName()).info(
				TRANSMODELERCOMMAND_ELEMENT + " = " + transmodelerCommand);

		Logger.getLogger(TransmodelerMATSim.class.getName()).info(
				"... program parameters look OK so far.");

		/*
		 * PRETEND TO RUN SIMULATION
		 */

		Logger.getLogger(MATSimDummy.class.getName()).info(
				"Running Transmodeler: " + transmodelerCommand + " ...");

		final Process proc;
		final int exitVal;
		try {
			proc = Runtime.getRuntime().exec(transmodelerCommand, null,
					new File(transmodelerFolderName));
			exitVal = proc.waitFor();
			if (exitVal != 0) {
				fatal("Transmodeler terminated with exit code " + exitVal + ".");
			}
		} catch (Exception e) {
			fatal(e);
		}

		Logger.getLogger(MATSimDummy.class.getName()).info(
				"... succeeded to run Transmodeler.");

		Logger.getLogger(MATSimDummy.class.getName()).info("DONE");
	}
}
