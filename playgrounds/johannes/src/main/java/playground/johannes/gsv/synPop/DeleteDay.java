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

import playground.johannes.synpop.data.PlainPerson;

import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class DeleteDay implements ProxyPersonTask {

	private Set<String> days;

	public DeleteDay() {
		days = new HashSet<String>();
	}
	
	public void setWeekdays() {
		days.add(CommonKeys.MONDAY);
		days.add(CommonKeys.TUESDAY);
		days.add(CommonKeys.WEDNESDAY);
		days.add(CommonKeys.THURSDAY);
		days.add(CommonKeys.FRIDAY);
	}
	
	@Override
	public void apply(PlainPerson person) {
		String day = person.getAttribute(CommonKeys.DAY);
		boolean found = false;
		
		for(String allowed : days) {
			if(allowed.equalsIgnoreCase(day)) {
				found = true;
				break;
			}
		}
		
		if(!found) {
			person.setAttribute(CommonKeys.DELETE, "true");
		}
	}

}
