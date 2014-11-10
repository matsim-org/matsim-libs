/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * TrajectoryReRealizer.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;
import playground.mzilske.clones.CloneService;

class TrajectoryReRealizer extends AbstractMultithreadedModule {

    private Sightings sightings;
    private Scenario scenario;
    private ZoneTracker.LinkToZoneResolver zones;
    private CloneService cloneService;

    TrajectoryReRealizer(Scenario scenario, Sightings sightings, ZoneTracker.LinkToZoneResolver zones, CloneService cloneService) {
        super(scenario.getConfig().global());
        this.sightings = sightings;
        this.scenario = scenario;
        this.zones = zones;
        this.cloneService = cloneService;
    }

    @Override
    public PlanAlgorithm getPlanAlgoInstance() {
        return new PlanAlgorithm() {
            @Override
            public void run(Plan plan) {
                Id personId = plan.getPerson().getId();
                Id originalPersonId = cloneService.resolveParentId(personId);
                Plan newPlan = PopulationFromSightings.createPlanWithRandomEndTimesInPermittedWindow(scenario, zones, sightings.getSightingsPerPerson().get(originalPersonId));
                plan.getPlanElements().clear();
                ((PlanImpl) plan).copyFrom(newPlan);
                for (PlanElement pe : plan.getPlanElements()) {
                    if (pe instanceof Leg) {
                        ((Leg) pe).setMode("car");
                    }
                }
            }
        };
    }

}
