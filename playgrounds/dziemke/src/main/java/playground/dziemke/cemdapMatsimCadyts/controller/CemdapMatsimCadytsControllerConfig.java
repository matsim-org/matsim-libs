/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
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

import playground.dziemke.analysis.SelectedPlansAnalyzer;
import playground.dziemke.analysis.TripAnalyzerV2Extended;

/**
 * @author dziemke
 */
public class CemdapMatsimCadytsControllerConfig {
	
	public static void main(final String[] args) {
		final Config config = ConfigUtils.loadConfig(args[0]);
		final double cadytsScoringWeight = Double.parseDouble(args[1]) * config.planCalcScore().getBrainExpBeta();
		
		final Scenario scenario = prepareScenario(config, Boolean.parseBoolean(args[2]), Double.parseDouble(args[3]));
		
		final Controler controler = new Controler(scenario);
		controler.addOverridingModule(new CadytsCarModule());

		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Inject private CadytsContext cadytsContext;
			@Inject ScoringParametersForPerson parameters;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				
				final ScoringParameters params = parameters.getScoringParameters(person);
				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cadytsContext);
				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight);
				sumScoringFunction.addScoringFunction(scoringFunction);

				return sumScoringFunction;
			}
		});

		controler.run();
		
		if (args.length > 4) {
			runAnalyses(config, args[4]);
		}
		
	}
	
	private static Scenario prepareScenario(Config config, final boolean modifyNetwork, final double speedFactor) {
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		if (modifyNetwork) {
			for (Link link : scenario.getNetwork().getLinks().values()) {
				if (link.getFreespeed() < 70/3.6) {
					if (link.getCapacity() < 1000.) {
						link.setFreespeed(speedFactor * link.getFreespeed());
					}
				}
				if (link.getLength() < 100) {
					link.setCapacity(2. * link.getCapacity());
				}
			}
		}
		return scenario;
	}
	
	private static void runAnalyses(Config config, String planningAreaShapeFile) {
		String baseDirectory = config.controler().getOutputDirectory();
		String runId = config.controler().getRunId();
		String networkFile = baseDirectory  + "/" + runId + ".output_network.xml.gz"; // args[0];
		String eventsFile = baseDirectory  + "/" + runId + ".output_events.xml.gz"; // args[1];
		String usedIteration = Integer.toString(config.controler().getLastIteration()); // usedIteration = args[5];
		// onlySpecificMode = args[6]; onlyBerlinBased = args[7]; useDistanceFilter = args[8]
        String outputDirectory = baseDirectory + "/analysis";
		
		TripAnalyzerV2Extended.main(new String[]{networkFile, eventsFile, planningAreaShapeFile, outputDirectory, runId, usedIteration, "false", "false", "false"});
		TripAnalyzerV2Extended.main(new String[]{networkFile, eventsFile, planningAreaShapeFile, outputDirectory, runId, usedIteration, "false", "true", "true"});
		TripAnalyzerV2Extended.main(new String[]{networkFile, eventsFile, planningAreaShapeFile, outputDirectory, runId, usedIteration, "true", "false", "false"});
		TripAnalyzerV2Extended.main(new String[]{networkFile, eventsFile, planningAreaShapeFile, outputDirectory, runId, usedIteration, "true", "true", "true"});
		
		String plansInterval = Integer.toString(config.controler().getWritePlansInterval());
		
		SelectedPlansAnalyzer.main(new String[]{baseDirectory, runId, usedIteration, plansInterval, "false", "true"});
		// useInterimPlans = args[4]; useOutputPlans = args[5]
	}
}