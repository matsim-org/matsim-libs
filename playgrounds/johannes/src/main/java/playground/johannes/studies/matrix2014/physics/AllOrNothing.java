/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.johannes.studies.matrix2014.physics;

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.util.ProgressLogger;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.data.io.PopulationIO;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

/**
 * @author johannes
 */
public class AllOrNothing {

    private static final Logger logger = Logger.getLogger(AllOrNothing.class);

    public static void main(String args[]) throws IOException {
        String personsFile = args[0];
        String volumesFile = args[1];

        logger.info("Loading persons...");
        Set<Person> persons = PopulationIO.loadFromXML(personsFile, new PlainFactory());

        logger.info("Calculating link volumes...");
        TObjectDoubleMap<String> volumes = new AllOrNothing().run(persons);

        logger.info("Writing volumes...");
        BufferedWriter writer = new BufferedWriter(new FileWriter(volumesFile));
        writer.write("id\tvolume");
        writer.newLine();

        TObjectDoubleIterator<String> it = volumes.iterator();
        for(int i = 0; i < volumes.size(); i++) {
            it.advance();
            writer.write(it.key());
            writer.write("\t");
            writer.write(String.valueOf(it.value()));
            writer.newLine();
        }
        writer.close();

        logger.info("Done.");
    }

    public TObjectDoubleMap<String> run(Set<Person> persons) {
        TObjectDoubleMap<String> volumes = new TObjectDoubleHashMap<>();

        ProgressLogger.init(persons.size(), 2, 10);

        for (Person p : persons) {
            if(!p.getEpisodes().isEmpty()) {
                double weight = Double.parseDouble(p.getAttribute(CommonKeys.PERSON_WEIGHT));
                Episode e = p.getEpisodes().get(0);

                for (Segment leg : e.getLegs()) {
                    String route = leg.getAttribute(CommonKeys.LEG_ROUTE);

                    if (route != null) {
                        String linkIds[] = route.split(" ");
                        for (String id : linkIds) {
                            volumes.adjustOrPutValue(id, weight, weight);
                        }
                    }
                }
            }

            ProgressLogger.step();
        }

        ProgressLogger.terminate();

        return volumes;
    }
}
