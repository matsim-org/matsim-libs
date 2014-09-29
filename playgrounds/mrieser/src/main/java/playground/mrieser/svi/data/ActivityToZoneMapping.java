/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mrieser.svi.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 * @author mrieser
 */
public class ActivityToZoneMapping {

	private final Map<Id<Person>, String[]> actZonesPerAgent = new LinkedHashMap<Id<Person>, String[]>();

	public ActivityToZoneMapping() {
	}

	public void addAgentActivityZones(final Id<Person> agentId, final String[] zones) {
		this.actZonesPerAgent.put(agentId, zones.clone());
	}

	public String[] getAgentActivityZones(final Id<Person> agentId) {
		return this.actZonesPerAgent.get(agentId);
	}

	/*package*/ Set<Id<Person>> getAgentIds() {
		return this.actZonesPerAgent.keySet();
	}
}
