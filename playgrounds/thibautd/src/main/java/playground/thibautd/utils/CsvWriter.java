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
package playground.thibautd.utils;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * @author thibautd
 */
public class CsvWriter implements AutoCloseable {
	private final BufferedWriter writer;
	private final CsvUtils.TitleLine titleLine;

	private String[] currentLine = null;

	private final char sep, quote;

	public CsvWriter( char sep, char quote, final CsvUtils.TitleLine titleLine, final String file ) {
		this.titleLine = titleLine;
		this.sep = sep;
		this.quote = quote;
		this.writer = IOUtils.getBufferedWriter( file );
		try {
			writer.write( CsvUtils.buildCsvLine( sep , quote , titleLine.getNames() ) );
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	public void setField( final String name , final String value ) {
		if ( currentLine == null ) currentLine = new String[ titleLine.getNField() ];
		this.currentLine[ titleLine.getIndexOfField( name ) ] = value;
	}

	public void nextLine() {
		if ( currentLine == null ) return;
		try {
			writer.newLine();
			writer.write( CsvUtils.buildCsvLine( sep , quote , currentLine ) );
			currentLine = null;
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	@Override
	public void close() throws IOException {
		nextLine();
		writer.close();
	}
}

