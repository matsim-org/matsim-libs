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
package playground.thibautd.negotiation.locationnegotiation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.thibautd.utils.LambdaCounter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author thibautd
 */
public class GroupPlansWriter implements AutoCloseable {
	private static final Logger log = Logger.getLogger( GroupPlansWriter.class );
	private final BufferedWriter writer;
	private final LocationHelper locations;

	private final AtomicInteger iteration = new AtomicInteger( 0 );

	private final LambdaCounter counter = new LambdaCounter( l -> log.info( "Writing info on location # "+l+" (Iteration "+iteration.get()+")" ) );

	public GroupPlansWriter( final String path, final LocationHelper locations ) {
		writer = IOUtils.getBufferedWriter( path );
		this.locations = locations;

		try {
			writer.write( "iteration\tagentId\ttype\tjointActId\tnumberParticipants\tdistance" );
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	public void writePlans( final GroupPlans plans ) {
		iteration.incrementAndGet();
		log.info( "write joint results for iteration # "+iteration );
		try {
			for ( Plan plan : plans.getAllIndividualPlans() ) {
				counter.incCounter();
				final LocationProposition proposition = (LocationProposition) plan.getCustomAttributes().get( "proposition" );
				writer.newLine();
				writer.write( ( iteration.get() )+"\t"+
						plan.getPerson().getId()+"\t"+
						proposition.getType()+"\t"+
						proposition.getProposer().getId()+"-"+iteration+"\t"+
						proposition.getGroup().size()+"\t"+
								CoordUtils.calcEuclideanDistance(
										locations.getHomeLocation( plan.getPerson().getId() ).getCoord(),
										proposition.getFacility().getCoord() ) );
			}
			counter.printCounter();
			writer.flush();
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	@Override
	public void close() throws IOException {
		counter.printCounter();
		writer.close();
	}
}
