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

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import playground.thibautd.negotiation.framework.NegotiationAgent;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;

/**
 * @author thibautd
 */
public class ChosenLocationWriter implements AutoCloseable {
	private final BufferedWriter writer;
	private final LocationHelper locations;

	private final Counter counter = new Counter( "writing location # " );

	public ChosenLocationWriter( final String file, final LocationHelper locations ) throws IOException {
		writer = IOUtils.getBufferedWriter( file );
		this.locations = locations;
		writer.write( "groupNr\tpersonId\tfacilityId\tdistance\ttype" );
	}

	public void writeLocation( final NegotiationAgent<LocationProposition> agent ) {
		try {
			counter.incCounter();
			final LocationProposition location = agent.getBestProposition();

			final String groupId = location == null ? "NA" : getGroupId( location.getGroup() );
			final String facilityId = location == null ? "NA" : location.getFacility().getId().toString();

			final String distance = location == null ? "NA" :
					""+CoordUtils.calcEuclideanDistance(
							location.getFacility().getCoord(),
							locations.getHomeLocation( agent.getId() ).getCoord() );

			final String type = location == null ? "NA" : location.getType().toString();

			writer.newLine();
			writer.write( groupId+"\t"+agent.getId()+"\t"+facilityId+"\t"+distance+"\t"+type );
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	private static String getGroupId( final Collection<Person> participants ) {
		return participants.stream()
				.map( Person::getId )
				.sorted()
				.collect(
						StringBuilder::new,
						StringBuilder::append,
						StringBuilder::append )
				.toString();
	}

	@Override
	public void close() throws IOException {
		counter.printCounter();
		writer.close();
	}
}
