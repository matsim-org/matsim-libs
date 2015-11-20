package gunnar.ihop2.transmodeler.run;

import static gunnar.ihop2.transmodeler.run.RunMATSimWithTransmodeler.EVENTSFILE;
import static gunnar.ihop2.transmodeler.run.RunMATSimWithTransmodeler.TRANSMODELERCOMMAND;
import static gunnar.ihop2.transmodeler.run.RunMATSimWithTransmodeler.TRANSMODELERCONFIG;
import static gunnar.ihop2.transmodeler.run.RunMATSimWithTransmodeler.TRANSMODELERFOLDER;
import static gunnar.ihop2.transmodeler.run.TransmodelerMATSim.LINKATTRIBUTE_FILENAME_ELEMENT;
import static gunnar.ihop2.transmodeler.run.TransmodelerMATSim.PATHS_ELEMENT;
import gunnar.ihop2.transmodeler.tripswriting.TransmodelerTripWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TransmodelerMobsim implements Mobsim {

	private final EventsManager eventsConsumer;

	private final String eventsFile;
	private final String transmodelerFolder;
	private final String transmodelerCommand;
	private final String linkAttributesFileName;
	private final String pathFileName;
	private final String tripFileName;
	private final Scenario scenario;

	@Inject
	public TransmodelerMobsim(final Scenario scenario,
			final EventsManager events) {
		this.eventsConsumer = events;
		this.eventsFile = scenario.getConfig().getModule(TRANSMODELERCONFIG)
				.getValue(EVENTSFILE);
		this.transmodelerFolder = scenario.getConfig()
				.getModule(TRANSMODELERCONFIG).getValue(TRANSMODELERFOLDER);
		this.transmodelerCommand = scenario.getConfig()
				.getModule(TRANSMODELERCONFIG).getValue(TRANSMODELERCOMMAND);
		this.linkAttributesFileName = scenario.getConfig()
				.getModule(TRANSMODELERCONFIG)
				.getValue(LINKATTRIBUTE_FILENAME_ELEMENT);
		this.pathFileName = scenario.getConfig().getModule(TRANSMODELERCONFIG)
				.getValue(PATHS_ELEMENT);
		this.tripFileName = scenario.getConfig().getModule(TRANSMODELERCONFIG)
				.getValue(TransmodelerMATSim.TRIPS_ELEMENT);
		this.scenario = scenario;
	}

	@Override
	public void run() {

		/*
		 * Read in link attributes.
		 */
		Logger.getLogger(this.getClass().getName()).info(
				"Loading file: " + this.linkAttributesFileName);
		final ObjectAttributes linkAttributes = new ObjectAttributes();
		final ObjectAttributesXmlReader linkAttrReader = new ObjectAttributesXmlReader(
				linkAttributes);
		linkAttrReader.parse(this.linkAttributesFileName);

		/*
		 * Write Transmodeler routes and trips file.
		 */
		Logger.getLogger(this.getClass().getName()).info(
				"Writing files: " + this.pathFileName + ", "
						+ this.tripFileName);
		final TransmodelerTripWriter tripWriter = new TransmodelerTripWriter(
				this.scenario.getPopulation(), linkAttributes);
		try {
			tripWriter.writeTrips(this.pathFileName, this.tripFileName);
		} catch (FileNotFoundException e) {
			Logger.getLogger(this.getClass().getName()).severe(e.getMessage());
			e.printStackTrace();
		}

		/*
		 * Execute Transmodeler.
		 */
		Logger.getLogger(this.getClass().getName()).info(
				"Executing: " + this.transmodelerCommand);
		final Process proc;
		final int exitVal;
		try {
			proc = Runtime.getRuntime().exec(transmodelerCommand, null,
					new File(transmodelerFolder));
			exitVal = proc.waitFor();
			if (exitVal != 0) {
				throw new RuntimeException(
						"Transmodeler terminated with exit code " + exitVal
								+ ".");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		/*
		 * Read Transmodeler events file.
		 */
		Logger.getLogger(this.getClass().getName()).info(
				"Loading file: " + this.eventsFile);
		final MatsimEventsReader reader = new MatsimEventsReader(
				this.eventsConsumer);
		reader.readFile(this.eventsFile);
	}
}
