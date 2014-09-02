/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ClonesControlerListener.java
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

package playground.mzilske.clones;

import java.util.ArrayList;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import com.google.inject.Provider;

class ClonesControlerListener implements Provider<ControlerListener> {

    @Inject @com.google.inject.name.Named("clonefactor")
    double clonefactor;

    @Inject
    Scenario scenario;

    @Override
    public ControlerListener get() {
        return new StartupListener() {
            @Override
            public void notifyStartup(StartupEvent event) {
                // clonefactor == 1 will leave everything as is, without stay-at-home-plans.
                if (clonefactor > 1) {
                    for (Person person : scenario.getPopulation().getPersons().values()) {
                        Plan plan2 = scenario.getPopulation().getFactory().createPlan();
                        plan2.setType("emptyClonePlan");
                        person.addPlan(plan2);
                        person.setSelectedPlan(new RandomPlanSelector<Plan, Person>().selectPlan(person));
                    }
                    for (Person person : new ArrayList<Person>(scenario.getPopulation().getPersons().values())) {
                        for (int i = 0; i < clonefactor - 1; i++) {
                            Id personId = new IdImpl("I" + i + "_" + person.getId().toString());
                            Person clone = scenario.getPopulation().getFactory().createPerson(personId);
                            for (Plan plan : person.getPlans()) {
                                Plan clonePlan = scenario.getPopulation().getFactory().createPlan();
                                ((PlanImpl) clonePlan).copyFrom(plan);
                                clone.addPlan(clonePlan);
                            }
                            clone.setSelectedPlan(new RandomPlanSelector<Plan, Person>().selectPlan(clone));
                            scenario.getPopulation().addPerson(clone);
                        }
                    }
                }
            }

        };
    }

}
