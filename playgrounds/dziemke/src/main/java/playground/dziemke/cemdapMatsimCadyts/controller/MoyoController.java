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

import javax.inject.Inject;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.cadyts.pt.CadytsPtContext;
import org.matsim.contrib.cadyts.pt.CadytsPtModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author dziemke
 */
public class MoyoController {
	
	public static void main(String[] args) {
		final Config config = ConfigUtils.loadConfig("../../../shared-svn/projects/ptManuel/calibration/my_config_dz.xml", new CadytsConfigGroup());
		
		config.transit().setUseTransit(true);
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		{
			StrategySettings strategySettings = new StrategySettings();
			strategySettings.setStrategyName("ChangeExpBeta");
			strategySettings.setWeight(1.0);
			config.strategy().addStrategySettings(strategySettings);
		}{
			StrategySettings strategySettings = new StrategySettings();
			strategySettings.setStrategyName("ReRoute");
			strategySettings.setWeight(0.5);
//			strategySettings.setDisableAfter(90);
			config.strategy().addStrategySettings(strategySettings);
		}
	
		
		final Controler controler = new Controler(config);	
//		controler.addOverridingModule(new CadytsCarModule());
		controler.addOverridingModule(new CadytsPtModule());
//		controler.getConfig().getModule("cadytsCar").addParam("startTime", "00:00:00"); // TODO reactivate
//		controler.getConfig().getModule("cadytsCar").addParam("endTime", "24:00:00");


		/* Add Cadyts component to scoring function */
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
//			@Inject private CadytsContext cadytsContext;
			@Inject private CadytsPtContext cadytsContext;
			@Inject CharyparNagelScoringParametersForPerson parameters;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final CharyparNagelScoringParameters params = parameters.getScoringParameters(person);

				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

//				final CadytsScoring<Link> scoringFunction = new CadytsScoring<Link>(person.getSelectedPlan(), config, cadytsContext);
				final CadytsScoring<TransitStopFacility> scoringFunction = new CadytsScoring<TransitStopFacility>(person.getSelectedPlan(), config, cadytsContext);
				final double cadytsScoringWeight = 15.0 * config.planCalcScore().getBrainExpBeta();
				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight);
				sumScoringFunction.addScoringFunction(scoringFunction);

				return sumScoringFunction;
			}
		});

		controler.run();
	}
}