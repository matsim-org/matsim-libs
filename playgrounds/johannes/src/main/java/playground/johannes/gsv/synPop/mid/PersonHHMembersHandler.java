/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.source.mid2008.generator.PersonAttributeHandler;

import java.util.Map;

/**
 * @author johannes
 *
 */
public class PersonHHMembersHandler implements PersonAttributeHandler {

	/* (non-Javadoc)
	 * @see playground.johannes.synpop.source.mid2008.generator.PersonAttributeHandler#handle(playground.johannes.synpop.data.PlainPerson, java.util.Map)
	 */
	@Override
	public void handle(Person person, Map<String, String> attributes) {
		String val = attributes.get(MIDKeys.HH_MEMEBERS);
		if(val != null) {
			int num = Integer.parseInt(val);
			
			if(num >= 1 && num <= 11) {
				person.setAttribute(CommonKeys.HH_MEMBERS, String.valueOf(num));
			}
		}

	}

}
