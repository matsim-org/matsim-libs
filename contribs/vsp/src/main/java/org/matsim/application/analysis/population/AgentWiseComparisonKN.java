package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.DefaultAnalysisMainModeIdentifier;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import picocli.CommandLine;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.matsim.api.core.v01.TransportMode.*;
import static org.matsim.application.ApplicationUtils.globFile;
import static org.matsim.application.analysis.population.AddVttsEtcToActivities.getMUSE_h;
import static org.matsim.application.analysis.population.AgentWiseComparisonKNUtils.*;
import static org.matsim.application.analysis.population.AgentWiseComparisonKNUtils.deltaColumn;
import static org.matsim.application.analysis.population.AgentWiseComparisonKNUtils.getAnalysisPopulation;
import static org.matsim.application.analysis.population.HeadersKN.*;
import static org.matsim.core.config.groups.ScoringConfigGroup.*;
import static org.matsim.core.population.PersonUtils.getMarginalUtilityOfMoney;

@CommandLine.Command(name = "monetary-utility", description = "List and compare fare, dailyRefund and utility values for agents in base and policy case.")
public class AgentWiseComparisonKN implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger( AgentWiseComparisonKN.class );
	public static final String KN_MONEY = "knMoney";


	@CommandLine.Parameters(description = "Path to run output directory for which analysis should be performed.")
	private Path inputPath;

	@CommandLine.Option(names = "--base-path", description = "Path to run directory of base case.", required = true)
	private Path baseCasePath;

	@CommandLine.Option(names = "--prefix", description = "Prefix for filtered events output file, optional. This can be a list of multiple prefixes.", split = ",")
	private List<String> prefixList = new ArrayList<>();
	// (yy not clear to me how this works.  Different prefixes for different input paths? kai, nov'25)

	ScoringFunctionFactory scoringFunctionFactory;
	MutableScenario scenario;
	private List<PreparedGeometry> geometries;
	TripRouter tripRouter;
	TripRouter tripRouter2;


	public static void main(String[] args) {
		new AgentWiseComparisonKN().execute(args );
	}

	@Override
	public Integer call() throws Exception {
		// yyyyyy we do not read the events from the base case so if there is important info (such as agents stuck in the base case) we ignore it!!
		// --> das stimmt glaube ich nicht.

		List<String> eventsFilePatterns = new ArrayList<>();

		if (!prefixList.isEmpty()) {
			for (String prefix : prefixList) {
				eventsFilePatterns.add("*" + prefix + "output_events_filtered.xml.gz");
			}
		} else {
			eventsFilePatterns.add("*output_events.xml.gz");
		}

		String baseFacilitiesFilename = globFile( baseCasePath, "*output_" + Controler.DefaultFiles.facilities.getFilename() + ".gz" ).toString();
		String baseTransitScheduleFilename = globFile( baseCasePath, "*output_" + Controler.DefaultFiles.transitSchedule.getFilename() + ".gz" ).toString();
		String baseNetworkFilename = globFile( baseCasePath, "*output_" + Controler.DefaultFiles.network.getFilename() + ".gz" ).toString();

		String baseConfigFilename = globFile( baseCasePath, "*output_config_reduced.xml" ).toString();
		// (The reduced config has fewer problems with newly introduced config params.)

		Config baseConfig = ConfigUtils.loadConfig(baseConfigFilename);
		baseConfig.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );

		baseConfig.scoring().addActivityParams( new ActivityParams( TripStructureUtils.createStageActivityType( car ) ).setScoringThisActivityAtAll( false ) );
		baseConfig.scoring().addActivityParams( new ActivityParams( TripStructureUtils.createStageActivityType( bike ) ).setScoringThisActivityAtAll( false ) );
		baseConfig.scoring().addActivityParams( new ActivityParams( TripStructureUtils.createStageActivityType( walk ) ).setScoringThisActivityAtAll( false ) );
		baseConfig.scoring().addActivityParams( new ActivityParams( TripStructureUtils.createStageActivityType( pt ) ).setScoringThisActivityAtAll( false ) );

		baseConfig.counts().setInputFile( null );

		// yy might be easier to read the scenario in a systematic way
		// --> well, not really, since the path names in the config file use https

		scenario = ScenarioUtils.createMutableScenario( baseConfig );

		final Population basePopulation = readAndCleanPopulation( baseCasePath, eventsFilePatterns);

		computeAndSetMarginalUtilitiesOfMoney( basePopulation );

		// ---

		scenario.setPopulation( basePopulation );
		// (need a scenario for injection!)

		URL url = Paths.get( "/Users/kainagel/runs-svn/gartenfeld/caseStudies/v6.4-cutout/drt-case-study/output-gartenfeld-v6.4-cutout-10pct-drt/analysis/drt/serviceArea.shp" ).toUri().toURL();
		this.geometries = ShpGeometryUtils.loadPreparedGeometries( url );

		new MatsimFacilitiesReader(scenario).readFile( baseFacilitiesFilename );
		new TransitScheduleReader( scenario ).readFile( baseTransitScheduleFilename );
		NetworkUtils.readNetwork( scenario.getNetwork(), baseNetworkFilename );
		{
			com.google.inject.Injector injector = new Injector.InjectorBuilder( scenario )
													  .addStandardModules()
													  .addOverridingModule( new AbstractModule(){
														  @Override public void install(){
															  bind( ScoringParametersForPerson.class ).to(
																  IncomeDependentUtilityOfMoneyPersonScoringParameters.class );
														  }
													  } )
													  .build();
			this.scoringFunctionFactory = injector.getInstance( ScoringFunctionFactory.class );

			this.tripRouter = injector.getInstance( TripRouter.class );
			// (this is quite expensive and should thus be done later. kai, dec'25)
		}
		{
			String policyTransitScheduleFilename = globFile( inputPath, "*output_" + Controler.DefaultFiles.transitSchedule.getFilename() + ".gz" ).toString();
			String policyNetworkFilename = globFile( inputPath, "*output_" + Controler.DefaultFiles.network.getFilename() + ".gz" ).toString();
			MutableScenario scenario2 = ScenarioUtils.createMutableScenario( baseConfig );
			new MatsimNetworkReader( scenario2.getNetwork() ).readFile( policyNetworkFilename );
			scenario2.setActivityFacilities( scenario.getActivityFacilities() );
			scenario2.setPopulation( scenario.getPopulation() );
			new TransitScheduleReader( scenario2 ).readFile( policyTransitScheduleFilename );
			com.google.inject.Injector injector2 = new Injector.InjectorBuilder( scenario2 )
													   .addStandardModules()
													   .addOverridingModule( new AbstractModule(){
														   @Override public void install(){ bind( ScoringParametersForPerson.class ).to( IncomeDependentUtilityOfMoneyPersonScoringParameters.class ); }
													   } )
													   .build();
			this.tripRouter2 = injector2.getInstance( TripRouter.class );
		}

		// ===

		// yyyyyy !!!!!! We now have a different avgIncome in the scoring fct than in the general population.  !!!!!!! yyyyyy
		// --> (presumably) compute the mUoM here instead of in the preproc.

		tagPersonsToAnalyse( basePopulation, geometries, scenario );

//		Table baseTableTrips = generateTripsTableFromPopulation( basePopulation, config, true );
		Table baseTableTrips = null;
		Table baseTablePersons = generatePersonTableFromPopulation( basePopulation, baseConfig, null );

		// ### next cometh the policy data:

		String policyConfigFilename = globFile( inputPath, "*output_config_reduced.xml" ).toString();
		// (The reduced config has fewer problems with newly introduced config params.)

		Config policyConfig = ConfigUtils.loadConfig(policyConfigFilename);

		MutableScenario policyScenario = ScenarioUtils.createMutableScenario( policyConfig );
		policyScenario.setNetwork(  this.scenario.getNetwork() );
		policyScenario.setTransitSchedule( this.scenario.getTransitSchedule() );

		Population policyPopulation = readAndCleanPopulation( inputPath, eventsFilePatterns);
		policyScenario.setPopulation( policyPopulation );

//		AgentWiseComparisonKNUtils.writeSpecialPopulationForVia( inputPath, policyPopulation );

		Table personsTablePolicy = generatePersonTableFromPopulation( policyPopulation, policyConfig, basePopulation );

		compare( policyScenario, personsTablePolicy, baseTableTrips, baseTablePersons, this.scenario, baseConfig, inputPath );

		return 0;
	}
	@NotNull  Table generatePersonTableFromPopulation( Population population, Config config, Population basePopulation ){

		Table table = Table.create( StringColumn.create( PERSON_ID)
//			, IntColumn.create( HeadersKN.TRIP_IDX )
			// per agent:
//			, DoubleColumn.create( UTL_OF_MONEY)
			, DoubleColumn.create( SCORE)
			, DoubleColumn.create( MONEY)
			, DoubleColumn.create( MONEY_SCORE )
			//, DoubleColumn.create( HeadersKN.MUSL_h ) // only base table, see below
			, DoubleColumn.create( TTIME)
			, DoubleColumn.create( ASCS)
//			, DoubleColumn.create( ADDTL_TRAV_SCORE )
			, DoubleColumn.create( U_TRAV_DIRECT )
			, DoubleColumn.create( U_LINESWITCHES )
			, DoubleColumn.create( ACTS_SCORE )
			, StringColumn.create( MODE_SEQ )
			, StringColumn.create( ACT_SEQ )
			, StringColumn.create( HeadersKN.ANALYSIS_POPULATION )
								  );

		if ( basePopulation==null ) {
			table.addColumns( /* DoubleColumn.create( MUTTS_H ), */ DoubleColumn.create( UTL_OF_MONEY ), DoubleColumn.create( MUSL_h ) );
		}
		// (This has the advantage that one does not need to run the VTTS code on the policy cases.)

		MainModeIdentifier mainModeIdentifier = new DefaultAnalysisMainModeIdentifier();
		PopulationFactory pf = population.getFactory();
		double popSumMuse_h = 0.;
		double popCntMuse_h = 0.;
		for( Person person : population.getPersons().values() ){
			// yyyyyy much/all of the following needs to be differentiated by subpopulation !!! yyyyyy

			table.stringColumn( ANALYSIS_POPULATION ).append( getAnalysisPopulation( person ) );

			double computedPersonScore = 0.;

			table.stringColumn( PERSON_ID ).append( person.getId().toString() );
//			table.doubleColumn( SCORE).append( person.getSelectedPlan().getScore() );
			// is now computed; see further down. kai, dec'25

			processMUoM( basePopulation==null, person, table );

			{
				ScoringFunction sf = this.scoringFunctionFactory.createNewScoringFunction( person );
				Activity firstActivity = null;
				double sumMuse_h = 0.;
				double cntMuse_h = 0.;
				for( Activity activity1 : TripStructureUtils.getActivities( person.getSelectedPlan(), StageActivityHandling.ExcludeStageActivities ) ){
					sf.handleActivity( activity1 );
					if ( basePopulation==null ) {
						if ( activity1.getStartTime().isDefined() && activity1.getEndTime().isDefined() ){
							// Ihab-style MarginalSumScoringFct computation:
							ScoringFunction sfNormal = this.scoringFunctionFactory.createNewScoringFunction( person );
							ScoringFunction sfEarly = this.scoringFunctionFactory.createNewScoringFunction( person );
							double scoreNormalBefore = sfNormal.getScore();
							double scoreEarlyBefore = sfEarly.getScore();
							sfNormal.handleActivity( activity1 );
							Activity earlyActivity = pf.createActivityFromLinkId( activity1.getType(), activity1.getLinkId() );
							PopulationUtils.copyFromTo( activity1, earlyActivity );
							earlyActivity.setStartTime( earlyActivity.getStartTime().seconds() - 1 );
							sfEarly.handleActivity( earlyActivity );
							sfNormal.finish();
							sfEarly.finish();
							double scoreDiffNormal = sfNormal.getScore() - scoreNormalBefore;
							double scoreDiffEarly = sfEarly.getScore() - scoreEarlyBefore;
							final double muse_h = (scoreDiffEarly - scoreDiffNormal) * 3600.;
							AddVttsEtcToActivities.setMUSE_h( activity1, muse_h );
							sumMuse_h += muse_h;
							cntMuse_h ++;
						} else if ( activity1.getStartTime().isUndefined() ) {
							firstActivity = activity1;
						} else {
							Gbl.assertIf( activity1.getEndTime().isUndefined() );
							// Ihab-style MarginalSumScoringFct computation:
							ScoringFunction sfNormal = this.scoringFunctionFactory.createNewScoringFunction( person );
							ScoringFunction sfEarly = this.scoringFunctionFactory.createNewScoringFunction( person );
							double scoreNormalBefore = sfNormal.getScore();
							double scoreEarlyBefore = sfEarly.getScore();
							// treat the after-midnight-activity:
							sfNormal.handleActivity( firstActivity );
							sfEarly.handleActivity( firstActivity );
							// handle the before-midnight-activity (this part of the code is the same as above!)
							sfNormal.handleActivity( activity1 );
							Activity earlyActivity = pf.createActivityFromLinkId( activity1.getType(), activity1.getLinkId() );
							PopulationUtils.copyFromTo( activity1, earlyActivity );
							earlyActivity.setStartTime( earlyActivity.getStartTime().seconds() - 1 );
							sfEarly.handleActivity( earlyActivity );
							sfNormal.finish();
							sfEarly.finish();
							double scoreDiffNormal = sfNormal.getScore() - scoreNormalBefore;
							double scoreDiffEarly = sfEarly.getScore() - scoreEarlyBefore;
							final double muse_h = (scoreDiffEarly - scoreDiffNormal) * 3600.;
							AddVttsEtcToActivities.setMUSE_h( activity1, muse_h );
							sumMuse_h += muse_h;
							cntMuse_h ++;
						}
					}
				}
				sf.finish();

				table.doubleColumn( HeadersKN.ACTS_SCORE ).append( sf.getScore() );
				computedPersonScore += sf.getScore();

				if ( basePopulation==null ) {
					AddVttsEtcToActivities.setMUSE_h( person.getSelectedPlan(), sumMuse_h/cntMuse_h );
				}
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
//			double addtlTravelScore = 0.;
			double directTravelScore = 0.;
			double lineSwitchesScore = 0.;
			double sumMUSL_h = 0.;
			double cntMUSL_H = 0.;
			List<String> modeSeq = new ArrayList<>();
			List<String> actSeq = new ArrayList<>();
			boolean firstTrip = true;
			for( TripStructureUtils.Trip trip : TripStructureUtils.getTrips( person.getSelectedPlan() ) ){
				// per trip:

				if ( firstTrip ) {
					firstTrip = false;
					actSeq.add( trip.getOriginActivity().getType().substring( 0,4  ) );
				}
				modeSeq.add( shortenModeString( mainModeIdentifier.identifyMainMode( trip.getTripElements() ) ) );
				actSeq.add( trip.getDestinationActivity().getType().substring( 0,4  ) );

				if ( basePopulation==null ){
					Double musl_h = getMUSE_h( trip.getDestinationActivity() );
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
					directTravelScore += leg.getTravelTime().seconds()/3600. * modeParams.getMarginalUtilityOfTraveling() ;

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
							lineSwitchesScore += subpopScoringParams.getUtilityOfLineSwitch();
						} else {
							haveAddedFirstPtAscOfTrip = true;
						}
					}
				}
			}
			// here we are done with the trip loop and now need to memorize the person values:

			table.doubleColumn( TTIME ).append( sumTtimes/3600. );
			// (dies erzeugt keinen weiteren score!)
			{
				table.doubleColumn( ASCS ).append( sumAscs );
				computedPersonScore += sumAscs;
			}
			{
				table.doubleColumn( U_TRAV_DIRECT ).append( directTravelScore );
				computedPersonScore += directTravelScore;
				table.doubleColumn( U_LINESWITCHES ).append( lineSwitchesScore );
				computedPersonScore += lineSwitchesScore;
			}
			if ( basePopulation==null ){
				if( cntMUSL_H > 0 ){
					table.doubleColumn( MUSL_h ).append( sumMUSL_h / cntMUSL_H );
				} else{
					table.doubleColumn( MUSL_h ).append( 6 ); // yyyy ????
				}
				popSumMuse_h += sumMUSL_h / cntMUSL_H;
				popCntMuse_h ++; // person-based weight; could also justify trip-based weight as more policy-relevant. kai, dec'25
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
					marginalUtilityOfMoney = getMarginalUtilityOfMoney( basePerson );
				}
				// yyyy We have persons in the policy case which are no longer in the base case.
				moneyScore = (sumMoney + dailyMoney) * marginalUtilityOfMoney;

				table.doubleColumn( HeadersKN.MONEY_SCORE ).append( moneyScore );
				computedPersonScore += moneyScore;

			}

			table.doubleColumn( SCORE ).append( computedPersonScore );
			table.stringColumn( HeadersKN.ACT_SEQ ).append( String.join( "|", actSeq ) );
			table.stringColumn( HeadersKN.MODE_SEQ ).append( String.join( "--", modeSeq ) );

			formatTable( table, 2 );

			// ... end person loop:
		}
		if ( basePopulation==null ){
			MUTTS_AV = popSumMuse_h / popCntMuse_h;
			log.warn( "MUTTS_AV={}; popSumMuse_h={}; popCntMuse_h={}", MUTTS_AV, popSumMuse_h, popCntMuse_h );
		}

		return table;
	}

	void compare( Scenario policyScenario, Table personsTablePolicy, Table tripsTableBase, Table personsTableBase, Scenario baseScenario, Config baseConfig, Path outputPath ) throws IOException {

		somehowComputeRuleOfHalf( baseScenario.getPopulation(), policyScenario.getPopulation(), personsTablePolicy );

		Table joinedTable = personsTableBase.joinOn( PERSON_ID ).inner( true, personsTablePolicy );

		log.info( "print joined table:" );
		System.out.println( joinedTable );

//		printSpecificPerson( joinedTable, "960148" );

		joinedTable.addColumns( deltaColumn( joinedTable, TTIME ), deltaColumn( joinedTable, MONEY ) );

		joinedTable.addColumns(
//			joinedTable.doubleColumn( deltaOf( TTIME ) ).multiply( joinedTable.doubleColumn( MUSL_h ) ).multiply( -1 ).setName( WEIGHTED_TTIME )
//			,joinedTable.doubleColumn( deltaOf( MONEY) ).multiply( joinedTable.doubleColumn( UTL_OF_MONEY ) ).setName( WEIGHTED_MONEY )
		);

		joinedTable = joinedTable.where(
			joinedTable.stringColumn( ANALYSIS_POPULATION ).isEqualTo( "true" ).or (
				joinedTable.stringColumn( keyTwoOf( ANALYSIS_POPULATION ) ).isEqualTo( "true" )
																				   ) );
//		joinedTable = joinedTable.where( joinedTable.stringColumn( MODE_SEQ ).isEqualTo( joinedTable.stringColumn( keyTwoOf( MODE_SEQ ) ) ) );
		joinedTable = joinedTable.where( joinedTable.stringColumn( keyTwoOf( MODE_SEQ ) ).containsString( "pt" ) );


		Table deltaTable = Table.create( joinedTable.column( PERSON_ID )
			, joinedTable.column( UTL_OF_MONEY )
//			, joinedTable.column( MUSL_h )
			, joinedTable.column( SCORE )
			, joinedTable.column( MONEY )
			, joinedTable.column( TTIME )
			, joinedTable.column( ASCS )
			, joinedTable.column( U_TRAV_DIRECT )
			, joinedTable.column( U_LINESWITCHES)
//			, joinedTable.column( ADDTL_TRAV_SCORE )
			// unweighted deltas:
			, deltaColumn( joinedTable, TTIME )
			, deltaColumn( joinedTable, MONEY )
			// delta computation:
			, deltaColumn( joinedTable, SCORE )
//			, joinedTable.column( WEIGHTED_TTIME )
//			, joinedTable.column( WEIGHTED_MONEY )
			, deltaColumn( joinedTable, ACTS_SCORE )
			, deltaColumn( joinedTable, U_TRAV_DIRECT )
			, deltaColumn( joinedTable, U_LINESWITCHES )
			, deltaColumn( joinedTable, MONEY_SCORE )
			, deltaColumn( joinedTable, ASCS )
			//
			, joinedTable.column( W1_TTIME_DIFF_REM )
			, joinedTable.column( W2_TTIME_DIFF_REM )
			, joinedTable.column( IX_DIFF_REMAINING )
			, joinedTable.column( W1_TTIME_DIFF_SWI )
			, joinedTable.column( W2_TTIME_DIFF_SWI )
			, joinedTable.column( IX_DIFF_SWITCHING )
			// information:
			, joinedTable.column( MODE_SEQ )
			, joinedTable.column( keyTwoOf( MODE_SEQ ) )
									   );

		formatTable( deltaTable, 0 );

		System.out.println( deltaTable.sortOn( deltaOf( SCORE ) ) );

		// ===

		writeMatsimScoresSummaryTable( outputPath, baseConfig, deltaTable );

		writeRuleOfHalfSummaryTable( inputPath, baseConfig, deltaTable );

		writeAscTable( baseConfig );

	}

	private void somehowComputeRuleOfHalf( Population basePopulation, Population policyPopulation, Table personsTablePolicy ){
		var ttimeRemHom = DoubleColumn.create( W1_TTIME_DIFF_REM );
		var ttimeRemHet = DoubleColumn.create( W2_TTIME_DIFF_REM );
		var ixRem = DoubleColumn.create( IX_DIFF_REMAINING );
		var ttimeSwiHom = DoubleColumn.create( W1_TTIME_DIFF_SWI );
		var ttimeSwiHet = DoubleColumn.create( W2_TTIME_DIFF_SWI );
		var ixSwi = DoubleColumn.create( IX_DIFF_SWITCHING );

		for( Person policyPerson : policyPopulation.getPersons().values() ){
			double sumTravTimeDiffsRemainersHom = 0.;
			double sumTravTimeDiffsRemainersHet = 0.;
			double sumTravTimeDiffsSwitchersHom = 0.;
			double sumTravTimeDiffsSwitchersHet = 0.;

			double sumIchangesDiffsRemainers = 0;
			double sumIchangesDiffsSwitchers = 0.;

			Person basePerson = basePopulation.getPersons().get( policyPerson.getId() );

			if ( basePerson!=null ){
				Double musl = (Double) getMUSE_h( basePerson.getSelectedPlan() );
				if ( musl==null ) {
					log.warn("muse is null; I do not know why; personId={}", basePerson.getId() );
					musl = MUTTS_AV;
				}
				List<TripStructureUtils.Trip> baseTrips = TripStructureUtils.getTrips( basePerson.getSelectedPlan() );
				List<TripStructureUtils.Trip> policyTrips = TripStructureUtils.getTrips( policyPerson.getSelectedPlan() );
				Gbl.assertIf( baseTrips.size() == policyTrips.size() );
				for( int ii = 0 ; ii < baseTrips.size() ; ii++ ){
					final TripStructureUtils.Trip policyTrip = policyTrips.get( ii );
					final String policyMainMode = TripStructureUtils.identifyMainMode( policyTrip.getTripElements() );
					if( TransportMode.pt.equals( policyMainMode ) ){
						TripStructureUtils.Trip baseTrip = baseTrips.get( ii );
						if( TransportMode.pt.equals( TripStructureUtils.identifyMainMode( baseTrip.getTripElements() ) ) ){
							// Altnutzer; compute base ttime etc. from actual trip:
							double sumBaseTtime = 0.;
							double sumBaseLineSwitches = -1;
							for( Leg leg : baseTrip.getLegsOnly() ){
								sumBaseTtime += leg.getTravelTime().seconds();
								if( TransportMode.pt.equals( leg.getMode() ) ){
									sumBaseLineSwitches++;
								}
							}
							// compute policy ttime from actual trip:
							double sumPolicyTtime = 0.;
							double sumPolicyLineSwitches = -1.;
							for( Leg leg : policyTrip.getLegsOnly() ){
								sumPolicyTtime += leg.getTravelTime().seconds();
								if( TransportMode.pt.equals( leg.getMode() ) ){
									sumPolicyLineSwitches++;
								}
							}
							log.warn("MUTTS_AV={}", MUTTS_AV);
							sumTravTimeDiffsRemainersHom += (sumPolicyTtime - sumBaseTtime) * MUTTS_AV * (-1);
							sumTravTimeDiffsRemainersHet += (sumPolicyTtime - sumBaseTtime) * musl * (-1);
							sumIchangesDiffsRemainers += (sumPolicyLineSwitches - sumBaseLineSwitches);
						} else{
							// Neunutzer; rule-of-half
							// first need hypothetical base travel time
							final Result baseResult = routeTrip( baseTrip, policyMainMode, basePerson );
							// then compute hypothetical policy travel time
							final Result policyResult = routeTrip2( policyTrip, policyMainMode, policyPerson );
							// sum up (rule-of-half is done later):
							sumTravTimeDiffsSwitchersHom += (policyResult.sumTtime() - baseResult.sumTtime()) * MUTTS_AV * (-1);
							sumTravTimeDiffsSwitchersHet += (policyResult.sumTtime() - baseResult.sumTtime()) * musl * (-1);
							sumIchangesDiffsSwitchers += (policyResult.sumLineSwitches() - baseResult.sumLineSwitches());
						}
					}
				}
			}
			ttimeRemHom.append( sumTravTimeDiffsRemainersHom/3600. );
			ttimeRemHet.append( sumTravTimeDiffsRemainersHet/3600. );
			ttimeSwiHom.append( sumTravTimeDiffsSwitchersHom/3600. );
			ttimeSwiHet.append( sumTravTimeDiffsSwitchersHet/3600. );
			ixRem.append( sumIchangesDiffsRemainers );
			ixSwi.append( sumIchangesDiffsSwitchers );
		}
		personsTablePolicy.addColumns( ttimeRemHom, ttimeRemHet, ixRem, ttimeSwiHom, ttimeSwiHet, ixSwi );
		log.info("persons table policy after adding RoH entries:");
		System.out.println(personsTablePolicy);
	}

	private @NotNull Result routeTrip( TripStructureUtils.Trip baseTrip, String policyMainMode, Person basePerson ){
		Facility fromFacility = FacilitiesUtils.toFacility( baseTrip.getOriginActivity(), scenario.getActivityFacilities() );
		Facility toFacility = FacilitiesUtils.toFacility( baseTrip.getDestinationActivity(), scenario.getActivityFacilities() );
		final List<? extends PlanElement> planElements = this.tripRouter.calcRoute( policyMainMode, fromFacility, toFacility,
			baseTrip.getOriginActivity().getEndTime().seconds(), basePerson, null );

		// count the number of line switches:
		double sumBaseTtime = 0.;
		double sumBaseLineSwitches = -1;
		for( Leg leg : TripStructureUtils.getLegs( planElements ) ){
			sumBaseTtime += leg.getTravelTime().seconds();
			if( TransportMode.pt.equals( leg.getMode() ) ){
				sumBaseLineSwitches++;
			}
		}

		// return the result:
		return new Result( sumBaseTtime, sumBaseLineSwitches );
	}

	private @NotNull Result routeTrip2( TripStructureUtils.Trip baseTrip, String policyMainMode, Person basePerson ){
		Facility fromFacility = FacilitiesUtils.toFacility( baseTrip.getOriginActivity(), scenario.getActivityFacilities() );
		Facility toFacility = FacilitiesUtils.toFacility( baseTrip.getDestinationActivity(), scenario.getActivityFacilities() );
		final List<? extends PlanElement> planElements = this.tripRouter2.calcRoute( policyMainMode, fromFacility, toFacility,
			baseTrip.getOriginActivity().getEndTime().seconds(), basePerson, null );

		// count the number of line switches:
		double sumBaseTtime = 0.;
		double sumBaseLineSwitches = -1;
		for( Leg leg : TripStructureUtils.getLegs( planElements ) ){
			sumBaseTtime += leg.getTravelTime().seconds();
			if( TransportMode.pt.equals( leg.getMode() ) ){
				sumBaseLineSwitches++;
			}
		}

		// return the result:
		return new Result( sumBaseTtime, sumBaseLineSwitches );
	}

	private record Result(double sumTtime, double sumLineSwitches){
	}

}
