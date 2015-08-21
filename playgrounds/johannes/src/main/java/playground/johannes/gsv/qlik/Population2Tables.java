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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import playground.johannes.gsv.matrices.episodes2matrix.SetZones;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.synpop.source.mid2008.processing.TaskRunner;
import playground.johannes.sna.util.ProgressLogger;
import playground.johannes.synpop.data.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

/**
 * @author johannes
 */
public class Population2Tables {

    private static final Logger logger = Logger.getLogger(Population2Tables.class);

    private static final String SEPARATOR = ",";

    public static void main(String[] args) throws IOException {
        String inPop = args[0];
        String outDir = args[1];

        XMLParser reader = new XMLParser();
        reader.setValidating(false);
        reader.parse(inPop);

        Collection<PlainPerson> persons = reader.getPersons();

        logger.info("Copying zone attributes...");
        TaskRunner.run(new CopyZoneAttributes(), persons);
        logger.info("Copying act type attributes...");
        TaskRunner.run(new CopyActTypeAttributes(), persons);

        logger.info("Writing tables...");

        BufferedWriter personWriter = new BufferedWriter(new FileWriter(String.format("%s/persons.csv", outDir)));
        BufferedWriter plansWriter = new BufferedWriter(new FileWriter(String.format("%s/episodes.csv", outDir)));
        BufferedWriter actsWriter = new BufferedWriter(new FileWriter(String.format("%s/activities.csv", outDir)));
        BufferedWriter legsWriter = new BufferedWriter(new FileWriter(String.format("%s/legs.csv", outDir)));

        String[] personKeys = new String[]{CommonKeys.PERSON_AGE, CommonKeys.PERSON_SEX, CommonKeys.HH_INCOME, CommonKeys
                .HH_MEMBERS, CommonKeys.PERSON_CARAVAIL};

        String[] planKeys = new String[] {"datasource"};

        String[] actKeys = new String[] {CommonKeys.ACTIVITY_TYPE, SetZones.ZONE_KEY, CommonKeys.ACTIVITY_START_TIME,
                CommonKeys.ACTIVITY_END_TIME};

        String[] legKeys = new String[] {CopyActTypeAttributes.PREV_ACT_TYPE, CopyActTypeAttributes.NEXT_ACT_TYPE,
                CopyZoneAttributes.FROM_ZONE_KEY, CopyZoneAttributes.TO_ZONE_KEY, CommonKeys.LEG_START_TIME,
                CommonKeys.LEG_END_TIME, CommonKeys.LEG_MODE, CommonKeys.LEG_GEO_DISTANCE, CommonKeys.LEG_ROUTE_DISTANCE};

        personWriter.write("personId,");
        personWriter.write(StringUtils.join(personKeys, SEPARATOR));
        personWriter.newLine();

        plansWriter.write("personId,");
        plansWriter.write(StringUtils.join(planKeys, SEPARATOR));
        plansWriter.newLine();

        actsWriter.write("personId,");
        actsWriter.write(StringUtils.join(actKeys, SEPARATOR));
        actsWriter.newLine();

        legsWriter.write("personId,legId,");
        legsWriter.write(StringUtils.join(legKeys, SEPARATOR));
        legsWriter.newLine();

        ProgressLogger.init(persons.size(), 2, 10);

        int legCounter = 0;
        for(Person person : persons) {
            personWriter.write(person.getId());
            personWriter.write(SEPARATOR);
            writeAttributes(personWriter, person, personKeys);

            for(Episode episode : person.getEpisodes()) {
                plansWriter.write(episode.getPerson().getId());
                plansWriter.write(SEPARATOR);
                writeAttributes(plansWriter, episode, planKeys);

                for(Segment act : episode.getActivities()) {
                    actsWriter.write(act.getEpisode().getPerson().getId());
                    actsWriter.write(SEPARATOR);
                    writeAttributes(actsWriter, act, actKeys);
                }

                for(Segment leg : episode.getLegs()) {
                    legsWriter.write(leg.getEpisode().getPerson().getId());
                    legsWriter.write(SEPARATOR);
                    legsWriter.write(String.valueOf(legCounter++));
                    legsWriter.write(SEPARATOR);
                    writeAttributes(legsWriter, leg, legKeys);
                }
            }
            ProgressLogger.step();
        }

        personWriter.close();
        plansWriter.close();
        actsWriter.close();
        legsWriter.close();

        logger.info("Done.");
    }

    private static void writeAttributes(BufferedWriter writer, Attributable attrs, String[] keys) throws IOException {
        for(String key : keys) {
            writer.write(getAttribute(key, attrs));
            writer.write(SEPARATOR);
        }
        writer.newLine();
    }

    private static String getAttribute(String key, Attributable attrs) {
        String val = attrs.getAttribute(key);
        if(val == null) return "";
        else return val;
    }
}
