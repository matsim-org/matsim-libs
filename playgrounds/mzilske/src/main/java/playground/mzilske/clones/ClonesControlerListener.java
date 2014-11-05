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

import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scoring.SumScoringFunction;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class ClonesControlerListener implements Provider<ControlerListener> {

    public static final String EMPTY_CLONE_PLAN = "emptyClonePlan";

    @Inject
    Scenario scenario;

    @Inject
    CloneService cloneService;

    @Override
    public ControlerListener get() {
        final double clonefactor = ConfigUtils.addOrGetModule(scenario.getConfig(), ClonesConfigGroup.NAME, ClonesConfigGroup.class).getCloneFactor();
        return new StartupListener() {
            @Override
            public void notifyStartup(StartupEvent event) {
                // clonefactor == 1 will leave everything as is, without stay-at-home-plans.
                if (clonefactor > 1) {
                    for (Person person : scenario.getPopulation().getPersons().values()) {
                        Plan plan2 = scenario.getPopulation().getFactory().createPlan();
                        plan2.setType(EMPTY_CLONE_PLAN);
                        person.addPlan(plan2);
                        person.setSelectedPlan(new RandomPlanSelector<Plan, Person>().selectPlan(person));
                    }
                    for (Person person : new ArrayList<>(scenario.getPopulation().getPersons().values())) {
                        for (int i = 0; i < clonefactor - 1; i++) {
                            Id<Person> personId = Id.createPersonId("I" + i + "_" + person.getId().toString());
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
                ExpBetaPlanSelector<Plan, Person> planSelector = new ExpBetaPlanSelector<>(scenario.getConfig().planCalcScore());
                for (Person person : scenario.getPopulation().getPersons().values()) {
                    List<Plan> plans = new ArrayList<>(person.getPlans());
                    for (Plan plan : plans) {
                        person.setSelectedPlan(plan);
                        SumScoringFunction.BasicScoring scoring = cloneService.createNewScoringFunction(person);
                        scoring.finish();
                        plan.setScore(scoring.getScore());
                    }
                    person.setSelectedPlan(planSelector.selectPlan(person));
                }
            }

        };
    }

}
