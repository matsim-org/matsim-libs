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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.roadpricing.RoadPricingSchemeImpl.Cost;
import org.matsim.vehicles.Vehicle;

/**
 * Calculates the travel disutility for links, including tolls. Currently supports distance, cordon and area tolls.
 *
 * @author mrieser
 */
public class TravelDisutilityIncludingToll implements TravelDisutility {

	/*package*/ final RoadPricingScheme scheme;
	private final TollRouterBehaviour tollCostHandler;
	private final TravelDisutility normalTravelDisutility;
	private final double marginalUtilityOfMoney;
	private static int utlOfMoneyWrnCnt = 0 ;

	public TravelDisutilityIncludingToll(final TravelDisutility normalTravelDisutility, final RoadPricingScheme scheme, Config config) {
		this( normalTravelDisutility, scheme, config.planCalcScore().getMarginalUtilityOfMoney() ) ;
	}
	public TravelDisutilityIncludingToll(final TravelDisutility normalTravelDisutility, final RoadPricingScheme scheme, 
			double marginalUtilityOfMoney ) {

		this.scheme = scheme;
		this.normalTravelDisutility = normalTravelDisutility;
		if (RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(scheme.getType())) {
			this.tollCostHandler = new DistanceTollCostBehaviour();
		} else if (scheme.getType() == RoadPricingScheme.TOLL_TYPE_AREA) {
			this.tollCostHandler = new AreaTollCostBehaviour();
			Logger.getLogger(this.getClass()).warn("area pricing is (even) more brittle than the other toll schemes; " +
					"make sure you know what you are doing.  kai, apr'13") ;
		} else if (scheme.getType() == RoadPricingScheme.TOLL_TYPE_CORDON) {
			this.tollCostHandler = new CordonTollCostBehaviour();
		} else if (scheme.getType() == RoadPricingScheme.TOLL_TYPE_LINK) {
			this.tollCostHandler = new LinkTollCostBehaviour();
		} else {
			throw new IllegalArgumentException("RoadPricingScheme of type \"" + scheme.getType() + "\" is not supported.");
		}
		this.marginalUtilityOfMoney = marginalUtilityOfMoney ;
		if ( utlOfMoneyWrnCnt < 1 && this.marginalUtilityOfMoney != 1. ) {
			utlOfMoneyWrnCnt ++ ;
			Logger.getLogger(this.getClass()).warn("There are no test cases for marginalUtilityOfMoney != 1.  Please write one " +
					"and delete this message.  kai, apr'13 ") ;
		}
	}
	
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		double normalTravelDisutilityForLink = this.normalTravelDisutility.getLinkTravelDisutility(link, time, person, vehicle);
		double tollCost = this.tollCostHandler.getTollCost(link, time, person, vehicle);
		return normalTravelDisutilityForLink + tollCost*this.marginalUtilityOfMoney ;
		// sign convention: these are all costs (= disutilities), so they are all normally positive.  tollCost is positive, marginalUtilityOfMoney as well.
	}
	
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return this.normalTravelDisutility.getLinkMinimumTravelDisutility(link);
	}

	private interface TollRouterBehaviour {
		public double getTollCost(Link link, double time, Person person, Vehicle vehicle);
	}

	/*package*/ class DistanceTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTollCost(final Link link, final double time, Person person, Vehicle vehicle) {
			// not sure why it is the object at one level, and the id on the next level.
			// In any case, retrofitting this so it accepts null arguments.  kai, apr'14
			Id personId = null ;
			if ( person != null ) {
				personId = person.getId();
			}
			Id vehicleId = null ;
			if ( vehicle != null ) {
				vehicleId = vehicle.getId();
			}
			Cost cost_per_m = TravelDisutilityIncludingToll.this.scheme.getLinkCostInfo(link.getId(), time, personId, vehicleId );
			if (cost_per_m == null) {
				return 0.0;
			}
			return cost_per_m.amount * link.getLength();
		}
	}

	private static int wrnCnt2 = 0 ;
	
	/*package*/ class AreaTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTollCost(final Link link, final double time, Person person, Vehicle vehicle) {
			// not sure why it is the object at one level, and the id on the next level.
			// In any case, retrofitting this so it accepts null arguments.  kai, apr'14
			Id personId = null ;
			if ( person != null ) {
				personId = person.getId();
			}
			Id vehicleId = null ;
			if ( vehicle != null ) {
				vehicleId = vehicle.getId();
			}
			RoadPricingSchemeImpl.Cost cost = TravelDisutilityIncludingToll.this.scheme.getLinkCostInfo(link.getId(), time, personId, vehicleId );
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
		public double getTollCost(final Link link, final double time, Person person, Vehicle vehicle) {
			// not sure why it is the object at one level, and the id on the next level.
			// In any case, retrofitting this so it accepts null arguments.  kai, apr'14
			Id personId = null ;
			if ( person != null ) {
				personId = person.getId();
			}
			Id vehicleId = null ;
			if ( vehicle != null ) {
				vehicleId = vehicle.getId();
			}
			RoadPricingSchemeImpl.Cost cost = TravelDisutilityIncludingToll.this.scheme.getLinkCostInfo(link.getId(), time, personId, vehicleId );
			if (cost == null) {
				return 0.0;
			}
			return cost.amount;
		}
	}
	
	class LinkTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTollCost(final Link link, final double time, Person person, Vehicle vehicle) {
			// not sure why it is the object at one level, and the id on the next level.
			// In any case, retrofitting this so it accepts null arguments.  kai, apr'14
			Id personId = null ;
			if ( person != null ) {
				personId = person.getId();
			}
			Id vehicleId = null ;
			if ( vehicle != null ) {
				vehicleId = vehicle.getId();
			}
			Cost cost_per_m = TravelDisutilityIncludingToll.this.scheme.getLinkCostInfo(link.getId(), time, personId, vehicleId );
			if (cost_per_m == null) {
				return 0.0;
			}
			return cost_per_m.amount;
		}
	}

}
