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

package playground.christoph.evacuation.withinday.replanning.identifiers;

import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.ptproject.qsim.comparators.PersonAgentComparator;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.config.EvacuationConfig;

public class InsecureLegPerformingIdentifier extends DuringLegIdentifier {
	
	private static final Logger log = Logger.getLogger(InsecureLegPerformingIdentifier.class);
	
	private final LinkReplanningMap linkReplanningMap;
	private final CoordAnalyzer coordAnalyzer;
	private final Network network;	
	
	/*package*/ InsecureLegPerformingIdentifier(LinkReplanningMap linkReplanningMap, Network network, CoordAnalyzer coordAnalyzer) {
		this.linkReplanningMap = linkReplanningMap;
		this.network = network;
		this.coordAnalyzer = coordAnalyzer;
	}
	
	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {
		Set<PlanBasedWithinDayAgent> legPerformingAgents =  linkReplanningMap.getLegPerformingAgents();
		Set<PlanBasedWithinDayAgent> agentsToReplan = new TreeSet<PlanBasedWithinDayAgent>(new PersonAgentComparator());
		
		for (MobsimAgent personAgent : legPerformingAgents) {
			Link currentLink = this.network.getLinks().get(personAgent.getCurrentLinkId());
			boolean isAffected = this.coordAnalyzer.isLinkAffected(currentLink);
			
			/*
			 * If the agent is affected, add it to the replanning list.
			 */
			if (isAffected) agentsToReplan.add((PlanBasedWithinDayAgent) personAgent);
		}
		if (time == EvacuationConfig.evacuationTime) log.info("Found " + agentsToReplan.size() + " Agents performing a Leg in an insecure area.");
		
		return agentsToReplan;
	}
	
}
