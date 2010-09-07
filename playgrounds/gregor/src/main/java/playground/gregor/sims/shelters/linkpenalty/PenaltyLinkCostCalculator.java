/* *********************************************************************** *
 * project: org.matsim.*
 * PenaltyLinkCostCalculator.java
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
package playground.gregor.sims.shelters.linkpenalty;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;


public class PenaltyLinkCostCalculator implements TravelCost{

	private final TravelTimeCalculator tc;
	private final ShelterInputCounter sc;
	public PenaltyLinkCostCalculator(TravelTimeCalculator tc, ShelterInputCounter sc) {
		this.tc = tc;
		this.sc = sc;
	}
	public double getLinkTravelCost(Link link, double time) {
		return this.tc.getLinkTravelTime(link, time) + this.sc.getLinkPenalty(link, time);
	}
	

}
