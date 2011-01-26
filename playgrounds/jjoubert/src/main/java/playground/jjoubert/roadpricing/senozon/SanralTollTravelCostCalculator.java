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

package playground.jjoubert.roadpricing.senozon;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingScheme.Cost;

/**
 * Calculates the travel costs for links, including tolls. Currently supports distance, cordon and area tolls.
 *
 * @author mrieser
 */
public class SanralTollTravelCostCalculator implements PersonalizableTravelCost {

	/*package*/ final RoadPricingScheme scheme;
	private final TollRouterBehaviour tollCostHandler;
	private final PersonalizableTravelCost costHandler;
	private Person person = null;

	public SanralTollTravelCostCalculator(final PersonalizableTravelCost costCalculator, final RoadPricingScheme scheme) {
		this.scheme = scheme;
		this.costHandler = costCalculator;

		if (RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(scheme.getType())) this.tollCostHandler = new DistanceTollCostBehaviour();
		else if (scheme.getType() == RoadPricingScheme.TOLL_TYPE_AREA) this.tollCostHandler = new AreaTollCostBehaviour();
		else if (scheme.getType() == RoadPricingScheme.TOLL_TYPE_CORDON) this.tollCostHandler = new CordonTollCostBehaviour();
		else {
			throw new IllegalArgumentException("RoadPricingScheme of type \"" + scheme + "\" is not supported.");
		}

	}

	@Override
	public double getLinkGeneralizedTravelCost(final Link link, final double time) {
		double baseCost = this.costHandler.getLinkGeneralizedTravelCost(link, time);
		double tollCost = this.tollCostHandler.getTollCost(link, time);
		return baseCost + tollCost;
	}

	private interface TollRouterBehaviour {
		public double getTollCost(Link link, double time);
	}

	/*package*/ class DistanceTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTollCost(final Link link, final double time) {
			Cost cost = SanralTollTravelCostCalculator.this.scheme.getLinkCost(link.getId(), time);
			if (cost == null) {
				return 0.0;
			}
			return cost.amount * link.getLength() * SanralTollFactor.getTollFactor(person.getId(), link.getId(), time);
		}
	}

	/*package*/ class AreaTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTollCost(final Link link, final double time) {
			RoadPricingScheme.Cost cost = SanralTollTravelCostCalculator.this.scheme.getLinkCost(link.getId(), time);
			if (cost == null) {
				return 0.0;
			}
			/* just return some really high costs for tolled links, so that still a
			 * route could be found if there is no other possibility.
			 */
			return 1000;
		}
	}

	/*package*/ class CordonTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTollCost(final Link link, final double time) {
			RoadPricingScheme.Cost cost = SanralTollTravelCostCalculator.this.scheme.getLinkCost(link.getId(), time);
			if (cost == null) {
				return 0.0;
			}
			return cost.amount * SanralTollFactor.getTollFactor(person.getId(), link.getId(), time);
		}
	}

	@Override
	public void setPerson(Person person) {
		this.costHandler.setPerson(person);
		this.person = person;
	}

}
