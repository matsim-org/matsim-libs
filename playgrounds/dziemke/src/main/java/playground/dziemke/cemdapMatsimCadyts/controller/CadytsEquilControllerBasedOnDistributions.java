package playground.dziemke.cemdapMatsimCadyts.controller;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.cadyts.measurement.Measurement;
import org.matsim.contrib.cadyts.measurement.MeasurementCadytsContext;
import org.matsim.contrib.cadyts.measurement.Measurements;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters.Builder;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.counts.Counts;

public class CadytsEquilControllerBasedOnDistributions {
	private final static Logger log = Logger.getLogger(CadytsEquilControllerBasedOnDistributions.class);
//	private final static String CADYTS = "cadytsCar";

	public static void main(final String[] args) {
		final Config config = ConfigUtils.createConfig();

		// global
		config.global().setRandomSeed(4711);
		config.global().setCoordinateSystem("Atlantis");

		// network
		String inputNetworkFile = "/Users/dominik/SVN/shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/input/network_diff_lengths2.xml";
		config.network().setInputFile(inputNetworkFile);

		// plans
		String inputPlansFile = "/Users/dominik/SVN/shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/input/plans1000.xml";
		config.plans().setInputFile(inputPlansFile);

		//simulation
		config.qsim().setStartTime(0.);
		config.qsim().setEndTime(0.);
		config.qsim().setSnapshotPeriod(0.);

		// counts
		//		String countsFileName = "/Users/dominik/Workspace/data/examples/equil/input/counts100-200.xml";
		//		config.counts().setCountsFileName(countsFileName);
		//		//config.counts().setCountsScaleFactor(100);
		//		config.counts().setOutputFormat("all");

		// vsp experimental
		config.vspExperimental().setVspDefaultsCheckingLevel( VspDefaultsCheckingLevel.warn );

		// controller
		String runId = "80";
		String outputDirectory = "/Users/dominik/SVN/shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/output/" + runId + "/";
		config.controler().setRunId(runId);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(200);
		//		config.controler().setLastIteration(10);
		Set<String> snapshotFormat = new HashSet<String>();
		//snapshotFormat.add("otfvis");
		config.controler().setSnapshotFormat(snapshotFormat);

		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		{
			StrategySettings strategySettings2 = new StrategySettings( ConfigUtils.createAvailableStrategyId(config) ) ;
			strategySettings2.setStrategyName( DefaultStrategy.ReRoute.name() );
			strategySettings2.setWeight(0.1);
			strategySettings2.setDisableAfter(90);
			config.strategy().addStrategySettings(strategySettings2);
		}{				
			StrategySettings strategySettings2 = new StrategySettings( ConfigUtils.createAvailableStrategyId(config) ) ;
			strategySettings2.setStrategyName( DefaultSelector.ChangeExpBeta.name() );
			strategySettings2.setWeight(0.9);
			config.strategy().addStrategySettings(strategySettings2);
		}

		//		StrategySettings strategySettings4 = new StrategySettings( ConfigUtils.createAvailableStrategyId(config) ) ;
		//		strategySettings4.setStrategyName(CADYTS);
		//		strategySettings4.setWeight(1.);
		//		config.strategy().addStrategySettings(strategySettings4);

		config.strategy().setMaxAgentPlanMemorySize(5);

		// planCalcScore
		ActivityParams homeActivity = new ActivityParams("h");
		homeActivity.setTypicalDuration(12*60*60);
		config.planCalcScore().addActivityParams(homeActivity);

		ActivityParams workActivity = new ActivityParams("w");
		workActivity.setTypicalDuration(8*60*60);
		workActivity.setOpeningTime(7*60*60);
		workActivity.setClosingTime(18*60*60);
		config.planCalcScore().addActivityParams(workActivity);

		// start controller
		final Controler controler = new Controler(config);

		final MeasurementCadytsContext cContext2 = new MeasurementCadytsContext(config, buildMeasurements());
		controler.addControlerListener(cContext2);

		// scoring function
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {

				final Builder paramsBuilder = new Builder(ScenarioUtils.createScenario(config), person.getId());

				final CharyparNagelScoringParameters params = paramsBuilder.build();

				SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				final CadytsScoring<Measurement> scoringFunction2 = new CadytsScoring<>(person.getSelectedPlan(), config, cContext2);
				final double cadytsScoringWeight2 = config.planCalcScore().getBrainExpBeta() ;
				scoringFunction2.setWeightOfCadytsCorrection(cadytsScoringWeight2) ;
				scoringFunctionAccumulator.addScoringFunction(scoringFunction2 );

				return scoringFunctionAccumulator;
			}
		}) ;



		controler.run() ;
	}


	private static TreeMap<Integer, Integer> buildMeasurementsMapCumulative(){
		TreeMap<Integer, Integer> map = new TreeMap<Integer, Integer>();

		map.put(69200, 10);
		map.put(69400, 50);
		map.put(69600, 150);
		map.put(69800, 300);
		map.put(70000, 700);
		map.put(70200, 850);
		map.put(70400, 950);
		map.put(70600, 990);
		map.put(70800, 1000);

		return map;
	}


	private static TreeMap<Integer, Integer> buildMeasurementsMapSingle(){
		TreeMap<Integer, Integer> map = new TreeMap<Integer, Integer>();

		map.put(69200, 10);
		map.put(69400, 40);
		map.put(69600, 100);
		map.put(69800, 150);
		map.put(70000, 300);
		map.put(70200, 150);
		map.put(70400, 100);
		map.put(70600, 40);
		map.put(70800, 10);

		return map;
	}


	private static Tuple<Counts<Measurement>,Measurements> buildMeasurements(){
		Counts<Measurement> counts = new Counts<>();
		Measurements measurements = new Measurements() ;
		{
			Id<Measurement> id = Id.create(69200, Measurement.class);
			counts.createAndAddCount(id, null);
			Double value = 10.;
			for (int h = 1; h<=24; h++) {
				counts.getCount(id).createVolume(h, value);
			}
			double lowerBound = 0 ;
			measurements.add(counts.getCount(id), lowerBound);
		}

		{
			Id<Measurement> id = Id.create(69400, Measurement.class);
			counts.createAndAddCount(id, null);
			Double value = 40.;
			for (int h = 1; h<=24; h++) {
				counts.getCount(id).createVolume(h, value);
			}
			double lowerBound = 10*60. ;
			measurements.add(counts.getCount(id), lowerBound);
		}
		{
			Id<Measurement> id = Id.create(69600, Measurement.class);
			counts.createAndAddCount(id, null);
			Double value = 100.;
			for (int h = 1; h<=24; h++) {
				counts.getCount(id).createVolume(h, value);
			}
			double lowerBound = 20*60. ;
			measurements.add(counts.getCount(id), lowerBound);
		}
		{
			Id<Measurement> id = Id.create(69800, Measurement.class);
			counts.createAndAddCount(id, null);
			Double value = 150.;
			for (int h = 1; h<=24; h++) {
				counts.getCount(id).createVolume(h, value);
			}
			double lowerBound = 25*60 ;
			measurements.add(counts.getCount(id), lowerBound);
		}
		{
			Id<Measurement> id = Id.create(70000, Measurement.class);
			counts.createAndAddCount(id, null);
			Double value = 300.;
			for (int h = 1; h<=24; h++) {
				counts.getCount(id).createVolume(h, value);
			}
			double lowerBound = 30*60 ;
			measurements.add(counts.getCount(id), lowerBound);
		}
		{
			Id<Measurement> id = Id.create(70200, Measurement.class);
			counts.createAndAddCount(id, null);
			Double value = 150.;
			for (int h = 1; h<=24; h++) {
				counts.getCount(id).createVolume(h, value);
			}
			double lowerBound = 40*60 ;
			measurements.add(counts.getCount(id), lowerBound);
		}
		{
			Id<Measurement> id = Id.create(70400, Measurement.class);
			counts.createAndAddCount(id, null);
			Double value = 100.;
			for (int h = 1; h<=24; h++) {
				counts.getCount(id).createVolume(h, value);
			}
			double lowerBound = 50*60 ;
			measurements.add(counts.getCount(id), lowerBound);
		}
		{
			Id<Measurement> id = Id.create(70600, Measurement.class);
			counts.createAndAddCount(id, null);
			Double value = 40.;
			for (int h = 1; h<=24; h++) {
				counts.getCount(id).createVolume(h, value);
			}
			double lowerBound = 60*60 ;
			measurements.add(counts.getCount(id), lowerBound);
		}
		{
			Id<Measurement> id = Id.create(70800, Measurement.class);
			counts.createAndAddCount(id, null);
			Double value = 10.;
			for (int h = 1; h<=24; h++) {
				counts.getCount(id).createVolume(h, value);
			}
			double lowerBound = 70*60. ;
			measurements.add(counts.getCount(id), lowerBound);
		}

		return new Tuple<>( counts, measurements ) ;
	}
}