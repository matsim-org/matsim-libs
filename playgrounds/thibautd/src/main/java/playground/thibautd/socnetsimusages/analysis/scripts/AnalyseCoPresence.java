/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyseCoPresence.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsimusages.analysis.scripts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.xml.sax.Attributes;

import playground.ivt.utils.ArgParser;
import playground.ivt.utils.ArgParser.Args;
import org.matsim.contrib.socnetsim.framework.events.CourtesyEvent;

/**
 * @author thibautd
 */
public class AnalyseCoPresence {
	// TODO filter by act type
	// TODO filter by coord?
	// TODO wraparound
	public static void main(final String[] args) {
		final ArgParser parser = new ArgParser();

		parser.setDefaultValue( "-e" , null );
		parser.setDefaultValue( "-o" , null );

		parser.setDefaultValue( "-p" , null );

		parser.setDefaultValue( "-a" , null );
		parser.setDefaultValue( "-s" , null );
		parser.setDefaultValue( "-sa" , "subpopulation" );

		main( parser.parseArgs( args ) );
	}

	private static void main( final Args args ) {
		final String eventsFile = args.getValue( "-e" );
		final String outputFile = args.getValue( "-o" );

		final String populationFile = args.getValue( "-p" );

		final String attributesFile = args.getValue( "-a" );
		final String subpopulation = args.getValue( "-s" );
		final String subpopAtt = args.getValue( "-sa" );

		final ObjectAttributes personAttributes = new ObjectAttributes();
		if ( attributesFile != null ) {
			new ObjectAttributesXmlReader( personAttributes ).parse( attributesFile );
		}

		final EventsManager events = EventsUtils.createEventsManager( );

		final Map<String, PersonStatisticsAccumulator> stats = new HashMap< >();
		new MatsimXmlParser() {
			@Override
			public void startTag( String name , Attributes atts , Stack<String> context ) {
				if ( name.equals( "person" ) ) {
					final String id = atts.getValue( "id" );
					final String subpop = (String) personAttributes.getAttribute( id , subpopAtt );

					if ( equals( subpop, subpopulation ) ) {
						stats.put( id, new PersonStatisticsAccumulator( id ) );
					}

				}
			}

			private boolean equals( Object o1 , Object o2 ) {
				if ( o1 == null ) return o2 == null;
				return o1.equals( o2 );
			}

			@Override
			public void endTag( String name , String content , Stack<String> context ) {
				// TODO Auto-generated method stub

			}
		}.parse( populationFile );

		events.addHandler( new BasicEventHandler() {
			@Override public void reset( int i ) {}

			@Override
			public void handleEvent( final Event event ) {
				try {
					if ( event.getEventType().equals( ""+CourtesyEvent.Type.sayHelloEvent ) ) {
						final String ego = event.getAttributes().get( "egoId" );
						final String alter = event.getAttributes().get( "alterId" );
						stats.get( ego ).notifyHello( alter , event.getTime() );
					}

					if ( event.getEventType().equals( ""+CourtesyEvent.Type.sayGoodbyeEvent ) ) {
						final String ego = event.getAttributes().get( "egoId" );
						final String alter = event.getAttributes().get( "alterId" );
						stats.get( ego ).notifyGoodBye( alter , event.getTime() );
					}
				}
				catch ( Exception e ) {
					throw new RuntimeException( "problem handling event "+event , e );
				}
			}
		} );

		new EventsReaderXMLv1( events ).parse( eventsFile );

		try ( final BufferedWriter writer = IOUtils.getBufferedWriter( outputFile ) ) {
			writer.write( "personId\tnMetContacts\ttimePassedWithContacts\tcumulatedTimePassedWithContacts\tmaxSimultaneousContacts" );

			for ( PersonStatisticsAccumulator acc : stats.values() ) {
				writer.newLine();
				writer.write( acc.personId+"\t" );
				writer.write( acc.interactionsWith.size()+"\t" );
				writer.write( acc.timePassedWithSocialContacts_s+"\t" );
				writer.write( acc.cumulatedTimePassedWithSocialContacts_s+"\t" );
				writer.write( acc.maxSimultaneousContacts+"" );
			}
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	private static class PersonStatisticsAccumulator {
		private final String personId;

		private Set< String > interactionsWith = new HashSet< >();
		private double timePassedWithSocialContacts_s = 0;
		private double cumulatedTimePassedWithSocialContacts_s = 0;
		private int maxSimultaneousContacts = 0;

		private double startMultiInteraction = Double.NaN;
		private final Map<String, Double> interactionStarts = new HashMap< >();
		
		public PersonStatisticsAccumulator(
				final String personId ) {
			this.personId = personId;
		}

		public void notifyHello( final String alter , final double time ) {
			interactionsWith.add( alter );

			if ( interactionStarts.isEmpty() ) startMultiInteraction = time;
			interactionStarts.put( alter , time );

			if ( interactionStarts.size() > maxSimultaneousContacts ) {
				maxSimultaneousContacts = interactionStarts.size();
			}
		}

		public void notifyGoodBye( final String alter , final double time ) {
			final Double start = interactionStarts.remove( alter );
			if ( start == null ) return; // do not consider wraparound...

			cumulatedTimePassedWithSocialContacts_s += time - start;
			if ( interactionStarts.isEmpty() ) {
				timePassedWithSocialContacts_s += time - startMultiInteraction;
				startMultiInteraction = Double.NaN;
			}
		}
	}

}

