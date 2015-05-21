/* *********************************************************************** *
 * project: org.matsim.*
 * FlattenCliqueComposition.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.scripts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Stack;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

import org.matsim.contrib.socnetsim.framework.cliques.population.CliquesSchemaNames;

/**
 * flattens a clique file for easy usage from other tools.
 * @author thibautd
 */
public class FlattenCliqueComposition {
	private static final String SEP = "\t";
	private static final String CLIQUE = "cliqueId";
	private static final String PERSON = "personId";

	public static void main(final String[] args) {
		String inFile = args[ 0 ];
		String outFile = args[ 1 ];

		BufferedWriter writer = IOUtils.getBufferedWriter( outFile );
		Parser parser = new Parser( writer );
		parser.parse( inFile );
		try {
			writer.close();
		} catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	private static class Parser extends MatsimXmlParser {
		private final BufferedWriter writer;
		private String currentClique = null;
		private int line = 0;
		private final Counter counter = new Counter( "writing line # " );

		public Parser(final BufferedWriter writer) {
			super( false );
			this.writer = writer;
			try {
				this.writer.write( CLIQUE + SEP + PERSON );
			} catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}

		@Override
		public void startTag(
				final String name,
				final Attributes atts,
				final Stack<String> context) {
			if (name.equals( CliquesSchemaNames.CLIQUE )) {
				currentClique = atts.getValue( CliquesSchemaNames.CLIQUE_ID );
			}
			else if (name.equals( CliquesSchemaNames.MEMBER )) {
				line++;
				counter.incCounter();
				try {
					writer.newLine();
					writer.write(line + SEP + currentClique + SEP + atts.getValue( CliquesSchemaNames.MEMBER_ID ));
				}
				catch (IOException e) {
					throw new UncheckedIOException( e );
				}
			}
		}

		@Override
		public void endTag(
				final String name,
				final String content,
				final Stack<String> context) {
			// TODO Auto-generated method stub
			
		}
	}
}

