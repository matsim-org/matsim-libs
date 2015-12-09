/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * TrajectoryReRealizerModules.java
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

package playground.mzilske.cdr;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import playground.mzilske.clones.CloneService;

import javax.inject.Inject;
import javax.inject.Provider;

public class TrajectoryReRealizerModule extends AbstractModule {

    @Override
    public void install() {
        addPlanStrategyBinding("ReRealize").toProvider(TrajectoryReRealizerProvider.class);
    }

    static class TrajectoryReRealizerProvider implements Provider<PlanStrategy> {
        private Scenario scenario;
        private Sightings sightings;
        private ZoneTracker.LinkToZoneResolver zones;
        private CloneService cloneService;
        private Provider<TripRouter> tripRouterProvider;

        @Inject
        TrajectoryReRealizerProvider(Scenario scenario, Sightings sightings, ZoneTracker.LinkToZoneResolver zones, CloneService cloneService, Provider<TripRouter> tripRouterProvider) {
            this.scenario = scenario;
            this.sightings = sightings;
            this.zones = zones;
            this.cloneService = cloneService;
            this.tripRouterProvider = tripRouterProvider;
        }

        @Override
        public PlanStrategy get() {
            PlanStrategyImpl planStrategy = new PlanStrategyImpl(new RandomPlanSelector<Plan, Person>());
            planStrategy.addStrategyModule(new TrajectoryReRealizer(scenario, sightings, zones, cloneService));
            planStrategy.addStrategyModule(new ReRoute(scenario, tripRouterProvider));
            return planStrategy;
        }
    }
}
