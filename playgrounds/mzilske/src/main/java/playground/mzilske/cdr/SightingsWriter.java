/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * SightingsWriter.java
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
import playground.mzilske.ant2014.FileIO;
import playground.mzilske.ant2014.StreamingOutput;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class SightingsWriter {
    private final Sightings sightings;

    public SightingsWriter(Sightings allSightings) {
        this.sightings = allSightings;
    }

    public void write(String filename) {
        FileIO.writeToFile(filename, new StreamingOutput() {
            @Override
            public void write(PrintWriter pw) throws IOException {
                for (Map.Entry<Id, List<Sighting>> entry : sightings.getSightingsPerPerson().entrySet()) {
                    for (Sighting sighting : entry.getValue()) {
                        pw.printf("%s\t%d\t%s\n", entry.getKey().toString(), (long) sighting.getTime(), sighting.getCellTowerId());
                    }
                }
            }
        });
    }

}
