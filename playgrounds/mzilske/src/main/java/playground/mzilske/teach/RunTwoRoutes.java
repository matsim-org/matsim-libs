/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * RunTwoRoutes.java
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

package playground.mzilske.teach;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

public class RunTwoRoutes {

    public static void main(String[] args) {
        new RunTwoRoutes().run();
    }

    void run() {
        Config config = ConfigUtils.createConfig();
        config.controler().setLastIteration(100);
        {
            StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings(Id.create("1", StrategyConfigGroup.StrategySettings.class));
            stratSets.setStrategyName("BestScore");
//            stratSets.setStrategyName("SelectExpBeta");
            // Both work. Both need SelectRandom to work.
            stratSets.setWeight(1.0);
            config.strategy().addStrategySettings(stratSets);
        }
        {
            StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings(Id.create("2", StrategyConfigGroup.StrategySettings.class));
            stratSets.setStrategyName("SelectRandom");
            stratSets.setWeight(0.1);
            stratSets.setDisableAfter(90);
            config.strategy().addStrategySettings(stratSets);
        }
        final Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).parse(this.getClass().getResourceAsStream("two-routes.xml"));
        new MatsimPopulationReader(scenario).parse(this.getClass().getResourceAsStream("thirty.xml"));
        Controler controler = new Controler(scenario);
        controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
            @Override
            public ScoringFunction createNewScoringFunction(Person person) {
                SumScoringFunction sumScoringFunction = new SumScoringFunction();
                sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(new CharyparNagelScoringParameters.Builder(scenario.getConfig().planCalcScore(), scenario.getConfig().planCalcScore().getScoringParameters(null), scenario.getConfig().scenario()).build(), scenario.getNetwork()));
                return sumScoringFunction;
            }
        });
        controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
        controler.run();
    }

}
