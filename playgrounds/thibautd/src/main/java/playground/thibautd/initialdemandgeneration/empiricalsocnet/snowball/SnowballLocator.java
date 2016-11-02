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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.population.PersonUtils;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.CliqueStub;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.Ego;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.EgoLocator;
import playground.thibautd.utils.spatialcollections.SpatialCollectionUtils;

/**
 * @author thibautd
 */
@Singleton
public class SnowballLocator implements EgoLocator, Position, SpatialCollectionUtils.Metric<double[]> {
	// a difference in the categorical variables is equivalent to 10'000 km
	private static final double NON_SPATIAL_FACTOR = 10 * 1000 * 1000;
	private static final double CLIQUE_SIZE_FACTOR = 100 * NON_SPATIAL_FACTOR;

	private final SocialPositions positions;

	@Inject
	public SnowballLocator( final SocialPositions positions ) {
		this.positions = positions;
	}

	@Override
	public int getDimensionality() {
		return 5;
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
	public double[] getCoord( final CliqueStub stub ) {
		final Ego ego = stub.getEgo();
		final Coord coord = calcCoord( ego );

		return new double[]{
				coord.getX() ,
				coord.getY() ,
				NON_SPATIAL_FACTOR * positions.calcAgeClass( PersonUtils.getAge( ego.getPerson() ) ) ,
				NON_SPATIAL_FACTOR * SocialPositions.getSex( ego ).ordinal(),
				stub.getCliqueSize() * CLIQUE_SIZE_FACTOR };
	}

	@Override
	public double[] calcPosition(
			final CliqueStub center,
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
				position.isSameSex() ? egoCoord[ 3 ] : NON_SPATIAL_FACTOR - egoCoord[ 3 ],
				egoCoord[ 4 ] };
	}

	@Override
	public double calcDistance( final double[] t1, final double[] t2 ) {
		double d = Math.sqrt( Math.pow( t1[0] - t2[0] , 2 ) + Math.pow( t1[ 1 ] - t2[ 1 ] , 2 ) );
		for ( int i=2; i < getDimensionality(); i++ ) d += Math.abs( t1[ i ] - t2[ i ] );
		return d;
	}
}
