/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractFlowsAndAltitudes.java
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
package eu.eunoiaproject.bikesharing.framework.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.ivt.utils.ArgParser;
import playground.ivt.utils.ArgParser.Args;
import eu.eunoiaproject.bikesharing.framework.BikeSharingConstants;

import playground.ivt.utils.MapUtils;

/**
 * @author thibautd
 */
public class ExtractFlowsAndAltitudes {
	public static void main(final String[] args) throws IOException {
		final ArgParser parser = new ArgParser();
		parser.setDefaultValue( "-e" , "--events-file" , null );
		parser.setDefaultValue( "-a" , "--attributes-file" , null );
		parser.setDefaultValue( "-n" , "--altitude-attribute-name" , "alt_dhm25" );
		parser.setDefaultValue( "-o" , "--output" , null );
		parser.setDefaultValue( "-oa" , "--output-aggregated" , null );
		main( parser.parseArgs( args ) );
	}

	private static void main(final Args args) throws IOException {
		final String eventsFile = args.getValue( "-e" );
		final String attributesFile = args.getValue( "-a" );
		final String attributeName = args.getValue( "-n" );
		final String outputFile = args.getValue( "-o" );
		final String outputFlowsFile = args.getValue( "-oa" );

		final ObjectAttributes atts = new ObjectAttributes();
		new ObjectAttributesXmlReader( atts ).parse( attributesFile );
		
		final Map<Od, Od> flows = new HashMap<Od, Od>();
		final Map<Id, AggregatedFlow> aggFlows = new HashMap<Id, AggregatedFlow>();
		final MapUtils.Factory<AggregatedFlow> flowFactory =
			new MapUtils.Factory<AggregatedFlow>() {
				@Override
				public AggregatedFlow create() {
					return new AggregatedFlow();
				}
			};
		final EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(
				new ActivityStartEventHandler() {
					final Map<Id, Id> departureStationForAgent = new HashMap<Id, Id>();

					@Override
					public void reset(int iteration) {}

					@Override
					public void handleEvent(ActivityStartEvent event) {
						if ( !event.getActType().equals( BikeSharingConstants.INTERACTION_TYPE ) ) return;
						final Id departureStation = departureStationForAgent.remove( event.getPersonId() );

						if ( departureStation == null ) {
							// no departure: it must be one
							departureStationForAgent.put( event.getPersonId() , event.getFacilityId() );
							return;
						}

						final Od od = getOd( new Od( departureStation , event.getFacilityId() ) );
						od.incFlow( departureStation , event.getFacilityId() );
						MapUtils.getArbitraryObject(
							departureStation,
							aggFlows,
							flowFactory ).outFlow++;
						MapUtils.getArbitraryObject(
							event.getFacilityId(),
							aggFlows,
							flowFactory ).inFlow++;
					}

					private Od getOd(final Od od) {
						final Od inMap = flows.get( od );
						if ( inMap != null ) return inMap;
						flows.put( od , od );
						return od;
					}
				});
		new MatsimEventsReader( events ).readFile( eventsFile );


		if ( outputFile != null ) {
			final BufferedWriter writer = IOUtils.getBufferedWriter( outputFile );

			writer.write( "station1\tstation2\talt1\talt2\tflow12\tflow21" );
			for ( Od od : flows.keySet() ) {
				final double altO = (Double) atts.getAttribute( od.o.toString() , attributeName );
				final double altD = (Double) atts.getAttribute( od.d.toString() , attributeName );
				writer.newLine();
				writer.write( od.o+"\t"+od.d+"\t"+altO+"\t"+altD+"\t"+od.odFlow+"\t"+od.doFlow );
			}
			writer.close();
		}

		if ( outputFlowsFile != null ) {
			final BufferedWriter writer = IOUtils.getBufferedWriter( outputFlowsFile );

			writer.write( "station\talt\toutFlow\tinFlow" );
			for ( Map.Entry<Id, AggregatedFlow> e : aggFlows.entrySet() ) {
				final double alt = (Double) atts.getAttribute( e.getKey().toString() , attributeName );
				writer.newLine();
				writer.write( e.getKey()+"\t"+alt+"\t"+e.getValue().outFlow+"\t"+e.getValue().inFlow );
			}
			writer.close();
		}

	}

	private static class AggregatedFlow {
		public int outFlow = 0;
		public int inFlow = 0;
	}

	private static class Od {
		private final Id o,d;
		private int odFlow = 0;
		private int doFlow = 0;

		public Od(
				final Id o,
				final Id d) {
			this.o = o;
			this.d = d;
		}

		public void incFlow(final Id oFlow , final Id dFlow ) {
			if ( oFlow.equals( o ) && dFlow.equals( d ) ) {
				odFlow++;
			}
			else if ( oFlow.equals( d ) && dFlow.equals( o ) ) {
				doFlow++;
			}
			else throw new IllegalArgumentException( oFlow+" and "+dFlow+" not compatible with "+o+" and "+d );
		}

		@Override
		public boolean equals(final Object other) {
			return equals( (Od) other ); 
		}

		public boolean equals(final Od other) {
			return (other.o.equals( o ) && other.d.equals( d )) ||
				(other.o.equals( d ) && other.d.equals( o ));
		}

		@Override
		public int hashCode() {
			return o.hashCode() + d.hashCode();
		}
	}
}

