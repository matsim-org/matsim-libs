/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * RunLocationChoice.java
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
import org.matsim.contrib.locationchoice.bestresponse.preprocess.MaxDCScoreWrapper;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.ReadOrComputeMaxDCScore;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DCScoringFunctionFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class RunLocationChoiceFrozenEpsilons {

    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig(args[0]);
        final Scenario scenario = ScenarioUtils.loadScenario(config);

        final DestinationChoiceBestResponseContext lcContext = new DestinationChoiceBestResponseContext(scenario) ;
        lcContext.init();
        scenario.addScenarioElement(DestinationChoiceBestResponseContext.ELEMENT_NAME, lcContext);

        ReadOrComputeMaxDCScore computer = new ReadOrComputeMaxDCScore(lcContext);
        computer.readOrCreateMaxDCScore(config, lcContext.kValsAreRead());

        MaxDCScoreWrapper dcScore = new MaxDCScoreWrapper();
        final ObjectAttributes personsMaxDCScoreUnscaled = computer.getPersonsMaxEpsUnscaled();
        dcScore.setPersonsMaxDCScoreUnscaled(personsMaxDCScoreUnscaled);
        scenario.addScenarioElement(MaxDCScoreWrapper.ELEMENT_NAME, dcScore);

        DCScoringFunctionFactory scoringFunctionFactory = new DCScoringFunctionFactory(scenario, lcContext);
        scoringFunctionFactory.setUsingConfigParamsForScoring(true) ;

        Controler controler = new Controler(scenario);
        controler.setScoringFunctionFactory(scoringFunctionFactory);
        controler.addPlanStrategyFactory("MyLocationChoice", new PlanStrategyFactory(){
            @Override
            public PlanStrategy createPlanStrategy(Scenario scenario, EventsManager eventsManager) {
                return new BestReplyLocationChoicePlanStrategy(scenario) ;
            }
        });
        controler.run();
    }

}
