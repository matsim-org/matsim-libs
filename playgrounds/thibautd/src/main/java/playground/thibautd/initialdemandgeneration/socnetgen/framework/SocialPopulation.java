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

import org.matsim.api.core.v01.Id;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public class SocialPopulation<T extends Agent> {
	private final Map<Id, T> agents = new LinkedHashMap<Id, T>();
	private final Map<Id, T> unmodifiableAgentsMap = Collections.unmodifiableMap( agents );
	private final Collection<T> unmodifiableAgents = Collections.unmodifiableCollection( agents.values() );

	public void addAgent( final T agent ) {
		agents.put( agent.getId() , agent );
	}

	public Collection<T> getAgents() {
		return unmodifiableAgents;
	}

	public Map<Id, T> getAgentsMap() {
		return this.unmodifiableAgentsMap;
	}
}

