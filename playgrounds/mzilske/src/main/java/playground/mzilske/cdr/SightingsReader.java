/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * SightingsReader.java
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

package playground.mzilske.cdr;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SightingsReader {

    private Sightings sightings;

    public SightingsReader(Sightings sightings) {
        this.sightings = sightings;
    }

    public void read(InputStream in) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line = reader.readLine();
            while (line != null) {
                String tokens[] = line.split("\\s+");
                Id<Person> personId = Id.create(tokens[0], Person.class);
                List<Sighting> perPerson = sightings.getSightingsPerPerson().get(personId);
                if (perPerson == null) {
                    perPerson = new ArrayList<>();
                    sightings.getSightingsPerPerson().put(personId, perPerson);
                }
                long timeInSeconds = Long.parseLong(tokens[1]);
                String cellTowerId = tokens[2];
                Sighting sighting = new Sighting(personId, timeInSeconds, cellTowerId);
                perPerson.add(sighting);
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
