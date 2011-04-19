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

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.ptproject.qsim.agents.WithinDayAgent;
import org.matsim.ptproject.qsim.comparators.PersonAgentComparator;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;

import playground.christoph.evacuation.config.EvacuationConfig;

public class InsecureLegPerformingIdentifier extends DuringLegIdentifier {
	
	private static final Logger log = Logger.getLogger(InsecureLegPerformingIdentifier.class);
	
	protected LinkReplanningMap linkReplanningMap;
	protected Coord centerCoord;
	protected double secureDistance;
	protected Network network;
	
	
	/*package*/ InsecureLegPerformingIdentifier(LinkReplanningMap linkReplanningMap, Network network, Coord centerCoord, double secureDistance) {
		this.linkReplanningMap = linkReplanningMap;
		this.network = network;
		this.centerCoord = centerCoord;
		this.secureDistance = secureDistance;
	}
	
	public Set<WithinDayAgent> getAgentsToReplan(double time) {
		Set<WithinDayAgent> legPerformingAgents =  linkReplanningMap.getLegPerformingAgents();
		Collection<WithinDayAgent> handledAgents = this.getHandledAgents();
		Set<WithinDayAgent> agentsToReplan = new TreeSet<WithinDayAgent>(new PersonAgentComparator());
		
		if (handledAgents == null) return agentsToReplan;	
		
		for (PersonAgent personAgent : legPerformingAgents) {		
			/*
			 * Remove the Agent from the list, if the replanning flag is not set.
			 */
			if (!handledAgents.contains(personAgent)) continue;
			
			/*
			 * Remove the Agent from the list, if the current Link is in an secure Area.
			 */
			Link currentLink = this.network.getLinks().get(personAgent.getCurrentLinkId());
			double distanceToStartNode = CoordUtils.calcDistance(currentLink.getFromNode().getCoord(), centerCoord);
			double distanceToEndNode = CoordUtils.calcDistance(currentLink.getToNode().getCoord(), centerCoord);
			if (distanceToStartNode > secureDistance && distanceToEndNode > secureDistance) {
				continue;
			}
			
			/*
			 * Add the Agent to the Replanning List
			 */
			agentsToReplan.add((WithinDayAgent)personAgent);
		}
		if (time == EvacuationConfig.evacuationTime) log.info("Found " + agentsToReplan.size() + " Agents performing a Leg in an insecure area.");
		
		return agentsToReplan;
	}
	
}
