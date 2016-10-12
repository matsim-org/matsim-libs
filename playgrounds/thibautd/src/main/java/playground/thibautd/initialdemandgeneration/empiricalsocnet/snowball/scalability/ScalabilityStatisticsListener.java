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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.scalability;

import com.google.inject.Inject;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.Ego;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.SocialNetworkSampler;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.SnowballLocator;
import playground.thibautd.initialdemandgeneration.socnetgen.framework.SnaUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.DoubleStream;

import static playground.meisterk.PersonAnalyseTimesByActivityType.Activities.e;

/**
 * @author thibautd
 */
public class ScalabilityStatisticsListener implements AutoCloseable {
	private static final Logger log = Logger.getLogger( ScalabilityStatisticsListener.class );
	
	private final BufferedWriter writer;
	private final Set<Set<Ego>> cliques = new HashSet<>();
	private final Set<Ego> egos = new HashSet<>();
	private final TObjectIntMap<Ego> nCliqueTies = new TObjectIntHashMap<>();

	private double currSample = -1;
	private int currTryNr = -1;

	private long start_ms = -1;

	public ScalabilityStatisticsListener( final String file ) {
		this.writer = IOUtils.getBufferedWriter( file );
		try {
			writer.write( "currSample\ttryNr\t" +
					"duration_ms\tpopulationSize\t" +
					"cliqueSizeMin\tcliqueSizeQ1\tcliqueSizeMedian\tcliqueSizeQ3\tcliqueSizeMax\t" +
					"degreeMin\tdegreeQ1\tdegreeMedian\tdegreeQ3\tdegreeMax\t" +
					"distanceMin\tdistanceQ1\tdistanceMedian\tdistanceQ3\tdistanceMax\t" +
					"overlapMin\toverlapQ1\toverlapMedian\toverlapQ3\toverlapMax\t" +
					"nConnectedComponents\t" +
					"componentSizeMin\tcomponentSizeQ1\tcomponentSizeMedian\tcomponentSizeQ3\tcomponentSizeMax" );
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	@Inject
	public void injectCallback(
			final SocialNetworkSampler sampler ) {
		log.info( "register new callback for listening cliques creation" );
		sampler.addCliqueListener( this::handleClique );
	}

	public void handleClique( final Set<Ego> clique ) {
		cliques.add( clique );
		egos.addAll( clique );
		for ( Ego e : clique ) {
			nCliqueTies.adjustOrPutValue(
					e,
					clique.size(),
					clique.size() );
		}
	}

	public void startTry( final double sample , final int tryNr ) {
		cliques.clear();
		egos.clear();
		nCliqueTies.clear();
		this.currSample = sample;
		this.currTryNr = tryNr;
		this.start_ms = System.currentTimeMillis();
	}

	public void endTry( final SocialNetwork sn ) {
		try {
			final double dur_ms = System.currentTimeMillis() - start_ms;
			log.info( "write statistics for try "+currTryNr+" of sample rate "+currSample );
			writer.newLine();
			writer.write( currSample +"\t"+currTryNr );
			writer.write( "\t" );
			writer.write( dur_ms +"\t"+sn.getEgos().size() );
			writer.write( "\t" );

			log.info( "write clique size statistics..." );
			writeBoxPlot( cliques , Set::size );
			writer.write( "\t" );

			log.info( "write degree statistics..." );
			writeBoxPlot( egos , e -> e.getAlters().size() );
			writer.write( "\t" );

			log.info( "write distance statistics..." );
			writeBoxPlotFlat( egos , ScalabilityStatisticsListener::getDistances );
			writer.write( "\t" );

			log.info( "write overlap statistics..." );
			writeBoxPlot( egos , (Ego e) -> nCliqueTies.get( e ) / e.getAlters().size() );

			log.info( "write connected components statistics..." );
			final Collection<Set<Id>> components = SnaUtils.identifyConnectedComponents( sn );
			writer.write( "\t"+components.size()+"\t" );
			writeBoxPlot( components , Set::size );

			log.info( "write statistics for try "+currTryNr+" of sample rate "+currSample+": DONE" );
			writer.flush();
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	private static DoubleStream getDistances( final Ego ego ) {
		final Coord egoCoord = SnowballLocator.calcCoord( ego );
		return ego.getAlters().stream()
				.map( SnowballLocator::calcCoord )
				.mapToDouble( ac -> CoordUtils.calcEuclideanDistance( ac , egoCoord ) );
	}

	private <T> void writeBoxPlot( final Collection<T> objs, final ToDoubleFunction<T> stat ) throws IOException {
		writeBoxPlotFlat( objs , (T o) -> DoubleStream.of( stat.applyAsDouble( o ) ) );
	}

	private <T> void writeBoxPlotFlat( final Collection<T> objs, final Function<T,DoubleStream> stat ) throws IOException {
		final double[] arr = objs.stream().flatMapToDouble( stat ).toArray();

		// could sort a random subset only if collection very long, for efficiency (only if really too long)
		Arrays.sort( arr );

		final int last = arr.length - 1;
		double min = arr[ 0 ];
		double q1 = arr[ last / 4 ];
		double median = arr[ last / 2 ];
		double q3 = arr[ 3 * last / 4 ];
		double max = arr[ last ];

		writer.write( min+"\t"+q1+"\t"+median+"\t"+q3+"\t"+max );
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}

}
