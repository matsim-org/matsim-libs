/* *********************************************************************** *
 * project: org.matsim.*
 * CreateLegHistogramForSubpopulation.java
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
package playground.ivt.analysis.scripts;

import org.apache.log4j.Logger;
import org.matsim.analysis.LegHistogram;
import org.matsim.analysis.LegHistogramChart;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import playground.ivt.utils.ArgParser;
import playground.ivt.utils.ArgParser.Args;
import playground.ivt.utils.SubpopulationFilteringEventsManager;

import java.util.HashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public class CreateLegHistogramForSubpopulation {
	private static final Logger log =
		Logger.getLogger(CreateLegHistogramForSubpopulation.class);

	public static void main(final String[] args) {
		final ArgParser parser = new ArgParser();
		parser.setDefaultValue( "-a" , "--person-attributes" , null );
		parser.setDefaultValue( "-e" , "--input-events" , null );
		parser.setDefaultValue( "-n" , "--input-network" , null );
		parser.setDefaultValue( "-d" , "--output-data" , null );
		parser.setDefaultValue( "-f" , "--output-figure" , null );

		parser.setDefaultValue( "-x" , "--x-center" , "683518" );
		parser.setDefaultValue( "-y" , "--y-center" , "246836" );
		parser.setDefaultValue( "-r" , "--radius-km" , "20" );

		// optionnal: if none, default subpopulation
		parser.setDefaultValue( "-s" , "--subpopulation-name" , null );
		main( parser.parseArgs( args ) );
	}

	public static void main(final Args args) {
		final String personAttributesFile = args.getValue( "-a" );
		final String inputEventsFile = args.getValue( "-e" );
		final String inputNetworkFile = args.getValue( "-n" );
		final String outputDataFile = args.getValue( "-d" );
		final String outputFigure = args.getValue( "-f" );
		final String subpopulationName = args.getValue( "-s" );

		final Network network = readNetwork( inputNetworkFile );

		final Coord center = new Coord(args.getDoubleValue("-x"), args.getDoubleValue("-y"));
		final double radius = 1000 * args.getDoubleValue( "-r" );

		final ObjectAttributes atts = new ObjectAttributes();
		new ObjectAttributesXmlReader( atts ).parse( personAttributesFile );

		final LegHistogram histogram = new LegHistogram( 300 );
		final SubpopulationFilteringEventsManager events =
			new SubpopulationFilteringEventsManager( atts , subpopulationName );
		events.addHandler(
				new FilteringWrapper(
					center,
					radius,
					network,
					histogram ) );

		new EventsReaderXMLv1( events ).parse( inputEventsFile );

		if ( outputDataFile != null ) histogram.write( outputDataFile );
		if ( outputFigure != null ) LegHistogramChart.writeGraphic(histogram, outputFigure);
	}

	private static Network readNetwork(final String f) {
		if ( f == null ) return null;

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader(sc.getNetwork()).readFile( f );
		return sc.getNetwork();
	}

	private static class FilteringWrapper implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler {
		private final  Coord center;
		private final double radius;
		private final LegHistogram histogram;

		private final Network network;

		// process departures only if arrival is "accepted"
		private final Map<Id, PersonDepartureEvent> departures = new HashMap<Id, PersonDepartureEvent>();

		public FilteringWrapper(
				final Coord center,
				final double radius,
				final Network network,
				final LegHistogram histogram) {
			this.center = center;
			this.radius = radius;
			this.histogram = histogram;
			this.network = network;
		}

		@Override
		public void reset(final int iteration) {
			histogram.reset( iteration );
		}

		@Override
		public void handleEvent(final PersonStuckEvent event) {
			final PersonDepartureEvent departure = departures.remove( event.getPersonId() );
			if ( departure != null && acceptPerson( event.getPersonId() ) ) {
				histogram.handleEvent( departure );
				histogram.handleEvent( event );
			}
		}

		@Override
		public void handleEvent(final PersonArrivalEvent event) {
			final PersonDepartureEvent departure = departures.remove( event.getPersonId() );
			if ( departure == null ) return;
			if ( acceptPerson( event.getPersonId() ) && acceptLink( event.getLinkId() ) ) {
				histogram.handleEvent( departure );
				histogram.handleEvent( event );
			}
		}

		@Override
		public void handleEvent(final PersonDepartureEvent event) {
			if ( acceptPerson( event.getPersonId() ) && acceptLink( event.getLinkId() ) ) {
				departures.put( event.getPersonId() , event );
			}
		}

		private boolean acceptPerson( final Id personId ) {
			// reject transit drivers
			// XXX not nice...
			return !personId.toString().startsWith( "pt_" );
		}

		private boolean acceptLink( final Id linkId ) {
			if ( network == null ) return true;
			final Link link = network.getLinks().get( linkId );
			if ( link == null ) throw new IllegalArgumentException( "no link with id "+linkId );
			final Coord linkCoord = link.getCoord();

			final double dist =
				CoordUtils.calcEuclideanDistance(
						center,
						linkCoord );
			return dist <= radius;
		}
	}
}

