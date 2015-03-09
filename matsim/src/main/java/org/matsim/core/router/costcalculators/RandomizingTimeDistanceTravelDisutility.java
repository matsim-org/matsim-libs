/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeDistanceCostCalculator.java
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

package org.matsim.core.router.costcalculators;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * A simple cost calculator which only respects time and distance to calculate generalized costs
 *
 * @author mrieser
 */
public final class RandomizingTimeDistanceTravelDisutility implements TravelDisutility {
	private static final Logger log = Logger.getLogger( RandomizingTimeDistanceTravelDisutility.class ) ;

	private static int normalisationWrnCnt = 0; 

	private final TravelTime timeCalculator;
	private final double marginalCostOfTime;
	private final double marginalCostOfDistance;
	
	private final double normalization ;
	private double sigma ;

	private Random random;

	private double logNormalRnd;

	private Person prevPerson;
	
	private static int wrnCnt = 0 ;

	// === start Builder ===
	public static class Builder implements TravelDisutilityFactory{
		private double sigma = 0. ;
		public Builder() {
		}
		@Override
		public TravelDisutility createTravelDisutility(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
                return new RandomizingTimeDistanceTravelDisutility(timeCalculator, cnScoringGroup, this.sigma )  ;
		}
		public void setSigma( double val ) {
			this.sigma = val ;
		}
	}  
	// === end Builder ===

	@Deprecated // use the builder.  Let me know if/why this is not possible in your case.  kai, mar'15
	public RandomizingTimeDistanceTravelDisutility(final TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
		this( timeCalculator, cnScoringGroup, 0. ) ;
	}
	
	RandomizingTimeDistanceTravelDisutility(final TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, double sigma ) { 
		this.timeCalculator = timeCalculator;

		/* Usually, the travel-utility should be negative (it's a disutility) but the cost should be positive. Thus negate the utility.*/
		this.marginalCostOfTime = (- cnScoringGroup.getTraveling_utils_hr() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);

		this.marginalCostOfDistance = - cnScoringGroup.getMonetaryDistanceCostRateCar() * cnScoringGroup.getMarginalUtilityOfMoney() ;
		
		ModeParams params = cnScoringGroup.getModes().get( TransportMode.car ) ;
		if ( params.getMarginalUtilityOfDistance() !=  0.0 ) {
			throw new RuntimeException( "marginal utility of distance not honored for travel disutility; aborting ... (should be easy to implement)") ;
		}
				
		if ( wrnCnt < 1 ) {
			wrnCnt++ ;
			if ( cnScoringGroup.getMonetaryDistanceCostRateCar() > 0. ) {
				Logger.getLogger(this.getClass()).warn("Monetary distance cost rate needs to be NEGATIVE to produce the normal " +
				"behavior; just found positive.  Continuing anyway.  This behavior may be changed in the future.") ;
			}
		}
		
		this.sigma = sigma ;
		if ( sigma != 0. ) {
			this.random = MatsimRandom.getLocalInstance() ;
			this.normalization = 1./Math.exp( this.sigma*this.sigma/2 );
			if ( normalisationWrnCnt < 10 ) {
				normalisationWrnCnt++ ;
				log.info(" sigma: " + this.sigma + "; resulting normalization: " + normalization ) ;
			}
		} else {
			this.normalization = 1. ;
		}

		
	}


	public RandomizingTimeDistanceTravelDisutility(
			final TravelTime timeCalculator,
			final double marginalCostOfTime_s,
			final double marginalCostOfDistance_m ) {
		this.timeCalculator = timeCalculator;
		this.marginalCostOfTime = marginalCostOfTime_s;
		this.marginalCostOfDistance = marginalCostOfDistance_m;
		
		throw new RuntimeException("currently disabled; tlk to kai if I forget to repair this.  mar'15") ;
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		// randomize if applicable:
		if ( sigma != 0. ) {
			if ( person==null ) {
				throw new RuntimeException("you cannot use the randomzing travel disutility without person.  If you need this without a person, set"
						+ "sigma to zero.") ;
			}
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
			person.getCustomAttributes().put("logNormalRnd", logNormalRnd ) ; // do not use custom attributes in core??  but what would be a better solution here?? kai, mar'15
		} else {
			logNormalRnd = 1. ;
		}
		
		// end randomize
		
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);
		return this.marginalCostOfTime * travelTime + logNormalRnd * this.marginalCostOfDistance * link.getLength();
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {
		return (link.getLength() / link.getFreespeed()) * this.marginalCostOfTime + this.marginalCostOfDistance * link.getLength();
	}

}
