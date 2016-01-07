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

import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * @author thibautd
 */
public class ConnectionScanData {
	private final ContigousConnections connections;
	private final Footpaths footpaths;

	public ConnectionScanData( ContigousConnections connections, Footpaths footpaths ) {
		this.connections = connections;
		this.footpaths = footpaths;
	}

	public ContigousConnections getConnections() {
		return connections;
	}

	public Footpaths getFootpaths() {
		return footpaths;
	}

	public static ConnectionScanData createData( final TransitSchedule schedule ) {
		for ( TransitLine line : schedule.getTransitLines().values() ) {
			for ( TransitRoute route : line.getRoutes().values() ) {
				for ( Departure d : route.getDepartures().values() ) {
					// create connection
				}
			}
		}

		// TODO: footpaths

		return null;
	}
}

