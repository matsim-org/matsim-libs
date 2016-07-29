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

package playground.johannes.gsv.synPop.analysis;

import org.apache.log4j.Logger;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.data.io.XMLHandler;
import playground.johannes.synpop.data.io.XMLWriter;

import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class ExtractLongDistancePersons {

	private static final Logger logger = Logger.getLogger(ExtractLongDistancePersons.class);
	
	public static void main(String[] args) {
	
		logger.info("Loading persons...");
		XMLHandler parser = new XMLHandler(new PlainFactory());
		parser.setValidating(false);

		parser.readFile(args[0]);
		logger.info(String.format("Loaded %s persons.", parser.getPersons().size()));
		
		double threshold = Double.parseDouble(args[3]);
		
		Set<Person> remove = new HashSet<>();
		
		logger.info("Extracting persons...");
		for(Person person : parser.getPersons()) {
			for(Episode plan : person.getEpisodes()) {
				for(Attributable leg : plan.getLegs()) {
					String val = leg.getAttribute(CommonKeys.LEG_ROUTE_DISTANCE);
					if(val != null) {
						double d = Double.parseDouble(val);
						
						if(d < threshold) {
							remove.add(person);
							break;
						}
					}
				}
			}
		}
		
		logger.info(String.format("Removing %s persons...", remove.size()));
		for(Person person : remove) {
			parser.getPersons().remove(person);
		}
		
		logger.info(String.format("Writing %s long-dist persons to file...", parser.getPersons().size()));
		XMLWriter writer = new XMLWriter();
		writer.write(args[1], parser.getPersons());
		
		logger.info(String.format("Writing %s short-dist persons to file...", remove.size()));
		writer.write(args[2], remove);
		
		logger.info("Done.");

	}

}
