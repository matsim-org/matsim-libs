/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.johannes.gsv.qlik;

import playground.johannes.gsv.matrices.episodes2matrix.SetZones;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.io.KeyMatrixTxtIO;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.data.Segment;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 * @author johannes
 */
public class ODTabelGenerator {

    public static void main(String args[]) throws IOException {
        XMLParser reader = new XMLParser();
        reader.setValidating(false);
        reader.parse(args[0]);

        Collection<PlainPerson> persons = reader.getPersons();

        KeyMatrix m = new KeyMatrix();

        for(Person person : persons) {
            for(Episode episode : person.getEpisodes()) {
                for(Segment leg : episode.getLegs()) {
//                    String i = leg.getAttribute(CopyZoneAttributes.FROM_ZONE_KEY);
//                    String j = leg.getAttribute(CopyZoneAttributes.TO_ZONE_KEY);
                    String i = leg.previous().getAttribute(SetZones.ZONE_KEY);
                    String j = leg.next().getAttribute(SetZones.ZONE_KEY);
                    if(i != null && j != null) {
                        m.add(i, j, 1);
                    }
                }
            }
        }

        int counter = 0;
        BufferedWriter writer = new BufferedWriter(new FileWriter(args[1]));
        writer.write("id,from,to");
        writer.newLine();
        Set<String> keys = m.keys();
        for(String i : keys) {
            for(String j : keys) {
                if(m.get(i, j) != null) {
                    writer.write(String.valueOf(counter));
                    writer.write(",");
                    writer.write(i);
                    writer.write(",");
                    writer.write(j);
                    writer.newLine();
                    counter++;
                }
            }
        }
    }
}
