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

import org.apache.log4j.Logger;
import org.matsim.contrib.common.util.ProgressLogger;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainFactory;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.data.io.XMLHandler;
import playground.johannes.synpop.data.io.XMLWriter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 */
public class CopyMiDAttributes {

    private static final Logger logger = Logger.getLogger(CopyMiDAttributes.class);

    public static void main(String[] args) {
        String simIn = args[0];
        String midIn = args[1];
        String simOut = args[2];

        XMLHandler reader = new XMLHandler(new PlainFactory());
        reader.setValidating(false);

        logger.info("Loading sim persons...");
        reader.parse(simIn);
        Collection<PlainPerson> simPersons = (Set<PlainPerson>)reader.getPersons();

        logger.info("Loading mid persons...");
        reader.parse(midIn);
        Collection<PlainPerson> tmpPerson = (Set<PlainPerson>)reader.getPersons();

        Map<String, Person> midPersons = new HashMap<>();
        for(Person person : tmpPerson) {
            midPersons.put(person.getId(), person);
        }

        logger.info("Copying attributes...");
        ProgressLogger.init(simPersons.size(), 10, 2);
        int notFound = 0;
        for(Person simPerson : simPersons) {
            String simId = simPerson.getId();
            int idx = simId.indexOf("clone");
            if(idx > 0) {
                String midId = simId.substring(0, idx);

                Person midPerson = midPersons.get(midId);

                if (midPerson != null) {
                    simPerson.setAttribute(CommonKeys.PERSON_SEX, midPerson.getAttribute(CommonKeys.PERSON_SEX));
                    simPerson.setAttribute(CommonKeys.PERSON_AGE, midPerson.getAttribute(CommonKeys.PERSON_AGE));
                    simPerson.setAttribute(CommonKeys.HH_INCOME, midPerson.getAttribute(CommonKeys.HH_INCOME));
                    simPerson.setAttribute(CommonKeys.HH_MEMBERS, midPerson.getAttribute(CommonKeys.HH_MEMBERS));
                    simPerson.setAttribute(CommonKeys.PERSON_CARAVAIL, midPerson.getAttribute(CommonKeys.PERSON_CARAVAIL));
                } else {
                    notFound++;
                }
            }

            ProgressLogger.step();
        }
        ProgressLogger.terminate();

        if(notFound > 0) {
            logger.info(String.format("Could not find templates for %s persons.", notFound));
        }

        logger.info("Writing persons...");
        XMLWriter writer = new XMLWriter();
        writer.write(simOut, simPersons);
        logger.info("Done.");
    }
}
