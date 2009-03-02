/* *********************************************************************** *
 * project: org.matsim.*
 * WithindayAgentLogicFactory.java
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

package org.matsim.withinday;

import java.util.LinkedList;
import java.util.List;

import org.matsim.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.network.NetworkLayer;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.utils.collections.Tuple;
import org.matsim.withinday.beliefs.AgentBeliefs;
import org.matsim.withinday.beliefs.AgentBeliefsImpl;
import org.matsim.withinday.contentment.AgentContentment;
import org.matsim.withinday.contentment.PlanScore;
import org.matsim.withinday.percepts.AgentPercepts;
import org.matsim.withinday.percepts.NextLinkTravelTimePerception;
import org.matsim.withinday.routeprovider.AStarLandmarksRouteProvider;
import org.matsim.withinday.routeprovider.HierarchicalRouteProvider;
import org.matsim.withinday.routeprovider.RouteProvider;


/**
 * @author dgrether
 *
 */
public class WithindayAgentLogicFactory {

	protected NetworkLayer network;
	private CharyparNagelScoringConfigGroup scoringConfig;
	protected AStarLandmarksRouteProvider aStarRouteProvider;
	
	public WithindayAgentLogicFactory(final NetworkLayer network, final CharyparNagelScoringConfigGroup scoringConfig) {
		this.network = network;
		this.scoringConfig = scoringConfig;
		this.aStarRouteProvider = new AStarLandmarksRouteProvider(this.network);
	}
	
	
	public RouteProvider createRouteProvider() {
		return new HierarchicalRouteProvider(this.aStarRouteProvider);
	}

	public AgentContentment createAgentContentment(final WithindayAgent agent) {
		return new PlanScore(agent, this.scoringConfig.getPerforming(), this.scoringConfig.getLateArrival());
	}
	
	public ScoringFunctionFactory createScoringFunctionFactory() {
		return new CharyparNagelScoringFunctionFactory(this.scoringConfig);
	}
	
	
	public Tuple<AgentBeliefs, List<AgentPercepts>> createAgentPerceptsBeliefs(final int sightDistance) {
		//create the beliefs
		AgentBeliefs beliefs = new AgentBeliefsImpl();
		NextLinkTravelTimePerception nextLinkPerception = new NextLinkTravelTimePerception(sightDistance);
		beliefs.addTravelTimePerception(nextLinkPerception);
		//create the percepts
		List<AgentPercepts> percepts = new LinkedList<AgentPercepts>();
		percepts.add(nextLinkPerception);
		
		return new Tuple<AgentBeliefs, List<AgentPercepts>>(beliefs, percepts);
		
	}
	
}
