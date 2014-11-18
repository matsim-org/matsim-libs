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

import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import playground.johannes.gsv.synPop.ApplySampleProbas;
import playground.johannes.gsv.synPop.DeleteNoLegs;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.gsv.synPop.mid.PersonCloner;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

/**
 * @author johannes
 *
 */
public class ClonePopulation {

	public static final Logger logger = Logger.getLogger(ClonePopulation.class);
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
		
		logger.info("Cloning persons...");
		Random random = new XORShiftRandom();
		persons = PersonCloner.weightedClones(persons, 2000000, random);
		new ApplySampleProbas(82000000).apply(persons);
		logger.info(String.format("Generated %s persons.", persons.size()));

		logger.info("Deleting persons with no legs..." );
		persons = ProxyTaskRunner.runAndDeletePerson(new DeleteNoLegs(), persons);
		logger.info("Population size = " + persons.size());
		
		XMLWriter writer = new XMLWriter();
		writer.write("/home/johannes/gsv/mid2008/pop/pop.mob.xml", persons);
	}

}
