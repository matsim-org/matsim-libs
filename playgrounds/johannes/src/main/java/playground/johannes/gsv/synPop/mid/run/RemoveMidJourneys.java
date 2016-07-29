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
public class RemoveMidJourneys {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		XMLHandler parser = new XMLHandler(new PlainFactory());
		parser.setValidating(false);
		parser.readFile(args[0]);
		
		Set<PlainPerson> persons = (Set<PlainPerson>)parser.getPersons();
		
		double proba = Double.parseDouble(args[2]);

		Random random = new XORShiftRandom(4711);
		
		Set<PlainPerson> remove = new HashSet<>(persons.size());
		
		for(PlainPerson person : persons) {
			for(Episode plan : person.getEpisodes()) {
				if(MiDValues.MID_JOUNREYS.equalsIgnoreCase(plan.getAttribute(CommonKeys.DATA_SOURCE))) {
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
