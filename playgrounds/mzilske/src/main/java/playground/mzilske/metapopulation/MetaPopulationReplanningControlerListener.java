/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MetaPopulationReplanningControlerListener.java
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

package playground.mzilske.metapopulation;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;

class MetaPopulationReplanningControlerListener implements ReplanningListener {

    private final GenericStrategyManager<MetaPopulationPlan, Person> strategyManager;
    private MetaPopulations metaPopulations;

    private Scenario scenario;

    @Inject
    MetaPopulationReplanningControlerListener(MetaPopulations metaPopulations, Scenario scenario) {
        this.metaPopulations = metaPopulations;
        this.scenario = scenario;
        this.strategyManager = new GenericStrategyManager<MetaPopulationPlan, Person>();
        this.strategyManager.setMaxPlansPerAgent(5);
        final ExpBetaPlanSelector<MetaPopulationPlan, Person> metaPopulationPlanExpBetaPlanSelector = new ExpBetaPlanSelector<MetaPopulationPlan, Person>(1.0);
        GenericPlanStrategyImpl<MetaPopulationPlan, Person> select = new GenericPlanStrategyImpl<MetaPopulationPlan, Person>(metaPopulationPlanExpBetaPlanSelector);
        this.strategyManager.addStrategy(select, null, 0.5);
        GenericPlanStrategy<MetaPopulationPlan, Person> replan = new GenericPlanStrategy<MetaPopulationPlan, Person>() {
            ReplanningContext replanningContext;

            @Override
            public void run(HasPlansAndId<MetaPopulationPlan, Person> metaPopulation) {
                MetaPopulationPlan selectedPlan = metaPopulation.getSelectedPlan();
                Person person = PopulationUtils.createPerson(Id.create("wurst", Person.class));
                PlanImpl p0 = new PlanImpl();
                p0.setScore(0.0);
                person.addPlan(p0);
                PlanImpl p1 = new PlanImpl();
                p1.setScore(selectedPlan.getScore());
                person.addPlan(p1);

                double newScaleFactor = selectedPlan.getScaleFactor() * (ExpBetaPlanSelector.getSelectionProbability(new ExpBetaPlanSelector<Plan, Person>(1.0), person, p1) / 0.5);
                if (replanningContext.getIteration() < 25) {
                    newScaleFactor += Math.random() * 0.1 - 0.05;
                }

                MetaPopulationPlan newPlan = new MetaPopulationPlan(newScaleFactor);
                metaPopulation.addPlan(newPlan);
                metaPopulation.setSelectedPlan(newPlan);
            }

            @Override
            public void init(ReplanningContext replanningContext) {
                this.replanningContext = replanningContext;
            }

            @Override
            public void finish() {}
        };
        this.strategyManager.addStrategy(replan, null, 0.5);
    }

    @Override
    public void notifyReplanning(ReplanningEvent event) {
        strategyManager.run(metaPopulations.getMetaPopulations(), null, event.getIteration(), event.getReplanningContext());
        scenario.getPopulation().getPersons().clear();
        for (MetaPopulation metaPopulation : metaPopulations.getMetaPopulations()) {
            for (Person person : metaPopulation.getPersons()) {
                scenario.getPopulation().addPerson(person);
            }
        }
    }

}
