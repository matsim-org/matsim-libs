/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.mid.run;

import org.apache.log4j.Logger;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.synpop.data.PlainPerson;

import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class ExtractMidJourneys {

	private static final Logger logger = Logger.getLogger(ExtractMidJourneys.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
	
		logger.info("Loading persons...");
		parser.parse(args[0]);
		Set<PlainPerson> persons = parser.getPersons();
		logger.info(String.format("Loaded %s persons.", persons.size()));
		
		Set<PlainPerson> newPersons = new HashSet<>();
		for(PlainPerson person : persons) {
			if("midjourneys".equalsIgnoreCase(person.getEpisodes().get(0).getAttribute("datasource"))) {
				newPersons.add(person);
			}
		}
		logger.info(String.format("New population size: %s.", newPersons.size()));
		
		logger.info("Writing persons...");
		XMLWriter writer = new XMLWriter();
		writer.write(args[1], newPersons);
		logger.info("Done.");
	}

}
