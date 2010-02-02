/* *********************************************************************** *
 * project: org.matsim.*
 * VisumNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.yalcin.visum;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;

public class VisumNetwork {

	private final Map<Id, Stop> stops = new TreeMap<Id, Stop>();
	private QuadTree<Stop> stopsQT = null;

	public void addStop(final Stop stop) {
		Stop oldStop = this.stops.put(stop.id, stop);
		if (oldStop != null) {
			// there was already a stop with the same id
			// redo the insertion
			this.stops.put(oldStop.id, oldStop);
			throw new IllegalArgumentException("There is already a stop with the same id.");
		}
		this.stopsQT = null;
	}

	/**
	 * Returns a list of all stops that are located at most <code>distance<code> away from <code>coord</code>.
	 * @param coord
	 * @param distance
	 * @return list of stops, empty list if no stop is found.
	 */
	public Collection<Stop> findStops(final Coord coord, final double distance) {
		if (this.stopsQT == null) {
			this.cacheStops();
		}
		return this.stopsQT.get(coord.getX(), coord.getY(), distance);
	}

	public Stop findNearestStop(final Coord coord) {
		if (this.stopsQT == null) {
			this.cacheStops();
		}
		return this.stopsQT.get(coord.getX(), coord.getY());
	}

	private synchronized void cacheStops() {
		if (this.stopsQT != null) {
			return;
		}
		// find bounds
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (Stop stop : this.stops.values()) {
			double x = stop.coord.getX();
			double y = stop.coord.getY();
			if (x < minX) {
				minX = x;
			}
			if (x > maxX) {
				maxX = x;
			}
			if (y < minY) {
				minY = y;
			}
			if (y > maxY) {
				maxY = y;
			}
		}
		QuadTree<Stop> quadtree = new QuadTree<Stop>(minX, minY, maxX, maxY);
		this.stopsQT = quadtree;
		for (Stop stop : this.stops.values()) {
			quadtree.put(stop.coord.getX(), stop.coord.getY(), stop);
		}
	}

	public static class Stop {
		public final Id id;
		public final String name;
		public final Coord coord;

		public Stop(final Id id, final String name, final Coord coord) {
			this.id = id;
			this.name = name;
			this.coord = coord;
		}
	}
}
