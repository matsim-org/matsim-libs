/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MyCallBehavior.java
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

package playground.mzilske.stratum;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import playground.mzilske.cdr.CallBehavior;

import javax.inject.Inject;

class MyCallBehavior implements CallBehavior {

    @Inject
    Scenario scenario;

    @Override
    public boolean makeACall(ActivityEndEvent event) {
        Plan plan = scenario.getPopulation().getPersons().get(event.getPersonId()).getSelectedPlan();
        if (event.getActType().equals("home") || plan.getCustomAttributes().get("prop").equals(0)) {
            return true;
        } else if (plan.getCustomAttributes().get("prop").equals(2)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean makeACall(ActivityStartEvent event) {
        Plan plan = scenario.getPopulation().getPersons().get(event.getPersonId()).getSelectedPlan();
        if (event.getActType().equals("home") || plan.getCustomAttributes().get("prop").equals(0)) {
            return true;
        } else if (plan.getCustomAttributes().get("prop").equals(2)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean makeACall(Id id, double time) {
        return false;
    }

    @Override
    public boolean makeACallAtMorningAndNight(Id<Person> id) {
        return true;
    }

}
