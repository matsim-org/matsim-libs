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

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static playground.meisterk.PersonAnalyseTimesByActivityType.Activities.e;

/**
 * @author thibautd
 */
public abstract class AbstractCsvWriter implements AutoCloseable {
	private static final Logger log = Logger.getLogger( AbstractCsvWriter.class );
	private final BufferedWriter writer;

	// execute writing in a parallel thread, not to slow down the algorithm
	// it is static to avoid too many threads for simple tasks (intuitively,
	// one big networks, building the clique should be slower than all the writers combined)
	private static final ExecutorService executor = Executors.newSingleThreadExecutor();

	protected abstract String titleLine();
	protected abstract Iterable<String> cliqueLines( Set<Ego> clique);

	protected AbstractCsvWriter(
			final String file ,
			final SocialNetworkSampler sampler,
			final AutocloserModule.Closer closer ) {
		log.info( "opening "+file );
		this.writer = IOUtils.getBufferedWriter( file );
		try {
			writer.write( titleLine() );
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
		// OK, as it is designed to be used by injection
		sampler.addCliqueListener( this::write );
		closer.add( this::close );
	}

	public final void write( final Set<Ego> egos ) {
		// execute outside of parallel thread, to make sure eg. stopwatch can be implemented
		final Iterable<String> lines = cliqueLines( egos );
		executor.submit( () -> {
			for ( String l : lines ) {
				try {
					writer.newLine();
					writer.write( l );
				}
				catch ( IOException e ) {
					throw new UncheckedIOException( e );
				}
			}
		});
	}

	@Override
	public synchronized final void close() throws IOException, InterruptedException {
		// synchronisation is not strictly necessary (one can call executor.shutdown repeteadly),
		// but it allows to log to look at how long it takes.
		if ( !executor.isShutdown() ) {
			log.info( "shutting down writing thread..." );
			executor.shutdown();
			executor.awaitTermination( Long.MAX_VALUE , TimeUnit.DAYS );
			log.info( "shutting down writing thread... DONE" );
		}
		writer.close();
	}
}
