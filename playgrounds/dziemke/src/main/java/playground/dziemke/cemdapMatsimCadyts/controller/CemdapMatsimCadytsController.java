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

package playground.dziemke.cemdapMatsimCadyts.controller;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory.SubpopulationCharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

public class CemdapMatsimCadytsController {
	//private final static Logger log = Logger.getLogger(CemdapMatsimCadytsController.class);
	
	public static void main(String[] args) {
		final Config config = ConfigUtils.createConfig();
		
		// global
		//config.global().setRandomSeed(4711);
		config.global().setCoordinateSystem("GK4");
		
		// network
		String inputNetworkFile = "../../shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
		config.network().setInputFile(inputNetworkFile);
		
		// plans
		String inputPlansFile = "../../container/demand/input/cemdap2matsim/24/plans.xml.gz";
		config.plans().setInputFile(inputPlansFile);
		
		// simulation
		config.qsim().setFlowCapFactor(0.02);
		config.qsim().setStorageCapFactor(0.02);
		config.qsim().setRemoveStuckVehicles(false);
				
						
		// counts
		String countsFileName = "../../shared-svn/studies/countries/de/berlin/counts/iv_counts/vmz_di-do.xml";
		config.counts().setCountsFileName(countsFileName);
		config.counts().setCountsScaleFactor(100);
		config.counts().setOutputFormat("all");
		
		// vsp experimental
		config.vspExperimental().addParam("vspDefaultsCheckingLevel", "ignore");
				
		// controller
		String runId = "run_146";
		String outputDirectory = "D:/Workspace/container/demand/output/" + runId + "/";
		config.controler().setRunId(runId);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(150);
		Set<EventsFileFormat> eventsFileFormats = Collections.unmodifiableSet(EnumSet.of(EventsFileFormat.xml));
		config.controler().setEventsFileFormats(eventsFileFormats);
		config.controler().setMobsim("qsim");
		config.controler().setWritePlansInterval(50);
		config.controler().setWriteEventsInterval(50);
		Set<String> snapshotFormat = new HashSet<String>();
		//snapshotFormat.add("otfvis");
		config.controler().setSnapshotFormat(snapshotFormat);
				
		// strategy
//		StrategySettings strategySettings1 = new StrategySettings(new IdImpl(1));
//		strategySettings1.setModuleName("ChangeExpBeta");
//		strategySettings1.setProbability(1.0);
//		config.strategy().addStrategySettings(strategySettings1);
		
		StrategySettings strategySettings2 = new StrategySettings(Id.create(2, StrategySettings.class));
		strategySettings2.setStrategyName("ReRoute");
		strategySettings2.setWeight(0.5);
		strategySettings2.setDisableAfter(90);
		config.strategy().addStrategySettings(strategySettings2);
		
		StrategySettings strategySetinngs3 = new StrategySettings(Id.create(1, StrategySettings.class));
		strategySetinngs3.setStrategyName("cadytsCar");
		strategySetinngs3.setWeight(1.0);
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
		controler.getConfig().getModule("cadytsCar").addParam("endTime", "24:00:00");
		
        // not necessary anymore, just use normal ChangeExpBeta
//		controler.addPlanStrategyFactory("cadytsCar", new PlanStrategyFactory() {
//			@Override
//			public PlanStrategy get() {
//				return new PlanStrategyImpl(new ExpBetaPlanChangerWithCadytsPlanRegistration<Link>(
//						controler.getConfig().planCalcScore().getBrainExpBeta(), cContext));
//			}
//		});
		
		// scoring function
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			final CharyparNagelScoringParametersForPerson parameters = new SubpopulationCharyparNagelScoringParameters( controler.getScenario() );

			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final CharyparNagelScoringParameters params = parameters.getScoringParameters( person );

				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				// outcommenting following lines until return statement -> set scoring to zero
				// outcommenting following three lines -> cadyts-only scoring
				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				final CadytsScoring<Link> scoringFunction = new CadytsScoring<Link>(person.getSelectedPlan(), config, cContext);
				//final double cadytsScoringWeight = 0.0;
				final double cadytsScoringWeight = 15.0;
				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
				sumScoringFunction.addScoringFunction(scoringFunction );

				return sumScoringFunction;
			}
		}) ;

		controler.run();
	}
}
