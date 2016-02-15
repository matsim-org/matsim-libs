/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractActivityDurationsFromExecutedPlans.java
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
package playground.thibautd.scripts;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.xml.sax.Attributes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Stack;

/**
 * @author thibautd
 */
public class ExtractActivityDurationsFromExecutedPlans {
	public static void main(final String[] args) {
		final String inPopFile = args[ 0 ];
		final String outDatFile = args[ 1 ];

		final Parser p = new Parser( outDatFile );
		p.parse( inPopFile );
		p.close();
	}

	private static class Parser extends MatsimXmlParser {
		private final BufferedWriter writer;

		private String currentAgentId = null;
		private boolean isFirstAct = true;
		private double endFirstAct = Double.NaN;
		private final Counter counter = new Counter( "plan # " );

		public Parser(final String outFile) {
			writer = IOUtils.getBufferedWriter( outFile );

			try {
				writer.write( "agentId\tactType\tactDur_s" );
			}
			catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}

		@Override
		public void startTag(
				final String name,
				final Attributes atts,
				final Stack<String> context) {
			if ( name.equals( "person" ) ) {
				currentAgentId = atts.getValue( "id" );
			}
			if ( name.equals( "plan" ) ) {
				counter.incCounter();
				isFirstAct = true;
			}
			if ( name.equals( "act" ) ) {
				if ( isFirstAct ) {
					isFirstAct = false;
					endFirstAct = Time.parseTime( atts.getValue( "end_time" ) );
				}
				else {
					final double startTime = Time.parseTime( atts.getValue( "start_time" ) );

					final String endTimeString = atts.getValue( "end_time" );
					final double endTime = endTimeString == null ?
						endFirstAct + 24 * 3600 :
						Time.parseTime( endTimeString );

					try {
						writer.newLine();
						writer.write( currentAgentId+"\t"+atts.getValue( "type" )+"\t"+(endTime - startTime) );
					}
					catch (IOException e) {
						throw new UncheckedIOException( e );
					}
				}
			}
		}

		@Override
		public void endTag(
				final String name,
				final String content,
				final Stack<String> context) {
		}

		public void close() {
			try {
				writer.close();
			}
			catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}
	}
}
