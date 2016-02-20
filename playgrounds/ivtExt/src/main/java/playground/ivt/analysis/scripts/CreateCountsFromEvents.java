/* *********************************************************************** *
 * project: org.matsim.*
 * CreateCountsFromEvents.java
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
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;
import org.matsim.counts.algorithms.CountSimComparisonTableWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm.DistanceFilter;
import org.matsim.counts.algorithms.CountsHtmlAndGraphsWriter;
import org.matsim.counts.algorithms.graphs.CountsErrorGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsLoadCurveGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimReal24GraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimRealPerHourGraphCreator;
import playground.ivt.utils.ArgParser;
import playground.ivt.utils.ArgParser.Args;

import java.util.Locale;

/**
 * @author thibautd
 */
public class CreateCountsFromEvents {
	private static final Logger log =
		Logger.getLogger(CreateCountsFromEvents.class);

	public static void main(final String[] args) {
		final ArgParser parser = new ArgParser();

		// input
		parser.setDefaultValue( "-n" , "--network" , null );
		parser.setDefaultValue( "-c" , "--counts" , null );
		parser.setDefaultValue( "-e" , "--events" , null );

		// parameters
		parser.setDefaultValue( "-s" , "--scale" , "10" );
		parser.setDefaultValue( "-proj" , "--projection" , "CH1903_LV03" );

		parser.setDefaultValue( "-x" , "--x-center" , "683518" );
		parser.setDefaultValue( "-y" , "--y-center" , "246836" );
		parser.setDefaultValue( "-r" , "--radius-km" , "20" );

		// out files per format. Any of them can be null
		parser.setDefaultValue( "-html" , "--html-out-file" , null );
		parser.setDefaultValue( "-kmz" , "--kmz-out-file" , null );
		parser.setDefaultValue( "-txt" , "--txt-out-file" , null );

		main( parser.parseArgs( args ) );
	}

	private static void main(final Args args) {
		final String netFile = args.getValue( "-n" );
		final String countsFile = args.getValue( "-c" );
		final String eventsFile = args.getValue( "-e" );

		final double scaleFactor = args.getDoubleValue( "-s" );

		final String coordSystem = args.getValue( "-proj" );

		final Coord center = new Coord(args.getDoubleValue("-x"), args.getDoubleValue("-y"));
		final double radius = 1000 * args.getDoubleValue( "-r" );

		final String htmlOutFile = args.getValue( "-html" );
		final String kmzOutFile = args.getValue( "-kmz" );
		final String txtOutFile = args.getValue( "-txt" );

		final Network network = readNetwork( netFile );
		final Counts counts = readCounts( countsFile );
		final VolumesAnalyzer volumes = readVolumes( network , eventsFile );

		// this is pretty confusing: parameters to the algorithm are passed
		// by the constructor, algorithm is run by the run() method, and
		// results can be obtained by the getComparison() method.
		final CountsComparisonAlgorithm cca =
			new CountsComparisonAlgorithm(
					volumes,
					counts,
					network,
					scaleFactor );
		cca.setDistanceFilter(
				new DistanceFilter() {
					@Override
					public boolean isInRange(final Count count) {
						try {
							final Coord c = count.getCoord() != null ?
									count.getCoord() :
									network.getLinks().get( count.getLocId() ).getCoord();
							return CoordUtils.calcEuclideanDistance( c , center ) <= radius;
						}
						catch ( Exception e ) {
							// ignore and proceed, as is the case in the default distance filter...
							log.error( "Error while locating count "+count+". Error was: " , e );
							log.error( "Proceed anyway..." );
							return false;
						}
					}
				});
		cca.run();

		if ( htmlOutFile != null ) {
			try {
				final CountsHtmlAndGraphsWriter cgw =
					new CountsHtmlAndGraphsWriter(
							htmlOutFile,
							cca.getComparison(),
							-1 );
				cgw.addGraphsCreator(new CountsSimRealPerHourGraphCreator("sim and real volumes"));
				cgw.addGraphsCreator(new CountsErrorGraphCreator("errors"));
				cgw.addGraphsCreator(new CountsLoadCurveGraphCreator("link volumes"));
				cgw.addGraphsCreator(new CountsSimReal24GraphCreator("average working day sim and count volumes"));
				cgw.createHtmlAndGraphs();
			}
			catch ( Exception e ) {
				log.error( "got exception when creating html output." , e );
				log.error( "proceed to next output format, if any." ); 
			}
		}

		if ( kmzOutFile != null ) {
			try {
				final CountSimComparisonKMLWriter kmlWriter =
					new CountSimComparisonKMLWriter(
						cca.getComparison(),
						network,
						TransformationFactory.getCoordinateTransformation(
							coordSystem,
							TransformationFactory.WGS84 ));
				kmlWriter.writeFile( kmzOutFile );
			}
			catch ( Exception e ) {
				log.error( "got exception when creating kmz output." , e );
				log.error( "proceed to next output format, if any." ); 
			}
		}

		if ( txtOutFile != null ) {
			try {
				final CountSimComparisonTableWriter ctw=
					new CountSimComparisonTableWriter(
							cca.getComparison(),
							Locale.ENGLISH);
				ctw.writeFile( txtOutFile );
			}
			catch ( Exception e ) {
				log.error( "got exception when creating txt output." , e );
				log.error( "proceed to next output format, if any." ); 
			}
		}
	}

	private static VolumesAnalyzer readVolumes(
			final Network network,
			final String eventsFile) {
		final VolumesAnalyzer volumes = new VolumesAnalyzer( 3600 , 24 * 3600 - 1 , network );
		final EventsManager events = EventsUtils.createEventsManager();
		events.addHandler( volumes );
		new MatsimEventsReader( events ).readFile( eventsFile );
		return volumes;
	}

	private static Counts readCounts(final String countsFile) {
		final Counts counts = new Counts();
		new MatsimCountsReader( counts ).readFile( countsFile );
		return counts;
	}

	private static Network readNetwork(final String netFile) {
		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader(sc.getNetwork()).readFile( netFile );
		return sc.getNetwork();
	}
}

