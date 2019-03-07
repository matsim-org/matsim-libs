/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * RunLocationChoiceBestResponse.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.contrib.locationchoice.zzunused;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceContext;
import org.matsim.contrib.locationchoice.zzunused.DestinationChoiceInitializer;
import org.matsim.contrib.locationchoice.bestresponse.DCScoringFunctionFactory;
import org.matsim.contrib.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

@Deprecated // (I think)
class RunLocationChoiceBestResponse {

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0], new DestinationChoiceConfigGroup() );
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		run(scenario);
	}

	public static void run(Scenario scenario) {


		DestinationChoiceContext dcContext = new DestinationChoiceContext(scenario);

		DCScoringFunctionFactory dcScoringFunctionFactory = new DCScoringFunctionFactory(scenario, dcContext);
		DestinationChoiceConfigGroup dccg = ConfigUtils.addOrGetModule( dcContext.getScenario().getConfig(), DestinationChoiceConfigGroup.class);
		if (dccg.getPrefsFile() == null && !scenario.getConfig().facilities().getInputFile().equals("null")) {
			dcScoringFunctionFactory.setUsingConfigParamsForScoring(false);
		} else {
			dcScoringFunctionFactory.setUsingConfigParamsForScoring(true);
		}

		Controler controler = new Controler(scenario);
		controler.addControlerListener(new DestinationChoiceInitializer(dcContext));
		if (dccg.getRestraintFcnExp() > 0.0 && dccg.getRestraintFcnFactor() > 0.0) {
			controler.addControlerListener(new FacilitiesLoadCalculator(dcContext.getFacilityPenalties()));
		}
		controler.setScoringFunctionFactory(dcScoringFunctionFactory);
		controler.run();
	}

}
