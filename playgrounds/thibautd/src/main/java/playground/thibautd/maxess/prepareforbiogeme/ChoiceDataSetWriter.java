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
package playground.thibautd.maxess.prepareforbiogeme;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * @author thibautd
 */
public class ChoiceDataSetWriter<T> implements AutoCloseable {
	private final BufferedWriter writer;
	private final ChoiceSetRecordFiller<T> recordFiller;

	private final Counter counter = new Counter( "Write record # " );

	public ChoiceDataSetWriter(
			final ChoiceSetRecordFiller<T> recordFiller,
			final String filename ) {
		this.recordFiller = recordFiller;
		this.writer = IOUtils.getBufferedWriter( filename );
		writeLine(recordFiller.getFieldNames());
	}

	private void writeLine(final List<?> v) {
		try {
			counter.incCounter();
			for ( int i=0; i < v.size() - 1; i++ ) {
				writer.write( v.get( i )+"\t" );
			}
			writer.write(v.get(v.size() - 1).toString());
			writer.newLine();
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	public void write( final ChoiceSet<T> cs ) {
		writeLine( recordFiller.getFieldValues( cs ) );
	}

	@Override
	public void close() throws IOException {
		counter.printCounter();
		writer.close();
	}

	public interface ChoiceSetRecordFiller<T> {
		List<String> getFieldNames();

		/**
		 * Returning numbers is needed for BIOGEME, but one might relax the requirement if estimation is done for instance
		 * with R-mlogit
		 * @param cs
		 * @return
		 */
		List<? extends Number> getFieldValues( ChoiceSet<T> cs );
	}
}
