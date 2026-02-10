package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.DefaultAnalysisMainModeIdentifier;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import picocli.CommandLine;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Table;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.matsim.api.core.v01.TransportMode.*;
import static org.matsim.application.ApplicationUtils.globFile;
import static org.matsim.application.analysis.population.AddVttsEtcToActivities.getMUSE_h;
import static org.matsim.application.analysis.population.AgentWiseComparisonKNUtils.*;
import static org.matsim.application.analysis.population.HeadersKN.*;
import static org.matsim.core.config.groups.ScoringConfigGroup.*;
import static org.matsim.core.population.PersonUtils.getMarginalUtilityOfMoney;
import static org.matsim.core.router.TripStructureUtils.StageActivityHandling.ExcludeStageActivities;

@CommandLine.Command(name = "monetary-utility", description = "List and compare fare, dailyRefund and utility values for agents in base and policy case.")
public class AgentWiseComparisonKN implements MATSimAppCommand{
	// I need a simpler way to organize this workflow.  Use case ZEZ (where I need to start from the basic matsim files).

	// The main problem for me is that I repeatedly need to put the file paths into the IntelliJ run configurations.  I would like to do this at most once.

	// My current intuition would be to build an umbrella class that implements the MATSimAppCommand and call the respective methods by one-liners.

	// Alternatively (or even at the same time) I could put args into main methods and call from there.

	private static final Logger log = LogManager.getLogger( AgentWiseComparisonKN.class );
	public static final String KN_MONEY = "knMoney";

	private static int scoreWrnCnt = 0;

	private static final boolean doRoh = false;

	@CommandLine.Parameters(description = "Path to run output directory for which analysis should be performed.")
	private Path inputPath;

	@CommandLine.Option(names = "--base-path", description = "Path to run directory of base case.", required = true)
	private Path baseCasePath;

	@CommandLine.Option(names = "--prefix", description = "Prefix for filtered events output file, optional. This can be a list of multiple prefixes.", split = ",")
	private List<String> prefixList = new ArrayList<>();
	// (yy not clear to me how this works.  Different prefixes for different input paths? kai, nov'25)

	// (--> I think that it allowed, either by design or by accident, EITHER multiple prefixes in the same dir,
	// OR one different prefix per different directory.  I think that I removed the second functionality some time ago.)

	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();

	ScoringFunctionFactory scoringFunctionFactory;
	MutableScenario baseScenario;
	private List<PreparedGeometry> geometries;

	com.google.inject.Injector injector;
	com.google.inject.Injector injector2;
//	TripRouter tripRouter1;
//	TripRouter tripRouter2;

	private static final String onlyMoneyAndStuck = "onlyMoneyAndStuck.";

	public static void main( String[] args ){
		Gbl.assertIf( args==null || args.length==0 );

		// equil:
//	private static final String baseDir="/Users/kainagel/git/all-matsim/matsim-example-project/referenceOutput/";
//	private static final String policyDir="/Users/kainagel/git/all-matsim/matsim-example-project/referenceOutput/";

		// gartenfeld:
//		final String baseDir="/Users/kainagel/runs-svn/gartenfeld/caseStudies/v6.4-cutout/base-case-ctd/output-gartenfeld-v6.4-cutout-10pct-base-case-ctd/";
//		final String policyDir="/Users/kainagel/runs-svn/gartenfeld/caseStudies/v6.4-cutout/siemensbahn-case-study/output-gartenfeld-v6.4-cutout-10pct-siemensbahn/";
//		final String shpFile=null;

//		 zez:
//		final String baseDir="/Users/kainagel/shared-svn/projects/zez/b_wo_zez/";
//		final String policyDir="/Users/kainagel/shared-svn/projects/zez/c_w_zez/";
//		final String shpFile="../berlin.shp";
//		 yyyy consider a 10 or even 1pct sample of the population

		// meckel:
		final String baseDir="/Users/kainagel/runs-svn/Abschlussarbeiten/2025/Niklas.Meckel.U0/meckel/run8_NF_500it_10pct/output/";
		final String policyDir="/Users/kainagel/runs-svn/Abschlussarbeiten/2025/Niklas.Meckel.U0/meckel/run14_U0_500it_10pct/output/";
		final String shpFile = null;

		// Eduardo Lima:
//		final String baseDir="/Users/kainagel/runs-svn/Abschlussarbeiten/2025/Eduardo_Lima_Siemensbahn/output_final/output-Base_Case-10pct-nach-3900it-S21Jungfernheide/";
//		final String policyDir="/Users/kainagel/runs-svn/Abschlussarbeiten/2025/Eduardo_Lima_Siemensbahn/output_final/output-SiBa-10pct-nach-3900it-S21Jungfernheide/";
//		final String shpFile = null;

		// ===

//		generateExperiencedPlans( baseDir );
//		generateExperiencedPlans( policyDir );
//		generateFilteredEventsFile( baseDir );
//		generateFilteredEventsFile( policyDir );
		agentWiseComparison( baseDir, policyDir, shpFile );
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
		String inFileName = globFile( Path.of( baseDir ),  "*output_" + Controler.DefaultFiles.events.getFilename() + ".gz" ).toString();
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
		if ( shp!=null && shp.isDefined() ){
			URL url = Paths.get( shp.getShapeFile() ).toUri().toURL();
			this.geometries = ShpGeometryUtils.loadPreparedGeometries( url );

			// there is shp.readFeatures(), but I cannot get the Geometries out of the Features :-(.  kai, jan'26
			// --> yy but other people are able to do this ...
		}

		List<String> eventsFilePatterns = new ArrayList<>();

		if( !prefixList.isEmpty() ){
			for( String prefix : prefixList ){
				eventsFilePatterns.add( "*" + prefix + "output_events_filtered.xml.gz" );
			}
		} else{
			eventsFilePatterns.add( "*output_events.xml.gz" );
		}

		Config baseConfig = ConfigUtils.loadConfig( globFile( baseCasePath, "*output_config_reduced.xml" ).toString() );
		// (The reduced config has fewer problems with newly introduced config params.)

		baseConfig.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );

		baseConfig.scoring().addActivityParams( new ActivityParams( TripStructureUtils.createStageActivityType( car ) ).setScoringThisActivityAtAll( false ) );
		baseConfig.scoring().addActivityParams( new ActivityParams( TripStructureUtils.createStageActivityType( bike ) ).setScoringThisActivityAtAll( false ) );
		baseConfig.scoring().addActivityParams( new ActivityParams( TripStructureUtils.createStageActivityType( walk ) ).setScoringThisActivityAtAll( false ) );
		baseConfig.scoring().addActivityParams( new ActivityParams( TripStructureUtils.createStageActivityType( pt ) ).setScoringThisActivityAtAll( false ) );
		// yy whey do we need the above?

		baseConfig.counts().setInputFile( null );

//		baseConfig.routing().setNetworkModes( Collections.singletonList( TransportMode.car ) );  // the rail raptor tries to go to the links which are connected to facilities

		String baseFacilitiesFilename = globFile( baseCasePath, "*output_" + Controler.DefaultFiles.facilities.getFilename() + ".gz" ).toString();
		baseConfig.facilities().setInputFile( baseFacilitiesFilename );

		String baseTransitScheduleFilename = null;
		if ( baseConfig.transit().isUseTransit() ){
			baseTransitScheduleFilename = globFile( baseCasePath, "*output_" + Controler.DefaultFiles.transitSchedule.getFilename() + ".gz" ).toString();
		}
		baseConfig.transit().setTransitScheduleFile( baseTransitScheduleFilename );

		String baseNetworkFilename = globFile( baseCasePath, "*output_" + Controler.DefaultFiles.network.getFilename() + ".gz" ).toString();
		baseConfig.network().setInputFile( baseNetworkFilename );

		baseConfig.plans().setInputFile( null );
		baseConfig.network().setChangeEventsInputFile( null );
		baseConfig.transit().setVehiclesFile( null );
		baseConfig.vehicles().setVehiclesFile( null );

		baseScenario = (MutableScenario) ScenarioUtils.loadScenario( baseConfig );

		final Population basePopulation = readAndCleanPopulation( baseCasePath, eventsFilePatterns );
		computeAndSetMarginalUtilitiesOfMoney( basePopulation );
		computeAndSetIncomeDeciles( basePopulation );
		baseScenario.setPopulation( basePopulation );

		{
			this.injector = new Injector.InjectorBuilder( baseScenario )
													  .addStandardModules()
													  .addOverridingModule( new AbstractModule(){
														  @Override public void install(){
															  bind( ScoringParametersForPerson.class ).to( IncomeDependentUtilityOfMoneyPersonScoringParameters.class );
														  }
													  } )
													  .build();
			this.scoringFunctionFactory = injector.getInstance( ScoringFunctionFactory.class );
		}
		{
			// Here, we do not want to use ScenarioUtils.loadScenario, because we might be able to rescue files from the base scenario.  (Not totally clear.)
			String policyTransitScheduleFilename = globFile( inputPath, "*output_" + Controler.DefaultFiles.transitSchedule.getFilename() + ".gz" ).toString();
			String policyNetworkFilename = globFile( inputPath, "*output_" + Controler.DefaultFiles.network.getFilename() + ".gz" ).toString();
			MutableScenario scenario2 = ScenarioUtils.createMutableScenario( baseConfig );
			new MatsimNetworkReader( scenario2.getNetwork() ).readFile( policyNetworkFilename );
			scenario2.setActivityFacilities( baseScenario.getActivityFacilities() );
			scenario2.setPopulation( baseScenario.getPopulation() );
			new TransitScheduleReader( scenario2 ).readFile( policyTransitScheduleFilename );
			this.injector2 = new Injector.InjectorBuilder( scenario2 )
													   .addStandardModules()
													   .addOverridingModule( new AbstractModule(){
														   @Override public void install(){ bind( ScoringParametersForPerson.class ).to( IncomeDependentUtilityOfMoneyPersonScoringParameters.class ); }
													   } )
													   .build();
		}

		// ===

		// yyyyyy !!!!!! We now have a different avgIncome in the scoring fct than in the general population.  !!!!!!! yyyyyy
		// --> (presumably) compute the mUoM here instead of in the preproc.

		tagPersonsToAnalyse( basePopulation, geometries, baseScenario );

		// yyyy Ich frage mich im Moment, ob das mit dem "taggen" wirklich der richtige Weg ist, oder ob es nicht konsequenter ist, die anderen Personen zu entfernen.
		// --> Vorteil von taggen: man kann mehrfach taggen und auswerten.  Z.B. "all", "lowest decile", "highest decile" in one go.

		// --> und z.B. bei incomeDecile macht es einen Unterschied, ob ich das nur in Bln oder im gesamten Szenario bestimme.




//		Table baseTableTrips = generateTripsTableFromPopulation( basePopulation, config, true );
		Table baseTableTrips = null;
		Table baseTablePersons = generatePersonTableFromPopulation( basePopulation, baseConfig, null );

		// ### next cometh the policy data:

		String policyConfigFilename = globFile( inputPath, "*output_config_reduced.xml" ).toString();
		// (The reduced config has fewer problems with newly introduced config params.)

		Config policyConfig = ConfigUtils.loadConfig( policyConfigFilename );

		MutableScenario policyScenario = ScenarioUtils.createMutableScenario( policyConfig );
		policyScenario.setNetwork( this.baseScenario.getNetwork() );
		policyScenario.setTransitSchedule( this.baseScenario.getTransitSchedule() );

		Population policyPopulation = readAndCleanPopulation( inputPath, eventsFilePatterns );
		policyScenario.setPopulation( policyPopulation );

		Table personsTablePolicy = generatePersonTableFromPopulation( policyPopulation, policyConfig, basePopulation );

		compare( policyScenario, personsTablePolicy, baseTableTrips, baseTablePersons, this.baseScenario, baseConfig, inputPath );

		return 0;
	}

	@NotNull Table generatePersonTableFromPopulation( Population population, Config config, Population basePopulation ){
		final boolean isBaseTable = (basePopulation == null);

		Table table = createPopulationTable();

		if( isBaseTable ){
			table.addColumns( DoubleColumn.create( UTL_OF_MONEY ), DoubleColumn.create( MUSE_h ), IntColumn.create( INCOME_DECILE ) );
		}

		MainModeIdentifier mainModeIdentifier = new DefaultAnalysisMainModeIdentifier();
		PopulationFactory pf = population.getFactory();
		double popSumMuse_h = 0.;
		double popCntMuse_h = 0.;
		Counter counter = new Counter( "in method generatePersonTableFromPopulation(...); processing person #  ");
		for( Person person : population.getPersons().values() ){
			counter.incCounter();

			// yyyyyy much/all of the following needs to be differentiated by subpopulation !!! yyyyyy

			table.stringColumn( PERSON_ID ).append( person.getId().toString() );

			final Double scoreFromMatsim = person.getSelectedPlan().getScore();
			table.doubleColumn( MATSIM_SCORE ).append( scoreFromMatsim );

			table.stringColumn( ANALYSIS_POPULATION ).append( getIsInShp( person ) );

			processMUoM( isBaseTable, person, table );

			if ( isBaseTable ){
				table.intColumn( INCOME_DECILE ).append( getIncomeDecileBetween0And9( person ) );
			}

			double computedPersonScore = 0.;
			{
				// activity times:
				ScoringFunction sf = this.scoringFunctionFactory.createNewScoringFunction( person );
				ScoringFunction sfNormal = this.scoringFunctionFactory.createNewScoringFunction( person );
				ScoringFunction sfEarly = this.scoringFunctionFactory.createNewScoringFunction( person );
				Activity firstActivity = null;
				double sumMuse_h = 0.;
				double cntMuse_h = 0.;
				for( Activity act : TripStructureUtils.getActivities( person.getSelectedPlan(), ExcludeStageActivities ) ){
					sf.handleActivity( act );
					if( isBaseTable ){
						if( act.getStartTime().isDefined() && act.getEndTime().isDefined() ){
							// Ihab-style MarginalSumScoringFct computation but w/o leg:
							double scoreNormalBefore = sfNormal.getScore();
							double scoreEarlyBefore = sfEarly.getScore();
							sumMuse_h += computeMUSE_h( act, sfNormal, pf, sfEarly, scoreNormalBefore, scoreEarlyBefore );
							cntMuse_h++;
						} else if( act.getStartTime().isUndefined() ){
							firstActivity = act;
						} else{
							Gbl.assertIf( act.getEndTime().isUndefined() );
							// Ihab-style MarginalSumScoringFct computation but w/o leg:
							double scoreNormalBefore = sfNormal.getScore();
							double scoreEarlyBefore = sfEarly.getScore();
							// treat the after-midnight-activity:
							sfNormal.handleActivity( firstActivity );
							sfEarly.handleActivity( firstActivity );
							// handle the before-midnight-activity
							sumMuse_h += computeMUSE_h( act, sfNormal, pf, sfEarly, scoreNormalBefore, scoreEarlyBefore );
							cntMuse_h++;
						}
					}
				}

				sf.finish();

				table.doubleColumn( HeadersKN.ACTS_SCORE ).append( sf.getScore() );
				computedPersonScore += sf.getScore();

				if( isBaseTable ){
					AddVttsEtcToActivities.setMUSE_h( person.getSelectedPlan(), sumMuse_h / cntMuse_h );
				}
			}

			double sumMoney = 0.;
			Double moneyFromEvents = (Double) person.getAttributes().getAttribute( KN_MONEY );
			if( moneyFromEvents != null ){
				sumMoney += moneyFromEvents;
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
				// per trip:

				if( firstTrip ){
					firstTrip = false;
					actSeq.add( trip.getOriginActivity().getType().substring( 0, 4 ) );
				}
				modeSeq.add( shortenModeString( mainModeIdentifier.identifyMainMode( trip.getTripElements() ) ) );
				actSeq.add( trip.getDestinationActivity().getType().substring( 0, 4 ) );

				if( isBaseTable ){
					Double musl_h = getMUSE_h( trip.getDestinationActivity() );
					if( musl_h != null && musl_h > 0 && musl_h < 16.30 ){
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
					directTravelScore += leg.getTravelTime().seconds() / 3600. * modeParams.getMarginalUtilityOfTraveling();

					// money:
					sumMoney += leg.getRoute().getDistance() * modeParams.getMonetaryDistanceRate();

					dailyMoneyByMode.put( leg.getMode(), modeParams.getDailyMonetaryConstant() );
					// we only want this once!

					// ascs:
					sumAscs += modeParams.getConstant();
					if( TransportMode.pt.equals( leg.getMode() ) ){
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
			}
			// here we are done with the trip loop and now need to memorize the person values:

			table.doubleColumn( TTIME ).append( sumTtimes / 3600. );
			// (dies erzeugt keinen weiteren score!)

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
				// note that cntMuse_h can be 0 (because if "weird" activities) and then muse_h becomes NaN.
				table.doubleColumn( MUSE_h ).append( muse_h );
				// (means we are sometimes appending NaN.)

				if ( cntMuse_h > 0 ){
					popSumMuse_h += sumMuse_h / cntMuse_h;
					popCntMuse_h++; // person-based weight; could also justify trip-based weight as more policy-relevant. kai, dec'25
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
				if( !Gbl.equal( scoreFromMatsim, computedPersonScore, 1 ) ){
					log.warn( "personId={}; scoreFromMS={}; computedPersonScore={}; possible reason: score averaging in ms", person.getId(), scoreFromMatsim, computedPersonScore );
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


	private static int cnt = 0;

	private void somehowComputeRuleOfHalf( Population basePopulation, Population policyPopulation, Table personsTablePolicy ){
		var ttimeRemHom = DoubleColumn.create( W1_TTIME_DIFF_REM );
		var ttimeRemHet = DoubleColumn.create( W2_TTIME_DIFF_REM );
		var ixRem = DoubleColumn.create( IX_DIFF_REMAINING );
		var ttimeSwiHom = DoubleColumn.create( W1_TTIME_DIFF_SWI );
		var ttimeSwiHet = DoubleColumn.create( W2_TTIME_DIFF_SWI );
		var ixSwi = DoubleColumn.create( IX_DIFF_SWITCHING );

		TripRouter tripRouter1 = this.injector.getInstance( TripRouter.class );
		TripRouter tripRouter2 = this.injector2.getInstance( TripRouter.class );

		Counter counter = new Counter( "somehowComputeRuleOfHalf; person # " );
		for( Person policyPerson : policyPopulation.getPersons().values() ){
			counter.incCounter();

			double sumTravTimeDiffsRemainersHom = 0.;
			double sumTravTimeDiffsRemainersHet = 0.;
			double sumTravTimeDiffsSwitchersHom = 0.;
			double sumTravTimeDiffsSwitchersHet = 0.;

			double sumIchangesDiffsRemainers = 0;
			double sumIchangesDiffsSwitchers = 0.;

			Person basePerson = basePopulation.getPersons().get( policyPerson.getId() );

			if( basePerson != null ){
				Double musl = (Double) getMUSE_h( basePerson.getSelectedPlan() );
				if( musl == null ){
					log.warn( "muse is null; I do not know why; personId={}", basePerson.getId() );
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
							if ( cnt < 1 ){
								log.warn( "MUTTS_AV={}", MUTTS_AV );
								cnt++;
//								if ( cnt==1 ) {
//									log.warn( Gbl.FUTURE_SUPPRESSED );
//								}
							}
							sumTravTimeDiffsRemainersHom += (sumPolicyTtime - sumBaseTtime) * MUTTS_AV * (-1);
							sumTravTimeDiffsRemainersHet += (sumPolicyTtime - sumBaseTtime) * musl * (-1);
							sumIchangesDiffsRemainers += (sumPolicyLineSwitches - sumBaseLineSwitches);
						} else{
							// Neunutzer; rule-of-half
							// first need hypothetical base travel time
							final Result baseResult = routeTrip( baseTrip, policyMainMode, basePerson, tripRouter1 );
							// then compute hypothetical policy travel time
							final Result policyResult = routeTrip( policyTrip, policyMainMode, policyPerson, tripRouter2 );
							// sum up (rule-of-half is done later):
							sumTravTimeDiffsSwitchersHom += (policyResult.sumTtime() - baseResult.sumTtime()) * MUTTS_AV * (-1);
							sumTravTimeDiffsSwitchersHet += (policyResult.sumTtime() - baseResult.sumTtime()) * musl * (-1);
							sumIchangesDiffsSwitchers += (policyResult.sumLineSwitches() - baseResult.sumLineSwitches());
						}
					}
				}
			}
			ttimeRemHom.append( sumTravTimeDiffsRemainersHom / 3600. );
			ttimeRemHet.append( sumTravTimeDiffsRemainersHet / 3600. );
			ttimeSwiHom.append( sumTravTimeDiffsSwitchersHom / 3600. );
			ttimeSwiHet.append( sumTravTimeDiffsSwitchersHet / 3600. );
			ixRem.append( sumIchangesDiffsRemainers );
			ixSwi.append( sumIchangesDiffsSwitchers );
		}
		personsTablePolicy.addColumns( ttimeRemHom, ttimeRemHet, ixRem, ttimeSwiHom, ttimeSwiHet, ixSwi );
		log.info( "persons table policy after adding RoH entries:" );
		System.out.println( personsTablePolicy );
	}


	private @NotNull Result routeTrip( TripStructureUtils.Trip baseTrip, String policyMainMode, Person basePerson, TripRouter tripRouter ){
		Facility fromFacility = FacilitiesUtils.toFacility( baseTrip.getOriginActivity(), baseScenario.getActivityFacilities() );
		Facility toFacility = FacilitiesUtils.toFacility( baseTrip.getDestinationActivity(), baseScenario.getActivityFacilities() );
		final List<? extends PlanElement> planElements = tripRouter.calcRoute( policyMainMode, fromFacility, toFacility,
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

	void compare( Scenario policyScenario, Table personsTablePolicy, Table tripsTableBase, Table personsTableBase, Scenario baseScenario,
				  Config baseConfig, Path outputPath ) {

		if ( doRoh ){
			somehowComputeRuleOfHalf( baseScenario.getPopulation(), policyScenario.getPopulation(), personsTablePolicy );
		}
		Table joinedTable = personsTableBase.joinOn( PERSON_ID ).inner( true, personsTablePolicy );

		log.info(""); log.info( "print joined table:" );
		System.out.println( joinedTable );

//		printSpecificPerson( joinedTable, "960148" );

		joinedTable.addColumns( deltaColumn( joinedTable, TTIME ), deltaColumn( joinedTable, MONEY ) );

//		joinedTable = joinedTable.where( joinedTable.stringColumn( ANALYSIS_POPULATION ).isEqualTo( "true" ).or( joinedTable.stringColumn( keyTwoOf( ANALYSIS_POPULATION ) ).isEqualTo( "true" ) ) );
		joinedTable = joinedTable.where( joinedTable.stringColumn( MODE_SEQ ).isEqualTo( joinedTable.stringColumn( keyTwoOf( MODE_SEQ ) ) ) );
		joinedTable = joinedTable.where( joinedTable.stringColumn( keyTwoOf( MODE_SEQ ) ).containsString( "pt" ) );
//		joinedTable = joinedTable.where( joinedTable.stringColumn( MODE_SEQ ).containsString( "pt" ).or( joinedTable.stringColumn( keyTwoOf( MODE_SEQ ) ).containsString( "pt" ) ) );


		Table deltaTable = createDeltaTable( joinedTable );

		formatTable( deltaTable, 1 );

		log.info("");log.info("print sorted table:");
		System.out.println( deltaTable.sortOn( deltaOf( SCORE ) ) );

		// ===

		Table rohDeltaTable = null;
		if ( doRoh ){
			rohDeltaTable = createRohDeltaTable( joinedTable );

			formatTable( rohDeltaTable, 1 );

			System.out.println( rohDeltaTable.sortOn( deltaOf( SCORE ) ) );
		}

		// ===

		writeMatsimScoresSummaryTables( "all:", outputPath, baseConfig, deltaTable );
		writeMatsimScoresSummaryTables( "0th decile:", outputPath, baseConfig, deltaTable.where( deltaTable.intColumn( HeadersKN.INCOME_DECILE ).isEqualTo( 0 ) ) );
		writeMatsimScoresSummaryTables( "last decile:", outputPath, baseConfig, deltaTable.where( deltaTable.intColumn( HeadersKN.INCOME_DECILE ).isEqualTo( 9 ) ) );

		if ( doRoh ){
			writeRuleOfHalfSummaryTable( inputPath, baseConfig, rohDeltaTable );
		}

		writeAscTable( baseConfig );

	}


}
