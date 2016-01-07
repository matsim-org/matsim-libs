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

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.List;

/**
 * @author thibautd
 */
public class Footpaths {
	private final TIntObjectMap<List<Footpath>> footpathsPerStation = new TIntObjectHashMap<>();

	public void addFootpath( final int station , final Footpath footpath ) {
		List<Footpath> list = footpathsPerStation.get( station );

		if ( list == null ) {
			list = new ArrayList<>(  );
			footpathsPerStation.put( station , list );
		}

		list.add( footpath );
	}

	public List<Footpath> getFootpaths( final int station ) {
		return footpathsPerStation.get( station );
	}

	public static class Footpath {
		private final int originStation, destinationStation;
		private final double walkDistance;

		public Footpath( int originStation, int destinationStation, double walkDistance ) {
			this.originStation = originStation;
			this.destinationStation = destinationStation;
			this.walkDistance = walkDistance;
		}

		public int getDestinationStation() {
			return destinationStation;
		}

		public int getOriginStation() {
			return originStation;
		}

		public double getWalkDistance() {
			return walkDistance;
		}
	}
}
