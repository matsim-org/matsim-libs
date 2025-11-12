package playground.vsp.analysis.modules.VTTS;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.*;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.DefaultMobsimModule;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.StrategyManagerModule;
import org.matsim.core.replanning.annealing.ReplanningAnnealer;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.*;
import org.matsim.core.scoring.functions.*;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorModule;
import org.matsim.core.utils.timing.TimeInterpretationModule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import picocli.CommandLine;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@CommandLine.Command(name = "write-experienced-plans",
	description = "Writes experienced plans next to events file.")
public class RunVTTSAnalysis implements MATSimAppCommand {

	// yyyyyy FIXME: Think about good name for this class!

	private static final Logger log = LogManager.getLogger(RunVTTSAnalysis.class);

	@CommandLine.Option(names = "--path", description = "Path to output folder", required = true)
	private Path path;

	@CommandLine.Option(names = "--runId", description = "Run id (i.e. prefixes of files)")
	private String runId;

	@CommandLine.Option(names = "--prefix", description = "Prefix for filtered events output file, optional." )
	private String prefix;

	@CommandLine.Option(names = "--threads", description = "Number of threads to use for processing events", defaultValue = "1")
	private int numberOfThreads = 1;

	public static void main(String[] args) {
		new RunVTTSAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		String runPrefix = Objects.nonNull(runId) ? runId + "." : "";
		Path configPath = path.resolve(runPrefix + "output_" + Controler.DefaultFiles.config.getFilename());

		Config config = ConfigUtils.loadConfig(configPath.toString());
		config.eventsManager().setNumberOfThreads(numberOfThreads);

		EventsManager eventsManager = EventsUtils.createEventsManager(config);
		Path eventsPath = path.resolve(runPrefix + "output_" + Controler.DefaultFiles.events.getFilename() + ".gz");
		if ( prefix!=null ){
			eventsPath = ApplicationUtils.globFile( path, "*" + prefix + "output_events_filtered.xml.gz" );
		}

		Path populationFilename = path.resolve( runPrefix + "output_" + PlansScoring.EXPERIENCED_PLANS_XML + ".gz" );
		if ( !Files.exists( populationFilename ) ){
			populationFilename = path.resolve( runPrefix + "output_" + Controler.DefaultFiles.population.getFilename() + ".gz" );
		}
		Scenario scenario = new ScenarioUtils.ScenarioBuilder(config)
								.setNetwork(NetworkUtils.readNetwork(path.resolve(runPrefix + "output_" + Controler.DefaultFiles.network.getFilename() + ".gz").toString()))
								.setPopulation(PopulationUtils.readPopulation( populationFilename.toString() ) )
								.build();

		new TransitScheduleReader(scenario).readFile(path.resolve(runPrefix + "output_" + Controler.DefaultFiles.transitSchedule.getFilename() + ".gz").toString());

		scenario.getConfig().controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
		scenario.getConfig().counts().setInputFile( null );

		// there is probably a better way of doing the following, but I haven't figured out how to use the override syntax w/o Controller.
		Module module= new AbstractModule(){
			@Override
			public void install(){
				install( new NewControlerModule() );
				install( new ControlerDefaultCoreListenersModule() );
				install( new ScenarioByInstanceModule( scenario ) );

				install(new EventsManagerModule() );
				install(new DefaultMobsimModule() );
				install(new TravelTimeCalculatorModule() );
				install(new TravelDisutilityModule() );
				install(new TripRouterModule() );
				install(new StrategyManagerModule() );
				install(new TimeInterpretationModule() );
//				if (getConfig().replanningAnnealer().isActivateAnnealingModule()) {
//					addControllerListenerBinding().to( ReplanningAnnealer.class );
//				}

				bind( ScoringFunctionFactory.class ).to( CharyparNagelScoringFunctionFactory.class );

//				Map<String, ScoringConfigGroup.ScoringParameterSet> scoringParameter = getConfig().scoring().getScoringParametersPerSubpopulation();
//
//				boolean tasteVariations = scoringParameter.values().stream().anyMatch(s -> s.getTasteVariationsParams() != null);
//
//				// If there are taste variations, the individual scoring parameters are used
//				if (tasteVariations) {
//					bind(ScoringParametersForPerson.class).to( IndividualPersonScoringParameters.class ).in( Singleton.class );
//					addControllerListenerBinding().to( IndividualPersonScoringOutputWriter.class ).in(Singleton.class );
//
//				} else {
//					bind(ScoringParametersForPerson.class).to(SubpopulationScoringParameters.class);
//				}

				bind( ScoringParametersForPerson.class ).to( IncomeDependentUtilityOfMoneyPersonScoringParameters.class );
			}
		};
		Injector injector = org.matsim.core.controler.Injector.createInjector( config, module );
		ScoringParametersForPerson scoringParametersForPerson = injector.getInstance( ScoringParametersForPerson.class );




		VTTSHandler vttsHandler = new VTTSHandler( scenario, scoringParametersForPerson );
		eventsManager.addHandler( vttsHandler );

		log.info("Reading events from file: {}", eventsPath);
		eventsManager.initProcessing();
		EventsUtils.readEvents(eventsManager, eventsPath.toString());
		eventsManager.finishProcessing();

		vttsHandler.computeFinalVTTS();
		vttsHandler.printVTTSHistogram( eventsPath.getParent().resolve( config.controller().getRunId() + ".vttsHistogram.tsv" ).toString() );
		vttsHandler.printVTTS( eventsPath.getParent().resolve( config.controller().getRunId() + ".vtts.tsv" ).toString() );
		vttsHandler.printAvgVTTSperPerson( eventsPath.getParent().resolve( config.controller().getRunId() + ".vttsPerPerson.tsv" ).toString() );

		return 0;
	}
}
