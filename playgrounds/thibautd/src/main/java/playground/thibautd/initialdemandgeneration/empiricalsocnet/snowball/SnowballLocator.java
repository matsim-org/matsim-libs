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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball;

import com.google.inject.Singleton;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.population.PersonUtils;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.Ego;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.EgoLocator;

/**
 * @author thibautd
 */
@Singleton
public class SnowballLocator implements EgoLocator, Position {
	// a difference in the categorical variables is equivalent to 10'000 km
	private static double NON_SPATIAL_FACTOR = 10 * 1000 * 1000;

	@Override
	public int getDimensionality() {
		return 4;
	}

	public static Coord calcCoord( final Ego ego ) {
		return ego.getPerson().getSelectedPlan().getPlanElements().stream()
				.filter( pe -> pe instanceof Activity )
				.map( pe -> (Activity) pe )
				.findFirst()
				.get()
				.getCoord();
	}

	@Override
	public double[] getCoord( final Ego ego ) {
		final Coord coord = calcCoord( ego );

		return new double[]{
				coord.getX() ,
				coord.getY() ,
				NON_SPATIAL_FACTOR * SocialPositions.calcAgeClass( PersonUtils.getAge( ego.getPerson() ) ) ,
				NON_SPATIAL_FACTOR * SocialPositions.getSex( ego ).ordinal() };
	}

	@Override
	public double[] calcPosition( final Ego center,
			final SocialPositions.CliquePosition position,
			final double rotation ) {
		final double[] egoCoord = getCoord( center );

		final double bearing = position.getBearing() + rotation;
		final double xTranslation = Math.cos( bearing ) * position.getDistance();
		final double yTranslation = Math.sin( bearing ) * position.getDistance();

		return new double[]{
				egoCoord[ 0 ] + xTranslation ,
				egoCoord[ 1 ] + yTranslation ,
				// can go out of bounds, in which case the closest is the first or last class.
				// would be better to manage to get something "at a distance", whatever the direction...
				egoCoord[ 2 ] + NON_SPATIAL_FACTOR * position.getAgeClassDistance() ,
				position.isSameSex() ? egoCoord[ 3 ] : NON_SPATIAL_FACTOR - egoCoord[ 3 ] };
	}
}
