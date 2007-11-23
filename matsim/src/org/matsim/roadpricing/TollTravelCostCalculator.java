/* *********************************************************************** *
 * project: org.matsim.*
 * TollTravelCostCalculator.java
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

package org.matsim.roadpricing;

import org.matsim.network.Link;
import org.matsim.roadpricing.RoadPricingScheme.Cost;
import org.matsim.router.util.TravelCostI;

public class TollTravelCostCalculator implements TravelCostI {

	private final RoadPricingScheme scheme;
	private final TollRouterBehaviour tollCostHandler;
	private final TravelCostI costHandler;

	public TollTravelCostCalculator(final TravelCostI costCalculator, final RoadPricingScheme scheme) {
		this.scheme = scheme;
		this.costHandler = costCalculator;

		if (scheme.getType() == "distance") this.tollCostHandler = new DistanceTollCostBehaviour();
		else if (scheme.getType() == "area") this.tollCostHandler = new AreaTollCostBehaviour();
		else if (scheme.getType() == "cordon") this.tollCostHandler = new CordonTollCostBehaviour();
		else {
			throw new IllegalArgumentException("RoadPricingScheme of type \"" + scheme + "\" is not supported.");
		}

	}

	public double getLinkTravelCost(final Link link, final double time) {
		double baseCost = this.costHandler.getLinkTravelCost(link, time);
		double tollCost = this.tollCostHandler.getTollCost(link, time);
		return baseCost + tollCost;
	}

	private interface TollRouterBehaviour {
		public double getTollCost(Link link, double time);
	}

	private class DistanceTollCostBehaviour implements TollRouterBehaviour {
		public double getTollCost(final Link link, final double time) {
			Cost cost = TollTravelCostCalculator.this.scheme.getLinkCost(link.getId(), time);
			if (cost == null) {
				return 0.0;
			}
			return cost.amount * link.getLength();
		}
	}

	private class AreaTollCostBehaviour implements TollRouterBehaviour {
		public double getTollCost(final Link link, final double time) {
			RoadPricingScheme.Cost cost = TollTravelCostCalculator.this.scheme.getLinkCost(link.getId(), time);
			if (cost == null) {
				return 0.0;
			}
			/* just return some really high costs for tolled links, so that still a
			 * route could be found if there is no other possibility.
			 */
			return 1000;
		}
	}

	private class CordonTollCostBehaviour implements TollRouterBehaviour {
		public double getTollCost(final Link link, final double time) {
			RoadPricingScheme.Cost cost = TollTravelCostCalculator.this.scheme.getLinkCost(link.getId(), time);
			if (cost == null) {
				return 0.0;
			}
			/* just return some really high costs for tolled links, so that still a
			 * route could be found if there is no other possibility.
			 */
			return cost.amount;
		}
	}

}
