/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MetaPopulation.java
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

class MetaPopulation implements HasPlansAndId<MetaPopulationPlan, Person> {

    private final Id<Person> id;
    private List<MetaPopulationPlan> plans;
    private MetaPopulationPlan selectedPlan;
    private List<Person> currentPersons;
    private List<Person> templatePopulation;
    private Random random = new Random();
    private int nextId = 0;

    MetaPopulation(Collection<Person> templatePopulation, String id) {
        this.id = Id.create(id, Person.class);
        this.templatePopulation = new ArrayList<Person>(templatePopulation);
        plans = new ArrayList<MetaPopulationPlan>();
        currentPersons = new ArrayList<Person>(templatePopulation);
    }

    @Override
    public List<MetaPopulationPlan> getPlans() {
        return plans;
    }

    @Override
    public boolean addPlan(MetaPopulationPlan p) {
        plans.add(p);
        return true;
    }

    @Override
    public boolean removePlan(MetaPopulationPlan p) {
        plans.remove(p);
        return true;
    }

    @Override
    public MetaPopulationPlan getSelectedPlan() {
        return this.selectedPlan;
    }

    @Override
    public void setSelectedPlan(MetaPopulationPlan selectedPlan) {
        this.selectedPlan = selectedPlan;
    }

    @Override
    public MetaPopulationPlan createCopyOfSelectedPlanAndMakeSelected() {
        MetaPopulationPlan plan = new MetaPopulationPlan(selectedPlan.getScaleFactor());
        setSelectedPlan(plan);
        return plan;
    }

    @Override
    public Id<Person> getId() {
        return id;
    }

    public List<Person> getPersons() {
        int nAgents = (int) Math.round(selectedPlan.getScaleFactor() * templatePopulation.size());
        int deltaAgents = nAgents - currentPersons.size();
        if (deltaAgents > 0) {
            for (int i=0; i<deltaAgents; i++) {
                Person templatePerson = templatePopulation.get(random.nextInt(templatePopulation.size()));
                Person person = PersonImpl.createPerson(Id.create(id.toString() + "_" + nextId++, Person.class));
                for (Plan templatePlan : templatePerson.getPlans()) {
                    PlanImpl plan = new PlanImpl();
                    plan.copyFrom(templatePlan);
                    person.addPlan(plan);
                }
                currentPersons.add(person);
            }
        } else if (deltaAgents < 0) {
            for (int i = deltaAgents; i < 0; i++) {
                currentPersons.remove(random.nextInt(currentPersons.size()));
            }
        }
        return currentPersons;
    }

}
