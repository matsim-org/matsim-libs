package org.matsim.application.analysis.population;

import jakarta.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.*;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Objects;

@CommandLine.Command(name = "write-experienced-plans",
	description = "Writes experienced plans next to events file.")
public class ExperiencedPlansWriter implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(ExperiencedPlansWriter.class);

	@CommandLine.Option(names = "--path", description = "Path to output folder", required = true)
	private Path path;

	@CommandLine.Option(names = "--runId", description = "Run id (i.e. prefixes of files)")
	private String runId;

	@CommandLine.Option(names = "--threads", description = "Number of threads to use for processing events", defaultValue = "1")
	private int numberOfThreads = 1;

	public static void main(String[] args) {
		new ExperiencedPlansWriter().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		String runPrefix = Objects.nonNull(runId) ? runId + "." : "";
		Path configPath = path.resolve(runPrefix + "output_" + Controler.DefaultFiles.config.getFilename());

		Path eventsPath = path.resolve(runPrefix + "output_" + Controler.DefaultFiles.events.getFilename() + ".gz");

		// ---

		Config config = ConfigUtils.loadConfig(configPath.toString());
		config.eventsManager().setNumberOfThreads(numberOfThreads);

		Path outputExpPlansPath = eventsPath.getParent().resolve(config.controller().getRunId() + ".output_" + Controler.DefaultFiles.experiencedPlans.getFilename() + ".gz");

		// ---

		Scenario scenario = new ScenarioUtils.ScenarioBuilder(config)
			.setNetwork(NetworkUtils.readNetwork(path.resolve(runPrefix + "output_" + Controler.DefaultFiles.network.getFilename() + ".gz").toString()))
			.setPopulation(PopulationUtils.readPopulation(path.resolve(runPrefix + "output_" + Controler.DefaultFiles.population.getFilename() + ".gz").toString()))
			.build();

		new TransitScheduleReader(scenario).readFile(path.resolve(runPrefix + "output_" + Controler.DefaultFiles.transitSchedule.getFilename() + ".gz").toString());

		// ===

		EventsToActivities eventsToActivities = new EventsToActivities();
		EventsToLegs eventsToLegs = new EventsToLegs(scenario);

		com.google.inject.Injector injector = new Injector.InjectorBuilder( scenario ).addStandardModules().addOverridingModule( new AbstractModule(){
			@Override public void install(){
				bind( EventsToActivities.class ).toInstance( eventsToActivities );
				bind( EventsToLegs.class ).toInstance( eventsToLegs );
			}
		} ).build();

//		ExperiencedPlansService experiencedPlansService = ExperiencedPlansServiceFactory.create(scenario, eventsToActivities, eventsToLegs);
		ExperiencedPlansService experiencedPlansService = injector.getInstance( ExperiencedPlansService.class );
		((IterationStartsListener) experiencedPlansService).notifyIterationStarts( null );

		// (The way in which this is plugged together is a little odd: ExperiencedPlansService will be plugged into
		// EventsToActivities and EventsToLegs in the same way in which a scoring fct is plugged into those handlers.)

		// ===

		EventsManager eventsManager = EventsUtils.createEventsManager(config);

		eventsManager.addHandler(eventsToActivities);
		eventsManager.addHandler(eventsToLegs);

		log.info("Reading events from file: {}", eventsPath);
		eventsManager.initProcessing();
		EventsUtils.readEvents(eventsManager, eventsPath.toString());

		eventsManager.finishProcessing();

		// I just put in the following manually.  It is normally called from a mobsim listener.  There might be a better place to do this but took me
		// already 2 hrs to get to this point here. kai, nov'25
		eventsToActivities.finish();

		// ===

		log.info("Writing experienced plans to file: {}", outputExpPlansPath);
		experiencedPlansService.writeExperiencedPlans(outputExpPlansPath.toString());

		return 0;
	}
}
