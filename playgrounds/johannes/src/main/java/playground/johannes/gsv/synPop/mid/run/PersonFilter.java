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

import java.util.Set;

import org.apache.log4j.Logger;

import playground.johannes.gsv.synPop.ApplySampleProbas;
import playground.johannes.gsv.synPop.ConvertRide2Car;
import playground.johannes.gsv.synPop.DeleteModes;
import playground.johannes.gsv.synPop.DeleteNoLegs;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.analysis.DeleteShortTrips;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.gsv.synPop.mid.PersonCloner;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

/**
 * @author johannes
 *
 */
public class PersonFilter {

	private static final Logger logger = Logger.getLogger(PersonFilter.class);
	
	public static void main(String args[]) {
		String outDir = "/home/johannes/gsv/mid2008/pop/";
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		XMLWriter writer = new XMLWriter();
		
		logger.info("Loading persons...");
		parser.parse("/home/johannes/gsv/mid2008/pop/pop.xml");
		Set<ProxyPerson> persons = parser.getPersons();
		logger.info(String.format("Loaded %s persons.", persons.size()));

		logger.info("Cloning persons...");
		persons = PersonCloner.weightedClones(persons, 1000000, new XORShiftRandom());
		new ApplySampleProbas(82000000).apply(persons);
		logger.info("Population size = " + persons.size());
		
		logger.info("Converting ride legs to car legs...");
		ProxyTaskRunner.run(new ConvertRide2Car(), persons);
		
		logger.info("Converting activities to misc type...");
		ProxyTaskRunner.run(new Convert2MiscType(), persons);
		
		logger.info("Removing non mobile persons...");
		persons = ProxyTaskRunner.runAndDelete(new DeleteNoLegs(), persons);
		logger.info(String.format("Persons after filte: %s", persons.size()));
		writer.write(outDir + "pop.mob.xml", persons);
		
		logger.info("Removing non car persons...");
		persons = ProxyTaskRunner.runAndDelete(new DeleteModes("car"), persons);
		logger.info(String.format("Persons after filte: %s", persons.size()));
		writer.write(outDir + "pop.car.xml", persons);
		
		logger.info("Removing legs with less than 3 KM...");
		ProxyTaskRunner.run(new DeleteShortTrips(3000), persons);
		persons = ProxyTaskRunner.runAndDelete(new DeleteNoLegs(), persons);
		logger.info(String.format("Persons after filte: %s", persons.size()));
		writer.write(outDir + "pop.car.wo3km.xml", persons);
		
		logger.info("Done.");
	}
}
