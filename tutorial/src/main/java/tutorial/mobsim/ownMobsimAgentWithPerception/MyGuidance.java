/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package tutorial.mobsim.ownMobsimAgentWithPerception;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

/**
 * @author nagel
 *
 */
class MyGuidance {
	
	private MyObserver observer;
	private Scenario scenario;


	MyGuidance( MyObserver observer, Scenario sc ) {
		this.observer = observer ;
		this.scenario = sc ;
	}

	public Id<Link> getBestOutgoingLink(Id<Link> linkId) {
		Link currentLink = this.scenario.getNetwork().getLinks().get( linkId ) ;
		Node outNode = currentLink.getToNode() ;
		Map<Id<Link>, ? extends Link> outLinks = outNode.getOutLinks() ;
		Id<Link> bestLinkId = null ;
		double bestLinkCongestion = Double.POSITIVE_INFINITY ;
		for ( Link outLink : outLinks.values() ) {
			if ( this.observer.congestionLevel( outLink.getId() ) < bestLinkCongestion ) {
				bestLinkCongestion = this.observer.congestionLevel( outLink.getId() ) ;
				bestLinkId = outLink.getId();
			}
		}
		return bestLinkId ;
	}
	


}
