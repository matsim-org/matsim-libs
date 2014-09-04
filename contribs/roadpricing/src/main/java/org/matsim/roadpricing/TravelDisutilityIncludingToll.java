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

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.roadpricing.RoadPricingSchemeImpl.Cost;
import org.matsim.vehicles.Vehicle;

/**
 * Calculates the travel disutility for links, including tolls. Currently supports distance, cordon and area tolls.
 *
 * @author mrieser
 */
public class TravelDisutilityIncludingToll implements TravelDisutility {
	// needs to be public. kai, sep'14

	private static final Logger log = Logger.getLogger( TravelDisutilityIncludingToll.class ) ;

	private final RoadPricingScheme scheme;
	private final TollRouterBehaviour tollCostHandler;
	private final TravelDisutility normalTravelDisutility;
	private final double marginalUtilityOfMoney;
	private Random random = null ;
	private final double normalization ;
	private final double sigma ;

	private Person prevPerson;

	private double logNormalRnd;
	private static int utlOfMoneyWrnCnt = 0 ;

	// === start Builder ===
	public static class Builder implements TravelDisutilityFactory{
		private final RoadPricingScheme scheme;
		private final double marginalUtilityOfMoney ;
		private TravelDisutilityFactory previousTravelDisutilityFactory;
		private double sigma = 3. ;
		public Builder( TravelDisutilityFactory previousTravelDisutilityFactory, RoadPricingScheme scheme, double marginalUtilityOfMoney ) {
			this.scheme = scheme ;
			this.marginalUtilityOfMoney = marginalUtilityOfMoney ;
			this.previousTravelDisutilityFactory = previousTravelDisutilityFactory ;
		}
		public Builder( TravelDisutilityFactory previousTravelDisutilityFactory, RoadPricingScheme scheme, Config config ) {
			this( previousTravelDisutilityFactory, scheme, config.planCalcScore().getMarginalUtilityOfMoney() ) ;
		}
		@Override
		public TravelDisutility createTravelDisutility(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
			return new TravelDisutilityIncludingToll( 
					previousTravelDisutilityFactory.createTravelDisutility(timeCalculator, cnScoringGroup),
					this.scheme, 
					this.marginalUtilityOfMoney, 
					this.sigma 
					);
		}
		public void setSigma( double val ) {
			this.sigma = val ;
		}
	}  
	// === end Builder ===

	@Deprecated // use the builder.  Let me know if/why this is not possible in your case.  kai, sep'14
	public TravelDisutilityIncludingToll(final TravelDisutility normalTravelDisutility, final RoadPricingScheme scheme, Config config) {
		this( normalTravelDisutility, scheme, config.planCalcScore().getMarginalUtilityOfMoney(), 0. ) ;
		// this is using sigma=0 for backwards compatibility (not sure how often this is needed)
	}
	private TravelDisutilityIncludingToll(final TravelDisutility normalTravelDisutility, final RoadPricingScheme scheme, 
			double marginalUtilityOfMoney, double sigma )
	// this should remain private; try using the Builder or ask. kai, sep'14
	{
		this.scheme = scheme;
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
		this.marginalUtilityOfMoney = marginalUtilityOfMoney ;
		if ( utlOfMoneyWrnCnt < 1 && this.marginalUtilityOfMoney != 1. ) {
			utlOfMoneyWrnCnt ++ ;
			Logger.getLogger(this.getClass()).warn("There are no test cases for marginalUtilityOfMoney != 1.  Please write one " +
					"and delete this message.  kai, apr'13 ") ;
		}

		this.sigma = sigma ;
		if ( sigma != 0. ) {
			this.random = MatsimRandom.getLocalInstance() ;
			this.normalization = 1./Math.exp( this.sigma*this.sigma/2 );
			log.warn(" sigma: " + this.sigma + "; resulting normalization: " + normalization ) ;
		} else {
			this.normalization = 1. ;
		}

	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) 
	{
		// randomize if applicable:
		if ( sigma != 0. ) {
			if ( person != prevPerson ) {
				prevPerson = person ;

				logNormalRnd = Math.exp( sigma * random.nextGaussian() ) ;
				logNormalRnd *= normalization ;
				// this should be a log-normal distribution with sigma as the "width" parameter.   Instead of figuring out the "location"
				// parameter mu, I rather just normalize (which should be the same, see next). kai, nov'13

				/* The argument is something like this:<ul> 
				 * <li> exp( mu + sigma * Z) with Z = Gaussian generates lognormal with mu and sigma.
				 * <li> The mean of this is exp( mu + sigma^2/2 ) .  
				 * <li> If we set mu=0, the expectation value is exp( sigma^2/2 ) .
				 * <li> So in order to set the expectation value to one (which is what we want), we need to divide by exp( sigma^2/2 ) .
				 * </ul>
				 * Should be tested. kai, jan'14 */
			}
		} else {
			logNormalRnd = 1. ;
		}
		// end randomize

		double normalTravelDisutilityForLink = this.normalTravelDisutility.getLinkTravelDisutility(link, time, person, vehicle);
		double tollCost = this.tollCostHandler.getTollCost(link, time, person, vehicle);
		return normalTravelDisutilityForLink + tollCost*this.marginalUtilityOfMoney*logNormalRnd ;
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
			Cost cost_per_m = scheme.getLinkCostInfo(link.getId(), time, personId, vehicleId );
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
			Cost cost = scheme.getLinkCostInfo(link.getId(), time, personId, vehicleId );
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
			Cost cost = scheme.getLinkCostInfo(link.getId(), time, personId, vehicleId );
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
			Cost cost = scheme.getLinkCostInfo(link.getId(), time, personId, vehicleId );
			if (cost == null) {
				return 0.0;
			}
			return cost.amount;
		}
	}

}
