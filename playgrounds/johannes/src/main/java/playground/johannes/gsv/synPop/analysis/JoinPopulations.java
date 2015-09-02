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
import playground.johannes.synpop.data.io.XMLHandler;
import playground.johannes.synpop.data.io.XMLWriter;
import playground.johannes.synpop.data.PlainFactory;
import playground.johannes.synpop.data.PlainPerson;

import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class JoinPopulations {

	private static final Logger logger = Logger.getLogger(JoinPopulations.class);
	
	public static void main(String[] args) {
		XMLHandler parser = new XMLHandler(new PlainFactory());
		parser.setValidating(false);
	
		logger.info("Loading persons 1...");
		parser.parse(args[0]);
		Set<PlainPerson> persons = new HashSet<>((Set<PlainPerson>)parser.getPersons());
		
		logger.info("Loading persons 2...");
		parser = new XMLHandler(new PlainFactory());
		parser.setValidating(false);
		parser.parse(args[1]);
		persons.addAll((Set<PlainPerson>)parser.getPersons());
		
		logger.info("Writing persons...");
		XMLWriter writer = new XMLWriter();
		writer.write(args[2], persons);
		logger.info("Done.");
	}

}
