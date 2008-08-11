/* *********************************************************************** *
 * project: org.matsim.*
 * BDIAgent.java
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

package playground.gregor.withinday_evac;

import java.util.Collection;
import java.util.HashMap;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Person;

import playground.gregor.withinday_evac.analyzer.Analyzer;
import playground.gregor.withinday_evac.analyzer.BlockedLinksAnalyzer;
import playground.gregor.withinday_evac.analyzer.DestinationReachedAnalyzer;
import playground.gregor.withinday_evac.analyzer.FollowGuideAnalyzer;
import playground.gregor.withinday_evac.analyzer.HerdAnalyzer;
import playground.gregor.withinday_evac.analyzer.ReRouteAnalyzer;
import playground.gregor.withinday_evac.communication.FollowGuideMessage;
import playground.gregor.withinday_evac.communication.InformationEntity;
import playground.gregor.withinday_evac.communication.InformationExchanger;
import playground.gregor.withinday_evac.communication.InformationStorage;
import playground.gregor.withinday_evac.communication.Message;
import playground.gregor.withinday_evac.communication.NextLinkMessage;
import playground.gregor.withinday_evac.mobsim.OccupiedVehicle;


public class BDIAgent {

	private final Person person;
	private final OccupiedVehicle vehicle;
	private final InformationExchanger informationExchanger;
	private final Beliefs beliefs;
	private final boolean isBDIAgent;
	private final DecisionMaker decisionMaker;
	private final Intentions intentions;

	public BDIAgent(final Person person, final OccupiedVehicle v, final InformationExchanger informationExchanger, final NetworkLayer networkLayer, boolean isBDI){
		this.person = person;
		this.vehicle = v;
		this.vehicle.setAgent(this);
		this.informationExchanger = informationExchanger;
		this.beliefs = new Beliefs();
		this.intentions = new Intentions();
		this.intentions.setDestination(networkLayer.getNode(new IdImpl("en2")));
		final HashMap<String,Analyzer> analyzers = getAnalyzer(networkLayer);
		this.decisionMaker = new DecisionMaker(analyzers);
		this.isBDIAgent = isBDI;

		
	}

	private HashMap<String, Analyzer> getAnalyzer(final NetworkLayer networkLayer) {
		final HashMap<String,Analyzer> analyzers = new HashMap<String,Analyzer>();
		analyzers.put("FollowGuideAnalyzer", new FollowGuideAnalyzer(this.beliefs));
		analyzers.put("HerdAnalyzer", new HerdAnalyzer(this.beliefs));
		analyzers.put("BlockedLinksAnalyzer", new BlockedLinksAnalyzer(this.beliefs));
		analyzers.put("ReRouteAnalyzer", new ReRouteAnalyzer(this.beliefs,networkLayer,this.intentions));
		analyzers.put("DestinationReachedAnalyzer", new DestinationReachedAnalyzer(this.beliefs,this.intentions));
		// TODO Auto-generated method stub
		return analyzers;
	}

	public Link replan(final double now, final Id nodeId) {
		
		final InformationStorage infos = this.informationExchanger.getInformationStorage(nodeId);
		
		updateBeliefs(infos.getInformation(now));
		
		Link nextLink;
		if (!this.isBDIAgent) {//guides
			nextLink = this.vehicle.chooseNextLink();
			final Message msg = new FollowGuideMessage(nextLink);
			final InformationEntity ie = new InformationEntity(now,InformationEntity.MSG_TYPE.FOLLOW_ME,msg);
			infos.addInformationEntity(ie);
		} else {
			nextLink = this.decisionMaker.chooseNextLink(now,nodeId);
//			if (nextLink == null) { //if now guide then choose random link
//				final Node n = this.vehicle.getCurrentNode();
//				double prob_sum = n.getOutLinks().size() * Gbl.random.nextDouble(); 
//				for (final Link l : n.getOutLinks().values()) {
//					if (--prob_sum <= 0) {
//						nextLink = l;
//						break;
//					}
//				}
//			}
			
		}
		
		final Message msg = new NextLinkMessage(nextLink);
		final InformationEntity ie = new InformationEntity(now,InformationEntity.MSG_TYPE.MY_NEXT_LINK,msg);
		infos.addInformationEntity(ie);
		return nextLink;
		
	}

	
	
	private void updateBeliefs(final Collection<InformationEntity> information) {
		this.beliefs.update(information);
		this.beliefs.setCurrentLink(this.vehicle.getCurrentLink());
		
	}
}
