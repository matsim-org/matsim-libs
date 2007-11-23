/* *********************************************************************** *
 * project: org.matsim.*
 * PersonTypeStats.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.plans.algorithms;

import java.util.HashMap;
import java.util.Map;

import org.matsim.plans.Person;

public class PersonTypeStats extends PersonAlgorithm {

	private Map<String, Integer> stats = new HashMap<String, Integer>();
	
	@Override
	public void run(Person person) {
		String type = person.getType();
		if (type == null) type = "null";
		
		Integer cnt = stats.get(type);
		if (cnt == null) cnt = 0;
		stats.put(type, cnt + 1);

	}
	
	public Map<String, Integer> getStats() {
		return null;
	}

}
