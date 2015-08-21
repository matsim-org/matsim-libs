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

package playground.johannes.synpop.source.mid2008.generator;

import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;

import java.util.Map;

/**
 * @author johannes
 *
 */
public class PersonHHMembersHandler implements PersonAttributeHandler {

	@Override
	public void handle(Person person, Map<String, String> attributes) {
		String val = attributes.get(VariableNames.HH_MEMEBERS);
		if(val != null) {
			int num = Integer.parseInt(val);
			
			if(num >= 1 && num <= 11) {
				person.setAttribute(CommonKeys.HH_MEMBERS, String.valueOf(num));
			}
		}

	}

}
