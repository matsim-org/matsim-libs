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

package playground.johannes.synpop.data.io;

import org.apache.log4j.Logger;
import playground.johannes.synpop.data.Factory;
import playground.johannes.synpop.data.Person;

import java.util.Collection;
import java.util.Set;

/**
 * @author johannes
 */
public class PopulationIO {

    private static final Logger logger = Logger.getLogger(PopulationIO.class);

    public static Set<? extends Person> loadFromXML(String file, Factory factory) {
        XMLHandler parser = new XMLHandler(factory);
        parser.setValidating(false);
        parser.parse(file);

        Set<? extends Person> persons = parser.getPersons();
        logger.info(String.format("Loaded %s persons.", persons.size()));
        return persons;
    }

    public static void writeToXML(String file, Collection<? extends Person> population) {
        XMLWriter writer = new XMLWriter();
        writer.write(file, population);
    }
}
