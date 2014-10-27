/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CountWokers.java
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

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

class CountWorkers {

    public static void main(String[] args) {
        final ExperimentResource experiment = new ExperimentResource("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/");
        final RegimeResource uncongested = experiment.getRegime("uncongested");
        RunResource baseRun = uncongested.getBaseRun();
        Population population = baseRun.getOutputScenario().getPopulation();
        int nWorkers = 0;
        for (Person person : population.getPersons().values()) {
            if (isWorker(person)) nWorkers++;
        }
        System.out.printf("Workers: %d, non-workers: %d", nWorkers, population.getPersons().size() - nWorkers);
    }

    static boolean isWorker(Person person) {
        for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
            if (pe instanceof Activity) {
                if (((Activity) pe).getType().equals("work")) {
                    return true;
                }
            }
        }
        return false;
    }

}
