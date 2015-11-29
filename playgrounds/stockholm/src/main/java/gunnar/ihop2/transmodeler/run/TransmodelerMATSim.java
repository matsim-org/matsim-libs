package gunnar.ihop2.transmodeler.run;

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

import org.matsim.contrib.signals.router.InvertedNetworkRoutingModuleModule;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;

import cadyts.utilities.misc.StreamFlushHandler;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TransmodelerMATSim {

	public static final String IHOP2_ELEMENT = "ihop2";

	public static final String MATSIMCONFIG_FILENAME_ELEMENT = "matsimconfig";

	public static final String LINKATTRIBUTE_FILENAME_ELEMENT = "linkattributefile";

	public static final String LANES_FILENAME_ELEMENT = "lanesfile";

	public static final String PATHS_ELEMENT = "paths";

	public static final String TRIPS_ELEMENT = "trips";

	public static final String EVENTS_ELEMENT = "events";

	public static final String TRANSMODELERFOLDER_ELEMENT = "transmodelerfolder";

	public static final String TRANSMODELERCOMMAND_ELEMENT = "transmodelercommand";

	public static final String TRANSMODELERCONFIG = "transmodeler";
	
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

		final String matsimConfigFileName = config.get(IHOP2_ELEMENT,
				MATSIMCONFIG_FILENAME_ELEMENT);
		checkNonNull(matsimConfigFileName, "matsimconfig file name");
		checkFileExistence(matsimConfigFileName, "matsimconfig file");
		Logger.getLogger(MATSimDummy.class.getName()).info(
				MATSIMCONFIG_FILENAME_ELEMENT + " = " + matsimConfigFileName);

		final String linkAttributeFileName = config.get(IHOP2_ELEMENT,
				LINKATTRIBUTE_FILENAME_ELEMENT);
		checkNonNull(linkAttributeFileName, "linkattribute file name");
		checkFileExistence(linkAttributeFileName, "linkattribute file");
		Logger.getLogger(MATSimDummy.class.getName()).info(
				LINKATTRIBUTE_FILENAME_ELEMENT + " = " + linkAttributeFileName);

		final String lanesFileName = config.get(IHOP2_ELEMENT,
				LANES_FILENAME_ELEMENT);
		checkNonNull(lanesFileName, "lanes file name");
		checkFileExistence(lanesFileName, "lanes file");
		Logger.getLogger(MATSimDummy.class.getName()).info(
				LANES_FILENAME_ELEMENT + " = " + lanesFileName);

		final String pathsFileName = config.get(IHOP2_ELEMENT, PATHS_ELEMENT);
		checkNonNull(pathsFileName, "paths file file name");
		Logger.getLogger(TransmodelerMATSim.class.getName()).info(
				PATHS_ELEMENT + " = " + pathsFileName);

		final String tripsFileName = config.get(IHOP2_ELEMENT, TRIPS_ELEMENT);
		checkNonNull(tripsFileName, "trips file file name");
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

		final int iteration = 0;

		/*
		 * -------------------- CONFIGURE --------------------
		 */

		Logger.getLogger(MATSimDummy.class.getName()).info(
				"Loading matsim configuration file: " + matsimConfigFileName
						+ " ... ");
		final org.matsim.core.config.Config matsimConfig = ConfigUtils
				.loadConfig(matsimConfigFileName);
		matsimConfig.getModule("controler").addParam("overwriteFiles",
				"deleteDirectoryIfExists");
		matsimConfig.getModule("controler").addParam("outputDirectory",
				"./matsim-output." + iteration + "/");
		matsimConfig.network().setLaneDefinitionsFile(lanesFileName);
		matsimConfig.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(
				true);
		matsimConfig.controler().setLinkToLinkRoutingEnabled(true);

		final ConfigGroup transmodelerConfigGroup = new ConfigGroup(
				TRANSMODELERCONFIG);
		transmodelerConfigGroup.addParam(EVENTS_ELEMENT, eventsFileName);
		transmodelerConfigGroup.addParam(TRANSMODELERFOLDER_ELEMENT,
				transmodelerFolderName);
		transmodelerConfigGroup.addParam(TRANSMODELERCOMMAND_ELEMENT,
				transmodelerCommand);
		transmodelerConfigGroup.addParam(LINKATTRIBUTE_FILENAME_ELEMENT,
				linkAttributeFileName);
		transmodelerConfigGroup.addParam(PATHS_ELEMENT, pathsFileName);
		transmodelerConfigGroup.addParam(TRIPS_ELEMENT, tripsFileName);
		matsimConfig.addModule(transmodelerConfigGroup);

		/*
		 * -------------------- RUN MATSIM --------------------
		 */

		Logger.getLogger(MATSimDummy.class.getName()).info(
				"Running MATSim/Transmodeler ...");

		final Controler controler = new Controler(matsimConfig);
		controler.addOverridingModule(new InvertedNetworkRoutingModuleModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				binder().bind(Mobsim.class).to(TransmodelerMobsim.class);
			}
		});

		controler.run();

		Logger.getLogger(MATSimDummy.class.getName()).info(
				"... MATSim/Transmodeler run completed.");

		Logger.getLogger(MATSimDummy.class.getName()).info("DONE");
	}
}
