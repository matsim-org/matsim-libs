package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.ExperiencedPlansService;
import org.matsim.core.scoring.ExperiencedPlansServiceFactory;
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

		Config config = ConfigUtils.loadConfig(configPath.toString());
		config.eventsManager().setNumberOfThreads(numberOfThreads);

		EventsManager eventsManager = EventsUtils.createEventsManager(config);
		Path eventsPath = path.resolve(runPrefix + "output_" + Controler.DefaultFiles.events.getFilename() + ".gz");
		Path output = eventsPath.getParent().resolve(config.controller().getRunId() + ".output_" + Controler.DefaultFiles.experiencedPlans.getFilename() + ".gz");

		Scenario scenario = new ScenarioUtils.ScenarioBuilder(config)
			.setNetwork(NetworkUtils.readNetwork(path.resolve(runPrefix + "output_" + Controler.DefaultFiles.network.getFilename() + ".gz").toString()))
			.setPopulation(PopulationUtils.readPopulation(path.resolve(runPrefix + "output_" + Controler.DefaultFiles.population.getFilename() + ".gz").toString()))
			.build();

		new TransitScheduleReader(scenario).readFile(path.resolve(runPrefix + "output_" + Controler.DefaultFiles.transitSchedule.getFilename() + ".gz").toString());

		EventsToActivities eventsToActivities = new EventsToActivities();
		EventsToLegs eventsToLegs = new EventsToLegs(scenario);

		eventsManager.addHandler(eventsToActivities);
		eventsManager.addHandler(eventsToLegs);

		ExperiencedPlansService experiencedPlansService = ExperiencedPlansServiceFactory.create(scenario, eventsToActivities, eventsToLegs);

		log.info("Reading events from file: {}", eventsPath);
		eventsManager.initProcessing();
		EventsUtils.readEvents(eventsManager, eventsPath.toString());

		eventsManager.finishProcessing();

		// I just put in the following manually.  It is normally called from a mobsim listener.  There might be a better place to do this but took me
		// already 2 hrs to get to this point here. kai, nov'25
		eventsToActivities.finish();

		log.info("Writing experienced plans to file: {}", output);
		experiencedPlansService.writeExperiencedPlans(output.toString());

		return 0;
	}
}
