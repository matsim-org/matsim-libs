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

import playground.johannes.gsv.synPop.ProxyPerson;

/**
 * @author johannes
 *
 */
public class PersonMunicipalityClassHandler implements PersonAttributeHandler {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.mid.PersonAttributeHandler#handle(playground.johannes.gsv.synPop.ProxyPerson, java.util.Map)
	 */
	@Override
	public void handle(ProxyPerson person, Map<String, String> attributes) {
		String val = attributes.get(MIDKeys.PERSON_MUNICIPALITY);

		if(val.equalsIgnoreCase("unter 5.000 Einw.")) {
			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_LOWER, 0);
			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_UPPER, 5000);
		} else if(val.equalsIgnoreCase("5.000 bis unter 20.000 Einw.")) {
			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_LOWER, 5000);
			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_UPPER, 20000);
		} else if(val.equalsIgnoreCase("20.000 bis unter 50.000 Einw.")) {
			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_LOWER, 20000);
			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_UPPER, 50000);
		} else if(val.equalsIgnoreCase("50.000 bis unter 100.000 Einw.")) {
			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_LOWER, 50000);
			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_UPPER, 100000);
		} else if(val.equalsIgnoreCase("100.000 bis unter 500.000 Einw.")) {
			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_LOWER, 100000);
			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_UPPER, 500000);
		} else if(val.equalsIgnoreCase("500.000 und mehr Einw.")) {
			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_LOWER, 500000);
			person.setAttribute(MIDKeys.PERSON_MUNICIPALITY_UPPER, Integer.MAX_VALUE);
		}
	}

}
