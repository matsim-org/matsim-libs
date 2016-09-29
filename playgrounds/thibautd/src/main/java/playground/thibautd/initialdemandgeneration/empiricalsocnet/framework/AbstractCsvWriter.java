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
public abstract class AbstractCsvWriter  implements Consumer<Set<Ego>>, AutoCloseable {
	private final BufferedWriter writer;

	protected abstract String titleLine();
	protected abstract Iterable<String> cliqueLines( Set<Ego> clique);

	public AbstractCsvWriter( final String file ) {
		this.writer = IOUtils.getBufferedWriter( file );
		try {
			writer.write( titleLine() );
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	@Override
	public final void accept( final Set<Ego> egos ) {
		for ( String l : cliqueLines( egos ) ) {
			try {
				writer.write( l );
			}
			catch ( IOException e ) {
				throw new UncheckedIOException( e );
			}
		}
	}

	@Override
	public final void close() throws IOException {
		writer.close();
	}
}
