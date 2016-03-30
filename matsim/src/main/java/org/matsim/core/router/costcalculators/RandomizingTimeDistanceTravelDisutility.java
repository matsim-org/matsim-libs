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

import java.util.Collections;
import java.util.Random;
import java.util.Set;

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
	private final double sigma ;

	private final Random random;

	// "cache" of the random value
	private double logNormalRnd;
	private Person prevPerson;
	
	private static int wrnCnt = 0 ;

	// === start Builder ===
	public static class Builder implements TravelDisutilityFactory{
		private final String mode;
		private double sigma = 0. ;
		private final PlanCalcScoreConfigGroup cnScoringGroup;

		public Builder( final String mode, PlanCalcScoreConfigGroup cnScoringGroup ) {
			this.mode = mode;
			this.cnScoringGroup = cnScoringGroup;
		}

		@Override
		public RandomizingTimeDistanceTravelDisutility createTravelDisutility(
				final TravelTime timeCalculator) {
			logWarningsIfNecessary( cnScoringGroup );

			/* Usually, the travel-utility should be negative (it's a disutility) but the cost should be positive. Thus negate the utility.*/
			final ModeParams params = cnScoringGroup.getModes().get( mode ) ;
			if ( params == null ) {
				throw new NullPointerException( mode+" is not part of the valid mode parameters "+cnScoringGroup.getModes().keySet() );
			}
			final double marginalCostOfTime_s = (-params.getMarginalUtilityOfTraveling() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);

			final double marginalCostOfDistance_m = -params.getMonetaryDistanceRate() * cnScoringGroup.getMarginalUtilityOfMoney() ;

			if ( params.getMarginalUtilityOfDistance() !=  0.0 ) {
				throw new RuntimeException( "marginal utility of distance not honored for travel disutility; aborting ... (should be easy to implement)") ;
			}


			double normalization = 1;
			if ( sigma != 0. ) {
				normalization = 1. / Math.exp(this.sigma * this.sigma / 2);
				if (normalisationWrnCnt < 10) {
					normalisationWrnCnt++;
					log.info(" sigma: " + this.sigma + "; resulting normalization: " + normalization);
				}
			}

			return new RandomizingTimeDistanceTravelDisutility(
					timeCalculator,
					marginalCostOfTime_s,
					marginalCostOfDistance_m,
					normalization,
					sigma);
		}

		private void logWarningsIfNecessary(final PlanCalcScoreConfigGroup cnScoringGroup) {
			if ( wrnCnt < 1 ) {
				wrnCnt++ ;
				if ( cnScoringGroup.getModes().get( mode ).getMonetaryDistanceRate() > 0. ) {
					log.warn("Monetary distance cost rate needs to be NEGATIVE to produce the normal " +
							"behavior; just found positive.  Continuing anyway.  This behavior may be changed in the future.") ;
				}

				final Set<String> monoSubpopKeyset = Collections.singleton( null );
				if ( !cnScoringGroup.getScoringParametersPerSubpopulation().keySet().equals( monoSubpopKeyset ) ) {
					log.warn( "Scoring parameters are defined for different subpopulations." +
							" The routing disutility will only consider the ones of the default subpopulation.");
					log.warn( "This warning can safely be ignored if disutility of traveling only depends on travel time.");
				}
			}
		}

		public Builder setSigma( double val ) {
			this.sigma = val ;
			return this;
		}
	}  
	// === end Builder ===

	private RandomizingTimeDistanceTravelDisutility(
			final TravelTime timeCalculator,
			final double marginalCostOfTime_s,
			final double marginalCostOfDistance_m,
			final double normalization,
			final double sigma) {
		this.timeCalculator = timeCalculator;
		this.marginalCostOfTime = marginalCostOfTime_s;
		this.marginalCostOfDistance = marginalCostOfDistance_m;
		this.normalization = normalization;
		this.sigma = sigma;
		this.random = sigma != 0 ? MatsimRandom.getLocalInstance() : null;
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
			// do not use custom attributes in core??  but what would be a better solution here?? kai, mar'15
			// Is this actually used anywhere? As far as I can see, this is at least no used in this class... td, Oct'15
			person.getCustomAttributes().put("logNormalRnd", logNormalRnd ) ;
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
