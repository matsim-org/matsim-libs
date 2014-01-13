/* *********************************************************************** *
 * project: org.matsim.*
 * SocialPopulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.socnetgen.framework;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * @author thibautd
 */
public class SocialPopulation {
	private final Map<Id, Agent> agents = new LinkedHashMap<Id, Agent>();
	private final Map<Id, Agent> unmodifiableAgentsMap = Collections.unmodifiableMap( agents );
	private final Collection<Agent> unmodifiableAgents = Collections.unmodifiableCollection( agents.values() );

	public void addAgent( final Agent agent ) {
		agents.put( agent.getId() , agent );
	}

	public Collection<Agent> getAgents() {
		return unmodifiableAgents;
	}

	public Map<Id, Agent> getAgentsMap() {
		return this.unmodifiableAgentsMap;
	}
}

