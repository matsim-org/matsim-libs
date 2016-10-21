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
package playground.thibautd.scripts;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import playground.thibautd.utils.spatialcollections.KDTree;
import playground.thibautd.utils.spatialcollections.SpatialCollectionUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * @author thibautd
 */
public class BenchmarkKDTreeNearestNeighbor {
	private static final Logger log = Logger.getLogger( BenchmarkKDTreeNearestNeighbor.class );
	private static final int N_QUERIES = 1000;

	public static void main( final String... args ) {
		//try ( final BufferedWriter writer = IOUtils.getBufferedWriter( args[ 0 ] ) ) {
		try ( final BufferedWriter writer = IOUtils.getBufferedWriter( "times.dat" ) ) {
			writer.write( "size\tdimension\tnQueries\texact_time_ms\tdistance_exact\tappr_time_ms\tdistance_appr" );

			for ( int size=100; size < 1E8; size *= 10  ) {
				log.info( "look at size "+size );
				for ( int dim=2; dim < 5; dim++ ) {
					log.info( "look at dimension "+dim );
					runBenchmark( size , dim , writer );
				}
			}
		}
		catch ( IOException e ) {
			throw new RuntimeException( e );
		}
	}

	private static void runBenchmark(
			final int size,
			final int dim,
			final BufferedWriter writer ) throws IOException {
		final Random r = new Random( size * dim );
		final List<double[]> points = new ArrayList<>(  );

		for ( int i = 0; i < size; i++ ) {
			final double[] p = new double[ dim ];
			for ( int j=0; j < dim; j++ ) p[ j ] = r.nextDouble();
			points.add( p );
		}

		final KDTree<double[]> qt =
				new KDTree<>(
						dim,
						d -> d );
		qt.add( points );

		for ( int i=0; i < 100; i++ ) {
			final Collection<double[]> searched = new ArrayList<>(  );
			for ( int q=0; q < N_QUERIES; q++ ) {
				final double[] p = new double[ dim ];
				for ( int j = 0; j < dim; j++ ) p[ j ] = r.nextDouble();
				searched.add( p );
			}

			final long start = System.currentTimeMillis();
			final double distExact = searched.stream()
					.mapToDouble( p -> SpatialCollectionUtils.euclidean( p , qt.getClosestEuclidean( p ) ) )
					.average()
					.getAsDouble();
			final long mid = System.currentTimeMillis();
			// in perfectly balanced binary tree, number of leaves is (n + 1) / 2
			// so size / x is a good way to define Emax
			final double distAppr = searched.stream()
					.mapToDouble( p -> SpatialCollectionUtils.euclidean( p , qt.getClosest( p , SpatialCollectionUtils::euclidean , x -> true , 0. , size / 100 ) ) )
					.average()
					.getAsDouble();
			final long end = System.currentTimeMillis();

			writer.newLine();
			writer.write( size+"\t"+dim+"\t"+N_QUERIES+"\t"+(mid - start)+"\t"+distExact+"\t"+(end - mid)+"\t"+distAppr );
		}
	}
}
