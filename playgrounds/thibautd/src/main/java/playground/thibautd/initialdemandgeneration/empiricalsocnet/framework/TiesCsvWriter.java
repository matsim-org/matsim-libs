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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.framework;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author thibautd
 */
public class TiesCsvWriter implements Consumer<Set<Ego>>, AutoCloseable  {
	private final BufferedWriter writer;

	public TiesCsvWriter( final String file ) {
		this.writer = IOUtils.getBufferedWriter( file );
		try {
			writer.write( "egoId\tegoPlannedDegree\talterId\talterPlannedDegree" );
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	@Override
	public void accept( final Set<Ego> egos ) {
		for ( Ego ego : egos ) {
			for ( Ego alter : egos ) {
				if ( alter == ego ) break;
				try {
					writer.newLine();
					writer.write( ego.getId() +"\t"+ ego.getDegree() );
					writer.write( "\t" );
					writer.write( alter.getId() +"\t"+ alter.getDegree() );
				}
				catch ( IOException e ) {
					throw new UncheckedIOException( e );
				}
			}
		}
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}
}
