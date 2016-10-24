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
package playground.thibautd.utils.spatialcollections;

/**
 * @author thibautd
 */
public class SpatialCollectionUtils {
	public static double manhattan( double[] c1 , double[] c2 ) {
		double d = 0;

		for (int i=0; i < c1.length; i++ ) {
			d += Math.abs( c1[ i ] - c2[ i ] );
		}

		return d;
	}

	public static double squaredEuclidean( double[] c1 , double[] c2 ) {
		double d = 0;

		for (int i=0; i < c1.length; i++ ) {
			d += Math.pow( c1[ i ] - c2[ i ] , 2 );
		}

		return d;
	}

	public static double euclidean( double[] c1 , double[] c2 ) {
		return Math.sqrt( squaredEuclidean( c1 , c2 ) );
	}

	public interface Coordinate<T> extends GenericCoordinate<double[],T> {}

	public interface GenericCoordinate<C,T> {
		C getCoord( T object );
	}

	public interface Metric<C> {
		double calcDistance( C t1, C t2 );
	}

	public interface Distance extends Metric<double[]> {}
}
