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

import java.io.BufferedReader;
import java.io.IOException;

/**
 * @author thibautd
 */
public class CsvParser implements AutoCloseable {
	private final char sep, quote;
	private final BufferedReader reader;

	private final CsvUtils.TitleLine titleLine;

	private String[] currentLine = null;

	public CsvParser(
			final char sep,
			final char quote,
			final String file ) throws IOException {
		this.sep = sep;
		this.quote = quote;
		this.reader = IOUtils.getBufferedReader( file );
		this.titleLine = CsvUtils.parseTitleLine( sep , quote , reader.readLine() );
	}

	public CsvUtils.TitleLine getTitleLine() {
		return titleLine;
	}

	public boolean nextLine() throws IOException {
		final String l = reader.readLine();
		if ( l == null ) return false;
		currentLine = CsvUtils.parseCsvLine( sep , quote , l );
		return true;
	}

	public String getField( final String name ) {
		// no check of validity. Assume users of this class know what they are doing, or are able to understand what goes wrong
		return currentLine[ titleLine.getIndexOfField( name ) ];
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}
}
