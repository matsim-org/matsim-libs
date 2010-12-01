/* *********************************************************************** *
 * project: org.matsim.*
 * LinkPenaltyTravelCost.java
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
package playground.gregor.sims.shelters.linkpenaltyII;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

public class LinkPenaltyTravelCost implements TravelCost {

	
	private final TravelTime tt;
	private final ShelterInputCounterLinkPenalty lp;

	public LinkPenaltyTravelCost(TravelTime tt, ShelterInputCounterLinkPenalty lp) {
		this.tt = tt;
		this.lp = lp;
	}
	
	public double getLinkGeneralizedTravelCost(Link link, double time) {
			return this.tt.getLinkTravelTime(link, time) + this.lp.getLinkTravelCost(link, time);
	}

}
