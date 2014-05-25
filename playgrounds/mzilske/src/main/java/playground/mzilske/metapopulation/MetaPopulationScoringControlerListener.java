/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MetaPopulationScoringControlerListener.java
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

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.ScoringListener;

import javax.inject.Inject;

class MetaPopulationScoringControlerListener implements ScoringListener {

    @Inject
    MetaPopulations metaPopulations;

    @Override
    public void notifyScoring(ScoringEvent event) {
        for (MetaPopulation metaPopulation : metaPopulations.getMetaPopulations()) {
            double score = 0.0;
            for (Person person : metaPopulation.getPersons()) {
                score += person.getSelectedPlan().getScore();
            }
            MetaPopulationPlan plan = metaPopulation.getSelectedPlan();
            plan.setScore(score / metaPopulation.getPersons().size());
        }
    }

}
