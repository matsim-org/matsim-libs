package playground.dziemke.cadyts.examples;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.car.CadytsCarScoring;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.car.CadytsPlanChanger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

public class EquilController04a {
	private final static Logger log = Logger.getLogger(EquilController04a.class);

	public static void main(final String[] args) {
		final Config config = ConfigUtils.createConfig();
		
		// global
		config.global().setRandomSeed(4711);
		config.global().setCoordinateSystem("Atlantis");
		
		// network
		String inputNetworkFile = "D:/Workspace/container/examples/equil/input/network.xml";
		config.network().setInputFile(inputNetworkFile);
		
		// plans
		String inputPlansFile = "D:/Workspace/container/examples/equil/input/plans100.xml";
		config.plans().setInputFile(inputPlansFile);
		
		//simulation
		config.addSimulationConfigGroup(new SimulationConfigGroup());
		config.simulation().setStartTime(0);
		config.simulation().setEndTime(0);
		config.simulation().setSnapshotPeriod(60);
		//config.simulation().setFlowCapFactor(0.01);
		//config.simulation().setStorageCapFactor(0.02);
		
		// counts
		String countsFileName = "D:/Workspace/container/examples/equil/input/counts100.xml";
		config.counts().setCountsFileName(countsFileName);
		//config.counts().setCountsScaleFactor(100);
		config.counts().setOutputFormat("all");
		
		// controller
		//String runId = "run_xy";
		String outputDirectory = "D:/Workspace/container/examples/equil/output/04a/";
		//config.controler().setRunId(runId);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(50);
		//Set<EventsFileFormat> eventsFileFormats = Collections.unmodifiableSet(EnumSet.of(EventsFileFormat.xml));
		//config.controler().setEventsFileFormats(eventsFileFormats);
		config.controler().setMobsim("queueSimulation");
		//config.controler().setWritePlansInterval(50);
		//config.controler().setWriteEventsInterval(50);
		//Set<String> snapshotFormat = Collections.emptySet();
		Set<String> snapshotFormat = new HashSet<String>();
		//snapshotFormat.add("otfvis");
		config.controler().setSnapshotFormat(snapshotFormat);
		
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
				
		// strategy
//		StrategySettings strategySettings1 = new StrategySettings(new IdImpl(1));
//		strategySettings1.setModuleName("BestScore");
//		strategySettings1.setProbability(0.9);
//		config.strategy().addStrategySettings(strategySettings1);
		
		StrategySettings strategySettings1 = new StrategySettings(new IdImpl(1));
		strategySettings1.setModuleName("cadytsCar");
		strategySettings1.setProbability(0.9);
		config.strategy().addStrategySettings(strategySettings1);
		
		StrategySettings strategySettings2 = new StrategySettings(new IdImpl(2));
		strategySettings2.setModuleName("ReRoute");
		strategySettings2.setProbability(0.1);
		config.strategy().addStrategySettings(strategySettings2);
				
		config.strategy().setMaxAgentPlanMemorySize(5);
		
		// start controller
		final Controler controler = new Controler(config);
		
		// cadytsCar
		final CadytsContext cContext = new CadytsContext(controler.getConfig());
		controler.addControlerListener(cContext);
		
		controler.getConfig().getModule("cadytsCar").addParam("startTime", "00:00:00");
		controler.getConfig().getModule("cadytsCar").addParam("endTime", "24:00:00");
						
		// plan strategy
		controler.addPlanStrategyFactory("cadytsCar", new PlanStrategyFactory() {
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario2, EventsManager events2) {
				final CadytsPlanChanger planSelector = new CadytsPlanChanger(cContext);
				planSelector.setCadytsWeight(30.*scenario2.getConfig().planCalcScore().getBrainExpBeta());
				return new PlanStrategyImpl(planSelector);
			}
		});
				
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
//				final CadytsCarScoring scoringFunction = new CadytsCarScoring(plan, config, cContext);
//				final double cadytsScoringWeight = 1.0;
//				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
//				scoringFunctionAccumulator.addScoringFunction(scoringFunction );
//
//				return scoringFunctionAccumulator;
//			}
//		}) ;
		
		// run controller 
		controler.run() ;
	}
}