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

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ConvertRide2Car;
import playground.johannes.gsv.synPop.DeleteModes;
import playground.johannes.gsv.synPop.DeleteNoLegs;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.analysis.DeleteShortLongTrips;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.gsv.synPop.mid.MIDKeys;

/**
 * @author johannes
 *
 */
public class PersonFilter {

	private static final Logger logger = Logger.getLogger(PersonFilter.class);
	
	public static void main(String args[]) {
		String outDir = "/home/johannes/gsv/mid2008/pop/";
//		String outDir = "/Users/jillenberger/Dropbox/work/raw/";
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		XMLWriter writer = new XMLWriter();
		
		logger.info("Loading persons...");
		parser.parse("/home/johannes/gsv/mid2008/pop/pop.xml");
//		parser.parse("/Users/jillenberger/Dropbox/work/raw/pop.xml");
		Set<ProxyPerson> persons = parser.getPersons();
		logger.info(String.format("Loaded %s persons.", persons.size()));

//		logger.info("Cloning persons...");
//		persons = PersonCloner.weightedClones(persons, 100000, new XORShiftRandom());
//		new ApplySampleProbas(82000000).apply(persons);
//		logger.info("Population size = " + persons.size());
		
		logger.info("Converting ride legs to car legs...");
		ProxyTaskRunner.run(new ConvertRide2Car(), persons);
		
		logger.info("Converting activities to misc type...");
		ProxyTaskRunner.run(new Convert2MiscType(), persons);
		
		logger.info("Removing non mobile persons...");
		persons = ProxyTaskRunner.runAndDeletePerson(new DeleteNoLegs(), persons);
		logger.info(String.format("Persons after filter: %s", persons.size()));
		writer.write(outDir + "pop.mob.xml", persons);
		
		logger.info("Removing non car persons...");
		persons = ProxyTaskRunner.runAndDeletePerson(new DeleteModes("car"), persons);
		logger.info(String.format("Persons after filter: %s", persons.size()));
		writer.write(outDir + "pop.car.xml", persons);
		
		logger.info("Removing legs with less than 3 KM...");
		ProxyTaskRunner.run(new DeleteShortLongTrips(3000, true), persons);
		persons = ProxyTaskRunner.runAndDeletePerson(new DeleteNoLegs(), persons);
		logger.info(String.format("Persons after filter: %s", persons.size()));
		
//		writer.write(outDir + "pop.car.wo3km.xml", persons);
//		writer.write(outDir + "hesen.car.wo3km.midjourneys.xml", persons);
		
		logger.info("Removing legs with more than 1000 KM...");
		ProxyTaskRunner.run(new DeleteShortLongTrips(1000000, false), persons);
		persons = ProxyTaskRunner.runAndDeletePerson(new DeleteNoLegs(), persons);
		logger.info(String.format("Persons after filter: %s", persons.size()));
		writer.write(outDir + "pop.car.3-1000km.xml", persons);
		
		logger.info("Extracting MID trips...");
		Set<ProxyPerson> newPersons = new HashSet<>();
		for(ProxyPerson person : persons) {
			if(MIDKeys.MID_TRIPS.equalsIgnoreCase(person.getPlans().get(0).getAttribute(CommonKeys.DATA_SOURCE))) {
				newPersons.add(person);
			}
		}
		logger.info(String.format("Persons after filter: %s", newPersons.size()));
		writer.write(outDir + "pop.car.3-1000km.trips.xml", newPersons);
		
		logger.info("Extracting MID journeys...");
		newPersons = new HashSet<>();
		for(ProxyPerson person : persons) {
			if(MIDKeys.MID_JOUNREYS.equalsIgnoreCase(person.getPlans().get(0).getAttribute(CommonKeys.DATA_SOURCE))) {
				newPersons.add(person);
			}
		}
		logger.info(String.format("Persons after filter: %s", newPersons.size()));
		writer.write(outDir + "pop.car.3-1000km.journeys.xml", newPersons);
		
		logger.info("Done.");
	}
}
