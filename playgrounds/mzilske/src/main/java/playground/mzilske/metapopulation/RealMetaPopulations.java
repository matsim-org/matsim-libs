/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * RealMetaPopulations.java
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
class RealMetaPopulations implements MetaPopulations {

    private List<MetaPopulation> metaPopulations;

    @Inject
    RealMetaPopulations(Scenario scenario) {
        metaPopulations = new ArrayList<MetaPopulation>();
        List<Person> pop0 = new ArrayList<Person>();
        List<Person> pop1 = new ArrayList<Person>();
        for (Person person : scenario.getPopulation().getPersons().values()) {
            if (person.getId().toString().startsWith("7h")) {
                pop0.add(person);
            } else {
                pop1.add(person);
            }
        }
        {
            MetaPopulation metaPopulation = new MetaPopulation(pop0, "0");
            MetaPopulationPlan plan = new MetaPopulationPlan(1.0);
            metaPopulation.addPlan(plan);
            metaPopulation.addPlan(new MetaPopulationPlan(1.1));
            metaPopulation.setSelectedPlan(plan);
            metaPopulations.add(metaPopulation);
        }
        {
            MetaPopulation metaPopulation = new MetaPopulation(pop1, "1");
            MetaPopulationPlan plan = new MetaPopulationPlan(1.0);
            metaPopulation.addPlan(plan);
            metaPopulation.addPlan(new MetaPopulationPlan(1.1));
            metaPopulation.setSelectedPlan(plan);
            metaPopulations.add(metaPopulation);
        }
    }

    @Override
    public Iterable<? extends MetaPopulation> getMetaPopulations() {
        return metaPopulations;
    }

}
