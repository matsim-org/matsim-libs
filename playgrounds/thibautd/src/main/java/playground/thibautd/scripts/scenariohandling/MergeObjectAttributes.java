/* *********************************************************************** *
 * project: org.matsim.*
 * MergeObjectAttributes.java
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
package playground.thibautd.scripts.scenariohandling;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * merges object attributes files to stdout
 * @author thibautd
 */
public class MergeObjectAttributes {
	public static void main(final String[] args) throws Exception {
		// disable logging, to avoid messing up with output
		Logger.getRootLogger().setLevel( Level.OFF );

		final Map<String, List<String>> linesPerObjectId = new LinkedHashMap<String, List<String>>();
		for ( String attributeFile : args ) {
			addLinesToMap( attributeFile , linesPerObjectId );
		}
		output( System.out , linesPerObjectId );
	}

	private static void output(
			final PrintStream out,
			final Map<String, List<String>> linesPerObjectId) {
		out.println( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
		out.println( "<!DOCTYPE objectAttributes SYSTEM \"http://matsim.org/files/dtd/objectattributes_v1.dtd\">" );
		out.println();
		out.println( "<objectAttributes>" );
		for ( Map.Entry<String, List<String>> objectAtts : linesPerObjectId.entrySet() ) {
			final String objectStartTag = objectAtts.getKey();
			final List<String> lines = objectAtts.getValue();

			out.println( "\t"+objectStartTag );
			for ( String line : lines ) out.println( line );
			out.println( "\t</object>" );
		}
		out.println( "</objectAttributes>" );
	}

	private static void addLinesToMap(
			final String attributeFile,
			final Map<String, List<String>> linesPerObjectId) throws IOException {
		List<String> currentLines = null;

		final BufferedReader reader = IOUtils.getBufferedReader( attributeFile );
		for ( String line = reader.readLine(); line != null; line = reader.readLine() ) {
			if (  line.trim().equals( "</object>" ) ) {
				currentLines = null;
			}

			if ( currentLines != null ) {
				// TODO: if memory problems, use line.intern()
				currentLines.add( line );
			}

			if ( line.trim().startsWith( "<object id=" ) ) {
				assert currentLines == null;
				currentLines = MapUtils.getList( line.trim() , linesPerObjectId );
			}
		}
		reader.close();
	}
}

