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

package playground.gregor.withindayevac;

import java.util.Collection;

import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.mobsim.queuesim.PersonAgent;
import org.matsim.mobsim.queuesim.SimulationTimer;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Person;

import playground.gregor.withindayevac.analyzer.NextLinkWithEstimatedTravelTimeOption;
import playground.gregor.withindayevac.analyzer.Option;
import playground.gregor.withindayevac.communication.InformationEntity;
import playground.gregor.withindayevac.communication.InformationExchanger;
import playground.gregor.withindayevac.communication.InformationStorage;
import playground.gregor.withindayevac.communication.Message;
import playground.gregor.withindayevac.communication.NextLinkMessage;
import playground.gregor.withindayevac.communication.NextLinkWithEstimatedTravelTimeMessage;


public class BDIAgent extends PersonAgent {

	private final InformationExchanger informationExchanger;
	private final Beliefs beliefs;
//	private final DecisionMaker decisionMaker;
	private final Intentions intentions;
	private boolean isGuide;
	private final DecisionTree decisionTree;
	private final int iteration;

	public BDIAgent(final Person person, final InformationExchanger informationExchanger, final NetworkLayer networkLayer, final int iteration){
		super(person);
		this.informationExchanger = informationExchanger;
		this.beliefs = new Beliefs(this.informationExchanger);
		this.intentions = new Intentions();
		this.intentions.setDestination(networkLayer.getNode(new IdImpl("en2")));
		this.iteration = iteration;
		this.decisionTree = new DecisionTree(this.beliefs,this.getPerson().getSelectedPlan(),this.intentions,networkLayer, iteration, this.informationExchanger);
		
		if (person.getId().toString().contains("guide")) {
			this.isGuide = true;
		} else {
			this.isGuide = false;
		}
	}

	@Override
	public Link chooseNextLink() {
		return this.replan(SimulationTimer.getTime(), this.getCurrentLink().getToNode().getId());
	}
	

	private Link replan(final double now, final Id nodeId) {
		
		final InformationStorage infos = this.informationExchanger.getInformationStorage(nodeId);
		
		updateBeliefs(infos.getInformation(now));
		

		Option nextOption = this.decisionTree.getNextOption(now);
		Link nextLink = nextOption.getNextLink();

		
		if (nextOption instanceof NextLinkWithEstimatedTravelTimeOption) {
			Message msg = new NextLinkWithEstimatedTravelTimeMessage(nextLink,((NextLinkWithEstimatedTravelTimeOption)nextOption).getEstTTime());
			final InformationEntity ie = new InformationEntity(60,now,InformationEntity.MSG_TYPE.MY_NEXT_LINK_W_EST_TRAVELTIME,msg);
			infos.addInformationEntity(ie);
			
			
		}
		
//		if (nextLink != null && this.isGuide) {
//			this.isGuide = false;
//		} 
//		if (this.isGuide && nextLink == null) {
//			nextLink = this.chooseNextLink();
//		}
//		
//		if (this.isGuide) {
//			final Message msg = new FollowGuideMessage(nextLink);
////			final InformationEntity ie = new InformationEntity(now,InformationEntity.MSG_TYPE.FOLLOW_ME,msg);
//			final InformationEntity ie = new InformationEntity(30*3600,now,InformationEntity.MSG_TYPE.FOLLOW_ME,msg);
//			infos.addInformationEntity(ie);			
//		}

		

		final Message msg = new NextLinkMessage(nextLink);
		final InformationEntity ie = new InformationEntity(now,InformationEntity.MSG_TYPE.MY_NEXT_LINK,msg);
		infos.addInformationEntity(ie);
		
		return nextLink;
		
	}

	
	
	private void updateBeliefs(final Collection<InformationEntity> information) {
//		this.beliefs.update(information);
		this.beliefs.setCurrentLink(this.getCurrentLink());
		
	}
}
