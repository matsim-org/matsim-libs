/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * SightingsPopulation.java
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

package playground.mzilske.sensors;

import org.matsim.api.core.v01.Scenario;
import playground.mzilske.cdr.PopulationFromSightings;
import playground.mzilske.cdr.Sightings;
import playground.mzilske.cdr.ZoneTracker;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SightingsPopulation {

    @Inject
    Sightings sightings;

    @Inject
    ZoneTracker.LinkToZoneResolver linkToZoneResolver;

    public void insertPersons(Scenario scenario) {
        PopulationFromSightings.createPopulationWithRandomRealization(scenario, sightings, linkToZoneResolver);
    }

}
