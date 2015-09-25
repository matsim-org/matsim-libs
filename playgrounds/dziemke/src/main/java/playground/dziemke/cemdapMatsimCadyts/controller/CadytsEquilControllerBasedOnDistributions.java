package playground.dziemke.cemdapMatsimCadyts.controller;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.distribution.CadytsContextDistributionBased;
import org.matsim.contrib.cadyts.general.CadytsPlanChanger;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.cadyts.measurement.MeasurementCadytsContext;
import org.matsim.contrib.cadyts.measurement.Measurement;
import org.matsim.contrib.cadyts.measurement.Measurements;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters.CharyparNagelScoringParametersBuilder;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;

public class CadytsEquilControllerBasedOnDistributions {
	private final static Logger log = Logger.getLogger(CadytsEquilControllerBasedOnDistributions.class);

	public static void main(final String[] args) {
		final Config config = ConfigUtils.createConfig();
		
		// global
		config.global().setRandomSeed(4711);
		config.global().setCoordinateSystem("Atlantis");
		
		// network
		String inputNetworkFile = "/Users/dominik/Workspace/data/examples/equil/input/network_diff_lengths2.xml";
		config.network().setInputFile(inputNetworkFile);
		
		// plans
		String inputPlansFile = "/Users/dominik/Workspace/data/examples/equil/input/plans1000.xml";
		config.plans().setInputFile(inputPlansFile);
		
		//simulation
		config.addModule( new SimulationConfigGroup() );
		((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).setStartTime(0);
		((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).setEndTime(0);
		((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).setSnapshotPeriod(60);
		
		// counts
//		String countsFileName = "/Users/dominik/Workspace/data/examples/equil/input/counts100-200.xml";
//		config.counts().setCountsFileName(countsFileName);
//		//config.counts().setCountsScaleFactor(100);
//		config.counts().setOutputFormat("all");
		
		// vsp experimental
		// config.vspExperimental().addParam("vspDefaultsCheckingLevel", "abort");
		config.vspExperimental().addParam("vspDefaultsCheckingLevel", "ignore");
		
		// controller
		String runId = "74";
		String outputDirectory = "/Users/dominik/Workspace/data/examples/equil/output/" + runId + "/";
		config.controler().setRunId(runId);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(200);
//		config.controler().setLastIteration(10);
		//Set<EventsFileFormat> eventsFileFormats = Collections.unmodifiableSet(EnumSet.of(EventsFileFormat.xml));
		//config.controler().setEventsFileFormats(eventsFileFormats);
		config.controler().setMobsim("qsim");
		Set<String> snapshotFormat = new HashSet<String>();
		//snapshotFormat.add("otfvis");
		config.controler().setSnapshotFormat(snapshotFormat);
		
		// strategy
//		StrategySettings strategySettings1 = new StrategySettings(Id.create(1, StrategySettings.class));
//		strategySettings1.setStrategyName("ChangeExpBeta");
//		strategySettings1.setWeight(1.);
//		config.strategy().addStrategySettings(strategySettings1);
		
		StrategySettings strategySettings2 = new StrategySettings(Id.create(2, StrategySettings.class));
		strategySettings2.setStrategyName("ReRoute");
		strategySettings2.setWeight(1.);
		strategySettings2.setDisableAfter(90);
		config.strategy().addStrategySettings(strategySettings2);
				
//		StrategySettings strategySettings3 = new StrategySettings(Id.create(1, StrategySettings.class));
//		strategySettings3.setStrategyName("cadytsCar");
////		strategySettings3.setWeight(1.);
//		strategySettings3.setWeight(.5);
//		config.strategy().addStrategySettings(strategySettings3);
		
		//
		StrategySettings strategySettings4 = new StrategySettings(Id.create(3, StrategySettings.class));
		strategySettings4.setStrategyName("cadytsCar");
		strategySettings4.setWeight(1.);
//		strategySettings4.setWeight(.5);
		config.strategy().addStrategySettings(strategySettings4);
		//
					
		config.strategy().setMaxAgentPlanMemorySize(5);
		
		// planCalcScore
		ActivityParams homeActivity = new ActivityParams("h");
		homeActivity.setPriority(1);
		homeActivity.setTypicalDuration(12*60*60);
		homeActivity.setMinimalDuration(8*60*60);
		config.planCalcScore().addActivityParams(homeActivity);
						
		ActivityParams workActivity = new ActivityParams("w");
		workActivity.setPriority(1);
		workActivity.setTypicalDuration(8*60*60);
		workActivity.setMinimalDuration(6*60*60);
		workActivity.setOpeningTime(7*60*60);
		workActivity.setLatestStartTime(9*60*60);
		workActivity.setClosingTime(18*60*60);
		config.planCalcScore().addActivityParams(workActivity);
		
		// start controller
		final Controler controler = new Controler(config);
		
		// cadytsContext (and cadytsCarConfigGroup)
		// CadytsContext generates new CadytsCarConfigGroup with name "cadytsCar"
//		final CadytsContext cContext = new CadytsContext(config);
//		controler.addControlerListener(cContext);
//			
//		controler.getConfig().getModule("cadytsCar").addParam("startTime", "06:00:00");
//		controler.getConfig().getModule("cadytsCar").addParam("endTime", "07:00:00");
		
		//
		final MeasurementCadytsContext cContext2 = new MeasurementCadytsContext(config, buildMeasurements());
//		final CadytsContextDistributionBased cContext2 = new CadytsContextDistributionBased(config, buildMeasurementsMapSingle());
//		final CadytsContextDistributionBased cContext2 = new CadytsContextDistributionBased(config, buildMeasurementsMapCumulative());		
//		final CadytsContextDistributionBased cContext2 = new CadytsContextDistributionBased(config);
//		final CadytsContextDistributionBased cContext2 = new CadytsContextDistributionBased(config, buildMeasurementsAsCounts());
		controler.addControlerListener(cContext2);
//		controler.getConfig().getModule("cadytsCar").addParam("startTime", "06:00:00");
//		controler.getConfig().getModule("cadytsCar").addParam("endTime", "07:00:00");
		// TODO may not be called "cadytsCar" a second time
		
		//controler.getConfig().getModule("cadytsCar").addParam("preparatoryIterations", "20");
		//controler.getConfig().getModule("cadytsCar").addParam("useBruteForce", "false");
						
		// plan strategy
//		controler.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				addPlanStrategyBinding("cadytsCar").toProvider(new javax.inject.Provider<PlanStrategy>() {
//					@Override
//					public PlanStrategy get() {
//						return new PlanStrategyImpl(new CadytsPlanChanger(controler.getScenario(), cContext));
//					}
//				});
//			}
//		});
		
		//
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("cadytsCar").toProvider(new javax.inject.Provider<PlanStrategy>() {
					@Override
					public PlanStrategy get() {
						return new PlanStrategyImpl(new CadytsPlanChanger(controler.getScenario(), cContext2));
					}
				});
			}
		});
		//
				
		// scoring function
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {

//				final CharyparNagelScoringParameters params = CharyparNagelScoringParameters.getBuilder(config.planCalcScore(), config.scenario()).create();
				final CharyparNagelScoringParametersBuilder paramsBuilder = CharyparNagelScoringParameters.getBuilder(
						ScenarioUtils.createScenario(config), person.getId());
				
				final CharyparNagelScoringParameters params = paramsBuilder.create();
				
				SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

//				final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cContext);
//				final double cadytsScoringWeight = 30. * config.planCalcScore().getBrainExpBeta() ;
//				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
//				scoringFunctionAccumulator.addScoringFunction(scoringFunction );
				
				//
//				final CadytsScoring<Link> scoringFunction2 = new CadytsScoring<>(person.getSelectedPlan(), config, cContext2);
//				final CadytsScoring<Integer> scoringFunction2 = new CadytsScoring<>(person.getSelectedPlan(), config, cContext2);
				final CadytsScoring<Measurement> scoringFunction2 = new CadytsScoring<>(person.getSelectedPlan(), config, cContext2);
				final double cadytsScoringWeight2 = 0. * config.planCalcScore().getBrainExpBeta() ;
				scoringFunction2.setWeightOfCadytsCorrection(cadytsScoringWeight2) ;
				scoringFunctionAccumulator.addScoringFunction(scoringFunction2 );
				//

				return scoringFunctionAccumulator;
			}
		}) ;
		
		
		
		controler.run() ;
	}
	
	
//	private static Map<String, String> buildMeasurementsMap(){
//	private static TreeMap<Id<Link>, Count> buildMeasurementsMap(){
//	private static TreeMap<Double, Double> buildMeasurementsMap(){
	private static TreeMap<Integer, Integer> buildMeasurementsMapCumulative(){
//		Map<String, String> map = new TreeMap<String, String>();
//		TreeMap<Double, Double> map = new TreeMap<Double, Double>();
		// cumulative!!!
//		TreeMap<Id<Link>, Double> map = new TreeMap<Id<Link>, Double>();
		TreeMap<Integer, Integer> map = new TreeMap<Integer, Integer>();
//		map.put(Id.create(2000, Link.class), 10.);
//		map.put(Id.create(4000, Link.class), 50.);
//		map.put(Id.create(6000, Link.class), 150.);
//		map.put(Id.create(8000, Link.class), 300.);
//		map.put(Id.create(10000, Link.class), 700.);
//		map.put(Id.create(12000, Link.class), 850.);
//		map.put(Id.create(14000, Link.class), 950.);
//		map.put(Id.create(16000, Link.class), 990.);
//		map.put(Id.create(18000, Link.class), 1000.);
		
		map.put(69200, 10);
		map.put(69400, 50);
		map.put(69600, 150);
		map.put(69800, 300);
		map.put(70000, 700);
		map.put(70200, 850);
		map.put(70400, 950);
		map.put(70600, 990);
		map.put(70800, 1000);
		
//		map.put(62000, 10);
//		map.put(64000, 20);
//		map.put(66000, 30);
//		map.put(68000, 40);
//		map.put(70000, 50);
//		map.put(72000, 60);
//		map.put(74000, 70);
//		map.put(76000, 80);
//		map.put(78000, 1000);
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
	
	
	
//	private static Counts buildMeasurementsAsCounts(){
//		Counts counts = new Counts();
//		{
//			Id<Link> id = Id.create(2000, Link.class);
//			counts.createAndAddCount(id, "< 2000m");
//			Double value = 10.;
//			for (int h=1; h<=24; h++) {
//				counts.getCount(id).createVolume(h, value);
//			}
//		}
//
//		
//	}
	
	
	private static Measurements buildMeasurements(){
		Measurements measurements = new Measurements();
		
		{
			Id<Measurement> id = Id.create(69200, Measurement.class);
			measurements.createAndAddMeasurement(id);
			Double value = 10.;
			for (int h = 1; h<=24; h++) {
				measurements.getMeasurment(id).createVolume(h, value);
			}
		}

		{
			Id<Measurement> id = Id.create(69400, Measurement.class);
			measurements.createAndAddMeasurement(id);
			Double value = 40.;
			for (int h = 1; h<=24; h++) {
				measurements.getMeasurment(id).createVolume(h, value);
			}
		}
		{
			Id<Measurement> id = Id.create(69600, Measurement.class);
			measurements.createAndAddMeasurement(id);
			Double value = 100.;
			for (int h = 1; h<=24; h++) {
				measurements.getMeasurment(id).createVolume(h, value);
			}
		}
		{
			Id<Measurement> id = Id.create(69800, Measurement.class);
			measurements.createAndAddMeasurement(id);
			Double value = 150.;
			for (int h = 1; h<=24; h++) {
				measurements.getMeasurment(id).createVolume(h, value);
			}
		}
		{
			Id<Measurement> id = Id.create(70000, Measurement.class);
			measurements.createAndAddMeasurement(id);
			Double value = 300.;
			for (int h = 1; h<=24; h++) {
				measurements.getMeasurment(id).createVolume(h, value);
			}
		}
		{
			Id<Measurement> id = Id.create(70200, Measurement.class);
			measurements.createAndAddMeasurement(id);
			Double value = 150.;
			for (int h = 1; h<=24; h++) {
				measurements.getMeasurment(id).createVolume(h, value);
			}
		}
		{
			Id<Measurement> id = Id.create(70400, Measurement.class);
			measurements.createAndAddMeasurement(id);
			Double value = 100.;
			for (int h = 1; h<=24; h++) {
				measurements.getMeasurment(id).createVolume(h, value);
			}
		}
		{
			Id<Measurement> id = Id.create(70600, Measurement.class);
			measurements.createAndAddMeasurement(id);
			Double value = 40.;
			for (int h = 1; h<=24; h++) {
				measurements.getMeasurment(id).createVolume(h, value);
			}
		}
		{
			Id<Measurement> id = Id.create(70800, Measurement.class);
			measurements.createAndAddMeasurement(id);
			Double value = 10.;
			for (int h = 1; h<=24; h++) {
				measurements.getMeasurment(id).createVolume(h, value);
			}
		}
		
		return measurements;
	}
}