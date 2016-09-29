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

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

import java.io.IOException;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Should be created as a resource in a try-with-resources block. Not sure how dirty purists would consider that, but
 * does the job with little code.
 *
 * @author thibautd
 */
public class CsvWritingModule extends AbstractModule implements AutoCloseable{
	private final CliquesCsvWriter cliquesCsvWriter;
	private final TiesCsvWriter tiesCsvWriter;

	public CsvWritingModule( final String outputDirectory ) {
		this.cliquesCsvWriter = new CliquesCsvWriter( outputDirectory+"/output_cliques.csv" );
		this.tiesCsvWriter = new TiesCsvWriter( outputDirectory+"/output_ties.csv" );
	}

	@Override
	protected void configure() {
		bind( new TypeLiteral<Consumer<Set<Ego>>>(){} ).toInstance( cliquesCsvWriter.andThen( tiesCsvWriter ) );
	}

	@Override
	public void close() throws IOException {
		cliquesCsvWriter.close();
		tiesCsvWriter.close();
	}
}
