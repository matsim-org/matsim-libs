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

import eu.eunoiaproject.bikesharing.framework.BikeSharingConstants;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacilities;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacilitiesReader;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacility;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import playground.ivt.utils.ArgParser;
import playground.ivt.utils.ArgParser.Args;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public class ExtractFlowsAndAltitudes {
	public static void main(final String[] args) throws IOException {
		final ArgParser parser = new ArgParser();
		parser.setDefaultValue( "-e" , "--events-file" , null );
		parser.setDefaultValue( "-a" , "--attributes-file" , null );
		parser.setDefaultValue( "-s" , "--stations-file" , null );
		parser.setDefaultValue( "-n" , "--altitude-attribute-name" , "alt_dhm25" );
		parser.setDefaultValue( "-o" , "--output" , null );
		parser.setDefaultValue( "-oa" , "--output-aggregated" , null );
		main( parser.parseArgs( args ) );
	}

	private static void main(final Args args) throws IOException {
		final String eventsFile = args.getValue( "-e" );
		final String attributesFile = args.getValue( "-a" );
		final String stationsFile = args.getValue( "-s" );
		final String attributeName = args.getValue( "-n" );
		final String outputFile = args.getValue( "-o" );
		final String outputFlowsFile = args.getValue( "-oa" );

		final BikeSharingFacilities stations = readStations( stationsFile );

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
					final Map<Id<Person>, Id<BikeSharingFacility>> departureStationForAgent = new HashMap< >();

					@Override
					public void reset(int iteration) {}

					@Override
					public void handleEvent(ActivityStartEvent event) {
						if ( !event.getActType().equals( BikeSharingConstants.INTERACTION_TYPE ) ) return;
						final Id<BikeSharingFacility> departureStation = departureStationForAgent.remove( event.getPersonId() );

						final Id<BikeSharingFacility> eventFacilityId =
								Id.create(
									event.getFacilityId(),
									BikeSharingFacility.class );
						if ( departureStation == null ) {
							// no departure: it must be one
							departureStationForAgent.put(
								event.getPersonId(),
								eventFacilityId );
							return;
						}

						final Od od = getOd( new Od( departureStation , eventFacilityId ) );
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

			writer.write( "station1\tx1\ty1\tstation2\tx2\ty2\talt1\talt2\tflow12\tflow21" );
			for ( Od od : flows.keySet() ) {
				final double altO = (Double) atts.getAttribute( od.o.toString() , attributeName );
				final double altD = (Double) atts.getAttribute( od.d.toString() , attributeName );
				writer.newLine();
				final BikeSharingFacility stationO = stations.getFacilities().get( od.o );
				final BikeSharingFacility stationD = stations.getFacilities().get( od.d );
				writer.write( od.o+"\t"+stationO.getCoord().getX()+"\t"+stationO.getCoord().getY()+"\t"+
						od.d+"\t"+stationD.getCoord().getX()+"\t"+stationD.getCoord().getY()+"\t"+
						altO+"\t"+altD+"\t"+od.odFlow+"\t"+od.doFlow );
			}
			writer.close();
		}

		if ( outputFlowsFile != null ) {
			final BufferedWriter writer = IOUtils.getBufferedWriter( outputFlowsFile );

			writer.write( "station\tx\ty\talt\toutFlow\tsqrtOutFlow\tinFlow\tsqrtInFlow\toutExcess\tsqrtOutExcess" );
			for ( Map.Entry<Id, AggregatedFlow> e : aggFlows.entrySet() ) {
				final double alt = (Double) atts.getAttribute( e.getKey().toString() , attributeName );
				final BikeSharingFacility station = stations.getFacilities().get( e.getKey() );
				writer.newLine();
				final double outExcess = e.getValue().outFlow - e.getValue().inFlow;
				writer.write( e.getKey()+"\t"+
						station.getCoord().getX()+"\t"+station.getCoord().getY()+"\t"+
						alt+"\t"+
						e.getValue().outFlow+"\t"+Math.sqrt( e.getValue().outFlow )+"\t"+
						e.getValue().inFlow+"\t"+Math.sqrt( e.getValue().inFlow )+"\t"+
						outExcess+"\t"+signSqrt( outExcess ) );
			}
			writer.close();
		}

	}

	private static double signSqrt( double outExcess ) {
		final double signum = Math.signum( outExcess );
		return signum * Math.sqrt( signum * outExcess );
	}

	public static BikeSharingFacilities readStations( final String stationsFile ) {
		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new BikeSharingFacilitiesReader( scenario ).parse( stationsFile );
		return (BikeSharingFacilities)
				scenario.getScenarioElement(
					BikeSharingFacilities.ELEMENT_NAME );
	}

	private static class AggregatedFlow {
		public int outFlow = 0;
		public int inFlow = 0;
	}

	private static class Od {
		private final Id<BikeSharingFacility> o,d;
		private int odFlow = 0;
		private int doFlow = 0;

		public Od(
				final Id<BikeSharingFacility> o,
				final Id<BikeSharingFacility> d) {
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

