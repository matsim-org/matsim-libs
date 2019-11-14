/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * PlanRouterTest.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

public class PlanRouterTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void test0() {
        final Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
        config.plans().setInputFile("plans1.xml");
        
        final Scenario scenario = ScenarioUtils.loadScenario(config);
        Plan plan = scenario.getPopulation().getPersons().get(Id.createPersonId(1)).getSelectedPlan();
        for (Trip trip: TripStructureUtils.getTrips(plan)) {
        	for (Leg leg : trip.getLegsOnly()) {
        		if (leg.getMode().equals(TransportMode.car)) {
        			leg.setRoute(null);
        		}
        	}
        }
        
        com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(), new AbstractModule() {
            @Override
            public void install() {
                install(new TripRouterModule());
                install(new ScenarioByInstanceModule(scenario));
                addTravelTimeBinding("car").toInstance(new FreespeedTravelTimeAndDisutility(config.planCalcScore()));
                addTravelDisutilityFactoryBinding("car").toInstance(new OnlyTimeDependentTravelDisutilityFactory());
            }
        });
        TripRouter tripRouter = injector.getInstance(TripRouter.class);
		
		PlanRouter planRouter = new PlanRouter(tripRouter);     
		planRouter.run(plan);
		
		int tripsWithRouteAfterPlanRouter = 0;
        for (Trip trip: TripStructureUtils.getTrips(plan)) {
        	for (Leg leg : trip.getLegsOnly()) {
        		if (leg.getMode().equals(TransportMode.car) && leg.getRoute() != null) {
        			tripsWithRouteAfterPlanRouter++;
        		}
        	}
        }
        
		Assert.assertEquals("Wrong number of car routes.", 3, tripsWithRouteAfterPlanRouter);
    }

}