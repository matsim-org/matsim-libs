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
import playground.johannes.synpop.data.PlainFactory;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.data.io.XMLHandler;
import playground.johannes.synpop.data.io.XMLWriter;
import playground.johannes.synpop.processing.TaskRunner;

import java.util.Set;

/**
 * @author johannes
 *
 */
public class ExtractStatePopulation {

	private static final Logger logger = Logger.getLogger(ExtractStatePopulation.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String infile = args[0];
		String outfile = args[1];
		String state = args[2];
		
		XMLHandler parser = new XMLHandler(new PlainFactory());
		parser.setValidating(false);
	
		logger.info("Loading persons...");
		parser.parse(infile);
		Set<PlainPerson> persons = (Set<PlainPerson>)parser.getPersons();
		logger.info(String.format("Loaded %s persons.", persons.size()));
		
		logger.info("Applying filter...");
		TaskRunner.validatePersons(new DeletePersonKeyValue("state", state), persons);
		logger.info(String.format("Population size: %s", persons.size()));
		
		XMLWriter writer = new XMLWriter();
		writer.write(outfile, persons);
	}

}
