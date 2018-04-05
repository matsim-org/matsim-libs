
/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package vwExamples.initalruns.berlin;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalModule;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
//import org.matsim.contrib.drt.optimizer.rebalancing.DemandBasedRebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.run.DrtConfigConsistencyChecker;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import peoplemover.ClosestStopBasedDrtRoutingModule;
import vwExamples.peoplemoverVWExample.CustomRebalancing.DemandBasedRebalancingStrategyMy;
import vwExamples.peoplemoverVWExample.CustomRebalancing.RelocationWriter;
import vwExamples.peoplemoverVWExample.CustomRebalancing.ZonalDemandAggregatorMy;
import vwExamples.peoplemoverVWExample.CustomRebalancing.ZonalIdleVehicleCollectorMy;
import vwExamples.peoplemoverVWExample.CustomRebalancing.ZonalRelocationAggregatorMy;

/** * @author axer */

public class RunCleanBerlin {
	

	public static void main(String[] args) {
		

	final Config config = ConfigUtils.loadConfig("E:\\Axer\\MatsimDataStore2\\Berlin_Clean\\input\\config.xml");
	
	Scenario scenario = ScenarioUtils.loadScenario(config);
	
	
	//Overwrite existing configuration parameters
	config.plans().setInputFile("E:\\Axer\\MatsimDataStore2\\Berlin_Clean\\output\\be_vw_252_neu_2\\ITERS\\it.10\\be_vw_252_neu_2.10.plans.xml.gz");
	config.network().setInputFile("network/be_251.output_network.xml.gz");
	String runId = "be_vw_252_neu_3_disInnov";
	config.controler().setRunId(runId);	
	config.strategy().setFractionOfIterationsToDisableInnovation(0.5);
	config.controler().setOutputDirectory("E:\\Axer\\MatsimDataStore2\\Berlin_Clean\\output\\"+runId); //Define dynamically the the output path
	Controler controler = new Controler(config);
	controler.addOverridingModule(new CadytsCarModule());
	
	// include cadyts into the plan scoring (this will add the cadyts corrections to the scores):
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			private final ScoringParametersForPerson parameters = new SubpopulationScoringParameters( scenario );
			@Inject CadytsContext cContext;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {

				final ScoringParameters params = parameters.getScoringParameters( person );
				
				SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cContext);
				final double cadytsScoringWeight = 20. * config.planCalcScore().getBrainExpBeta() ;
				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
				scoringFunctionAccumulator.addScoringFunction(scoringFunction );

				return scoringFunctionAccumulator;
			}
		}) ;
	
	controler.run();
			

	}
}

