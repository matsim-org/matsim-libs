/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.replanning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.buffer.BufferParameters;

/**
 * Provide a common constructor and some common methods which should not differ between strategies,
 * e.g. there should be only one algorithm to determine the 2nd terminus stop in order to have a 
 * unique terminus stop definition among all strategies.
 * 
 * @author aneumann
 */
abstract class AbstractPStrategyModule implements PStrategy {

	AbstractPStrategyModule() {

	}

	/**
	 * Find the 2nd terminus stop (1st terminus is at index 0 per definition).
	 * 
	 * Returns stop index instead of the stop, in order to cater for stops which are
	 * served multiple times
	 * 
	 * @param stops
	 * @return index of the stop which is half way on the route from start stop over
	 *         all stops back to the start stop
	 */
	final int findStopIndexWithLargestDistance(ArrayList<TransitStopFacility> stops) {
		double totatDistance = 0;
		Map<Integer, Double> distFromStart2StopIndex = new HashMap<>();
		TransitStopFacility previousStop = stops.get(0);

		for (int i = 0; i < stops.size(); i++) {
			TransitStopFacility currentStop = stops.get(i);
			totatDistance = totatDistance
					+ CoordUtils.calcEuclideanDistance(previousStop.getCoord(), currentStop.getCoord());
			distFromStart2StopIndex.put(i, totatDistance);
			previousStop = currentStop;
		}
		// add leg from last to first stop
		totatDistance = totatDistance
				+ CoordUtils.calcEuclideanDistance(previousStop.getCoord(), stops.get(0).getCoord());

		// first terminus is first stop in stops, other terminus is stop half way on the
		// circular route beginning at the first stop
		for (int i = 1; i < stops.size(); i++) {
			if (distFromStart2StopIndex.get(i) >= totatDistance / 2) {
				if (Math.abs(totatDistance / 2 - distFromStart2StopIndex.get(i - 1)) > Math
						.abs(totatDistance / 2 - distFromStart2StopIndex.get(i))) {
					return i;
				} else {
					return i - 1;
				}
			}
		}

		return 0;
	}

	final Set<Id<TransitStopFacility>> getStopsUsed(Collection<TransitRoute> routes) {
		Set<Id<TransitStopFacility>> stopsUsed = new TreeSet<>();
		for (TransitRoute route : routes) {
			for (TransitRouteStop stop : route.getStops()) {
				stopsUsed.add(stop.getStopFacility().getId());
			}
		}
		return stopsUsed;
	}

	final List<Geometry> createGeometryFromStops(ArrayList<TransitStopFacility> stops, int remoteStopIndex) {
		List<Geometry> geometries = new LinkedList<>();

		ArrayList<Coordinate> coords = new ArrayList<>();
		for (int i = 0; i < stops.size(); i++) {
			TransitStopFacility stop = stops.get(i);
			if (i == remoteStopIndex) {
				// terminate current line string
				coords.add(new Coordinate(stop.getCoord().getX(), stop.getCoord().getY(), 0.0));
				Coordinate[] coordinates = coords.toArray(new Coordinate[coords.size()]);
				Geometry lineString = new GeometryFactory().createLineString(coordinates);
				geometries.add(lineString);
				// create new line string
				coords = new ArrayList<>();
				coords.add(new Coordinate(stop.getCoord().getX(), stop.getCoord().getY(), 0.0));
			} else {
				coords.add(new Coordinate(stop.getCoord().getX(), stop.getCoord().getY(), 0.0));
			}
		}
		// add first stop to close the circle
		coords.add(new Coordinate(stops.get(0).getCoord().getX(), stops.get(0).getCoord().getY(), 0.0));

		Coordinate[] coordinates = coords.toArray(new Coordinate[coords.size()]);
		Geometry lineString = new GeometryFactory().createLineString(coordinates);
		geometries.add(lineString);
		return geometries;
	}

	final Geometry createBuffer(List<Geometry> lineStrings, double bufferSize, boolean excludeTermini) {
		BufferParameters bufferParameters = new BufferParameters();

		if (excludeTermini) {
			bufferParameters.setEndCapStyle(BufferParameters.CAP_FLAT);
		} else {
			bufferParameters.setEndCapStyle(BufferParameters.CAP_ROUND);
		}

		Geometry union = null;

		for (Geometry lineString : lineStrings) {
			Geometry buffer = BufferOp.bufferOp(lineString, bufferSize, bufferParameters);
			if (union == null) {
				union = buffer;
			} else {
				union = union.union(buffer);
			}
		}

		return union;
	}

}