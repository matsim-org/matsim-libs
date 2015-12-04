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

package playground.johannes.gsv.synPop;

import org.apache.log4j.Logger;
import playground.johannes.synpop.data.PlainFactory;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.data.io.XMLHandler;
import playground.johannes.synpop.data.io.XMLWriter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MergePopulations {
	
	private static final Logger logger = Logger.getLogger(MergePopulations.class);

	public static void main(String[] args) {
		XMLHandler parser = new XMLHandler(new PlainFactory());
		parser.setValidating(false);

//		parser.addToBlacklist("workLoc");
//		parser.addToBlacklist("homeLoc");
//		parser.addToBlacklist("homeCoord");
//		parser.addToBlacklist("location");
//		parser.addToBlacklist("coord");
//		parser.addToBlacklist("state");
//		parser.addToBlacklist("inhabClass");
//		parser.addToBlacklist("index");
//		parser.addToBlacklist("roundTrip");
//		parser.addToBlacklist("origin");
//		parser.addToBlacklist("purpose");
//		parser.addToBlacklist("delete");
		
		logger.info("Loading persons...");
		parser.parse(args[0]);
		Set<PlainPerson> persons1 = (Set<PlainPerson>)parser.getPersons();
		logger.info(String.format("Loaded %s persons.", persons1.size()));

		logger.info("Loading persons...");
		parser.parse(args[1]);
		Set<PlainPerson> persons2 = (Set<PlainPerson>)parser.getPersons();
		logger.info(String.format("Loaded %s persons.", persons2.size()));

//		logger.info("Loading persons...");
//		parser.parse(args[2]);
//		Set<PlainPerson> persons3 = parser.getPersons();
//		logger.info(String.format("Loaded %s persons.", persons3.size()));

		double w1 = 1;
		double w2 = 1;
//		double w3 = 1;
		
//		double wsum = w1 + w2 + w3;
		double wsum = w1 + w2;
		double proba1 = w1/wsum;
		double proba2 = w2/wsum;
//		double proba3 = w3/wsum;
		
		logger.info(String.format("Probability for population 1: %s", proba1));
		logger.info(String.format("Probability for population 2: %s", proba2));
//		logger.info(String.format("Probability for population 3: %s", proba3));
		
		int N = Integer.parseInt(args[3]);
		logger.info(String.format("Generating %s persons...", N));
		
		int n1 = (int) (N * proba1);
		int n2 = N - n1;
//		int n2 = (int) (N * proba2);
//		int n3 = N - (n1 + n2);
		
		logger.info(String.format("Person from population 1: %s", n1));
		logger.info(String.format("Person from population 2: %s", n2));
//		logger.info(String.format("Person from population 2: %s", n3));
		
		Set<PlainPerson> newPersons = new HashSet<>(N);
		
		logger.info("Merging population 1...");
		Iterator<PlainPerson> it = persons1.iterator();
		for(int i = 0; i < n1; i++) {
			newPersons.add(it.next());
		}
		
		logger.info("Merging population 2...");
		it = persons2.iterator();
		for(int i = 0; i < n2; i++) {
			newPersons.add(it.next());
		}
		
//		logger.info("Merging population 3...");
//		it = persons3.iterator();
//		for(int i = 0; i < n3; i++) {
//			newPersons.add(it.next());
//		}
		
		logger.info("Writing population...");
		XMLWriter writer = new XMLWriter();
//		writer.write(args[3], newPersons);
		writer.write(args[2], newPersons);
		logger.info("Done.");
	}

}
