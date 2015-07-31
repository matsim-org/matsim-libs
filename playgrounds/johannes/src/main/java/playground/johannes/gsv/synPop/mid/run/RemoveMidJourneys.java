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

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.gsv.synPop.mid.MIDKeys;
import playground.johannes.socialnetworks.utils.XORShiftRandom;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.PlainPerson;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class RemoveMidJourneys {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		parser.parse(args[0]);
		
		Set<PlainPerson> persons = parser.getPersons();
		
		double proba = Double.parseDouble(args[2]);

		Random random = new XORShiftRandom(4711);
		
		Set<PlainPerson> remove = new HashSet<>(persons.size());
		
		for(PlainPerson person : persons) {
			for(Episode plan : person.getPlans()) {
				if(MIDKeys.MID_JOUNREYS.equalsIgnoreCase(plan.getAttribute(CommonKeys.DATA_SOURCE))) {
					if(random.nextDouble() < proba) {
						remove.add(person);
						break;
					}
				}
			}
		}
		
		for(PlainPerson person : remove) {
			persons.remove(person);
		}
		
		XMLWriter writer = new XMLWriter();
		writer.write(args[1], persons);
	}

}
