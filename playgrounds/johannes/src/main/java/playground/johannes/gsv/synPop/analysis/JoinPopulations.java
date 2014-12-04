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

import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.io.XMLWriter;

/**
 * @author johannes
 *
 */
public class JoinPopulations {

	private static final Logger logger = Logger.getLogger(JoinPopulations.class);
	
	public static void main(String[] args) {
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
	
		logger.info("Loading persons 1...");
		parser.parse(args[0]);
		Set<ProxyPerson> persons = new HashSet<>(parser.getPersons());
		
		logger.info("Loading persons 2...");
		parser = new XMLParser();
		parser.setValidating(false);
		parser.parse(args[1]);
		persons.addAll(parser.getPersons());
		
		logger.info("Writing persons...");
		XMLWriter writer = new XMLWriter();
		writer.write(args[2], persons);
		logger.info("Done.");
	}

}
