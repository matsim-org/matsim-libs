
/* *********************************************************************** *
 * project: org.matsim.*
 * ExperiencedPlansServiceImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.scoring;

import org.matsim.api.core.v01.Scenario;

public class ExperiencedPlansServiceFactory {

    /*
     This should only be needed in Postprocessing. The way to access Experienced Plans during a simulation is via dependency injection.
     */
    public static ExperiencedPlansService create(Scenario scenario, EventsToActivities eventsToActivities, EventsToLegs eventsToLegs) {
        return new ExperiencedPlansServiceImpl(eventsToActivities, eventsToLegs, scenario);

    }
}
