/* *********************************************************************** *
 * project: org.matsim.*
 * FollowPlanAnalyzer.java
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

import java.util.HashSet;

import org.matsim.network.Link;
import org.matsim.population.Plan;
import org.matsim.population.routes.CarRoute;

import playground.gregor.withindayevac.Beliefs;
import playground.gregor.withindayevac.communication.InformationExchanger;

public class FollowPlanAnalyzer implements Analyzer {

	private final Beliefs beliefs;
	private final Link[] links;
	private final HashSet<Link> linksSet = new HashSet<Link>();
	double coef = 1;
	private final Link startLink;
	private final Link destLink;
	private final double estArivalTime;
	private final InformationExchanger informationExchanger;
	private final boolean isScored;

	public FollowPlanAnalyzer(final Beliefs beliefs, final Plan plan, final InformationExchanger informationExchanger) {
		this.beliefs = beliefs;
		this.informationExchanger = informationExchanger;
		this.links = ((CarRoute) plan.getNextLeg(plan.getFirstActivity()).getRoute()).getLinks();
		this.startLink = plan.getFirstActivity().getLink();
		if (plan.getScore() == Plan.UNDEF_SCORE){
			this.isScored = false;
			this.estArivalTime = Double.POSITIVE_INFINITY;
		} else {
			this.isScored = true;
			this.estArivalTime = plan.getFirstActivity().getEndTime() + plan.getScore() * -600;
		}
		this.destLink = plan.getNextActivity(plan.getNextLeg(plan.getFirstActivity())).getLink(); 
		initLinkHashSet();
	}

	private void initLinkHashSet() {
		for (Link link : this.links) {
			this.linksSet.add(link);
		}
	}

	public Option getAction(final double now) {
		Link link = this.beliefs.getCurrentLink();
		if (link == this.startLink) {
			if (this.isScored) {
				return new NextLinkWithEstimatedTravelTimeOption(this.links[0],1 * this.coef,this.estArivalTime-now);
			} else {
				return new NextLinkOption(this.links[0],1 * this.coef);
			}
		}
		
		
		if (!this.linksSet.contains(link)) {
			this.linksSet.clear();
			return null;
		}
		for (int i = 0; i < this.links.length; i++) {
			if (this.links[i] == link) {
				if (i > this.links.length-2){
//					//TEST
//					Message m = new NextLinkWithEstimatedTravelTimeMessage(this.destLink,this.estArivalTime - now);
//					InformationEntity ie = new InformationEntity(now,InformationEntity.MSG_TYPE.MY_NEXT_LINK_W_EST_TRAVELTIME,m);
//					this.informationExchanger.getInformationStorage(link.getToNode().getId()).addInformationEntity(ie);
					if (this.isScored) {
						return new NextLinkWithEstimatedTravelTimeOption(this.destLink,1 * this.coef, this.estArivalTime-now);
					}
					return new NextLinkOption(this.destLink,1 * this.coef);
				}
//				//TEST
//				Message m = new NextLinkWithEstimatedTravelTimeMessage(this.links[i+1],this.estArivalTime - now);
//				InformationEntity ie = new InformationEntity(now,InformationEntity.MSG_TYPE.MY_NEXT_LINK_W_EST_TRAVELTIME,m);
//				this.informationExchanger.getInformationStorage(link.getToNode().getId()).addInformationEntity(ie);
				if (this.isScored){
					return new NextLinkWithEstimatedTravelTimeOption(this.links[i+1],1 * this.coef, this.estArivalTime-now); 
				} else {
					return new NextLinkOption(this.links[i+1],1 * this.coef);
				}
				
			}
		}
		throw new RuntimeException("this should not happen!!");
	}

	public void setCoefficient(final double coef) {
		this.coef = coef;
		
	}
	

}
