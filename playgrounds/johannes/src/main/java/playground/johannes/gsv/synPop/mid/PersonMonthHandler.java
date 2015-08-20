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

import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.source.mid2008.generator.PersonAttributeHandler;

import java.util.Map;

/**
 * @author johannes
 *
 */
public class PersonMonthHandler implements PersonAttributeHandler {

	@Override
	public void handle(Person person, Map<String, String> attributes) {
		String val = attributes.get("stich_m");
		if(val != null) {
			if(val.equalsIgnoreCase("Januar")) {
				person.setAttribute(MIDKeys.PERSON_MONTH, MIDKeys.JANUARY);
			} else if(val.equalsIgnoreCase("Februar")) {
				person.setAttribute(MIDKeys.PERSON_MONTH, MIDKeys.FEBRUARY);
			} else if(val.equalsIgnoreCase("März")) {
				person.setAttribute(MIDKeys.PERSON_MONTH, MIDKeys.MARCH);
			} else if(val.equalsIgnoreCase("April")) {
				person.setAttribute(MIDKeys.PERSON_MONTH, MIDKeys.APRIL);
			} else if(val.equalsIgnoreCase("Mai")) {
				person.setAttribute(MIDKeys.PERSON_MONTH, MIDKeys.MAY);
			} else if(val.equalsIgnoreCase("Juni")) {
				person.setAttribute(MIDKeys.PERSON_MONTH, MIDKeys.JUNE);
			} else if(val.equalsIgnoreCase("July")) {
				person.setAttribute(MIDKeys.PERSON_MONTH, MIDKeys.JULY);
			} else if(val.equalsIgnoreCase("August")) {
				person.setAttribute(MIDKeys.PERSON_MONTH, MIDKeys.AUGUST);
			} else if(val.equalsIgnoreCase("September")) {
				person.setAttribute(MIDKeys.PERSON_MONTH, MIDKeys.SEPTEMBER);
			} else if(val.equalsIgnoreCase("Oktober")) {
				person.setAttribute(MIDKeys.PERSON_MONTH, MIDKeys.OCTOBER);
			} else if(val.equalsIgnoreCase("November")) {
				person.setAttribute(MIDKeys.PERSON_MONTH, MIDKeys.NOVEMBER);
			} else if(val.equalsIgnoreCase("Dezember")) {
				person.setAttribute(MIDKeys.PERSON_MONTH, MIDKeys.DECEMBER);
			}
		}

	}

}
