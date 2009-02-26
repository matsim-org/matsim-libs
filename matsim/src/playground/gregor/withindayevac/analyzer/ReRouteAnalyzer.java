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

package playground.gregor.withindayevac.analyzer;

import java.util.Collection;
import java.util.HashSet;

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.router.Dijkstra;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.LeastCostPathCalculator.Path;

import playground.gregor.withindayevac.Beliefs;
import playground.gregor.withindayevac.Intentions;
import playground.gregor.withindayevac.communication.InformationEntity;
import playground.gregor.withindayevac.communication.LinkBlockedMessage;
import playground.gregor.withindayevac.communication.InformationEntity.MSG_TYPE;

public class ReRouteAnalyzer implements Analyzer {

	private final NetworkLayer network;
	private final Beliefs beliefs;
	private Link lastCurrent = null;
	private Link lastNext = null;
	private int linkCount;
	private Link [] linkRoute;
	private final Intentions intentions;
	private HashSet<Link> blockedLinks;
	private double coef = 1;

	public ReRouteAnalyzer(final Beliefs beliefs, final NetworkLayer network, final Intentions intentions) {
		this.network = network;
		this.beliefs = beliefs;
		this.intentions = intentions;
	}

	public NextLinkOption getAction(final double now) {
		
		updateBlockedLinks(now);
		
		if (this.lastCurrent != null && this.beliefs.getCurrentLink().getId() == this.lastCurrent.getId()){
			if (!blocked(this.lastNext)){
				return new NextLinkOption(this.lastNext,1*this.coef);	
			}
		} else if (this.lastNext != null && this.beliefs.getCurrentLink().getId() == this.lastNext.getId()){
			if (!blocked(this.linkRoute[this.linkCount])){
				this.lastCurrent = this.lastNext;
				this.lastNext = this.linkRoute[this.linkCount++];
				return new NextLinkOption(this.lastNext ,1*this.coef);
			}
		} 


		final Dijkstra router = new Dijkstra(this.network,new FreespeedTravelTimeCost(),new FreespeedTravelTimeCost());
		final Path path = router.calcLeastCostPath(this.beliefs.getCurrentLink().getToNode(), this.intentions.getDestination(), now);
		this.linkRoute = path.links.toArray(new Link[path.links.size()]);
		this.linkCount = 0;
		this.lastCurrent = this.beliefs.getCurrentLink();
		this.lastNext = this.linkRoute[this.linkCount++];
		return new NextLinkOption(this.lastNext,1*this.coef);
	}

	private boolean blocked(final Link link) {
		return this.blockedLinks.contains(link);
	}

	private void updateBlockedLinks(final double now) {
		this.blockedLinks = new HashSet<Link>();
//		final ArrayList<InformationEntity> ies = this.beliefs.getInfos().get(MSG_TYPE.LINK_BLOCKED);
		Id nodeId = this.beliefs.getCurrentLink().getToNode().getId();
		Collection<InformationEntity> ies = this.beliefs.getInfos(now, MSG_TYPE.LINK_BLOCKED, nodeId);
//		final ArrayList<InformationEntity> ies = this.beliefs.getInfos().get(MSG_TYPE.MY_NEXT_LINK);
		if (ies.size() == 0) {
			return;
		}
		
		for (final InformationEntity ie : ies) {
			this.blockedLinks.add(((LinkBlockedMessage)ie.getMsg()).getLink());
		}
		
	}

	public void setCoefficient(final double coef) {
		this.coef  = coef;
		
	}
	
	
}
