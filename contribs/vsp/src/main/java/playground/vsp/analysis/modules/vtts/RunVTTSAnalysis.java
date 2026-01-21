package playground.vsp.analysis.modules.vtts;

import com.google.inject.Injector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerUtils;

import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Objects;

@CommandLine.Command(name = "write-experienced-plans",
	description = "Writes experienced plans next to events file.")
public class RunVTTSAnalysis implements MATSimAppCommand {

	// yyyyyy FIXME: Think about good name for this class!

	private static final Logger log = LogManager.getLogger(RunVTTSAnalysis.class);

	@CommandLine.Option(names = "--path", description = "Path to output folder", required = true)
	private Path path;

	@CommandLine.Option(names = "--runId", description = "Run id (i.e. prefixes of files)")
	private String runId;

	@CommandLine.Option(names = "--threads", description = "Number of threads to use for processing events", defaultValue = "1")
	private int numberOfThreads = 1;

	public static void main(String[] args) {
		new RunVTTSAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		String runPrefix = Objects.nonNull(runId) ? runId + "." : "";
		Path configPath = path.resolve(runPrefix + "output_" + Controler.DefaultFiles.config + ".xml");

		Config config = ConfigUtils.loadConfig(configPath.toString());
		config.eventsManager().setNumberOfThreads(numberOfThreads);

		config.controller().setOutputDirectory("/Users/gregorr/Documents/work/respos/runs-svn/IATBR/baseCaseContinued/");

		EventsManager eventsManager = EventsUtils.createEventsManager(config);
		Path eventsPath = path.resolve(runPrefix + "output_" + Controler.DefaultFiles.events.name() + ".xml.gz");
//		Path output = eventsPath.getParent().resolve(config.controller().getRunId() + ".output_" + Controler.DefaultFiles.experiencedPlans.getFilename() + ".gz");

		Scenario scenario = new ScenarioUtils.ScenarioBuilder(config)
								.setNetwork(NetworkUtils.readNetwork(path.resolve(runPrefix + "output_" + Controler.DefaultFiles.network + ".xml.gz").toString()))
								.setPopulation(PopulationUtils.readPopulation(path.resolve(runPrefix + "output_" + "plans" + ".xml.gz").toString()))
								.build();

		new TransitScheduleReader(scenario).readFile(path.resolve(runPrefix + "output_" + Controler.DefaultFiles.transitSchedule + ".xml.gz").toString());

		scenario.getConfig().controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
		scenario.getConfig().counts().setInputFile( null );
		Injector injector = ControlerUtils.createAdhocInjector(config, scenario );
		ScoringParametersForPerson scoringParametersForPerson = injector.getInstance( ScoringParametersForPerson.class );

		VTTSHandler vttsHandler = new VTTSHandler( scenario, scoringParametersForPerson );
		eventsManager.addHandler( vttsHandler );

		log.info("Reading events from file: {}", eventsPath);
		eventsManager.initProcessing();
		EventsUtils.readEvents(eventsManager, eventsPath.toString());
		eventsManager.finishProcessing();

		vttsHandler.computeFinalVTTS();
		vttsHandler.printCarVTTS("carVtts.tsv");
		vttsHandler.printVTTS( eventsPath.getParent().resolve( config.controller().getRunId() + ".vtts.tsv" ).toString() );
		vttsHandler.printAvgVTTSperPerson( eventsPath.getParent().resolve( config.controller().getRunId() + ".vttsPerPerson.tsv" ).toString() );

		log.info("Number of persons in VTTS map: {}", vttsHandler.getPersonId2TripNr2VTTSh().size());


		return 0;
	}
}
