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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.ExperiencedPlansService;
import org.matsim.core.scoring.ExperiencedPlansServiceFactory;
import picocli.CommandLine;

import java.nio.file.Path;

@CommandLine.Command(name = "write-experienced-plans",
	description = "Writes experienced plans next to events file.")
public class ExperiencedPlansWriter implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(ExperiencedPlansWriter.class);

	@CommandLine.Option(names = "--events", description = "Path to events file", required = true)
	private Path eventsPath;

	@CommandLine.Option(names = "--config", description = "Path to config file", required = true)
	private String configPath;

	@CommandLine.Option(names = "--threads", description = "Number of threads to use for processing events", defaultValue = "1")
	private int numberOfThreads = 1;

	public static void main(String[] args) {
		new ExperiencedPlansWriter().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		Config config = ConfigUtils.loadConfig(configPath);
		config.eventsManager().setNumberOfThreads(numberOfThreads);

		EventsManager eventsManager = EventsUtils.createEventsManager(config);
		Path output = eventsPath.getParent().resolve(config.controller().getRunId() + ".output_" + Controler.DefaultFiles.experiencedPlans.getFilename() + ".gz");

		// Loads the scenario from config file. It doesn't matter that the input population and network is loaded.
		// The scenario is later used as reference for static objects (like network and transit schedule) and gathering of ids of the population.
		// The actual experienced plans are built via events.
		Scenario scenario = ScenarioUtils.loadScenario(config);

		EventsToActivities eventsToActivities = new EventsToActivities();
		EventsToLegs eventsToLegs = new EventsToLegs(scenario);

		eventsManager.addHandler(eventsToActivities);
		eventsManager.addHandler(eventsToLegs);

		ExperiencedPlansService experiencedPlansService = ExperiencedPlansServiceFactory.create(scenario, eventsToActivities, eventsToLegs);

		log.info("Reading events from file: {}", eventsPath);
		eventsManager.initProcessing();
		EventsUtils.readEvents(eventsManager, eventsPath.toString());
		eventsManager.finishProcessing();

		log.info("Writing experienced plans to file: {}", output);
		experiencedPlansService.writeExperiencedPlans(output.toString());

		return 0;
	}
}
