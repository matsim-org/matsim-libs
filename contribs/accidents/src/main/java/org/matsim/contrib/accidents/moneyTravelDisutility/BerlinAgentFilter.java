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

package org.matsim.contrib.accidents.moneyTravelDisutility;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
* @author ikaddoura
*/

public class BerlinAgentFilter implements AgentFilter {

	private String[] vehicleTypeIdPrefixes = {"lkw"};

	@Override
	public String getAgentTypeFromId(Id<Person> id) {
		
		if (id==null) {
			return "null";
		} else {
			for (String prefix : vehicleTypeIdPrefixes) {
				if (id.toString().startsWith(prefix)) {
					return prefix;
				}
			}
			return "other";
		}
	}

}

