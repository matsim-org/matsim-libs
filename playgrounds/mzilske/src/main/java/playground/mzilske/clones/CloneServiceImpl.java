/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CloneServiceImpl.java
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

import com.google.inject.name.Named;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.SumScoringFunction;

import javax.inject.Inject;

class CloneServiceImpl implements CloneService {

    @Inject
    @Named("clonefactor")
    double clonefactor;

    @Override
    public Id<Person> resolveParentId(Id<Person> cloneId) {
        String id = cloneId.toString();
        String originalId;
        if (id.startsWith("I"))
            originalId = id.substring(id.indexOf("_") + 1);
        else
            originalId = id;
        return Id.create(originalId, Person.class);
    }

    @Override
    public SumScoringFunction.BasicScoring createNewScoringFunction(Person person) {
        return new CloneScoring(person);
    }

    private class CloneScoring implements SumScoringFunction.BasicScoring {
        private Person person;

        public CloneScoring(Person person) {
            this.person = person;
        }

        @Override
        public void finish() {}

        @Override
        public double getScore() {
            if (clonefactor > 1.0 && !ClonesControlerListener.EMPTY_CLONE_PLAN.equals(person.getSelectedPlan().getType())) {
                return scoreOffset();
            } else {
                return 0.0;
            }
        }

        private double scoreOffset() {
            return - Math.log( (clonefactor - 1.0) * (person.getPlans().size() - 1.0) );
        }

    }

}
