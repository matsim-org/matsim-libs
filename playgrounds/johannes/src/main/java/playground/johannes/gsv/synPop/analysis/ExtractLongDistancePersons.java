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

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.io.XMLWriter;

/**
 * @author johannes
 *
 */
public class ExtractLongDistancePersons {

	private static final Logger logger = Logger.getLogger(ExtractLongDistancePersons.class);
	
	public static void main(String[] args) {
	
		logger.info("Loading persons...");
		XMLParser parser = new XMLParser();
		parser.setValidating(false);

		parser.parse(args[0]);
		logger.info(String.format("Loaded %s persons.", parser.getPersons().size()));
		
		double threshold = Double.parseDouble(args[3]);
		
		Set<ProxyPerson> remove = new HashSet<>();
		
		logger.info("Extracting persons...");
		for(ProxyPerson person : parser.getPersons()) {
			for(ProxyPlan plan : person.getPlans()) {
				for(ProxyObject leg : plan.getLegs()) {
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
		for(ProxyPerson person : remove) {
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
