package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
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
import picocli.CommandLine;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.nio.file.Path;
import java.util.*;

import static org.matsim.api.core.v01.TransportMode.*;
import static org.matsim.application.ApplicationUtils.globFile;
import static org.matsim.application.analysis.population.AddVttsEtcToActivities.getMUSE_h;
import static org.matsim.application.analysis.population.AgentWiseComparisonKNUtils.*;
import static org.matsim.application.analysis.population.HeadersKN.*;
import static org.matsim.core.config.groups.ScoringConfigGroup.*;
import static org.matsim.core.population.PersonUtils.getMarginalUtilityOfMoney;
import static org.matsim.core.router.TripStructureUtils.StageActivityHandling.ExcludeStageActivities;

/**
 * @author nagel, gregorr
 * This is my proposal for the VTTS handler.

 * This class calculates VTTS (Value of Travel Time Savings) based on
 * individual travel disutility from MATSim runs.
 *
 * It reads a base-case MATSim output, computes person-level travel disutility,
 * activity delay disutility, and VTTS values in EUR/h.
 * Author: nagel, gregorr
 */



public class VttsCalculationBasedOnKn implements MATSimAppCommand {


	private static final Logger log = LogManager.getLogger(VttsCalculationBasedOnKn.class);
	public static final String KN_MONEY = "knMoney";

	private static int scoreWrnCnt = 0;

//	@CommandLine.Parameters(description = "Path to run output directory for which analysis should be performed.")
//	private Path inputPath;

	@CommandLine.Option(names = "--base-path", description = "Path to run directory of base case.", required = true)
	private Path baseCasePath;


	// (--> I think that it allowed, either by design or by accident, EITHER multiple prefixes in the same dir,
	// OR one different prefix per different directory.  I think that I removed the second functionality some time ago.)

	ScoringFunctionFactory scoringFunctionFactory;
	MutableScenario baseScenario;

	com.google.inject.Injector injector;
	com.google.inject.Injector injector2;
	TripRouter tripRouter1;
	TripRouter tripRouter2;


	//private static final String baseDir = "/Users/gregorr/Documents/work/respos/runs-svn/IATBR/baseCaseContinued";

	public static void main(String[] args) {
		new VttsCalculationBasedOnKn().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Config baseConfig = ConfigUtils.loadConfig(globFile(baseCasePath, "*output_config_reduced.xml").toString());
		// (The reduced config has fewer problems with newly introduced config params.)

		baseConfig.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		baseConfig.scoring().addActivityParams(new ActivityParams(TripStructureUtils.createStageActivityType(car)).setScoringThisActivityAtAll(false));
		baseConfig.scoring().addActivityParams(new ActivityParams(TripStructureUtils.createStageActivityType(bike)).setScoringThisActivityAtAll(false));
		baseConfig.scoring().addActivityParams(new ActivityParams(TripStructureUtils.createStageActivityType(walk)).setScoringThisActivityAtAll(false));
		baseConfig.scoring().addActivityParams(new ActivityParams(TripStructureUtils.createStageActivityType(pt)).setScoringThisActivityAtAll(false));
		// yy whey do we need the above?

		baseConfig.counts().setInputFile(null);
		baseConfig.controller().setOutputDirectory("/Users/gregorr/Documents/work/respos/runs-svn/IATBR/baseCaseContinued/");


//		baseConfig.routing().setNetworkModes( Collections.singletonList( TransportMode.car ) );  // the rail raptor tries to go to the links which are connected to facilities

		String baseFacilitiesFilename = globFile(baseCasePath, "*output_" + Controler.DefaultFiles.facilities.getFilename() + ".gz").toString();
		baseConfig.facilities().setInputFile(baseFacilitiesFilename);

		String baseTransitScheduleFilename = null;
		if (baseConfig.transit().isUseTransit()) {
			baseTransitScheduleFilename = globFile(baseCasePath, "*output_" + Controler.DefaultFiles.transitSchedule.getFilename() + ".gz").toString();
		}
		baseConfig.transit().setTransitScheduleFile(baseTransitScheduleFilename);

		String baseNetworkFilename = globFile(baseCasePath, "*output_" + Controler.DefaultFiles.network.getFilename() + ".gz").toString();
		baseConfig.network().setInputFile(baseNetworkFilename);

		baseConfig.plans().setInputFile(null);
		baseConfig.network().setChangeEventsInputFile(null);
		baseConfig.transit().setVehiclesFile(null);
		baseConfig.vehicles().setVehiclesFile(null);

		baseScenario = (MutableScenario) ScenarioUtils.loadScenario(baseConfig);

		final Population basePopulation = readAndCleanPopulation(baseCasePath);

		// Compute marginal utilities of money for each person
		computeAndSetMarginalUtilitiesOfMoney(basePopulation);

		baseScenario.setPopulation(basePopulation);

		// -------------------------
		// Create injectors for scoring/trip routing
		// -------------------------

		{
			this.injector = new Injector.InjectorBuilder(baseScenario).addStandardModules().addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class);
				}
			}).build();
			this.scoringFunctionFactory = injector.getInstance(ScoringFunctionFactory.class);
			this.tripRouter1 = injector.getInstance(TripRouter.class);

		}
		{
			//String policyTransitScheduleFilename = globFile( inputPath, "*output_" + Controler.DefaultFiles.transitSchedule.getFilename() + ".gz" ).toString();
			//String policyNetworkFilename = globFile( inputPath, "*output_" + Controler.DefaultFiles.network.getFilename() + ".gz" ).toString();
			MutableScenario scenario = ScenarioUtils.createMutableScenario(baseConfig);
			//new MatsimNetworkReader( scenario.getNetwork() ).readFile( policyNetworkFilename );
			scenario.setActivityFacilities(baseScenario.getActivityFacilities());
			scenario.setPopulation(baseScenario.getPopulation());
			//new TransitScheduleReader( scenario ).readFile( policyTransitScheduleFilename );
			this.injector2 = new Injector.InjectorBuilder(scenario).addStandardModules().addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class);
				}
			}).build();
		}
		this.tripRouter2 = injector2.getInstance(TripRouter.class);


		Table baseTablePersons = generatePersonTableFromPopulation(basePopulation, baseConfig, null);
		Table baseTableTrips = generateTripVTTSWithActivityDelay(basePopulation, baseConfig);

		//write the tables:
		baseTableTrips.write().csv("/Users/gregorr/Documents/work/respos/runs-svn/IATBR/baseCaseContinued/vtts/agentWiseComparisonKN-trips.csv");
		baseTablePersons.write().csv("/Users/gregorr/Documents/work/respos/runs-svn/IATBR/baseCaseContinued/vtts/agentWiseComparisonKN-persons.csv");


		return 0;
	}

	@NotNull Table generatePersonTableFromPopulation(Population population, Config config, Population basePopulation) {

		Table table = Table.create(StringColumn.create(PERSON_ID), DoubleColumn.create(MATSIM_SCORE), DoubleColumn.create(SCORE), DoubleColumn.create(MONEY), DoubleColumn.create(MONEY_SCORE), DoubleColumn.create(TTIME), DoubleColumn.create(ASCS), DoubleColumn.create(U_TRAV_DIRECT), DoubleColumn.create(U_LINESWITCHES), DoubleColumn.create(ACTS_SCORE), StringColumn.create(MODE_SEQ), StringColumn.create(ACT_SEQ), StringColumn.create(ANALYSIS_POPULATION));

		if (basePopulation == null) {
			table.addColumns(DoubleColumn.create(UTL_OF_MONEY), DoubleColumn.create(MUSE_h));
		}

		MainModeIdentifier mainModeIdentifier = new DefaultAnalysisMainModeIdentifier();
		PopulationFactory pf = population.getFactory();
		double popSumMuse_h = 0.;
		double popCntMuse_h = 0.;
		Counter counter = new Counter("in method generatePersonTableFromPopulation(...); processing person #  ");
		for (Person person : population.getPersons().values()) {
			counter.incCounter();

			// yyyyyy much/all of the following needs to be differentiated by subpopulation !!! yyyyyy

			table.stringColumn(PERSON_ID).append(person.getId().toString());

			final Double scoreFromMatsim = person.getSelectedPlan().getScore();
			table.doubleColumn(MATSIM_SCORE).append(scoreFromMatsim);

			table.stringColumn(ANALYSIS_POPULATION).append(getAnalysisPopulation(person));
			processMUoM(basePopulation == null, person, table);

			double computedPersonScore = 0.;
			{
				// activity times:
				ScoringFunction sf = this.scoringFunctionFactory.createNewScoringFunction(person);
				ScoringFunction sfNormal = this.scoringFunctionFactory.createNewScoringFunction(person);
				ScoringFunction sfEarly = this.scoringFunctionFactory.createNewScoringFunction(person);
				Activity firstActivity = null;
				double sumMuse_h = 0.;
				double cntMuse_h = 0.;
				for (Activity act : TripStructureUtils.getActivities(person.getSelectedPlan(), ExcludeStageActivities)) {
					sf.handleActivity(act);
					if (basePopulation == null) {
						if (act.getStartTime().isDefined() && act.getEndTime().isDefined()) {
							// Ihab-style MarginalSumScoringFct computation but w/o leg:
							double scoreNormalBefore = sfNormal.getScore();
							double scoreEarlyBefore = sfEarly.getScore();
							sumMuse_h += computeMUSE_h(act, sfNormal, pf, sfEarly, scoreNormalBefore, scoreEarlyBefore);
							cntMuse_h++;
						} else if (act.getStartTime().isUndefined()) {
							firstActivity = act;
						} else {
							Gbl.assertIf(act.getEndTime().isUndefined());
							// Ihab-style MarginalSumScoringFct computation but w/o leg:
							double scoreNormalBefore = sfNormal.getScore();
							double scoreEarlyBefore = sfEarly.getScore();
							// treat the after-midnight-activity:
							sfNormal.handleActivity(firstActivity);
							sfEarly.handleActivity(firstActivity);
							// handle the before-midnight-activity
							sumMuse_h += computeMUSE_h(act, sfNormal, pf, sfEarly, scoreNormalBefore, scoreEarlyBefore);
							cntMuse_h++;
						}
					}
				}

				sf.finish();

				table.doubleColumn(HeadersKN.ACTS_SCORE).append(sf.getScore());
				computedPersonScore += sf.getScore();

				if (basePopulation == null) {
					AddVttsEtcToActivities.setMUSE_h(person.getSelectedPlan(), sumMuse_h / cntMuse_h);
				}
			}

			double sumMoney = 0.;
			Double moneyFromEvents = (Double) person.getAttributes().getAttribute(KN_MONEY);
			if (moneyFromEvents != null) {
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
			for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan())) {
				// per trip:

				if (firstTrip) {
					firstTrip = false;
					actSeq.add(trip.getOriginActivity().getType().substring(0, 4));
				}
				modeSeq.add((mainModeIdentifier.identifyMainMode(trip.getTripElements())));
				actSeq.add(trip.getDestinationActivity().getType().substring(0, 4));

				if (basePopulation == null) {
					Double musl_h = getMUSE_h(trip.getDestinationActivity());
					if (musl_h != null && musl_h > 0 && musl_h < 16.30) {
						sumMuse_h += musl_h;
						cntMuse_h++;
					}
				}

				boolean haveAddedFirstPtAscOfTrip = false;
				for (Leg leg : trip.getLegsOnly()) {
					final ScoringParameterSet subpopScoringParams = config.scoring().getScoringParameters(PopulationUtils.getSubpopulation(person));
					final ModeParams modeParams = subpopScoringParams.getModes().get(leg.getMode());

					// ttime:
					sumTtimes += leg.getTravelTime().seconds();
					directTravelScore += leg.getTravelTime().seconds() / 3600. * modeParams.getMarginalUtilityOfTraveling();

					// money:
					sumMoney += leg.getRoute().getDistance() * modeParams.getMonetaryDistanceRate();

					dailyMoneyByMode.put(leg.getMode(), modeParams.getDailyMonetaryConstant());
					// we only want this once!

					// ascs:
					sumAscs += modeParams.getConstant();
					if (TransportMode.pt.equals(leg.getMode())) {
						if (haveAddedFirstPtAscOfTrip) {
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

			table.doubleColumn(TTIME).append(sumTtimes / 3600.);
			// (dies erzeugt keinen weiteren score!)

			{
				table.doubleColumn(ASCS).append(sumAscs);
				computedPersonScore += sumAscs;
			}
			{
				table.doubleColumn(U_TRAV_DIRECT).append(directTravelScore);
				computedPersonScore += directTravelScore;
				table.doubleColumn(U_LINESWITCHES).append(lineSwitchesScore);
				computedPersonScore += lineSwitchesScore;
			}
			if (basePopulation == null) {
				double muse_h = sumMuse_h / cntMuse_h;
				// note that cntMuse_h can be 0 (because if "weird" activities) and then muse_h becomes NaN.
				table.doubleColumn(MUSE_h).append(muse_h);
				// (means we are sometimes appending NaN.)

				if (cntMuse_h > 0) {
					popSumMuse_h += sumMuse_h / cntMuse_h;
					popCntMuse_h++; // person-based weight; could also justify trip-based weight as more policy-relevant. kai, dec'25
				}
			}

			// money:
			double moneyScore;
			{
				double dailyMoney = 0.;
				for (Double value : dailyMoneyByMode.values()) {
					dailyMoney += value;
				}
				table.doubleColumn(MONEY).append(sumMoney + dailyMoney);
				Person basePerson = person;
				if (basePopulation != null) {
					basePerson = basePopulation.getPersons().get(person.getId());
				}
				Double marginalUtilityOfMoney = 1.;
				if (basePerson != null) {
					marginalUtilityOfMoney = getMarginalUtilityOfMoney(basePerson);
				}
				// yyyy We have persons in the policy case which are no longer in the base case.
				moneyScore = (sumMoney + dailyMoney) * marginalUtilityOfMoney;

				table.doubleColumn(HeadersKN.MONEY_SCORE).append(moneyScore);
				computedPersonScore += moneyScore;

			}

			if (scoreWrnCnt < 10) {
				if (!Gbl.equal(scoreFromMatsim, computedPersonScore, 1)) {
					log.warn("personId={}; scoreFromMS={}; computedPersonScore={}; possible reason: score averaging in ms", person.getId(), scoreFromMatsim, computedPersonScore);
					scoreWrnCnt++;
					if (scoreWrnCnt == 10) {
						log.warn(Gbl.FUTURE_SUPPRESSED);
					}
				}
			}

			table.doubleColumn(SCORE).append(computedPersonScore);
			table.stringColumn(HeadersKN.ACT_SEQ).append(String.join("|", actSeq));
			table.stringColumn(HeadersKN.MODE_SEQ).append(String.join("--", modeSeq));

			formatTable(table, 2);

			// ... end person loop:
		}
		if (basePopulation == null) {
			MUTTS_AV = popSumMuse_h / popCntMuse_h;
			log.warn("MUTTS_AV={}; popSumMuse_h={}; popCntMuse_h={}", MUTTS_AV, popSumMuse_h, popCntMuse_h);
		}

		// Add a new column for travel disutility per person
		DoubleColumn travelDisutility_h = DoubleColumn.create("travelDisutility_h");

		// Loop over the table rows
		for (int i = 0; i < table.rowCount(); i++) {
			double travelDisutility = table.doubleColumn(U_TRAV_DIRECT).get(i);
			travelDisutility_h.append(travelDisutility);
		}

// Add the new column to the table
		table.addColumns(travelDisutility_h);

		return table;
	}


	@NotNull
	Table generateTripVTTSWithActivityDelay(
		Population population,
		Config config) {

		Table table = Table.create(
			StringColumn.create(PERSON_ID),
			IntColumn.create("tripIdx"),
			StringColumn.create("mainMode"),
			DoubleColumn.create("travelDisutility_h"),
			DoubleColumn.create("activityDelayDisutility_h"),
			DoubleColumn.create("mUTTS_[u/h]"),
			DoubleColumn.create(UTL_OF_MONEY),
			DoubleColumn.create("VTTS_[EUR/h]")
		);

		MainModeIdentifier mainModeIdentifier =
			new DefaultAnalysisMainModeIdentifier();

		PopulationFactory pf = population.getFactory();

		for (Person person : population.getPersons().values()) {

			final String personId = person.getId().toString();
			final double uom = getMarginalUtilityOfMoney(person);

			ScoringFunction sfNormal =
				scoringFunctionFactory.createNewScoringFunction(person);
			ScoringFunction sfEarly =
				scoringFunctionFactory.createNewScoringFunction(person);

			int tripIdx = 0;

			List<TripStructureUtils.Trip> trips =
				TripStructureUtils.getTrips(person.getSelectedPlan());

			for (TripStructureUtils.Trip trip : trips) {

				double travelDisutility = 0.0;

				String mainMode = mainModeIdentifier.identifyMainMode(trip.getTripElements());

				// --- travel disutility ---
				for (Leg leg : trip.getLegsOnly()) {

					ScoringParameterSet params =
						config.scoring().getScoringParameters(
							PopulationUtils.getSubpopulation(person));

					ModeParams modeParams =
						params.getModes().get(leg.getMode());

					double tt_h = leg.getTravelTime().seconds() / 3600.0;
					travelDisutility +=
						tt_h * modeParams.getMarginalUtilityOfTraveling();
				}

				// --- activity delay disutility (destination activity) ---
				Activity destAct = trip.getDestinationActivity();

				double activityDelayDisutility = 0.0;

				if (destAct.getStartTime().isDefined()
					&& destAct.getEndTime().isDefined()) {

					double scoreNormalBefore = sfNormal.getScore();
					double scoreEarlyBefore = sfEarly.getScore();

					activityDelayDisutility =
						computeMUSE_h(
							destAct,
							sfNormal,
							pf,
							sfEarly,
							scoreNormalBefore,
							scoreEarlyBefore);
				}

				double mUTTS_h =
					(travelDisutility + activityDelayDisutility);

				double vtts_eur_h =
					uom > 0 ? mUTTS_h / uom : Double.NaN;

				// ---- append row ----
				table.stringColumn(PERSON_ID).append(personId);
				table.intColumn("tripIdx").append(tripIdx);
				table.stringColumn("mainMode").append(mainMode);
				table.doubleColumn("travelDisutility_h").append(travelDisutility);
				table.doubleColumn("activityDelayDisutility_h").append(activityDelayDisutility);
				table.doubleColumn("mUTTS_[u/h]").append(mUTTS_h);
				table.doubleColumn(UTL_OF_MONEY).append(uom);
				table.doubleColumn("VTTS_[EUR/h]").append(vtts_eur_h);

				tripIdx++;
			}
		}

		formatTable(table, 2);
		return table;
	}

}
