package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
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
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.ExperiencedPlansService;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.utils.tablesaw.TablesawUtils;
import picocli.CommandLine;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;
import tech.tablesaw.aggregate.AggregateFunctions;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.NumberColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvWriteOptions;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Layout;
import tech.tablesaw.plotly.traces.HistogramTrace;

import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@CommandLine.Command(name = "generate-experienced-plans-with-vtts", description = "")
public class GenerateExperiencedPlansWithVTTS implements MATSimAppCommand {
	// yyyy in the maybe not so long term it would be good to give this class an explicit output directory since otherwise the test
	// method around this class will clutter the test input directory.  And possibly we would want to put the output from classes
	// such as this one into different directories to differentiate explicit run output from postprocessed material.

	private static final Logger log = LogManager.getLogger( GenerateExperiencedPlansWithVTTS.class );
	public static final String VTTS_H = "VTTS_h (incoming trip)";
	public static final String MUTTS_H = "mUTTS_h (incoming trip)";

	@CommandLine.Option(names = "--path", description = "Path to input folder", required = true)
	private Path path;

	@CommandLine.Option(names = "--output", description = "Overwrite output folder defined by the application")
	protected Path output;

	@CommandLine.Option(names = "--runId", description = "Run id (i.e. prefixes of files)")
	private String runId;

//	@CommandLine.Option(names = "--prefix", description = "Prefix for filtered events output file, optional." )
//	private String prefix;

	@CommandLine.Option(names = "--threads", description = "Number of threads to use for processing events", defaultValue = "1")
	private int numberOfThreads = 1;

	public static void main(String[] args) {
		new GenerateExperiencedPlansWithVTTS().execute(args );
	}

	@Override
	public Integer call() throws Exception {
		String runPrefix = Objects.nonNull( runId ) ? runId + "." : "";

		Path configPath = path.resolve(runPrefix + "output_" + Controler.DefaultFiles.config.getFilename() );

		Path eventsPath = path.resolve(runPrefix + "output_" + Controler.DefaultFiles.events.getFilename() + ".gz");
//		if ( prefix!=null ){
//			eventsPath = ApplicationUtils.globFile( path, "*" + prefix + "output_events_filtered.xml.gz" );
//		}

		Path populationFilename = /* path.resolve( runPrefix + "output_" + Controler.DefaultFiles.experiencedPlans.getFilename() + ".gz" );
		if ( !Files.exists( populationFilename ) ){
			populationFilename = */ path.resolve( runPrefix + "output_" + Controler.DefaultFiles.population.getFilename() + ".gz" );
//		}

		// ---

		Config config = ConfigUtils.loadConfig(configPath.toString());
		config.eventsManager().setNumberOfThreads(numberOfThreads);
		config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
		// (yyyy this is dangerous; in particular someone might change it to "deleteDirectory")

		config.counts().setInputFile( null );

		// ---

		Path outputDir;
		if ( output != null ) {
			outputDir = output;
			// (if "output" comes from the testutils, then it is relative to the IDE java root)
		} else {
			outputDir = path ; // outputDir = inputDir
			// the original was eventsPath.getParent() instead of just "path", where the getParent() presumably just strips the filename.  Don't know why it used that indirection.
		}

		Path outputExpPlansPath = outputDir.resolve(config.controller().getRunId() + ".output_" + Controler.DefaultFiles.experiencedPlans.getFilename() + ".gz");

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

		// ===

		EventsToActivities eventsToActivities = new EventsToActivities();
		EventsToLegs eventsToLegs = new EventsToLegs(scenario);

		com.google.inject.Injector injector = new Injector.InjectorBuilder( scenario )
												  .addStandardModules()
												  .addOverridingModule( new AbstractModule(){
													  @Override public void install(){
														  bind( ScoringParametersForPerson.class ).to( IncomeDependentUtilityOfMoneyPersonScoringParameters.class );
														  bind( EventsToActivities.class ).toInstance( eventsToActivities );
														  bind( EventsToLegs.class ).toInstance( eventsToLegs );
													  }
												  } )
												  .build();

		// ===

//		ExperiencedPlansService experiencedPlansService = ExperiencedPlansServiceFactory.create(scenario, eventsToActivities, eventsToLegs);
		ExperiencedPlansService experiencedPlansService = injector.getInstance( ExperiencedPlansService.class );
		((IterationStartsListener) experiencedPlansService).notifyIterationStarts( null );

		// (The way in which this is plugged together is a little odd: ExperiencedPlansService will be plugged into
		// EventsToActivities and EventsToLegs in the same way in which a scoring fct is plugged into those handlers.)

		ScoringParametersForPerson scoringParametersForPerson = injector.getInstance( ScoringParametersForPerson.class );

		// ===

		EventsManager eventsManager = EventsUtils.createEventsManager(config);

		eventsManager.addHandler(eventsToActivities);
		eventsManager.addHandler(eventsToLegs);

		VTTSHandlerKN vttsHandler = new VTTSHandlerKN( scenario, scoringParametersForPerson );
		eventsManager.addHandler( vttsHandler );

		eventsManager.initProcessing();
		log.info("Reading events from file: {}", eventsPath);
		EventsUtils.readEvents(eventsManager, eventsPath.toString());
		eventsManager.finishProcessing();

		vttsHandler.computeFinalVTTS();
		eventsToActivities.finish();

		// yy There is, in many places in the above, the issue that things need to start and to finish.  Sometimes this is
		// hardcoded, sometimes this goes via notifyIterationStarts, sometimes this is implicit in the constructor, sometimes this
		// is passed on by the eventsManager to the handlers (e.g. reset).  Would be nice if we got this a bit more consistent. kai,
		// nov'25

		// ===

		Map<Id<Person>, List<VTTSHandlerKN.TripData>> tripDataMap = vttsHandler.getTripDataMap();
		Population expPlans = experiencedPlansService.getPopulationWithExperiencedPlans();

		// The following is a really complicated way to do a join.  Maybe first convert to tablesaw and then do this?
		for( Person person : expPlans.getPersons().values() ){
			final List<Activity> activities = TripStructureUtils.getActivities( person.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities );
			List<VTTSHandlerKN.TripData> tripDataList = tripDataMap.get( person.getId() );
			for ( int ii=1; ii<activities.size(); ii++ ) {
				Activity activity = activities.get( ii );
				VTTSHandlerKN.TripData tripData = tripDataList.get( ii-1 );  // activity # 1 belongs to trip # 0!
				activity.getAttributes().putAttribute( VTTS_H, tripData.VTTSh );
				activity.getAttributes().putAttribute( MUTTS_H, tripData.mUTTSh );
			}
		}

		log.info("Writing experienced plans to file: {}", outputExpPlansPath);
		PopulationUtils.writePopulation( expPlans, outputExpPlansPath.toString() );

		// ===

		NumberFormat format1 = NumberFormat.getNumberInstance( Locale.GERMAN );
		format1.setMaximumFractionDigits( 1 );
		format1.setMinimumFractionDigits( 1 );

		Table tripsTable = vttsHandler.getTablesawTripsTable();

		for( Column<?> column : tripsTable.columns() ){
			if ( column instanceof DoubleColumn ) {
				((NumberColumn<?, ?>) column).setPrintFormatter( format1, "n/a" );
			}
		}

		log.info( "print table:");
		System.out.println( System.lineSeparator() + tripsTable + System.lineSeparator() );

		HistogramTrace histogramTrace = HistogramTrace.builder( tripsTable.doubleColumn( HeadersKN.vttsh ) ).build();
		final Layout.LayoutBuilder layoutBuilder = Layout.builder().width( 1000 );
		Figure figure = new Figure( layoutBuilder.build(), histogramTrace );

		Path htmlPath = outputDir.resolve(runPrefix + "histogram.html" );
		TablesawUtils.writeFigureToHtmlFile( htmlPath.toString(), figure );

		log.info( "print summary statistics:");
		final Table muttsStats = tripsTable.summarize( HeadersKN.muttsh, AggregateFunctions.mean, AggregateFunctions.quartile1, AggregateFunctions.median, AggregateFunctions.quartile3, AggregateFunctions.percentile95 ).apply();
		System.out.println( System.lineSeparator() + muttsStats + System.lineSeparator() );
		final Table vttsStats = tripsTable.summarize( HeadersKN.vttsh, AggregateFunctions.mean, AggregateFunctions.quartile1, AggregateFunctions.median, AggregateFunctions.quartile3, AggregateFunctions.percentile95 ).apply();
		System.out.println( vttsStats + System.lineSeparator() );


		{
			var options = CsvWriteOptions.builder( outputDir.resolve( config.controller().getRunId() + ".muttsStats.tsv" ).toString() ).separator( '\t' );
			muttsStats.write().usingOptions( options.build() );
		}
		{
			var options = CsvWriteOptions.builder( outputDir.resolve( config.controller().getRunId() + ".vttsStats.tsv" ).toString() ).separator( '\t' );
			vttsStats.write().usingOptions( options.build() );
		}
		{
			var options = CsvWriteOptions.builder( outputDir.resolve( config.controller().getRunId() + ".tablesaw.tsv" ).toString() ).separator( '\t' );
			tripsTable.write().usingOptions( options.build() );
		}

//		vttsHandler.printVTTSHistogram( eventsPath.getParent().resolve( config.controller().getRunId() + ".vttsHistogram.tsv" ).toString() );
//		vttsHandler.printVTTS( eventsPath.getParent().resolve( config.controller().getRunId() + ".vtts.tsv" ).toString() );
//		vttsHandler.printAvgVTTSperPerson( eventsPath.getParent().resolve( config.controller().getRunId() + ".vttsPerPerson.tsv" ).toString() );

		return 0;
	}
}
