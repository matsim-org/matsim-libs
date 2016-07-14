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

package playground.jjoubert.roadpricing.senozon.routing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.roadpricing.RoadPricingSchemeImpl.Cost;
import org.matsim.vehicles.Vehicle;

import playground.jjoubert.roadpricing.senozon.SanralTollFactor;

/**
 * Calculates the travel costs for links, including tolls. Currently supports distance, cordon and area tolls.
 *
 * @author mrieser
 */
public class SanralTravelDisutilityIncludingToll implements TravelDisutility {

	private final RoadPricingScheme scheme;
	private final TollRouterBehaviour tollCostHandler;
	private final TravelDisutility costHandler;

	public SanralTravelDisutilityIncludingToll(final TravelDisutility costCalculator, final RoadPricingScheme scheme) {
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
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		double baseCost = this.costHandler.getLinkTravelDisutility(link, time, person, vehicle);
		double tollCost = this.tollCostHandler.getTollCost(link, time, person, vehicle);
		return baseCost + tollCost;
	}
	
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	private interface TollRouterBehaviour {
		public double getTollCost(Link link, double time, Person person, Vehicle vehicle);
	}

	/*package*/ class DistanceTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTollCost(final Link link, final double time, final Person person, Vehicle vehicle) {
			Cost cost = SanralTravelDisutilityIncludingToll.this.scheme.getLinkCostInfo(link.getId(), time, person.getId(), vehicle.getId());
			if (cost == null) {
				return 0.0;
			}
			return cost.amount * link.getLength() * SanralTollFactor.getTollFactor(person.getId(), link.getId(), time);
		}
	}

	/*package*/ class AreaTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTollCost(final Link link, final double time, final Person person, Vehicle vehicle) {
			RoadPricingSchemeImpl.Cost cost = SanralTravelDisutilityIncludingToll.this.scheme.getLinkCostInfo(link.getId(), time, person.getId(), vehicle.getId());
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
		public double getTollCost(final Link link, final double time, final Person person, Vehicle vehicle) {
			RoadPricingSchemeImpl.Cost cost = SanralTravelDisutilityIncludingToll.this.scheme.getLinkCostInfo(link.getId(), time, person.getId(), vehicle.getId());
			if (cost == null) {
				return 0.0;
			}
			return cost.amount * SanralTollFactor.getTollFactor(person.getId(), link.getId(), time);
		}
	}

}
