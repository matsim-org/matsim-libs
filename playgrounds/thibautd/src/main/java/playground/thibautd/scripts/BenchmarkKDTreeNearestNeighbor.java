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
import playground.thibautd.utils.KDTree;

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
			writer.write( "size\tdimension\tstrategy\tnQueries\ttime_ms" );

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

		final KDTree<double[]> dfs =
				new KDTree<>(
						KDTree.SearchStrategy.DFS,
						false,
						dim,
						d -> d );
		dfs.add( points );

		final KDTree<double[]> bbf =
				new KDTree<>(
						KDTree.SearchStrategy.BBF,
						false,
						dim,
						d -> d );
		bbf.add( points );

		for ( int i=0; i < 100; i++ ) {
			final Collection<double[]> searched = new ArrayList<>(  );
			for ( int q=0; q < N_QUERIES; q++ ) {
				final double[] p = new double[ dim ];
				for ( int j = 0; j < dim; j++ ) p[ j ] = r.nextDouble();
				searched.add( p );
			}

			long start = System.currentTimeMillis();
			for ( double[] p : searched ) dfs.getClosestEuclidean( p );
			long mid = System.currentTimeMillis();
			for ( double[] p : searched ) bbf.getClosestEuclidean( p );
			long end = System.currentTimeMillis();

			writer.newLine();
			writer.write( size+"\t"+dim+"\tDFS\t"+N_QUERIES+"\t"+(mid - start) );
			writer.newLine();
			writer.write( size+"\t"+dim+"\tBBF\t"+N_QUERIES+"\t"+(end - mid) );
		}
	}
}
