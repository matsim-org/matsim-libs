package playground.dziemke.cemdapMatsimCadyts.controller;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsPlanChanger;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

public class CadytsEquilController {
	private final static Logger log = Logger.getLogger(CadytsEquilController.class);

	public static void main(final String[] args) {
		final Config config = ConfigUtils.createConfig();
		
		// global
		config.global().setRandomSeed(4711);
		config.global().setCoordinateSystem("Atlantis");
		
		// network
		// String inputNetworkFile = "D:/Workspace/container/demand/input/iv_counts/network.xml";
		// changed to original location
		// String inputNetworkFile = "../../shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
		// String inputNetworkFile = "/Users/dominik/Workspace/shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
		String inputNetworkFile = "/Users/dominik/Workspace/data/examples/equil/input/network.xml";
		config.network().setInputFile(inputNetworkFile);
		
		// plans
		// String inputPlansFile = "D:/Workspace/container/examples/equil/input/plans1000.xml";
		String inputPlansFile = "/Users/dominik/Workspace/data/examples/equil/input/plans1000.xml";
		config.plans().setInputFile(inputPlansFile);
		
		//simulation
//		config.addSimulationConfigGroup(new SimulationConfigGroup());
//		config.simulation().setStartTime(0);
//		config.simulation().setEndTime(0);
//		config.simulation().setSnapshotPeriod(60);
		//config.simulation().setFlowCapFactor(0.01);
		//config.simulation().setStorageCapFactor(0.02);
//		config.addQSimConfigGroup(new QSimConfigGroup());
////		config.getQSimConfigGroup().setFlowCapFactor(0.01);
////		config.getQSimConfigGroup().setStorageCapFactor(0.02);
//		config.getQSimConfigGroup().setStartTime(0);
//		config.getQSimConfigGroup().setEndTime(0);
//		config.getQSimConfigGroup().setSnapshotPeriod(60);
		
		config.addModule( new SimulationConfigGroup() );
		((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).setStartTime(0);
		((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).setEndTime(0);
		((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).setSnapshotPeriod(60);
		
		// counts
		// String countsFileName = "D:/Workspace/container/examples/equil/input/counts100-200.xml";
		String countsFileName = "/Users/dominik/Workspace/data/examples/equil/input/counts100-200.xml";
		config.counts().setCountsFileName(countsFileName);
		//config.counts().setCountsScaleFactor(100);
		config.counts().setOutputFormat("all");
		
		// vsp experimental
		// config.vspExperimental().addParam("vspDefaultsCheckingLevel", "abort");
		config.vspExperimental().addParam("vspDefaultsCheckingLevel", "ignore");
		
		// controller
		String runId = "60b";
		// String outputDirectory = "D:/Workspace/container/examples/equil/output/" + runId + "/";
		String outputDirectory = "/Users/dominik/Workspace/data/examples/equil/output/" + runId + "/";
		config.controler().setRunId(runId);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(200);
		//Set<EventsFileFormat> eventsFileFormats = Collections.unmodifiableSet(EnumSet.of(EventsFileFormat.xml));
		//config.controler().setEventsFileFormats(eventsFileFormats);
		//config.controler().setMobsim("queueSimulation");
		config.controler().setMobsim("qsim");
		// KAI: change to QSim
		//config.controler().setWritePlansInterval(50);
		//config.controler().setWriteEventsInterval(50);
		//Set<String> snapshotFormat = Collections.emptySet();
		Set<String> snapshotFormat = new HashSet<String>();
		//snapshotFormat.add("otfvis");
		config.controler().setSnapshotFormat(snapshotFormat);
		
		// strategy
		StrategySettings strategySettings1 = new StrategySettings(Id.create(1, StrategySettings.class));
		strategySettings1.setStrategyName("ChangeExpBeta");
		strategySettings1.setWeight(1.);
		config.strategy().addStrategySettings(strategySettings1);
		
		StrategySettings strategySettings2 = new StrategySettings(Id.create(2, StrategySettings.class));
		strategySettings2.setStrategyName("ReRoute");
		strategySettings2.setWeight(1.);
		//strategySettings2.setProbability(.5);
		//strategySettings2.setDisableAfter(60);
		strategySettings2.setDisableAfter(90);
		config.strategy().addStrategySettings(strategySettings2);
				
//		StrategySettings strategySettings3 = new StrategySettings(Id.create(1, StrategySettings.class));
//		strategySettings3.setStrategyName("cadytsCar");
//		strategySettings3.setWeight(1.);
//		config.strategy().addStrategySettings(strategySettings3);
					
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
		//workActivity.setEarliestEndTime();
		workActivity.setClosingTime(18*60*60);
		config.planCalcScore().addActivityParams(workActivity);
		
		// start controller
		final Controler controler = new Controler(config);
		
		// cadytsContext (and cadytsCarConfigGroup)
		
//		final CadytsContext cContext = new CadytsContext(controler.getConfig());
//		// CadytsContext generates new CadytsCarConfigGroup with name "cadytsCar"
//		controler.addControlerListener(cContext);
		
//		final CadytsContext cContext = new CadytsContext(config);
//		controler.addControlerListener(cContext);
//			
//		controler.getConfig().getModule("cadytsCar").addParam("startTime", "06:00:00");
//		controler.getConfig().getModule("cadytsCar").addParam("endTime", "07:00:00");
		//controler.getConfig().getModule("cadytsCar").addParam("preparatoryIterations", "20");
		//controler.getConfig().getModule("cadytsCar").addParam("useBruteForce", "false");
						
		// old plan strategy
//		controler.addPlanStrategyFactory("cadytsCar", new PlanStrategyFactory() {
//			@Override
//			public PlanStrategy createPlanStrategy(Scenario scenario2, EventsManager events2) {
//				final CadytsPlanChanger planSelector = new CadytsPlanChanger(cContext);
//				planSelector.setCadytsWeight(0.*scenario2.getConfig().planCalcScore().getBrainExpBeta());
//				return new PlanStrategyImpl(planSelector);
//			}
//		});
		
		// new plan strategy
//		controler.addPlanStrategyFactory("cadytsCar", new PlanStrategyFactory() {
//			@Override
//			public PlanStrategy createPlanStrategy(Scenario scenario, EventsManager eventsManager) {
//				// return new PlanStrategyImpl(new CadytsExtendedExpBetaPlanChanger(
//				return new PlanStrategyImpl(new ExpBetaPlanChangerWithCadytsPlanRegistration(
//						scenario.getConfig().planCalcScore().getBrainExpBeta(), cContext));
//			}
//		} ) ;
		
		
		
//		controler.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
////				addPlanStrategyBinding(CADYTS_STRATEGY_NAME).toProvider(new javax.inject.Provider<PlanStrategy>() {
//				addPlanStrategyBinding("cadytsCar").toProvider(new javax.inject.Provider<PlanStrategy>() {
//					@Override
//					public PlanStrategy get() {
////						return new PlanStrategyImpl(new CadytsPlanChanger(controler.getScenario(), context));
//						return new PlanStrategyImpl(new CadytsPlanChanger(controler.getScenario(), cContext));
//					}
//				});
//			}
//		});
				
		// scoring function
//		final CharyparNagelScoringParameters params = new CharyparNagelScoringParameters(config.planCalcScore());
//		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
//			@Override
//			public ScoringFunction createNewScoringFunction(Plan plan) {
//						
//				ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
//				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
//				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
//				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
//
//				//final CadytsCarScoring scoringFunction = new CadytsCarScoring(plan, config, cContext);
//				final CadytsScoring scoringFunction = new CadytsScoring(plan, config, cContext);
//				final double cadytsScoringWeight = 30.0;
//				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
//				scoringFunctionAccumulator.addScoringFunction(scoringFunction );
//
//				return scoringFunctionAccumulator;
//			}
//		});
		
//		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
//			@Override
//			public ScoringFunction createNewScoringFunction(Person person) {
//
//				final CharyparNagelScoringParameters params = CharyparNagelScoringParameters.getBuilder(config.planCalcScore(), config.scenario()).create();
//				
//				SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
//				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
//				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
//				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
//
//				final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cContext);
////				final double cadytsScoringWeight = 30. * config.planCalcScore().getBrainExpBeta() ;
//				final double cadytsScoringWeight = 0. * config.planCalcScore().getBrainExpBeta() ;
//				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
//				scoringFunctionAccumulator.addScoringFunction(scoringFunction );
//
//				return scoringFunctionAccumulator;
//			}
//		}) ;
		
		// zero scoring function
//		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
//			@Override
//			public ScoringFunction createNewScoringFunction(Plan plan) {
//				ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
//				return scoringFunctionAccumulator;
//			}
//		});
		
		controler.run() ;
	}
}