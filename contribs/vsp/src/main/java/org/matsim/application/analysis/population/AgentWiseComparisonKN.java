package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.OutputDirectoryHierarchy;
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.matsim.api.core.v01.TransportMode.*;
import static org.matsim.application.ApplicationUtils.globFile;
import static org.matsim.application.analysis.population.AgentWiseComparisonKNUtils.deltaColumn;
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

	private ScoringFunctionFactory scoringFunctionFactory;


	public static void main(String[] args) {
		new AgentWiseComparisonKN().execute(args );
	}

	@Override
	public Integer call() throws Exception {
		// yyyyyy we do not read the events from the base case so if there is important info (such as agents stuck in the base case) we ignore it!!
		// --> das stimmt glaube ich nicht.

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
		AgentWiseComparisonKNUtils.cleanPopulation( basePopulation );
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
			AgentWiseComparisonKNUtils.handleEventsfile( baseCasePath, pattern, basePopulation );
			// (most of the time, this should be a filtered events file, and we only use money and stuck info)
		}

		// ===

//		Table baseTableTrips = generateTripsTableFromPopulation( basePopulation, config, true );
		Table baseTableTrips = null;
		Table baseTablePersons = generatePersonTableFromPopulation( basePopulation, config, null );

		// ### next cometh the policy data:

		if (eventsFiles.size() == 1) { // maybe this special case in included in the next?
			String pattern = eventsFiles.getFirst();
			for (Path inputPath : inputPaths) {
				readPolicyDataAndCompare(inputPath, pattern, baseTableTrips, baseTablePersons, basePopulation );
			}
		} else {
			for (String pattern : eventsFiles) {
				readPolicyDataAndCompare( inputPaths.get(eventsFiles.indexOf(pattern ) ), pattern, baseTableTrips, baseTablePersons, basePopulation );
			}
		}
		return 0;
	}

	@NotNull private Table generatePersonTableFromPopulation( Population population, Config config, Population basePopulation ){

		Table table = Table.create( StringColumn.create( PERSON_ID)
//			, IntColumn.create( HeadersKN.TRIP_IDX )
			// per agent:
//			, DoubleColumn.create( UTL_OF_MONEY)
			, DoubleColumn.create( SCORE)
			, DoubleColumn.create( HeadersKN.COMPUTED_SCORE_ERROR )
			, DoubleColumn.create( MONEY)
			, DoubleColumn.create( MONEY_SCORE )
			//, DoubleColumn.create( HeadersKN.MUSL_h ) // only base table, see below
			, DoubleColumn.create( TTIME)
			, DoubleColumn.create( ASCS)
			, DoubleColumn.create( ADDTL_TRAV_SCORE )
			, DoubleColumn.create( ACTS_SCORE )
			, StringColumn.create( MODE_SEQ )
			, StringColumn.create( ACT_SEQ )
								  );

		if ( basePopulation==null ) {
			table.addColumns( /* DoubleColumn.create( MUTTS_H ), */ DoubleColumn.create( UTL_OF_MONEY ), DoubleColumn.create( MUSL_h ) );
		}
		// (This has the advantage that one does not need to run the VTTS code on the policy cases.)

		MainModeIdentifier mainModeIdentifier = new DefaultAnalysisMainModeIdentifier();
		for( Person person : population.getPersons().values() ){
			// yyyyyy much/all of the following needs to be differentiated by subpopulation !!! yyyyyy

			double computedPersonScore = 0.;

			table.stringColumn( PERSON_ID ).append( person.getId().toString() );
			table.doubleColumn( SCORE).append( person.getSelectedPlan().getScore() );

			AgentWiseComparisonKNUtils.processMUoM( basePopulation==null, person, table );

			double actsScore;
			{
				ScoringFunction sf = this.scoringFunctionFactory.createNewScoringFunction( person );
				for( Activity activity1 : TripStructureUtils.getActivities( person.getSelectedPlan(), StageActivityHandling.ExcludeStageActivities ) ){
					sf.handleActivity( activity1 );
				}
				sf.finish();
				actsScore = sf.getScore();

				table.doubleColumn( HeadersKN.ACTS_SCORE ).append( actsScore);
				computedPersonScore += sf.getScore();
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

				modeSeq.add( AgentWiseComparisonKNUtils.shortenModeString( mainModeIdentifier.identifyMainMode( trip.getTripElements() ) ) );
				actSeq.add( trip.getDestinationActivity().getType().substring( 0,4  ) );

				if ( basePopulation==null ){
					Double musl_h = AgentWiseComparisonKNUtils.getMUSL_h( trip.getDestinationActivity() );
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
						} else {
							haveAddedFirstPtAscOfTrip = true;
						}
					}
				}
			}
			// here we are done with the trip loop and now need to memorize the person values:

			table.doubleColumn( TTIME ).append( sumTtimes/3600. );
			// dies erzeugt keinen weiteren score!
			{
				table.doubleColumn( ASCS ).append( sumAscs );
				computedPersonScore += sumAscs;
			}
			{
				table.doubleColumn( ADDTL_TRAV_SCORE ).append( addtlTravelScore );
				computedPersonScore += addtlTravelScore;
			}
			if ( basePopulation==null ){
				if( cntMUSL_H > 0 ){
					table.doubleColumn( MUSL_h ).append( sumMUSL_h / cntMUSL_H );
				} else{
					table.doubleColumn( MUSL_h ).append( 6 );
				}
			}

			// money:
			double moneyScore ;
			{
				double dailyMoney = 0.;
				for( Double value : dailyMoneyByMode.values() ){
					dailyMoney += value;
				}
				table.doubleColumn( MONEY ).append( sumMoney + dailyMoney );
				Person basePerson = person;
				if ( basePopulation!=null ) {
					basePerson = basePopulation.getPersons().get( person.getId() );
				}
				Double marginalUtilityOfMoney = 1.;
				if ( basePerson!=null ) {
					marginalUtilityOfMoney = AgentWiseComparisonKNUtils.getMarginalUtilityOfMoney( basePerson );
				}
				// yyyy We have persons in the policy case which are no longer in the base case.
				moneyScore = (sumMoney + dailyMoney) * marginalUtilityOfMoney;

				table.doubleColumn( HeadersKN.MONEY_SCORE ).append( moneyScore );
				computedPersonScore += moneyScore;

			}

			table.stringColumn( HeadersKN.ACT_SEQ ).append( String.join( "|", actSeq ) );
			table.stringColumn( HeadersKN.MODE_SEQ ).append( String.join( "--", modeSeq ) );

			// difference between matsim score and post-computed score:
			AgentWiseComparisonKNUtils.formatTable( table, 2 );
			final double difference = person.getSelectedPlan().getScore() - computedPersonScore;
			table.doubleColumn( COMPUTED_SCORE_ERROR ).append( difference );
			if ( Math.abs( difference ) > 1e-12 ) {
				System.err.println();
				log.warn( "simulationScore={}; computedScore={}; difference={}", person.getSelectedPlan().getScore(),
					computedPersonScore, difference );
				log.warn( "actsScore={}; addtlTravScore={}; moneyScore={}; sumAscs={}", actsScore, addtlTravelScore, moneyScore, sumAscs );
				Table lastRowTable = table.inRange( table.rowCount() - 1, table.rowCount() );
				System.err.println( lastRowTable.print() );
				System.err.println();
			}
			// ... end person loop:
		}
		{
//			log.info("print table:");
//			System.out.println( table );
		}
		return table;
	}

	private void printSpecificPerson( Table table, String personId ){
		final Table filteredTable = table.where( table.stringColumn( PERSON_ID ).isEqualTo( personId ) );
		AgentWiseComparisonKNUtils.formatTable( filteredTable, 2 );
		log.info( "print table for specific person:" );
		System.out.println( filteredTable );
	}

	private void readPolicyDataAndCompare( Path inputPath, String pattern, Table tripsTableBase, Table personsTableBase, Population basePopulation ) throws IOException {
		log.info("Running on {}", inputPath);

		String baseConfigFilename = globFile( inputPath, "*output_config.xml" ).toString();
		Config config = ConfigUtils.loadConfig(baseConfigFilename);

		String populationFileName = globFile(inputPath, "*vtts_experienced_plans.xml.gz").toString();

		Population policyPopulation = PopulationUtils.readPopulation( populationFileName );
		log.warn( "popSize={}",policyPopulation.getPersons().size());
		AgentWiseComparisonKNUtils.cleanPopulation( policyPopulation );
		log.warn( "popSize={}",policyPopulation.getPersons().size());

		AgentWiseComparisonKNUtils.handleEventsfile( inputPath, pattern, policyPopulation );

		Table personsTablePolicy = generatePersonTableFromPopulation( policyPopulation, config, basePopulation );

		Table joinedTable = personsTableBase.joinOn( PERSON_ID ).inner( true, personsTablePolicy );

		System.out.println( joinedTable );

		printSpecificPerson( joinedTable, "960148" );

		log.info("scoreErrorBase={}", joinedTable.doubleColumn( COMPUTED_SCORE_ERROR ).sum() );
		log.info( "scoreErrorT2={}", joinedTable.doubleColumn( HeadersKN.keyTwoOf( COMPUTED_SCORE_ERROR ) ).sum());

//		log.info("exit");
//		System.exit(-1);

		joinedTable.addColumns( deltaColumn( joinedTable, TTIME ), deltaColumn( joinedTable, MONEY ) );

		joinedTable.addColumns(
			joinedTable.doubleColumn( deltaOf( TTIME ) ).multiply( joinedTable.doubleColumn( MUSL_h ) ).multiply( -1 ).setName( WEIGHTED_TTIME )
//			,joinedTable.doubleColumn( deltaOf( MONEY) ).multiply( joinedTable.doubleColumn( UTL_OF_MONEY ) ).setName( WEIGHTED_MONEY )
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
//			, joinedTable.column( WEIGHTED_MONEY )
			, deltaColumn( joinedTable, MONEY_SCORE )
			, deltaColumn( joinedTable, ASCS )
			, deltaColumn( joinedTable, ADDTL_TRAV_SCORE )
			, deltaColumn( joinedTable, ACTS_SCORE )
			// information:
			, joinedTable.column( MODE_SEQ )
			, joinedTable.column( keyTwoOf( MODE_SEQ ) )
									   );

		AgentWiseComparisonKNUtils.formatTable( deltaTable, 2 );

//		System.out.println( deltaTable );

		printSpecificPerson( deltaTable, "960148" );

		// ===
		{
			final double factor = 1./config.qsim().getFlowCapFactor();
			final double score_sum = deltaTable.doubleColumn( deltaOf( SCORE ) ).sum() * factor;
			final double weighted_ttime_sum = deltaTable.doubleColumn( WEIGHTED_TTIME ).sum() * factor;
//			final double money_score = deltaTable.doubleColumn( WEIGHTED_MONEY ).sum() * factor;
			final double money_score = deltaTable.doubleColumn( deltaOf(MONEY_SCORE) ).sum() * factor;
			final double ascs_sum = deltaTable.doubleColumn( deltaOf( ASCS ) ).sum() * factor;
			final double addtl_trav_score = deltaTable.doubleColumn( deltaOf( ADDTL_TRAV_SCORE ) ).sum() * factor;
			final double acts_score = deltaTable.doubleColumn( deltaOf( ACTS_SCORE ) ).sum() * factor;

			final StringBuilder score_cmt = new StringBuilder( "is the overall benefit (potentially negative) in score space. This has the following contributions:" );
			final StringBuilder weighted_ttime_cmt = new StringBuilder( "... is the travel time benefit (re-weighted by indiv. mUSL)." );
			final StringBuilder weighted_money_cmt = new StringBuilder( "... is the monetary benefit (re-weighted by indiv. mUoM)." );
			final StringBuilder asc_cmt = new StringBuilder( "... are the ASC benefits." );
			final StringBuilder addtl_trav_cmt = new StringBuilder( "... are the additional travel score benefits." );
			final StringBuilder sum_cmt = new StringBuilder( "is the sum of these contributions." );
			final StringBuilder acts_score_cmt = new StringBuilder( "... are the activities score (= pure travel time) benefits." );
			final StringBuilder alt_sum_cmt = new StringBuilder("is the sum of these contributions.") ;

			final int maxLen = score_cmt.length();
			alignLeft( weighted_ttime_cmt, maxLen );
			alignLeft( weighted_money_cmt, maxLen );
			alignLeft( asc_cmt, maxLen );
			alignLeft( addtl_trav_cmt, maxLen );
			alignLeft( sum_cmt, maxLen );
			alignLeft( acts_score_cmt, maxLen );
			alignLeft( alt_sum_cmt, maxLen );

			// ---

			Table summaryTable = Table.create( DoubleColumn.create( "value" ), StringColumn.create( "comment" ) );

			summaryTable.doubleColumn( "value" ).append( score_sum );
			summaryTable.stringColumn( "comment" ).append( score_cmt.toString() );

//			summaryTable.doubleColumn( "value" ).append( weighted_ttime_sum );
//			summaryTable.stringColumn( "comment" ).append( weighted_ttime_cmt.toString() );

			summaryTable.doubleColumn( "value" ).append( acts_score );
			summaryTable.stringColumn( "comment" ).append( acts_score_cmt.toString() );

			summaryTable.doubleColumn( "value" ).append( addtl_trav_score );
			summaryTable.stringColumn( "comment" ).append( addtl_trav_cmt.toString() );

			summaryTable.doubleColumn( "value" ).append( money_score );
			summaryTable.stringColumn( "comment" ).append( weighted_money_cmt.toString() );

			summaryTable.doubleColumn( "value" ).append( ascs_sum );
			summaryTable.stringColumn( "comment" ).append( asc_cmt.toString() );

//			summaryTable.doubleColumn( "value" ).append( weighted_ttime_sum + money_score + ascs_sum + addtl_trav_score );
//			summaryTable.stringColumn( "comment" ).append( sum_cmt.toString() );

			summaryTable.doubleColumn( "value" ).append( acts_score + ascs_sum + addtl_trav_score + money_score );
			summaryTable.stringColumn( "comment" ).append( alt_sum_cmt.toString() );

			AgentWiseComparisonKNUtils.formatTable( summaryTable, 0 );

			System.out.println();
			log.info( "Results rescaled to 100% by multiplying with {}.", factor );
			System.out.println( summaryTable + System.lineSeparator() );
			System.out.println();

		}
		// ===
		// yy die ASCs besser als separate Tabelle
		{
			Table summaryTable = Table.create( DoubleColumn.create( "ASC" ), StringColumn.create( "mode" ));

			for( ModeParams modeParams : config.scoring().getModes().values() ){
				summaryTable.doubleColumn( "ASC" ).append( modeParams.getConstant() );
				summaryTable.stringColumn( "mode" ).append( modeParams.getMode() ) ;
			}

			AgentWiseComparisonKNUtils.formatTable(  summaryTable, 2 );

			System.out.println( System.lineSeparator() + summaryTable + System.lineSeparator() );

		}
		// ---



	}

	private static void alignLeft( StringBuilder weighted_ttime_cmt, int maxLen ){
		weighted_ttime_cmt.append( " ".repeat( maxLen - weighted_ttime_cmt.length() ) );
	}


}
