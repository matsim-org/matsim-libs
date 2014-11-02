/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ODSightings.java
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

package playground.mzilske.matrices;

import java.util.Arrays;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;

import playground.mzilske.cdr.Sighting;
import playground.mzilske.cdr.Sightings;

class MatricesToSightings {

    static void insertSightingsForMatrices(TimedMatrices timedMatrices, Sightings sightings) {
        for (TimedMatrix timedMatrix : timedMatrices.getMatrices()) {
            Matrix matrix = timedMatrix.getMatrix();
            double startTime = timedMatrix.getStartTime();
            double endTime = timedMatrix.getEndTime();
            for (Collection<Entry> entries : matrix.getFromLocations().values()) {
                for (Entry entry : entries) {
                    for (int i = 0; i < entry.getValue(); i++) {
                        Id<Person> personId = personId(entry, i);
                        sightings.getSightingsPerPerson().put(personId, Arrays.asList(
                                        new Sighting(personId, (long) startTime, entry.getFromLocation().toString()),
                                        new Sighting(personId, (long) endTime, entry.getToLocation().toString()))
                        );
                    }
                }
            }
        }
    }

    static Id<Person> personId(Entry entry, int i) {
        return Id.create(entry.getFromLocation().toString() + "_" + entry.getToLocation().toString() + "_" +i, Person.class);
    }

}
