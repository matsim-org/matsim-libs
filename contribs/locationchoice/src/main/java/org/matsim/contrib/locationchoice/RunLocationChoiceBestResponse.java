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

package org.matsim.contrib.locationchoice;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DCScoringFunctionFactory;
import org.matsim.contrib.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunLocationChoiceBestResponse {

    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig(args[0], new DestinationChoiceConfigGroup());
        final Scenario scenario = ScenarioUtils.loadScenario(config);
        run(scenario);
    }

    public static void run(Scenario scenario) {
        DestinationChoiceBestResponseContext dcContext = new DestinationChoiceBestResponseContext(scenario);
        dcContext.init();
        DCScoringFunctionFactory dcScoringFunctionFactory = new DCScoringFunctionFactory(scenario, dcContext);
        DestinationChoiceConfigGroup dccg = (DestinationChoiceConfigGroup) dcContext.getScenario().getConfig().getModule(DestinationChoiceConfigGroup.GROUP_NAME);
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
