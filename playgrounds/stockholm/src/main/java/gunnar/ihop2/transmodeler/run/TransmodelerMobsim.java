package gunnar.ihop2.transmodeler.run;

import static gunnar.ihop2.transmodeler.run.RunMATSimWithTransmodeler.EVENTSFILE;
import static gunnar.ihop2.transmodeler.run.RunMATSimWithTransmodeler.TRANSMODELERCOMMAND;
import static gunnar.ihop2.transmodeler.run.RunMATSimWithTransmodeler.TRANSMODELERCONFIG;
import static gunnar.ihop2.transmodeler.run.RunMATSimWithTransmodeler.TRANSMODELERFOLDER;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.mobsim.framework.Mobsim;

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
	}

	@Override
	public void run() {

		System.out.println("TRANSMODELERCOMMAND = " + this.transmodelerCommand);
		System.out.println("TRANSMODELERFOLDER = " + this.transmodelerFolder);

		/*
		 * final Process proc; final int exitVal; try { proc =
		 * Runtime.getRuntime().exec(transmodelerCommand, null, new
		 * File(transmodelerFolder)); exitVal = proc.waitFor(); if (exitVal !=
		 * 0) { throw new RuntimeException(
		 * "Transmodeler terminated with exit code " + exitVal + "."); } } catch
		 * (Exception e) { throw new RuntimeException(e); }
		 */

		final MatsimEventsReader reader = new MatsimEventsReader(
				this.eventsConsumer);
		reader.readFile(this.eventsFile);
	}

}
