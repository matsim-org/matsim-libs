/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.framework;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.log4j.Logger;

/**
 * @author thibautd
 */
public class InitialPointProvider implements Provider<Thresholds> {
	private static final Logger log = Logger.getLogger(InitialPointProvider.class);
	
	private final TiesWeightDistribution distribution;
	private final int populationSize;
	private final SocialNetworkGenerationConfigGroup config;

	@Inject
	public InitialPointProvider(
			final TiesWeightDistribution distribution,
			final IndexedPopulation population,
			final SocialNetworkGenerationConfigGroup config) {
		this.distribution = distribution;
		this.populationSize = population.size();
		this.config = config;
	}

	@Override
	public Thresholds get() {
		final double primary = Double.isNaN( config.getInitialPrimaryThreshold() ) ?
			generateHeuristicPrimaryThreshold( distribution , populationSize , config.getTargetDegree() ) :
			config.getInitialPrimaryThreshold();
		final double secondary = Double.isNaN( config.getInitialSecondaryReduction() ) ?
			0 : config.getInitialSecondaryReduction();

		final Thresholds thresholds = new Thresholds( primary , secondary );
		log.info( "initial thresholds: "+thresholds );
		return thresholds;
	}

	private static double generateHeuristicPrimaryThreshold(
			final TiesWeightDistribution distr ,
			final int populationSize,
			final double targetDegree ) {
		log.info( "generating heuristic initial points" );

		// rationale: sqrt(n) alters * sqrt(n) alters_of_alter
		final double target = Math.sqrt( targetDegree );
		return distr.findLowerBound( (long) (populationSize * target) );
	}
}
