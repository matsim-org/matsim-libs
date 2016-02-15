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

import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.processing.PersonTask;

/**
 * @author johannes
 *
 */
public class DeletePersonKeyValue implements PersonTask {

	private final String key;
	
	private final String value;
	
	public DeletePersonKeyValue(String key, String value) {
		this.key = key;
		this.value = value;
	}
	
	@Override
	public void apply(Person person) {
		String val = person.getAttribute(key);
		if(!value.equalsIgnoreCase(val)) {
			person.setAttribute(CommonKeys.DELETE, "true");
		}
	}

}
