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
package playground.thibautd.maxess.prepareforbiogeme.framework;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author thibautd
 */
public class ChoiceDataSetWriter<T> implements AutoCloseable {
	private final BufferedWriter writer;
	private final ChoiceSetRecordFiller<T> recordFiller;

	private final Counter counter = new Counter( "Write record # " );

	private Collection<String> header = null;

	public ChoiceDataSetWriter(
			final ChoiceSetRecordFiller<T> recordFiller,
			final String filename ) {
		this.recordFiller = recordFiller;
		this.writer = IOUtils.getBufferedWriter( filename );
	}

	private void writeLine(final Collection<?> v) {
		try {
			counter.incCounter();
			int i=0;
			for ( Object o : v ) {
				writer.write( o+(i++ < v.size() ? "\t" : "") );
			}
			writer.newLine();
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	public void write( final ChoiceSet<T> cs ) {
		final Map<String,? extends Number> fields = recordFiller.getFieldValues( cs );
		if ( header == null ) {
			header = fields.keySet();
			writeLine(header);
		}
		writeLine( fields.values() );
	}

	@Override
	public void close() throws IOException {
		counter.printCounter();
		writer.close();
	}

	public interface ChoiceSetRecordFiller<T> {
		/**
		 * Returning numbers is needed for BIOGEME, but one might relax the requirement if estimation is done for instance
		 * with R-mlogit
		 * @param cs
		 * @return
		 */
		Map<String,? extends Number> getFieldValues( ChoiceSet<T> cs );
	}
}
