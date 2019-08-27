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

package org.matsim.contrib.roadpricing;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

/**
 * Calculates the travel disutility for links, including tolls. Currently 
 * supports distance, cordon and area tolls.
 *
 * @author mrieser
 */
class TravelDisutilityIncludingToll implements TravelDisutility {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger( TravelDisutilityIncludingToll.class ) ;

	@Inject
	Scenario scenario;
	@Inject
	RoadPricingScheme scheme;
	private final TollRouterBehaviour tollCostHandler;
	private final TravelDisutility normalTravelDisutility;
	private final double marginalUtilityOfMoney;
	private final double sigma ;

	private static int utlOfMoneyWrnCnt = 0 ;

	TravelDisutilityIncludingToll(final TravelDisutility normalTravelDisutility, final RoadPricingScheme scheme, Config config)
////	TravelDisutilityIncludingToll(final TravelDisutility normalTravelDisutility, Config config)
	{
		this( normalTravelDisutility, scheme, config.planCalcScore().getMarginalUtilityOfMoney(), 0. ) ;
		// this is using sigma=0 for backwards compatibility (not sure how often this is needed)
	}

	TravelDisutilityIncludingToll(final TravelDisutility normalTravelDisutility, final RoadPricingScheme scheme,
			double marginalUtilityOfMoney, double sigma ) {

//		this.scheme = RoadPricingUtils.getScheme(scenario);

		this.scheme = scheme;

		Gbl.assertNotNull(this.scheme);

		this.normalTravelDisutility = normalTravelDisutility;
		if (RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(this.scheme.getType())) {
			this.tollCostHandler = new DistanceTollCostBehaviour();
		} else if (this.scheme.getType() == RoadPricingScheme.TOLL_TYPE_AREA) {
			this.tollCostHandler = new AreaTollCostBehaviour();
			Logger.getLogger(this.getClass()).warn("area pricing is more brittle than the other toll schemes; " +
					"make sure you know what you are doing.  kai, apr'13 & sep'14") ;
		} else if (this.scheme.getType() == RoadPricingScheme.TOLL_TYPE_CORDON) {
			this.tollCostHandler = new CordonTollCostBehaviour();
		} else if (this.scheme.getType() == RoadPricingScheme.TOLL_TYPE_LINK) {
			this.tollCostHandler = new LinkTollCostBehaviour();
		} else {
			throw new IllegalArgumentException("RoadPricingScheme of type \"" + this.scheme.getType() + "\" is not supported.");
		}
		this.marginalUtilityOfMoney = marginalUtilityOfMoney ;
		if ( utlOfMoneyWrnCnt < 1 && this.marginalUtilityOfMoney != 1. ) {
			utlOfMoneyWrnCnt ++ ;
			Logger.getLogger(this.getClass()).warn("There are no test cases for marginalUtilityOfMoney != 1.  Please write one " +
					"and delete this message.  kai, apr'13 ") ;
		}

		this.sigma = sigma ;
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) 
	{
		double normalTravelDisutilityForLink = this.normalTravelDisutility.getLinkTravelDisutility(link, time, person, vehicle);

		double logNormalRnd = 1. ;
		// randomize if applicable:
		if ( sigma != 0. ) {
			logNormalRnd = (double) person.getCustomAttributes().get("logNormalRnd") ;
		}
		// end randomize

		double tollCost = this.tollCostHandler.getTypicalTollCost(link, time );
		
		return normalTravelDisutilityForLink + tollCost*this.marginalUtilityOfMoney*logNormalRnd ;
		// sign convention: these are all costs (= disutilities), so they are all normally positive.  tollCost is positive, marginalUtilityOfMoney as well.
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return this.normalTravelDisutility.getLinkMinimumTravelDisutility(link);
	}

	private interface TollRouterBehaviour {
		public double getTypicalTollCost(Link link, double time);
	}

	/*package*/ class DistanceTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTypicalTollCost(final Link link, final double time) {
			RoadPricingCost cost_per_m = scheme.getTypicalLinkCostInfo(link.getId(), time );
			if (cost_per_m == null) {
				return 0.0;
			}
			return cost_per_m.amount * link.getLength();
		}
	}

	private static int wrnCnt2 = 0 ;

	/*package*/ class AreaTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTypicalTollCost(final Link link, final double time) {
			RoadPricingCost cost = scheme.getTypicalLinkCostInfo(link.getId(), time );
			if (cost == null) {
				return 0.0;
			}
			/* just return some really high costs for tolled links, so that still a
			 * route could be found if there is no other possibility. */
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
		public double getTypicalTollCost(final Link link, final double time) {
			RoadPricingCost cost = scheme.getTypicalLinkCostInfo(link.getId(), time );
			if (cost == null) {
				return 0.0;
			}
			return cost.amount;
		}
	}

	/* package */ class LinkTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTypicalTollCost(final Link link, final double time) {
			RoadPricingCost cost = scheme.getTypicalLinkCostInfo(link.getId(), time );
			if (cost == null) {
				return 0.0;
			}
			return cost.amount;
		}
	}

}
