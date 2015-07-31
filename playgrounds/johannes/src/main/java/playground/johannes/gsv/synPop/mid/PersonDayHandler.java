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

package playground.johannes.gsv.synPop.mid;

import java.util.Map;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPerson;

/**
 * @author johannes
 *
 */
public class PersonDayHandler implements PersonAttributeHandler {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.mid.LegAttributeHandler#handle(playground.johannes.synpop.data.PlainElement, java.util.Map)
	 */
	@Override
	public void handle(ProxyPerson person, Map<String, String> attributes) {
		String day = attributes.get(MIDKeys.SURVEY_DAY);
		if(day.equalsIgnoreCase("Montag")) {
			person.setAttribute(CommonKeys.DAY, CommonKeys.MONDAY);
		} else if(day.equalsIgnoreCase("Dienstag")) {
			person.setAttribute(CommonKeys.DAY, CommonKeys.TUESDAY);
		} else if(day.equalsIgnoreCase("Mittwoch")) {
			person.setAttribute(CommonKeys.DAY, CommonKeys.WEDNESDAY);
		} else if(day.equalsIgnoreCase("Donnerstag")) {
			person.setAttribute(CommonKeys.DAY, CommonKeys.THURSDAY);
		} else if(day.equalsIgnoreCase("Freitag")) {
			person.setAttribute(CommonKeys.DAY, CommonKeys.FRIDAY);
		} else if(day.equalsIgnoreCase("Samstag")) {
			person.setAttribute(CommonKeys.DAY, CommonKeys.SATURDAY);
		} else if(day.equalsIgnoreCase("Sonntag")) {
			person.setAttribute(CommonKeys.DAY, CommonKeys.SUNDAY);
		}
	}

}
