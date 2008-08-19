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

package playground.gregor.withinday_evac.analyzer;

import java.util.HashSet;

import org.matsim.network.Link;
import org.matsim.population.Plan;

import playground.gregor.withinday_evac.Beliefs;

public class FollowPlanAnalyzer implements Analyzer {

	private final Beliefs beliefs;
	private final Link[] links;
	private final HashSet<Link> linksSet = new HashSet<Link>();
	double coef = 1;
	private final Link startLink;
	private final Link destLink;

	public FollowPlanAnalyzer(final Beliefs beliefs, final Plan plan) {
		this.beliefs = beliefs;
		this.links = plan.getNextLeg(plan.getFirstActivity()).getRoute().getLinkRoute();
		this.startLink = plan.getFirstActivity().getLink();
		this.destLink = plan.getNextActivity(plan.getNextLeg(plan.getFirstActivity())).getLink(); 
		initLinkHashSet();
	}

	private void initLinkHashSet() {
		for (Link link : this.links) {
			this.linksSet.add(link);
		}
	}

	public NextLinkOption getAction(final double now) {
		Link link = this.beliefs.getCurrentLink();
		if (link == this.startLink) {
			return new NextLinkOption(this.links[0],1 * this.coef);
		}
		
		
		if (!this.linksSet.contains(link)) {
			this.linksSet.clear();
			return null;
		}
		for (int i = 0; i < this.links.length; i++) {
			if (this.links[i] == link) {
				if (i > this.links.length-2){
					return new NextLinkOption(this.destLink,1 * this.coef);
				}
				return new NextLinkOption(this.links[i+1],1 * this.coef); 
			}
		}
		throw new RuntimeException("this should not happen!!");
	}

	public void setCoefficient(final double coef) {
		this.coef = coef;
		
	}
	

}
