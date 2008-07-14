/* *********************************************************************** *
 * project: org.matsim.*
 * ReRouteAnalyzer.java
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

package playground.gregor.withinday_evac.analyzer;

import java.util.ArrayList;
import java.util.HashSet;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Route;
import org.matsim.router.Dijkstra;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;

import playground.gregor.withinday_evac.Intentions;
import playground.gregor.withinday_evac.beliefs.Beliefs;
import playground.gregor.withinday_evac.communication.InformationEntity;
import playground.gregor.withinday_evac.communication.LinkBlockedMessage;
import playground.gregor.withinday_evac.communication.InformationEntity.MSG_TYPE;

public class ReRouteAnalyzer implements Analyzer {

	private final NetworkLayer network;
	private final Beliefs beliefs;
	private Link lastCurrent = null;
	private Link lastNext = null;
	private int linkCount;
	private Link [] linkRoute;
	private final Intentions intentions;
	private HashSet<Link> blockedLinks;

	public ReRouteAnalyzer(final Beliefs beliefs, final NetworkLayer network, final Intentions intentions) {
		this.network = network;
		this.beliefs = beliefs;
		this.intentions = intentions;
	}

	public Action getAction(final double now) {
		
		updateBlockedLinks();
		
		if (this.lastCurrent != null && this.beliefs.getCurrentLink().getId() == this.lastCurrent.getId()){
			if (!blocked(this.lastNext)){
				return new NextLinkAction(this.lastNext,1);	
			}
		} else if (this.lastNext != null && this.beliefs.getCurrentLink().getId() == this.lastNext.getId()){
			if (!blocked(this.linkRoute[this.linkCount])){
				return new NextLinkAction(this.linkRoute[this.linkCount++],1);
			}
		} 


		final Dijkstra router = new Dijkstra(this.network,new FreespeedTravelTimeCost(),new FreespeedTravelTimeCost());
		final Route route = router.calcLeastCostPath(this.beliefs.getCurrentLink().getToNode(), this.intentions.getDestination(), now);
		this.linkRoute = route.getLinkRoute();
		this.linkCount = 0;
		this.lastCurrent = this.beliefs.getCurrentLink();
		this.lastNext = this.linkRoute[this.linkCount++];
		return new NextLinkAction(this.lastNext,1);


	}

	private boolean blocked(final Link link) {
		return this.blockedLinks.contains(link);
	}

	private void updateBlockedLinks() {
		this.blockedLinks = new HashSet<Link>();
		final ArrayList<InformationEntity> ies = this.beliefs.getInfos().get(MSG_TYPE.LINK_BLOCKED);
		if (ies == null) {
			return;
		}
		
		for (final InformationEntity ie : ies) {
			this.blockedLinks.add(((LinkBlockedMessage)ie.getMsg()).getLink());
		}
		
	}
	
	
}
