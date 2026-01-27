package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.ExperiencedPlansService;
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
		if ( args==null || args.length==0 ) {
			args = new String[] {
//				"--path", "/Users/kainagel/kairuns/equil/output"
				"--path", "/Users/kainagel/git/all-matsim/matsim-example-project/scenarios/equil/output"
			};
		}
		new ExperiencedPlansWriter().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		// yyyy the output_config has the input files as as files. :-(

		String runPrefix = Objects.nonNull(runId) ? runId + "." : "";
		Path configPath = path.resolve(runPrefix + "output_" + Controler.DefaultFiles.config.getFilename() );

		Config config = ConfigUtils.loadConfig(configPath.toString());
		config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
		config.eventsManager().setNumberOfThreads(numberOfThreads);

		// yyyyyy there is a runId from args, and a config.controller().getRunId().  there is also "prefix", which hedges against no runId.  Needs to be sorted out!!!!

		Path eventsPath = path.resolve(runPrefix + "output_" + Controler.DefaultFiles.events.getFilename() + ".gz");
		Path output = eventsPath.getParent().resolve(config.controller().getRunId() + ".output_" + Controler.DefaultFiles.experiencedPlans.getFilename() + ".gz");
		if ( config.controller().getRunId()==null || config.controller().getRunId().length()==0 ) {
			output = eventsPath.getParent().resolve("output_" + Controler.DefaultFiles.experiencedPlans.getFilename() + ".gz");
		}
		// (There is functionality for this in the matsim core.)

		Scenario scenario = new ScenarioUtils.ScenarioBuilder(config)
			.setNetwork(NetworkUtils.readNetwork(path.resolve(runPrefix + "output_" + Controler.DefaultFiles.network.getFilename() + ".gz").toString()))
			.setPopulation(PopulationUtils.readPopulation(path.resolve(runPrefix + "output_" + Controler.DefaultFiles.population.getFilename() + ".gz").toString()))
			.build();

		if ( config.transit().isUseTransit() ){
			new TransitScheduleReader( scenario ).readFile( path.resolve( runPrefix + "output_" + Controler.DefaultFiles.transitSchedule.getFilename() + ".gz" ).toString() );
		}

		config.counts().setInputFile( null );

		com.google.inject.Injector injector = new Injector.InjectorBuilder( scenario ).addStandardModules().build();

		EventsManager eventsManager = injector.getInstance( EventsManager.class );
		ExperiencedPlansService experiencedPlansService = injector.getInstance( ExperiencedPlansService.class );
		EventsToActivities eventsToActivities = injector.getInstance( EventsToActivities.class );

		if ( experiencedPlansService instanceof IterationStartsListener ) {
			((IterationStartsListener) experiencedPlansService).notifyIterationStarts( null );
		}

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
