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

import java.util.Collection;

import org.apache.log4j.Logger;

import playground.johannes.gsv.synPop.DeleteRandom;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPersonTaskComposite;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.io.XMLWriter;

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
		
		parser.parse("/home/johannes/gsv/synpop/output/38.pop.xml");
		logger.info(String.format("Loaded %s persons.", parser.getPersons().size()));
		
		ProxyPersonTaskComposite tasks = new ProxyPersonTaskComposite();
		tasks.addComponent(new DeleteRandom(0.5));
		
		Collection<ProxyPerson> subset = ProxyTaskRunner.runAndDelete(tasks, parser.getPersons());
		logger.info(String.format("New population: %s persons.", subset.size()));
		
		logger.info("Writing population...");
		XMLWriter writer = new XMLWriter();
		writer.write("/home/johannes/gsv/synpop/output/38.pop.50.xml", subset);
		logger.info("Done.");
		
	}

}
