/* *********************************************************************** *
 * project: org.matsim.*
 * InsecureLegPerformingIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.withinday.replanning.identifiers.old;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.comparators.PersonAgentComparator;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.config.EvacuationConfig;

public class InsecureLegPerformingIdentifier extends DuringLegAgentSelector {
	
	private static final Logger log = Logger.getLogger(InsecureLegPerformingIdentifier.class);
	
	private final LinkReplanningMap linkReplanningMap;
	private final MobsimDataProvider mobsimDataProvider;
	private final CoordAnalyzer coordAnalyzer;
	private final Network network;	
	
	/*package*/ InsecureLegPerformingIdentifier(LinkReplanningMap linkReplanningMap, MobsimDataProvider mobsimDataProvider, 
			Network network, CoordAnalyzer coordAnalyzer) {
		this.linkReplanningMap = linkReplanningMap;
		this.mobsimDataProvider = mobsimDataProvider;
		this.network = network;
		this.coordAnalyzer = coordAnalyzer;
	}
	
	@Override
	public Set<MobsimAgent> getAgentsToReplan(double time) {
		
		Set<Id<Person>> legPerformingAgents = new HashSet<>(this.linkReplanningMap.getLegPerformingAgents());
		Map<Id<Person>, MobsimAgent> mapping = this.mobsimDataProvider.getAgents();
		
		// apply filter to remove agents that should not be replanned
		this.applyFilters(legPerformingAgents, time);
		
		Set<MobsimAgent> agentsToReplan = new TreeSet<MobsimAgent>(new PersonAgentComparator());		
		for (Id<Person> id : legPerformingAgents) {
			
			MobsimAgent agent = mapping.get(id);
			
			Link currentLink = this.network.getLinks().get(agent.getCurrentLinkId());
			boolean isAffected = this.coordAnalyzer.isLinkAffected(currentLink);
			
			/*
			 * If the agent is affected, add it to the replanning list.
			 */
			if (isAffected) agentsToReplan.add(agent);
		}
		if (time == EvacuationConfig.evacuationTime) log.info("Found " + agentsToReplan.size() + " Agents performing a Leg in an insecure area.");
		
		return agentsToReplan;
	}
	
}
