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

package playground.anhorni.surprice;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.roadpricing.RoadPricingSchemeImpl.Cost;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vehicles.Vehicle;

/**
 * Calculates the travel disutility for links, including tolls. Currently supports distance, cordon and area tolls.
 *
 * @author mrieser
 */
public class SurpriceTravelDisutilityIncludingToll implements TravelDisutility {

	/*package*/ final RoadPricingScheme scheme;
	private final TollRouterBehaviour tollCostHandler;
	private final TravelDisutility travelDisutilityHandler;
	private ObjectAttributes preferences;
	

	public SurpriceTravelDisutilityIncludingToll(final TravelDisutility travelDisutilityCalculator, final RoadPricingScheme scheme,
			ObjectAttributes preferences) {
		this.scheme = scheme;
		this.preferences = preferences;
		this.travelDisutilityHandler = travelDisutilityCalculator;
		if (RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(scheme.getType())) {
			this.tollCostHandler = new DistanceTollCostBehaviour();
		} else if (scheme.getType() == RoadPricingScheme.TOLL_TYPE_AREA) {
			this.tollCostHandler = new AreaTollCostBehaviour();
		} else if (scheme.getType() == RoadPricingScheme.TOLL_TYPE_CORDON) {
			this.tollCostHandler = new CordonTollCostBehaviour();
		} else if (scheme.getType() == RoadPricingScheme.TOLL_TYPE_LINK) {
			this.tollCostHandler = new LinkTollCostBehaviour();
		} else {
			throw new IllegalArgumentException("RoadPricingScheme of type \"" + scheme.getType() + "\" is not supported.");
		}
	}
	
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		double baseDisutility = this.travelDisutilityHandler.getLinkTravelDisutility(link, time, person, vehicle);
		double dudm = (Double)this.preferences.getAttribute(person.getId().toString(), "dudm");
		double tollDisutility = dudm * this.tollCostHandler.getTollCost(link, time, person);
		return baseDisutility + tollDisutility;
	}
	
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return this.travelDisutilityHandler.getLinkMinimumTravelDisutility(link);
	}

	private interface TollRouterBehaviour {
		public double getTollCost(Link link, double time, Person person);
	}

	/*package*/ class DistanceTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTollCost(final Link link, final double time, Person person) {
			Cost cost_per_m = SurpriceTravelDisutilityIncludingToll.this.scheme.getLinkCostInfo(link.getId(), time, person.getId());
			if (cost_per_m == null) {
				return 0.0;
			}
			return cost_per_m.amount * link.getLength();
		}
	}

	private static int wrnCnt2 = 0 ;
	
	/*package*/ class AreaTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTollCost(final Link link, final double time, Person person) {
			RoadPricingSchemeImpl.Cost cost = SurpriceTravelDisutilityIncludingToll.this.scheme.getLinkCostInfo(link.getId(), time, person.getId());
			if (cost == null) {
				return 0.0;
			}
			/* just return some really high costs for tolled links, so that still a
			 * route could be found if there is no other possibility.
			 */
			if ( wrnCnt2 < 1 ) {
				wrnCnt2 ++ ;
				Logger.getLogger(this.getClass()).warn("at least here, the area toll does not use the true toll value. " +
						"This may work anyways, but without more explanation it is not obvious to me.  kai, mar'11") ;
			}
			return 1000;
		}
	}

	/*package*/ class CordonTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTollCost(final Link link, final double time, Person person) {
			RoadPricingSchemeImpl.Cost cost = SurpriceTravelDisutilityIncludingToll.this.scheme.getLinkCostInfo(link.getId(), time, person.getId());
			if (cost == null) {
				return 0.0;
			}
			return cost.amount;
		}
	}
	
	class LinkTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTollCost(final Link link, final double time, Person person) {
			Cost cost_per_m = SurpriceTravelDisutilityIncludingToll.this.scheme.getLinkCostInfo(link.getId(), time, person.getId());
			if (cost_per_m == null) {
				return 0.0;
			}
			return cost_per_m.amount;
		}
	}

}
