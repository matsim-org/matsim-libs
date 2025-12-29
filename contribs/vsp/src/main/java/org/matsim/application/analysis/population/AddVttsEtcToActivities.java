package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.utils.tablesaw.TablesawUtils;
import picocli.CommandLine;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.NumberColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvWriteOptions;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Layout;
import tech.tablesaw.plotly.traces.HistogramTrace;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static java.lang.Math.exp;
import static tech.tablesaw.aggregate.AggregateFunctions.*;

@CommandLine.Command(name = "run-vtts-analysis", description = "")
public class AddVttsEtcToActivities implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger( AddVttsEtcToActivities.class );

	@CommandLine.Option(names = "--path", description = "Path to output folder", required = true)
	private Path path;

	@CommandLine.Option(names = "--runId", description = "Run id (i.e. prefixes of files)")
	private String runId;

	@CommandLine.Option(names = "--output", description = "Overwrite output folder defined by the application")
	protected Path output;

//	@CommandLine.Option(names = "--prefix", description = "Prefix for filtered events output file, optional." )
//	private String prefix;

	@CommandLine.Option(names = "--threads", description = "Number of threads to use for processing events", defaultValue = "1")
	private int numberOfThreads = 1;

	public static void main(String[] args) {
		new AddVttsEtcToActivities().execute(args );
	}

	@Override
	public Integer call() throws Exception {
		String runPrefix = Objects.nonNull(runId ) ? runId + "." : "";
		Path configPath = path.resolve(runPrefix + "output_" + Controler.DefaultFiles.config.getFilename() );

		Path eventsPath = path.resolve(runPrefix + "output_" + Controler.DefaultFiles.events.getFilename() + ".gz");
//		if ( prefix!=null ){
//			eventsPath = ApplicationUtils.globFile( path, "*" + prefix + "output_events_filtered.xml.gz" );
//		}

		Path populationFilename = path.resolve( runPrefix + "postproc_" + Controler.DefaultFiles.experiencedPlans.getFilename() + ".gz" );
		if ( !Files.exists( populationFilename ) ){
			populationFilename = path.resolve( runPrefix + "output_" + Controler.DefaultFiles.experiencedPlans.getFilename() + ".gz" );
		}

		// ---

		Config config = ConfigUtils.loadConfig(configPath.toString());
		config.eventsManager().setNumberOfThreads(numberOfThreads);
		config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
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

		Path outputExpPlansPath = outputDir.resolve(config.controller().getRunId() + ".vtts_" + Controler.DefaultFiles.experiencedPlans.getFilename() + ".gz");

		// ---

		Scenario scenario = new ScenarioUtils.ScenarioBuilder(config)
//								.setNetwork(NetworkUtils.readNetwork(path.resolve(runPrefix + "output_" + Controler.DefaultFiles.network.getFilename() + ".gz").toString()))
								.setPopulation(PopulationUtils.readPopulation( populationFilename.toString() ) )
								.build();

//		new TransitScheduleReader(scenario).readFile(path.resolve(runPrefix + "output_" + Controler.DefaultFiles.transitSchedule.getFilename() + ".gz").toString());

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

		VTTSHandler vttsHandler = new VTTSHandler( scenario, scoringParametersForPerson );
		eventsManager.addHandler( vttsHandler );

		eventsManager.initProcessing();
		log.info("Reading events from file: {}", eventsPath);
		EventsUtils.readEvents(eventsManager, eventsPath.toString());
		eventsManager.finishProcessing();
		vttsHandler.computeFinalVTTS();

		// ===

		Population population = scenario.getPopulation();

		Map<Id<Person>, List<VTTSHandler.TripData>> tripDataMap = vttsHandler.getTripDataMap();

		// The following is a really complicated way to do a join.  Maybe first convert to tablesaw and then do this?
		for( Person person : population.getPersons().values() ){
			final List<Activity> activities = TripStructureUtils.getActivities( person.getSelectedPlan(),
				TripStructureUtils.StageActivityHandling.ExcludeStageActivities );
			List<VTTSHandler.TripData> tripDataList = tripDataMap.get( person.getId() );
			double sumMuse = 0.;
			double cntMuse = 0.;
			for( int ii = 1 ; ii < activities.size() ; ii++ ){
				Activity activity = activities.get( ii );
				VTTSHandler.TripData tripData = tripDataList.get( ii - 1 );  // activity # 1 belongs to trip # 0!
				setVTTS_h( activity, tripData.VTTSh );
				setMUTTS_h( activity, tripData.mUTTSh );
				setMUSE_h( activity, tripData.musl_h );
				setActScore( activity, tripData.actScore );
				if( tripData.musl_h > 0. && tripData.musl_h < 6 * exp( 1. ) ){
					// There are acts that start long after their end time, and in consequence immediately end again.  If they start
					// earlier, they will also just end earlier, but Ihab's calculation gives them a meaningful MUSE.  This is then
					// in the linear regime, and in consequence beta_perf * e - beta_trav(mode).
					// yyyy For the time being we assume that beta_perf=6 and beta_trav(mode) = 0.
					sumMuse += tripData.musl_h;
					cntMuse++;
				}
				if ( cntMuse >0 ) {
					setMUSE_h( person.getSelectedPlan(), sumMuse / cntMuse );
				} else {
					setMUSE_h( person.getSelectedPlan(), 6);
					// yyyy I don't like this.  Maybe set nothing and compensate later? kai, dec'25
				}
			}
		}

		log.info("Writing experienced plans to file: {}", outputExpPlansPath);
		PopulationUtils.writePopulation( population, outputExpPlansPath.toString() );

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
		{
			var options = CsvWriteOptions.builder( outputDir.resolve( config.controller().getRunId() + ".tablesaw.tsv" ).toString() ).separator( '\t' );
			tripsTable.write().usingOptions( options.build() );
		}

		// all the following would need to be separated by subpopulation

		HistogramTrace histogramTrace = HistogramTrace.builder( tripsTable.doubleColumn( HeadersKN.vttsh ) ).build();
		final Layout.LayoutBuilder layoutBuilder = Layout.builder().width( 1000 );
		Figure figure = new Figure( layoutBuilder.build(), histogramTrace );

		Path htmlPath = outputDir.resolve(runPrefix + "histogram.html" );
		TablesawUtils.writeFigureToHtmlFile( htmlPath.toString(), figure );

		log.info( "print summary statistics:");
		final Table muttsStats = tripsTable.summarize( HeadersKN.muttsh, mean, quartile1, median, quartile3, percentile95 ).apply();
		System.out.println( System.lineSeparator() + muttsStats + System.lineSeparator() );
		final Table vttsStats = tripsTable.summarize( HeadersKN.vttsh, mean, quartile1, median, quartile3, percentile95 ).apply();
		System.out.println( vttsStats + System.lineSeparator() );


		{
			var options = CsvWriteOptions.builder( outputDir.resolve( config.controller().getRunId() + ".muttsStats.tsv" ).toString() ).separator( '\t' );
			muttsStats.write().usingOptions( options.build() );
		}
		{
			var options = CsvWriteOptions.builder( outputDir.resolve( config.controller().getRunId() + ".vttsStats.tsv" ).toString() ).separator( '\t' );
			vttsStats.write().usingOptions( options.build() );
		}

//		vttsHandler.printVTTSHistogram( eventsPath.getParent().resolve( config.controller().getRunId() + ".vttsHistogram.tsv" ).toString() );
//		vttsHandler.printVTTS( eventsPath.getParent().resolve( config.controller().getRunId() + ".vtts.tsv" ).toString() );
//		vttsHandler.printAvgVTTSperPerson( eventsPath.getParent().resolve( config.controller().getRunId() + ".vttsPerPerson.tsv" ).toString() );

		return 0;
	}

	private static final String MUTTS_H = "mUTTS_h (incoming trip)";
	public static void setMUTTS_h( Activity activity, double mUTTSh ){
		activity.getAttributes().putAttribute( MUTTS_H, mUTTSh );
	}
	public static Double getMUTTS_h( Activity activity ) {
		return (Double) activity.getAttributes().getAttribute( MUTTS_H );
	}

	private static final String VTTS_H = "VTTS_h (incoming trip)";
	public static void setVTTS_h( Activity activity, double vttSh ){
		activity.getAttributes().putAttribute( VTTS_H, vttSh );
	}
	public static Double getVTTS_h( Activity activity ) {
		return (Double) activity.getAttributes().getAttribute( VTTS_H );
	}

	private static final String MUSE_H = "marginal_utility_of_starting_earlier_h";
	public static void setMUSE_h( Activity activity, double muse_h ){
		activity.getAttributes().putAttribute( MUSE_H, muse_h );
	}
	public static Double getMUSE_h( Activity activity ) {
		Double muse = (Double) activity.getAttributes().getAttribute( MUSE_H );
		if ( muse!=null ){
			return muse;
		} else {
			return (Double) activity.getAttributes().getAttribute( "marginal_utility_of_starting_later_h" );
		}
	}
	public static void setMUSE_h( Plan plan, double muse_h ) {
		plan.getAttributes().putAttribute( MUSE_H, muse_h );
	}
	public static Double getMUSE_h( Plan plan ) {
		return (Double) plan.getAttributes().getAttribute( MUSE_H );
	}

	public static void setActScore( Activity activity, double score ) {
		activity.getAttributes().putAttribute( "activityScore", score );
	}
}
