package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.DefaultAnalysisMainModeIdentifier;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import picocli.CommandLine;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.io.IOException;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.*;

import static org.matsim.api.core.v01.TransportMode.*;
import static org.matsim.application.ApplicationUtils.globFile;
import static org.matsim.application.analysis.population.HeadersKN.*;
import static org.matsim.core.config.groups.ScoringConfigGroup.*;

@CommandLine.Command(name = "monetary-utility", description = "List and compare fare, dailyRefund and utility values for agents in base and policy case.")
public class AgentWiseComparisonKN implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger( AgentWiseComparisonKN.class );
	public static final String KN_MONEY = "knMoney";


	@CommandLine.Parameters(arity = "1..*", description = "Path to run output directories for which analysis should be performed.")
	private List<Path> inputPaths;

	@CommandLine.Option(names = "--base-path", description = "Path to run directory of base case.", required = true)
	private Path baseCasePath;

	@CommandLine.Option(names = "--prefix", description = "Prefix for filtered events output file, optional. This can be a list of multiple prefixes. " +
		"Number of prefixes has to be equal to number of inputPaths and the list of prefixes has to have the same order as inputPaths.", split = ",")
	private List<String> prefixList = new ArrayList<>();
	// (yy not totally obvious to me how this works.  Different prefixes for different input paths? kai, nov'25)

	NumberFormat format = NumberFormat.getNumberInstance( Locale.GERMANY);
	private ScoringFunctionFactory scoringFunctionFactory;


	public static void main(String[] args) {
		new AgentWiseComparisonKN().execute(args );
	}

	@Override
	public Integer call() throws Exception {
		// yyyyyy we do not read the events from the base case so if there is important info (such as agents stuck in the base case) we ignore it!!
		// --> das stimmt glaube ich nicht.

		format.setMaximumFractionDigits(2);
		format.setMinimumFractionDigits( 2 );

		if (!prefixList.isEmpty() && prefixList.size() != inputPaths.size()) {
			log.error("The numbers of prefixes {} and input paths {} do not match.", prefixList, inputPaths);
			return 2;
		}

		List<String> eventsFiles = new ArrayList<>();

		if (!prefixList.isEmpty()) {
			for (String prefix : prefixList) {
				eventsFiles.add("*" + prefix + "output_events_filtered.xml.gz");
			}
		} else {
			eventsFiles.add("*output_events.xml.gz");
		}

		String basePopulationFilename = globFile( baseCasePath, "*vtts_experienced_plans.xml.gz" ).toString();
		String baseConfigFilename = globFile( baseCasePath, "*output_config_reduced.xml" ).toString();
		// (The reduced config has fewer problems with newly introduced config params.)

		Config config = ConfigUtils.loadConfig(baseConfigFilename);
		config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );

		config.scoring().addActivityParams( new ActivityParams( TripStructureUtils.createStageActivityType( car ) ).setScoringThisActivityAtAll( false ) );
		config.scoring().addActivityParams( new ActivityParams( TripStructureUtils.createStageActivityType( bike ) ).setScoringThisActivityAtAll( false ) );
		config.scoring().addActivityParams( new ActivityParams( TripStructureUtils.createStageActivityType( walk ) ).setScoringThisActivityAtAll( false ) );
		config.scoring().addActivityParams( new ActivityParams( TripStructureUtils.createStageActivityType( pt ) ).setScoringThisActivityAtAll( false ) );

		Population basePopulation = PopulationUtils.readPopulation( basePopulationFilename );
		log.warn( "popSize={}",basePopulation.getPersons().size());
		cleanPopulation( basePopulation );
		log.warn( "popSize={}",basePopulation.getPersons().size());

		// ---

		MutableScenario scenario = ScenarioUtils.createMutableScenario( config );
		scenario.setPopulation( basePopulation );
		// (need a scenario for the following!)

		com.google.inject.Injector injector = new Injector.InjectorBuilder( scenario )
												  .addStandardModules()
												  .addOverridingModule( new AbstractModule(){
													  @Override public void install(){
														  bind( ScoringParametersForPerson.class ).to( IncomeDependentUtilityOfMoneyPersonScoringParameters.class );
													  }
												  } )
												  .build();
		this.scoringFunctionFactory = injector.getInstance( ScoringFunctionFactory.class );

		// ---

		for (String pattern : eventsFiles) {
			handleEventsfile( baseCasePath, pattern, basePopulation );
			// (most of the time, this should be a filtered events file, and we only use money and stuck info)
		}

		// ===

//		Table baseTableTrips = generateTripsTableFromPopulation( basePopulation, config, true );
		Table baseTableTrips = null;
		Table baseTablePersons = generatePersonTableFromPopulation( basePopulation, config, true );

		// ### next cometh the policy data:

		if (eventsFiles.size() == 1) { // maybe this special case in included in the next?
			String pattern = eventsFiles.getFirst();
			for (Path inputPath : inputPaths) {
				readPolicyDataAndCompare(inputPath, pattern, baseTableTrips, baseTablePersons );
			}
		} else {
			for (String pattern : eventsFiles) {
				readPolicyDataAndCompare( inputPaths.get(eventsFiles.indexOf(pattern ) ), pattern, baseTableTrips, baseTablePersons );
			}
		}
		return 0;
	}
	private static void handleEventsfile( Path path, String pattern, Population population ){
		String baseEventsFile = globFile( path, pattern ).toString();

		double popSizeBefore = population.getPersons().size();

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler( new MyMoneyEventsHandler( population ) );
		events.addHandler( new MyStuckEventsHandler( population ) );
		events.initProcessing();

		MatsimEventsReader baseReader = new MatsimEventsReader( events );
		baseReader.readFile( baseEventsFile );
		events.finishProcessing();

		log.warn("popSize before={}; popSize after={}; ", popSizeBefore, population.getPersons().size() );
	}

	@NotNull private Table generatePersonTableFromPopulation( Population population, Config config, boolean isBaseTable ){

		Table table = Table.create( StringColumn.create( PERSON_ID)
//			, IntColumn.create( HeadersKN.TRIP_IDX )
			// per agent:
//			, DoubleColumn.create( UTL_OF_MONEY)
			, DoubleColumn.create( SCORE)
			, DoubleColumn.create( MONEY)
			//, DoubleColumn.create( HeadersKN.MUSL_h ) // only base table, see below
			, DoubleColumn.create( TTIME)
			, DoubleColumn.create( ASCS)
			, DoubleColumn.create( ADDTL_TRAV_SCORE )
			, DoubleColumn.create( ACTS_SCORE )
			, StringColumn.create( MODE_SEQ )
			, StringColumn.create( ACT_SEQ )
								  );

		if ( isBaseTable ) {
			table.addColumns( /* DoubleColumn.create( MUTTS_H ), */ DoubleColumn.create( UTL_OF_MONEY ), DoubleColumn.create( MUSL_h ) );
		}
		// (This has the advantage that one does not need to run the VTTS code on the policy cases.)

		MainModeIdentifier mainModeIdentifier = new DefaultAnalysisMainModeIdentifier();
		for( Person person : population.getPersons().values() ){
			// yyyyyy much/all of the following needs to be differentiated by subpopulation !!! yyyyyy


			table.stringColumn( PERSON_ID ).append( person.getId().toString() );
			table.doubleColumn( SCORE).append( person.getSelectedPlan().getScore() );

			processMUoM( isBaseTable, person, table );

			{
				ScoringFunction sf = this.scoringFunctionFactory.createNewScoringFunction( person );
				for( Activity activity1 : TripStructureUtils.getActivities( person.getSelectedPlan(),
					StageActivityHandling.ExcludeStageActivities ) ){
					sf.handleActivity( activity1 );
				}
				sf.finish();
				table.doubleColumn( HeadersKN.ACTS_SCORE ).append( sf.getScore() );
			}

			double sumMoney = 0.;
			Double moneyFromEvents = (Double) person.getAttributes().getAttribute( KN_MONEY );
			if ( moneyFromEvents!=null ) {
				sumMoney += moneyFromEvents ;
			};
			Map<String,Double> dailyMoneyByMode = new TreeMap<>();



//			double sumWeightedTtime = 0.;
			double sumTtimes = 0.;
			double sumAscs = 0.;
			double addtlTravelScore = 0.;
			double sumMUSL_h = 0.;
			double cntMUSL_H = 0.;
			List<String> modeSeq = new ArrayList<>();
			List<String> actSeq = new ArrayList<>();
			for( TripStructureUtils.Trip trip : TripStructureUtils.getTrips( person.getSelectedPlan() ) ){
				// per trip:

				modeSeq.add( shortenModeString( mainModeIdentifier.identifyMainMode( trip.getTripElements() ) ) );
				actSeq.add( trip.getDestinationActivity().getType().substring( 0,4  ) );

				if ( isBaseTable ){
					Double musl_h = getMUSL_h( trip.getDestinationActivity() );
					if( musl_h != null && musl_h > 0 && musl_h < 16.30 ){
						sumMUSL_h += musl_h;
						cntMUSL_H ++;
					}
				}

				boolean haveAddedFirstPtAscOfTrip = false;
				for( Leg leg : trip.getLegsOnly() ){
					final ScoringParameterSet subpopScoringParams = config.scoring().getScoringParameters( PopulationUtils.getSubpopulation( person ) );
					final ModeParams modeParams = subpopScoringParams.getModes().get( leg.getMode() );

					// ttime:
					sumTtimes += leg.getTravelTime().seconds();
					addtlTravelScore += leg.getTravelTime().seconds()/3600. * modeParams.getMarginalUtilityOfTraveling() ;

					// money:
					sumMoney += leg.getRoute().getDistance() * modeParams.getMonetaryDistanceRate();

					dailyMoneyByMode.put( leg.getMode(), modeParams.getDailyMonetaryConstant() );
					// we only want this once!

					// ascs:
					sumAscs += modeParams.getConstant();
					if ( TransportMode.pt.equals( leg.getMode() ) ) {
						if ( haveAddedFirstPtAscOfTrip ){
							//deduct this again:
							sumAscs -= modeParams.getConstant();
							// instead, add the (dis)utility of line switch:
							addtlTravelScore += subpopScoringParams.getUtilityOfLineSwitch();
//							if ( isTestPerson( person.getId() ) ) {
//								log.warn( "personId={}; addtlTravelScore={};", person.getId(), addtlTravelScore );
//							}
						} else {
							haveAddedFirstPtAscOfTrip = true;
						}
					}
				}
			}
			// here we are done with the trip loop and now need to memorize the person values:

			table.doubleColumn( TTIME ).append( sumTtimes/3600. );
			table.doubleColumn( ASCS ).append( sumAscs );
			table.doubleColumn( ADDTL_TRAV_SCORE ).append( addtlTravelScore );
			if ( isBaseTable ){
				if( cntMUSL_H > 0 ){
					table.doubleColumn( MUSL_h ).append( sumMUSL_h / cntMUSL_H );
				} else{
					table.doubleColumn( MUSL_h ).append( 6 );
				}
			}

			// money:
			double dailyMoney = 0.;
			for( Double value : dailyMoneyByMode.values() ){
				dailyMoney += value;
			}
			table.doubleColumn( MONEY ).append( sumMoney + dailyMoney );

			table.stringColumn( HeadersKN.ACT_SEQ ).append( String.join( "|", actSeq ) );
			table.stringColumn( HeadersKN.MODE_SEQ ).append( String.join( "--", modeSeq ) );

			// ... end person loop.
		}
		for( Column<?> column : table.columns() ){
			if ( column instanceof DoubleColumn ) {
				((DoubleColumn) column).setPrintFormatter( format, "n/a" );
			}
		}
		{
			log.info("print table:");
			System.out.println( table );
		}
		printSpecificPerson( table );
		return table;
	}

	private static int wrnCnt = 0;

	static void processMUoM( boolean isBaseTable, Person person, Table table ){
		if ( isBaseTable ){
			final Double marginalUtilityOfMoney = getMarginalUtilityOfMoney( person );
			if ( marginalUtilityOfMoney != null ){
				table.doubleColumn( UTL_OF_MONEY ).append( marginalUtilityOfMoney );
			} else {
				table.doubleColumn( UTL_OF_MONEY ).append( 1. );
				if ( wrnCnt<10 ){
					log.warn( "marginalUtlOfMoney is null; personId={}", person.getId() );
					wrnCnt++;
					if( wrnCnt == 10 ){
						log.warn( Gbl.FUTURE_SUPPRESSED );
					}
				}
			}
		}
	}

	private void printSpecificPerson( Table table ){
		final Table filteredTable = table.where( table.stringColumn( PERSON_ID ).isEqualTo( "960148" ) );
		for( Column<?> column : filteredTable.columns() ){
			if( column instanceof DoubleColumn ){
				((DoubleColumn) column).setPrintFormatter( format, "n/a" );
			}
		}
		log.info( "print table for specific person:" );
		System.out.println( filteredTable );
	}

	private void readPolicyDataAndCompare( Path inputPath, String pattern, Table tripsTableBase, Table personsTableBase ) throws IOException {
		log.info("Running on {}", inputPath);

		String baseConfigFilename = globFile( inputPath, "*output_config.xml" ).toString();
		Config config = ConfigUtils.loadConfig(baseConfigFilename);

		String populationFileName = globFile(inputPath, "*vtts_experienced_plans.xml.gz").toString();

		Population policyPopulation = PopulationUtils.readPopulation( populationFileName );
		log.warn( "popSize={}",policyPopulation.getPersons().size());
		cleanPopulation( policyPopulation );
		log.warn( "popSize={}",policyPopulation.getPersons().size());

		handleEventsfile( inputPath, pattern, policyPopulation );

		Table personsTablePolicy = generatePersonTableFromPopulation( policyPopulation, config, false );

		Table joinedTable = personsTableBase.joinOn( PERSON_ID ).inner( true, personsTablePolicy );

		System.out.println( joinedTable );

		printSpecificPerson( joinedTable );

		joinedTable.addColumns( deltaColumn( joinedTable, TTIME ), deltaColumn( joinedTable, MONEY ) );

		joinedTable.addColumns(
			joinedTable.doubleColumn( deltaOf( TTIME ) ).multiply( joinedTable.doubleColumn( MUSL_h ) ).multiply( -1 ).setName( WEIGHTED_TTIME ),
			joinedTable.doubleColumn( deltaOf( MONEY) ).multiply( joinedTable.doubleColumn( UTL_OF_MONEY ) ).setName( WEIGHTED_MONEY )
		);

		Table deltaTable = Table.create( joinedTable.column( PERSON_ID )
			, joinedTable.column( UTL_OF_MONEY )
			, joinedTable.column( MUSL_h )
			, joinedTable.column( SCORE )
			, joinedTable.column( MONEY )
			, joinedTable.column( TTIME )
			, joinedTable.column( ASCS )
			, joinedTable.column( ADDTL_TRAV_SCORE )
			// unweighted deltas:
			, deltaColumn( joinedTable, TTIME )
			, deltaColumn( joinedTable, MONEY )
			// delta computation:
			, deltaColumn( joinedTable, SCORE )
			, joinedTable.column( WEIGHTED_TTIME )
			, joinedTable.column( WEIGHTED_MONEY )
			, deltaColumn( joinedTable, ASCS )
			, deltaColumn( joinedTable, ADDTL_TRAV_SCORE )
			, deltaColumn( joinedTable, ACTS_SCORE )
			// information:
			, joinedTable.column( MODE_SEQ )
			, joinedTable.column( keyTwoOf( MODE_SEQ ) )
									   );


		formatTable( deltaTable, 2 );

		System.out.println( deltaTable );

		printSpecificPerson( deltaTable );

		// ===
		{

			final double score_sum = deltaTable.doubleColumn( deltaOf( SCORE ) ).sum();
			final double weighted_ttime_sum = deltaTable.doubleColumn( WEIGHTED_TTIME ).sum();
			final double weighted_money_sum = deltaTable.doubleColumn( WEIGHTED_MONEY ).sum();
			final double ascs_sum = deltaTable.doubleColumn( deltaOf( ASCS ) ).sum();
			final double addtl_trav_score = deltaTable.doubleColumn( deltaOf( ADDTL_TRAV_SCORE ) ).sum();
			final double sumOfContributions = addtl_trav_score + weighted_money_sum + ascs_sum + addtl_trav_score;
			final double acts_score = deltaTable.doubleColumn( deltaOf( ACTS_SCORE ) ).sum();

			final StringBuilder score_cmt = new StringBuilder(
				"is the overall benefit (potentially negative) in score space. This has the following contributions:" );
			final StringBuilder weighted_ttime_cmt = new StringBuilder( "... is the travel time benefit (re-weighted by indiv. mUSL)." );
			final StringBuilder weighted_money_cmt = new StringBuilder( "... is the monetary benefit (re-weighted by indiv. mUoM)." );
			final StringBuilder asc_cmt = new StringBuilder( "... are the ASC benefits." );
			final StringBuilder addtl_trav_cmt = new StringBuilder( "... are the additional travel score benefits." );
			final StringBuilder sum_cmt = new StringBuilder( "is the sum of these contributions." );
			final StringBuilder acts_score_cmt = new StringBuilder( "... are the activites score benefits." );

			final int maxLen = score_cmt.length();
			alignLeft( weighted_ttime_cmt, maxLen );
			alignLeft( weighted_money_cmt, maxLen );
			alignLeft( asc_cmt, maxLen );
			alignLeft( addtl_trav_cmt, maxLen );
			alignLeft( sum_cmt, maxLen );
			alignLeft( acts_score_cmt, maxLen );

			// ---

			Table summaryTable = Table.create( DoubleColumn.create( "value" ), StringColumn.create( "comment" ) );

			summaryTable.doubleColumn( "value" ).append( score_sum );
			summaryTable.stringColumn( "comment" ).append( score_cmt.toString() );

			summaryTable.doubleColumn( "value" ).append( weighted_ttime_sum );
			summaryTable.stringColumn( "comment" ).append( weighted_ttime_cmt.toString() );

			summaryTable.doubleColumn( "value" ).append( weighted_money_sum );
			summaryTable.stringColumn( "comment" ).append( weighted_money_cmt.toString() );

			summaryTable.doubleColumn( "value" ).append( ascs_sum );
			summaryTable.stringColumn( "comment" ).append( asc_cmt.toString() );

			summaryTable.doubleColumn( "value" ).append( addtl_trav_score );
			summaryTable.stringColumn( "comment" ).append( addtl_trav_cmt.toString() );

			summaryTable.doubleColumn( "value" ).append( sumOfContributions );
			summaryTable.stringColumn( "comment" ).append( sum_cmt.toString() );

			summaryTable.doubleColumn( "value" ).append( acts_score );
			summaryTable.stringColumn( "comment" ).append( acts_score_cmt.toString() );

			formatTable( summaryTable, 0 );

			System.out.println( System.lineSeparator() + "!! The following is NOT rescaled to 100% !!" );
			System.out.println( summaryTable + System.lineSeparator() );

		}
		// ===
		// yy die ASCs besser als separate Tabelle
		{
			Table summaryTable = Table.create( DoubleColumn.create( "ASC" ), StringColumn.create( "mode" ));

			for( ModeParams modeParams : config.scoring().getModes().values() ){
				summaryTable.doubleColumn( "ASC" ).append( modeParams.getConstant() );
				summaryTable.stringColumn( "mode" ).append( modeParams.getMode() ) ;
			}

			formatTable(  summaryTable, 2 );

			System.out.println( System.lineSeparator() + summaryTable + System.lineSeparator() );

		}
		// ---



	}
	@NotNull private static void alignLeft( StringBuilder weighted_ttime_cmt, int maxLen ){
		weighted_ttime_cmt.append( " ".repeat( maxLen - weighted_ttime_cmt.length() ) );
	}
	private void formatTable( Table deltaTable, int nDigits ){
		format.setMaximumFractionDigits( nDigits );
		format.setMinimumFractionDigits( 0 );
		for( Column<?> column : deltaTable.columns() ){
			if ( column instanceof DoubleColumn ) {
				((DoubleColumn) column).setPrintFormatter( format, "n/a" );
			}
		}
	}
	private void tripWiseDifferenceTable( Table tableBase, Table tablePolicy ){
		// yy There might be a more direct way to do the table join; I didn't find it.  (I now think that the main issue is that
		// under some circumstances the join column name of the second table needs to be given and I had some syntax where this was
		// not necessary and the error message was not helpful.)
		{
			final StringColumn columnToAdd = tablePolicy.stringColumn( PERSON_ID ).concatenate( "-" ).concatenate( tablePolicy.intColumn( TRIP_IDX ).asStringColumn() ).setName( "abc" );
			tablePolicy.addColumns( columnToAdd );
		}

		// Compute overlapping columns (excluding join keys)
		Set<String> leftCols = new HashSet<>( tableBase.columnNames());
		Set<String> rightCols = new HashSet<>( tablePolicy.columnNames());

//		leftCols.remove( PERSON_ID);
//		leftCols.remove( TRIP_IDX);
//		rightCols.remove( PERSON_ID);
//		rightCols.remove( TRIP_IDX);

		Set<String> duplicates = new HashSet<>(leftCols);
		duplicates.retainAll(rightCols);

		// Rename duplicates in right-hand table
		for (String dup : duplicates) {
			tablePolicy.column( dup ).setName( keyTwoOf(  dup ) );
		}

//		Table filteredTableBase = tableBase.where( tableBase.stringColumn( MODE_SEQ ).containsString("car").andNot( tableBase.stringColumn( MODE_SEQ ).containsString( "eCar" ) ) );
		Table filteredTableBase = tableBase;

//		Table filteredTablePolicy = tablePolicy.where( tablePolicy.stringColumn( keyTwoOf(HeadersKN.MODE_SEQ) ).containsString( "drt" ) );
//		Table filteredTablePolicy = tablePolicy.where( tablePolicy.stringColumn( HeadersKN.keyTwoOf(HeadersKN.MODE_SEQ ) ).containsString( "eCar" ) );
		Table filteredTablePolicy = tablePolicy;

		System.out.println( filteredTableBase.summary() );
		System.out.println( filteredTablePolicy.summary() );

		Table joinedTable = filteredTableBase.joinOn( "abc" ).inner( filteredTablePolicy, "abc_r");

		joinedTable.addColumns( deltaColumn( joinedTable, TTIME ) );
		{
			// The effect of the direct marginal utl of travelling (beta_trav) is already included in the (dis)utl computation of the corresponding legs.  Here we only need the effect on the activity times, thus MUSL:
			final DoubleColumn newColumn = joinedTable.doubleColumn( deltaOf( TTIME ) ).multiply( joinedTable.doubleColumn(
				MUSL_h ) ).multiply( -1 ).setName( deltaOf(  WEIGHTED_TTIME ) );
			joinedTable.addColumns( newColumn );
		}
		joinedTable.addColumns( deltaColumn( joinedTable, MONEY ) );
		{
			final DoubleColumn newColumn = joinedTable.doubleColumn( deltaOf( MONEY ) ).multiply( joinedTable.doubleColumn( UTL_OF_MONEY ) ).setName( deltaOf( WEIGHTED_MONEY) );
			joinedTable.addColumns( newColumn );
		}
		joinedTable.addColumns( deltaColumn( joinedTable, ADDTL_TRAV_SCORE ) );


		Table deltaTable = Table.create( joinedTable.column( PERSON_ID), joinedTable.column( TRIP_IDX )
			, joinedTable.column( SCORE )
//			, joinedTable.column( keyTwoOf( SCORE ) )
			, joinedTable.column( TTIME )
			, joinedTable.column( deltaOf( TTIME ) )
//			, joinedTable.column( MUTTS_H )
			, joinedTable.column( MUSL_h )
			, joinedTable.column( deltaOf( MONEY ) )
			, joinedTable.column( UTL_OF_MONEY)
			, joinedTable.column( ASCS )
			//
			, deltaColumn( joinedTable, SCORE)
			, joinedTable.column( deltaOf( WEIGHTED_TTIME ) )
			, joinedTable.column( deltaOf( WEIGHTED_MONEY ) )
			, deltaColumn( joinedTable, ASCS)
			, joinedTable.column( deltaOf( ADDTL_TRAV_SCORE ) )
			//
			, joinedTable.column( MODE )
			, joinedTable.column( keyTwoOf( MODE ) )
			, joinedTable.column( ACT_AT_END )
									   );

		deltaTable.write().usingOptions( CsvWriteOptions.builder( "deltaTable.tsv" ).separator( '\t' ).build() );

		log.warn("###");
		log.warn(
			"D_SCORE_MEAN=" + deltaTable.doubleColumn( deltaOf( SCORE ) ).mean()
				+"; d_w_ttime_mean=" + deltaTable.doubleColumn( deltaOf( WEIGHTED_TTIME ) ).mean()
				+ "; d_w_money=" + deltaTable.where( deltaTable.numberColumn( TRIP_IDX ).isEqualTo( 0 ) ).doubleColumn( deltaOf( WEIGHTED_MONEY) ).mean()
				+ "; d_w_ascs=" + deltaTable.doubleColumn( deltaOf( ASCS ) ).mean()
				+ "; d_addtlTScore=" + deltaTable.doubleColumn( deltaOf( ADDTL_TRAV_SCORE ) ).mean()
				+ "; d_ttime_mean=" + deltaTable.doubleColumn( deltaOf( TTIME) ).mean() / 3600 * 6
				);
		log.warn("###");

		Table sortedTable = deltaTable.sortOn( deltaOf( SCORE), TRIP_IDX );

		// I can set the format to columns that already exist at this stage:
		for( Column<?> column : sortedTable.columns() ){
			if ( column instanceof DoubleColumn ) {
				((DoubleColumn) column).setPrintFormatter( format, "n/a" );
			}
		}

		log.info( "sorted table coming here ..." );
		System.out.println( sortedTable );
	}
	private DoubleColumn deltaColumn( Table joinedTable, String key ){
		return joinedTable.doubleColumn( keyTwoOf( key ) ).subtract( joinedTable.doubleColumn( key ) ).setName( deltaOf( key ) );
	}

	static boolean isTestPerson( Id<Person> personId ){
		return switch( personId.toString() ){
			case
//				"766222", "459926", "1279437", "1055071", "1083364", "1450752", "114301" , "203311",
				 // lausitz:
//				 "1012515",
				 "960148",
					// dresden:
					"1084690","34371","843219","588488",
				// berlin:
				 "berlin_c5d528ea"
					-> true;
			default -> false;
		};
	}

	private static void cleanPopulation( Population basePopulation ){
		List<Id<Person>> personsToRemove = new ArrayList<>();
		for( Person person : basePopulation.getPersons().values() ){
			Id<Person> personId = person.getId();
//			if ( personId.toString().contains("goods") || personId.toString().contains("commercial") || personId.toString().contains("freight")  ) {
			if ( !"person".equals( PopulationUtils.getSubpopulation( person ) ) ) {
				personsToRemove.add( person.getId() );
			}
		}
		for( Id<Person> personId : personsToRemove ){
			basePopulation.removePerson( personId );
		}
	}

	static String shortenModeString( String string ) {
		return string.replace( "electric_car", "eCar" ).replace( "electric_ride", "eRide" );
	}

	private static class MyMoneyEventsHandler implements PersonMoneyEventHandler{
		private final Population population;
		public MyMoneyEventsHandler( Population population ){
			this.population = population;
		}
		@Override public void handleEvent( PersonMoneyEvent event ){
			Person person = population.getPersons().get( event.getPersonId() );
			Double moneyAttrib = (Double) person.getAttributes().getAttribute( KN_MONEY );
			if ( moneyAttrib == null ) {
				person.getAttributes().putAttribute( KN_MONEY, event.getAmount() );
			} else {
				person.getAttributes().putAttribute( KN_MONEY, moneyAttrib + event.getAmount() );
			}
		}
	}

	private static class MyStuckEventsHandler implements PersonStuckEventHandler {
		private final Population population;
		public MyStuckEventsHandler( Population population ){
			this.population = population;
		}
		@Override public void handleEvent( PersonStuckEvent event ){
			Person result = population.removePerson( event.getPersonId() );
		}
	}

	//	public static void setMUTTS_h( Activity activity, double mUTTSh ){
//		activity.getAttributes().putAttribute( MUTTS_H, mUTTSh );
//	}
	public static Double getMUTTS_h( Activity activity ) {
		return (Double) activity.getAttributes().getAttribute( "mUTTS_h (incoming trip)" );
	}

	//	public static void setVTTS_h( Activity activity, double vttSh ){
//		activity.getAttributes().putAttribute( VTTS_H, vttSh );
//	}
	public static Double getVTTS_h( Activity activity ) {
		return (Double) activity.getAttributes().getAttribute( "VTTS_h (incoming trip)" );
	}

	public static Double getMarginalUtilityOfMoney( Person person ) {
		return (Double) person.getAttributes().getAttribute( "marginalUtilityOfMoney" );
	}
//	public static void setMarginalUtilityOfMoney( Person person, double marginalUtilityOfMoney ) {
//		person.getAttributes().putAttribute( MARGINAL_UTILITY_OF_MONEY, marginalUtilityOfMoney );
//	}

//	public static void setMUSL_h( Activity activity, double musl_h ){
//		activity.getAttributes().putAttribute( MUSL_h, musl_h );
//	}
	public static Double getMUSL_h( Activity activity ) {
		return (Double) activity.getAttributes().getAttribute( "marginal_utility_of_starting_later_h" );
	}


}
