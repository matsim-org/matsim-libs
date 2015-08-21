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


import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.source.mid2008.processing.PersonTask;

/**
 * @author johannes
 *
 */
public class DeleteOverlappingLegsTask implements PersonTask {

	/* (non-Javadoc)
	 * @see playground.johannes.synpop.source.mid2008.processing.PersonTask#apply(playground.johannes.synpop.data.PlainPerson)
	 */
	@Override
	public void apply(Person person1) {
		PlainPerson person = (PlainPerson)person1;
		/*
		 * Check for overlapping legs.
		 */
		double prevEnd = 0;
		for(Attributable leg : person.getPlan().getLegs()) {
			String startStr = leg.getAttribute(CommonKeys.LEG_START_TIME);
			if(startStr != null) {
				double start = Double.parseDouble(startStr);
				if(start < prevEnd) {
					person.setAttribute(CommonKeys.DELETE, "true");
					return;
				}
			}
			String endStr = leg.getAttribute(CommonKeys.LEG_END_TIME);
			if(endStr != null) {
				prevEnd = Double.parseDouble(endStr);
			} else {
				if(startStr == null) {
					person.setAttribute(CommonKeys.DELETE, "true"); // redundant with DeleteMissingTimesTask
					return;
				}
				prevEnd = Double.parseDouble(startStr) + 1;
			}	
		}

	}

}
