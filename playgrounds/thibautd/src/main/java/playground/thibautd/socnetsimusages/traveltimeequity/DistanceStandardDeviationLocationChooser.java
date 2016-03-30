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
package playground.thibautd.socnetsimusages.traveltimeequity;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import org.matsim.contrib.socnetsim.jointactivities.replanning.modules.prismiclocationchoice.PrismicLocationChoiceAlgorithm.LocationChooser;
import org.matsim.contrib.socnetsim.jointactivities.replanning.modules.prismiclocationchoice.PrismicLocationChoiceAlgorithm.Subchain;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;

import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * @author thibautd
 */
public class DistanceStandardDeviationLocationChooser implements LocationChooser {
	private final Random random = MatsimRandom.getLocalInstance();

	@Override
	public ActivityFacility choose(
			final Collection<Subchain> subchains,
			final List<ActivityFacility> choiceSet) {
		final TDoubleList weights = new TDoubleArrayList( choiceSet.size() );

		double sum = 0;
		for ( ActivityFacility f : choiceSet ) {
			final double weight = 1 / 1E-5 +
					computeStandardDeviation(
							f,
							subchains );

			sum += weight;
			weights.add( sum );
		}

		final double choice = random.nextDouble() * sum;

		for ( int i = 0; i < weights.size(); i++ ) {
			if ( choice <= weights.get( i ) ) return choiceSet.get( i );
		}
		throw new RuntimeException( "choice="+choice+" was not inferior to anything in "+weights);
	}

	private double computeStandardDeviation(final ActivityFacility f, final Collection<Subchain> subchains) {
		final TDoubleList distances = new TDoubleArrayList( subchains.size() );

		double avg = 0;
		for ( Subchain s : subchains ) {
			final double distance = CoordUtils.calcEuclideanDistance( s.getStart().getCoord() , f.getCoord() ) +
							CoordUtils.calcEuclideanDistance( s.getEnd().getCoord() , f.getCoord() );
			distances.add( distance );
			avg += distance;
		}
		avg /= subchains.size();

		double stdDev = 0;
		for ( double d : distances.toArray() ) {
			stdDev += Math.abs( d - avg );
		}
		stdDev /= subchains.size();

		return stdDev;
	}
}
