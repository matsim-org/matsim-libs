/* *********************************************************************** *
 * project: org.matsim.*
 * RandomizingTimeDistanceTravelDisutilityFactory.java
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A factory for a disutility that leads to randomized Pareto search.  Starting point is to have something like disutility(link) = alpha * time + beta *
 * money.  Evidently, for beta=0 one obtains the time-minimal path, and for alpha=0 the money-minimal path.  The current version will randomize the prefactor
 * of the monetary term.  It is implemented in a way that the random number is drawn once per routing call, i.e. the route is computed with a constant
 * tradeoff between money and time, but with the next call it will be a different trade-off.
 * <br/>
 * The idea is to come up with different routes, with the hope that one of them ends up being a good one for the scoring function.  See, e.g.,
 * <a href="https://arxiv.org/abs/1002.4330v1">https://arxiv.org/abs/1002.4330v1</a>
 * for how to generate route alternatives (where Pareto routing is one option), and
 * <a href="https://doi.org/10.1016/j.procs.2014.05.488">https://doi.org/10.1016/j.procs.2014.05.488</a> for a paper testing the approach.
 */
public class RandomizingTimeDistanceTravelDisutilityFactory implements TravelDisutilityFactory {
	private static final Logger log = LogManager.getLogger( RandomizingTimeDistanceTravelDisutilityFactory.class ) ;

	private static final AtomicInteger wrnCnt = new AtomicInteger(0);
	private static final AtomicInteger normalisationWrnCnt = new AtomicInteger(0);

	private final String mode;
	private final double sigma;
	private final ScoringConfigGroup cnScoringGroup;

	public RandomizingTimeDistanceTravelDisutilityFactory( final String mode, Config config ) {
		// NOTE: It is difficult to get rid of this constructor completely, since "mode" needs to be passed in.  One could still get all other
		// material from injection, but there are many uses of this class outside injection.

		this.mode = mode;
		this.cnScoringGroup = config.scoring();
		this.sigma = config.routing().getRoutingRandomness();
	}

	@Override
	public TravelDisutility createTravelDisutility( final TravelTime travelTime) {
		// yyyy This here should honor subpopulations.  It is really not so difficult; something like cnScoringGroup.getScoringParameters( "subpop"
		// ).getMarginalUtilityOfMoney(); That line, or some variant of it, would need to be in the TravelDisutility directly. And I am quite unsure what is the status of
		// the "default" subpopulation anyways ... I seem to recall that Thibaut wanted to get rid of that.  The following method at least outputs
		// a warning.  However, we know by now that few people think about such warnings. kai, mar'20
		logWarningsIfNecessary( cnScoringGroup );

		final ScoringConfigGroup.ModeParams params = cnScoringGroup.getModes().get( mode ) ;
		if ( params == null ) {
			throw new NullPointerException( mode+" is not part of the valid mode parameters "+cnScoringGroup.getModes().keySet() );
		}

		/* Usually, the travel-utility should be negative (it's a disutility) but the cost should be positive. Thus negate the utility.*/
		final double marginalCostOfTime_s = (-params.getMarginalUtilityOfTraveling() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);
		final double marginalCostOfDistance_m = - params.getMonetaryDistanceRate() * cnScoringGroup.getMarginalUtilityOfMoney()
				- params.getMarginalUtilityOfDistance() ;

		double normalization = 1;
		if ( sigma != 0. ) {
			normalization = 1. / Math.exp(this.sigma * this.sigma / 2);
			if (normalisationWrnCnt.getAndIncrement() < 10) {
				log.info(" sigma: " + this.sigma + "; resulting normalization: " + normalization);
			}
		}

		return new RandomizingTimeDistanceTravelDisutility(
				travelTime,
				marginalCostOfTime_s,
				marginalCostOfDistance_m,
				normalization,
				sigma);
	}

	private void logWarningsIfNecessary(final ScoringConfigGroup cnScoringGroup) {
		if ( wrnCnt.getAndIncrement() < 1 ) {
			if ( cnScoringGroup.getModes().get( mode ).getMonetaryDistanceRate() > 0. ) {
				log.warn("Monetary distance cost rate needs to be NEGATIVE to produce the normal " +
						"behavior; just found positive.  Continuing anyway.") ;
			}

			final Set<String> monoSubpopKeyset = Collections.singleton( null );
			if ( !cnScoringGroup.getScoringParametersPerSubpopulation().keySet().equals( monoSubpopKeyset ) ) {
				log.warn( "Scoring parameters are defined for different subpopulations." +
						" The routing disutility will only consider the ones of the default subpopulation.");
				log.warn( "This warning can safely be ignored if disutility of traveling only depends on travel time.");
			}

			if ( cnScoringGroup.getModes().get( mode ).getMonetaryDistanceRate() == 0. && this.sigma != 0. ) {
				log.warn("There will be no routing randomness for mode={}. The randomization of the travel disutility requires the monetary distance rate "
						+ "to be different than zero. Continuing anyway.", mode) ;
				log.warn( "You can also set the width of the routing randomness to zero:");
				log.warn("\t\tconfig.routing().setRoutingRandomness( 0. );");
				log.warn( "in code, or");
				log.warn("\t<module name=\"routing\" >");
				log.warn("\t\t<param name=\"routingRandomness\" value=\"0.\" />");
				log.warn("in the xml config");
			}

			if ( (cnScoringGroup.getModes().get( mode ).getMarginalUtilityOfTraveling() + cnScoringGroup.getPerforming_utils_hr())  == 0. && this.sigma != 0. ) {
				log.warn("There will be no routing randomness for mode={}. The randomization of the travel disutility requires the travel time cost rate "
						+ "to be different than zero. Continuing anyway.", mode) ;
			}
		}
	}

}
