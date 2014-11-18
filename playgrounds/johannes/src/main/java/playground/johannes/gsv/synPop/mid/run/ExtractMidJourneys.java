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

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.io.XMLWriter;

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
		parser.parse("/home/johannes/gsv/mid2008/pop/pop.xml");
		Set<ProxyPerson> persons = parser.getPersons();
		logger.info(String.format("Loaded %s persons.", persons.size()));
		
		Set<ProxyPerson> newPersons = new HashSet<>();
		for(ProxyPerson person : persons) {
			if("midjourneys".equalsIgnoreCase(person.getPlans().get(0).getAttribute("datasource"))) {
				newPersons.add(person);
			}
		}
		
		logger.info("Writing persons...");
		XMLWriter writer = new XMLWriter();
		writer.write("/home/johannes/gsv/mid2008/pop/pop.midjourneys.xml", newPersons);
		logger.info("Done.");
	}

}
