/* *********************************************************************** *
 * project: org.matsim.*
 * LinkFilterFactory.java
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

package org.matsim.withinday.replanning.identifiers.filter;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilterFactory;

public class LinkFilterFactory implements AgentFilterFactory {

	private final Set<Id<Link>> links;
	private final MobsimDataProvider mobsimDataProvider;
	
	public LinkFilterFactory(Set<Id<Link >> links, MobsimDataProvider mobsimDataProvider) {
		this.links = links;
		this.mobsimDataProvider = mobsimDataProvider;
	}
	
	@Override
	public LinkFilter createAgentFilter() {
		return new LinkFilter(this.mobsimDataProvider.getAgents(), this.links);
	}

}
