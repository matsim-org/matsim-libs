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

package playground.mzilske.sensors;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import playground.mzilske.cdr.Sightings;
import playground.mzilske.cdr.SightingsImpl;
import playground.mzilske.d4d.Sighting;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.*;

class ODSightings implements Provider<Sightings> {

    @Inject
    Matrix matrix;

    long startTime = 9 * 60 * 60;
    long endTime = 10 * 60 * 60;

    @Override
    public Sightings get() {
        Map<Id, List<Sighting>> sightings = new HashMap<Id, List<Sighting>>();
        for (Collection<Entry> entries : matrix.getFromLocations().values()) {
            for (Entry entry : entries) {
                for (int i = 0; i < entry.getValue(); i++) {
                    Id personId = personId(entry, i);
                    sightings.put(personId, Arrays.asList(
                                    new Sighting(personId, startTime, entry.getFromLocation().toString()),
                                    new Sighting(personId, endTime, entry.getToLocation().toString()))
                    );
                }
            }
        }
        return new SightingsImpl(sightings);
    }

    private Id personId(Entry entry, int i) {
        return new IdImpl(entry.getFromLocation().toString() + "_" + entry.getToLocation().toString() + "_" +i);
    }

}
