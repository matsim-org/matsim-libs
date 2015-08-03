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

import playground.johannes.synpop.data.PlainPerson;

import java.util.Map;

/**
 * @author johannes
 *
 */
public class PersonStateHandler implements PersonAttributeHandler {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.mid.PersonAttributeHandler#handle(playground.johannes.synpop.data.PlainPerson, java.util.Map)
	 */
	@Override
	public void handle(PlainPerson person, Map<String, String> attributes) {
		String state = attributes.get("bland");
		if(state != null) {
			person.setAttribute(MIDKeys.PERSON_STATE, state);
		}

	}

}
