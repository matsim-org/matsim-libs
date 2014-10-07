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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.io.XMLWriter;

public class MergePopulations {
	
	private static final Logger logger = Logger.getLogger(MergePopulations.class);

	public static void main(String[] args) {
		XMLParser parser = new XMLParser();
		parser.setValidating(false);

		logger.info("Loading persons...");
		parser.parse("");
		Set<ProxyPerson> persons1 = parser.getPersons();
		logger.info(String.format("Loaded %s persons.", persons1.size()));

		logger.info("Loading persons...");
		parser.parse("");
		Set<ProxyPerson> persons2 = parser.getPersons();
		logger.info(String.format("Loaded %s persons.", persons2.size()));

		double proba1 = 0;
		double proba2 = 0;
		
		proba1 = proba1/(proba1 + proba2);
		proba2 = proba2/(proba1 + proba2);
		
		int N = 1000000;
		
		int n1 = (int) (N * proba1);
		int n2 = N - n1;
		
		Set<ProxyPerson> newPersons = new HashSet<>(N);
		
		Iterator<ProxyPerson> it = persons1.iterator();
		for(int i = 0; i < n1; i++) {
			newPersons.add(it.next());
		}
		
		it = persons2.iterator();
		for(int i = 0; i < n2; i++) {
			newPersons.add(it.next());
		}
		
		XMLWriter writer = new XMLWriter();
		writer.write("", newPersons);
	}

}
