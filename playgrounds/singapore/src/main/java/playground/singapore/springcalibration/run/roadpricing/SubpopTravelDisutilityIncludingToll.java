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

package playground.singapore.springcalibration.run.roadpricing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl.Cost;
import org.matsim.vehicles.Vehicle;

/**
 * Calculates the travel disutility for links, including tolls. Currently supports distance, cordon and area tolls.
 *
 * @author mrieser
 */
class SubpopTravelDisutilityIncludingToll implements TravelDisutility {

	private static final Logger log = Logger.getLogger( SubpopTravelDisutilityIncludingToll.class ) ;
	private double tollCostFactor = 1.0;

	private final RoadPricingScheme scheme;
	private final TollRouterBehaviour tollCostHandler;
	private final TravelDisutility normalTravelDisutility;
	private CharyparNagelScoringParametersForPerson parameters;
	// no sigma required, we wanna go for the subpops' marginal utility of money!
	//private final double sigma ;

	public SubpopTravelDisutilityIncludingToll(final TravelDisutility normalTravelDisutility, 
			final RoadPricingScheme scheme, CharyparNagelScoringParametersForPerson parameters, double sigma) {
		this.scheme = scheme;
		//this.sigma = sigma;
		this.parameters = parameters;
		this.normalTravelDisutility = normalTravelDisutility;
		if (RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(scheme.getType())) {
			this.tollCostHandler = new DistanceTollCostBehaviour();
		} else if (scheme.getType() == RoadPricingScheme.TOLL_TYPE_AREA) {
			this.tollCostHandler = new AreaTollCostBehaviour();
			Logger.getLogger(this.getClass()).warn("area pricing is more brittle than the other toll schemes; " +
					"make sure you know what you are doing.  kai, apr'13 & sep'14") ;
		} else if (scheme.getType() == RoadPricingScheme.TOLL_TYPE_CORDON) {
			this.tollCostHandler = new CordonTollCostBehaviour();
		} else if (scheme.getType() == RoadPricingScheme.TOLL_TYPE_LINK) {
			this.tollCostHandler = new LinkTollCostBehaviour();
		} else {
			throw new IllegalArgumentException("RoadPricingScheme of type \"" + scheme.getType() + "\" is not supported.");
		}
		log.info("Initialized SubpopTravelDisutilityIncludingToll");
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) 
	{
		//log.info("getLinkTravelDisutility for person: " + person.getId().toString());
		double normalTravelDisutilityForLink = this.normalTravelDisutility.getLinkTravelDisutility(link, time, person, vehicle);

		double tollCost = this.tollCostHandler.getTypicalTollCost(link, time );
		
		double marginalUtilityOfMoney = parameters.getScoringParameters(person).marginalUtilityOfMoney;
		//log.info("mom: " + marginalUtilityOfMoney);
		return normalTravelDisutilityForLink + this.tollCostFactor * tollCost * marginalUtilityOfMoney;
		// sign convention: these are all costs (= disutilities), so they are all normally positive.  tollCost is positive, marginalUtilityOfMoney as well.
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		//log.info("getLinkTravelDisutility for link: " + link.getId().toString());
		return this.normalTravelDisutility.getLinkMinimumTravelDisutility(link);
	}

	private interface TollRouterBehaviour {
		public double getTypicalTollCost(Link link, double time);
	}

	/*package*/ class DistanceTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTypicalTollCost(final Link link, final double time) {
			Cost cost_per_m = scheme.getTypicalLinkCostInfo(link.getId(), time );
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
			Cost cost = scheme.getTypicalLinkCostInfo(link.getId(), time );
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
			Cost cost = scheme.getTypicalLinkCostInfo(link.getId(), time );
			if (cost == null) {
				return 0.0;
			}
			return cost.amount;
		}
	}

	/* package */ class LinkTollCostBehaviour implements TollRouterBehaviour {
		@Override
		public double getTypicalTollCost(final Link link, final double time) {
			Cost cost = scheme.getTypicalLinkCostInfo(link.getId(), time );
			if (cost == null) {
				return 0.0;
			}
			return cost.amount;
		}
	}

}
