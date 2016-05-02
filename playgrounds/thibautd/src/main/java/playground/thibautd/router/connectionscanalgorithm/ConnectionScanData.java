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
package playground.thibautd.router.connectionscanalgorithm;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.socnetsim.utils.QuadTreeRebuilder;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.*;

import java.util.*;

/**
 * @author thibautd
 */
public class ConnectionScanData {
	private final ContigousConnections connections;
	private final Footpaths footpaths;
	private final StopFacilityIndexer indexer;

	public ConnectionScanData(ContigousConnections connections, Footpaths footpaths, StopFacilityIndexer indexer) {
		this.connections = connections;
		this.footpaths = footpaths;
		this.indexer = indexer;
	}

	public ContigousConnections getConnections() {
		return connections;
	}

	public Footpaths getFootpaths() {
		return footpaths;
	}

	public static ConnectionScanData createData(
			final TransitSchedule schedule,
			final double maxBeelineWalkConnectionDistance ) {
		final StopFacilityIndexer stopNumericalIds = new StopFacilityIndexer( schedule );
		final ContigousConnections connections =
				createConnections(
						stopNumericalIds,
						schedule);
		final Footpaths footpaths =
				createFootpaths(
						stopNumericalIds,
						schedule,
						maxBeelineWalkConnectionDistance );

		return new ConnectionScanData( connections , footpaths, stopNumericalIds);
	}

	private static Footpaths createFootpaths(
			final StopFacilityIndexer stopNumericalIds,
			final TransitSchedule schedule,
			final double maxBeelineWalkConnectionDistance) {
		final QuadTreeRebuilder<Id<TransitStopFacility>> quadTreeRebuilder = new QuadTreeRebuilder<>();

		for ( TransitStopFacility s : schedule.getFacilities().values() ) {
			quadTreeRebuilder.put( s.getCoord() , s.getId() );
		}

		final QuadTree<Id<TransitStopFacility>> quadTree = quadTreeRebuilder.getQuadTree();

		final Footpaths footpaths = new Footpaths();
		for ( TransitStopFacility s : schedule.getFacilities().values() ) {
			final Collection<Id<TransitStopFacility>> close = quadTree.getDisk( s.getCoord().getX() , s.getCoord().getY() , maxBeelineWalkConnectionDistance );

			if ( !close.isEmpty() ) {
				final int id = stopNumericalIds.getIndex( s.getId() );
				for ( Id<TransitStopFacility> other : close ) {
					final double distance =
							CoordUtils.calcEuclideanDistance(
									schedule.getFacilities().get( s.getId() ).getCoord(),
									schedule.getFacilities().get( other ).getCoord() );
					footpaths.addFootpath(
							id,
							new Footpaths.Footpath(
									id,
									stopNumericalIds.getIndex( other ),
									distance ));
				}
			}
		}
		return footpaths;
	}

	private static ContigousConnections createConnections(
			final StopFacilityIndexer stopNumericalIds,
			final TransitSchedule schedule) {
		final List<Connection> connections = new ArrayList<>();
		int tripId = 0;
		for ( TransitLine line : schedule.getTransitLines().values() ) {
			for ( TransitRoute route : line.getRoutes().values() ) {
				for ( Departure departure : route.getDepartures().values() ) {
					// create connection
					final double lineDeparture = departure.getDepartureTime();
					double departureTime = departure.getDepartureTime();
					TransitRouteStop lastStop = null;
					for ( TransitRouteStop stop : route.getStops() ) {
						if ( lastStop != null ) {
							connections.add(
									new Connection(
											tripId,
											departureTime,
											stop.getArrivalOffset() + lineDeparture,
											lastStop.getStopFacility().getId(),
											stop.getStopFacility().getId() ) );
							departureTime = lineDeparture + stop.getDepartureOffset();
						}
						lastStop = stop;
					}
					tripId++;
				}
			}
		}

		Collections.sort( connections );

		final ContigousConnections container = new ContigousConnections( connections.size() );

		int i=0;
		for ( Connection c : connections ) {
			container.setConnection(
					i++,
					stopNumericalIds.getIndex( c.getDepartureStation() ),
					stopNumericalIds.getIndex( c.getArrivalStation() ),
					tripId,
					c.getDepartureTime(),
					c.getArrivalTime() );
		}

		return container;
	}
}

