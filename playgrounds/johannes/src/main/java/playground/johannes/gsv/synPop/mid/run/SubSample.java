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
import playground.johannes.gsv.synPop.DeleteRandom;
import playground.johannes.gsv.synPop.PersonTaskComposite;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.processing.TaskRunner;

import java.util.Collection;

/**
 * @author johannes
 *
 */
public class SubSample {

	private static final Logger logger = Logger.getLogger(SubSample.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		
		parser.addToBlacklist("workLoc");
		parser.addToBlacklist("homeLoc");
		parser.addToBlacklist("homeCoord");
		parser.addToBlacklist("location");
		parser.addToBlacklist("coord");
		parser.addToBlacklist("state");
		parser.addToBlacklist("inhabClass");
		parser.addToBlacklist("index");
		parser.addToBlacklist("roundTrip");
		parser.addToBlacklist("origin");
		parser.addToBlacklist("purpose");
		parser.addToBlacklist("delete");
		
		parser.parse(args[0]);
		logger.info(String.format("Loaded %s persons.", parser.getPersons().size()));
		
		double proba = Double.parseDouble(args[1]);
		PersonTaskComposite tasks = new PersonTaskComposite();
		tasks.addComponent(new DeleteRandom(1-proba));
		
		Collection<PlainPerson> subset = TaskRunner.runAndDeletePerson(tasks, parser.getPersons());
		logger.info(String.format("New population: %s persons.", subset.size()));
		
		logger.info("Writing population...");
		XMLWriter writer = new XMLWriter();
		writer.write(args[2], subset);
		logger.info("Done.");
		
	}

}
