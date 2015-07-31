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


import playground.johannes.synpop.data.Element;
import playground.johannes.synpop.data.PlainPerson;

/**
 * @author johannes
 *
 */
public class DeleteOverlappingLegsTask implements ProxyPersonTask {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.ProxyPersonTask#apply(playground.johannes.synpop.data.PlainPerson)
	 */
	@Override
	public void apply(PlainPerson person) {
		/*
		 * Check for overlapping legs.
		 */
		double prevEnd = 0;
		for(Element leg : person.getPlan().getLegs()) {
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
