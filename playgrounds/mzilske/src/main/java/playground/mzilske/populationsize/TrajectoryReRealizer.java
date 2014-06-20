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

package playground.mzilske.populationsize;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.ReplanningContext;
import playground.mzilske.cdr.PopulationFromSightings;
import playground.mzilske.cdr.ZoneTracker;
import playground.mzilske.d4d.Sighting;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

class TrajectoryReRealizer implements PlanStrategyModule {

    private final Map<Id, List<Sighting>> sightings;
    private Scenario scenario;
    private ZoneTracker.LinkToZoneResolver zones;

    @Inject
    public TrajectoryReRealizer(Map<Id, List<Sighting>> sightings, Scenario scenario, ZoneTracker.LinkToZoneResolver zones) {
        this.sightings = sightings;
        this.scenario = scenario;
        this.zones = zones;
    }

    @Override
    public void prepareReplanning(ReplanningContext replanningContext) {

    }

    @Override
    public void handlePlan(Plan plan) {
        Id personId = plan.getPerson().getId();
        Plan newPlan = PopulationFromSightings.createPlanWithRandomEndTimesInPermittedWindow(scenario, zones, sightings.get(personId));
        plan.getPlanElements().clear();
        ((PlanImpl) plan).copyFrom(newPlan);
    }

    @Override
    public void finishReplanning() {

    }

}
