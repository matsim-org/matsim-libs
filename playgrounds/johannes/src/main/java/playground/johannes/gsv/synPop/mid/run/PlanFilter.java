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

import playground.johannes.gsv.synPop.DeleteLegs;
import playground.johannes.gsv.synPop.ProxyPersonTaskComposite;
import playground.johannes.gsv.synPop.io.XMLParser;

/**
 * @author johannes
 *
 */
public class PlanFilter {

	private static final Logger logger = Logger.getLogger(PlanFilter.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		
		parser.parse("/home/johannes/gsv/synpop/output/1400000000.pop.xml.gz");
		logger.info(String.format("Loaded %s persons.", parser.getPersons().size()));
		
		ProxyPersonTaskComposite tasks = new ProxyPersonTaskComposite();
		tasks.addComponent(new DeleteLegs("car"));
		
	}

	
}
