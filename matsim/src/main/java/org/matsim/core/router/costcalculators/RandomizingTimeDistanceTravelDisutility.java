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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.Random;

/**
 * @author mrieser
 */
final class RandomizingTimeDistanceTravelDisutility implements TravelDisutility {

	private final TravelTime timeCalculator;
	private final double marginalCostOfTime;
	private final double marginalCostOfDistance;
	
	private final double normalization ;
	private final double sigma ;

	private final Random random;

	// "cache" of the random value
	private double logNormalRnd;
	private Person prevPerson;

	RandomizingTimeDistanceTravelDisutility(
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
						+ " sigma to zero. If you are loading a scenario from a config, set the routingRandomness in the plansCalcRoute config group to zero.") ;
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
