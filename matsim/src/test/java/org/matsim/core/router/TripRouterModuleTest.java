/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * LeastCostPathCalculatorModuleTest.java
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

package org.matsim.core.router;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.testcases.MatsimTestUtils;

import junit.framework.Assert;

public class TripRouterModuleTest {

    @Rule
    public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

    @Test
    public void testRouterCreation() {
        for (ControlerConfigGroup.RoutingAlgorithmType routingAlgorithmType : ControlerConfigGroup.RoutingAlgorithmType.values()) {
            Config config = ConfigUtils.createConfig();
            config.controler().setRoutingAlgorithmType(routingAlgorithmType);
            Scenario scenario = ScenarioUtils.createScenario(config);
            LeastCostPathCalculatorFactory defaultLeastCostPathCalculatorFactory = TripRouterFactoryBuilderWithDefaults.createDefaultLeastCostPathCalculatorFactory(scenario);
            LeastCostPathCalculator pathCalculator = defaultLeastCostPathCalculatorFactory.createPathCalculator(
                    scenario.getNetwork(), 
                    ControlerDefaults.createDefaultTravelDisutilityFactory(scenario).createTravelDisutility(new FreeSpeedTravelTime()),
                    new FreeSpeedTravelTime());
            Assert.assertNotNull(pathCalculator);
        }
    }

}
