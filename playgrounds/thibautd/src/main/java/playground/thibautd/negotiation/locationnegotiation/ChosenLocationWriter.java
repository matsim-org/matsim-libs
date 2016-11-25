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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * @author thibautd
 */
public class ChosenLocationWriter implements AutoCloseable {
	private final BufferedWriter writer;
	private int groupNr = 0;

	public ChosenLocationWriter( final String file ) throws IOException {
		writer = IOUtils.getBufferedWriter( file );
		writer.write( "groupNr\tpersonId\tfacilityId" );
	}

	public void writeLocation( final LocationProposition location ) {
		try {
			groupNr++;
			for ( Id<Person> person : location.getGroupIds() ) {
				writer.newLine();
				writer.write( groupNr+"\t"+person+"\t"+location.getFacility().getId() );
			}
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}
}
