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
import org.matsim.contrib.common.util.XORShiftRandom;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.PlainFactory;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.data.io.XMLHandler;
import playground.johannes.synpop.data.io.XMLWriter;
import playground.johannes.synpop.source.mid2008.MiDValues;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class KeepMidTrips {

	private static final Logger logger = Logger.getLogger(KeepMidTrips.class);
	
	public static void main(String[] args) {
		XMLHandler parser = new XMLHandler(new PlainFactory());
		parser.setValidating(false);
		parser.readFile(args[0]);
		
		Set<PlainPerson> persons = (Set<PlainPerson>)parser.getPersons();
		
		logger.info(String.format("Loaded %s perons.", persons.size()));
		
		double proba = Double.parseDouble(args[2]);

		Random random = new XORShiftRandom(4711);
		
		Set<PlainPerson> keep = new HashSet<>(persons.size());
		
		for(PlainPerson person : persons) {
			for(Episode plan : person.getEpisodes()) {
				if(MiDValues.MID_TRIPS.equalsIgnoreCase(plan.getAttribute(CommonKeys.DATA_SOURCE))) {
					if(random.nextDouble() < proba) {
						keep.add(person);
						break;
					}
				}
			}
		}
		
//		for(PlainPerson person : keep) {
//			persons.remove(person);
//		}
		
		logger.info(String.format("Writing %s persons...", keep.size()));
		
		XMLWriter writer = new XMLWriter();
		writer.write(args[1], keep);
	}

}
