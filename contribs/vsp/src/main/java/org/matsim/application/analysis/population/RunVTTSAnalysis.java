package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.*;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import picocli.CommandLine;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;
import tech.tablesaw.aggregate.AggregateFunctions;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvWriteOptions;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Layout;
import tech.tablesaw.plotly.traces.HistogramTrace;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@CommandLine.Command(name = "run-vtts-analysis", description = "")
public class RunVTTSAnalysis implements MATSimAppCommand {
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
		String runPrefix = Objects.nonNull(runId ) ? runId + "." : "";
		Path configPath = path.resolve(runPrefix + "output_" + Controler.DefaultFiles.config.getFilename() );

		Path eventsPath = path.resolve(runPrefix + "output_" + Controler.DefaultFiles.events.getFilename() + ".gz");
		if ( prefix!=null ){
			eventsPath = ApplicationUtils.globFile( path, "*" + prefix + "output_events_filtered.xml.gz" );
		}

		Path populationFilename = path.resolve( runPrefix + "output_" + Controler.DefaultFiles.experiencedPlans.getFilename() + ".gz" );
		if ( !Files.exists( populationFilename ) ){
			populationFilename = path.resolve( runPrefix + "output_" + Controler.DefaultFiles.population.getFilename() + ".gz" );
		}

		// ---

		Config config = ConfigUtils.loadConfig(configPath.toString());
		config.eventsManager().setNumberOfThreads(numberOfThreads);
		config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
		config.counts().setInputFile( null );

		// ---

		Scenario scenario = new ScenarioUtils.ScenarioBuilder(config)
								.setNetwork(NetworkUtils.readNetwork(path.resolve(runPrefix + "output_" + Controler.DefaultFiles.network.getFilename() + ".gz").toString()))
								.setPopulation(PopulationUtils.readPopulation( populationFilename.toString() ) )
								.build();

		new TransitScheduleReader(scenario).readFile(path.resolve(runPrefix + "output_" + Controler.DefaultFiles.transitSchedule.getFilename() + ".gz").toString());

/*
		{
			List<Person> toRemove = new ArrayList<>();
			for( Person person : scenario.getPopulation().getPersons().values() ){
				if ( ! "person".equals( PopulationUtils.getSubpopulation( person ) ) ) {
					toRemove.add( person );
				}
			}
			for( Person person : toRemove ){
				scenario.getPopulation().removePerson( person.getId() );
			}
		}
*/
		// The above removes persons of the "null" subpopulation, which one probably does not want.
		// The question now is what we will do with commercial agents who do not have income.

		// this is awfully slow:
//		ScenarioChecker scenarioChecker = new ScenarioChecker( scenario );
//		scenarioChecker.addScenarioChecker( new ScenarioChecker.ActivityChecker() );
//		scenarioChecker.run();

		com.google.inject.Injector injector = new Injector.InjectorBuilder( scenario )
												  .addStandardModules()
												  .addOverridingModule( new AbstractModule(){
													  @Override public void install(){
														  bind( ScoringParametersForPerson.class ).to( IncomeDependentUtilityOfMoneyPersonScoringParameters.class );
													  }
												  } )
												  .build();

		ScoringParametersForPerson scoringParametersForPerson = injector.getInstance( ScoringParametersForPerson.class );


		// ===

		EventsManager eventsManager = EventsUtils.createEventsManager(config);

		VTTSHandlerKN vttsHandler = new VTTSHandlerKN( scenario, scoringParametersForPerson );
		eventsManager.addHandler( vttsHandler );

		eventsManager.initProcessing();
		log.info("Reading events from file: {}", eventsPath);
		EventsUtils.readEvents(eventsManager, eventsPath.toString());
		eventsManager.finishProcessing();
		vttsHandler.computeFinalVTTS();

		// ===

//		NumberFormat format1 = NumberFormat.getNumberInstance( Locale.GERMAN );
//		format1.setMaximumFractionDigits( 1 );
//		format1.setMinimumFractionDigits( 1 );

		Table tripTable = vttsHandler.getTablesawTripsTable();

		System.out.println( tripTable );

		HistogramTrace histogramTrace = HistogramTrace.builder( tripTable.doubleColumn( HeadersKN.vttsh ) ).build();
		final Layout.LayoutBuilder layoutBuilder = Layout.builder().width( 1000 );
		Figure figure = new Figure( layoutBuilder.build(), histogramTrace );
//		Plot.show( figure );

		log.info( tripTable.summarize( HeadersKN.muttsh, AggregateFunctions.mean, AggregateFunctions.quartile1, AggregateFunctions.median, AggregateFunctions.quartile3, AggregateFunctions.percentile95 ).apply() );
		log.info( tripTable.summarize( HeadersKN.vttsh, AggregateFunctions.mean, AggregateFunctions.quartile1, AggregateFunctions.median, AggregateFunctions.quartile3, AggregateFunctions.percentile95 ).apply() );

		var options = CsvWriteOptions.builder( eventsPath.getParent().resolve( config.controller().getRunId() + ".tablesawVTTS.tsv" ).toString()  ).separator( '\t' );
//		tripTable.write().usingOptions( options.build() );

		/// yyyy Look into [GenerateExperiencedPlansWithVTTS] for a better way how to deal with a test output dir that is separate from the test input dir.

		//		vttsHandler.printVTTSHistogram( eventsPath.getParent().resolve( config.controller().getRunId() + ".vttsHistogram.tsv" ).toString() );
//		vttsHandler.printVTTS( eventsPath.getParent().resolve( config.controller().getRunId() + ".vtts.tsv" ).toString() );
//		vttsHandler.printAvgVTTSperPerson( eventsPath.getParent().resolve( config.controller().getRunId() + ".vttsPerPerson.tsv" ).toString() );

		return 0;
	}
}
