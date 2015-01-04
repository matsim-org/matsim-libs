/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Main.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
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

package playground.mzilske.extendtraces;

import org.matsim.api.core.v01.Id;
import playground.mzilske.cdr.Sighting;
import playground.mzilske.cdr.Sightings;
import playground.mzilske.populationsize.ExperimentResource;
import playground.mzilske.populationsize.RegimeResource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        final ExperimentResource experiment = new ExperimentResource("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/");
        final RegimeResource congested = experiment.getRegime("uncongested3");
        Sightings sightings = congested.getMultiRateRun("randomcountlocations100.0").getSightings("5");
        final Map<Id, List<Sighting>> map = new HashMap<>();
        int i=0;
        for (Map.Entry<Id, List<Sighting>> entry : sightings.getSightingsPerPerson().entrySet()) {
            map.put(entry.getKey(), entry.getValue());
            i++;
            if (i>2000) break;
        }
        sightings = new Sightings() {
            @Override
            public Map<Id, List<Sighting>> getSightingsPerPerson() {
                return map;
            }
        };
        for (double epsilon : Arrays.asList(600.0)) {
            System.out.printf("epsilon: %d seconds\n", (int) epsilon);
            new TraceQuery(sightings, epsilon, 0.1).query();
        }
    }

}
