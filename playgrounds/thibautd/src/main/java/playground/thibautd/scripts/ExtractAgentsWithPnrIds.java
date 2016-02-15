/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractAgentsWithPnrIds.java
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

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;
import playground.thibautd.parknride.ParkAndRideConstants;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Stack;

/**
 * Extracts the ids of agents for which the selected plan includes park and ride.
 * For use with VIA's agent filter.
 * @author thibautd
 */
public class ExtractAgentsWithPnrIds {
	private static final Logger log =
		Logger.getLogger(ExtractAgentsWithPnrIds.class);

	public static void main(final String[] args) {
		String inputFile = args[ 0 ];
		String outputFile = args[ 1 ];

		log.info( "START" );
		PopulationParser parser = new PopulationParser( outputFile );
		log.info( "reading "+inputFile );
		parser.parse( inputFile );
		log.info( "finish" );
		parser.finish();
		log.info( "END" );

	}


	/**
	 * to avoid having to load the full scenario just to get activity types
	 */
	private static class PopulationParser extends MatsimXmlParser {
		private final static String PERSON = "person";
		private final static String ID = "id";
		private final static String ACTIVITY = "act";
		private final static String TYPE = "type";
		private final static String PLAN = "plan";
		private final static String SELECTED = "selected";
		private final BufferedWriter writer;
		private final Counter counter = new Counter( "analysing agent # " );

		private int dumpCount = 0;
		private String currentId = null;
		private boolean agentAlreadyMarked = false;
		private boolean isSelectedPlan = false;

		public PopulationParser(final String outputFileName) {
			log.info( "ids will be written to "+outputFileName );
		 	writer = IOUtils.getBufferedWriter( outputFileName );
		}

		public void finish() {
			try {
				writer.close();
			} catch (IOException e) {
				throw new RuntimeException( e );
			}
			counter.printCounter();
			log.info( dumpCount+" ids were identified." );
		}

		@Override
		public void startTag(
				final String name,
				final Attributes atts,
				final Stack<String> context) {
			if (PERSON.equals( name )) {
				counter.incCounter();
				currentId = atts.getValue( ID );
				agentAlreadyMarked = false;
				isSelectedPlan = false;
			}
			else if (PLAN.equals( name )) {
				isSelectedPlan = "yes".equals( atts.getValue( SELECTED ) );
			}
			else if (isSelectedPlan && !agentAlreadyMarked && ACTIVITY.equals( name )) {
				if (ParkAndRideConstants.PARKING_ACT.equals( atts.getValue( TYPE ) )) {
					try {
						writer.write( currentId );
						writer.newLine();
						dumpCount++;
					} catch (IOException e) {
						throw new RuntimeException( e );
					}
					agentAlreadyMarked = true;
				}
			}
		}

		@Override
		public void endTag(
				final String name,
				final String content,
				final Stack<String> context) {
			// nothing to do
		}
	}
}

