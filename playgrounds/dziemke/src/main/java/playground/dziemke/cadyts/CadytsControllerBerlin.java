/* *********************************************************************** *
 * project: org.matsim.*
 * CadytsController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.dziemke.cadyts;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.cadyts.general.ExpBetaPlanChangerWithCadytsPlanRegistration;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
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

public class CadytsControllerBerlin {
	private final static Logger log = Logger.getLogger(CadytsControllerBerlin.class);
	
	public static void main(String[] args) {
		final Config config = ConfigUtils.createConfig();
		
		// global
		//config.global().setRandomSeed(4711);
		config.global().setCoordinateSystem("GK4");
		
		// network
		String inputNetworkFile = "D:/Workspace/container/demand/input/iv_counts/network.xml";
		config.network().setInputFile(inputNetworkFile);
		
		// plans
		String inputPlansFile = "D:/Workspace/container/demand/input/cemdap2matsim/24/plans.xml.gz";
		//String inputPlansFile = "D:/Workspace/container/demand/input/hwh/population3.xml";
		//String inputPlansFile = "D:/Workspace/container/demand/input/cemdap2matsim/24/plansSelection10.xml.gz";
		config.plans().setInputFile(inputPlansFile);			
		
		// simulation
//		config.addQSimConfigGroup(new QSimConfigGroup());
//		//config.getQSimConfigGroup().setFlowCapFactor(0.01);
//		config.getQSimConfigGroup().setFlowCapFactor(0.02);
//		config.getQSimConfigGroup().setStorageCapFactor(0.02);
//		//config.getQSimConfigGroup().setStorageCapFactor(0.05);
//		config.getQSimConfigGroup().setRemoveStuckVehicles(false);
		
		config.addModule( new SimulationConfigGroup() );
		//config.simulation().setStartTime(0);
		//config.simulation().setEndTime(0);
		//config.simulation().setSnapshotPeriod(60);
		((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).setFlowCapFactor(0.02);
		((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).setStorageCapFactor(0.02);
		((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).setRemoveStuckVehicles(false);
		
						
		// counts
		String countsFileName = "D:/Workspace/container/demand/input/iv_counts/vmz_di-do.xml";
		config.counts().setCountsFileName(countsFileName);
		config.counts().setCountsScaleFactor(100);
		config.counts().setOutputFormat("all");
		
		// vsp experimental
		config.vspExperimental().addParam("vspDefaultsCheckingLevel", "abort");
				
		// controller
		String runId = "run_id";
		String outputDirectory = "D:/Workspace/container/demand/output/" + runId + "/";
		//String outputDirectory = "D:/Workspace/container/demand/output/beeline/" + runId + "/";
		config.controler().setRunId(runId);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(150);
		//config.controler().setLastIteration(0);
		Set<EventsFileFormat> eventsFileFormats = Collections.unmodifiableSet(EnumSet.of(EventsFileFormat.xml));
		config.controler().setEventsFileFormats(eventsFileFormats);
		config.controler().setMobsim("qsim");
		config.controler().setWritePlansInterval(50);
		//config.controler().setWritePlansInterval(10);
		config.controler().setWriteEventsInterval(50);
		//config.controler().setWriteEventsInterval(10);
		Set<String> snapshotFormat = new HashSet<String>();
		//snapshotFormat.add("otfvis");
		config.controler().setSnapshotFormat(snapshotFormat);
				
		// strategy
//		StrategySettings strategySettings1 = new StrategySettings(new IdImpl(2));
//		strategySettings1.setModuleName("ChangeExpBeta");
//		strategySettings1.setProbability(1.0);
//		config.strategy().addStrategySettings(strategySettings1);
		
		StrategySettings strategySettings2 = new StrategySettings(Id.create(1, StrategySettings.class));
		strategySettings2.setModuleName("ReRoute");
		//strategySettings2.setProbability(1.0);
		strategySettings2.setProbability(0.5);
		strategySettings2.setDisableAfter(90);
		//strategySettings2.setDisableAfter(150);
		config.strategy().addStrategySettings(strategySettings2);
		
		StrategySettings strategySetinngs3 = new StrategySettings(Id.create(2, StrategySettings.class));
		strategySetinngs3.setModuleName("cadytsCar") ;
		strategySetinngs3.setProbability(1.0) ;
		config.strategy().addStrategySettings(strategySetinngs3);
		
		config.strategy().setMaxAgentPlanMemorySize(10);
		//config.strategy().setMaxAgentPlanMemorySize(5);
		
		// planCalcScore
		ActivityParams homeActivity = new ActivityParams("home");
		homeActivity.setTypicalDuration(12*60*60);
		config.planCalcScore().addActivityParams(homeActivity);
		
		ActivityParams workActivity = new ActivityParams("work");
		workActivity.setTypicalDuration(9*60*60);
		config.planCalcScore().addActivityParams(workActivity);
		
		ActivityParams leisureActivity = new ActivityParams("leis");
		leisureActivity.setTypicalDuration(2*60*60);
		config.planCalcScore().addActivityParams(leisureActivity);
		
		ActivityParams shopActivity = new ActivityParams("shop");
		shopActivity.setTypicalDuration(1*60*60);
		config.planCalcScore().addActivityParams(shopActivity);
		
		ActivityParams otherActivity = new ActivityParams("other");
		otherActivity.setTypicalDuration(0.5*60*60);
		config.planCalcScore().addActivityParams(otherActivity);
		
		// ActivityParams educActivity = new ActivityParams("educ");
		// educActivity.setTypicalDuration(9*60*60);
		// config.planCalcScore().addActivityParams(educActivity);
		
		// start controller
		final Controler controler = new Controler(config);
		
		// cadytsContext (and cadytsCarConfigGroup)
		final CadytsContext cContext = new CadytsContext(controler.getConfig());
		// CadytsContext generates new CadytsCarConfigGroup with name "cadytsCar"
		controler.addControlerListener(cContext);
		
		controler.getConfig().getModule("cadytsCar").addParam("startTime", "00:00:00");
		//controler.getConfig().getModule("cadytsCar").addParam("startTime", "04:00:00");
		controler.getConfig().getModule("cadytsCar").addParam("endTime", "24:00:00");
		//controler.getConfig().getModule("cadytsCar").addParam("useBruteForce", "true");
		//controler.getConfig().getModule("cadytsCar").addParam("minFlowStddevVehH", "8");
		
		// old plan strategy
//		controler.addPlanStrategyFactory("cadytsCar", new PlanStrategyFactory() {
//			@Override
//			public PlanStrategy createPlanStrategy(Scenario scenario2, EventsManager events2) {
//				final CadytsPlanChanger planSelector = new CadytsPlanChanger(cContext);
//				// setting cadyts weight very high = close to brute force
//				planSelector.setCadytsWeight(0.*scenario2.getConfig().planCalcScore().getBrainExpBeta());
//				return new PlanStrategyImpl(planSelector);
//			}
//		});
		
		// new plan strategy
		controler.addPlanStrategyFactory("cadytsCar", new PlanStrategyFactory() {
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario, EventsManager eventsManager) {
				//return new PlanStrategyImpl(new CadytsExtendedExpBetaPlanChanger(
				return new PlanStrategyImpl(new ExpBetaPlanChangerWithCadytsPlanRegistration(
						scenario.getConfig().planCalcScore().getBrainExpBeta(), cContext));
			}
		});
		
		// scoring function
		final CharyparNagelScoringParameters params = new CharyparNagelScoringParameters(config.planCalcScore());
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				
				ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
				// outcommenting following lines until return statement -> set scoring to zero
				// outcommenting following three lines -> cadyts-only scoring
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				//final CadytsCarScoring scoringFunction = new CadytsCarScoring(plan, config, cContext);
				final CadytsScoring scoringFunction = new CadytsScoring(person.getSelectedPlan(), config, cContext);
				final double cadytsScoringWeight = 10.0;
				//final double cadytsScoringWeight = 0.0;
				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
				scoringFunctionAccumulator.addScoringFunction(scoringFunction );

				return scoringFunctionAccumulator;
			}
		}) ;

		controler.run();
	}
}
