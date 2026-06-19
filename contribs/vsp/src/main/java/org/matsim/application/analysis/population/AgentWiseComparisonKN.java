package org.matsim.application.analysis.population;

import com.google.inject.Injector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.DefaultAnalysisMainModeIdentifier;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import picocli.CommandLine;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Table;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.matsim.api.core.v01.TransportMode.*;
import static org.matsim.application.ApplicationUtils.globFile;
import static org.matsim.application.analysis.population.AddVttsEtcToActivities.getMUSE_h;
import static org.matsim.application.analysis.population.AgentWiseComparisonKNUtils.*;
import static org.matsim.application.analysis.population.HeadersKN.*;
import static org.matsim.core.config.groups.ScoringConfigGroup.*;
import static org.matsim.core.controler.Controler.*;
import static org.matsim.core.population.PersonUtils.getMarginalUtilityOfMoney;
import static org.matsim.core.router.TripStructureUtils.StageActivityHandling.ExcludeStageActivities;

@CommandLine.Command(name = "monetary-utility", description = "List and compare fare, dailyRefund and utility values for agents in base and policy case.")
public class AgentWiseComparisonKN implements MATSimAppCommand{
	private static final Logger log = LogManager.getLogger( AgentWiseComparisonKN.class );

	public static final String KN_MONEY = "knMoney";

	private static int scoreWrnCnt = 0;

	private static final boolean doRoh = false;
	static final boolean doSwitchers = true;
	static final boolean isDebugging = true;

	@CommandLine.Parameters(description = "Path to run output directory for which analysis should be performed.")
	private Path policyCasePath;

	@CommandLine.Option(names = "--base-path", description = "Path to run directory of base case.", required = true)
	private Path baseCasePath;

	@CommandLine.Option(names = "--prefix", description = "Prefix for filtered events output file, optional. This can be a list of multiple prefixes.", split = ",")
	private List<String> prefixList = new ArrayList<>();
	// (yy not clear to me how this works.  Different prefixes for different input paths? kai, nov'25)

	// (--> I think that it allowed, either by design or by accident, EITHER multiple prefixes in the same dir,
	// OR one different prefix per different directory.  I think that I removed the second functionality some time ago.)

	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();

	Scenario baseScenario;
	private List<PreparedGeometry> geometries;

	Injector baseCaseInjector;
	Injector policyInjector;

	private static final String onlyMoneyAndStuck = "onlyMoneyAndStuck.";

	public static void main( String[] args ){
		Gbl.assertIf( args==null || args.length==0 );

		String shpFile = null;

		// equil:
//	private static final String baseDir="/Users/kainagel/git/all-matsim/matsim-example-project/referenceOutput/";
//	private static final String policyDir="/Users/kainagel/git/all-matsim/matsim-example-project/referenceOutput/";

		// gartenfeld:
//		final String baseDir="/Users/kainagel/runs-svn/gartenfeld/caseStudies/v6.4-cutout/base-case-ctd/output-gartenfeld-v6.4-cutout-10pct-base-case-ctd/";
//		final String policyDir="/Users/kainagel/runs-svn/gartenfeld/caseStudies/v6.4-cutout/siemensbahn-case-study/output-gartenfeld-v6.4-cutout-10pct-siemensbahn/";

//		 zez:
//		final String baseDir="/Users/kainagel/shared-svn/projects/zez/b_wo_zez/";
//		final String policyDir="/Users/kainagel/shared-svn/projects/zez/c_w_zez/";
//		final String shpFile="../berlin.shp";
//		 yyyy consider a 10 or even 1pct sample of the population

		// meckel:
//		final String baseDir="/Users/kainagel/runs-svn/Abschlussarbeiten/2025/Niklas.Meckel.U0/meckel/run8_NF_500it_10pct/output/";
//		final String policyDir="/Users/kainagel/runs-svn/Abschlussarbeiten/2025/Niklas.Meckel.U0/meckel/run14_U0_500it_10pct/output/";

		// Eduardo Lima:
//		final String baseDir="/Users/kainagel/runs-svn/Abschlussarbeiten/2025/Eduardo_Lima_Siemensbahn/output_final/output-Base_Case-10pct-nach-3900it-S21Jungfernheide/";
//		final String policyDir="/Users/kainagel/runs-svn/Abschlussarbeiten/2025/Eduardo_Lima_Siemensbahn/output_final/output-SiBa-10pct-nach-3900it-S21Jungfernheide/";

		// matsim-dresden wrap-around experiment:
		// -- "old"
//		final String baseDir="/Users/kainagel/runs-svn/tramola-moritz/matsim-dresden/experiments/policies/1pct-base-ctd-wrap-around-handling-none-it500-20260218";
//		final String policyDir="/Users/kainagel/runs-svn/tramola-moritz/matsim-dresden/experiments/policies/1pct-policy-wrap-around-handling-none-it500-20260218";

		// -- "old" with cleaned routes:
//		final String baseDir="/Users/kainagel/runs-svn/tramola-moritz/matsim-dresden/experiments/policies/1pct-base-ctd-wrap-around-handling-none-it500-routesCleaned-20260224";
//		final String policyDir="/Users/kainagel/runs-svn/tramola-moritz/matsim-dresden/experiments/policies/1pct-policy-wrap-around-handling-none-it500-routesCleaned-20260218";

		// -- "new":
//		final String baseDir="/Users/kainagel/runs-svn/tramola-moritz/matsim-dresden/experiments/policies/1pct-base-ctd-wrap-around-handling-splitAndRemoveOpeningTimes-it500-20260218";
//		final String policyDir = "/Users/kainagel/runs-svn/tramola-moritz/matsim-dresden/experiments/policies/1pct-policy-wrap-around-handling-splitAndRemoveOpeningTimes-it500-20260218";

		// -- "new" with cleaned routes:
//		final String baseDir = "/Users/kainagel/runs-svn/tramola-moritz/matsim-dresden/experiments/policies/1pct-base-ctd-wrap-around-handling-splitAndRemoveOpeningTimes-it500-cleaned-20260224";
// 		final String policyDir = "/Users/kainagel/runs-svn/tramola-moritz/matsim-dresden/experiments/policies/1pct-policy-wrap-around-handling-splitAndRemoveOpeningTimes-it500-routesCleaned-20260218";

		// do last iteration again:
//		final String baseDir = "/Users/kainagel/runs-svn/tramola-moritz/matsim-dresden/experiments/policies/tests/1pct-base-ctd-wrap-around-handling-splitAndRemoveOpeningTimes-it0-20260315";
//		final String policyDir = "/Users/kainagel/runs-svn/tramola-moritz/matsim-dresden/experiments/policies/tests/1pct-policy-wrap-around-handling-splitAndRemoveOpeningTimes-it0-20260315";

		// iatbr glamobi:
//		final String baseDir="/Users/kainagel/runs-svn/IATBR/baseCaseContinued";
//		final String policyDir = "/Users/kainagel/runs-svn/IATBR/baseCaseContinued";

		// paul:
// 		final String baseDir="/Users/kainagel/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.4/output/berlin-v6.4-10pct";
//		final String policyDir="/Users/kainagel/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.4/output/berlin-v6.4-10pct";

		// autofrei:
//		final String baseDir="/Users/kainagel/runs-svn/matsim-berlin/autofrei/1pct-v6.4/berlin-autofrei-v6.4-baseCaseCtdExtended/";
//		final String policyDir="/Users/kainagel/runs-svn/matsim-berlin/autofrei/1pct-v6.4/berlin-autofrei-v6.4-policy";

		// lausitz ditrimo
		final String baseDir="/Users/kainagel/public-svn/matsim/scenarios/countries/de/lausitz/projects/DiTriMo/v2.0/00_base-case-ctd/";
		final String policyDir="/Users/kainagel/public-svn/matsim/scenarios/countries/de/lausitz/projects/DiTriMo/v2.0/02_drt-case-study/no-pooling-pt-fare/output-1-ruhland-bhf_full_plans/";
//		final String policyDir="/Users/kainagel/public-svn/matsim/scenarios/countries/de/lausitz/projects/DiTriMo/v2.0/02_drt-case-study/no-pooling-0-fare/output-1-ruhland-bhf_full_plans/";

		// ===

		generateExperiencedPlans( baseDir );
//		generateExperiencedPlans( policyDir );
		generateFilteredEventsFile( baseDir );
//		generateFilteredEventsFile( policyDir );
//		agentWiseComparison( baseDir, policyDir, shpFile );
	}

	// ---

	// the following methods stay here despite being static since they essentially belong to "main".
	private static void agentWiseComparison( String baseDir, String policyDir, String shpFile ){
		String[] args;
		if ( shpFile != null ){
			args = new String[]{"--prefix=" + onlyMoneyAndStuck, "--base-path=" + baseDir, "--shp=" + baseDir + "/" + shpFile, policyDir };
		} else {
			args = new String[]{"--prefix=" + onlyMoneyAndStuck, "--base-path=" + baseDir, policyDir };
		}
		new AgentWiseComparisonKN().execute( args );
	}
	private static void generateFilteredEventsFile( String baseDir ){
		String inFileName = globFile( Path.of( baseDir ),  "*output_" + DefaultFiles.events.getFilename() + ".gz" ).toString();
		String outFileName = baseDir + "/" + onlyMoneyAndStuck+"output_events_filtered.xml.gz";
		// (yy Das lässt die runId weg.)

		Config config = ConfigUtils.createConfig();
		EventsManager eventsManager = EventsUtils.createEventsManager( config );
		MatsimEventsReader reader = new MatsimEventsReader( eventsManager );
		eventsManager.initProcessing();

		EventWriterXML eventsWriter = new EventWriterXML( outFileName );
		eventsManager.addHandler( new BasicEventHandler(){
			@Override public void handleEvent( Event event ){
				if ( event instanceof PersonMoneyEvent || event instanceof PersonStuckEvent ) {
					eventsWriter.handleEvent( event );
				}
			}
		} );
		reader.readFile( inFileName );
		eventsManager.finishProcessing();

		eventsWriter.closeFile();
	}
	private static void generateExperiencedPlans( String baseDir ){
		String[] args;
		args = new String[]{
			"--path", baseDir,
			//				"--runId", runId
		};
		new ExperiencedPlansWriter().execute( args );
	}

	// ===

	@Override public Integer call() throws Exception{
		if ( shp.isDefined() ){
			this.geometries = ShpGeometryUtils.loadPreparedGeometries( Paths.get( shp.getShapeFile() ).toUri().toURL() );
		}

		List<String> eventsFilePatterns = new ArrayList<>();
		if( !prefixList.isEmpty() ){
			for( String prefix : prefixList ){
				eventsFilePatterns.add( "*" + prefix + "output_events_filtered.xml.gz" );
			}
		} else{
			eventsFilePatterns.add( "*output_events.xml.gz" );
		}

		// ############################
		// ############################
		// ### base case data:

		final Config baseConfig = prepareConfig( baseCasePath );

		// ===

		baseScenario = ScenarioUtils.loadScenario( baseConfig );

		cleanPopulation( baseCasePath, eventsFilePatterns, baseScenario.getPopulation() );

		computeAndSetMarginalUtilitiesOfMoney( baseScenario );
		computeAndSetIncomeDeciles( baseScenario.getPopulation() );

		// ===

		this.baseCaseInjector = createInjector( baseScenario );

		// ###

//		Table baseTableTrips = generateTripsTableFromPopulation( basePopulation, config, true );
		Table baseTableTrips = null;
		Table baseTablePersons = generatePersonTableFromPopulation( baseCaseInjector, null );

		baseTablePersons.addColumns( baseTablePersons.doubleColumn( MATSIM_SCORE ).subtract( baseTablePersons.doubleColumn( SCORE ) ).setName( "error" ) );

		printTable( baseTablePersons.sortOn( "error" ), "sorted by score differences bw mw and self-computed" );
		writeMuseHtml( baseTablePersons, policyCasePath );
		writeVseHtml( baseTablePersons, policyCasePath );

		// ############################
		// ############################
		// ### policy data:

		Config policyConfig = prepareConfig( policyCasePath );

		// ===

		Scenario policyScenario = ScenarioUtils.loadScenario( policyConfig );

		cleanPopulation( policyCasePath, eventsFilePatterns, policyScenario.getPopulation() );

		tagPersonsToAnalyse( policyScenario.getPopulation(), geometries, baseScenario );
		// (tagging is better than removing since (1) one can have multiple tags in one go, and (2) the average income becomes different once we start removing people)

		// ===

		this.policyInjector = createInjector( policyScenario );

		Table personsTablePolicy = generatePersonTableFromPopulation( policyInjector, baseScenario.getPopulation() );

		// ############################
		// ############################
		// ### compare:

		compare( baseCaseInjector, policyInjector, personsTablePolicy, baseTableTrips, baseTablePersons, policyCasePath );

		return 0;
	}

	@NotNull Table generatePersonTableFromPopulation( Injector injector, Population basePopulation ){
		final boolean isBaseTable = (basePopulation == null);

		Config config = injector.getInstance( Config.class );
		Population population = injector.getInstance( Population.class );
		ScoringFunctionFactory scoringFunctionFactory = injector.getInstance( ScoringFunctionFactory.class );

		Table table = createPopulationTable();

		if( isBaseTable ){
			table.addColumns( DoubleColumn.create( UTL_OF_MONEY ), DoubleColumn.create( MUSE_h ), IntColumn.create( INCOME_DECILE ) );
		}

		MainModeIdentifier mainModeIdentifier = new DefaultAnalysisMainModeIdentifier();
		double popSumMuse_h = 0.;
		double popCntMuse_h = 0.;
		Counter counter = new Counter( "in method generatePersonTableFromPopulation(...); processing person #  ");
		MuseComputation museComputation = baseCaseInjector.getInstance( MuseComputation.class );

		// yyyyyy much/all of the following needs to be differentiated by subpopulation !!! yyyyyy

		for( Person person : population.getPersons().values() ){
			counter.incCounter();

			table.stringColumn( PERSON_ID ).append( person.getId().toString() );
			table.doubleColumn( MATSIM_SCORE ).append( person.getSelectedPlan().getScore() );
			table.stringColumn( ANALYSIS_POPULATION ).append( getIsInShp( person ) );

			if ( isBaseTable ){
				processMUoM( person, table );
				table.intColumn( INCOME_DECILE ).append( getIncomeDecileBetween0And9( person ) );
				museComputation.computeMuseForAllActs( person.getSelectedPlan() );
			}

			double computedPersonScore = 0.;
			// yy since we are doing the tablesaw columns, may not need the overall sum here.
			// --> or put first into person attributes and generate tablesaw at the end
			{
				// activity times:
				ScoringFunction sf = scoringFunctionFactory.createNewScoringFunction( person );
				TripStructureUtils.getActivities( person.getSelectedPlan(), ExcludeStageActivities ).stream().forEach( act -> sf.handleActivity( act ) );
				sf.finish();
				table.doubleColumn( HeadersKN.ACTS_SCORE ).append( sf.getScore() );
				computedPersonScore += sf.getScore();
			}

			double sumMoney = 0.;
			{
				// money from events.  money from normal scoring comes later
				Double moneyFromEvents = (Double) person.getAttributes().getAttribute( KN_MONEY );
				if( moneyFromEvents != null ){
					sumMoney += moneyFromEvents;
				}
			}

			Map<String, Double> dailyMoneyByMode = new TreeMap<>();

			double sumTtimes = 0.;
			double sumAscs = 0.;
			double directTravelScore = 0.;
			double lineSwitchesScore = 0.;
			double sumMuse_h = 0.;
			double cntMuse_h = 0.;
			List<String> modeSeq = new ArrayList<>();
			List<String> actSeq = new ArrayList<>();
			boolean firstTrip = true;
			for( TripStructureUtils.Trip trip : TripStructureUtils.getTrips( person.getSelectedPlan() ) ){
				final String mainMode = TripStructureUtils.identifyMainMode( trip.getTripElements() );

				// per trip:
				double tripTTime = 0.;

				if( firstTrip ){
					firstTrip = false;
					actSeq.add( trip.getOriginActivity().getType().substring( 0, 4 ) );
				}
				modeSeq.add( shortenModeString( mainModeIdentifier.identifyMainMode( trip.getTripElements() ) ) );
				actSeq.add( trip.getDestinationActivity().getType().substring( 0, 4 ) );

				if( isBaseTable ){
					Double musl_h = getMUSE_h( trip.getDestinationActivity() );
//					if( musl_h != null && musl_h > 0 && musl_h < 16.30 ){
//					if( musl_h != null && musl_h > 0 ){
					if( musl_h != null ) {
						sumMuse_h += musl_h;
						cntMuse_h++;
					}
				}

				boolean haveAddedFirstPtAscOfTrip = false;
				for( Leg leg : trip.getLegsOnly() ){
					final ScoringParameterSet subpopScoringParams = config.scoring().getScoringParameters( PopulationUtils.getSubpopulation( person ) );
					final ModeParams modeParams = subpopScoringParams.getModes().get( leg.getMode() );

					// ttime:
					sumTtimes += leg.getTravelTime().seconds();
					tripTTime += leg.getTravelTime().seconds();
					directTravelScore += leg.getTravelTime().seconds() / 3600. * modeParams.getMarginalUtilityOfTraveling();

					// money:
					sumMoney += leg.getRoute().getDistance() * modeParams.getMonetaryDistanceRate();

					dailyMoneyByMode.put( leg.getMode(), modeParams.getDailyMonetaryConstant() );
					// we only want this once!

					// ascs:
					sumAscs += modeParams.getConstant();
					if( pt.equals( leg.getMode() ) ){
						if( haveAddedFirstPtAscOfTrip ){
							//deduct this again:
							sumAscs -= modeParams.getConstant();
							// instead, add the (dis)utility of line switch:
							lineSwitchesScore += subpopScoringParams.getUtilityOfLineSwitch();
						} else{
							haveAddedFirstPtAscOfTrip = true;
						}
					}
				}
				if ( isTestPerson( person.getId()  ) ){
					log.error( "personId={}; tripTtimeMs={}", person.getId(), tripTTime );
				}
			}
			// here we are done with the trip loop and now need to memorize the person values:

			table.doubleColumn( TTIME ).append( sumTtimes / 3600. );
			// (dies erzeugt keinen weiteren score!)

			if ( isTestPerson( person.getId() ) ) {
				log.error("personId={}; sumTTimes from ms calculation={}", person.getId(), sumTtimes );
			}

			// yyyy put the results of the utl computation first into person attributes, and convert to table separately

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
			if( isBaseTable ){
				double muse_h = sumMuse_h / cntMuse_h;
				// note that cntMuse_h can be 0 (because of "weird" activities) and then muse_h becomes NaN.
				table.doubleColumn( MUSE_h ).append( muse_h );
				// (means we are sometimes appending NaN.)

				if ( cntMuse_h > 0 ){
//					popSumMuse_h += sumMuse_h / cntMuse_h;
//					popCntMuse_h++; // person-based weight; could also justify trip-based weight as more policy-relevant. kai, dec'25
					popSumMuse_h += sumMuse_h;
					popCntMuse_h += cntMuse_h;
				}
			}

			// money:
			double moneyScore;
			{
				double dailyMoney = 0.;
				for( Double value : dailyMoneyByMode.values() ){
					dailyMoney += value;
				}
				table.doubleColumn( MONEY ).append( sumMoney + dailyMoney );
				Person basePerson = person;
				if( basePopulation != null ){
					basePerson = basePopulation.getPersons().get( person.getId() );
				}
				Double marginalUtilityOfMoney = 1.;
				if( basePerson != null ){
					marginalUtilityOfMoney = getMarginalUtilityOfMoney( basePerson );
				}
				// yyyy We have persons in the policy case which are no longer in the base case.
				moneyScore = (sumMoney + dailyMoney) * marginalUtilityOfMoney;

				table.doubleColumn( HeadersKN.MONEY_SCORE ).append( moneyScore );
				computedPersonScore += moneyScore;

			}

			if ( scoreWrnCnt < 10 ){
				if( !Gbl.equal( person.getSelectedPlan().getScore(), computedPersonScore, 1 ) ){
					log.warn( "personId={}; scoreFromMS={}; computedPersonScore={}; possible reason: score averaging in ms", person.getId(),
						person.getSelectedPlan().getScore(), computedPersonScore );
					scoreWrnCnt++;
					if ( scoreWrnCnt==10 ) {
						log.warn( Gbl.FUTURE_SUPPRESSED );
					}
				}
			}

			table.doubleColumn( SCORE ).append( computedPersonScore );
			table.stringColumn( HeadersKN.ACT_SEQ ).append( String.join( "|", actSeq ) );
			table.stringColumn( HeadersKN.MODE_SEQ ).append( String.join( "--", modeSeq ) );

			formatTable( table, 2 );

			// ... end person loop:
		}
		if( isBaseTable ){
			MUTTS_AV = popSumMuse_h / popCntMuse_h;
			log.warn( "MUTTS_AV={}; popSumMuse_h={}; popCntMuse_h={}", MUTTS_AV, popSumMuse_h, popCntMuse_h );
		}
		printTable( table, "print person table:" );
		return table;
	}


	void compare( Injector baseCaseInjector, Injector policyCaseInjector, Table personsTablePolicy, Table tripsTableBase, Table personsTableBase, Path outputPath ){
		Config baseConfig = baseCaseInjector.getInstance( Config.class );

		Scenario policyScenario = policyCaseInjector.getInstance( Scenario.class );

		if( doRoh ){
			new AgentWiseRuleOfHalfComputation( baseCaseInjector, this.policyInjector ).somehowComputeRuleOfHalf();
			addRohValuesToTable( policyScenario.getPopulation(), personsTablePolicy );
		}
		Table joinedTable = personsTableBase.joinOn( PERSON_ID ).inner( true, personsTablePolicy );

		log.info( "" );
		log.info( "print joined table:" );
		System.out.println( joinedTable );

		joinedTable.addColumns( deltaColumn( joinedTable, TTIME ), deltaColumn( joinedTable, MONEY ) );

//		joinedTable = joinedTable.where( joinedTable.stringColumn( ANALYSIS_POPULATION ).isEqualTo( "true" ).or( joinedTable.stringColumn( keyTwoOf( ANALYSIS_POPULATION ) ).isEqualTo( "true" ) ) );
//			joinedTable = joinedTable.where( joinedTable.stringColumn( MODE_SEQ ).containsString( "pt" ).or( joinedTable.stringColumn( keyTwoOf( MODE_SEQ ) ).containsString( "pt" ) ) );
		// (!!!! for the RoH, the reverse switchers need to be symmetrically included !!!!)
			joinedTable = joinedTable.where( joinedTable.stringColumn( MODE_SEQ ).containsString( "drt" ).or( joinedTable.stringColumn( keyTwoOf( MODE_SEQ ) ).containsString( "drt" ) ) );
//		joinedTable = joinedTable.dropWhere( joinedTable.stringColumn( keyTwoOf( MODE_SEQ ) ).containsString( "drt" ) ) ;

		Table copyOfJoinedTable = Table.create( joinedTable.columns() );

		{
			System.out.println("");
			System.out.println("## REMAINERS: ===");
			System.out.println("");

			// only keep the remainers:
			joinedTable = joinedTable.where( joinedTable.stringColumn( MODE_SEQ ).isEqualTo( joinedTable.stringColumn( keyTwoOf( MODE_SEQ ) ) ) );

			Table deltaTable = createDeltaTable( joinedTable );

			formatTable( deltaTable, 1 );

			log.info( "" );
			log.info( "print sorted table:" );
//			System.out.println( deltaTable.sortOn( deltaOf( SCORE ) ).sortOn( INCOME_DECILE ).print( 20 ) );
			System.out.println( deltaTable.sortOn( deltaOf( SCORE ) ).print( 20 ) );

			// ===

			Table rohDeltaTable = null;
			if( doRoh ){
				rohDeltaTable = createRohDeltaTable( joinedTable );

				formatTable( rohDeltaTable, 1 );

				log.info( "" );
				log.info( "print sorted roh table:" );
//				System.out.println( rohDeltaTable.sortOn( deltaOf( SCORE ) ).sortOn( INCOME_DECILE ).print( 20 ) );
				System.out.println( rohDeltaTable.sortOn( deltaOf( SCORE ) ).print( 20 ) );

				log.info("");
				log.info("print debugging table:");
				rohDeltaTable.addColumns(  rohDeltaTable.doubleColumn( deltaOf(ACTS_SCORE) ).subtract( rohDeltaTable.doubleColumn(
					U_TTIME_DIFF_REM_HET_PT ) ).setName( "ms - roh" ) );
				formatTable( rohDeltaTable, 2 );
				System.out.println( rohDeltaTable.sortOn( "ms - roh" ).print(20) );
				log.info("");
			}

			// ===

			writeMatsimScoresSummaryTables( "all:", outputPath, baseConfig, deltaTable );
		writeMatsimScoresSummaryTables( "0th decile:", outputPath, baseConfig, deltaTable.where( deltaTable.intColumn( HeadersKN.INCOME_DECILE ).isEqualTo( 0 ) ) );
		writeMatsimScoresSummaryTables( "last decile:", outputPath, baseConfig, deltaTable.where( deltaTable.intColumn( HeadersKN.INCOME_DECILE ).isEqualTo( 9 ) ) );

			if( doRoh ){
				writeRuleOfHalfSummaryTable( policyCasePath, baseConfig, rohDeltaTable );
			}
		}
		if ( !doSwitchers ) {
			log.error("stopping here since switchers are switched off");
			System.exit(-1);
		}
		{
			System.out.println();
			System.out.println("## SWITCHERS: ===");
			System.out.println();

			// only keep the switchers:
			joinedTable = copyOfJoinedTable.where( copyOfJoinedTable.stringColumn( MODE_SEQ ).isNotEqualTo( copyOfJoinedTable.stringColumn( keyTwoOf( MODE_SEQ ) ) ) );

			Table deltaTable = createDeltaTable( joinedTable );

			formatTable( deltaTable, 1 );

			log.info( "" );
			log.info( "print sorted table:" );
//			System.out.println( deltaTable.sortOn( deltaOf( SCORE ) ).sortOn( INCOME_DECILE ).print( 20 ) );
			System.out.println( deltaTable.sortOn( deltaOf( SCORE ) ).print( 20 ) );

			// ===

			Table rohDeltaTable = null;
			if( doRoh ){
				rohDeltaTable = createRohDeltaTable( joinedTable );

				formatTable( rohDeltaTable, 1 );

				log.info( "" );
				log.info( "print sorted roh table:" );
//				System.out.println( rohDeltaTable.sortOn( deltaOf( SCORE ) ).sortOn( INCOME_DECILE ).print( 20 ) );
				System.out.println( rohDeltaTable.sortOn( deltaOf( SCORE ) ).print( 20 ) );
			}

			// ===

			writeMatsimScoresSummaryTables( "all:", outputPath, baseConfig, deltaTable );
		writeMatsimScoresSummaryTables( "0th decile:", outputPath, baseConfig, deltaTable.where( deltaTable.intColumn( HeadersKN.INCOME_DECILE ).isEqualTo( 0 ) ) );
		writeMatsimScoresSummaryTables( "last decile:", outputPath, baseConfig, deltaTable.where( deltaTable.intColumn( HeadersKN.INCOME_DECILE ).isEqualTo( 9 ) ) );

			if( doRoh ){
				writeRuleOfHalfSummaryTable( policyCasePath, baseConfig, rohDeltaTable );
			}
		}
		writeAscTable( policyScenario.getConfig() );

	}


}
