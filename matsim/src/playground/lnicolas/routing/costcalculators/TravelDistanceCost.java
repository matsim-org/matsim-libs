/* *********************************************************************** *
 * project: org.matsim.*
 * TravelDistanceCost.java
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

package playground.lnicolas.routing.costcalculators;

import org.matsim.network.Link;
import org.matsim.router.util.TravelMinCostI;

/**
 * @author niclefeb
 * CostCalculator for Links based on length of the links
 */
public class TravelDistanceCost implements TravelMinCostI {

	public TravelDistanceCost() {
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.network.TravelCostI#getLinkTravelCost(org.matsim.network.Link, int)
	 */
	public double getLinkTravelCost(Link link, double time) {
		return link.getLength();
	}

	/* (non-Javadoc)
	 * @see org.matsim.network.TravelCostI#getLinkTravelTime(org.matsim.network.Link, int)
	 */
	public double getLinkTravelTime(Link link, double time) {
		return (link.getLength() / link.getFreespeed());
	}

	public double getLinkMinimumTravelCost(Link link) {
		return link.getLength();
	}

}
